package com.example.myapp.config;

import com.example.myapp.contants.enumeration.BlogStatus;
import com.example.myapp.domain.Blog;
import com.example.myapp.repository.BlogRepository;
import com.example.myapp.security.InMemoryUserStore;
import com.example.myapp.security.InMemoryUserStore.StoredUser;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer {

    private final InMemoryUserStore userStore;
    private final PasswordEncoder passwordEncoder;
    private final BlogRepository blogRepository;

    @PostConstruct
    void init() {
        userStore.save(new StoredUser("admin", "Administrator", "admin@example.com", passwordEncoder.encode("admin")));
        userStore.save(new StoredUser("user", "Demo User", "user@example.com", passwordEncoder.encode("password")));

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
    }
}
