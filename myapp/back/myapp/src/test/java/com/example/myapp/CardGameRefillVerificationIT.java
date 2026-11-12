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
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Pins the replenishing rule: once a trick is scored the winner draws first and then
 * <em>every</em> other player draws back up to five, whether they played or passed.
 *
 * <p>This is the rule a passing player depends on. If only the players who played refilled,
 * anyone holding something unbeatable would stay stuck on that hand for the rest of the game.
 */
@SpringBootTest
class CardGameRefillVerificationIT {

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

    private CardGamePlayer seat(CardGame game, String username) {
        return playerRepository.findByGameAndUsername(game, username).orElseThrow();
    }

    private int handSize(CardGame game, String username) {
        return CardDeck.parse(seat(game, username).getHand()).size();
    }

    /** The CPU seat, read through the repository — {@code game.getPlayers()} is lazy. */
    private CardGamePlayer cpuSeat(CardGame game) {
        return playerRepository.findByGameOrderBySeatAsc(game).stream()
            .filter(CardGamePlayer::isCpu)
            .findFirst()
            .orElseThrow();
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
     * A two-seat table with hands and deck set by hand, so the trick plays out the same way
     * every run. A table of CPUs would deal randomly and the assertion would depend on luck.
     */
    private CardGame riggedTable(String host, String hostHand, String guestHand, String deck) {
        loginAs(host);
        CardGame game = riggedSeats(host, hostHand, guestHand);

        game.setDeck(deck);
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
    void aPlayerWhoPassesStillRefillsToFive() {
        // The CPU holds a lone 4 — the weakest card in the game — against an ace, so it has to
        // pass. The deck holds enough for both seats to come back up to five.
        CardGame game = riggedTable("refill-passer", "ASKS9S8S7S", "4C", "6H6D6C8H8D8C9H9D");
        Long id = game.getId();
        String cpu = cpuSeat(game).getUsername();

        assertThat(handSize(reload(id), cpu)).isEqualTo(1);

        loginAs("refill-passer");
        // Leading the ace: the CPU cannot beat it, so it passes and the trick comes straight back.
        cardGameService.play(id, List.of("AS"));

        CardGame after = reload(id);
        // Tricks are numbered from 1 at the deal, so the first one resolving takes it to 2.
        assertThat(after.getTrickNumber()).as("the trick resolved").isEqualTo(2);
        assertThat(handSize(after, cpu))
            .as("a seat that passed still draws back up to five")
            .isEqualTo(5);
        assertThat(handSize(after, "refill-passer"))
            .as("the player who played also refills")
            .isEqualTo(5);
    }

    @Test
    void theTrickWinnerDrawsBeforeTheOthers() {
        // Exactly one card left in the deck, and both seats are a card short. The winner of the
        // trick must be the one who gets it.
        CardGame game = riggedTable("refill-winner", "ASKS9S8S", "4C", "7S");
        Long id = game.getId();
        String cpu = cpuSeat(game).getUsername();

        loginAs("refill-winner");
        cardGameService.play(id, List.of("AS"));

        CardGame after = reload(id);
        assertThat(CardDeck.parse(after.getDeck())).as("the last card was drawn").isEmpty();
        assertThat(seat(after, "refill-winner").getHand())
            .as("the trick winner draws first and so takes the last card")
            .contains("7S");
        assertThat(handSize(after, cpu))
            .as("the loser gets nothing left to draw")
            .isEqualTo(1);
    }
}
