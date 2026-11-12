package com.example.myapp;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.myapp.contants.enumeration.CardGameInvitationStatus;
import com.example.myapp.contants.enumeration.CardGameStatus;
import com.example.myapp.domain.CardGame;
import com.example.myapp.domain.CardGamePlayer;
import com.example.myapp.repository.CardGamePlayerRepository;
import com.example.myapp.repository.CardGameRepository;
import com.example.myapp.service.CardDeck;
import com.example.myapp.service.CardGameService;
import com.example.myapp.service.dto.CardGameDTO;
import com.example.myapp.service.dto.CardGameTrickEntryDTO;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Pins the bidding order within a trick: a trick is not won by the first answer to it. Every
 * other player keeps getting the turn, in seat order, for as long as they can raise, so a lead
 * of 8 can be met with a 9, then a 10, then a J. The trick belongs to the last player left
 * holding the top play once everyone else has passed.
 *
 * <p>The player who led is part of that cycle: having already played does not end their
 * involvement, and they get another turn when the play comes back round to them raised.
 */
@SpringBootTest
class CardGameTurnOrderVerificationIT {

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

    /** Seats in seat order — the turn order the rules follow. */
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
     * A three-seat table (host plus two CPUs) with hands and deck set by hand, so the trick
     * escalates identically every run. The deck is left long enough that no refill can empty
     * it, keeping the game off its end condition while the trick is examined.
     */
    private CardGame riggedThreeSeat(String host, String hostHand, String cpuOneHand, String cpuTwoHand) {
        loginAs(host);
        CardGame game = riggedSeats(host, hostHand, cpuOneHand, cpuTwoHand);

        game.setDeck("4H4D6H6D6C8H8D8C9H9D9C9S");
        game.setStatus(CardGameStatus.ACTIVE);
        game.setCurrentTurnUsername(host);
        game.setCurrentPlay(null);
        game.setCurrentPlayUsername(null);
        game.setCurrentShape(null);
        game.setCurrentRank(null);
        game.setTrickPile("");
        game.setPassedUsernames("");
        return gameRepository.save(game);
    }

    @Test
    void aLeadIsRaisedAroundTheTableRatherThanWonByTheFirstAnswer() {
        // Host leads the 8. The first CPU holds the only 9, the second the only 10, so each can
        // raise exactly once and neither can answer the J the host still holds.
        CardGame game = riggedThreeSeat("turnorder-raise", "8SJS4C4D4H", "9S4S", "TS6S");
        Long id = game.getId();
        List<CardGamePlayer> seats = seats(game);
        String cpuOne = seats.get(1).getUsername();
        String cpuTwo = seats.get(2).getUsername();

        loginAs("turnorder-raise");
        cardGameService.play(id, List.of("8S"));

        // Both CPUs answered in seat order while the service advanced the table, each raising
        // the previous play instead of the trick stopping at the first answer.
        CardGame afterRaises = reload(id);
        assertThat(afterRaises.getCurrentPlay()).as("the 8 was raised to a 9 and then a 10").isEqualTo("TS");
        assertThat(afterRaises.getCurrentPlayUsername()).as("the second CPU holds the top play").isEqualTo(cpuTwo);
        assertThat(afterRaises.getTrickNumber()).as("the trick is still running").isEqualTo(1);
        assertThat(afterRaises.getPassedUsernames()).as("nobody passed — both could raise").isEmpty();

        // The turn comes back to the host, who already played this trick. That is the point:
        // leading does not take you out of the bidding.
        assertThat(afterRaises.getCurrentTurnUsername())
            .as("the turn returns to the player who led, to raise again")
            .isEqualTo("turnorder-raise");

        loginAs("turnorder-raise");
        cardGameService.play(id, List.of("JS"));

        // Neither CPU can beat the J, so both pass and the trick finally settles on the host.
        CardGame afterTrick = reload(id);
        assertThat(afterTrick.getTrickNumber()).as("the trick resolved once both CPUs passed").isEqualTo(2);
        assertThat(afterTrick.getCurrentTurnUsername())
            .as("the last player to raise wins the trick and leads the next")
            .isEqualTo("turnorder-raise");
        assertThat(cpuOne).isNotEqualTo(cpuTwo); // distinct seats really did both act
    }

