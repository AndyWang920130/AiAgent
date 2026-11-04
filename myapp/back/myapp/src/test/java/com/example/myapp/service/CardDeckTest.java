package com.example.myapp.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.myapp.service.CardDeck.Play;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Rules tests for 70235. The rules are where the bugs would be and they are pure functions,
 * so they are pinned here rather than exercised through the service.
 */
class CardDeckTest {

    @Nested
    @DisplayName("deck")
    class DeckTests {

        @Test
        void twoPlayersGetOnePack() {
            assertThat(CardDeck.buildDeck(2)).hasSize(54);
        }

        @Test
        void threeToFivePlayersGetTwoPacks() {
            assertThat(CardDeck.buildDeck(3)).hasSize(108);
            assertThat(CardDeck.buildDeck(4)).hasSize(108);
            assertThat(CardDeck.buildDeck(5)).hasSize(108);
        }

        @Test
        void onePackHas52DistinctSuitedCardsAndTwoJokers() {
            List<String> deck = CardDeck.buildDeck(2);
            Set<String> distinct = new HashSet<>(deck);
            assertThat(distinct).hasSize(54); // every card in a single pack is unique
            assertThat(deck.stream().filter(CardDeck::isJoker)).hasSize(2);
        }

        @Test
        void everyCardParsesToAKnownRank() {
            assertThat(CardDeck.buildDeck(3)).allSatisfy(card -> assertThat(CardDeck.rankOf(card)).isNotNegative());
        }

        @Test
        void parseAndJoinRoundTrip() {
            List<String> cards = List.of("7S", "XB", "TD", "5C");
            assertThat(CardDeck.parse(CardDeck.join(cards))).isEqualTo(cards);
        }

        @Test
        void parseHandlesEmptyAndNull() {
            assertThat(CardDeck.parse(null)).isEmpty();
            assertThat(CardDeck.parse("")).isEmpty();
        }
    }

    @Nested
    @DisplayName("ranking")
    class RankingTests {

        @Test
        void confirmedOrderIsSevenJokerTwoThreeFiveThenAceDown() {
            // 7 > 0 (joker) > 2 > 3 > 5 > A > K > Q > J > 10 > 9 > 8 > 6 > 4
            List<String> strongestFirst = List.of("7S", "XB", "2S", "3S", "5S", "AS", "KS", "QS", "JS", "TS", "9S", "8S", "6S", "4S");
            List<Integer> ranks = strongestFirst.stream().map(CardDeck::rankOf).toList();
            assertThat(ranks).isSortedAccordingTo((a, b) -> Integer.compare(b, a));
            assertThat(ranks).doesNotHaveDuplicates();
        }

        @Test
        void bothJokersRankEqually() {
            assertThat(CardDeck.rankOf("XB")).isEqualTo(CardDeck.rankOf("XL"));
        }

        @Test
        void sevenIsStrongestAndFourIsWeakest() {
            assertThat(CardDeck.rankOf("7S")).isEqualTo(CardDeck.RANK_ORDER.length() - 1);
            assertThat(CardDeck.rankOf("4S")).isZero();
        }

        @Test
        void suitDoesNotAffectRank() {
            assertThat(CardDeck.rankOf("7S")).isEqualTo(CardDeck.rankOf("7C"));
        }

        @Test
        void sortByStrengthPutsSevenFirst() {
            assertThat(CardDeck.sortByStrength(List.of("4S", "7H", "KD", "XB"))).startsWith("7H", "XB");
        }
    }

    @Nested
    @DisplayName("points")
    class PointTests {

        @Test
        void onlyFiveTenAndKingScore() {
            assertThat(CardDeck.pointsOf("5S")).isEqualTo(5);
            assertThat(CardDeck.pointsOf("TS")).isEqualTo(10);
            assertThat(CardDeck.pointsOf("KS")).isEqualTo(10);
            assertThat(CardDeck.pointsOf("7S")).isZero();
            assertThat(CardDeck.pointsOf("AS")).isZero();
        }

        @Test
        void jokersArePowerfulButScoreNothing() {
            assertThat(CardDeck.pointsOf("XB")).isZero();
            assertThat(CardDeck.pointsOf("XL")).isZero();
        }

