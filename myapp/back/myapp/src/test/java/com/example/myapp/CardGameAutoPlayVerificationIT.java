package com.example.myapp;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.myapp.contants.enumeration.CardGameInvitationStatus;
import com.example.myapp.contants.enumeration.CardGameStatus;
import com.example.myapp.domain.CardGame;
import com.example.myapp.domain.CardGamePlayer;
import com.example.myapp.repository.CardGamePlayerRepository;
import com.example.myapp.repository.CardGameRepository;
import com.example.myapp.service.CardGameService;
import com.example.myapp.service.dto.CardGameDTO;
import com.example.myapp.service.dto.CardGamePlayerDTO;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Auto-play: a human seat whose turns the server takes.
 *
 * <p>What separates this from the turn-timeout path that already existed is <em>when</em> the
 * turn is taken. A timeout only fires once the clock has lapsed; an auto-play seat acts on the
 * next read of the table with the clock barely started. Every test here keeps the turn clock
 * nowhere near its budget, so a turn being taken can only be auto-play and not a timeout.
 */
@SpringBootTest
class CardGameAutoPlayVerificationIT {

    @Autowired
    private CardGameService cardGameService;

    @Autowired
    private CardGameRepository gameRepository;

    @Autowired
    private CardGamePlayerRepository playerRepository;

