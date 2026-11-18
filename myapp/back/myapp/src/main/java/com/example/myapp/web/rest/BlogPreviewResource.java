package com.example.myapp.web.rest;

import com.example.myapp.service.BlogPreviewService;
import com.example.myapp.service.BlogService;
import com.example.myapp.service.dto.BlogDTO;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * The link-preview surface for a blog post: what a crawler gets for {@code /blog/{id}}.
 *
 * Both endpoints are {@code permitAll} (SecurityConfig) because no crawler carries a token —
 * WeChat, Weibo and Telegram fetch the URL anonymously, and an endpoint that answers 401 produces
 * the bare card this exists to fix. They are therefore the only blog reads in the app that answer
 * without authentication, and both gate on {@link BlogPreviewService#isPreviewable} first.
 *
 * Separate from {@link BlogResource} on purpose: that controller is JSON for the signed-in app,
 * this one is HTML for anonymous robots, and the two have opposite defaults.
 */
@RestController
@RequestMapping("/api/v1")
public class BlogPreviewResource {

    private static final Logger LOG = LoggerFactory.getLogger(BlogPreviewResource.class);

    /** text/html alone would be sent as ISO-8859-1 and mangle every Chinese title. */
    private static final MediaType HTML_UTF8 =
        new MediaType(MediaType.TEXT_HTML, java.nio.charset.StandardCharsets.UTF_8);

    /** A hostname with an optional port, and nothing else. See {@link #baseUrl}. */
    private static final Pattern SAFE_HOST = Pattern.compile("^[A-Za-z0-9.-]+(:\\d{1,5})?$");

    private final BlogService blogService;

    private final BlogPreviewService blogPreviewService;

    /** Set this in production; see application.yaml. Empty means "derive from the request". */
    @Value("${app.preview.base-url:}")
    private String configuredBaseUrl;

    public BlogPreviewResource(BlogService blogService, BlogPreviewService blogPreviewService) {
        this.blogService = blogService;
        this.blogPreviewService = blogPreviewService;
    }

    /**
     * GET /blogs/{id}/preview : the HTML a link crawler reads to build the card.
     *
     * Answers 200 with meta tags for a public post and 200 with an empty shell for anything else.
     * Not 404 for the unavailable case: several crawlers discard the body on a 404 and fall back
     * to showing the raw URL, and the shell is what a person following a stale link should see.
     */
    @GetMapping("/blogs/{id}/preview")
    public ResponseEntity<String> preview(@PathVariable("id") Long id, HttpServletRequest request) {
        LOG.debug("Link preview requested for Blog : {}", id);
        Optional<BlogDTO> blog = blogService.findOne(id);

        String html = blog.filter(blogPreviewService::isPreviewable)
            .map(dto -> blogPreviewService.render(dto, baseUrl(request)))
            .orElseGet(blogPreviewService::renderNotFound);

        return ResponseEntity.ok()
            .contentType(HTML_UTF8)
            // Short, not none: a card that is shared widely is fetched once per recipient's
            // client, and five minutes is well inside the window where an edit still shows up.
            .cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)).cachePublic())
            .body(html);
    }

    /**
     * GET /blogs/{id}/preview-image : the post's first inline image, decoded.
     *
     * RichEditor stores an uploaded file as a base64 data URI inside the post body, which no
     * crawler can fetch. This gives those bytes a real URL so {@code og:image} can point at one.
     */
    @GetMapping("/blogs/{id}/preview-image")
    public ResponseEntity<byte[]> previewImage(@PathVariable("id") Long id) {
        LOG.debug("Link preview image requested for Blog : {}", id);
        BlogPreviewService.PreviewImage image = blogService.findOne(id)
            .filter(blogPreviewService::isPreviewable)
            .map(blogPreviewService::inlineImage)
            .orElse(null);

        if (image == null) {
            return ResponseEntity.notFound()
                .cacheControl(CacheControl.noStore())
                .build();
        }
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(image.contentType()))
            // The type is the one the author's data URI declared, and the bytes are theirs too.
            // Without nosniff a browser may sniff them as HTML and run it on our origin.
            .header("X-Content-Type-Options", "nosniff")
            .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePublic())
            .body(image.bytes());
    }

    /**
     * The absolute origin to build {@code og:url} and {@code og:image} from, or null for neither.
     *
     * {@code app.preview.base-url} wins when it is set, and setting it is the right answer in
     * production. Otherwise this is derived from the request — and the {@code Host} header is
     * supplied by the caller, so it is validated rather than trusted: these two values are
     * *published*, and an unchecked header would let a crawler be handed a card whose image and
     * canonical link point at a host of someone else's choosing.
     *
     * Returning null when nothing can be trusted is deliberate. A card with a title and a
     * description and no image still reads correctly; one pointing at the wrong origin does not.
     */
    private String baseUrl(HttpServletRequest request) {
        if (configuredBaseUrl != null && !configuredBaseUrl.isBlank()) {
            return configuredBaseUrl.trim().replaceAll("/+$", "");
        }

        String host = request.getHeader("Host");
        if (host == null || !SAFE_HOST.matcher(host).matches()) {
            // Not logged with the value: it is attacker-controlled and would be a CRLF injection
            // into the log line.
            LOG.warn("No usable Host header for blog preview; set app.preview.base-url. "
                + "Card will omit og:url and og:image.");
            return null;
        }

        // nginx forwards the original scheme; it is the only signal here that distinguishes the
        // 443 server block from the 80 one, since the proxy hop itself is plain HTTP.
        String forwarded = request.getHeader("X-Forwarded-Proto");
        String scheme = "https".equalsIgnoreCase(forwarded) ? "https"
            : "http".equalsIgnoreCase(forwarded) ? "http"
            : request.isSecure() ? "https" : "http";
        return scheme + "://" + host;
    }
}