    /**
     * The play that wins a trick must stay attributed to the trick it won. Scoring a trick
     * advances the trick number, so a log cleared at that moment would drop the winning play
     * and the next trick's lead would become the first thing a client ever saw — making the
     * winning play look as though it belonged to the following trick.
     */
    @Test
    void theWinningPlayStaysInTheTrickItWon() {
        // Host leads the 8 and then takes the trick with the 7, the strongest card in the game,
        // so neither CPU can answer and the trick resolves on that play.
        CardGame game = riggedThreeSeat("turnorder-winplay", "8S7S4C4D4H", "9S4S", "TS6S");
        Long id = game.getId();

        loginAs("turnorder-winplay");
        cardGameService.play(id, List.of("8S"));
        CardGameDTO midTrick = cardGameService.getGame(id);
        assertThat(midTrick.trickHistoryNumber())
            .as("mid-trick the history describes the trick in progress")
            .isEqualTo(midTrick.trickNumber());

        // The 7 cannot be beaten, so this play ends the trick.
        cardGameService.play(id, List.of("7S"));

        CardGame after = reload(id);
        assertThat(after.getTrickNumber()).as("the trick was scored").isEqualTo(2);
        assertThat(after.getTrickLogNumber())
            .as("the history still describes the trick that just finished, not the new one")
            .isEqualTo(1);
        assertThat(after.getTrickLog())
            .as("the winning play survived the trick being scored")
            .contains("7S");

        CardGameDTO dto = cardGameService.getGame(id);
        assertThat(dto.trickHistoryNumber()).as("the client is told which trick it is seeing").isEqualTo(1);
        // The winning play is the last *play*, not the last entry: both CPUs still have to pass
        // after it before the trick resolves, and those passes are part of this trick too.
        List<CardGameTrickEntryDTO> plays = dto.trickHistory().stream().filter(e -> !e.passed()).toList();
        assertThat(plays).as("the trick's plays survived it being scored").isNotEmpty();
        assertThat(plays.get(plays.size() - 1).cards())
            .as("the last play of the trick is the 7 that won it")
            .containsExactly("7S");
        assertThat(dto.trickHistory())
            .as("the passes that ended the trick are recorded against it as well")
            .anyMatch(CardGameTrickEntryDTO::passed);
    }

    @Test
    void aPlayerWhoPassesIsOutOfThatTrickButTheOthersKeepRaising() {
        // The first CPU holds nothing playable over the 8 and must pass. The second CPU can
        // raise, which hands the turn back to the host rather than ending the trick.
        CardGame game = riggedThreeSeat("turnorder-passer", "8SJS4C4D4H", "4S6S", "TS6H");
        Long id = game.getId();

        loginAs("turnorder-passer");
        cardGameService.play(id, List.of("8S"));

        CardGame after = reload(id);
        assertThat(after.getCurrentPlay()).as("the other CPU still raised to a 10").isEqualTo("TS");
        assertThat(after.getTrickNumber()).as("one pass does not end a three-handed trick").isEqualTo(1);
        assertThat(after.getCurrentTurnUsername())
            .as("the turn returns to the host, who may raise the 10")
            .isEqualTo("turnorder-passer");
    }

