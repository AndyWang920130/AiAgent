package com.example.myapp.web.rest;

import com.example.myapp.service.CardGameRecordService;
import com.example.myapp.service.dto.CardGameRecordDTO;
import com.example.myapp.utils.PageUtils;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * The administrator's 70235 match record list: which games were played, when they started and
 * ended, who created them and what each seat scored.
 *
 * <p>Read-only. A record is written by the game itself as a match ends, never through the web
 * layer, so there is nothing here to create or edit.
 *
 * <p>Mapped under {@code /api/v1/admin}, which {@code SecurityConfig} already restricts to
 * {@code ROLE_ADMIN} — no new matcher is needed for this path.
 */
@RestController
@RequestMapping("/api/v1/admin/card-game-records")
public class AdminCardGameRecordResource {

    private final CardGameRecordService cardGameRecordService;

    public AdminCardGameRecordResource(CardGameRecordService cardGameRecordService) {
        this.cardGameRecordService = cardGameRecordService;
    }

    /**
     * @param query   free text matched against the creator, any player, or the winner.
     * @param outcome SCORED or ABANDONED; omit for both.
     */
    @GetMapping("")
    public ResponseEntity<List<CardGameRecordDTO>> list(
        @RequestParam(required = false) String query,
        @RequestParam(required = false) String outcome,
        @PageableDefault(size = 20, sort = "endedDate", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<CardGameRecordDTO> page = cardGameRecordService.listForAdmin(query, outcome, pageable);
        HttpHeaders headers = PageUtils.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }
}