    private void loginAs(String username) {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(username, null, List.of())
        );
    }

    private CardGame reload(Long id) {
        return gameRepository.findById(id).orElseThrow();
    }

    private List<CardGamePlayer> seats(CardGame game) {
        return playerRepository.findByGameOrderBySeatAsc(game);
    }

    /**
     * Seat a host and one or more CPU opponents straight through the repositories.
     *
     * <p>Built by hand rather than through {@code createTable} because no service path seats a
     * CPU any more — that feature was withdrawn, and dropping the parameter is what makes "no new
     * CPU seat" a property of the code rather than of its callers. The seats themselves are still
     * supported on purpose: {@code isCpu()} has to keep answering truthfully for a game that was
     * dealt while the feature was live, and these tests are the only thing that exercises that
     * path. They lose nothing by not going through the service: every field it would have set —
     * hands, deck, status, whose turn it is — is overwritten by the caller anyway.
     *
     * <p>{@code cpu-1} upward matches the prefix the service reads to label a CPU seat, so these
     * tables look to it exactly like the legacy ones it still has to serve.
     */
    private CardGame riggedSeats(String host, String hostHand, String... cpuHands) {
        CardGame game = new CardGame();
        game.setStatus(CardGameStatus.ACTIVE);
        game.setHostUsername(host);
        game.setCreatedBy(host);
        game.setLastModifiedBy(host);
        CardGame saved = gameRepository.save(game);

        addSeat(saved, host, 0, false, hostHand);
        for (int i = 0; i < cpuHands.length; i++) {
            addSeat(saved, "cpu-" + (i + 1), i + 1, true, cpuHands[i]);
        }
        return saved;
    }

    private void addSeat(CardGame game, String username, int index, boolean cpu, String hand) {
        CardGamePlayer player = new CardGamePlayer();
        player.setGame(game);
        player.setUsername(username);
        player.setSeat(index);
        player.setCpu(cpu);
        player.setHand(hand);
        player.setInvitationStatus(CardGameInvitationStatus.ACCEPTED);
        player.setCreatedBy(game.getHostUsername());
        player.setLastModifiedBy(game.getHostUsername());
        playerRepository.save(player);
    }

    /**
     * A two-seat table against one CPU, with the human due to act and a deck long enough that
     * refills cannot drain it — so the game stays off its end condition while turns are taken.
     *
     * <p>{@code lastActionDate} is stamped now on purpose: it is what the turn clock is measured
     * from, so every assertion below runs with the clock freshly started and a timeout out of
     * reach.
     */
    private CardGame riggedTable(String host, String hostHand, String cpuHand) {
        loginAs(host);
        CardGame game = riggedSeats(host, hostHand, cpuHand);

        game.setDeck("4H4D6H6D6C8H8D8C9H9D9C9S");
        game.setStatus(CardGameStatus.ACTIVE);
        game.setStartedDate(Instant.now());
        game.setLastActionDate(Instant.now());
        game.setCurrentTurnUsername(host);
        game.setCurrentPlay(null);
        game.setCurrentPlayUsername(null);
        game.setCurrentShape(null);
        game.setCurrentRank(null);
        game.setTrickPile("");
        game.setPassedUsernames("");
        game.setTrickLog("");
        return gameRepository.save(game);
    }

    private CardGamePlayerDTO seatOf(CardGameDTO dto, String username) {
        return dto.players().stream()
            .filter(p -> username.equals(p.username()))
            .findFirst()
            .orElseThrow();
    }

    /**
     * Set a seat's flag straight through the repository, bypassing the service.
     *
     * <p>Needed because {@link CardGameService#setAutoPlay} advances the table, and on these
     * two-seat tables that means the game runs to its end before the test can look at it — see
     * {@link #aTableWhereEverySeatIsAutomaticRunsToItsEndInOneCall}. Arming the flag this way
     * leaves the table exactly as rigged, so the next service call is the only thing that moves
     * it and the assertions are about that call alone.
     */
    private void arm(CardGame game, String username, boolean enabled) {
        CardGamePlayer seat = seats(game).stream()
            .filter(p -> username.equals(p.getUsername()))
            .findFirst()
            .orElseThrow();
        seat.setAutoPlay(enabled);
        playerRepository.save(seat);
    }

    /**
     * Switching auto-play on while the turn is already yours plays that turn in the same
     * response. Waiting for the next poll would leave the player looking at a table that has
     * not moved, having just asked for it to be played for them.
     *
     * <p>Only the first turn is asserted here. How far past it this particular table gets is the
     * subject of {@link #aTableWhereEverySeatIsAutomaticRunsToItsEndInOneCall}.
     */
    @Test
    void turningAutoPlayOnTakesTheTurnThatIsAlreadyDue() {
        CardGame game = riggedTable("autoplay-now", "8S9SJS4C4D", "4S6S");
        Long id = game.getId();

        loginAs("autoplay-now");
        CardGameDTO before = cardGameService.getGame(id);
        assertThat(before.myTurn()).as("the human is due before anything is switched on").isTrue();
        assertThat(seatOf(before, "autoplay-now").autoPlay()).isFalse();

        CardGameDTO after = cardGameService.setAutoPlay(id, true);

        assertThat(seatOf(after, "autoplay-now").autoPlay()).as("the flag is reported back").isTrue();
        assertThat(reload(id).getTrickLog())
            .as("the turn was taken in the same response, not left for the next poll")
            .isNotBlank();
    }

    /**
     * On a table whose only human switches auto-play on, every seat is automatic — and because
     * turns are resolved inside the request rather than by a scheduler, the game plays itself out
     * to a winner before that one call returns.
     *
     * <p>Worth pinning rather than leaving implicit: it is the reason a test cannot arm this flag
     * through the service and then inspect a half-played table, and the reason a player who
     * switches on against CPUs alone sees a finished game rather than a moving one.
     */
    @Test
    void aTableWhereEverySeatIsAutomaticRunsToItsEndInOneCall() {
        CardGame game = riggedTable("autoplay-solo", "8S9SJS4C4D", "4S6S");
        Long id = game.getId();

        loginAs("autoplay-solo");
        CardGameDTO after = cardGameService.setAutoPlay(id, true);

        assertThat(after.status())
            .as("no seat was left to wait for, so the call did not stop at one turn")
            .isEqualTo("FINISHED");
        assertThat(after.winnerUsername()).as("and a finished game names its winner").isNotNull();
    }

    /**
     * With auto-play on, a plain read of the table takes the human's turn. The clock is what
     * makes this worth asserting: two tables are rigged identically, including a freshly stamped
     * {@code lastActionDate}, and only one seat is armed. The armed table moves on a read and the
     * other does not, so the difference is the flag and cannot be a lapsed timeout.
     */
    @Test
    void anAutoPlaySeatActsOnAReadRatherThanWaitingForTheClock() {
        CardGame armed = riggedTable("autoplay-read", "8S9SJS4C4D", "4S6S");
        arm(armed, "autoplay-read", true);

        CardGame bare = riggedTable("autoplay-bare", "8S9SJS4C4D", "4S6S");

        loginAs("autoplay-read");
        CardGameDTO read = cardGameService.getGame(armed.getId());
        assertThat(read.myTurn())
            .as("the turn did not stay with a player who handed it over")
            .isFalse();
        assertThat(reload(armed.getId()).getTrickLog())
            .as("something was played on the caller's behalf, on a read")
            .isNotBlank();

        loginAs("autoplay-bare");
        CardGameDTO control = cardGameService.getGame(bare.getId());
        assertThat(control.myTurn())
            .as("the same table without the flag is untouched, so the clock explains neither")
            .isTrue();
        assertThat(reload(bare.getId()).getTrickLog()).isBlank();
        assertThat(control.turnSecondsRemaining())
            .as("and that control table's clock is nowhere near lapsing")
            .isGreaterThan(0);
    }

    /**
     * Switching it off returns control, including the turn you are switching it off on.
     *
     * <p>This is the one that makes the write order in {@code setAutoPlay} matter. The call
     * advances the table, so if the flag were still set while that happened the player's own
     * turn would be spent on the way out — they would ask for control back and watch a card go
     * down anyway. The flag is written first, so the turn survives the call.
     */
    @Test
    void turningAutoPlayOffReturnsControlToThePlayer() {
        CardGame game = riggedTable("autoplay-off", "8S9SJS4C4D", "4S6S");
        Long id = game.getId();
        arm(game, "autoplay-off", true);

        loginAs("autoplay-off");
        CardGameDTO off = cardGameService.setAutoPlay(id, false);

        assertThat(seatOf(off, "autoplay-off").autoPlay()).as("the flag is off").isFalse();
        assertThat(off.myTurn()).as("and the turn being switched off on is still the player's").isTrue();
        assertThat(reload(id).getTrickLog()).as("nothing was played for them on the way out").isBlank();
        assertThat(cardGameService.getGame(id).myTurn()).as("a later read does not take it either").isTrue();
    }

    /**
     * The flag belongs to one seat. A table where one player has handed over their turns is
     * not a table where everybody has, and the response says which is which — the client shows
     * an "Auto" marker per seat from exactly this.
     */
    @Test
    void autoPlayIsPerSeatAndReportedForEverySeat() {
        CardGame game = riggedTable("autoplay-seat", "8S9SJS4C4D", "4S6S");
        Long id = game.getId();
        String cpu = seats(game).get(1).getUsername();

        loginAs("autoplay-seat");
        CardGameDTO dto = cardGameService.setAutoPlay(id, true);

        assertThat(seatOf(dto, "autoplay-seat").autoPlay()).as("mine is on").isTrue();
        assertThat(seatOf(dto, cpu).autoPlay())
            .as("the CPU seat's own flag is untouched — it plays itself through isCpu, not this")
            .isFalse();
        assertThat(seatOf(dto, cpu).cpu()).isTrue();
    }
}
