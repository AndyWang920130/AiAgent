package com.example.myapp.service;

import com.example.myapp.contants.enumeration.ReleaseNoteStatus;
import com.example.myapp.domain.ReleaseNote;
import com.example.myapp.domain.User;
import com.example.myapp.repository.ReleaseNoteRepository;
import com.example.myapp.repository.UserRepository;
import com.example.myapp.service.dto.ReleaseNoteDTO;
import com.example.myapp.service.dto.ReleaseNoteSaveDTO;
import com.example.myapp.utils.SecurityUtil;
import com.example.myapp.web.rest.errors.BadRequestAlertException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * The About section's release notes. Administrators write them; every signed-in user reads the
 * published ones.
 *
 * <p>The read methods come in pairs — {@link #listPublished} for anybody and
 * {@link #listForAdmin} for the editing page — and the difference is enforced by the query,
 * not by filtering afterwards. The write methods take no username and are reachable only
 * under {@code /api/v1/admin/**}, which {@code SecurityConfig} restricts to {@code ROLE_ADMIN}.
 */
@Service
@Transactional
public class ReleaseNoteService {

    private static final String ENTITY_NAME = "releaseNote";

    /**
     * What the rich editor sends for an empty body. {@code @NotBlank} passes it, so it is
     * checked here — otherwise an entry with no note at all can be saved.
     */
    private static final String EMPTY_EDITOR_BODY = "<p></p>";

    private final ReleaseNoteRepository releaseNoteRepository;
    private final UserRepository userRepository;

    public ReleaseNoteService(ReleaseNoteRepository releaseNoteRepository, UserRepository userRepository) {
        this.releaseNoteRepository = releaseNoteRepository;
        this.userRepository = userRepository;
    }

    // ----- Reading --------------------------------------------------------------------------

    /** Published notes, newest release first. What a non-administrator sees. */
    @Transactional(readOnly = true)
    public Page<ReleaseNoteDTO> listPublished(Pageable pageable) {
        return withNames(releaseNoteRepository.findByStatusOrderByReleasedDateDesc(ReleaseNoteStatus.PUBLISHED, pageable));
    }

    /** Every note including drafts, newest release first. The editing page's list. */
    @Transactional(readOnly = true)
    public Page<ReleaseNoteDTO> listForAdmin(Pageable pageable) {
        return withNames(releaseNoteRepository.findAllByOrderByReleasedDateDesc(pageable));
    }

    /**
     * The newest published version, for the About page. Null when nothing is published yet —
     * the client shows a placeholder rather than a wrong number.
     */
    @Transactional(readOnly = true)
    public String currentVersion() {
        return releaseNoteRepository
            .findFirstByStatusOrderByReleasedDateDesc(ReleaseNoteStatus.PUBLISHED)
            .map(ReleaseNote::getVersion)
            .orElse(null);
    }

    // ----- Writing --------------------------------------------------------------------------

    public ReleaseNoteDTO create(ReleaseNoteSaveDTO request) {
        String me = SecurityUtil.getCurrentUsername();
        String version = requireVersion(request);
        if (releaseNoteRepository.existsByVersion(version)) {
            throw new BadRequestAlertException("Version " + version + " already has a release note", ENTITY_NAME, "versionexists");
        }

        ReleaseNote note = new ReleaseNote();
        apply(note, request, version);
        note.setCreatedBy(me);
        note.setLastModifiedBy(me);
        return toDto(releaseNoteRepository.save(note));
    }

    public ReleaseNoteDTO update(Long id, ReleaseNoteSaveDTO request) {
        String me = SecurityUtil.getCurrentUsername();
        ReleaseNote note = releaseNoteRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        String version = requireVersion(request);
        // The row keeping its own version is not a duplicate; another row holding it is.
        Optional<ReleaseNote> clash = releaseNoteRepository.findByVersion(version);
        if (clash.isPresent() && !clash.get().getId().equals(id)) {
            throw new BadRequestAlertException("Version " + version + " already has a release note", ENTITY_NAME, "versionexists");
        }

        apply(note, request, version);
        note.setLastModifiedBy(me);
        return toDto(releaseNoteRepository.save(note));
    }

    /** Publish or withdraw a note. The list's one-click action; see {@code ReleaseNoteStatusDTO}. */
    public ReleaseNoteDTO setStatus(Long id, String rawStatus) {
        String me = SecurityUtil.getCurrentUsername();
        ReleaseNote note = releaseNoteRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        note.setStatus(parseStatus(rawStatus));
        note.setLastModifiedBy(me);
        return toDto(releaseNoteRepository.save(note));
    }

    public void delete(Long id) {
        ReleaseNote note = releaseNoteRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        releaseNoteRepository.delete(note);
    }

    // ----- Internals ------------------------------------------------------------------------

    /** Copy the editable fields across. Authorship and status parsing are handled by callers. */
    private void apply(ReleaseNote note, ReleaseNoteSaveDTO request, String version) {
        note.setVersion(version);
        note.setTitle(trimToNull(request.title()));
        note.setContent(requireContent(request));
        note.setReleasedDate(request.releasedDate());
        note.setStatus(request.status() == null || request.status().isBlank()
            ? ReleaseNoteStatus.DRAFT
            : parseStatus(request.status()));
    }

    private String requireVersion(ReleaseNoteSaveDTO request) {
        String version = trimToNull(request.version());
        if (version == null) {
            throw new BadRequestAlertException("A version is required", ENTITY_NAME, "emptyversion");
        }
        return version;
    }

    private String requireContent(ReleaseNoteSaveDTO request) {
        String content = trimToNull(request.content());
        if (content == null || EMPTY_EDITOR_BODY.equals(content)) {
            throw new BadRequestAlertException("A release note cannot be empty", ENTITY_NAME, "emptycontent");
        }
        return content;
    }

    /** An unrecognised status is refused rather than quietly filed as a draft. */
    private ReleaseNoteStatus parseStatus(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new BadRequestAlertException("A status is required", ENTITY_NAME, "emptystatus");
        }
        try {
            return ReleaseNoteStatus.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestAlertException("Unknown release note status: " + raw, ENTITY_NAME, "badstatus");
        }
    }

    /**
     * Map a page to DTOs, resolving every author's display name in one query rather than one
     * per row — the same reason {@code FeedbackService.withNames} exists.
     */
    private Page<ReleaseNoteDTO> withNames(Page<ReleaseNote> page) {
        Set<String> logins = page.getContent().stream()
            .map(ReleaseNote::getCreatedBy)
            .filter(login -> login != null && !login.isBlank())
            .collect(Collectors.toSet());
        Map<String, User> users = logins.isEmpty()
            ? Map.of()
            : userRepository.findByLoginIn(logins).stream()
                .collect(Collectors.toMap(User::getLogin, Function.identity(), (a, b) -> a));
        return page.map(note -> toDto(note, users.get(note.getCreatedBy())));
    }

    private ReleaseNoteDTO toDto(ReleaseNote note) {
        User author = note.getCreatedBy() == null
            ? null
            : userRepository.findOneByLogin(note.getCreatedBy()).orElse(null);
        return toDto(note, author);
    }

    private ReleaseNoteDTO toDto(ReleaseNote note, User author) {
        return new ReleaseNoteDTO(
            note.getId(),
            note.getVersion(),
            note.getTitle(),
            note.getContent(),
            note.getReleasedDate(),
            note.getStatus().name(),
            note.getCreatedBy(),
            author == null ? note.getCreatedBy() : displayName(author),
            note.getLastModifiedDate()
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

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