        @Test
        void onePackHolds100PointsAndTwoPacks200() {
            assertThat(CardDeck.pointsOf(CardDeck.buildDeck(2))).isEqualTo(100);
            assertThat(CardDeck.pointsOf(CardDeck.buildDeck(4))).isEqualTo(200);
        }
    }

    @Nested
    @DisplayName("shape detection")
    class ShapeTests {

        @Test
        void singleCardIsASingle() {
            Play play = CardDeck.detectShape(List.of("9S")).orElseThrow();
            assertThat(play.shape()).isEqualTo(CardDeck.SHAPE_SINGLE);
            assertThat(play.rank()).isEqualTo(CardDeck.rankOf("9S"));
        }

        @Test
        void twoOfTheSameRankIsAPair() {
            Play play = CardDeck.detectShape(List.of("9S", "9H")).orElseThrow();
            assertThat(play.shape()).isEqualTo(CardDeck.SHAPE_PAIR);
            assertThat(play.rank()).isEqualTo(CardDeck.rankOf("9S"));
        }

        @Test
        void twoJokersPairUp() {
            assertThat(CardDeck.detectShape(List.of("XB", "XL")).orElseThrow().shape())
                .isEqualTo(CardDeck.SHAPE_PAIR);
        }

        @Test
        void twoDifferentRanksAreNotAPlay() {
            assertThat(CardDeck.detectShape(List.of("9S", "TH"))).isEmpty();
        }

        @Test
        void tripleWithAPairIsAFullHouseRankedOnTheTriple() {
            Play play = CardDeck.detectShape(List.of("9S", "9H", "9D", "4S", "4H")).orElseThrow();
            assertThat(play.shape()).isEqualTo(CardDeck.SHAPE_FULL_HOUSE);
            assertThat(play.rank()).isEqualTo(CardDeck.rankOf("9S"));
        }

        @Test
        void fullHouseExtrasMustBeARealPair() {
            // The "two" in three-plus-two is a pair, not two spare cards: 4 and K do not pair,
            // so this is five cards with no shape rather than a full house of 9s.
            assertThat(CardDeck.detectShape(List.of("9S", "9H", "9D", "4S", "KH"))).isEmpty();
            // Nor does the triple's own rank excuse the pair from pairing.
            assertThat(CardDeck.detectShape(List.of("9S", "9H", "9D", "4S", "6H"))).isEmpty();
        }

        @Test
        void quadWithOneExtraIsRankedOnTheQuad() {
            Play play = CardDeck.detectShape(List.of("9S", "9H", "9D", "9C", "KH")).orElseThrow();
            assertThat(play.shape()).isEqualTo(CardDeck.SHAPE_QUAD_PLUS_ONE);
            assertThat(play.rank()).isEqualTo(CardDeck.rankOf("9S"));
        }

        @Test
        void fiveOfOneRankCountsAsQuadPlusOne() {
            // Possible with two packs. Four of a kind takes priority over the full house read.
            Play play = CardDeck.detectShape(List.of("9S", "9H", "9D", "9C", "9S")).orElseThrow();
            assertThat(play.shape()).isEqualTo(CardDeck.SHAPE_QUAD_PLUS_ONE);
        }

        @Test
        void threeOfARankStandsAlone() {
            Play play = CardDeck.detectShape(List.of("9S", "9H", "9D")).orElseThrow();
            assertThat(play.shape()).isEqualTo(CardDeck.SHAPE_TRIPLE);
            assertThat(play.rank()).isEqualTo(CardDeck.rankOf("9S"));
        }

        @Test
        void fourOfARankStandsAlone() {
            Play play = CardDeck.detectShape(List.of("9S", "9H", "9D", "9C")).orElseThrow();
            assertThat(play.shape()).isEqualTo(CardDeck.SHAPE_QUAD);
            assertThat(play.rank()).isEqualTo(CardDeck.rankOf("9S"));
        }

