package com.example.myapp.config;

import com.example.myapp.contants.enumeration.BlogStatus;
import com.example.myapp.contants.enumeration.BlogConfigType;
import com.example.myapp.contants.enumeration.GameConfigType;
import com.example.myapp.contants.enumeration.AchievementType;
import com.example.myapp.contants.enumeration.Role;
import com.example.myapp.domain.Blog;
import com.example.myapp.domain.BlogConfig;
import com.example.myapp.domain.GameConfig;
import com.example.myapp.domain.User;
import com.example.myapp.repository.AchievementRepository;
import com.example.myapp.repository.BlogConfigRepository;
import com.example.myapp.repository.BlogRepository;
import com.example.myapp.repository.GameConfigRepository;
import com.example.myapp.repository.UserRepository;
import com.example.myapp.service.AchievementService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class DataInitializer {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final BlogRepository blogRepository;
    private final BlogConfigRepository blogConfigRepository;
    private final GameConfigRepository gameConfigRepository;
    private final AchievementRepository achievementRepository;
    private final AchievementService achievementService;

    @PostConstruct
    void init() {
        createUserIfMissing("admin", "Administrator", "admin@example.com", "admin", Role.ADMIN);
        createUserIfMissing("user", "Demo User", "user@example.com", "password", Role.USER);

        if (blogRepository.count() == 0) {
            blogRepository.save(new Blog()
                .title("Getting Started with Vue 3 Composition API")
                .summary("The Composition API is one of the most exciting features in Vue 3. It provides a flexible way to organize component logic...")
                .content("<p>The Composition API is one of the most exciting features in Vue 3. It provides a flexible way to organize component logic.</p><p>By separating concerns into composable functions, you can create more maintainable and reusable code. Key benefits include better TypeScript support, improved code organization, and easier testing.</p>")
                .category("Frontend")
                .tag("Vue")
                .tagColor("green")
                .author("admin")
                .status(BlogStatus.PUBLISHED)
                .viewCount(1240L)
                .likes(89L)
                .commentCount(23L)
                .deleted(false));

            blogRepository.save(new Blog()
                .title("Building Scalable APIs with Spring Boot")
                .summary("Spring Boot makes it easy to create stand-alone, production-grade Spring applications with minimal configuration...")
                .content("<p>Spring Boot makes it easy to create stand-alone, production-grade Spring applications with minimal configuration.</p><p>With embedded servers and auto-configuration, you can get a REST API running in minutes. The framework handles dependency injection, transaction management, and security out of the box.</p>")
                .category("Backend")
                .tag("Java")
                .tagColor("orange")
                .author("admin")
                .status(BlogStatus.PUBLISHED)
                .viewCount(980L)
                .likes(64L)
                .commentCount(15L)
                .deleted(false));

            blogRepository.save(new Blog()
                .title("TypeScript Best Practices in 2025")
                .summary("TypeScript has become the go-to choice for large-scale JavaScript applications. Here are the patterns that matter most...")
                .content("<p>TypeScript has become the go-to choice for large-scale JavaScript applications. Here are the patterns that matter most in 2025.</p><ul><li>Always use strict mode</li><li>Prefer type inference over explicit annotations</li><li>Use utility types like Partial, Required, and Pick</li><li>Leverage template literal types for string manipulation</li></ul>")
                .category("Language")
                .tag("TypeScript")
                .tagColor("blue")
                .author("admin")
                .status(BlogStatus.PUBLISHED)
                .viewCount(2100L)
                .likes(142L)
                .commentCount(37L)
                .deleted(false));

            blogRepository.save(new Blog()
                .title("Docker & Kubernetes: A Practical Guide")
                .summary("Containerization has revolutionized how we deploy applications. Learn how to effectively use Docker and K8s together...")
                .content("<p>Containerization has revolutionized how we deploy applications. Learn how to effectively use Docker and Kubernetes together.</p><p>Start with a solid Dockerfile, define resource limits in your K8s manifests, and use Helm charts to manage complex deployments across environments.</p>")
                .category("DevOps")
                .tag("DevOps")
                .tagColor("purple")
                .author("admin")
                .status(BlogStatus.PUBLISHED)
                .viewCount(1560L)
                .likes(98L)
                .commentCount(29L)
                .deleted(false));
        }

        initBlogConfig();
        initGameConfig();
        backfillAchievements();
    }

    /**
     * One-time backfill of achievement points from content that existed before
     * the achievement system (e.g. seeded blogs). Runs per author only when that
     * author has no achievement rows yet, so it is safe across restarts and never
     * double-counts. Registration points are not backfilled — there is no record
     * of who self-registered vs. was seeded.
     */
    private void backfillAchievements() {
        List<Blog> blogs = blogRepository.findAll();
        Map<String, long[]> byAuthor = new HashMap<>(); // author -> [publishedCount, likesReceived]
        for (Blog blog : blogs) {
            String author = blog.getAuthor();
            if (author == null || author.isBlank()) {
                continue;
            }
            long[] tally = byAuthor.computeIfAbsent(author, a -> new long[2]);
            if (blog.getStatus() == BlogStatus.PUBLISHED) {
                tally[0] += 1;
            }
            tally[1] += blog.getLikes() == null ? 0L : blog.getLikes();
        }
        for (Map.Entry<String, long[]> entry : byAuthor.entrySet()) {
            String author = entry.getKey();
            if (achievementRepository.existsByLogin(author)) {
                continue; // already has achievements — leave live-earned data untouched
            }
            long published = entry.getValue()[0];
            long likes = entry.getValue()[1];
            achievementService.award(author, AchievementType.PUBLISH_ARTICLE, published * AchievementType.PUBLISH_ARTICLE.getDefaultPoints());
            achievementService.award(author, AchievementType.RECEIVE_LIKE, likes * AchievementType.RECEIVE_LIKE.getDefaultPoints());
        }
    }

    private void initBlogConfig() {
        createBlogConfigIfMissing(BlogConfigType.CATEGORY, "Frontend", null, "UI, Vue, JavaScript, CSS", 10);
        createBlogConfigIfMissing(BlogConfigType.CATEGORY, "Backend", null, "Services, APIs, databases", 20);
        createBlogConfigIfMissing(BlogConfigType.CATEGORY, "Language", null, "Programming language notes", 30);
        createBlogConfigIfMissing(BlogConfigType.CATEGORY, "DevOps", null, "Build, deployment, operations", 40);
        createBlogConfigIfMissing(BlogConfigType.TAG, "Vue", null, null, 10);
        createBlogConfigIfMissing(BlogConfigType.TAG, "Java", null, null, 20);
        createBlogConfigIfMissing(BlogConfigType.TAG, "Spring", null, null, 30);
        createBlogConfigIfMissing(BlogConfigType.TAG, "TypeScript", null, null, 40);
        createBlogConfigIfMissing(BlogConfigType.TAG_COLOR, "Blue", "blue", null, 10);
        createBlogConfigIfMissing(BlogConfigType.TAG_COLOR, "Green", "green", null, 20);
        createBlogConfigIfMissing(BlogConfigType.TAG_COLOR, "Orange", "orange", null, 30);
        createBlogConfigIfMissing(BlogConfigType.TAG_COLOR, "Purple", "purple", null, 40);
        createBlogConfigIfMissing(BlogConfigType.TAG_COLOR, "Red", "red", null, 50);
        createBlogConfigIfMissing(BlogConfigType.TAG_COLOR, "Cyan", "cyan", null, 60);
        createBlogConfigIfMissing(BlogConfigType.TAG_COLOR, "Geek Blue", "geekblue", null, 70);
    }

    private void createBlogConfigIfMissing(BlogConfigType type, String name, String value, String description, Integer sortOrder) {
        if (blogConfigRepository.existsByTypeAndNameIgnoreCase(type, name)) {
            return;
        }
        blogConfigRepository.save(new BlogConfig()
            .type(type)
            .name(name)
            .value(value)
            .description(description)
            .sortOrder(sortOrder));
    }

    private void initGameConfig() {
        createGameConfigIfMissing(GameConfigType.WHEEL_PRIZE, "Grand Prize", "red", null, 10);
        createGameConfigIfMissing(GameConfigType.WHEEL_PRIZE, "First Prize", "orange", null, 20);
        createGameConfigIfMissing(GameConfigType.WHEEL_PRIZE, "Second Prize", "gold", null, 30);
        createGameConfigIfMissing(GameConfigType.WHEEL_PRIZE, "Third Prize", "green", null, 40);
        createGameConfigIfMissing(GameConfigType.WHEEL_PRIZE, "Try Again", "blue", null, 50);
        createGameConfigIfMissing(GameConfigType.WHEEL_PRIZE, "Lucky Draw", "purple", null, 60);
        createGameConfigIfMissing(GameConfigType.PARAMETER, "spinDurationSeconds", "4", "How long the wheel spins, in seconds", 10);

        String[] listPrizeColors = { "red", "orange", "gold", "green", "cyan", "blue", "purple", "magenta" };
        for (int i = 1; i <= 20; i++) {
            createGameConfigIfMissing(
                GameConfigType.LIST_PRIZE,
                "Student " + i,
                listPrizeColors[(i - 1) % listPrizeColors.length],
                null,
                i * 10
            );
        }
    }

    private void createGameConfigIfMissing(GameConfigType type, String name, String value, String description, Integer sortOrder) {
        if (gameConfigRepository.existsByTypeAndNameIgnoreCase(type, name)) {
            return;
        }
        gameConfigRepository.save(new GameConfig()
            .type(type)
            .name(name)
            .value(value)
            .description(description)
            .sortOrder(sortOrder));
    }

    private void createUserIfMissing(String login, String name, String email, String password, Role role) {
        if (userRepository.existsByLogin(login)) {
            return;
        }
        userRepository.save(new User()
            .login(login)
            .realName(name)
            .nickName(name)
            .email(email)
            .password(passwordEncoder.encode(password))
            .role(role)
            .deleted(false));
    }
}
