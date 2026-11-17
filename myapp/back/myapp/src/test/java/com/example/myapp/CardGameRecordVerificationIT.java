package com.example.myapp;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.myapp.contants.enumeration.CardGameInvitationStatus;
import com.example.myapp.contants.enumeration.CardGameRecordOutcome;
import com.example.myapp.contants.enumeration.CardGameStatus;
import com.example.myapp.domain.CardGame;
import com.example.myapp.domain.CardGamePlayer;
import com.example.myapp.domain.CardGameRecord;
import com.example.myapp.domain.CardGameRecordPlayer;
import com.example.myapp.domain.User;
import com.example.myapp.repository.CardGamePlayerRepository;
import com.example.myapp.repository.CardGameRecordPlayerRepository;
import com.example.myapp.repository.CardGameRecordRepository;
import com.example.myapp.repository.CardGameRepository;
import com.example.myapp.repository.UserRepository;
import com.example.myapp.service.CardGameRecordService;
import com.example.myapp.service.CardGameService;
import com.example.myapp.service.dto.CardGameRecordDTO;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

/**
 * Covers the match history behind the administrators' 70235 record list.
 *
 * <p>The premise worth testing is that the history cannot be read off {@code twsny_card_game}:
 * that row is the <em>room</em>, and a rematch re-deals it in place — scores back to 0,
 * {@code startedDate} re-stamped, winner cleared. So the tests that matter are the ones about
 * <em>how many</em> records exist after a sequence of games, not just what one record contains.
 * {@link #reDealingWritesASecondRecordRatherThanOverwritingTheFirst} is the case the schema
 * loses today, and {@link #leavingARematchRoomDoesNotRecordTheMatchTwice} is the one a naive
 * write-on-every-terminal-status would get wrong.
 *
 * <p>Runs on the dev profile's in-memory H2 with {@code ddl-auto: create-drop}, so it needs no
 * external database.
 */
@SpringBootTest
class CardGameRecordVerificationIT {

    @Autowired
    private CardGameService cardGameService;

    @Autowired
    private CardGameRecordService cardGameRecordService;

    @Autowired
    private CardGameRepository gameRepository;

    @Autowired
    private CardGamePlayerRepository playerRepository;

    @Autowired
    private CardGameRecordRepository recordRepository;

