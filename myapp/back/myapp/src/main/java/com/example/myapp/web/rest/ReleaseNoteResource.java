package com.example.myapp.web.rest;

import com.example.myapp.service.ReleaseNoteService;
import com.example.myapp.service.dto.ReleaseNoteDTO;
import com.example.myapp.utils.PageUtils;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * The read side of the release notes, for any signed-in user. Published notes only — drafts
 * are excluded by the query behind {@link ReleaseNoteService#listPublished}, not filtered out
 * here.
 *
 * <p>Writing lives on {@link AdminReleaseNoteResource} under {@code /api/v1/admin}, which
 * {@code SecurityConfig} already restricts to {@code ROLE_ADMIN}. Splitting read from write by
 * path is what lets both sides rely on rules that already exist: this one falls under
 * {@code .anyRequest().authenticated()}, and no new matcher is involved on either side.
 */
@RestController
@RequestMapping("/api/v1/release-notes")
public class ReleaseNoteResource {

    private final ReleaseNoteService releaseNoteService;

    public ReleaseNoteResource(ReleaseNoteService releaseNoteService) {
        this.releaseNoteService = releaseNoteService;
    }

    @GetMapping("")
    public ResponseEntity<List<ReleaseNoteDTO>> list(@PageableDefault(size = 20) Pageable pageable) {
        Page<ReleaseNoteDTO> page = releaseNoteService.listPublished(pageable);
        HttpHeaders headers = PageUtils.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * The newest published version, for the About page. A map rather than a bare string so the
     * body is JSON either way, including when nothing is published yet.
     *
     * <p>{@code singletonMap} rather than the {@code Map.of} used elsewhere in these resources:
     * {@code Map.of} throws on a null value, and null is the normal answer here until the first
     * note is published.
     */
    @GetMapping("/current-version")
    public Map<String, String> currentVersion() {
        return Collections.singletonMap("version", releaseNoteService.currentVersion());
    }
}
