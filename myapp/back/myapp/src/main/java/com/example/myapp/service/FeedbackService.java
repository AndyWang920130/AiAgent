package com.example.myapp.service;

import com.example.myapp.contants.enumeration.FeedbackCategory;
import com.example.myapp.contants.enumeration.FeedbackStatus;
import com.example.myapp.contants.enumeration.NotificationType;
import com.example.myapp.contants.enumeration.Role;
import com.example.myapp.domain.Feedback;
import com.example.myapp.domain.User;
import com.example.myapp.repository.FeedbackRepository;
import com.example.myapp.repository.UserRepository;
import com.example.myapp.service.dto.FeedbackCreateDTO;
import com.example.myapp.service.dto.FeedbackDTO;
import com.example.myapp.service.dto.FeedbackReplyDTO;
import com.example.myapp.utils.SecurityUtil;
import com.example.myapp.web.rest.errors.BadRequestAlertException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Feedback sent by a user to the administrators. A submission notifies every active
 * administrator through {@link NotificationService}; an administrator's reply notifies the
 * submitter the same way, so neither side has to poll a page to find out.
 *
 * <p>Submitter-scoped methods key off {@link SecurityUtil#getCurrentUsername()}; the
 * administrator methods take no username and are reachable only under
 * {@code /api/v1/admin/**}, which {@code SecurityConfig} restricts to {@code ROLE_ADMIN}.
 */
@Service
@Transactional
public class FeedbackService {

    private static final Logger LOG = LoggerFactory.getLogger(FeedbackService.class);

    private static final String ENTITY_NAME = "feedback";

    /** Where the client's notification lands: the submitter's list and the triage page. */
    private static final String MY_FEEDBACK_LINK = "/feedback";
    private static final String ADMIN_FEEDBACK_LINK = "/admin/feedbacks";

    /** NEW and IN_PROGRESS both count as open — see {@link FeedbackStatus}. */
    private static final Set<FeedbackStatus> OPEN_STATUSES = Set.of(FeedbackStatus.NEW, FeedbackStatus.IN_PROGRESS);

    /**
     * Open submissions one account may hold at once. A submission fans a notification out to
     * every administrator, so this bounds how much noise one account can make; answering or
     * resolving what is already there frees the allowance up again.
     */
    private static final long OPEN_SUBMISSION_LIMIT = 20;

    private final FeedbackRepository feedbackRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public FeedbackService(
        FeedbackRepository feedbackRepository,
        UserRepository userRepository,
        NotificationService notificationService
    ) {
        this.feedbackRepository = feedbackRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    // ----- Submitter -----------------------------------------------------------------------

    /** Record a submission from the current user and tell every active administrator. */
    public FeedbackDTO submit(FeedbackCreateDTO request) {
        String me = SecurityUtil.getCurrentUsername();
        String title = trimToNull(request.title());
        String content = trimToNull(request.content());
        if (title == null || content == null) {
            throw new BadRequestAlertException("A title and a message are required", ENTITY_NAME, "empty");
        }
        if (feedbackRepository.countByUsernameAndStatusIn(me, OPEN_STATUSES) >= OPEN_SUBMISSION_LIMIT) {
            throw new BadRequestAlertException(
                "You already have " + OPEN_SUBMISSION_LIMIT + " submissions awaiting a reply",
                ENTITY_NAME,
                "toomanyopen"
            );
        }

        Feedback feedback = new Feedback();
        feedback.setUsername(me);
        feedback.setCategory(parseCategory(request.category()));
        feedback.setTitle(title);
        feedback.setContent(content);
        feedback.setContact(trimToNull(request.contact()));
        feedback.setStatus(FeedbackStatus.NEW);
        feedback.setCreatedBy(me);
        feedback.setLastModifiedBy(me);
        Feedback saved = feedbackRepository.save(feedback);

        notifyAdministrators(saved, me);
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public Page<FeedbackDTO> listMine(Pageable pageable) {
        String me = SecurityUtil.getCurrentUsername();
        return withNames(feedbackRepository.findByUsernameOrderByCreatedDateDesc(me, pageable));
    }

    /**
     * Withdraw one's own submission. Allowed only while it is still open: once an
     * administrator has answered, deleting it would strip the reply the submitter was sent.
     */
    public void withdraw(Long id) {
        String me = SecurityUtil.getCurrentUsername();
        Feedback feedback = feedbackRepository.findByIdAndUsername(id, me)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (feedback.getStatus() == FeedbackStatus.RESOLVED) {
            throw new BadRequestAlertException("An answered submission cannot be withdrawn", ENTITY_NAME, "answered");
        }
        feedbackRepository.delete(feedback);
    }

    // ----- Administrator -------------------------------------------------------------------

    /**
     * One page of submissions for the triage list. A blank {@code query} matches everything;
     * a null {@code status} or {@code category} means "any".
     */
    @Transactional(readOnly = true)
    public Page<FeedbackDTO> listForAdmin(String query, String status, String category, Pageable pageable) {
        String q = query == null || query.isBlank() ? "%" : "%" + query.trim().toLowerCase() + "%";
        return withNames(feedbackRepository.findForAdmin(q, parseStatusOrNull(status), parseCategoryOrNull(category), pageable));
    }

    /** Submissions still awaiting attention, for the badge on the triage page. */
    @Transactional(readOnly = true)
    public long openCount() {
        return feedbackRepository.countByStatusIn(OPEN_STATUSES);
    }

    /**
     * Answer and/or move a submission, then tell the submitter. Writing a reply without
     * naming a status resolves it, which is the common case from the triage page.
     */
    public FeedbackDTO reply(Long id, FeedbackReplyDTO request) {
        String me = SecurityUtil.getCurrentUsername();
        Feedback feedback = feedbackRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        String reply = trimToNull(request.reply());
        FeedbackStatus status = parseStatusOrNull(request.status());
        if (reply == null && status == null) {
            throw new BadRequestAlertException("Nothing to apply: give a reply, a status, or both", ENTITY_NAME, "empty");
        }

        if (reply != null) {
            feedback.setReply(reply);
            feedback.setRepliedBy(me);
            feedback.setRepliedDate(Instant.now());
        }
        feedback.setStatus(status != null ? status : FeedbackStatus.RESOLVED);
        feedback.setLastModifiedBy(me);
        Feedback saved = feedbackRepository.save(feedback);

        notifySubmitter(saved, me, reply != null);
        return toDto(saved);
    }

    /** Remove a submission outright. Used for spam, which no reply should be sent for. */
    public void delete(Long id) {
        Feedback feedback = feedbackRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        feedbackRepository.delete(feedback);
    }

    // ----- Internals -----------------------------------------------------------------------

    private void notifyAdministrators(Feedback feedback, String submitter) {
        List<String> admins = userRepository.findActiveLoginsByRole(Role.ADMIN.name());
        if (admins.isEmpty()) {
            // Nothing to do, and nothing is lost: the submission is saved either way and the
            // triage page lists it as soon as somebody holding ADMIN signs in.
            LOG.warn("Feedback {} saved with no active administrator to notify", feedback.getId());
            return;
        }
        String from = nameOf(submitter);
        for (String admin : admins) {
            if (admin.equals(submitter)) {
                // An administrator sending feedback does not need to be told about it.
                continue;
            }
            notificationService.notify(
                admin,
                NotificationType.INFO,
                "New feedback",
                from + " sent feedback: " + feedback.getTitle(),
                ADMIN_FEEDBACK_LINK
            );
        }
    }

    private void notifySubmitter(Feedback feedback, String administrator, boolean replied) {
        String by = nameOf(administrator);
        notificationService.notify(
            feedback.getUsername(),
            feedback.getStatus() == FeedbackStatus.RESOLVED ? NotificationType.SUCCESS : NotificationType.INFO,
            replied ? "Your feedback was answered" : "Your feedback was updated",
            replied
                ? by + " replied to \"" + feedback.getTitle() + "\""
                : by + " set \"" + feedback.getTitle() + "\" to " + feedback.getStatus().name(),
            MY_FEEDBACK_LINK
        );
    }

    /** An unrecognised category is refused rather than silently filed as OTHER. */
    private FeedbackCategory parseCategory(String raw) {
        if (raw == null || raw.isBlank()) {
            return FeedbackCategory.OTHER;
        }
        try {
            return FeedbackCategory.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestAlertException("Unknown feedback category: " + raw, ENTITY_NAME, "badcategory");
        }
    }

    private FeedbackCategory parseCategoryOrNull(String raw) {
        return raw == null || raw.isBlank() ? null : parseCategory(raw);
    }

    private FeedbackStatus parseStatusOrNull(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return FeedbackStatus.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestAlertException("Unknown feedback status: " + raw, ENTITY_NAME, "badstatus");
        }
    }

    /**
     * Map a page to DTOs, resolving every submitter's display name in one query rather than
     * one per row — the triage list is the only place that shows names it did not already
     * have, and a page of 20 would otherwise be 20 lookups.
     */
    private Page<FeedbackDTO> withNames(Page<Feedback> page) {
        Set<String> logins = page.getContent().stream().map(Feedback::getUsername).collect(Collectors.toSet());
        Map<String, User> users = logins.isEmpty()
            ? Map.of()
            : userRepository.findByLoginIn(logins).stream()
                .collect(Collectors.toMap(User::getLogin, Function.identity(), (a, b) -> a));
        return page.map(feedback -> toDto(feedback, users.get(feedback.getUsername())));
    }

    private FeedbackDTO toDto(Feedback feedback) {
        return toDto(feedback, userRepository.findOneByLogin(feedback.getUsername()).orElse(null));
    }

    private FeedbackDTO toDto(Feedback feedback, User submitter) {
        return new FeedbackDTO(
            feedback.getId(),
            feedback.getUsername(),
            submitter == null ? feedback.getUsername() : displayName(submitter),
            feedback.getCategory().name(),
            feedback.getTitle(),
            feedback.getContent(),
            feedback.getContact(),
            feedback.getStatus().name(),
            feedback.getReply(),
            feedback.getRepliedBy(),
            feedback.getRepliedDate(),
            feedback.getCreatedDate()
        );
    }

    private String displayName(User user) {
        if (user.getRealName() != null && !user.getRealName().isBlank()) {
            return user.getRealName();
        }
        if (user.getNickName() != null && !user.getNickName().isBlank()) {
            return user.getNickName();
        }
        return user.getLogin();
    }

    /** Display name for a login (falls back to the login itself), for notification text. */
    private String nameOf(String login) {
        return userRepository.findOneByLogin(login).map(this::displayName).orElse(login);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
