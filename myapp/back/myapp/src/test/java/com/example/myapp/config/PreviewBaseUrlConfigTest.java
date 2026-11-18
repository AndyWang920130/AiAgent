package com.example.myapp.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.PropertySources;
import org.springframework.core.env.PropertySourcesPropertyResolver;
import org.springframework.core.io.ClassPathResource;

/**
 * What {@code app.preview.base-url} actually resolves to under the prod profile.
 *
 * Worth a test for one reason: {@code :} is Spring's placeholder default-separator and the value
 * is {@code ${APP_PREVIEW_BASE_URL:https://twsny.cn}}, whose default contains two more of them.
 * Everything after the *first* colon is the default, so this resolves correctly — but it is the
 * kind of expression that reads as though it might not, and the failure would be silent: a
 * truncated origin still renders a card, just one whose image and canonical link go nowhere.
 *
 * Loading the yaml directly rather than through @SpringBootTest keeps this off the database; the
 * prod profile points at a MySQL that does not exist on a developer's machine.
 */
class PreviewBaseUrlConfigTest {

    private static final String KEY = "app.preview.base-url";

    private PropertySources load(String... overrides) throws IOException {
        MutablePropertySources sources = new MutablePropertySources();
        if (overrides.length > 0) {
            // Stands in for the environment, which outranks the file — same as a container
            // receiving APP_PREVIEW_BASE_URL from compose.
            sources.addFirst(new MapPropertySource("overrides",
                java.util.Map.of(overrides[0], overrides[1])));
        }
        List<PropertySource<?>> loaded = new YamlPropertySourceLoader()
            .load("prod", new ClassPathResource("config/application-prod.yaml"));
        loaded.forEach(sources::addLast);
        return sources;
    }

    @Test
    @DisplayName("prod publishes the site's own origin")
    void theDefaultIsTheRealOrigin() throws IOException {
        String resolved = new PropertySourcesPropertyResolver(load()).getProperty(KEY);

        // Not startsWith: a default truncated at the first colon would still start with "https".
        assertThat(resolved).isEqualTo("https://twsny.cn");
    }

    @Test
    @DisplayName("the environment can point it at another domain")
    void anOverrideWins() throws IOException {
        PropertySources sources = load("APP_PREVIEW_BASE_URL", "https://staging.twsny.cn");
        String resolved = new PropertySourcesPropertyResolver(sources).getProperty(KEY);

        assertThat(resolved).isEqualTo("https://staging.twsny.cn");
    }

    @Test
    @DisplayName("the default profile derives the origin from the request instead")
    void theBaseFileLeavesItEmpty() throws IOException {
        // Dev reaches the app by whatever address the phone or browser used; there is no one
        // name to publish, so BlogPreviewResource falls back to the validated Host header.
        MutablePropertySources sources = new MutablePropertySources();
        new YamlPropertySourceLoader()
            .load("default", new ClassPathResource("config/application.yaml"))
            .forEach(sources::addLast);

        assertThat(new PropertySourcesPropertyResolver(sources).getProperty(KEY)).isEmpty();
    }
}
