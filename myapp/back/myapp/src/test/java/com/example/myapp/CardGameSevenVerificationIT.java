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
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Pins the shortcut on a 7: a play whose comparison rank is the top rank cannot be answered, so
 * the remaining seats are passed automatically and the trick is scored in the same call instead
 * of asking each of them in turn for the only reply they have.
 *
 * <p>Every table here is seated with <strong>human</strong> opponents. A CPU passes inside the
 * same request anyway, so a table of CPUs cannot tell the shortcut from the ordinary circuit —
 * it is precisely the seat that would otherwise be left to click "Pass" on a play it provably
 * cannot beat that this is for. Human seats also leave the turn where the rules leave it, so
 * "the trick is still open and the next seat was asked" is observable.
 *
 * <p>The rule reads the play's rank, not its cards, which is what the two negative cases are
 * about: a full house padded with 7s is ranked on its triple and a higher full house takes it,
 * and a joker is still beaten by a 7.
 */
@SpringBootTest
class CardGameSevenVerificationIT {

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
     * A table of humans with hands set by hand and the host on a fresh lead.
     *
     * <p>Seated through the repositories for the reason the other verification ITs do it: no
     * service path rigs a hand. {@code startedDate} and {@code lastActionDate} are stamped now so
     * that neither the turn clock nor the whole-game cap fires while the table is examined, and
     * {@code trickNumber} starts at 1 because that is where a real deal starts it.
     *
     * <p>The deck is left long enough that no refill can empty it, keeping the game off its end
     * condition.
     */
    private CardGame riggedLead(String host, String hostHand, String... otherHands) {
        CardGame game = new CardGame();
        game.setStatus(CardGameStatus.ACTIVE);
        game.setHostUsername(host);
        game.setCreatedBy(host);
        game.setLastModifiedBy(host);
        game.setTrickNumber(1);
        CardGame saved = gameRepository.save(game);

        addSeat(saved, host, 0, hostHand);
        for (int i = 0; i < otherHands.length; i++) {
            addSeat(saved, opponent(host, i + 1), i + 1, otherHands[i]);
        }

        saved.setDeck("4H4D6H6D6C8H8D8C9H9D9C9S");
        saved.setCurrentTurnUsername(host);
        saved.setStartedDate(Instant.now());
        saved.setLastActionDate(Instant.now());
        return gameRepository.save(saved);
    }

    /**
     * The same table mid-trick: the first opponent has led {@code lead} and it is the host's turn
     * to answer it. This is the position the shortcut is about — the host raises with a 7, and the
     * seats behind them are the ones that would have been asked.
     *
     * <p>The led cards are not removed from the leader's hand. They are the play on the table,
     * which is all the rules read, and rigging the hand as well would only constrain the fixture
     * for no assertion's benefit — the same shortcut the other verification ITs take.
     */
    private CardGame riggedAnswer(String host, String lead, String hostHand, String... otherHands) {
        CardGame game = riggedLead(host, hostHand, otherHands);
        List<String> cards = CardDeck.parse(lead);
        CardDeck.Play play = CardDeck.detectShape(cards).orElseThrow();

        game.setCurrentPlay(lead);
        game.setCurrentPlayUsername(opponent(host, 1));
        game.setCurrentShape(play.shape());
        game.setCurrentRank(play.rank());
        game.setTrickPile(lead);
        game.setTrickLog("1:" + lead);
        game.setTrickLogNumber(game.getTrickNumber());
        game.setCurrentTurnUsername(host);
        return gameRepository.save(game);
    }

    private String opponent(String host, int index) {
        return host + "-opp" + index;
    }

    private void addSeat(CardGame game, String username, int index, String hand) {
        CardGamePlayer player = new CardGamePlayer();
        player.setGame(game);
        player.setUsername(username);
        player.setSeat(index);
        player.setCpu(false);
        player.setHand(hand);
        player.setInvitationStatus(CardGameInvitationStatus.ACCEPTED);
        player.setCreatedBy(game.getHostUsername());
        player.setLastModifiedBy(game.getHostUsername());
        playerRepository.save(player);
    }

