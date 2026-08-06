package com.example.myapp.service;

import com.example.myapp.contants.enumeration.GomokuGameStatus;
import com.example.myapp.contants.enumeration.NotificationType;
import com.example.myapp.domain.GomokuGame;
import com.example.myapp.domain.User;
import com.example.myapp.repository.GomokuGameRepository;
import com.example.myapp.repository.UserFollowRepository;
import com.example.myapp.repository.UserRepository;
import com.example.myapp.service.dto.GomokuGameDTO;
import com.example.myapp.service.dto.GomokuInvitesDTO;
import com.example.myapp.service.dto.GomokuOpponentDTO;
import com.example.myapp.utils.SecurityUtil;
import com.example.myapp.web.rest.errors.BadRequestAlertException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Server-authoritative logic for online Gomoku: invitations, game lifecycle, and move
 * validation with win detection. The board string is the single source of truth; the client
 * is never trusted to decide turns or winners.
 */
@Service
@Transactional
public class GomokuService {

    private static final Logger LOG = LoggerFactory.getLogger(GomokuService.class);
    private static final String ENTITY_NAME = "gomokuGame";
    private static final int SIZE = GomokuGame.BOARD_SIZE;
    private static final int[][] DIRS = { { 0, 1 }, { 1, 0 }, { 1, 1 }, { 1, -1 } };
    private static final List<GomokuGameStatus> LIVE = List.of(GomokuGameStatus.PENDING, GomokuGameStatus.ACTIVE);
    private static final String GOMOKU_LINK = "/mini-game/gomoku";

    private final GomokuGameRepository gameRepository;
    private final UserFollowRepository userFollowRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Value("${app.gomoku.invite-timeout-seconds:300}")
    private long inviteTimeoutSeconds;

    @Value("${app.gomoku.game-timeout-seconds:1800}")
    private long gameTimeoutSeconds;

    @Value("${app.gomoku.move-timeout-seconds:120}")
    private long moveTimeoutSeconds;

