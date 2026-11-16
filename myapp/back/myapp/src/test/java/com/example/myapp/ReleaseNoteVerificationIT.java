package com.example.myapp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.myapp.contants.enumeration.ReleaseNoteStatus;
import com.example.myapp.contants.enumeration.Role;
import com.example.myapp.domain.ReleaseNote;
import com.example.myapp.domain.User;
import com.example.myapp.repository.ReleaseNoteRepository;
import com.example.myapp.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Covers the release notes behind the About section, over real HTTP with real logins.
 *
 * <p>Driven through {@link MockMvc} rather than by calling the service, because the claim this
 * whole feature rests on is a <em>security</em> one: the service has no role check in it at all,
 * and writes are administrator-only purely because they are mapped under {@code /api/v1/admin},
 * which {@code SecurityConfig} restricts. Calling {@code ReleaseNoteService.create} from a test
 * would pass whoever was logged in and prove nothing about that boundary — only a request
 * through the filter chain does. Hence {@link #aNonAdministratorCannotWriteAReleaseNote}.
 *
 * <p>The other half is draft visibility: a draft must not reach a non-administrator. That is
 * enforced by the repository query the public path uses, so the tests compare what the two
 * endpoints return for the same row rather than inspecting the row.
 *
 * <p>Runs on the dev profile's in-memory H2 with {@code ddl-auto: create-drop}, so it needs no
 * external database.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ReleaseNoteVerificationIT {

    private static final String ADMIN = "rn_admin";
    private static final String MEMBER = "rn_member";
    private static final String PASSWORD = "Password123!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ReleaseNoteRepository releaseNoteRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private String adminToken;
    private String memberToken;

    /**
     * The Spring context is cached across tests in a run, so the table is emptied here rather
     * than relying on a fresh database: the public list and {@code currentVersion} are global
     * reads, and a note left behind by an earlier test would be indistinguishable from one this
     * test wrote. No other test touches this table.
     */
    @BeforeEach
    void setUp() throws Exception {
        releaseNoteRepository.deleteAll();
        seedUser(ADMIN, Role.ADMIN.name());
        seedUser(MEMBER, Role.USER.name());
        adminToken = tokenFor(ADMIN);
        memberToken = tokenFor(MEMBER);
    }

    private void seedUser(String login, String role) {
        if (userRepository.existsByLogin(login)) {
            return;
        }
        userRepository.save(new User()
            .login(login)
            .realName(login.equals(ADMIN) ? "Release Admin" : "Ordinary Member")
            .nickName(login)
            .email(login + "@example.com")
            .password(passwordEncoder.encode(PASSWORD))
            .role(role)
            .deleted(false));
    }

    private String tokenFor(String login) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + login + "\",\"password\":\"" + PASSWORD + "\"}"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("token").asText();
    }

    /** A release date n days back, truncated so it reads back from the column unchanged. */
    private Instant daysAgo(int days) {
        return Instant.now().minus(days, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MILLIS);
    }

    private String saveBody(String version, String title, Instant releasedDate, String status) {
        return "{\"version\":\"" + version + "\""
            + ",\"title\":\"" + title + "\""
            + ",\"content\":\"<p>Notes for " + version + "</p>\""
            + ",\"releasedDate\":\"" + releasedDate + "\""
            + (status == null ? "" : ",\"status\":\"" + status + "\"")
            + "}";
    }

    /** Create a note as the administrator and return its id. */
    private long createAsAdmin(String version, String title, Instant releasedDate, String status) throws Exception {
        String body = mockMvc.perform(post("/api/v1/admin/release-notes")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(saveBody(version, title, releasedDate, status)))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    // ----- The permission boundary -------------------------------------------------------------

    /**
     * The claim the feature rests on: an ordinary signed-in user cannot write a release note,
     * on any of the four write verbs. Asserted rather than assumed — no {@code @PreAuthorize}
     * or in-service check exists, so if the path were ever renamed out from under
     * {@code /api/v1/admin/**} every one of these would start returning 200.
     */
    @Test
    void aNonAdministratorCannotWriteAReleaseNote() throws Exception {
        long id = createAsAdmin("9.9.9", "Admin wrote this", daysAgo(1), "PUBLISHED");

        mockMvc.perform(post("/api/v1/admin/release-notes")
                .header("Authorization", "Bearer " + memberToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(saveBody("6.6.6", "Member tried", daysAgo(1), "PUBLISHED")))
            .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/v1/admin/release-notes/{id}", id)
                .header("Authorization", "Bearer " + memberToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(saveBody("9.9.9", "Member edited", daysAgo(1), "PUBLISHED")))
            .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/v1/admin/release-notes/{id}/status", id)
                .header("Authorization", "Bearer " + memberToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"DRAFT\"}"))
            .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/v1/admin/release-notes/{id}", id)
                .header("Authorization", "Bearer " + memberToken))
            .andExpect(status().isForbidden());

        // The admin list is administrator-only too: it is what exposes drafts.
        mockMvc.perform(get("/api/v1/admin/release-notes")
                .header("Authorization", "Bearer " + memberToken))
            .andExpect(status().isForbidden());

        assertThat(releaseNoteRepository.findAll())
            .as("none of the refused calls changed anything")
            .hasSize(1)
            .allSatisfy(note -> {
                assertThat(note.getVersion()).isEqualTo("9.9.9");
                assertThat(note.getTitle()).isEqualTo("Admin wrote this");
                assertThat(note.getStatus()).isEqualTo(ReleaseNoteStatus.PUBLISHED);
            });
    }

    /** Reading published notes needs only a login — that is the whole point of the split paths. */
    @Test
    void anOrdinaryUserCanReadPublishedNotesButAnonymousCannot() throws Exception {
        createAsAdmin("1.0.0", "First release", daysAgo(3), "PUBLISHED");

        mockMvc.perform(get("/api/v1/release-notes")
                .header("Authorization", "Bearer " + memberToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].version").value("1.0.0"));

        mockMvc.perform(get("/api/v1/release-notes"))
            .andExpect(status().isUnauthorized());
    }

    // ----- Draft visibility --------------------------------------------------------------------

    /**
     * A draft is invisible on the public list and present on the administrator's, and publishing
     * it is what moves it across. The same row, read two ways, before and after.
     */
    @Test
    void aDraftIsHiddenFromUsersUntilItIsPublished() throws Exception {
        long id = createAsAdmin("2.0.0", "Unreleased work", daysAgo(2), null);

        assertThat(releaseNoteRepository.findById(id).orElseThrow().getStatus())
            .as("a new note starts as a draft when no status is sent")
            .isEqualTo(ReleaseNoteStatus.DRAFT);

        mockMvc.perform(get("/api/v1/release-notes")
                .header("Authorization", "Bearer " + memberToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));

        mockMvc.perform(get("/api/v1/admin/release-notes")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].status").value("DRAFT"))
            .andExpect(jsonPath("$[0].content").value("<p>Notes for 2.0.0</p>"));

        // Publish it with the one-click action, which sends no body beyond the status.
        mockMvc.perform(put("/api/v1/admin/release-notes/{id}/status", id)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"PUBLISHED\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("PUBLISHED"))
            .andExpect(jsonPath("$.content")
                .value("<p>Notes for 2.0.0</p>")); // publishing did not blank the body

        mockMvc.perform(get("/api/v1/release-notes")
                .header("Authorization", "Bearer " + memberToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].version").value("2.0.0"));

        // And withdrawing hides it again, so a note published by mistake can be taken back.
        mockMvc.perform(put("/api/v1/admin/release-notes/{id}/status", id)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"DRAFT\"}"))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/release-notes")
                .header("Authorization", "Bearer " + memberToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));
    }

    /**
     * The About page's version is the newest <em>published</em> one. A draft for a later version
     * is exactly the case that would otherwise announce an unreleased version to every user.
     */
    @Test
    void currentVersionIgnoresALaterDraft() throws Exception {
        createAsAdmin("3.0.0", "Shipped", daysAgo(10), "PUBLISHED");
        createAsAdmin("4.0.0", "Not shipped yet", daysAgo(1), null);

        mockMvc.perform(get("/api/v1/release-notes/current-version")
                .header("Authorization", "Bearer " + memberToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.version")
                .value("3.0.0"));

        // Publishing the newer one moves the version forward.
        long newer = releaseNoteRepository.findByVersion("4.0.0").orElseThrow().getId();
        mockMvc.perform(put("/api/v1/admin/release-notes/{id}/status", newer)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"PUBLISHED\"}"))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/release-notes/current-version")
                .header("Authorization", "Bearer " + memberToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.version").value("4.0.0"));
    }

    /**
     * With nothing published, the version is null rather than absent or a made-up number — the
     * About page shows its "not released yet" placeholder on exactly this response.
     */
    @Test
    void currentVersionIsNullBeforeAnythingIsPublished() throws Exception {
        createAsAdmin("5.0.0", "Still a draft", daysAgo(1), null);

        mockMvc.perform(get("/api/v1/release-notes/current-version")
                .header("Authorization", "Bearer " + memberToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.version").value((Object) null));
    }

    // ----- Ordering, editing, and the duplicate guard -------------------------------------------

    /** The timeline reads newest release first, by release date rather than by insertion order. */
    @Test
    void publishedNotesComeBackNewestReleaseFirst() throws Exception {
        // Inserted oldest-release-last on purpose, so insertion order is the wrong answer.
        createAsAdmin("1.1.0", "Middle", daysAgo(20), "PUBLISHED");
        createAsAdmin("1.2.0", "Newest", daysAgo(5), "PUBLISHED");
        createAsAdmin("1.0.0", "Oldest", daysAgo(40), "PUBLISHED");

        mockMvc.perform(get("/api/v1/release-notes")
                .header("Authorization", "Bearer " + memberToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(3)))
            .andExpect(jsonPath("$[0].version").value("1.2.0"))
            .andExpect(jsonPath("$[1].version").value("1.1.0"))
            .andExpect(jsonPath("$[2].version").value("1.0.0"))
            // The author's display name is resolved, not their login.
            .andExpect(jsonPath("$[0].authorName").value("Release Admin"))
            .andExpect(jsonPath("$[0].author").value(ADMIN));
    }

    /** Two notes cannot claim one version, and the failure names the rule the client branches on. */
    @Test
    void aDuplicateVersionIsRefusedOnCreateAndOnUpdate() throws Exception {
        createAsAdmin("7.0.0", "Taken", daysAgo(9), "PUBLISHED");
        long other = createAsAdmin("7.1.0", "Also taken", daysAgo(8), "PUBLISHED");

        mockMvc.perform(post("/api/v1/admin/release-notes")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(saveBody("7.0.0", "Duplicate", daysAgo(1), "PUBLISHED")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorKey").value("versionexists"));

        // Moving an existing note onto another's version is refused the same way.
        mockMvc.perform(put("/api/v1/admin/release-notes/{id}", other)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(saveBody("7.0.0", "Stealing the version", daysAgo(8), "PUBLISHED")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorKey").value("versionexists"));

        assertThat(releaseNoteRepository.findAll()).as("neither refusal added a row").hasSize(2);
    }

    /**
     * A note keeping its own version while other fields change is not a duplicate. This is the
     * case a naive {@code existsByVersion} guard on update gets wrong, which would make a typo
     * in a published note unfixable.
     */
    @Test
    void editingANoteWithoutChangingItsVersionIsAllowed() throws Exception {
        Instant released = daysAgo(6);
        long id = createAsAdmin("8.0.0", "Original headline", released, "PUBLISHED");

        mockMvc.perform(put("/api/v1/admin/release-notes/{id}", id)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\":\"8.0.0\",\"title\":\"Fixed headline\""
                    + ",\"content\":\"<p>Corrected notes</p>\""
                    + ",\"releasedDate\":\"" + released + "\",\"status\":\"PUBLISHED\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Fixed headline"))
            .andExpect(jsonPath("$.content").value("<p>Corrected notes</p>"));

        ReleaseNote saved = releaseNoteRepository.findById(id).orElseThrow();
        assertThat(saved.getVersion()).isEqualTo("8.0.0");
        assertThat(saved.getReleasedDate())
            .as("the release date survives an edit that did not change it")
            .isEqualTo(released);
        assertThat(saved.getCreatedBy())
            .as("the original author is kept; created_by is updatable = false")
            .isEqualTo(ADMIN);
        assertThat(saved.getLastModifiedBy())
            .as("and who last edited it is recorded, which the list shows")
            .isEqualTo(ADMIN);
    }

    /** An empty editor body is refused: @NotBlank passes "<p></p>", so the service checks it. */
    @Test
    void anEmptyEditorBodyIsRefused() throws Exception {
        mockMvc.perform(post("/api/v1/admin/release-notes")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\":\"9.0.0\",\"title\":\"Nothing to say\""
                    + ",\"content\":\"<p></p>\""
                    + ",\"releasedDate\":\"" + daysAgo(1) + "\",\"status\":\"DRAFT\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorKey").value("emptycontent"));

        mockMvc.perform(post("/api/v1/admin/release-notes")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\":\"9.0.1\",\"title\":\"Bad status\""
                    + ",\"content\":\"<p>Fine</p>\""
                    + ",\"releasedDate\":\"" + daysAgo(1) + "\",\"status\":\"SOMETHING_ELSE\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorKey").value("badstatus"));

        assertThat(releaseNoteRepository.findAll()).as("neither bad request was saved").isEmpty();
    }

    /** Deleting removes the note from both lists. */
    @Test
    void anAdministratorCanDeleteANote() throws Exception {
        long id = createAsAdmin("10.0.0", "Published by mistake", daysAgo(1), "PUBLISHED");

        mockMvc.perform(delete("/api/v1/admin/release-notes/{id}", id)
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isNoContent());

        assertThat(releaseNoteRepository.findById(id)).isEmpty();
        mockMvc.perform(get("/api/v1/release-notes")
                .header("Authorization", "Bearer " + memberToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));
    }
}
