package com.example.myapp.web.rest;

import com.example.myapp.service.GomokuService;
import com.example.myapp.service.dto.GomokuGameDTO;
import com.example.myapp.service.dto.GomokuInvitesDTO;
import com.example.myapp.service.dto.GomokuOpponentDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for online Gomoku: opponent discovery, invitations, and gameplay.
 * All endpoints require authentication (see {@code SecurityConfig#anyRequest().authenticated()}).
 */
@RestController
@RequestMapping("/api/v1/gomoku")
public class GomokuResource {

    private final GomokuService gomokuService;

    public GomokuResource(GomokuService gomokuService) {
        this.gomokuService = gomokuService;
    }

    /** Users the caller may invite (mutual follows). */
    @GetMapping("/opponents")
    public ResponseEntity<List<GomokuOpponentDTO>> opponents() {
        return ResponseEntity.ok(gomokuService.listOpponents());
    }

    /** Send an invite to a fan. */
    @PostMapping("/invites")
    public ResponseEntity<GomokuGameDTO> invite(@Valid @RequestBody InviteRequest request) {
        return ResponseEntity.ok(gomokuService.invite(request.opponent()));
    }

    /** The caller's pending invites (incoming + outgoing) — polled by the lobby. */
    @GetMapping("/invites")
    public ResponseEntity<GomokuInvitesDTO> invites() {
        return ResponseEntity.ok(gomokuService.getInvites());
    }

    @PostMapping("/games/{id}/accept")
    public ResponseEntity<GomokuGameDTO> accept(@PathVariable Long id) {
        return ResponseEntity.ok(gomokuService.accept(id));
    }

    @PostMapping("/games/{id}/decline")
    public ResponseEntity<GomokuGameDTO> decline(@PathVariable Long id) {
        return ResponseEntity.ok(gomokuService.decline(id));
    }

    @PostMapping("/games/{id}/cancel")
    public ResponseEntity<GomokuGameDTO> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(gomokuService.cancel(id));
    }

    @PostMapping("/games/{id}/resign")
    public ResponseEntity<GomokuGameDTO> resign(@PathVariable Long id) {
        return ResponseEntity.ok(gomokuService.resign(id));
    }

    @PostMapping("/games/{id}/leave")
    public ResponseEntity<GomokuGameDTO> leave(@PathVariable Long id) {
        return ResponseEntity.ok(gomokuService.leave(id));
    }

    /** Full game state — polled during play. */
    @GetMapping("/games/{id}")
    public ResponseEntity<GomokuGameDTO> game(@PathVariable Long id) {
        return ResponseEntity.ok(gomokuService.getGame(id));
    }

    @PostMapping("/games/{id}/moves")
    public ResponseEntity<GomokuGameDTO> move(@PathVariable Long id, @Valid @RequestBody MoveRequest request) {
        return ResponseEntity.ok(gomokuService.move(id, request.row(), request.col()));
    }

    /** The caller's current active game, if any (used to resume on page load). */
    @GetMapping("/games/active")
    public ResponseEntity<GomokuGameDTO> active() {
        return gomokuService.getActiveGame().map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
    }

    public record InviteRequest(@NotNull String opponent) {}

    public record MoveRequest(@NotNull Integer row, @NotNull Integer col) {}
}
