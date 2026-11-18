package com.example.myapp;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.myapp.contants.enumeration.BlogVisibility;
import com.example.myapp.domain.Blog;
import com.example.myapp.repository.BlogRepository;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/**
 * The link-preview endpoints over real HTTP, with no token at all.
 *
 * Anonymous is the whole point and cannot be checked any other way: WeChat, Weibo and Telegram
 * fetch a shared URL with no Authorization header, so if the SecurityConfig matcher is wrong they
 * are answered 401 and the card falls back to the bare title — the exact symptom this work fixes,
 * reported by a person on a phone and invisible in every test that authenticates first.
 *
 * The rendering rules live in BlogPreviewServiceTest; this covers reachability and the gate.
 */
@SpringBootTest
@AutoConfigureMockMvc
class BlogPreviewHttpVerificationIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BlogRepository blogRepository;

    private static final byte[] IMAGE_BYTES = "pretend-png-bytes".getBytes(StandardCharsets.UTF_8);

    private Blog seedBlog(String title, BlogVisibility visibility, boolean deleted, String content) {
        Blog blog = new Blog();
        blog.setTitle(title);
        blog.setContent(content);
        blog.setSummary("The summary a card should show.");
        blog.setAuthor("preview_author");
        blog.setVisibility(visibility);
        blog.setDeleted(deleted);
        return blogRepository.save(blog);
    }

    private static String inlineImageContent() {
        return "<p><img src=\"data:image/png;base64,"
            + Base64.getEncoder().encodeToString(IMAGE_BYTES) + "\"></p>";
    }

    @Test
    void aPublicPostPreviewsWithNoTokenAtAll() throws Exception {
        Blog blog = seedBlog("Anonymous preview post", BlogVisibility.PUBLIC, false, "<p>Body.</p>");

        String html = mockMvc.perform(get("/api/v1/blogs/{id}/preview", blog.getId())
                .header("Host", "cards.example.com")
                .header("X-Forwarded-Proto", "https"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith("text/html"))
            .andReturn().getResponse().getContentAsString();

        assertThat(html)
            .contains("og:title")
            .contains("Anonymous preview post")
            .contains("og:description")
            .contains("The summary a card should show.")
            .contains("https://cards.example.com/blog/" + blog.getId());
    }

    @Test
    void theJsonReadStillNeedsAToken() throws Exception {
        // The permitAll is two exact paths, not a prefix. If it ever widens to /api/v1/blogs/**
        // every post in the app becomes readable anonymously, private ones included, because
        // GET /blogs/{id} does not filter on visibility.
        Blog blog = seedBlog("Not open as JSON", BlogVisibility.PUBLIC, false, "<p>Body.</p>");

        mockMvc.perform(get("/api/v1/blogs/{id}", blog.getId()))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void aPrivatePostRevealsNothing() throws Exception {
        Blog blog = seedBlog("Private title nobody may see", BlogVisibility.PRIVATE, false, "<p>Secret.</p>");

        String html = mockMvc.perform(get("/api/v1/blogs/{id}/preview", blog.getId())
                .header("Host", "cards.example.com"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        assertThat(html)
            .doesNotContain("Private title nobody may see")
            .doesNotContain("Secret.")
            .doesNotContain("og:title")
            .contains("noindex");
    }

    @Test
    void aDeletedPostRevealsNothing() throws Exception {
        // BlogService.findOne is a plain findById — it hands back soft-deleted rows — so the
        // deleted flag is honoured here only because isPreviewable reads it.
        Blog blog = seedBlog("Deleted title nobody may see", BlogVisibility.PUBLIC, true, "<p>Gone.</p>");

        String html = mockMvc.perform(get("/api/v1/blogs/{id}/preview", blog.getId())
                .header("Host", "cards.example.com"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        assertThat(html).doesNotContain("Deleted title nobody may see").doesNotContain("og:title");
    }

    @Test
    void anUnavailablePostIsIndistinguishableFromAnAbsentOne() throws Exception {
        // Otherwise a crawler walking ids learns which ones exist privately.
        Blog blog = seedBlog("Private for comparison", BlogVisibility.PRIVATE, false, "<p>Secret.</p>");

        String privateHtml = mockMvc.perform(get("/api/v1/blogs/{id}/preview", blog.getId())
                .header("Host", "cards.example.com"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        String absentHtml = mockMvc.perform(get("/api/v1/blogs/{id}/preview", 9_999_999L)
                .header("Host", "cards.example.com"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        assertThat(privateHtml).isEqualTo(absentHtml);
    }

    @Test
    void aChineseTitleSurvivesTheResponseEncoding() throws Exception {
        // text/html with no charset is sent as ISO-8859-1 and the card shows mojibake.
        Blog blog = seedBlog("中文分享标题", BlogVisibility.PUBLIC, false, "<p>正文。</p>");

        var response = mockMvc.perform(get("/api/v1/blogs/{id}/preview", blog.getId())
                .header("Host", "cards.example.com"))
            .andExpect(status().isOk())
            .andReturn().getResponse();

        assertThat(response.getCharacterEncoding()).isEqualToIgnoringCase("UTF-8");
        assertThat(response.getContentAsString(StandardCharsets.UTF_8)).contains("中文分享标题");
    }

    @Test
    void anUntrustworthyHostYieldsACardWithNoUrlOrImage() throws Exception {
        // og:url and og:image are published, so a Host that is not a bare hostname is refused
        // rather than echoed into them. The title and description still render.
        Blog blog = seedBlog("Host check post", BlogVisibility.PUBLIC, false, inlineImageContent());

        String html = mockMvc.perform(get("/api/v1/blogs/{id}/preview", blog.getId())
                .header("Host", "evil.example.com/path\"><x"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        assertThat(html)
            .contains("og:title")
            .doesNotContain("og:url")
            .doesNotContain("og:image")
            .doesNotContain("evil.example.com");
    }

    @Test
    void anUploadedImageIsServedAnonymouslyAsItsOwnBytes() throws Exception {
        // The case a photo added from a phone takes: RichEditor stores it as a base64 data URI,
        // which no crawler can fetch, so this endpoint is the only way it reaches a card.
        Blog blog = seedBlog("Post with a photo", BlogVisibility.PUBLIC, false, inlineImageContent());

        byte[] served = mockMvc.perform(get("/api/v1/blogs/{id}/preview-image", blog.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith("image/png"))
            .andExpect(header().string("X-Content-Type-Options", "nosniff"))
            .andReturn().getResponse().getContentAsByteArray();

        assertThat(served).isEqualTo(IMAGE_BYTES);
    }

    @Test
    void theCardPointsAtThatEndpoint() throws Exception {
        Blog blog = seedBlog("Post with a photo", BlogVisibility.PUBLIC, false, inlineImageContent());

        String html = mockMvc.perform(get("/api/v1/blogs/{id}/preview", blog.getId())
                .header("Host", "cards.example.com")
                .header("X-Forwarded-Proto", "https"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        assertThat(html).contains(
            "https://cards.example.com/api/v1/blogs/" + blog.getId() + "/preview-image");
    }

    @Test
    void aPostWithNoInlineImageHasNoImageToServe() throws Exception {
        Blog blog = seedBlog("No photo here", BlogVisibility.PUBLIC, false, "<p>Words only.</p>");

        mockMvc.perform(get("/api/v1/blogs/{id}/preview-image", blog.getId()))
            .andExpect(status().isNotFound());
    }

    @Test
    void aPrivatePostsImageIsNotServedEither() throws Exception {
        // The bytes are there; the gate is what withholds them.
        Blog blog = seedBlog("Private photo", BlogVisibility.PRIVATE, false, inlineImageContent());

        mockMvc.perform(get("/api/v1/blogs/{id}/preview-image", blog.getId()))
            .andExpect(status().isNotFound());
    }
}