    // ----- The four cases the request names: one, two, three and four 7s ---------------------

    @Test
    void aSingleSevenEndsTheTrickWithoutAskingAnybody() {
        // Both opponents hold cards, so they are able to answer — they simply have no answer to
        // a 7, which is the whole point.
        CardGame game = riggedAnswer("seven-single", "8S", "7S4C4D4H", "9S4S", "TS6S");
        Long id = game.getId();

        assertThat(game.getCurrentTurnUsername()).as("the host is answering the 8").isEqualTo("seven-single");
        assertThat(game.getTrickNumber()).isEqualTo(1);

        loginAs("seven-single");
        cardGameService.play(id, List.of("7S"));

        CardGame after = reload(id);
        assertThat(after.getTrickNumber())
            .as("the 7 scored the trick in the same call, with nobody left to ask")
            .isEqualTo(2);
        assertThat(after.getCurrentTurnUsername())
            .as("and the player who played it leads the next trick")
            .isEqualTo("seven-single");
        assertThat(after.getCurrentPlay()).as("the table was cleared for the new trick").isNull();
    }

    @Test
    void aPairOfSevensEndsTheTrick() {
        CardGame game = riggedAnswer("seven-pair", "8S8H", "7S7H4C4D", "9S9H4S", "TSTH6S");
        Long id = game.getId();

        loginAs("seven-pair");
        cardGameService.play(id, List.of("7S", "7H"));

        CardGame after = reload(id);
        assertThat(after.getTrickNumber()).as("a pair of 7s is unanswerable too").isEqualTo(2);
        assertThat(after.getCurrentTurnUsername()).isEqualTo("seven-pair");
    }

    @Test
    void threeSevensEndTheTrick() {
        CardGame game = riggedAnswer("seven-triple", "8S8H8D", "7S7H7D4C", "9S9H9D4S", "TSTHTD6S");
        Long id = game.getId();

        loginAs("seven-triple");
        cardGameService.play(id, List.of("7S", "7H", "7D"));

        CardGame after = reload(id);
        assertThat(after.getTrickNumber()).as("a triple of 7s is unanswerable too").isEqualTo(2);
        assertThat(after.getCurrentTurnUsername()).isEqualTo("seven-triple");
    }

    @Test
    void fourSevensEndTheTrick() {
        CardGame game = riggedAnswer("seven-quad", "8S8H8D8C", "7S7H7D7C", "9S9H9D9C", "TSTHTDTC");
        Long id = game.getId();

        loginAs("seven-quad");
        cardGameService.play(id, List.of("7S", "7H", "7D", "7C"));

        CardGame after = reload(id);
        assertThat(after.getTrickNumber()).as("four 7s are unanswerable too").isEqualTo(2);
        assertThat(after.getCurrentTurnUsername()).isEqualTo("seven-quad");
    }

    // ----- The five-card shapes, ranked on their set and not on their padding ----------------

    @Test
    void aFullHouseTripledOnSevensEndsTheTrick() {
        // Ranked on its triple, so this is a 7-ranked play however cheap the pair beside it is.
        CardGame game = riggedAnswer("seven-fh", "4S4H4D6S6H", "7S7H7D8C8D", "9S9H9D2S2H", "TSTHTD3S3H");
        Long id = game.getId();

        loginAs("seven-fh");
        cardGameService.play(id, List.of("7S", "7H", "7D", "8C", "8D"));

        // Read off the selection rather than off the row: the trick resolved inside that call,
        // and clearing the trick is what takes the rank off the table.
        assertThat(CardDeck.detectShape(List.of("7S", "7H", "7D", "8C", "8D")).orElseThrow().rank())
            .as("the play is ranked on its triple of 7s, not on the pair of 8s")
            .isEqualTo(CardDeck.MAX_RANK);

        CardGame after = reload(id);
        assertThat(after.getTrickNumber()).as("a full house of 7s is unanswerable").isEqualTo(2);
        assertThat(after.getCurrentTurnUsername()).isEqualTo("seven-fh");
        assertThat(after.getCurrentRank()).as("and the table was cleared behind it").isNull();
    }

