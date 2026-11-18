package com.example.myapp.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.myapp.contants.enumeration.BlogVisibility;
import com.example.myapp.service.dto.BlogDTO;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the link-preview document. Pure string building, so it is pinned here rather than
 * exercised through a request.
 *
 * The two that matter most are the escaping nest — the title and summary are author-written and
 * land inside {@code content="…"}, so an unescaped quote is stored XSS on our own origin — and
 * the visibility gate, because these endpoints are the only blog reads in the app that answer
 * without a token.
 */
class BlogPreviewServiceTest {

    private final BlogPreviewService service = new BlogPreviewService();

    private static final String BASE = "https://example.com";

    /** A public, undeleted post with a title, a summary and a body. */
    private BlogDTO post() {
        BlogDTO blog = new BlogDTO();
        blog.setId(42L);
        blog.setTitle("Hello");
        blog.setSummary("A short summary.");
        blog.setContent("<p>The body of the post.</p>");
        blog.setVisibility(BlogVisibility.PUBLIC);
        blog.setDeleted(false);
        return blog;
    }

    @Nested
    @DisplayName("what may be previewed anonymously")
    class VisibilityTests {

        @Test
        void aPublicUndeletedPostMayBe() {
            assertThat(service.isPreviewable(post())).isTrue();
        }

        @Test
        void aPrivatePostMayNot() {
            BlogDTO blog = post();
            blog.setVisibility(BlogVisibility.PRIVATE);
            assertThat(service.isPreviewable(blog)).isFalse();
        }

        @Test
        void aDeletedPostMayNot() {
            BlogDTO blog = post();
            blog.setDeleted(true);
            assertThat(service.isPreviewable(blog)).isFalse();
        }

        @Test
        void aPostWithNoVisibilitySetMayNot() {
            BlogDTO blog = post();
            blog.setVisibility(null);
            assertThat(service.isPreviewable(blog)).isFalse();
        }

        @Test
        void theNotFoundDocumentCarriesNoPostData() {
            String html = service.renderNotFound();
            assertThat(html).doesNotContain("og:title").doesNotContain("og:description");
            // A crawler walking ids must not be able to tell a private post from an absent one.
            assertThat(html).contains("noindex");
        }
    }

    @Nested
    @DisplayName("escaping")
    class EscapingTests {

        @Test
        void aTitleCannotCloseTheAttributeItSitsIn() {
            BlogDTO blog = post();
            blog.setTitle("\"><script>alert(1)</script>");
            String html = service.render(blog, BASE);

            assertThat(html).doesNotContain("<script>alert(1)</script>");
            assertThat(html).contains("&quot;&gt;&lt;script&gt;");
        }

        @Test
        void aSummaryCannotEither() {
            BlogDTO blog = post();
            blog.setSummary("\" onload=\"alert(1)");
            String html = service.render(blog, BASE);

            assertThat(html).doesNotContain("\" onload=\"");
            assertThat(html).contains("&quot; onload=&quot;");
        }

        @Test
        void anApostropheIsEscapedToo() {
            BlogDTO blog = post();
            blog.setTitle("it's here");
            assertThat(service.render(blog, BASE)).contains("it&#39;s here");
        }

        @Test
        void anAmpersandIsNotDoubleEscaped() {
            BlogDTO blog = post();
            blog.setTitle("A & B");
            String html = service.render(blog, BASE);

            assertThat(html).contains("A &amp; B");
            assertThat(html).doesNotContain("&amp;amp;");
        }

        @Test
        void anImageUrlFromTheBodyIsEscapedAsWell() {
            BlogDTO blog = post();
            blog.setContent("<p><img src=\"https://cdn.example.com/a.png?w=1&h=2\"></p>");
            assertThat(service.render(blog, BASE)).contains("a.png?w=1&amp;h=2");
        }
    }

    @Nested
    @DisplayName("the description")
    class DescriptionTests {

        @Test
        void theAuthorsSummaryIsUsedWhenThereIsOne() {
            assertThat(service.summaryOf(post())).isEqualTo("A short summary.");
        }

