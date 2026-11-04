package com.example.myapp.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The 70235 (七王五二三) rules engine: card encoding, ranking, shape detection, beating and
 * point values, plus the opponent strategy used for CPU seats and turn-timeout auto-play.
 *
 * <p>Pure static logic with no Spring or persistence involvement, so the rules can be unit
 * tested directly (see {@code CardDeckTest}) and {@code CardGameService} is left to deal only
 * with lifecycle and storage.
 *
 * <h2>Encoding</h2>
 * Two characters per card, rank then suit, so any card string is read by chunking into pairs:
 * ranks {@code 4 6 8 9 T J Q K A 5 3 2 7} ({@code T} = 10) plus {@code X} for a joker; suits
 * {@code S H D C}, or {@code B}/{@code L} in the suit slot for the big/little joker.
 *
 * <h2>Ranking</h2>
 * Weakest to strongest: {@code 4 6 8 9 10 J Q K A 5 3 2 joker 7}. The game's name is this
 * order read from the top: 7, joker (shown as 0), 2, 3, 5. Both jokers rank equally.
 *
 * <h2>Points</h2>
 * {@code 5} scores 5, {@code 10} and {@code K} score 10 each — 100 per 54-card pack. Jokers
 * are powerful but score nothing.
 */
public final class CardDeck {

    /** Rank characters weakest to strongest; the index into this string is the strength rank. */
    public static final String RANK_ORDER = "4689TJQKA532X7";

    /** Suits used for the 52 standard cards. */
    private static final String SUITS = "SHDC";

    /** Joker suit slots: big and little. They rank equally — the distinction is cosmetic. */
    private static final char BIG_JOKER = 'B';
    private static final char LITTLE_JOKER = 'L';

    /** Below this player count the game uses one pack; at it and above, two. */
    private static final int TWO_PACK_THRESHOLD = 3;

    public static final String SHAPE_SINGLE = "SINGLE";
    public static final String SHAPE_PAIR = "PAIR";
    /** Three of a rank on their own. Distinct from {@link #SHAPE_FULL_HOUSE}, which pads it. */
    public static final String SHAPE_TRIPLE = "TRIPLE";
    /** Four of a rank on their own. Distinct from {@link #SHAPE_QUAD_PLUS_ONE}. */
    public static final String SHAPE_QUAD = "QUAD";
    public static final String SHAPE_FULL_HOUSE = "FULL_HOUSE";
    public static final String SHAPE_QUAD_PLUS_ONE = "QUAD_PLUS_ONE";

    private CardDeck() {}

    /** A detected legal play: its shape and the rank that comparisons use. */
    public record Play(String shape, int rank, List<String> cards) {}

    // ----- Deck ---------------------------------------------------------------------------

    /**
     * A shuffled deck: one 54-card pack for a two-player game, two packs (108 cards) for
     * three to five players.
     */
    public static List<String> buildDeck(int playerCount) {
        int packs = playerCount >= TWO_PACK_THRESHOLD ? 2 : 1;
        List<String> cards = new ArrayList<>(packs * 54);
        for (int pack = 0; pack < packs; pack++) {
            for (char rank : RANK_ORDER.toCharArray()) {
                if (rank == 'X') {
                    continue; // jokers are added explicitly below — they have no suit
                }
                for (char suit : SUITS.toCharArray()) {
                    cards.add("" + rank + suit);
                }
            }
            cards.add("X" + BIG_JOKER);
            cards.add("X" + LITTLE_JOKER);
        }
        Collections.shuffle(cards);
        return cards;
    }

    /** Split a stored card string into individual two-character cards. */
    public static List<String> parse(String cards) {
        if (cards == null || cards.isBlank()) {
            return new ArrayList<>();
        }
        List<String> out = new ArrayList<>(cards.length() / 2);
        for (int i = 0; i + 1 < cards.length(); i += 2) {
            out.add(cards.substring(i, i + 2));
        }
        return out;
    }

    /** Join cards back into a stored string. */
    public static String join(List<String> cards) {
        return String.join("", cards);
    }

    // ----- Ranking and points -------------------------------------------------------------

    /** Strength of a card, 0 (weakest, a 4) to 13 (strongest, a 7). -1 for an unknown rank. */
    public static int rankOf(String card) {
        if (card == null || card.length() < 1) {
            return -1;
        }
        return RANK_ORDER.indexOf(card.charAt(0));
    }

    /** True when the card is a joker (either one). */
    public static boolean isJoker(String card) {
        return card != null && !card.isEmpty() && card.charAt(0) == 'X';
    }

