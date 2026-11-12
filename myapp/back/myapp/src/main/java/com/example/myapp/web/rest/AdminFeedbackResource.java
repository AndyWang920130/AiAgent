package com.example.myapp.web.rest;

import com.example.myapp.service.FeedbackService;
import com.example.myapp.service.dto.FeedbackDTO;
import com.example.myapp.service.dto.FeedbackReplyDTO;
import com.example.myapp.utils.PageUtils;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * The administrator's side of feedback: triage the list, answer a submission, drop spam.
 * Mapped under {@code /api/v1/admin}, which {@code SecurityConfig} already restricts to
 * {@code ROLE_ADMIN} — no new matcher is needed for these paths.
 */
@RestController
@RequestMapping("/api/v1/admin/feedbacks")
public class AdminFeedbackResource {

    private final FeedbackService feedbackService;

    public AdminFeedbackResource(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @GetMapping("")
    public ResponseEntity<List<FeedbackDTO>> list(
        @RequestParam(required = false) String query,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String category,
        @PageableDefault(size = 20, sort = "createdDate", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<FeedbackDTO> page = feedbackService.listForAdmin(query, status, category, pageable);
        HttpHeaders headers = PageUtils.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    @GetMapping("/open-count")
    public Map<String, Long> openCount() {
        return Map.of("count", feedbackService.openCount());
    }

    @PutMapping("/{id}/reply")
    public ResponseEntity<FeedbackDTO> reply(@PathVariable Long id, @Valid @RequestBody FeedbackReplyDTO request) {
        return ResponseEntity.ok(feedbackService.reply(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        feedbackService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
