package com.example.myapp;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.myapp.contants.enumeration.CardGameInvitationStatus;
import com.example.myapp.contants.enumeration.CardGameStatus;
import com.example.myapp.domain.CardGame;
import com.example.myapp.domain.CardGamePlayer;
import com.example.myapp.domain.Notification;
import com.example.myapp.domain.User;
import com.example.myapp.domain.UserFollow;
import com.example.myapp.repository.CardGamePlayerRepository;
import com.example.myapp.repository.CardGameRepository;
import com.example.myapp.repository.NotificationRepository;
import com.example.myapp.repository.UserFollowRepository;
import com.example.myapp.repository.UserRepository;
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
 * Covers what the client needs to show a result screen, and where an invitation notification
 * sends its recipient.
 *
 * <p>The result screen names the winner and lists every player's final score, so the response
 * for a finished game has to carry a winner, the player who went out first, and a score for
 * each seat. A game that ends without a winner (somebody left) still has to carry the scores,
 * because the screen shows the standings either way.
 */
@SpringBootTest
class CardGameResultVerificationIT {

    @Autowired
    private CardGameService cardGameService;

    @Autowired
    private CardGameRepository gameRepository;

    @Autowired
    private CardGamePlayerRepository playerRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserFollowRepository userFollowRepository;

    private void loginAs(String username) {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(username, null, List.of())
        );
    }

    /** A saved user, so an invitation has somebody real to go to. */
    private void user(String login) {
        userRepository.save(new User()
            .login(login)
            .realName(login)
            .nickName(login)
            .email(login + "@example.com")
            .password("not-used-here")
            .deleted(false));
    }

    private void follow(String follower, String following) {
        UserFollow link = new UserFollow();
        link.setFollowerUsername(follower);
        link.setFollowingUsername(following);
        link.setFollowedDate(Instant.now());
        userFollowRepository.save(link);
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
     *
     * <p>{@link #anInvitationNotificationLinksToTheGameItself} is deliberately not built this
     * way: it is about what the service does when a table is created, so it has to use the real
     * path.
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
     * A two-seat table against a CPU, rigged so the host plays its last card on the next play
     * and the game ends. An empty deck is what makes the end condition reachable: with cards
     * left to draw, the hand would be refilled instead.
     */
    private CardGame riggedEndgame(String host, String hostHand, String cpuHand) {
        loginAs(host);
        CardGame game = riggedSeats(host, hostHand, cpuHand);

        game.setDeck(""); // drained, so nobody refills and going out is final
        game.setStatus(CardGameStatus.ACTIVE);
        game.setStartedDate(Instant.now());
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

    /**
     * A finished game reports a winner and a score for every seat. Without these the result
     * screen has nothing to show: it names the winner from {@code winnerUsername} and builds the
     * standings from each player's {@code score}.
     */
    @Test
    void aFinishedGameReportsAWinnerAndEveryPlayersScore() {
        // The host holds the 7, the strongest card in the game, and nothing else. Playing it
        // empties their hand, so they go out first and collect the CPU's leftover points. The
        // CPU holds a King, which is worth 10, so the leftover total is not zero and the
        // collection is observable rather than vacuous.
        CardGame game = riggedEndgame("result-winner", "7S", "KH");
        Long id = game.getId();

        loginAs("result-winner");
        CardGameDTO finished = cardGameService.play(id, List.of("7S"));

        assertThat(finished.status()).as("playing the last card ended the game").isEqualTo("FINISHED");
        assertThat(finished.winnerUsername()).as("a finished game names its winner").isNotNull();
        assertThat(finished.firstOutUsername())
            .as("and names who emptied their hand first")
            .isEqualTo("result-winner");

        // Every seat carries a score, which is what the standings list renders.
        assertThat(finished.players())
            .as("every seat is reported with a score the standings can show")
            .hasSize(2)
            .allSatisfy(p -> assertThat(p.score()).isGreaterThanOrEqualTo(0));

        // The winner must be one of the seats, or the client cannot resolve it to a name.
        List<String> seated = finished.players().stream().map(CardGamePlayerDTO::username).toList();
        assertThat(seated).as("the winner is a seat at the table").contains(finished.winnerUsername());

        // Going out first collected the King left in the CPU's hand, so the host's score covers
        // those 10 points and the host is the winner on score.
        CardGamePlayerDTO host = finished.players().stream()
            .filter(p -> "result-winner".equals(p.username()))
            .findFirst()
            .orElseThrow();
        assertThat(host.score())
            .as("the player who went out first collected the 10 points left in the other hand")
            .isGreaterThanOrEqualTo(10);
        assertThat(finished.winnerUsername()).isEqualTo("result-winner");
    }

    /**
     * A game nobody won still reports the scores. Leaving an active table abandons it without
     * running the scoring, so there is no winner — but the standings are still shown, and a
     * response without scores would render an empty result screen.
     */
    @Test
    void anAbandonedGameReportsScoresWithoutAWinner() {
        CardGame game = riggedEndgame("result-abandon", "7S9S", "KH");
        Long id = game.getId();

        loginAs("result-abandon");
        CardGameDTO left = cardGameService.leave(id);

        assertThat(left.status()).isEqualTo("ABANDONED");
        assertThat(left.winnerUsername()).as("nobody won a game that was walked away from").isNull();
        assertThat(left.players())
            .as("the standings still have every seat to show")
            .hasSize(2)
            .allSatisfy(p -> {
                assertThat(p.name()).as("a row needs a name to display").isNotBlank();
                assertThat(p.score()).as("and a score").isGreaterThanOrEqualTo(0);
            });
    }

    /**
     * An invitation notification links to the 70235 table, not the card-game list. The link is
     * what the recipient clicks to answer the invitation, so pointing it at the list page leaves
     * them to find the game themselves — and the list page cannot show an invitation at all.
     */
    @Test
    void anInvitationNotificationLinksToTheGameItself() {
        user("result-host");
        user("result-guest");
        follow("result-guest", "result-host"); // only a follower can be invited

        loginAs("result-host");
        CardGameDTO table = cardGameService.createTable(List.of("result-guest"));
        assertThat(table.status()).as("an invited table waits rather than dealing").isEqualTo("PENDING");

        List<Notification> invites = notificationRepository.findByRecipientAndReadFalse("result-guest");
        assertThat(invites).as("the invitee was notified").isNotEmpty();
        assertThat(invites)
            .as("the notification links to the 70235 table, so one click reaches the invitation")
            .allSatisfy(n -> assertThat(n.getLink()).isEqualTo("/mini-game/card-games/70235"));

        // Accepting puts the guest in the table's own pending view, which is what that link
        // now opens onto: the game is still PENDING and the guest is seated in it.
        loginAs("result-guest");
        CardGameDTO accepted = cardGameService.accept(table.id());
        assertThat(accepted.status()).as("accepting seats the guest at a table not yet started").isEqualTo("PENDING");
        assertThat(accepted.players())
            .as("and the guest is one of its seats, so the table renders for them")
            .anySatisfy(p -> {
                assertThat(p.username()).isEqualTo("result-guest");
                assertThat(p.invitationStatus()).isEqualTo("ACCEPTED");
            });

        // The host is told, and that notification points at the table too.
        List<Notification> hostNotices = notificationRepository.findByRecipientAndReadFalse("result-host");
        assertThat(hostNotices)
            .as("the host's notification also opens the table")
            .isNotEmpty()
            .allSatisfy(n -> assertThat(n.getLink()).isEqualTo("/mini-game/card-games/70235"));
    }
}