    @Test
    void fourSevensWithAKickerEndTheTrick() {
        CardGame game = riggedAnswer("seven-quad1", "4S4H4D4C6S", "7S7H7D7C8S", "9S9H9D9C2S", "TSTHTDTC3S");
        Long id = game.getId();

        loginAs("seven-quad1");
        cardGameService.play(id, List.of("7S", "7H", "7D", "7C", "8S"));

        CardGame after = reload(id);
        assertThat(after.getTrickNumber()).as("quad 7s plus a kicker is unanswerable").isEqualTo(2);
        assertThat(after.getCurrentTurnUsername()).isEqualTo("seven-quad1");
    }

    /**
     * The case a "the selection contains a 7" rule would get wrong. This full house is ranked on
     * its triple of 4s, so the 7s in it are padding and a higher full house takes it — the table
     * has to be asked after all.
     *
     * <p>Led rather than raised because 4 is the weakest rank: there is no lower full house for
     * this one to answer.
     */
    @Test
    void aFullHouseWhoseSevensAreOnlyThePairDoesNotEndTheTrick() {
        CardGame game = riggedLead("seven-padding", "4S4H4D7S7H", "9S9H9D8S8H", "TSTHTD6S6H");
        Long id = game.getId();

        loginAs("seven-padding");
        cardGameService.play(id, List.of("4S", "4H", "4D", "7S", "7H"));

        CardGame after = reload(id);
        assertThat(after.getCurrentRank())
            .as("ranked on the triple of 4s, not on the 7s beside it")
            .isEqualTo(CardDeck.rankOf("4S"));
        assertThat(after.getTrickNumber()).as("the trick is still open").isEqualTo(1);
        assertThat(after.getCurrentTurnUsername())
            .as("the next seat is asked, because a higher full house beats this")
            .isEqualTo("seven-padding-opp1");
        assertThat(after.getPassedUsernames()).as("and nobody has been passed").isEmpty();
    }

    @Test
    void aJokerDoesNotEndTheTrickBecauseASevenTakesIt() {
        CardGame game = riggedLead("seven-joker", "XB4C4D4H8S", "7S4S", "TS6S");
        Long id = game.getId();

        loginAs("seven-joker");
        cardGameService.play(id, List.of("XB"));

        CardGame after = reload(id);
        assertThat(after.getTrickNumber()).as("a joker is not the top rank").isEqualTo(1);
        assertThat(after.getCurrentTurnUsername())
            .as("the seat holding the 7 is asked, and it has an answer")
            .isEqualTo("seven-joker-opp1");

        // And the 7 does take it, which is why the joker could not have short-circuited.
        loginAs("seven-joker-opp1");
        cardGameService.play(id, List.of("7S"));
        CardGame beaten = reload(id);
        assertThat(beaten.getTrickNumber()).as("the 7 beat the joker and ended the trick").isEqualTo(2);
        assertThat(beaten.getCurrentTurnUsername()).isEqualTo("seven-joker-opp1");
    }

    // ----- What the shortcut leaves behind ---------------------------------------------------

    /**
     * The passes are recorded, not skipped. The client draws the trick from this history, and a
     * finished trick whose opponents' rows were blank would read as though they never acted.
     */
    @Test
    void theAutomaticPassesAreRecordedAgainstTheTrickTheyEnded() {
        CardGame game = riggedAnswer("seven-log", "8S", "7S4C4D4H", "9S4S", "TS6S");
        Long id = game.getId();

        loginAs("seven-log");
        cardGameService.play(id, List.of("7S"));

        CardGameDTO dto = cardGameService.getGame(id);
        assertThat(dto.trickHistoryNumber())
            .as("the history still describes the trick that just finished, not the new one")
            .isEqualTo(1);
        assertThat(dto.trickHistory().stream().filter(CardGameTrickEntryDTO::passed).toList())
            .as("both opponents are on record as having passed the 7, in seat order")
            .extracting(CardGameTrickEntryDTO::username)
            .containsExactly("seven-log-opp1", "seven-log-opp2");

        List<CardGameTrickEntryDTO> plays = dto.trickHistory().stream().filter(e -> !e.passed()).toList();
        assertThat(plays.get(plays.size() - 1).cards())
            .as("the last play of the trick is the 7 that won it")
            .containsExactly("7S");
    }