        @Test
        void aBareTripleAndAThreePlusTwoAreDifferentShapes() {
            // Both are built on three of a rank, but they are not the same play and so never
            // meet: a bare triple cannot be answered by padding one out to five cards.
            String triple = CardDeck.detectShape(List.of("9S", "9H", "9D")).orElseThrow().shape();
            String full = CardDeck.detectShape(List.of("9S", "9H", "9D", "4S", "4H")).orElseThrow().shape();
            assertThat(triple).isNotEqualTo(full);
            // Same for four of a rank against four-plus-one.
            String quad = CardDeck.detectShape(List.of("9S", "9H", "9D", "9C")).orElseThrow().shape();
            String quadPlus = CardDeck.detectShape(List.of("9S", "9H", "9D", "9C", "KH")).orElseThrow().shape();
            assertThat(quad).isNotEqualTo(quadPlus);
        }

        @Test
        void threeOrFourMixedRanksAreIllegal() {
            assertThat(CardDeck.detectShape(List.of("9S", "9H", "TD"))).isEmpty();
            assertThat(CardDeck.detectShape(List.of("9S", "9H", "9D", "TC"))).isEmpty();
        }

        @Test
        void fiveCardsWithoutATripleAreIllegal() {
            assertThat(CardDeck.detectShape(List.of("9S", "9H", "TD", "TC", "KH"))).isEmpty();
        }

        @Test
        void emptyOrUnparseableSelectionsAreIllegal() {
            assertThat(CardDeck.detectShape(List.of())).isEmpty();
            assertThat(CardDeck.detectShape(null)).isEmpty();
            assertThat(CardDeck.detectShape(List.of("ZZ"))).isEmpty();
        }
    }

    @Nested
    @DisplayName("beating")
    class BeatingTests {

        /** The user's worked example: exactly these ten ranks answer a lead single 9. */
        @Test
        void leadNineIsBeatenByExactlyTenJQKAFiveThreeTwoJokerSeven() {
            int nine = CardDeck.rankOf("9S");
            List<String> beatsNine = new ArrayList<>();
            for (char rank : CardDeck.RANK_ORDER.toCharArray()) {
                String card = rank == 'X' ? "XB" : rank + "S";
                if (CardDeck.beats(CardDeck.SHAPE_SINGLE, CardDeck.rankOf(card), CardDeck.SHAPE_SINGLE, nine)) {
                    beatsNine.add(String.valueOf(rank));
                }
            }
            assertThat(beatsNine).containsExactly("T", "J", "Q", "K", "A", "5", "3", "2", "X", "7");
        }

        @Test
        void equalRankDoesNotBeatSoThePlayAlreadyDownHolds() {
            int nine = CardDeck.rankOf("9S");
            assertThat(CardDeck.beats(CardDeck.SHAPE_SINGLE, nine, CardDeck.SHAPE_SINGLE, nine)).isFalse();
        }

        @Test
        void lowerRankDoesNotBeat() {
            assertThat(CardDeck.beats(CardDeck.SHAPE_SINGLE, CardDeck.rankOf("8S"), CardDeck.SHAPE_SINGLE, CardDeck.rankOf("9S")))
                .isFalse();
        }

        @Test
        void shapesNeverMeetEachOther() {
            int high = CardDeck.rankOf("7S");
            int low = CardDeck.rankOf("4S");
            // Even a 7-ranked play of the wrong shape cannot take a 4-ranked one.
            assertThat(CardDeck.beats(CardDeck.SHAPE_PAIR, high, CardDeck.SHAPE_SINGLE, low)).isFalse();
            assertThat(CardDeck.beats(CardDeck.SHAPE_SINGLE, high, CardDeck.SHAPE_PAIR, low)).isFalse();
            assertThat(CardDeck.beats(CardDeck.SHAPE_FULL_HOUSE, high, CardDeck.SHAPE_SINGLE, low)).isFalse();
        }

        @Test
        void quadPlusOneDoesNotBeatAFullHouse() {
            // Confirmed by the user: there is no hierarchy between shapes.
            assertThat(CardDeck.beats(CardDeck.SHAPE_QUAD_PLUS_ONE, CardDeck.rankOf("7S"), CardDeck.SHAPE_FULL_HOUSE, CardDeck.rankOf("4S")))
                .isFalse();
        }

        @Test
        void sameShapeHigherRankBeats() {
            assertThat(CardDeck.beats(CardDeck.SHAPE_PAIR, CardDeck.rankOf("7S"), CardDeck.SHAPE_PAIR, CardDeck.rankOf("KS")))
                .isTrue();
            assertThat(CardDeck.beats(CardDeck.SHAPE_FULL_HOUSE, CardDeck.rankOf("2S"), CardDeck.SHAPE_FULL_HOUSE, CardDeck.rankOf("3S")))
                .isTrue();
        }

