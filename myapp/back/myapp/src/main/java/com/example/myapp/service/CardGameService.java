package com.example.myapp.service;

import com.example.myapp.contants.enumeration.CardGameInvitationStatus;
import com.example.myapp.contants.enumeration.CardGameStatus;
import com.example.myapp.contants.enumeration.NotificationType;
import com.example.myapp.domain.CardGame;
import com.example.myapp.domain.CardGamePlayer;
import com.example.myapp.domain.User;
import com.example.myapp.repository.CardGamePlayerRepository;
import com.example.myapp.repository.CardGameRepository;
import com.example.myapp.repository.UserFollowRepository;
import com.example.myapp.repository.UserRepository;
import com.example.myapp.service.CardDeck.Play;
import com.example.myapp.service.dto.CardGameDTO;
import com.example.myapp.service.dto.CardGameInvitesDTO;
import com.example.myapp.service.dto.CardGameOpponentDTO;
import com.example.myapp.service.dto.CardGamePlayerDTO;
import com.example.myapp.service.dto.CardGameTrickEntryDTO;
import com.example.myapp.utils.SecurityUtil;
import com.example.myapp.web.rest.errors.BadRequestAlertException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Server-authoritative logic for the online 70235 card game: table setup, invitations, the
 * match itself, CPU opponents and timeouts. The rules live in {@link CardDeck}; this class
 * owns lifecycle and storage.
 *
 * <p>The server holds the deck and every hand, and {@link #toDto} is built per recipient, so
 * a client only ever learns its own cards and everyone else's card count. The client is never
 * trusted to decide turns, legality or scores.
 *
 * <p>There is no scheduler in this application, so both CPU turns and timeouts are resolved
 * lazily: {@link #advance} runs on every read and every action, bringing the table up to date
 * before any response is built.
 */
@Service
@Transactional
public class CardGameService {
    private static final Logger LOG = LoggerFactory.getLogger(CardGameService.class);
    private static final String ENTITY_NAME = "cardGame";
    /**
     * Where a notification about one of these games takes the recipient: the 70235 table itself,
     * not the card-game list. Every notification here concerns a specific table — an invitation
     * to answer, a seat filled, a table withdrawn — and the list page cannot show any of them, so
     * landing there leaves the reader one more click from the thing they were told about.
     */
    private static final String CARD_GAME_LINK = "/mini-game/card-games/70235";
    private static final List<CardGameStatus> LIVE = List.of(CardGameStatus.PENDING, CardGameStatus.ACTIVE);

    /** Prefix for synthetic CPU seat usernames: {@code cpu-1}..{@code cpu-4}. */
    private static final String CPU_PREFIX = "cpu-";

    /**
     * Upper bound on server-taken moves resolved in one call — CPU seats and seats on
     * auto-play. The game terminates on its own (the deck drains and hands then shrink
     * strictly), so this is a safety net against a logic bug spinning forever rather than part
     * of the rules. It is reached legitimately only in the sense that a table where every seat
     * is automatic can run to its end inside one request, which is well under this bound.
     */
    private static final int MAX_AUTO_STEPS = 500;

    /**
     * Upper bound on tricks in one game, for the same reason. Far above any real game: with
     * 108 cards and at least one card leaving a hand per trick, a real game ends long before.
     */
    private static final int MAX_TRICKS = 1000;

    /** Matches the {@code trick_log} column. The log is trimmed from the front to stay inside it. */
    private static final int TRICK_LOG_MAX = 512;

    private final CardGameRepository gameRepository;
    private final CardGamePlayerRepository playerRepository;
    private final UserFollowRepository userFollowRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Value("${app.card-game.invite-timeout-seconds:300}")
    private long inviteTimeoutSeconds;

    @Value("${app.card-game.turn-timeout-seconds:120}")
    private long turnTimeoutSeconds;

    @Value("${app.card-game.game-timeout-seconds:3600}")
    private long gameTimeoutSeconds;

    public CardGameService(
        CardGameRepository gameRepository,
        CardGamePlayerRepository playerRepository,
        UserFollowRepository userFollowRepository,
        UserRepository userRepository,
        NotificationService notificationService
    ) {
        this.gameRepository = gameRepository;
        this.playerRepository = playerRepository;
        this.userFollowRepository = userFollowRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    // ----- Invitable users (one-way follow) -----------------------------------------------

    /**
     * Users the caller may invite: their followers. One-way by design — you can invite anyone
     * who follows you, whether or not you follow them back. This differs from Gomoku, which
     * requires a mutual follow; do not copy that check here.
     */
    @Transactional(readOnly = true)
    public List<CardGameOpponentDTO> listOpponents() {
        String me = SecurityUtil.getCurrentUsername();
        Set<String> followers = followersOf(me);
        if (followers.isEmpty()) {
            return List.of();
        }
        Map<String, User> byLogin = usersByLogin(followers);
        return followers.stream()
            .map(login -> {
                User u = byLogin.get(login);
                return new CardGameOpponentDTO(login, u == null ? login : displayName(u), u == null ? null : u.getAvatar());
            })
            .sorted((a, b) -> a.name().compareToIgnoreCase(b.name()))
            .toList();
    }

    /** Logins of everyone following {@code me}. */
    private Set<String> followersOf(String me) {
        return userFollowRepository
            .findByFollowingUsernameOrderByFollowedDateDesc(me, Pageable.unpaged())
            .stream()
            .map(f -> f.getFollowerUsername())
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /** Whether {@code candidate} follows {@code me}, and so may be invited by them. */
    private boolean followsMe(String candidate, String me) {
        return userFollowRepository.existsByFollowerUsernameAndFollowingUsername(candidate, me);
    }

    // ----- Table setup --------------------------------------------------------------------

    /**
     * Create a table seating the host, the invited followers and {@code cpuCount} CPU
     * opponents. A table with no human invitees deals immediately — there is nobody to wait
     * for. Otherwise it stays PENDING until the invitees answer and the host starts it.
     */
    public CardGameDTO createTable(int cpuCount, List<String> invitees) {
        String me = SecurityUtil.getCurrentUsername();
        List<String> humans = normalizeInvitees(invitees, me);
        int seats = 1 + humans.size() + cpuCount;
        if (cpuCount < 0) {
            throw new BadRequestAlertException("CPU count cannot be negative", ENTITY_NAME, "negativecpu");
        }
        if (seats < CardGame.MIN_PLAYERS || seats > CardGame.MAX_PLAYERS) {
            throw new BadRequestAlertException(
                "A table seats " + CardGame.MIN_PLAYERS + " to " + CardGame.MAX_PLAYERS + " players",
                ENTITY_NAME,
                "badseatcount"
            );
        }
        if (currentLiveGame(me).isPresent()) {
            throw new BadRequestAlertException("You already have a game in progress", ENTITY_NAME, "alreadyplaying");
        }
        for (String invitee : humans) {
            if (!userRepository.existsByLogin(invitee)) {
                throw new BadRequestAlertException("User not found: " + invitee, ENTITY_NAME, "usernotfound");
            }
            if (!followsMe(invitee, me)) {
                throw new BadRequestAlertException("You can only invite users who follow you", ENTITY_NAME, "notfollower");
            }
            if (currentLiveGame(invitee).isPresent()) {
                throw new BadRequestAlertException(nameOf(invitee) + " is already in a game", ENTITY_NAME, "opponentbusy");
            }
        }

        CardGame game = new CardGame();
        game.setStatus(CardGameStatus.PENDING);
        game.setHostUsername(me);
        game.setCreatedBy(me);
        game.setLastModifiedBy(me);
        CardGame saved = gameRepository.save(game);

        int seat = 0;
        // The host takes seat 0: seat order drives turn order, and the host leads trick one.
        addPlayer(saved, me, seat++, false, CardGameInvitationStatus.ACCEPTED);
        for (String invitee : humans) {
            addPlayer(saved, invitee, seat++, false, CardGameInvitationStatus.PENDING);
        }
        for (int i = 0; i < cpuCount; i++) {
            addPlayer(saved, CPU_PREFIX + (i + 1), seat++, true, CardGameInvitationStatus.ACCEPTED);
        }

        if (humans.isEmpty()) {
            // CPUs only: nothing to wait for, so deal straight away.
            deal(saved, me);
        } else {
            for (String invitee : humans) {
                notificationService.notify(
                    invitee,
                    NotificationType.INFO,
                    "70235 invitation",
                    nameOf(me) + " invited you to a game of 70235",
                    CARD_GAME_LINK
                );
            }
        }
        CardGame result = gameRepository.save(saved);
        return toDto(advance(result), me);
    }

    /** Reject blanks, self-invites and duplicates, preserving the order the host chose. */
    private List<String> normalizeInvitees(List<String> invitees, String me) {
        if (invitees == null) {
            return List.of();
        }
        Set<String> unique = new LinkedHashSet<>();
        for (String raw : invitees) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            String invitee = raw.trim();
            if (invitee.equals(me)) {
                throw new BadRequestAlertException("You cannot invite yourself", ENTITY_NAME, "inviteself");
            }
            if (!unique.add(invitee)) {
                throw new BadRequestAlertException("Duplicate invitee: " + invitee, ENTITY_NAME, "duplicateinvitee");
            }
        }
        return new ArrayList<>(unique);
    }

    private CardGamePlayer addPlayer(CardGame game, String username, int seat, boolean cpu, CardGameInvitationStatus status) {
        CardGamePlayer player = new CardGamePlayer();
        player.setGame(game);
        player.setUsername(username);
        player.setSeat(seat);
        player.setCpu(cpu);
        player.setInvitationStatus(status);
        player.setCreatedBy(game.getHostUsername());
        player.setLastModifiedBy(game.getHostUsername());
        return playerRepository.save(player);
    }

    // ----- Invitation responses -----------------------------------------------------------

    /** The caller's pending tables: invitations to answer, and tables they host. */
    @Transactional(readOnly = true)
    public CardGameInvitesDTO getInvites() {
        String me = SecurityUtil.getCurrentUsername();
        List<CardGameDTO> incoming = new ArrayList<>();
        List<CardGameDTO> outgoing = new ArrayList<>();
        for (CardGame game : gameRepository.findByParticipantAndStatus(me, CardGameStatus.PENDING)) {
            if (resolveTimeouts(game).getStatus() != CardGameStatus.PENDING) {
                continue; // lapsed while sitting in the lobby
            }
            (me.equals(game.getHostUsername()) ? outgoing : incoming).add(toDto(game, me));
        }
        return new CardGameInvitesDTO(incoming, outgoing);
    }

    public CardGameDTO accept(Long id) {
        String me = SecurityUtil.getCurrentUsername();
        CardGame game = requirePending(id);
        CardGamePlayer seat = requireInvitee(game, me, true);
        if (currentActiveGame(me).isPresent()) {
            throw new BadRequestAlertException("You already have a game in progress", ENTITY_NAME, "alreadyplaying");
        }
        seat.setInvitationStatus(CardGameInvitationStatus.ACCEPTED);
        seat.setLastModifiedBy(me);
        playerRepository.save(seat);
        notificationService.notify(
            game.getHostUsername(),
            NotificationType.SUCCESS,
            "Invitation accepted",
            nameOf(me) + " accepted your 70235 invitation",
            CARD_GAME_LINK
        );
        return toDto(game, me);
    }

    public CardGameDTO decline(Long id) {
        String me = SecurityUtil.getCurrentUsername();
        CardGame game = requirePending(id);
        CardGamePlayer seat = requireInvitee(game, me, false);
        boolean wasAccepted = seat.getInvitationStatus() == CardGameInvitationStatus.ACCEPTED;
        seat.setInvitationStatus(CardGameInvitationStatus.DECLINED);
        seat.setLastModifiedBy(me);
        playerRepository.save(seat);
        notificationService.notify(
            game.getHostUsername(),
            NotificationType.INFO,
            wasAccepted ? "Player left" : "Invitation declined",
            wasAccepted ? nameOf(me) + " left your 70235 table" : nameOf(me) + " declined your 70235 invitation",
            CARD_GAME_LINK
        );
        return toDto(game, me);
    }

    /** Host withdraws a table that has not started. Everyone still invited is told. */
    public CardGameDTO cancel(Long id) {
        String me = SecurityUtil.getCurrentUsername();
        CardGame game = requirePending(id);
        if (!me.equals(game.getHostUsername())) {
            throw new BadRequestAlertException("Only the host can cancel the table", ENTITY_NAME, "nothost");
        }
        game.setStatus(CardGameStatus.CANCELLED);
        game.setLastModifiedBy(me);
        CardGame saved = gameRepository.save(game);
        for (CardGamePlayer player : seatsOf(saved)) {
            if (player.isCpu() || me.equals(player.getUsername())) {
                continue;
            }
            notificationService.notify(
                player.getUsername(),
                NotificationType.INFO,
                "70235 table withdrawn",
                nameOf(me) + " withdrew the 70235 table",
                CARD_GAME_LINK
            );
        }
        return toDto(saved, me);
    }

    /**
     * Host deals and begins. Every invitee must have answered; those who declined lose their
     * seat, and the remaining seat count must still be legal.
     */
    public CardGameDTO start(Long id) {
        String me = SecurityUtil.getCurrentUsername();
        CardGame game = requirePending(id);
        if (!me.equals(game.getHostUsername())) {
            throw new BadRequestAlertException("Only the host can start the game", ENTITY_NAME, "nothost");
        }
        List<CardGamePlayer> seats = seatsOf(game);
        if (seats.stream().anyMatch(p -> p.getInvitationStatus() == CardGameInvitationStatus.PENDING)) {
            throw new BadRequestAlertException("Some invitations have not been answered yet", ENTITY_NAME, "invitespending");
        }
        deal(game, me);
        return toDto(advance(gameRepository.save(game)), me);
    }

    // ----- Deal -----------------------------------------------------------------------------

    /**
     * Build the deck for the final seat count, deal, and set the table playing. Players who
     * declined are dropped first, so the deck size matches who is actually at the table: one
     * 54-card pack for two players, two packs for three to five.
     *
     * <p>The host leads the first trick and is dealt the extra card for it.
     */
    private void deal(CardGame game, String actor) {
        List<CardGamePlayer> seats = seatsOf(game);
        List<CardGamePlayer> declined = seats.stream()
            .filter(p -> p.getInvitationStatus() == CardGameInvitationStatus.DECLINED)
            .toList();
        declined.forEach(playerRepository::delete);
        List<CardGamePlayer> playing = new ArrayList<>(
            seats.stream().filter(p -> p.getInvitationStatus() == CardGameInvitationStatus.ACCEPTED).toList()
        );
        if (playing.size() < CardGame.MIN_PLAYERS || playing.size() > CardGame.MAX_PLAYERS) {
            throw new BadRequestAlertException(
                "A game needs " + CardGame.MIN_PLAYERS + " to " + CardGame.MAX_PLAYERS + " players",
                ENTITY_NAME,
                "badseatcount"
            );
        }
        // Close the gaps left by declined seats so seat numbers stay contiguous — turn order
        // walks them modulo the seat count.
        for (int i = 0; i < playing.size(); i++) {
            playing.get(i).setSeat(i);
        }

        List<String> deck = CardDeck.buildDeck(playing.size());
        String leader = game.getHostUsername();
        for (CardGamePlayer player : playing) {
            int size = player.getUsername().equals(leader) ? CardGame.LEAD_HAND_SIZE : CardGame.TARGET_HAND_SIZE;
            List<String> hand = new ArrayList<>(deck.subList(0, size));
            deck.subList(0, size).clear();
            player.setHand(CardDeck.join(hand));
            player.setScore(0);
            player.setLastModifiedBy(actor);
            playerRepository.save(player);
        }

        Instant now = Instant.now();
        game.setDeck(CardDeck.join(deck));
        game.setStatus(CardGameStatus.ACTIVE);
        game.setCurrentTurnUsername(leader);
        game.setTrickNumber(1);
        game.setStartedDate(now);
        game.setLastActionDate(now);
        clearTrick(game);
        // clearTrick keeps the log (it has to outlive the trick it describes), so a fresh deal
        // clears it here instead — nothing has been played yet.
        game.setTrickLog("");
        game.setTrickLogNumber(0);
        game.setLastModifiedBy(actor);
        LOG.debug("Dealt 70235 game {} to {} players, {} cards left in the deck", game.getId(), playing.size(), deck.size());
    }

    /**
     * Reset the per-trick state: no play to beat, nobody passed, empty pile.
     *
     * <p>The trick log is left alone on purpose. A trick is won by its last play, so emptying
     * the log here would erase the winning play at the instant it won — {@link #appendTrickLog}
     * rolls the log over when the next trick actually starts instead.
     */
    private void clearTrick(CardGame game) {
        game.setCurrentPlay(null);
        game.setCurrentPlayUsername(null);
        game.setCurrentShape(null);
        game.setCurrentRank(null);
        game.setTrickPile("");
        game.setPassedUsernames("");
    }

    /**
     * Append one entry to the trick log: {@code seat:cards}, or {@code seat:-} for a pass.
     *
     * <p>The log is what lets a client show a lead being raised. Several seats can act inside a
     * single request — every CPU between one human turn and the next moves while the table is
     * advanced — so without it a player would see their 8 replaced by a 10 with no sign of the
     * 9 that came between.
     *
     * <p>Rolls over here rather than at resolution. A trick's winning play is its last entry,
     * so the completed log has to survive being scored for the winning play to be visible at
     * all; it is replaced only once the following trick's first action arrives.
     */
    private void appendTrickLog(CardGame game, CardGamePlayer seat, String cards) {
        String entry = seat.getSeat() + ":" + cards;
        boolean sameTrick = game.getTrickLogNumber() == game.getTrickNumber();
        String existing = game.getTrickLog();
        String log = !sameTrick || existing == null || existing.isEmpty()
            ? entry
            : existing + "|" + entry;
        // Bounded by the rules (a raise strictly increases in rank, a pass is final), but a
        // malformed state must not fail the write that scores a trick, so drop the oldest
        // entries rather than overflow the column.
        while (log.length() > TRICK_LOG_MAX && log.contains("|")) {
            log = log.substring(log.indexOf('|') + 1);
        }
        game.setTrickLog(log);
        game.setTrickLogNumber(game.getTrickNumber());
    }

    // ----- Gameplay -------------------------------------------------------------------------

    /** Full state for the caller, with CPU turns and timeouts brought up to date first. */
    public CardGameDTO getGame(Long id) {
        String me = SecurityUtil.getCurrentUsername();
        CardGame game = advance(requireParticipant(id, me));
        return toDto(game, me);
    }

    /** The caller's live table, if any — used to resume on page load. */
    public Optional<CardGameDTO> getActiveGame() {
        String me = SecurityUtil.getCurrentUsername();
        return currentActiveGame(me).map(g -> toDto(advance(g), me));
    }

    /**
     * Play a selection of cards. The cards must be in the caller's hand, must form a legal
     * shape, and — unless the caller is leading — must beat the play on the table.
     */
    public CardGameDTO play(Long id, List<String> cards) {
        String me = SecurityUtil.getCurrentUsername();
        CardGame game = advance(requireParticipant(id, me));
        // A click that lands after the table already moved on: show the resolved state rather
        // than an error the player can do nothing about.
        if (game.getStatus() != CardGameStatus.ACTIVE) {
            return toDto(game, me);
        }
        if (!me.equals(game.getCurrentTurnUsername())) {
            throw new BadRequestAlertException("It is not your turn", ENTITY_NAME, "notyourturn");
        }
        CardGamePlayer seat = seatOf(game, me);
        List<String> selection = cards == null ? List.of() : cards.stream().filter(c -> c != null && !c.isBlank()).toList();
        if (selection.isEmpty()) {
            throw new BadRequestAlertException("Select at least one card", ENTITY_NAME, "noselection");
        }
        List<String> hand = CardDeck.parse(seat.getHand());
        if (!removeCards(hand, selection)) {
            throw new BadRequestAlertException("Those cards are not in your hand", ENTITY_NAME, "notinhand");
        }
        Play detected = CardDeck.detectShape(selection)
            .orElseThrow(() -> new BadRequestAlertException("That is not a legal play", ENTITY_NAME, "illegalshape"));
        if (!CardDeck.beats(detected, game.getCurrentShape(), game.getCurrentRank())) {
            throw new BadRequestAlertException("That does not beat the current play", ENTITY_NAME, "cannotbeat");
        }
        applyPlay(game, seat, hand, detected, me);
        return toDto(advance(gameRepository.save(game)), me);
    }

    /** Pass this trick. Illegal when leading — somebody has to put a play down. */
    public CardGameDTO pass(Long id) {
        String me = SecurityUtil.getCurrentUsername();
        CardGame game = advance(requireParticipant(id, me));
        if (game.getStatus() != CardGameStatus.ACTIVE) {
            return toDto(game, me);
        }
        if (!me.equals(game.getCurrentTurnUsername())) {
            throw new BadRequestAlertException("It is not your turn", ENTITY_NAME, "notyourturn");
        }
        if (game.getCurrentPlay() == null) {
            throw new BadRequestAlertException("You are leading — you must play", ENTITY_NAME, "mustlead");
        }
        applyPass(game, seatOf(game, me));
        return toDto(advance(gameRepository.save(game)), me);
    }

    /**
     * Hand this player's turns to the server, or take them back. The same brain the CPU seats
     * use plays the seat while this is on, which is why the feature is here rather than in the
     * client: the rules would otherwise need a second implementation in the browser, and a
     * browser playing for you stops the moment its tab closes.
     *
     * <p>Only ever applies to the caller's own seat. The flag is written <em>before</em> the
     * table is advanced, which is what makes the switch work in both directions: turning it on
     * while the turn is already yours plays that turn in this same response, and turning it off
     * on your own turn keeps that turn for you instead of spending it on the way out.
     */
    public CardGameDTO setAutoPlay(Long id, boolean enabled) {
        String me = SecurityUtil.getCurrentUsername();
        CardGame game = requireParticipant(id, me);
        CardGamePlayer seat = seatOf(game, me);
        // A table that has already finished has nothing to automate, and an error here would be
        // about a state the caller cannot affect. Same treatment as a late play or pass.
        if (LIVE.contains(game.getStatus()) && seat.isAutoPlay() != enabled) {
            seat.setAutoPlay(enabled);
            seat.setLastModifiedBy(me);
            playerRepository.save(seat);
            LOG.debug("70235 game {}: {} turned auto-play {}", game.getId(), me, enabled ? "on" : "off");
        }
        return toDto(advance(game), me);
    }

    /**
     * Leave the table. An active game ends for everyone: hands are hidden, so there is no way
     * to carry on fairly once a seat empties mid-trick.
     */
    public CardGameDTO leave(Long id) {
        String me = SecurityUtil.getCurrentUsername();
        CardGame game = advance(requireParticipant(id, me));
        if (game.getLeftByUsername() != null) {
            return toDto(game, me);
        }
        boolean wasLive = LIVE.contains(game.getStatus());
        if (game.getStatus() == CardGameStatus.ACTIVE) {
            game.setStatus(CardGameStatus.ABANDONED);
        } else if (game.getStatus() == CardGameStatus.PENDING) {
            game.setStatus(CardGameStatus.CANCELLED);
        }
        game.setLeftByUsername(me);
        game.setLastModifiedBy(me);
        CardGame saved = gameRepository.save(game);
        if (wasLive) {
            for (CardGamePlayer player : seatsOf(saved)) {
                if (player.isCpu() || me.equals(player.getUsername())) {
                    continue;
                }
                notificationService.notify(
                    player.getUsername(),
                    NotificationType.WARNING,
                    "70235 game ended",
                    nameOf(me) + " left the table. The game was ended.",
                    CARD_GAME_LINK
                );
            }
        }
        return toDto(saved, me);
    }

    // ----- Engine ---------------------------------------------------------------------------

    /**
     * Commit a validated play: the cards leave the hand, join the trick pile, and become the
     * play to beat. Shared by human plays, CPU plays and timeout auto-plays, so all three go
     * through identical bookkeeping.
     */
    private void applyPlay(CardGame game, CardGamePlayer seat, List<String> remainingHand, Play play, String actor) {
        seat.setHand(CardDeck.join(remainingHand));
        seat.setLastModifiedBy(actor);
        playerRepository.save(seat);
        // Record who went out first as it happens. Only meaningful once the deck is empty —
        // before that a refill would put cards straight back in the hand. Two players can
        // empty during the same trick, so claiming it at the moment of the play (rather than
        // when the trick is scored) keeps the leftover-points award unambiguous.
        if (remainingHand.isEmpty() && game.getFirstOutUsername() == null && CardDeck.parse(game.getDeck()).isEmpty()) {
            game.setFirstOutUsername(seat.getUsername());
        }
        game.setCurrentPlay(CardDeck.join(play.cards()));
        game.setCurrentPlayUsername(seat.getUsername());
        game.setCurrentShape(play.shape());
        game.setCurrentRank(play.rank());
        game.setTrickPile(game.getTrickPile() + CardDeck.join(play.cards()));
        appendTrickLog(game, seat, CardDeck.join(play.cards()));
        // A raise puts a new question to the table, so who declined the old play no longer
        // means anything — everybody is live again, including whoever just passed.
        game.setPassedUsernames("");
        game.setLastActionDate(Instant.now());
        game.setLastModifiedBy(actor);
        advanceTurn(game);
    }

    /**
     * Commit a pass: the player declines the play currently on the table and keeps their cards.
     *
     * <p>Only the play on the table is declined, not the rest of the trick. Should somebody
     * behind them raise it, the turn comes back round and they may answer the new play — see
     * {@link #advanceTurn}.
     */
    private void applyPass(CardGame game, CardGamePlayer seat) {
        String username = seat.getUsername();
        Set<String> passed = passedSet(game);
        passed.add(username);
        game.setPassedUsernames(String.join(",", passed));
        appendTrickLog(game, seat, "-");
        game.setLastActionDate(Instant.now());
        game.setLastModifiedBy(username);
        advanceTurn(game);
    }

    /**
     * Hand the turn to the next seat, in seat order. When the search comes back round to the
     * player holding the current play, everyone else has declined to raise it, so the trick is
     * theirs and {@link #resolveTrick} takes over.
     *
     * <p><strong>A pass is not a withdrawal from the trick.</strong> Passing declines the play
     * that is on the table at that moment, nothing more. If somebody behind you raises it, the
     * question put to you has changed, and the turn comes back to you: pass on a 4, and when
     * the play is raised to an 8 and then a 9 you are asked again each time, and may pass again
     * or beat the 9. So the pass set is not consulted here — it is display only, and
     * {@link #applyPlay} clears it whenever a raise makes it stale.
     *
     * <p>This terminates: a raise must be strictly higher than the play it answers, so the rank
     * on the table only ever climbs, and a full circuit of passes back to the holder ends it.
     */
    private void advanceTurn(CardGame game) {
        List<CardGamePlayer> seats = seatsOf(game);
        if (seats.isEmpty()) {
            return;
        }
        int from = indexOfSeat(seats, game.getCurrentTurnUsername());
        for (int step = 1; step <= seats.size(); step++) {
            CardGamePlayer candidate = seats.get((from + step) % seats.size());
            if (candidate.getUsername().equals(game.getCurrentPlayUsername())) {
                break; // back to the holder of the best play — nobody raised it, trick over
            }
            if (CardDeck.parse(candidate.getHand()).isEmpty()) {
                continue; // no cards left to answer with
            }
            game.setCurrentTurnUsername(candidate.getUsername());
            return;
        }
        resolveTrick(game);
    }

    /**
     * Award the trick: its point cards go to the winner, who then leads the next one. Every
     * player then refills to five, the winner drawing first — draw order matters once the deck
     * is down to its last cards, so winning a trick also buys first claim on them.
     */
    private void resolveTrick(CardGame game) {
        List<CardGamePlayer> seats = seatsOf(game);
        String winner = game.getCurrentPlayUsername();
        if (winner == null) {
            return; // nothing was played; nothing to resolve
        }
        CardGamePlayer winnerSeat = seats.stream()
            .filter(p -> p.getUsername().equals(winner))
            .findFirst()
            .orElse(null);
        if (winnerSeat == null) {
            return;
        }
        int points = CardDeck.pointsOf(CardDeck.parse(game.getTrickPile()));
        winnerSeat.setScore(winnerSeat.getScore() + points);
        playerRepository.save(winnerSeat);
        LOG.debug("70235 game {} trick {} won by {} for {} points", game.getId(), game.getTrickNumber(), winner, points);

        // Refill order: the winner, then everyone else in seat order. Passing keeps your cards
        // for the trick but does not cost you the draw, so a player holding something unbeatable
        // is topped back up and stays in the game.
        List<CardGamePlayer> toRefill = new ArrayList<>();
        toRefill.add(winnerSeat);
        for (CardGamePlayer seat : seats) {
            if (!seat.getUsername().equals(winner)) {
                toRefill.add(seat);
            }
        }
        refill(game, toRefill);

        clearTrick(game);
        game.setCurrentTurnUsername(winner); // the trick winner leads the next one
        game.setTrickNumber(game.getTrickNumber() + 1);
        game.setLastActionDate(Instant.now());

        if (isOver(game) || game.getTrickNumber() > MAX_TRICKS) {
            finish(game);
        }
    }

    /** Draw each player back up to the target hand size, in the order given, while cards last. */
    private void refill(CardGame game, List<CardGamePlayer> players) {
        List<String> deck = CardDeck.parse(game.getDeck());
        for (CardGamePlayer player : players) {
            List<String> hand = CardDeck.parse(player.getHand());
            while (hand.size() < CardGame.TARGET_HAND_SIZE && !deck.isEmpty()) {
                hand.add(deck.remove(0));
            }
            player.setHand(CardDeck.join(hand));
            playerRepository.save(player);
        }
        game.setDeck(CardDeck.join(deck));
    }

    // ----- End of game ----------------------------------------------------------------------

    /** The game ends when the deck is empty and at least one player has no cards left. */
    private boolean isOver(CardGame game) {
        if (!CardDeck.parse(game.getDeck()).isEmpty()) {
            return false;
        }
        return seatsOf(game).stream().anyMatch(p -> CardDeck.parse(p.getHand()).isEmpty());
    }

    /**
     * Score and close the game. Every point card still held by the other players is awarded to
     * whoever emptied their hand first, then the highest total wins.
     *
     * <p>Emptying first is therefore a large scoring bonus rather than an outright win. A tie
     * on points goes to the player who went out first, which is the advantage the rules
     * already privilege.
     */
    private void finish(CardGame game) {
        List<CardGamePlayer> seats = seatsOf(game);
        final String outUser = resolveFirstOut(game, seats);
        game.setFirstOutUsername(outUser);

        if (outUser != null) {
            int leftover = seats.stream()
                .filter(seat -> !seat.getUsername().equals(outUser))
                .mapToInt(seat -> CardDeck.pointsOf(CardDeck.parse(seat.getHand())))
                .sum();
            for (CardGamePlayer seat : seats) {
                if (seat.getUsername().equals(outUser)) {
                    seat.setScore(seat.getScore() + leftover);
                    playerRepository.save(seat);
                }
            }
            LOG.debug("70235 game {} ended; {} went out first and collected {} leftover points", game.getId(), outUser, leftover);
        }

        // Highest score wins; the player who went out first breaks a tie.
        game.setWinnerUsername(
            seats.stream()
                .max((a, b) -> {
                    int byScore = Integer.compare(a.getScore(), b.getScore());
                    if (byScore != 0) {
                        return byScore;
                    }
                    int aOut = a.getUsername().equals(outUser) ? 1 : 0;
                    int bOut = b.getUsername().equals(outUser) ? 1 : 0;
                    return Integer.compare(aOut, bOut);
                })
                .map(CardGamePlayer::getUsername)
                .orElse(null)
        );
        game.setStatus(CardGameStatus.FINISHED);
        game.setCurrentTurnUsername(null);
    }

    /**
     * Who emptied their hand first. Normally already recorded by {@link #applyPlay} as it
     * happened; the fallbacks cover a game closed by the round cap, where the trick winner —
     * who drew first and leads next — takes precedence, then seat order.
     */
    private String resolveFirstOut(CardGame game, List<CardGamePlayer> seats) {
        if (game.getFirstOutUsername() != null) {
            return game.getFirstOutUsername();
        }
        List<String> empty = seats.stream()
            .filter(seat -> CardDeck.parse(seat.getHand()).isEmpty())
            .map(CardGamePlayer::getUsername)
            .toList();
        if (empty.isEmpty()) {
            return null; // round cap with every hand still holding cards
        }
        String turn = game.getCurrentTurnUsername();
        return empty.contains(turn) ? turn : empty.get(0);
    }

    // ----- CPU turns and timeouts (lazy, resolved on every read and action) -----------------

    /**
     * Bring the table up to date: resolve any lapsed deadline, then take every turn the server
     * is responsible for — CPU seats and seats on auto-play. Called on every read and action
     * path, so a response is never built from a stale table. Returns the same instance for
     * chaining.
     */
    private CardGame advance(CardGame game) {
        CardGame resolved = resolveTimeouts(game);
        return runAutomaticTurns(resolved);
    }

    /**
     * Play out consecutive CPU turns until a human is due or the game ends. There is no
     * scheduler in this application, so CPUs move when somebody looks at the table — which,
     * with the client polling, is about once a second.
     */
    private CardGame runAutomaticTurns(CardGame game) {
        int steps = 0;
        boolean moved = false;
        while (game.getStatus() == CardGameStatus.ACTIVE && steps++ < MAX_AUTO_STEPS) {
            String turn = game.getCurrentTurnUsername();
            if (turn == null) {
                break;
            }
            CardGamePlayer seat = seatsOf(game).stream()
                .filter(p -> p.getUsername().equals(turn))
                .findFirst()
                .orElse(null);
            if (seat == null || !isAutomatic(seat)) {
                break; // a human who is playing their own turns is due
            }
            act(game, seat, seat.getUsername());
            moved = true;
        }
        if (steps >= MAX_AUTO_STEPS) {
            LOG.warn("70235 game {} hit the automatic step cap; stopping to avoid a loop", game.getId());
        }
        return moved ? gameRepository.save(game) : game;
    }

    /**
     * Whether this seat's turns are taken by the server: a CPU opponent, or a human who has
     * switched auto-play on. Both go through {@link #act}, so a seat on auto-play plays exactly
     * as a CPU in that position would.
     */
    private boolean isAutomatic(CardGamePlayer seat) {
        return seat.isCpu() || seat.isAutoPlay();
    }

    /**
     * Take one turn on a player's behalf — the CPU brain, also used when a human's turn clock
     * runs out. Beats the current play if it can, otherwise passes; when leading it must play,
     * so it leads its cheapest bulk shape.
     */
    private void act(CardGame game, CardGamePlayer seat, String actor) {
        List<String> hand = CardDeck.parse(seat.getHand());
        if (game.getCurrentPlay() == null) {
            Optional<Play> lead = CardDeck.chooseLead(hand);
            if (lead.isPresent()) {
                List<String> remaining = new ArrayList<>(hand);
                removeCards(remaining, lead.get().cards());
                applyPlay(game, seat, remaining, lead.get(), actor);
                return;
            }
            // Leading with an empty hand should be unreachable: a player with no cards is
            // skipped by advanceTurn. Guard anyway so a bug cannot wedge the table.
            LOG.warn("70235 game {}: {} had to lead with no playable hand", game.getId(), seat.getUsername());
            resolveTrick(game);
            return;
        }
        Optional<Play> beat = CardDeck.chooseBeat(hand, game.getCurrentShape(), game.getCurrentRank());
        if (beat.isPresent()) {
            List<String> remaining = new ArrayList<>(hand);
            removeCards(remaining, beat.get().cards());
            applyPlay(game, seat, remaining, beat.get(), actor);
        } else {
            applyPass(game, seat);
        }
    }

    /**
     * Transition a table to its terminal state if a deadline has passed, or take an idle
     * player's turn for them. A no-op (and no write) when nothing has lapsed, so it is safe
     * on every read path. {@code @Version} guards concurrent resolutions.
     */
    private CardGame resolveTimeouts(CardGame game) {
        Instant now = Instant.now();
        if (game.getStatus() == CardGameStatus.PENDING) {
            if (game.getCreatedDate() != null && elapsedSeconds(game.getCreatedDate(), now) >= inviteTimeoutSeconds) {
                game.setStatus(CardGameStatus.EXPIRED);
                return gameRepository.save(game);
            }
            return game;
        }
        if (game.getStatus() != CardGameStatus.ACTIVE) {
            return game;
        }
        if (game.getStartedDate() != null && elapsedSeconds(game.getStartedDate(), now) >= gameTimeoutSeconds) {
            // Round cap. There are real scores on the table, so score it as it stands rather
            // than discarding the game as a draw.
            finish(game);
            return gameRepository.save(game);
        }
        // An idle player does not get to freeze the table: take their turn with the same brain
        // the CPUs use. One turn per lapsed clock — act() restamps lastActionDate, so the next
        // player gets a full budget of their own, and a table where everyone has walked away
        // unwinds a turn at a time until the whole-game cap above closes it.
        if (game.getLastActionDate() == null || elapsedSeconds(game.getLastActionDate(), now) < turnTimeoutSeconds) {
            return game;
        }
        String turn = game.getCurrentTurnUsername();
        if (turn == null) {
            return game;
        }
        CardGamePlayer seat = seatsOf(game).stream()
            .filter(p -> p.getUsername().equals(turn))
            .findFirst()
            .orElse(null);
        if (seat == null) {
            return game;
        }
        LOG.debug("70235 game {}: {} timed out; auto-playing", game.getId(), turn);
        act(game, seat, turn);
        return gameRepository.save(game);
    }

    // ----- Helpers --------------------------------------------------------------------------

    /**
     * Remove each selected card from the hand by value, counting duplicates — with two packs a
     * hand can legitimately hold two identical cards. False (and the list left untouched) when
     * the hand does not contain them all.
     */
    private boolean removeCards(List<String> hand, List<String> selection) {
        List<String> working = new ArrayList<>(hand);
        for (String card : selection) {
            if (!working.remove(card)) {
                return false;
            }
        }
        hand.clear();
        hand.addAll(working);
        return true;
    }

    /** Seats at the table in seat order — the order turns and refill draws follow. */
    private List<CardGamePlayer> seatsOf(CardGame game) {
        return playerRepository.findByGameOrderBySeatAsc(game);
    }

    private CardGamePlayer seatOf(CardGame game, String username) {
        return playerRepository
            .findByGameAndUsername(game, username)
            .orElseThrow(() -> new BadRequestAlertException("You are not at this table", ENTITY_NAME, "notparticipant"));
    }

    private int indexOfSeat(List<CardGamePlayer> seats, String username) {
        for (int i = 0; i < seats.size(); i++) {
            if (seats.get(i).getUsername().equals(username)) {
                return i;
            }
        }
        return 0;
    }

    private Set<String> passedSet(CardGame game) {
        String raw = game.getPassedUsernames();
        if (raw == null || raw.isBlank()) {
            return new LinkedHashSet<>();
        }
        return Arrays.stream(raw.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private CardGame requirePending(Long id) {
        CardGame game = gameRepository.findById(id)
            .orElseThrow(() -> new BadRequestAlertException("Game not found", ENTITY_NAME, "notfound"));
        if (resolveTimeouts(game).getStatus() != CardGameStatus.PENDING) {
            throw new BadRequestAlertException("This table is no longer open", ENTITY_NAME, "notpending");
        }
        return game;
    }

    private CardGame requireParticipant(Long id, String me) {
        CardGame game = gameRepository.findById(id)
            .orElseThrow(() -> new BadRequestAlertException("Game not found", ENTITY_NAME, "notfound"));
        if (playerRepository.findByGameAndUsername(game, me).isEmpty()) {
            throw new BadRequestAlertException("You are not at this table", ENTITY_NAME, "notparticipant");
        }
        return game;
    }

    /** The seat of an invited human who has not yet answered — the only one who may respond. */
    /**
     * The caller's seat, for answering an invitation. A seat that has already been answered can
     * still be declined — that is a guest leaving a table that has not started yet — but it
     * cannot be accepted twice, so {@code acceptOnly} tightens the check for that path.
     */
    private CardGamePlayer requireInvitee(CardGame game, String me, boolean acceptOnly) {
        CardGamePlayer seat = seatOf(game, me);
        if (me.equals(game.getHostUsername())) {
            throw new BadRequestAlertException("The host does not respond to their own invitation", ENTITY_NAME, "nothost");
        }
        if (seat.getInvitationStatus() == CardGameInvitationStatus.DECLINED) {
            throw new BadRequestAlertException("You have already declined this invitation", ENTITY_NAME, "alreadyanswered");
        }
        if (acceptOnly && seat.getInvitationStatus() != CardGameInvitationStatus.PENDING) {
            throw new BadRequestAlertException("You have already answered this invitation", ENTITY_NAME, "alreadyanswered");
        }
        return seat;
    }

    /** The caller's live table (pending or active), resolving timeouts so a lapsed one is ignored. */
    private Optional<CardGame> currentLiveGame(String username) {
        return gameRepository.findByParticipantAndStatuses(username, LIVE).stream()
            .map(this::resolveTimeouts)
            .filter(g -> LIVE.contains(g.getStatus()))
            .findFirst();
    }

    private Optional<CardGame> currentActiveGame(String username) {
        return gameRepository.findActiveForUser(username)
            .map(this::resolveTimeouts)
            .filter(g -> g.getStatus() == CardGameStatus.ACTIVE);
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

    /** Label for a CPU seat: {@code cpu-2} reads as "CPU 2". */
    private String cpuName(String username) {
        return "CPU " + username.substring(CPU_PREFIX.length());
    }

    // ----- Per-recipient DTO ----------------------------------------------------------------

    /**
     * Build the view of a table for one participant. {@code me} receives their own cards and
     * nothing but counts for everyone else — {@link CardGamePlayerDTO} has no hand field, so
     * there is no path by which another player's cards could be serialised.
     */
    private CardGameDTO toDto(CardGame game, String me) {
        List<CardGamePlayer> seats = seatsOf(game);
        Set<String> humanLogins = seats.stream()
            .filter(p -> !p.isCpu())
            .map(CardGamePlayer::getUsername)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<String, User> byLogin = usersByLogin(humanLogins);
        Set<String> passed = passedSet(game);
        Instant now = Instant.now();
        boolean active = game.getStatus() == CardGameStatus.ACTIVE;
        boolean pending = game.getStatus() == CardGameStatus.PENDING;

        List<CardGamePlayerDTO> players = seats.stream()
            .map(seat -> {
                User user = byLogin.get(seat.getUsername());
                String name = seat.isCpu()
                    ? cpuName(seat.getUsername())
                    : (user == null ? seat.getUsername() : displayName(user));
                int handCount = CardDeck.parse(seat.getHand()).size();
                return new CardGamePlayerDTO(
                    seat.getUsername(),
                    name,
                    user == null ? null : user.getAvatar(),
                    seat.getSeat(),
                    seat.isCpu(),
                    seat.isAutoPlay(),
                    seat.getInvitationStatus().name(),
                    handCount,
                    seat.getScore(),
                    passed.contains(seat.getUsername()),
                    handCount == 0 && active
                );
            })
            .toList();

        List<String> myHand = seats.stream()
            .filter(seat -> seat.getUsername().equals(me))
            .findFirst()
            .map(seat -> CardDeck.sortByStrength(CardDeck.parse(seat.getHand())))
            .orElseGet(List::of);

        boolean isHost = me.equals(game.getHostUsername());
        boolean canStart =
            pending &&
            isHost &&
            seats.stream().noneMatch(p -> p.getInvitationStatus() == CardGameInvitationStatus.PENDING) &&
            acceptedCount(seats) >= CardGame.MIN_PLAYERS &&
            acceptedCount(seats) <= CardGame.MAX_PLAYERS;

        User host = byLogin.get(game.getHostUsername());
        return new CardGameDTO(
            game.getId(),
            game.getStatus().name(),
            game.getHostUsername(),
            host == null ? game.getHostUsername() : displayName(host),
            CardDeck.parse(game.getDeck()).size(),
            game.getTrickNumber(),
            game.getCurrentShape(),
            game.getCurrentPlay() == null ? null : CardDeck.parse(game.getCurrentPlay()),
            game.getCurrentPlayUsername(),
            playNameOf(game.getCurrentPlayUsername(), byLogin),
            CardDeck.pointsOf(CardDeck.parse(game.getTrickPile())),
            trickHistoryOf(game, seats, byLogin),
            game.getTrickLogNumber(),
            game.getCurrentTurnUsername(),
            players,
            myHand,
            me,
            isHost,
            active && me.equals(game.getCurrentTurnUsername()),
            canStart,
            game.getFirstOutUsername(),
            game.getWinnerUsername(),
            game.getLeftByUsername(),
            game.getCreatedDate(),
            (int) turnTimeoutSeconds,
            active ? secondsRemaining(game.getLastActionDate(), turnTimeoutSeconds, now) : null,
            active ? secondsRemaining(game.getStartedDate(), gameTimeoutSeconds, now) : null,
            pending ? secondsRemaining(game.getCreatedDate(), inviteTimeoutSeconds, now) : null
        );
    }

    private long acceptedCount(List<CardGamePlayer> seats) {
        return seats.stream().filter(p -> p.getInvitationStatus() == CardGameInvitationStatus.ACCEPTED).count();
    }

    /** Display name for whoever holds the current play, CPU seats included. */
    private String playNameOf(String username, Map<String, User> byLogin) {
        if (username == null) {
            return null;
        }
        if (username.startsWith(CPU_PREFIX)) {
            return cpuName(username);
        }
        User user = byLogin.get(username);
        return user == null ? username : displayName(user);
    }

    /**
     * Decode the stored trick log into attributed entries, oldest first. Unparseable entries are
     * skipped rather than failing the response: the log is a display aid, and a game in progress
     * is worth more than a complete history of it.
     */
    private List<CardGameTrickEntryDTO> trickHistoryOf(CardGame game, List<CardGamePlayer> seats, Map<String, User> byLogin) {
        String log = game.getTrickLog();
        if (log == null || log.isBlank()) {
            return List.of();
        }
        List<CardGameTrickEntryDTO> entries = new ArrayList<>();
        for (String part : log.split("\\|")) {
            int colon = part.indexOf(':');
            if (colon <= 0) {
                continue;
            }
            int seatIndex;
            try {
                seatIndex = Integer.parseInt(part.substring(0, colon));
            } catch (NumberFormatException e) {
                continue;
            }
            CardGamePlayer seat = seats.stream().filter(p -> p.getSeat() == seatIndex).findFirst().orElse(null);
            if (seat == null) {
                continue; // a seat that is no longer at the table
            }
            String cards = part.substring(colon + 1);
            boolean passed = "-".equals(cards);
            List<String> played = passed ? List.of() : CardDeck.parse(cards);
            entries.add(
                new CardGameTrickEntryDTO(
                    seat.getUsername(),
                    playNameOf(seat.getUsername(), byLogin),
                    seatIndex,
                    played,
                    passed,
                    passed ? null : CardDeck.detectShape(played).map(Play::shape).orElse(null)
                )
            );
        }
        return entries;
    }
}