    /** Passes are logged in seat order from the player, as they are when the seats are asked. */
    @Test
    void theAutomaticPassesAreLoggedInSeatOrderStartingAfterThePlayer() {
        CardGame game = riggedAnswer("seven-order", "8S", "7S4C4D4H", "9S4S", "TS6S", "JS6H");
        Long id = game.getId();

        loginAs("seven-order");
        cardGameService.play(id, List.of("7S"));

        assertThat(reload(id).getTrickLog())
            .as("the three opponents passed in seat order after the play")
            .endsWith("0:7S|1:-|2:-|3:-");
    }

    /** The trick is scored like any other: points to the player who played the 7, hands refilled. */
    @Test
    void theSevensTrickIsScoredAndEveryHandRefills() {
        // The pile holds the K that was led and the 7 that answered it: ten points.
        CardGame game = riggedAnswer("seven-score", "KS", "7S4C4D4H", "9S4S", "TS6S");
        Long id = game.getId();

        loginAs("seven-score");
        cardGameService.play(id, List.of("7S"));

        List<CardGamePlayer> after = seats(reload(id));
        assertThat(after.get(0).getScore())
            .as("the trick's points went to the player who played the 7")
            .isEqualTo(10);
        assertThat(after.get(1).getScore()).isZero();
        assertThat(after.get(2).getScore()).isZero();
        assertThat(after)
            .as("everybody refilled to five, the seats that were passed included")
            .allSatisfy(seat -> assertThat(CardDeck.parse(seat.getHand())).hasSize(5));
    }

    /**
     * Leading a 7 resolves at once as well. Not a case the request named, but the same fact
     * settles it: the lead cannot be answered, so the leader wins their own lead and leads again.
     */
    @Test
    void leadingASevenWinsTheTrickImmediatelyAndLeadsTheNextOne() {
        CardGame game = riggedLead("seven-lead", "7S4C4D4H8S", "9S4S", "TS6S");
        Long id = game.getId();

        loginAs("seven-lead");
        cardGameService.play(id, List.of("7S"));

        CardGame after = reload(id);
        assertThat(after.getTrickNumber()).as("the lead was scored on the spot").isEqualTo(2);
        assertThat(after.getCurrentTurnUsername())
            .as("the leader won their own lead and leads the next trick")
            .isEqualTo("seven-lead");
        assertThat(after.getPassedUsernames())
            .as("the pass badges are cleared with the rest of the trick state")
            .isEmpty();
    }

    /**
     * A seat that is already out is not passed: it has no cards to decline with. The same
     * exclusion the ordinary circuit makes when it skips an empty hand.
     */
    @Test
    void aSeatWithNoCardsLeftIsNotPassed() {
        CardGame game = riggedAnswer("seven-out", "8S", "7S4C", "9S4S", "");
        Long id = game.getId();

        loginAs("seven-out");
        cardGameService.play(id, List.of("7S"));

        CardGame after = reload(id);
        assertThat(after.getTrickLog())
            .as("only the seat that still held cards was passed")
            .endsWith("0:7S|1:-");
        assertThat(after.getTrickNumber()).isEqualTo(2);
    }

    /**
     * The player who played the 7 is the last actor on the row. The seats that auto-passed did
     * not act — nothing they could have held would have changed the outcome.
     */
    @Test
    void theAttributionStaysWithThePlayerWhoPlayedTheSeven() {
        CardGame game = riggedAnswer("seven-actor", "8S", "7S4C4D4H", "9S4S", "TS6S");
        Long id = game.getId();

        loginAs("seven-actor");
        cardGameService.play(id, List.of("7S"));

        assertThat(reload(id).getLastModifiedBy())
            .as("a pass by a seat that was never asked is not an action by that seat")
            .isEqualTo("seven-actor");
    }
}