    public GomokuService(
        GomokuGameRepository gameRepository,
        UserFollowRepository userFollowRepository,
        UserRepository userRepository,
        NotificationService notificationService
    ) {
        this.gameRepository = gameRepository;
        this.userFollowRepository = userFollowRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    // ----- Opponents (mutual follows) -----------------------------------------------------

    @Transactional(readOnly = true)
    public List<GomokuOpponentDTO> listOpponents() {
        String me = SecurityUtil.getCurrentUsername();
        Set<String> mutual = mutualFollows(me);
        if (mutual.isEmpty()) {
            return List.of();
        }
        Map<String, User> byLogin = usersByLogin(mutual);
        return mutual.stream()
            .map(login -> {
                User u = byLogin.get(login);
                return new GomokuOpponentDTO(login, u == null ? login : displayName(u), u == null ? null : u.getAvatar());
            })
            .sorted((a, b) -> a.name().compareToIgnoreCase(b.name()))
            .toList();
    }

    private Set<String> mutualFollows(String me) {
        Set<String> iFollow = userFollowRepository
            .findByFollowerUsernameOrderByFollowedDateDesc(me, org.springframework.data.domain.Pageable.unpaged())
            .stream().map(f -> f.getFollowingUsername()).collect(Collectors.toSet());
        Set<String> followMe = userFollowRepository
            .findByFollowingUsernameOrderByFollowedDateDesc(me, org.springframework.data.domain.Pageable.unpaged())
            .stream().map(f -> f.getFollowerUsername()).collect(Collectors.toSet());
        iFollow.retainAll(followMe);
        return iFollow;
    }

    private boolean isMutualFollow(String a, String b) {
        return userFollowRepository.existsByFollowerUsernameAndFollowingUsername(a, b)
            && userFollowRepository.existsByFollowerUsernameAndFollowingUsername(b, a);
    }

    // ----- Invitations --------------------------------------------------------------------

    public GomokuGameDTO invite(String opponentLogin) {
        String me = SecurityUtil.getCurrentUsername();
        if (me.equals(opponentLogin)) {
            throw new BadRequestAlertException("You cannot invite yourself", ENTITY_NAME, "inviteself");
        }
        if (!userRepository.existsByLogin(opponentLogin)) {
            throw new BadRequestAlertException("User not found", ENTITY_NAME, "usernotfound");
        }
        if (!isMutualFollow(me, opponentLogin)) {
            throw new BadRequestAlertException("You can only invite users who follow each other", ENTITY_NAME, "notmutual");
        }
        if (currentActiveGame(me).isPresent()) {
            throw new BadRequestAlertException("You already have a game in progress", ENTITY_NAME, "alreadyplaying");
        }
        // Reuse a still-live game between us rather than creating a duplicate; skip any that just
        // expired/timed out (resolveTimeouts transitions them so they no longer count as live).
        Optional<GomokuGame> existing = gameRepository.findBetween(me, opponentLogin, LIVE).stream()
            .map(this::resolveTimeouts)
            .filter(g -> LIVE.contains(g.getStatus()))
            .findFirst();
        if (existing.isPresent()) {
            return toDto(existing.get(), me);
        }
        GomokuGame game = new GomokuGame();
        // Randomly decide who plays black. Black always moves first (Gomoku convention and
        // a real first-move advantage), so randomizing the colour keeps the first move fair
        // instead of always handing it to the inviter. The inviter's identity is preserved
        // in createdBy (immutable), which drives accept/decline/cancel authorization below.
        boolean inviterIsBlack = ThreadLocalRandom.current().nextBoolean();
        game.setBlackUsername(inviterIsBlack ? me : opponentLogin);
        game.setWhiteUsername(inviterIsBlack ? opponentLogin : me);
        game.setStatus(GomokuGameStatus.PENDING);
        game.setBoard(GomokuGame.EMPTY_BOARD);
        game.setCurrentPlayer(1);
        game.setCreatedBy(me);
        game.setLastModifiedBy(me);
        GomokuGame saved = gameRepository.save(game);
        // Notify the invited player so it shows in their bell with a link into the game.
        notificationService.notify(
            opponentLogin,
            NotificationType.INFO,
            "Gomoku invitation",
            nameOf(me) + " invited you to a Gomoku match",
            GOMOKU_LINK
        );
        return toDto(saved, me);
    }

    public GomokuInvitesDTO getInvites() {
        String me = SecurityUtil.getCurrentUsername();
        // Direction is by inviter (createdBy), not colour: an invite I created is outgoing,
        // one created by the other participant is incoming. Invites past their timeout are
        // resolved to EXPIRED here and dropped from the lists.
        List<GomokuGameDTO> incoming = new ArrayList<>();
        List<GomokuGameDTO> outgoing = new ArrayList<>();
        for (GomokuGame g : gameRepository.findByParticipantAndStatus(me, GomokuGameStatus.PENDING)) {
            if (resolveTimeouts(g).getStatus() != GomokuGameStatus.PENDING) {
                continue;
            }
            (me.equals(g.getCreatedBy()) ? outgoing : incoming).add(toDto(g, me));
        }
        return new GomokuInvitesDTO(incoming, outgoing);
    }

    public GomokuGameDTO accept(Long id) {
        String me = SecurityUtil.getCurrentUsername();
        GomokuGame game = requirePending(id);
        requireInvitee(game, me);
        if (currentActiveGame(me).isPresent()) {
            throw new BadRequestAlertException("You already have a game in progress", ENTITY_NAME, "alreadyplaying");
        }
        Instant now = Instant.now();
        game.setStatus(GomokuGameStatus.ACTIVE);
        game.setCurrentPlayer(1); // black moves first; black was assigned randomly at invite time
        game.setStartedDate(now);
        game.setLastMoveDate(now); // starts black's first-move clock
        game.setLastModifiedBy(me);
        GomokuGame saved = gameRepository.save(game);
        notificationService.notify(
            game.getCreatedBy(),
            NotificationType.SUCCESS,
            "Invitation accepted",
            nameOf(me) + " accepted your Gomoku invitation",
            GOMOKU_LINK
        );
        return toDto(saved, me);
    }

    public GomokuGameDTO decline(Long id) {
        String me = SecurityUtil.getCurrentUsername();
        GomokuGame game = requirePending(id);
        requireInvitee(game, me);
        game.setStatus(GomokuGameStatus.DECLINED);
        game.setLastModifiedBy(me);
        GomokuGame saved = gameRepository.save(game);
        notificationService.notify(
            game.getCreatedBy(),
            NotificationType.INFO,
            "Invitation declined",
            nameOf(me) + " declined your Gomoku invitation",
            GOMOKU_LINK
        );
        return toDto(saved, me);
    }

    public GomokuGameDTO cancel(Long id) {
        String me = SecurityUtil.getCurrentUsername();
        GomokuGame game = requirePending(id);
        if (!me.equals(game.getCreatedBy())) {
            throw new BadRequestAlertException("Only the inviter can cancel", ENTITY_NAME, "notinviter");
        }
        game.setStatus(GomokuGameStatus.CANCELLED);
        game.setLastModifiedBy(me);
        return toDto(gameRepository.save(game), me);
    }

    // ----- Gameplay -----------------------------------------------------------------------

    public GomokuGameDTO getGame(Long id) {
        String me = SecurityUtil.getCurrentUsername();
        GomokuGame game = resolveTimeouts(requireParticipant(id, me));
        return toDto(game, me);
    }

    public Optional<GomokuGameDTO> getActiveGame() {
        String me = SecurityUtil.getCurrentUsername();
        return currentActiveGame(me).map(g -> toDto(g, me));
    }

    public GomokuGameDTO move(Long id, int row, int col) {
        String me = SecurityUtil.getCurrentUsername();
        GomokuGame game = resolveTimeouts(requireParticipant(id, me));
        // A late click after the clock already lapsed: surface the resolved result, not an error.
        if (game.getStatus() != GomokuGameStatus.ACTIVE) {
            return toDto(game, me);
        }
        int myColor = colorOf(game, me);
        if (game.getCurrentPlayer() != myColor) {
            throw new BadRequestAlertException("It is not your turn", ENTITY_NAME, "notyourturn");
        }
        if (row < 0 || row >= SIZE || col < 0 || col >= SIZE) {
            throw new BadRequestAlertException("Move is out of bounds", ENTITY_NAME, "outofbounds");
        }
        int idx = row * SIZE + col;
        char[] cells = game.getBoard().toCharArray();
        if (cells[idx] != '0') {
            throw new BadRequestAlertException("Cell is already occupied", ENTITY_NAME, "occupied");
        }
        cells[idx] = (char) ('0' + myColor);
        game.setBoard(new String(cells));
        game.setLastMoveRow(row);
        game.setLastMoveCol(col);
        game.setMoveCount(game.getMoveCount() + 1);
        game.setLastMoveDate(Instant.now()); // resets the per-move clock for the next player

        if (isWin(cells, row, col, myColor)) {
            game.setStatus(GomokuGameStatus.FINISHED);
            game.setWinner(myColor);
        } else if (game.getMoveCount() == SIZE * SIZE) {
            game.setStatus(GomokuGameStatus.FINISHED);
            game.setWinner(0); // draw
        } else {
            game.setCurrentPlayer(myColor == 1 ? 2 : 1);
        }
        game.setLastModifiedBy(me);
        return toDto(gameRepository.save(game), me);
    }

    public GomokuGameDTO resign(Long id) {
        String me = SecurityUtil.getCurrentUsername();
        GomokuGame game = requireParticipant(id, me);
        if (game.getStatus() != GomokuGameStatus.ACTIVE) {
            throw new BadRequestAlertException("Game is not active", ENTITY_NAME, "notactive");
        }
        int myColor = colorOf(game, me);
        game.setStatus(GomokuGameStatus.RESIGNED);
        game.setWinner(myColor == 1 ? 2 : 1);
        game.setLastModifiedBy(me);
        return toDto(gameRepository.save(game), me);
    }

    // ----- Win detection ------------------------------------------------------------------

    private boolean isWin(char[] cells, int row, int col, int player) {
        char target = (char) ('0' + player);
        for (int[] d : DIRS) {
            int count = 1 + countDir(cells, row, col, d[0], d[1], target) + countDir(cells, row, col, -d[0], -d[1], target);
            if (count >= 5) {
                return true;
            }
        }
        return false;
    }

    private int countDir(char[] cells, int row, int col, int dr, int dc, char target) {
        int n = 0;
        int r = row + dr;
        int c = col + dc;
        while (r >= 0 && r < SIZE && c >= 0 && c < SIZE && cells[r * SIZE + c] == target) {
            n++;
            r += dr;
            c += dc;
        }
        return n;
    }

    // ----- Helpers ------------------------------------------------------------------------

    private GomokuGame requirePending(Long id) {
        GomokuGame game = gameRepository.findById(id)
            .orElseThrow(() -> new BadRequestAlertException("Game not found", ENTITY_NAME, "notfound"));
        if (game.getStatus() != GomokuGameStatus.PENDING) {
            throw new BadRequestAlertException("Invite is no longer pending", ENTITY_NAME, "notpending");
        }
        return game;
    }

    private GomokuGame requireParticipant(Long id, String me) {
        GomokuGame game = gameRepository.findById(id)
            .orElseThrow(() -> new BadRequestAlertException("Game not found", ENTITY_NAME, "notfound"));
        if (!isParticipant(game, me)) {
            throw new BadRequestAlertException("You are not a participant in this game", ENTITY_NAME, "notparticipant");
        }
        return game;
    }

    /** The invitee is the participant who did not create the invite (createdBy = inviter). */
    private void requireInvitee(GomokuGame game, String me) {
        if (!isParticipant(game, me) || me.equals(game.getCreatedBy())) {
            throw new BadRequestAlertException("Only the invitee can respond to this invite", ENTITY_NAME, "notinvitee");
        }
    }

    private boolean isParticipant(GomokuGame game, String me) {
        return me.equals(game.getBlackUsername()) || me.equals(game.getWhiteUsername());
    }

    private int colorOf(GomokuGame game, String username) {
        return username.equals(game.getBlackUsername()) ? 1 : 2;
    }

    // ----- Timeouts (lazy, resolved on read/act) ------------------------------------------

    /**
     * Transition a game/invite to its terminal state if a deadline has passed, persisting the
     * change. A no-op (and no write) when nothing has expired, so it is safe to call on every
     * read/validation path. Idempotent — {@code @Version} guards concurrent resolutions.
     */
    private GomokuGame resolveTimeouts(GomokuGame game) {
        Instant now = Instant.now();
        if (game.getStatus() == GomokuGameStatus.PENDING) {
            if (game.getCreatedDate() != null && elapsedSeconds(game.getCreatedDate(), now) >= inviteTimeoutSeconds) {
                game.setStatus(GomokuGameStatus.EXPIRED);
                return gameRepository.save(game);
            }
            return game;
        }
        if (game.getStatus() == GomokuGameStatus.ACTIVE) {
            if (game.getStartedDate() != null && elapsedSeconds(game.getStartedDate(), now) >= gameTimeoutSeconds) {
                game.setStatus(GomokuGameStatus.FINISHED);
                game.setWinner(0); // round cap → draw
                return gameRepository.save(game);
            }
            if (game.getLastMoveDate() != null && elapsedSeconds(game.getLastMoveDate(), now) >= moveTimeoutSeconds) {
                game.setStatus(GomokuGameStatus.TIMED_OUT);
                game.setWinner(game.getCurrentPlayer() == 1 ? 2 : 1); // player to move forfeits
                return gameRepository.save(game);
            }
        }
        return game;
    }

    /** The caller's active game, resolving any timeout first so a lapsed game no longer counts. */
    private Optional<GomokuGame> currentActiveGame(String me) {
        return gameRepository.findActiveForUser(me)
            .map(this::resolveTimeouts)
            .filter(g -> g.getStatus() == GomokuGameStatus.ACTIVE);
    }

    private long elapsedSeconds(Instant from, Instant to) {
        return Duration.between(from, to).getSeconds();
    }

    private Integer secondsRemaining(Instant from, long budgetSeconds, Instant now) {
        if (from == null) {
            return null;
        }
        return (int) Math.max(0, budgetSeconds - elapsedSeconds(from, now));
    }

    private Map<String, User> usersByLogin(Collection<String> logins) {
        if (logins.isEmpty()) {
            return Map.of();
        }
        return userRepository.findByLoginIn(logins).stream()
            .collect(Collectors.toMap(User::getLogin, Function.identity(), (a, b) -> a));
    }

    private String displayName(User user) {
        if (user.getRealName() != null && !user.getRealName().isBlank()) {
            return user.getRealName();
        }
        if (user.getNickName() != null && !user.getNickName().isBlank()) {
            return user.getNickName();
        }
        return user.getLogin();
    }

    /** Display name for a login (falls back to the login itself), for notification text. */
    private String nameOf(String login) {
        return userRepository.findOneByLogin(login).map(this::displayName).orElse(login);
    }

    private GomokuGameDTO toDto(GomokuGame game, String me) {
        Set<String> logins = new HashSet<>(List.of(game.getBlackUsername(), game.getWhiteUsername()));
        Map<String, User> byLogin = usersByLogin(new ArrayList<>(logins));
        User black = byLogin.get(game.getBlackUsername());
        User white = byLogin.get(game.getWhiteUsername());
        Instant now = Instant.now();
        boolean active = game.getStatus() == GomokuGameStatus.ACTIVE;
        boolean pending = game.getStatus() == GomokuGameStatus.PENDING;
        return new GomokuGameDTO(
            game.getId(),
            game.getBlackUsername(),
            black == null ? game.getBlackUsername() : displayName(black),
            black == null ? null : black.getAvatar(),
            game.getWhiteUsername(),
            white == null ? game.getWhiteUsername() : displayName(white),
            white == null ? null : white.getAvatar(),
            game.getStatus().name(),
            game.getBoard(),
            game.getCurrentPlayer(),
            game.getWinner(),
            game.getLastMoveRow(),
            game.getLastMoveCol(),
            game.getMoveCount(),
            colorOf(game, me),
            game.getCreatedDate(),
            (int) moveTimeoutSeconds,
            active ? secondsRemaining(game.getLastMoveDate(), moveTimeoutSeconds, now) : null,
            active ? secondsRemaining(game.getStartedDate(), gameTimeoutSeconds, now) : null,
            pending ? secondsRemaining(game.getCreatedDate(), inviteTimeoutSeconds, now) : null
        );
    }
}
