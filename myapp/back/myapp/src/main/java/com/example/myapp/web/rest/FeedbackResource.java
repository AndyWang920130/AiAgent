package com.example.myapp.web.rest;

import com.example.myapp.service.FeedbackService;
import com.example.myapp.service.dto.FeedbackCreateDTO;
import com.example.myapp.service.dto.FeedbackDTO;
import com.example.myapp.utils.PageUtils;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * The submitter's side of feedback: send one, see your own, withdraw one that has not been
 * answered. Every endpoint acts on the authenticated caller (see
 * {@code SecurityConfig#anyRequest().authenticated()}); the administrator's side lives in
 * {@link AdminFeedbackResource} under {@code /api/v1/admin}.
 */
@RestController
@RequestMapping("/api/v1/feedbacks")
public class FeedbackResource {

    private final FeedbackService feedbackService;

    public FeedbackResource(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @PostMapping("")
    public ResponseEntity<FeedbackDTO> submit(@Valid @RequestBody FeedbackCreateDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(feedbackService.submit(request));
    }

    @GetMapping("/mine")
    public ResponseEntity<List<FeedbackDTO>> listMine(
        @PageableDefault(size = 20, sort = "createdDate", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<FeedbackDTO> page = feedbackService.listMine(pageable);
        HttpHeaders headers = PageUtils.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> withdraw(@PathVariable Long id) {
        feedbackService.withdraw(id);
        return ResponseEntity.noContent().build();
    }
}