        @Test
        void aSummaryEqualToTheTitleFallsBackToTheBody() {
            // AddBlogView sends `excerpt: form.excerpt || form.title`, so a post whose author
            // left the excerpt blank has summary == title. A card showing the title twice is
            // the empty card this work exists to fix.
            BlogDTO blog = post();
            blog.setSummary("Hello");
            blog.setContent("<p>The real opening line.</p>");
            assertThat(service.summaryOf(blog)).isEqualTo("The real opening line.");
        }

        @Test
        void aBlankSummaryFallsBackToTheBody() {
            BlogDTO blog = post();
            blog.setSummary("   ");
            assertThat(service.summaryOf(blog)).isEqualTo("The body of the post.");
        }

        @Test
        void markupIsStrippedFromTheBody() {
            assertThat(service.stripHtml("<p>One</p><p>Two</p>")).isEqualTo("One Two");
        }

        @Test
        void scriptAndStyleBodiesAreNotReadAsText() {
            String html = "<style>.a{color:red}</style><script>var x=1</script><p>Text.</p>";
            assertThat(service.stripHtml(html)).isEqualTo("Text.");
        }

        @Test
        void entitiesAreDecodedForReading() {
            assertThat(service.stripHtml("<p>a &amp; b &nbsp;c</p>")).isEqualTo("a & b c");
        }

        @Test
        void aLongBodyIsTruncated() {
            BlogDTO blog = post();
            blog.setSummary(null);
            blog.setContent("<p>" + "word ".repeat(100) + "</p>");
            String summary = service.summaryOf(blog);

            assertThat(summary).endsWith("…");
            assertThat(summary.length()).isLessThanOrEqualTo(201);
        }

        @Test
        void aPostWithNoSummaryAndNoBodyStillRenders() {
            BlogDTO blog = post();
            blog.setSummary(null);
            blog.setContent(null);
            assertThat(service.summaryOf(blog)).isEmpty();
            assertThat(service.render(blog, BASE)).contains("og:title");
        }
    }

    @Nested
    @DisplayName("the image")
    class ImageTests {

        /** Stands in for an uploaded file: RichEditor stores one as a base64 data URI. */
        private static final byte[] PIXEL = "not-really-a-png-but-bytes".getBytes(StandardCharsets.UTF_8);

        private String dataUri(String type) {
            return "data:" + type + ";base64," + Base64.getEncoder().encodeToString(PIXEL);
        }

        @Test
        void coverImageIsPreferredWhenItIsEverSet() {
            BlogDTO blog = post();
            blog.setCoverImage("https://cdn.example.com/cover.png");
            blog.setContent("<p><img src=\"https://cdn.example.com/body.png\"></p>");
            assertThat(service.imageUrl(blog, BASE)).isEqualTo("https://cdn.example.com/cover.png");
        }

        @Test
        void otherwiseTheFirstImageInTheBodyIsUsed() {
            BlogDTO blog = post();
            blog.setContent("<p>Words</p><p><img src=\"https://cdn.example.com/body.png\"></p>");
            assertThat(service.imageUrl(blog, BASE)).isEqualTo("https://cdn.example.com/body.png");
        }

        @Test
        void aRelativeCoverImageIsRefused() {
            // An og:image has to be absolute; a relative one yields no card image at all. Falls
            // through to the body, which has no image here.
            BlogDTO blog = post();
            blog.setCoverImage("/uploads/cover.png");
            assertThat(service.imageUrl(blog, BASE)).isNull();
        }

        @Test
        void aProtocolRelativeCoverImageIsRefused() {
            BlogDTO blog = post();
            blog.setCoverImage("//evil.example/cover.png");
            assertThat(service.imageUrl(blog, BASE)).isNull();
        }

        @Test
        void anUploadedImagePointsAtThePreviewImageEndpoint() {
            // The case a photo added from a phone takes: a crawler cannot fetch a data URI.
            BlogDTO blog = post();
            blog.setContent("<p><img src=\"" + dataUri("image/png") + "\"></p>");
            assertThat(service.imageUrl(blog, BASE))
                .isEqualTo(BASE + "/api/v1/blogs/42/preview-image");
        }

        @Test
        void aPostWithNoImageHasNone() {
            assertThat(service.imageUrl(post(), BASE)).isNull();
        }