    /** Point value of a single card: 5 → 5, 10 → 10, K → 10, everything else 0. */
    public static int pointsOf(String card) {
        if (card == null || card.isEmpty()) {
            return 0;
        }
        return switch (card.charAt(0)) {
            case '5' -> 5;
            case 'T', 'K' -> 10;
            default -> 0;
        };
    }

    /** Total point value of a list of cards. */
    public static int pointsOf(List<String> cards) {
        int total = 0;
        for (String card : cards) {
            total += pointsOf(card);
        }
        return total;
    }

    /** Cards sorted strongest first — the order a hand is presented in. */
    public static List<String> sortByStrength(List<String> cards) {
        List<String> sorted = new ArrayList<>(cards);
        sorted.sort((a, b) -> Integer.compare(rankOf(b), rankOf(a)));
        return sorted;
    }

    // ----- Shapes -------------------------------------------------------------------------

    /**
     * Identify the legal play a selection forms, or empty if it is not one.
     *
     * <p>Legal shapes: a single; a pair of equal rank; a full house (three of a kind plus two
     * other cards, compared on the triple's rank); four of a kind plus one other card
     * (compared on the quad's rank). The extra cards in the two five-card shapes are
     * arbitrary — they need not pair up. With two packs a selection can hold five of one
     * rank, which counts as four of a kind plus one.
     */
    public static Optional<Play> detectShape(List<String> cards) {
        if (cards == null || cards.isEmpty()) {
            return Optional.empty();
        }
        for (String card : cards) {
            if (rankOf(card) < 0) {
                return Optional.empty(); // unparseable card
            }
        }
        Map<Integer, Integer> counts = new LinkedHashMap<>();
        for (String card : cards) {
            counts.merge(rankOf(card), 1, Integer::sum);
        }
        if (cards.size() == 1) {
            return Optional.of(new Play(SHAPE_SINGLE, rankOf(cards.get(0)), cards));
        }
        // Two, three and four of one rank are each a shape in their own right. One rank across
        // the whole selection is the check; mixed ranks at these sizes are not a play.
        if (cards.size() >= 2 && cards.size() <= 4) {
            if (counts.size() != 1) {
                return Optional.empty();
            }
            String shape = switch (cards.size()) {
                case 2 -> SHAPE_PAIR;
                case 3 -> SHAPE_TRIPLE;
                default -> SHAPE_QUAD;
            };
            return Optional.of(new Play(shape, rankOf(cards.get(0)), cards));
        }
        if (cards.size() == 5) {
            // Four of a kind takes priority: five cards of one rank (possible with two packs)
            // is a quad plus one, not a full house. Its fifth card is a free kicker.
            Optional<Integer> quad = rankWithAtLeast(counts, 4);
            if (quad.isPresent()) {
                return Optional.of(new Play(SHAPE_QUAD_PLUS_ONE, quad.get(), cards));
            }
            // Three plus two: the two must be a genuine pair, not two odd cards. With no rank
            // appearing four times, exactly two distinct ranks across five cards can only be a
            // triple and a pair, so the rank count is the whole check.
            Optional<Integer> triple = rankWithAtLeast(counts, 3);
            if (triple.isPresent() && counts.size() == 2) {
                return Optional.of(new Play(SHAPE_FULL_HOUSE, triple.get(), cards));
            }
        }
        return Optional.empty();
    }

    /** The highest rank appearing at least {@code n} times, if any. */
    private static Optional<Integer> rankWithAtLeast(Map<Integer, Integer> counts, int n) {
        return counts.entrySet().stream()
            .filter(e -> e.getValue() >= n)
            .map(Map.Entry::getKey)
            .max(Integer::compareTo);
    }

    /**
     * Whether a candidate play beats the one on the table: the same shape at a strictly
     * higher rank. Shapes never meet each other, so four of a kind plus one does not take a
     * full house, and equal ranks do not beat — the play already down holds.
     */
    public static boolean beats(String candidateShape, int candidateRank, String currentShape, int currentRank) {
        return candidateShape.equals(currentShape) && candidateRank > currentRank;
    }

    /** Whether a play beats the current table state, treating a null shape as a free lead. */
    public static boolean beats(Play candidate, String currentShape, Integer currentRank) {
        if (currentShape == null || currentRank == null) {
            return true; // leading a fresh trick: any legal shape goes
        }
        return beats(candidate.shape(), candidate.rank(), currentShape, currentRank);
    }

    // ----- Strategy (CPU seats, and auto-play on turn timeout) -----------------------------