    @Autowired
    private CardGameRecordPlayerRepository recordPlayerRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    private void loginAs(String username) {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(username, null, List.of())
        );
    }

    /** A saved user, so a record's creator resolves to a display name rather than a login. */
    private void user(String login, String realName) {
        userRepository.save(new User()
            .login(login)
            .realName(realName)
            .nickName(login)
            .email(login + "@example.com")
            .password("not-used-here")
            .deleted(false));
    }

    // ----- Rigging -----------------------------------------------------------------------------

    /**
     * Seat a host and one CPU opponent directly through the repositories, as
     * {@code CardGameResultVerificationIT} does: no service path seats a CPU any more, and
     * every field {@code createTable} would set is overwritten by {@link #rigForEndgame}.
     */
    private CardGame riggedSeats(String host) {
        CardGame game = new CardGame();
        game.setStatus(CardGameStatus.ACTIVE);
        game.setHostUsername(host);
        game.setCreatedBy(host);
        game.setLastModifiedBy(host);
        game.setTrickNumber(1); // where a real deal starts it; the entity default of 0 is a state no dealt game is in
        CardGame saved = gameRepository.save(game);

        addSeat(saved, host, 0, false);
        addSeat(saved, "cpu-1", 1, true);
        return saved;
    }

    private void addSeat(CardGame game, String username, int index, boolean cpu) {
        CardGamePlayer player = new CardGamePlayer();
        player.setGame(game);
        player.setUsername(username);
        player.setSeat(index);
        player.setCpu(cpu);
        player.setInvitationStatus(CardGameInvitationStatus.ACCEPTED);
        player.setCreatedBy(game.getHostUsername());
        player.setLastModifiedBy(game.getHostUsername());
        playerRepository.save(player);
    }

    /**
     * Put a dealt game one play away from ending: the deck is drained so nobody refills, the
     * host holds a single unbeatable card, and it is their turn.
     *
     * @param startedAt the deal's start time. A parameter because {@code (gameId, startedDate)}
     *                  is a record's identity, so two deals of the same room must be rigged with
     *                  two distinct values for the second record to be a second record. Use
     *                  {@link #minutesAgo} to build one at a precision a column can hold.
     */
    private CardGame rigForEndgame(CardGame game, String host, Instant startedAt) {
        for (CardGamePlayer seat : playerRepository.findByGameOrderBySeatAsc(game)) {
            // The host's 7 is the strongest card in the game, so playing it is legal whatever is
            // on the table and empties their hand. The CPU's King is worth 10 points, so the
            // leftover collected at scoring is non-zero and the scores are observably different.
            seat.setHand(seat.getUsername().equals(host) ? "7S" : "KH");
            playerRepository.save(seat);
        }
        game.setDeck("");
        game.setStatus(CardGameStatus.ACTIVE);
        game.setStartedDate(startedAt);
        game.setLastActionDate(Instant.now());
        game.setCurrentTurnUsername(host);
        game.setCurrentPlay(null);
        game.setCurrentPlayUsername(null);
        game.setCurrentShape(null);
        game.setCurrentRank(null);
        game.setFirstOutUsername(null);
        game.setWinnerUsername(null);
        game.setTrickPile("");
        game.setPassedUsernames("");
        game.setTrickLog("");
        return gameRepository.save(game);
    }

    /**
     * A timestamp n minutes back, truncated to milliseconds.
     *
     * <p>The truncation is what lets a test assert equality on a timestamp at all: a timestamp
     * column holds microseconds at best (H2 here, {@code DATETIME(6)} on MySQL), so an
     * {@code Instant} carrying nanoseconds does not read back equal to what was written.
     */
    private Instant minutesAgo(int minutes) {
        return Instant.now().minus(minutes, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.MILLIS);
    }

    private List<CardGameRecord> recordsOf(Long gameId) {
        return recordRepository.findAll().stream()
            .filter(r -> gameId.equals(r.getGameId()))
            .sorted(Comparator.comparing(CardGameRecord::getStartedDate))
            .toList();
    }

    private List<CardGameRecordPlayer> seatsOf(CardGameRecord record) {
        return recordPlayerRepository.findByRecordIdInOrderBySeatAsc(List.of(record.getId()));
    }

    // ----- A game that was played to the end ---------------------------------------------------

    /**
     * A scored game is recorded once, with everything the list needs column by column: both ends
     * of the match, the creator, and a score for every seat.
     */
    @Test
    void aScoredGameIsRecordedOnceWithBothTimestampsAndEverySeatsScore() {
        user("rec-scored", "Scored Host");
        loginAs("rec-scored");
        CardGame game = riggedSeats("rec-scored");
        Instant startedAt = minutesAgo(7);
        rigForEndgame(game, "rec-scored", startedAt);
        Long id = game.getId();

        Instant beforePlay = Instant.now();
        cardGameService.play(id, List.of("7S"));

        List<CardGameRecord> written = recordsOf(id);
        assertThat(written).as("a game played to its end is recorded, exactly once").hasSize(1);

        CardGameRecord record = written.get(0);
        assertThat(record.getOutcome()).isEqualTo(CardGameRecordOutcome.SCORED);
        assertThat(record.getHostUsername()).as("the list shows who created the game").isEqualTo("rec-scored");
        assertThat(record.getStartedDate())
            .as("the recorded start is the deal's, not the moment it was recorded")
            .isEqualTo(startedAt);
        assertThat(record.getEndedDate())
            .as("and the end is when the last card was played")
            .isBetween(beforePlay, Instant.now());
        assertThat(record.getWinnerUsername())
            .as("a scored game has a winner, and it is the player who went out")
            .isEqualTo("rec-scored");
        assertThat(record.getPlayerCount()).isEqualTo(2);

        List<CardGameRecordPlayer> seats = seatsOf(record);
        assertThat(seats).as("every seat's score is recorded, which is the column asked for").hasSize(2);
        assertThat(seats).allSatisfy(seat -> assertThat(seat.getScore()).isGreaterThanOrEqualTo(0));

        CardGameRecordPlayer host = seats.stream()
            .filter(seat -> seat.getUsername().equals("rec-scored"))
            .findFirst()
            .orElseThrow();
        assertThat(host.getScore())
            .as("the recorded score is the final one: going out collected the King's 10 points")
            .isGreaterThanOrEqualTo(10);
        assertThat(host.isCpu()).isFalse();
        assertThat(seats).anySatisfy(seat -> assertThat(seat.isCpu()).as("the CPU seat is recorded as one").isTrue());
    }

    /**
     * The point of the whole table: a second game in the same room is a second record.
     *
     * <p>Re-dealing resets the seats' scores and re-stamps {@code startedDate} on the room, so
     * the first game's result is gone from {@code twsny_card_game} the moment the second is
     * dealt. If this fails, the list cannot show a room's history — which is the reason the
     * result could not simply be read off the existing rows.
     */
    @Test
    void reDealingWritesASecondRecordRatherThanOverwritingTheFirst() {
        user("rec-redeal", "Redeal Host");
        loginAs("rec-redeal");
        CardGame game = riggedSeats("rec-redeal");
        Long id = game.getId();

        Instant firstDeal = minutesAgo(30);
        rigForEndgame(game, "rec-redeal", firstDeal);
        cardGameService.play(id, List.of("7S"));
        assertThat(recordsOf(id)).as("the first game is recorded").hasSize(1);
        int firstScore = seatsOf(recordsOf(id).get(0)).stream()
            .filter(seat -> seat.getUsername().equals("rec-redeal"))
            .findFirst()
            .orElseThrow()
            .getScore();
        assertThat(firstScore).as("the first game scored something worth keeping").isGreaterThanOrEqualTo(10);

        // A second game in the same room, rigged the same way but dealt later.
        Instant secondDeal = minutesAgo(5);
        rigForEndgame(gameRepository.findById(id).orElseThrow(), "rec-redeal", secondDeal);
        cardGameService.play(id, List.of("7S"));

        List<CardGameRecord> written = recordsOf(id);
        assertThat(written).as("two games played in one room are two records").hasSize(2);
        assertThat(written).extracting(CardGameRecord::getStartedDate)
            .as("each record keeps its own deal's start time")
            .containsExactly(firstDeal, secondDeal);
        assertThat(written.get(0).getId())
            .as("the first record was not reused for the second game")
            .isNotEqualTo(written.get(1).getId());

        // And the first record's scores survived the re-deal that zeroed the live seats.
        assertThat(seatsOf(written.get(0)))
            .as("the first record still holds its own scores, whatever the room's seats now say")
            .anySatisfy(seat -> {
                assertThat(seat.getUsername()).isEqualTo("rec-redeal");
                assertThat(seat.getScore()).isEqualTo(firstScore);
            });
    }

    // ----- A game that was walked out of -------------------------------------------------------

    /**
     * Leaving a game in progress records it as abandoned: the scores as they stood, no winner,
     * and who walked out.
     */
    @Test
    void leavingAnActiveGameRecordsItAsAbandonedWithNoWinner() {
        user("rec-abandon", "Abandon Host");
        loginAs("rec-abandon");
        CardGame game = riggedSeats("rec-abandon");
        Instant startedAt = minutesAgo(3);
        rigForEndgame(game, "rec-abandon", startedAt);
        Long id = game.getId();

        Instant beforeLeave = Instant.now();
        cardGameService.leave(id);

        List<CardGameRecord> written = recordsOf(id);
        assertThat(written).as("a game that was being played is recorded even though nobody won").hasSize(1);

        CardGameRecord record = written.get(0);
        assertThat(record.getOutcome()).isEqualTo(CardGameRecordOutcome.ABANDONED);
        assertThat(record.getWinnerUsername())
            .as("scoring never ran, so there is no winner to record")
            .isNull();
        assertThat(record.getLeftByUsername())
            .as("who walked out is the explanation the list shows")
            .isEqualTo("rec-abandon");
        assertThat(record.getStartedDate()).isEqualTo(startedAt);
        assertThat(record.getEndedDate()).isBetween(beforeLeave, Instant.now());
        assertThat(seatsOf(record))
            .as("the standings as they stood are still recorded")
            .hasSize(2)
            .allSatisfy(seat -> assertThat(seat.getScore()).isGreaterThanOrEqualTo(0));
    }

    /**
     * The double-write guard. {@code leave()} sets ABANDONED from both ACTIVE and REMATCH, but a
     * REMATCH room's match was already recorded by {@code finish()}. Leaving the room after the
     * game was scored must not record it a second time.
     */
    @Test
    void leavingARematchRoomDoesNotRecordTheMatchTwice() {
        user("rec-rematch", "Rematch Host");
        loginAs("rec-rematch");
        CardGame game = riggedSeats("rec-rematch");
        rigForEndgame(game, "rec-rematch", minutesAgo(10));
        Long id = game.getId();

        cardGameService.play(id, List.of("7S"));
        List<CardGameRecord> afterScoring = recordsOf(id);
        assertThat(afterScoring).as("scoring recorded the match").hasSize(1);
        assertThat(gameRepository.findById(id).orElseThrow().getStatus())
            .as("and the room is between games, not finished")
            .isEqualTo(CardGameStatus.REMATCH);

        // Now walk out of the room. The room becomes ABANDONED, but no match was in progress.
        cardGameService.leave(id);

        List<CardGameRecord> afterLeaving = recordsOf(id);
        assertThat(afterLeaving)
            .as("leaving a scored room adds no second record for a match already recorded")
            .hasSize(1);
        assertThat(afterLeaving.get(0).getId()).isEqualTo(afterScoring.get(0).getId());
        assertThat(afterLeaving.get(0).getOutcome())
            .as("and does not rewrite the outcome of a game that was played to its end")
            .isEqualTo(CardGameRecordOutcome.SCORED);
        assertThat(afterLeaving.get(0).getLeftByUsername())
            .as("nor blame the match on somebody leaving the room afterwards")
            .isNull();
    }

    // ----- Rooms that were never a match -------------------------------------------------------

    /**
     * A table nobody ever dealt is not a match. Cancelling before the deal records nothing —
     * there are no scores, no start and no end to show.
     */
    @Test
    void aTableCancelledBeforeTheDealIsNotRecorded() {
        user("rec-cancel", "Cancel Host");
        loginAs("rec-cancel");

        CardGame game = new CardGame();
        game.setStatus(CardGameStatus.PENDING);
        game.setHostUsername("rec-cancel");
        game.setCreatedBy("rec-cancel");
        game.setLastModifiedBy("rec-cancel");
        CardGame saved = gameRepository.save(game);
        addSeat(saved, "rec-cancel", 0, false);
        addSeat(saved, "cpu-1", 1, true);

        cardGameService.cancel(saved.getId());

        assertThat(gameRepository.findById(saved.getId()).orElseThrow().getStatus())
            .isEqualTo(CardGameStatus.CANCELLED);
        assertThat(recordsOf(saved.getId()))
            .as("a room that never dealt has no match to record")
            .isEmpty();
    }

    /**
     * Likewise an invitation nobody answered. The table lapses to EXPIRED on the next read, and
     * that is not a played game either.
     *
     * <p>{@code created_date} is {@code updatable = false}, so the table is aged with a native
     * update rather than a save — there is no other way to make the invite window lapse without
     * waiting out the real five minutes.
     */
    @Test
    @Transactional
    void anInvitationThatExpiredIsNotRecorded() {
        user("rec-expire", "Expire Host");
        loginAs("rec-expire");

        CardGame game = new CardGame();
        game.setStatus(CardGameStatus.PENDING);
        game.setHostUsername("rec-expire");
        game.setCreatedBy("rec-expire");
        game.setLastModifiedBy("rec-expire");
        CardGame saved = gameRepository.save(game);
        addSeat(saved, "rec-expire", 0, false);
        addSeat(saved, "cpu-1", 1, true);
        Long id = saved.getId();

        entityManager.flush();
        entityManager.createNativeQuery("update twsny_card_game set created_date = ?1 where id = ?2")
            .setParameter(1, Instant.now().minus(2, ChronoUnit.HOURS))
            .setParameter(2, id)
            .executeUpdate();
        entityManager.clear();

        cardGameService.getGame(id); // any read path resolves the lapsed invite

        assertThat(gameRepository.findById(id).orElseThrow().getStatus())
            .as("the unanswered invitation lapsed")
            .isEqualTo(CardGameStatus.EXPIRED);
        assertThat(recordsOf(id))
            .as("an invitation nobody answered was never a match")
            .isEmpty();
    }

    // ----- What the administrator's list returns -----------------------------------------------

    /**
     * The read side: a recorded match comes back through {@code listForAdmin} with the creator's
     * display name resolved, the CPU seat labelled, and the standings in score order.
     */
    @Test
    void theAdminListReturnsResolvedNamesAndStandingsInScoreOrder() {
        user("rec-list", "Listed Host");
        loginAs("rec-list");
        CardGame game = riggedSeats("rec-list");
        rigForEndgame(game, "rec-list", minutesAgo(12));
        cardGameService.play(game.getId(), List.of("7S"));

        var page = cardGameRecordService.listForAdmin(
            "rec-list",
            null,
            PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "endedDate"))
        );
        assertThat(page.getContent()).as("the recorded match is listed").hasSize(1);

        CardGameRecordDTO dto = page.getContent().get(0);
        assertThat(dto.hostName())
            .as("the list shows the creator's name, not their login")
            .isEqualTo("Listed Host");
        assertThat(dto.outcome()).isEqualTo("SCORED");
        assertThat(dto.startedDate()).isNotNull();
        assertThat(dto.endedDate()).as("a match ends after it starts").isAfter(dto.startedDate());

        assertThat(dto.players()).hasSize(2);
        assertThat(dto.players().get(0).score())
            .as("standings come back highest first, so the client renders them in order")
            .isGreaterThanOrEqualTo(dto.players().get(1).score());
        assertThat(dto.players().get(0).winner()).as("and the top seat is marked as the winner").isTrue();
        assertThat(dto.players())
            .as("a CPU seat is labelled rather than shown as cpu-1")
            .anySatisfy(seat -> {
                assertThat(seat.cpu()).isTrue();
                assertThat(seat.name()).isEqualTo("CPU 1");
            });

        // The outcome filter narrows to the same row, and excludes it when it does not match.
        assertThat(cardGameRecordService
            .listForAdmin("rec-list", "ABANDONED", PageRequest.of(0, 20))
            .getContent())
            .as("filtering by the other outcome excludes a scored match")
            .isEmpty();
        assertThat(cardGameRecordService
            .listForAdmin("rec-list", "SCORED", PageRequest.of(0, 20))
            .getContent())
            .as("and filtering by its own outcome keeps it")
            .hasSize(1);
    }

    /** Searching by a player who merely held a seat finds the match, not just the creator's. */
    @Test
    void theAdminListFindsAMatchByAnyPlayerNotJustTheCreator() {
        user("rec-search-host", "Search Host");
        loginAs("rec-search-host");
        CardGame game = riggedSeats("rec-search-host");
        rigForEndgame(game, "rec-search-host", minutesAgo(9));
        cardGameService.play(game.getId(), List.of("7S"));

        assertThat(cardGameRecordService.listForAdmin("cpu-1", null, PageRequest.of(0, 50)).getContent())
            .as("a search for a seated player finds the match they played in")
            .anySatisfy(dto -> assertThat(dto.hostUsername()).isEqualTo("rec-search-host"));
        assertThat(cardGameRecordService.listForAdmin("nobody-by-this-name", null, PageRequest.of(0, 50)).getContent())
            .as("and a search matching nobody finds nothing")
            .isEmpty();
    }
}