        @Test
        void theUploadedBytesComeBackIntact() {
            BlogDTO blog = post();
            blog.setContent("<p><img src=\"" + dataUri("image/jpeg") + "\"></p>");
            BlogPreviewService.PreviewImage image = service.inlineImage(blog);

            assertThat(image).isNotNull();
            assertThat(image.bytes()).isEqualTo(PIXEL);
            assertThat(image.contentType()).isEqualTo("image/jpeg");
        }

        @Test
        void svgIsNotServed() {
            // SVG can carry script and this would be served from our own origin.
            BlogDTO blog = post();
            blog.setContent("<p><img src=\"" + dataUri("image/svg+xml") + "\"></p>");
            assertThat(service.inlineImage(blog)).isNull();
            assertThat(service.imageUrl(blog, BASE)).isNull();
        }

        @Test
        void brokenBase64IsNotAnError() {
            BlogDTO blog = post();
            blog.setContent("<p><img src=\"data:image/png;base64,!!!not-base64!!!\"></p>");
            assertThat(service.inlineImage(blog)).isNull();
        }

        @Test
        void anExternalUrlIsNotTreatedAsInlineBytes() {
            BlogDTO blog = post();
            blog.setContent("<p><img src=\"https://cdn.example.com/body.png\"></p>");
            assertThat(service.inlineImage(blog)).isNull();
        }
    }

    @Nested
    @DisplayName("the document")
    class DocumentTests {

        @Test
        void itCarriesTheTagsACrawlerReads() {
            String html = service.render(post(), BASE);

            assertThat(html).contains("<meta property=\"og:title\" content=\"Hello\">");
            assertThat(html).contains("<meta property=\"og:description\" content=\"A short summary.\">");
            assertThat(html).contains("<meta name=\"description\" content=\"A short summary.\">");
            assertThat(html).contains("<meta property=\"og:url\" content=\"https://example.com/blog/42\">");
            assertThat(html).contains("<meta property=\"og:type\" content=\"article\">");
            assertThat(html).contains("og:site_name");
        }

        @Test
        void theCharsetIsDeclaredInsideTheFirstKilobyte() {
            // A crawler that has not been told the encoding by then may decode a Chinese title
            // as Latin-1 and build a card of mojibake.
            BlogDTO blog = post();
            blog.setTitle("中文标题");
            String html = service.render(blog, BASE);

            assertThat(html.indexOf("<meta charset=\"utf-8\">")).isBetween(0, 1024);
            assertThat(html).contains("中文标题");
        }

        @Test
        void withNoTrustedOriginTheUrlAndImageAreLeftOut() {
            // Rather than publish a card pointing at whatever host the request claimed.
            BlogDTO blog = post();
            blog.setContent("<p><img src=\"data:image/png;base64,AAAA\"></p>");
            String html = service.render(blog, null);

            assertThat(html).doesNotContain("og:url").doesNotContain("og:image");
            assertThat(html).contains("og:title").contains("og:description");
        }

        @Test
        void anImageUpgradesTheTwitterCardAndAppearsInTheBody() {
            BlogDTO blog = post();
            blog.setContent("<p><img src=\"https://cdn.example.com/body.png\"></p>");
            String html = service.render(blog, BASE);

            assertThat(html).contains("twitter:card\" content=\"summary_large_image\"");
            // WeChat has long fallen back to the first image in the document.
            assertThat(html).contains("<img src=\"https://cdn.example.com/body.png\"");
        }

        @Test
        void withNoImageTheTwitterCardIsThePlainOne() {
            assertThat(service.render(post(), BASE)).contains("twitter:card\" content=\"summary\"");
        }

        @Test
        void aRealBrowserIsSentOnToTheApp() {
            // WeChat's in-app browser sends the crawler's UA, so a person who taps the card
            // lands here too. The marker is what stops nginx routing that straight back.
            String html = service.render(post(), BASE);

            assertThat(html).contains("location.replace('/blog/42?app=1')");
            // A meta refresh would be followed by some crawlers, which would then read the
            // tag-less SPA shell and lose the card.
            assertThat(html).doesNotContain("http-equiv=\"refresh\"");
        }

        @Test
        void thereIsAVisibleWayIntoTheApp() {
            assertThat(service.render(post(), BASE)).contains("href=\"/blog/42?app=1\"");
        }
    }
}