    /**
     * A pass declines the play in front of you and nothing more. When somebody behind you raises
     * it, the question has changed, so the turn comes back round and you may answer the new play
     * or pass again — for as long as the trick keeps climbing.
     *
     * <p>The scenario is the one reported against the previous behaviour: a 4 is led, we pass,
     * another player raises to an 8, the first raises again to a 9, and we must still get the
     * choice to beat the 9. Previously the first pass took us out of the trick for good.
     */
    @Test
    void aPassDeclinesOnlyTheCurrentPlayAndTheTurnComesBackOnEveryRaise() {
        // Rigged mid-trick: the second CPU has led a 4 and it is our turn to answer it. Our hand
        // holds a 10, so every pass below is a choice rather than a lack of options.
        CardGame game = riggedThreeSeat("turnorder-reask", "TS4C4D4H", "8S", "9S6S");
        Long id = game.getId();
        List<CardGamePlayer> seats = seats(game);
        String cpuOne = seats.get(1).getUsername();
        String cpuTwo = seats.get(2).getUsername();

        game.setCurrentPlay("4S");
        game.setCurrentPlayUsername(cpuTwo);
        game.setCurrentShape(CardDeck.SHAPE_SINGLE);
        game.setCurrentRank(CardDeck.rankOf("4S"));
        game.setCurrentTurnUsername("turnorder-reask");
        gameRepository.save(game);

        loginAs("turnorder-reask");
        CardGameDTO afterPass = cardGameService.pass(id);

        // Passing on the 4 let the other two raise it: the first CPU to an 8, the second to a 9.
        CardGame after = reload(id);
        assertThat(after.getCurrentPlay()).as("the 4 was raised to an 8 and then a 9").isEqualTo("9S");
        assertThat(after.getCurrentPlayUsername()).isEqualTo(cpuTwo);
        assertThat(after.getTrickNumber()).as("still the same trick throughout").isEqualTo(1);

        // The point of the fix: having passed once, we are asked again now the play has changed.
        assertThat(after.getCurrentTurnUsername())
            .as("the turn comes back to the player who passed, because the play was raised")
            .isEqualTo("turnorder-reask");
        assertThat(afterPass.myTurn()).as("and the client is told it may act").isTrue();
        assertThat(after.getPassedUsernames())
            .as("the stale pass is cleared — it declined the 4, not the 9")
            .doesNotContain("turnorder-reask");

        // And the choice is real: the 10 beats the 9 and takes the trick.
        cardGameService.play(id, List.of("TS"));
        CardGame won = reload(id);
        assertThat(won.getTrickNumber()).as("the trick resolved on our raise").isEqualTo(2);
        assertThat(won.getCurrentTurnUsername())
            .as("winning the trick means leading the next one")
            .isEqualTo("turnorder-reask");
        assertThat(cpuOne).isNotEqualTo(cpuTwo); // distinct seats really did both act
    }

    /**
     * Every trick entry is attributable to a seat at the table, by username. The client shows each
     * player's last action under that player's own position, which it can only do by joining
     * {@code trickHistory} to {@code players} on that field — so an entry naming a username that
     * is not in {@code players} would be an action the UI could not place.
     *
     * <p>Also pins that a seat acting twice in one trick is readable as such. A pass no longer
     * ends a player's involvement, so a seat can appear several times, and the seat's display has
     * to resolve to its <em>latest</em> action rather than its first.
     */
    @Test
    void everyTrickEntryIsAttributableToASeatAtTheTable() {
        // The host leads an 8, the first CPU passes (nothing playable), the second raises to a 10,
        // and the turn comes back to the host, who raises to a J. That gives a trick in which the
        // host appears twice and one seat appears as a pass.
        CardGame game = riggedThreeSeat("turnorder-attrib", "8SJS4C4D4H", "4S6S", "TS6H");
        Long id = game.getId();
        List<CardGamePlayer> seats = seats(game);
        String cpuOne = seats.get(1).getUsername();

        loginAs("turnorder-attrib");
        cardGameService.play(id, List.of("8S"));
        cardGameService.play(id, List.of("JS"));

        CardGameDTO dto = cardGameService.getGame(id);
        List<String> seated = dto.players().stream().map(p -> p.username()).toList();
        assertThat(dto.trickHistory()).as("the trick produced entries to attribute").isNotEmpty();
        assertThat(dto.trickHistory())
            .as("every entry names a seat the client can place it under")
            .allSatisfy(entry -> {
                assertThat(seated).contains(entry.username());
                assertThat(entry.name()).as("and carries a display name for that seat").isNotBlank();
            });

        // The host acted twice in this trick; the later action is the one their seat must show.
        List<CardGameTrickEntryDTO> mine = dto.trickHistory().stream()
            .filter(e -> "turnorder-attrib".equals(e.username()))
            .toList();
        assertThat(mine).as("the player who led and then raised appears twice").hasSizeGreaterThanOrEqualTo(2);
        assertThat(mine.get(mine.size() - 1).cards())
            .as("entries are oldest first, so the last one is the J that seat raised to")
            .containsExactly("JS");

        // The seat that passed is attributed too, with no cards, so its display reads as a pass
        // rather than as an empty seat that has not acted.
        assertThat(dto.trickHistory())
            .as("the passing seat is recorded against its own username")
            .anySatisfy(entry -> {
                assertThat(entry.username()).isEqualTo(cpuOne);
                assertThat(entry.passed()).isTrue();
                assertThat(entry.cards()).isEmpty();
            });
    }
}