    /**
     * Every legal play available from a hand. Used by the strategy below; also the basis for
     * deciding whether a player has any answer to the current table state.
     */
    public static List<Play> legalPlays(List<String> hand) {
        List<Play> plays = new ArrayList<>();
        List<String> sorted = sortByStrength(hand);
        // Singles.
        for (String card : sorted) {
            plays.add(new Play(SHAPE_SINGLE, rankOf(card), List.of(card)));
        }
        // Group by rank so pairs and the five-card shapes can be assembled.
        Map<Integer, List<String>> byRank = new LinkedHashMap<>();
        for (String card : sorted) {
            byRank.computeIfAbsent(rankOf(card), k -> new ArrayList<>()).add(card);
        }
        for (Map.Entry<Integer, List<String>> entry : byRank.entrySet()) {
            List<String> group = entry.getValue();
            // Two, three and four of a rank each stand alone, so a group of four offers a pair,
            // a triple and a quad — a smaller shape may be the only one that answers the table.
            if (group.size() >= 2) {
                plays.add(new Play(SHAPE_PAIR, entry.getKey(), List.of(group.get(0), group.get(1))));
            }
            if (group.size() >= 3) {
                plays.add(new Play(SHAPE_TRIPLE, entry.getKey(), List.copyOf(group.subList(0, 3))));
            }
            if (group.size() >= 4) {
                plays.add(new Play(SHAPE_QUAD, entry.getKey(), List.copyOf(group.subList(0, 4))));
            }
            // A five-card shape needs the set plus the right padding: a quad takes any single
            // kicker, but three plus two needs a real pair, so a triple with only odd cards
            // beside it has no five-card play at all.
            if (group.size() >= 3 && hand.size() >= 5) {
                List<String> cards;
                if (group.size() >= 4) {
                    cards = new ArrayList<>(group.subList(0, 4));
                    weakestOtherRank(sorted, entry.getKey(), 1).forEach(cards::add);
                } else {
                    cards = new ArrayList<>(group.subList(0, 3));
                    weakestOtherRank(sorted, entry.getKey(), 2).forEach(cards::add);
                }
                // Built by hand, so confirm it against the rules rather than trusting the
                // assembly: whatever detectShape says this is, is what it is.
                if (cards.size() == 5) {
                    detectShape(cards).ifPresent(plays::add);
                }
            }
        }
        return plays;
    }

    /**
     * {@code count} cards of one rank other than {@code excludedRank}, taken from the weakest
     * such rank available, or empty when no single rank can supply that many.
     *
     * <p>This is the padding for a five-card shape. The padding never affects what the play
     * beats — a full house is compared on its triple — so the cheapest cards that satisfy the
     * shape are the right ones to give away. Asking for two returns a pair, which is why a
     * triple sitting beside two odd cards yields no play here.
     *
     * @param sorted the hand, strongest first
     */
    private static List<String> weakestOtherRank(List<String> sorted, int excludedRank, int count) {
        Map<Integer, List<String>> byRank = new LinkedHashMap<>();
        for (String card : sorted) {
            if (rankOf(card) != excludedRank) {
                byRank.computeIfAbsent(rankOf(card), k -> new ArrayList<>()).add(card);
            }
        }
        List<String> weakest = null;
        for (Map.Entry<Integer, List<String>> entry : byRank.entrySet()) {
            List<String> group = entry.getValue();
            if (group.size() >= count && (weakest == null || entry.getKey() < rankOf(weakest.get(0)))) {
                weakest = group;
            }
        }
        return weakest == null ? List.of() : List.copyOf(weakest.subList(0, count));
    }

    /**
     * What to lead: the largest shape available at its lowest rank. Shedding several cards at
     * once empties a hand faster, and hands refill to five anyway, so the cheap high-volume
     * play is the right default. A competent beginner, not a solver.
     */
    public static Optional<Play> chooseLead(List<String> hand) {
        return legalPlays(hand).stream()
            .min((a, b) -> {
                int bySize = Integer.compare(b.cards().size(), a.cards().size()); // bigger shape first
                return bySize != 0 ? bySize : Integer.compare(a.rank(), b.rank()); // then weakest
            });
    }

    /** The cheapest legal answer to the play on the table, or empty when there is none (pass). */
    public static Optional<Play> chooseBeat(List<String> hand, String currentShape, int currentRank) {
        return legalPlays(hand).stream()
            .filter(play -> beats(play.shape(), play.rank(), currentShape, currentRank))
            .min((a, b) -> Integer.compare(a.rank(), b.rank()));
    }
}