        @Test
        void anyLegalShapeLeadsAFreshTrick() {
            Play pair = CardDeck.detectShape(List.of("4S", "4H")).orElseThrow();
            assertThat(CardDeck.beats(pair, null, null)).isTrue();
        }
    }

    @Nested
    @DisplayName("strategy")
    class StrategyTests {

        @Test
        void leadPrefersTheBiggestShapeAtItsLowestRank() {
            // A full house of 4s is available alongside singles; dumping five cards wins out.
            Play lead = CardDeck.chooseLead(List.of("4S", "4H", "4D", "9S", "9H")).orElseThrow();
            assertThat(lead.cards()).hasSize(5);
            assertThat(lead.rank()).isEqualTo(CardDeck.rankOf("4S"));
        }

        @Test
        void leadFallsBackToTheWeakestSingle() {
            Play lead = CardDeck.chooseLead(List.of("7S", "9H", "4D")).orElseThrow();
            assertThat(lead.shape()).isEqualTo(CardDeck.SHAPE_SINGLE);
            assertThat(lead.cards()).containsExactly("4D");
        }

        @Test
        void beatPlaysTheCheapestLegalAnswer() {
            Play beat = CardDeck
                .chooseBeat(List.of("7S", "TH", "KD", "4C"), CardDeck.SHAPE_SINGLE, CardDeck.rankOf("9S"))
                .orElseThrow();
            assertThat(beat.cards()).containsExactly("TH"); // 10 is the cheapest card above a 9
        }

        @Test
        void beatIsEmptyWhenNoAnswerExists() {
            // Only a higher single answers a single; a lone pair-mate cannot be split up to help.
            Optional<Play> beat = CardDeck.chooseBeat(List.of("4S", "6H"), CardDeck.SHAPE_SINGLE, CardDeck.rankOf("7S"));
            assertThat(beat).isEmpty();
        }

        @Test
        void beatRespectsShapeSoAPairCannotTakeASingle() {
            Optional<Play> beat = CardDeck.chooseBeat(List.of("7S", "7H"), CardDeck.SHAPE_FULL_HOUSE, CardDeck.rankOf("4S"));
            assertThat(beat).isEmpty();
        }

        @Test
        void legalPlaysFromAHandWithoutFiveCardsHasNoFiveCardShapes() {
            assertThat(CardDeck.legalPlays(List.of("9S", "9H", "9D")))
                .noneMatch(play -> play.cards().size() == 5);
        }

        @Test
        void legalPlaysPadsAFullHouseWithAPairAndNotWithOddCards() {
            // A triple of 9s with only odd cards beside it has no five-card play: the generator
            // used to pad with the two weakest cards regardless, which is not a legal play.
            assertThat(CardDeck.legalPlays(List.of("9S", "9H", "9D", "4S", "KH")))
                .noneMatch(play -> CardDeck.SHAPE_FULL_HOUSE.equals(play.shape()));
            // Given a real pair it takes it, and prefers the weakest pair to give away least.
            Play full = CardDeck
                .legalPlays(List.of("9S", "9H", "9D", "4S", "4H", "KS", "KH"))
                .stream()
                .filter(play -> CardDeck.SHAPE_FULL_HOUSE.equals(play.shape()))
                .findFirst()
                .orElseThrow();
            assertThat(full.rank()).isEqualTo(CardDeck.rankOf("9S"));
            assertThat(full.cards()).containsExactlyInAnyOrder("9S", "9H", "9D", "4S", "4H");
        }

        @Test
        void everyGeneratedPlayIsALegalShape() {
            // The generator assembles five-card plays by hand, so hold it to the same rules the
            // validator applies to a human selection.
            List<String> hand = List.of("9S", "9H", "9D", "9C", "4S", "4H", "KS", "7D", "XS", "5C");
            assertThat(CardDeck.legalPlays(hand))
                .allSatisfy(play -> assertThat(CardDeck.detectShape(play.cards()))
                    .as("generated play %s", play.cards())
                    .isPresent());
        }
    }
}
