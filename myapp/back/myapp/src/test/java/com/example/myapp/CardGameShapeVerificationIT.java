package com.example.myapp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.myapp.contants.enumeration.CardGameStatus;
import com.example.myapp.domain.CardGame;
import com.example.myapp.domain.CardGamePlayer;
import com.example.myapp.repository.CardGamePlayerRepository;
import com.example.myapp.repository.CardGameRepository;
import com.example.myapp.service.CardDeck;
import com.example.myapp.service.CardGameService;
import com.example.myapp.service.dto.CardGameDTO;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Pins the make-up of a three-plus-two: the "two" is a pair, not two spare cards. A triple with
 * two odd cards beside it is not a play at all, and the server has to say so — the client greys
 * the button out, but only this check is binding.
 *
 * <p>Driven through {@link CardGameService#play} rather than {@link CardDeck} directly, because
 * the rule is only worth anything if it holds on the path a request actually takes.
 */
@SpringBootTest
class CardGameShapeVerificationIT {

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
     * A three-seat table with the host on lead and a hand set by hand. The deck is left long
     * enough that no refill can empty it, keeping the game off its end condition.
     */
    private CardGame riggedLead(String host, String hostHand) {
        loginAs(host);
        CardGameDTO created = cardGameService.createTable(2, List.of());
        CardGame game = reload(created.id());

        List<CardGamePlayer> seats = seats(game);
        seats.get(0).setHand(hostHand);
        seats.get(1).setHand("4S4H");
        seats.get(2).setHand("6S6H");
        seats.forEach(playerRepository::save);

        game.setDeck("4D6D8H8D8C9H9D9C9S");
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
    void aTripleWithTwoOddCardsIsRejected() {
        // Three 9s, then a 4 and a K that do not pair with each other.
        CardGame game = riggedLead("shape-odd", "9S9H9D4SKH");
        loginAs("shape-odd");

        assertThatThrownBy(() -> cardGameService.play(game.getId(), List.of("9S", "9H", "9D", "4S", "KH")))
            .as("triple plus two odd cards")
            .hasMessageContaining("not a legal play");

        // Rejected before anything was committed: the hand is intact and it is still our turn.
        CardGame after = reload(game.getId());
        assertThat(CardDeck.parse(seats(after).get(0).getHand())).hasSize(5);
        assertThat(after.getCurrentPlay()).isNull();
        assertThat(after.getCurrentTurnUsername()).isEqualTo("shape-odd");
    }

    @Test
    void aTripleWithARealPairIsAccepted() {
        CardGame game = riggedLead("shape-pair", "9S9H9D4S4H");
        loginAs("shape-pair");

        CardGameDTO played = cardGameService.play(game.getId(), List.of("9S", "9H", "9D", "4S", "4H"));

        // Asserted on the trick history rather than on currentShape: neither CPU holds anything
        // that answers a full house, so both pass and the trick resolves inside this same
        // request, which clears the table. The history is kept through resolution for exactly
        // this reason — the play that wins a trick stays attributed to it.
        assertThat(played.trickHistory())
            .as("the full house was accepted and recorded")
            .isNotEmpty();
        assertThat(played.trickHistory().get(0).cards()).containsExactlyInAnyOrder("9S", "9H", "9D", "4S", "4H");
        // Ranked on the triple, not on the pair.
        assertThat(played.trickHistory().get(0).shape()).isEqualTo(CardDeck.SHAPE_FULL_HOUSE);
        assertThat(CardDeck.detectShape(played.trickHistory().get(0).cards()).orElseThrow().rank())
            .isEqualTo(CardDeck.rankOf("9S"));
    }

    @Test
    void threeOfARankIsPlayableOnItsOwn() {
        CardGame game = riggedLead("shape-triple", "9S9H9D4S");
        loginAs("shape-triple");

        CardGameDTO played = cardGameService.play(game.getId(), List.of("9S", "9H", "9D"));

        assertThat(played.trickHistory()).isNotEmpty();
        assertThat(played.trickHistory().get(0).cards()).containsExactlyInAnyOrder("9S", "9H", "9D");
        assertThat(played.trickHistory().get(0).shape()).isEqualTo(CardDeck.SHAPE_TRIPLE);
    }

    @Test
    void fourOfARankIsPlayableOnItsOwn() {
        CardGame game = riggedLead("shape-quad", "9S9H9D9C4S");
        loginAs("shape-quad");

        CardGameDTO played = cardGameService.play(game.getId(), List.of("9S", "9H", "9D", "9C"));

        assertThat(played.trickHistory()).isNotEmpty();
        assertThat(played.trickHistory().get(0).cards()).containsExactlyInAnyOrder("9S", "9H", "9D", "9C");
        assertThat(played.trickHistory().get(0).shape()).isEqualTo(CardDeck.SHAPE_QUAD);
    }

    @Test
    void aBareTripleIsNotAnsweredByAThreePlusTwo() {
        // Shapes never meet, so padding a higher triple out to five cards does not answer a
        // bare triple — the answer has to be three cards.
        CardGame game = riggedLead("shape-mix", "9S9H9D4S");
        loginAs("shape-mix");
        CardGameDTO played = cardGameService.play(game.getId(), List.of("9S", "9H", "9D"));

        // Read from the history rather than currentShape: neither CPU can answer a triple, so
        // both pass and the trick resolves inside this request, clearing the table.
        assertThat(played.trickHistory().get(0).shape()).isEqualTo(CardDeck.SHAPE_TRIPLE);
        // A full house of kings is a bigger pile of cards and a higher rank, and still does not
        // beat it, because it is a different shape.
        assertThat(CardDeck.beats(CardDeck.SHAPE_FULL_HOUSE, CardDeck.rankOf("KS"), CardDeck.SHAPE_TRIPLE, CardDeck.rankOf("9S")))
            .isFalse();
        // A higher bare triple does.
        assertThat(CardDeck.beats(CardDeck.SHAPE_TRIPLE, CardDeck.rankOf("KS"), CardDeck.SHAPE_TRIPLE, CardDeck.rankOf("9S")))
            .isTrue();
    }

    @Test
    void theCpuNeverOffersAnIllegalThreePlusTwo() {
        // Every play the CPU brain can assemble from a hand holding a triple, a quad, odd cards
        // and only one usable pair has to satisfy the same validator a human selection does.
        List<String> hand = List.of("9S", "9H", "9D", "9C", "4S", "4H", "KS", "7D", "XS", "5C");
        assertThat(CardDeck.legalPlays(hand))
            .isNotEmpty()
            .allSatisfy(play ->
                assertThat(CardDeck.detectShape(play.cards())).as("CPU play %s", play.cards()).isPresent()
            );

        // And a triple with nothing to pair beside it yields no five-card play at all.
        assertThat(CardDeck.legalPlays(List.of("9S", "9H", "9D", "4S", "KH")))
            .noneMatch(play -> play.cards().size() == 5);
    }
}
