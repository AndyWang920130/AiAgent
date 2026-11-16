package com.example.myapp.web.rest;

import com.example.myapp.service.ReleaseNoteService;
import com.example.myapp.service.dto.ReleaseNoteDTO;
import com.example.myapp.service.dto.ReleaseNoteSaveDTO;
import com.example.myapp.service.dto.ReleaseNoteStatusDTO;
import com.example.myapp.utils.PageUtils;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * The administrator's side of the release notes: write one, edit it, publish or withdraw it,
 * delete it. Mapped under {@code /api/v1/admin}, which {@code SecurityConfig} already restricts
 * to {@code ROLE_ADMIN} — no new matcher is needed for these paths.
 *
 * <p>The listing here includes drafts, which is the only reason it exists separately from the
 * public {@link ReleaseNoteResource#list}.
 */
@RestController
@RequestMapping("/api/v1/admin/release-notes")
public class AdminReleaseNoteResource {

    private final ReleaseNoteService releaseNoteService;

    public AdminReleaseNoteResource(ReleaseNoteService releaseNoteService) {
        this.releaseNoteService = releaseNoteService;
    }

    @GetMapping("")
    public ResponseEntity<List<ReleaseNoteDTO>> list(@PageableDefault(size = 20) Pageable pageable) {
        Page<ReleaseNoteDTO> page = releaseNoteService.listForAdmin(pageable);
        HttpHeaders headers = PageUtils.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    @PostMapping("")
    public ResponseEntity<ReleaseNoteDTO> create(@Valid @RequestBody ReleaseNoteSaveDTO request) {
        return ResponseEntity.ok(releaseNoteService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ReleaseNoteDTO> update(@PathVariable Long id, @Valid @RequestBody ReleaseNoteSaveDTO request) {
        return ResponseEntity.ok(releaseNoteService.update(id, request));
    }

    /** Publish or withdraw, without resending the body. */
    @PutMapping("/{id}/status")
    public ResponseEntity<ReleaseNoteDTO> setStatus(@PathVariable Long id, @Valid @RequestBody ReleaseNoteStatusDTO request) {
        return ResponseEntity.ok(releaseNoteService.setStatus(id, request.status()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        releaseNoteService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
