package com.example.myapp.web.rest;

import com.example.myapp.contants.enumeration.CardGamePhrase;
import com.example.myapp.service.CardGameService;
import com.example.myapp.service.dto.CardGameDTO;
import com.example.myapp.service.dto.CardGameInvitesDTO;
import com.example.myapp.service.dto.CardGameOpponentDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for the online 70235 card game: follower discovery, table setup,
 * invitations and gameplay. All endpoints require authentication (see
 * {@code SecurityConfig#anyRequest().authenticated()}) and are scoped to the caller — every
 * response is built for the requesting player, so no hand is ever disclosed to anyone else.
 */
@RestController
@RequestMapping("/api/v1/card-games")
public class CardGameResource {

    private final CardGameService cardGameService;

    public CardGameResource(CardGameService cardGameService) {
        this.cardGameService = cardGameService;
    }

    /** Users the caller may invite: their followers (one-way follow is enough). */
    @GetMapping("/opponents")
    public ResponseEntity<List<CardGameOpponentDTO>> opponents() {
        return ResponseEntity.ok(cardGameService.listOpponents());
    }

    /** Create a table seating the host and the invited followers. */
    @PostMapping("/tables")
    public ResponseEntity<CardGameDTO> createTable(@Valid @RequestBody TableRequest request) {
        return ResponseEntity.ok(cardGameService.createTable(request.invitees()));
    }

    /** The caller's pending tables (incoming invitations + tables they host) — polled by the lobby. */
    @GetMapping("/invites")
    public ResponseEntity<CardGameInvitesDTO> invites() {
        return ResponseEntity.ok(cardGameService.getInvites());
    }

    @PostMapping("/games/{id}/accept")
    public ResponseEntity<CardGameDTO> accept(@PathVariable Long id) {
        return ResponseEntity.ok(cardGameService.accept(id));
    }

    @PostMapping("/games/{id}/decline")
    public ResponseEntity<CardGameDTO> decline(@PathVariable Long id) {
        return ResponseEntity.ok(cardGameService.decline(id));
    }

    @PostMapping("/games/{id}/cancel")
    public ResponseEntity<CardGameDTO> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(cardGameService.cancel(id));
    }

    /** Host deals and begins play. */
    @PostMapping("/games/{id}/start")
    public ResponseEntity<CardGameDTO> start(@PathVariable Long id) {
        return ResponseEntity.ok(cardGameService.start(id));
    }

    /** Play a selection of cards, e.g. {@code {"cards": ["TS", "TH"]}}. */
    @PostMapping("/games/{id}/plays")
    public ResponseEntity<CardGameDTO> play(@PathVariable Long id, @Valid @RequestBody PlayRequest request) {
        return ResponseEntity.ok(cardGameService.play(id, request.cards()));
    }

    @PostMapping("/games/{id}/pass")
    public ResponseEntity<CardGameDTO> pass(@PathVariable Long id) {
        return ResponseEntity.ok(cardGameService.pass(id));
    }

    /**
     * Hand the caller's turns to the server, or take them back. Scoped to the caller's own
     * seat — there is no parameter for whose seat to set.
     */
    @PostMapping("/games/{id}/auto-play")
    public ResponseEntity<CardGameDTO> autoPlay(@PathVariable Long id, @Valid @RequestBody AutoPlayRequest request) {
        return ResponseEntity.ok(cardGameService.setAutoPlay(id, request.enabled()));
    }

    /**
     * Say whether the caller wants the next game, once this one has been scored. Caller's own
     * seat only, like auto-play — the host's included, which is why there is no separate
     * endpoint for dealing again: the response to the last player readying up is the next game
     * already dealt.
     */
    @PostMapping("/games/{id}/ready")
    public ResponseEntity<CardGameDTO> ready(@PathVariable Long id, @Valid @RequestBody ReadyRequest request) {
        return ResponseEntity.ok(cardGameService.setReady(id, request.ready()));
    }

    /**
     * Say one of the predefined phrases to the table, e.g. {@code {"phrase": "NICE_PLAY"}}.
     * Caller's own seat only, like auto-play and readiness.
     */
    @PostMapping("/games/{id}/phrases")
    public ResponseEntity<CardGameDTO> phrase(@PathVariable Long id, @Valid @RequestBody PhraseRequest request) {
        return ResponseEntity.ok(cardGameService.sendPhrase(id, request.phrase()));
    }

    /** Leave the table, ending the game or closing the room for everyone still in it. */
    @PostMapping("/games/{id}/leave")
    public ResponseEntity<CardGameDTO> leave(@PathVariable Long id) {
        return ResponseEntity.ok(cardGameService.leave(id));
    }

    /** Full state for the caller — polled during play. */
    @GetMapping("/games/{id}")
    public ResponseEntity<CardGameDTO> game(@PathVariable Long id) {
        return ResponseEntity.ok(cardGameService.getGame(id));
    }

    /** The caller's current active game, if any (used to resume on page load). */
    @GetMapping("/games/active")
    public ResponseEntity<CardGameDTO> active() {
        return cardGameService.getActiveGame().map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
    }

    /**
     * Invitees only. This carried a {@code cpuCount} until CPU seats were withdrawn; an old
     * client still sending one is ignored rather than rejected, since a 400 on table creation
     * is a worse answer than a table of the humans they picked.
     */
    public record TableRequest(List<String> invitees) {}

    public record PlayRequest(@NotEmpty List<String> cards) {}

    public record AutoPlayRequest(@NotNull Boolean enabled) {}

    public record ReadyRequest(@NotNull Boolean ready) {}

    /**
     * Binds straight to the enum rather than taking a {@code String} the service then looks up.
     * That is what rejects a code nobody put in the picker, at the edge, before it can be stored
     * and become a problem for whoever has to render it — and it is the whole of what makes
     * these phrases predefined, since the picker itself constrains nothing a crafted POST has to
     * respect. An unparseable value is a 400 from the message converter.
     */
    public record PhraseRequest(@NotNull CardGamePhrase phrase) {}
}
