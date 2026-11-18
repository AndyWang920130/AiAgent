package com.example.myapp.service;

import com.example.myapp.contants.enumeration.BlogVisibility;
import com.example.myapp.service.dto.BlogDTO;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Builds the small HTML document a link crawler is served for {@code /blog/{id}}.
 *
 * WeChat, Weibo, QQ, Telegram, Slack and the rest all build a link card the same way: they fetch
 * the URL with their own crawler and read the {@code og:*} tags out of the response. None of them
 * run JavaScript, so the SPA shell nginx serves today gives them nothing — a shared post shows as
 * a bare title, and only because the title is passed in the {@code navigator.share} payload.
 * nginx routes crawler requests for a blog URL here instead (see {@code nginx.conf.template}).
 *
 * Every interpolated value is author-written free text, so all of it goes through {@link #escape}.
 * A post titled {@code "><script>…} would otherwise close the meta tag it sits in and inject into
 * a page served from our own origin.
 *
 * Deliberately has no repository or request dependency: it takes a DTO and a base URL and returns
 * a string, which is what makes every rule below checkable without a running server.
 */
@Service
public class BlogPreviewService {

    /** Shown as {@code og:site_name}. Matches the {@code <title>} in front/index.html. */
    private static final String SITE_NAME = "aiagentcode";

    /** Cap for a description derived from the post body. Crawlers truncate near 200 anyway. */
    private static final int DESCRIPTION_LIMIT = 200;

    /** First {@code <img>} source in the body. TipTap always emits a quoted src. */
    private static final Pattern FIRST_IMG = Pattern.compile(
        "<img[^>]+src\\s*=\\s*[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);

    /** A base64 data URI, as RichEditor writes for an uploaded file. */
    private static final Pattern DATA_URI = Pattern.compile(
        "^data:(image/[a-z0-9.+-]+);base64,(.+)$", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    /**
     * Raster types we will decode and serve. {@code image/svg+xml} is excluded on purpose: SVG
     * can carry script, and this is served from our own origin.
     */
    private static final Set<String> SERVABLE_TYPES =
        Set.of("image/png", "image/jpeg", "image/gif", "image/webp", "image/bmp");

    /** Decoded bytes of a post's first inline image, for the preview-image endpoint. */
    public record PreviewImage(byte[] bytes, String contentType) {}

    /**
     * Whether a post may be rendered to an anonymous crawler.
     *
     * The same gate the share button applies (BlogDetailView's {@code canShare}), and it has to
     * stay the same one: the preview endpoints are {@code permitAll}, so they are the only blog
     * reads in the app that answer without a token. {@code GET /api/v1/blogs/{id}} does not
     * filter on visibility — a separate, pre-existing hole — and this must not widen it to
     * callers who are not signed in at all.
     */
    public boolean isPreviewable(BlogDTO blog) {
        return blog != null
            && blog.getVisibility() == BlogVisibility.PUBLIC
            && !Boolean.TRUE.equals(blog.getDeleted());
    }

    /**
     * The crawler-facing document for a post.
     *
     * @param baseUrl absolute origin, e.g. {@code https://example.com}, or null when none could
     *                be trusted. The url and image tags are then left out rather than published
     *                pointing at whatever host the request claimed to be for.
     */
    public String render(BlogDTO blog, String baseUrl) {
        String title = escape(titleOf(blog));
        String description = escape(summaryOf(blog));
        String image = imageUrl(blog, baseUrl);
        String canonical = baseUrl == null ? null : escape(baseUrl + "/blog/" + blog.getId());

        StringBuilder html = new StringBuilder(1024);
        html.append("<!DOCTYPE html>\n<html lang=\"zh-CN\">\n<head>\n");
        // charset first, and it must land inside the first 1024 bytes: a crawler that has not
        // been told the encoding by then may decode a Chinese title as Latin-1 and build a card
        // full of mojibake.
        html.append("<meta charset=\"utf-8\">\n");
        html.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        html.append("<title>").append(title).append("</title>\n");
        html.append("<meta name=\"description\" content=\"").append(description).append("\">\n");
        html.append("<meta property=\"og:type\" content=\"article\">\n");
        html.append("<meta property=\"og:site_name\" content=\"").append(SITE_NAME).append("\">\n");
        html.append("<meta property=\"og:title\" content=\"").append(title).append("\">\n");
        html.append("<meta property=\"og:description\" content=\"").append(description).append("\">\n");
        if (canonical != null) {
            html.append("<meta property=\"og:url\" content=\"").append(canonical).append("\">\n");
            html.append("<link rel=\"canonical\" href=\"").append(canonical).append("\">\n");
        }
        if (image != null) {
            String safeImage = escape(image);
            html.append("<meta property=\"og:image\" content=\"").append(safeImage).append("\">\n");
            html.append("<meta name=\"twitter:image\" content=\"").append(safeImage).append("\">\n");
        }
        html.append("<meta name=\"twitter:card\" content=\"")
            .append(image != null ? "summary_large_image" : "summary").append("\">\n");
        html.append("<meta name=\"twitter:title\" content=\"").append(title).append("\">\n");
        html.append("<meta name=\"twitter:description\" content=\"").append(description).append("\">\n");
        html.append("</head>\n<body>\n");
        html.append("<h1>").append(title).append("</h1>\n");
        // A real <img> as well as og:image. WeChat has long fallen back to the first image in the
        // document when it cannot use the tag, and this costs one line.
        if (image != null) {
            html.append("<p><img src=\"").append(escape(image)).append("\" alt=\"")
                .append(title).append("\"></p>\n");
        }
        html.append("<p>").append(description).append("</p>\n");
        html.append("<p><a href=\"/blog/").append(blog.getId())
            .append("?app=1\">阅读全文 / Read the full post</a></p>\n");
        html.append(redirectScript(blog.getId()));
        html.append("</body>\n</html>\n");
        return html.toString();
    }

    /**
     * Sends a real browser on to the app.
     *
     * Needed because one UA cannot be split: WeChat's in-app browser sends the same
     * {@code MicroMessenger} token as the crawler that built the card, so a person tapping the
     * card arrives here too. They get the app; the crawler, which runs no script, keeps the tags.
     *
     * {@code ?app=1} is load-bearing — it is what stops nginx routing the redirect straight back
     * here (the UA still matches). BlogDetailView strips it from the address bar on mount so it
     * does not end up in the next link someone shares.
     *
     * A {@code <meta http-equiv="refresh">} would be wrong here: some crawlers follow it, and
     * they would then read the tag-less SPA shell and lose the card this exists to build.
     *
     * The id is a {@code Long}, so there is nothing to escape into the script.
     */
    private String redirectScript(Long id) {
        return "<script>\n"
            + "if (!/[?&]app=1(&|$)/.test(location.search)) {\n"
            + "  location.replace('/blog/" + id + "?app=1');\n"
            + "}\n"
            + "</script>\n";
    }

    /**
     * The document for a post that does not exist, is private, or is deleted.
     *
     * Carries no post data at all — not even the title. A crawler probing ids must not be able to
     * tell a private post from an absent one, and the response is public. A person who follows a
     * stale link still gets something with a way into the app.
     */
    public String renderNotFound() {
        return "<!DOCTYPE html>\n<html lang=\"zh-CN\">\n<head>\n"
            + "<meta charset=\"utf-8\">\n"
            + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n"
            + "<title>" + SITE_NAME + "</title>\n"
            + "<meta name=\"robots\" content=\"noindex\">\n"
            + "</head>\n<body>\n"
            + "<p><a href=\"/blog\">" + SITE_NAME + "</a></p>\n"
            + "</body>\n</html>\n";
    }

    private String titleOf(BlogDTO blog) {
        String title = blog.getTitle();
        return title == null || title.isBlank() ? SITE_NAME : title.trim();
    }

    /**
     * The card's description: the author's summary, else the opening of the post itself.
     *
     * The fallback is not decoration. AddBlogView sends {@code excerpt: form.excerpt || form.title}
     * (AddBlogView.vue:43), so a post whose author left the excerpt blank has {@code summary}
     * equal to its own title — and a card showing the title twice says nothing. Stripping the
     * body gives the reader an actual first line.
     */
    String summaryOf(BlogDTO blog) {
        String summary = blog.getSummary();
        if (summary != null && !summary.isBlank() && !summary.trim().equals(titleOf(blog))) {
            return truncate(summary.trim(), DESCRIPTION_LIMIT);
        }
        return truncate(stripHtml(blog.getContent()), DESCRIPTION_LIMIT);
    }

    /**
     * Visible text from an HTML fragment.
     *
     * Script and style bodies go first, or their source would be read as text and the description
     * would open with CSS. Escaping is {@link #escape}'s job, not this method's — this only has to
     * produce something readable.
     */
    String stripHtml(String html) {
        if (html == null || html.isBlank()) return "";
        String text = html.replaceAll("(?is)<(script|style)[^>]*>.*?</\\1>", " ");
        text = text.replaceAll("(?s)<[^>]+>", " ");
        text = text.replace("&nbsp;", " ")
                   .replace("&lt;", "<")
                   .replace("&gt;", ">")
                   .replace("&quot;", "\"")
                   .replace("&#39;", "'")
                   .replace("&amp;", "&");
        return text.replaceAll("\\s+", " ").trim();
    }

    /** Cut to {@code limit} characters on a word boundary where there is one nearby. */
    private String truncate(String text, int limit) {
        if (text == null) return "";
        if (text.length() <= limit) return text;
        String cut = text.substring(0, limit);
        int space = cut.lastIndexOf(' ');
        // Chinese does not space its words, so a boundary may not exist. Only back off to one
        // when it is near the end, or a CJK line would be cut at its first ASCII space.
        if (space > limit - 20) cut = cut.substring(0, space);
        return cut.trim() + "…";
    }

    /**
     * An absolute, crawler-fetchable image URL for the card, or null if the post has none.
     *
     * {@code coverImage} is preferred and is currently always null: the field exists on the entity,
     * the DTO and the schema, and nothing in the app ever writes it — blogApi.create and .update
     * do not send it and no view renders it. It is honoured first so that filling it in later is
     * all it takes.
     *
     * So in practice the image comes from the post body, which RichEditor writes two ways:
     * an external URL ({@code applyImageUrl}) or a base64 data URI for an uploaded file
     * ({@code handleImageFile}). A data URI is worthless to a crawler — it cannot fetch one — so
     * that case points at the preview-image endpoint, which decodes and serves the bytes. That is
     * the path a photo added from a phone takes.
     */
    String imageUrl(BlogDTO blog, String baseUrl) {
        String cover = blog.getCoverImage();
        if (isHttpUrl(cover)) return cover.trim();

        String src = firstImageSrc(blog.getContent());
        if (isHttpUrl(src)) return src.trim();
        if (src != null && baseUrl != null && inlineImage(blog) != null) {
            return baseUrl + "/api/v1/blogs/" + blog.getId() + "/preview-image";
        }
        return null;
    }

    /**
     * Absolute http(s) only. A relative or protocol-relative value is refused: {@code //host/x}
     * reads as scheme-relative, which in an absolute tag points off-site entirely.
     */
    private boolean isHttpUrl(String value) {
        if (value == null) return false;
        String v = value.trim().toLowerCase();
        return v.startsWith("http://") || v.startsWith("https://");
    }

    private String firstImageSrc(String content) {
        if (content == null) return null;
        Matcher match = FIRST_IMG.matcher(content);
        return match.find() ? match.group(1) : null;
    }

    /**
     * The post's first inline image, decoded, or null if it has none we will serve.
     *
     * Only the types in {@link #SERVABLE_TYPES}, and the declared type is what the endpoint
     * reports — so the endpoint also sends {@code X-Content-Type-Options: nosniff}, since these
     * bytes are author-supplied and served from our own origin.
     */
    public PreviewImage inlineImage(BlogDTO blog) {
        String src = firstImageSrc(blog == null ? null : blog.getContent());
        if (src == null) return null;
        Matcher uri = DATA_URI.matcher(src.trim());
        if (!uri.matches()) return null;

        String contentType = uri.group(1).toLowerCase();
        if (!SERVABLE_TYPES.contains(contentType)) return null;
        try {
            byte[] bytes = Base64.getMimeDecoder().decode(uri.group(2));
            return bytes.length == 0 ? null : new PreviewImage(bytes, contentType);
        } catch (IllegalArgumentException e) {
            // Hand-edited or truncated base64. No image, not a 500.
            return null;
        }
    }

    /**
     * HTML-escape for both text and a double-quoted attribute value.
     *
     * The quote matters most: every value here lands in {@code content="…"}, and a title holding
     * one would otherwise end the attribute and let the rest of it be parsed as markup — stored
     * XSS on our own origin, authored through the ordinary blog editor.
     */
    static String escape(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }
}
