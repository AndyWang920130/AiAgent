package com.example.myapp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.myapp.domain.Blog;
import com.example.myapp.repository.BlogRepository;
import com.example.myapp.service.BlogCommentService;
import com.example.myapp.service.dto.BlogCommentDTO;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
class BlogCommentVerificationIT {

    @Autowired
    private BlogRepository blogRepository;

    @Autowired
    private BlogCommentService blogCommentService;

    private void loginAs(String username) {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(username, null, List.of())
        );
    }

    @Test
    void commentLifecycleUpdatesCountAndEnforcesPermissions() {
        // Blog authored by "dave", who is neither commenter, so ownership vs. authorship can be tested separately.
        Blog blog = new Blog();
        blog.setTitle("Verification post");
        blog.setContent("content");
        blog.setAuthor("dave");
        blog = blogRepository.save(blog);
        Long blogId = blog.getId();

        loginAs("alice");
        BlogCommentDTO aliceComment = blogCommentService.create(blogId, "Hello from alice").orElseThrow();
        assertThat(aliceComment.canDelete()).isTrue();

        loginAs("bob");
        BlogCommentDTO bobComment = blogCommentService.create(blogId, "Hello from bob").orElseThrow();

        assertThat(blogRepository.findById(blogId).orElseThrow().getCommentCount()).isEqualTo(2L);

        List<BlogCommentDTO> asBob = blogCommentService.list(blogId, PageRequest.of(0, 20)).orElseThrow().getContent();
        assertThat(asBob).hasSize(2);
        BlogCommentDTO bobOwnFromList = asBob.stream().filter(c -> c.id().equals(bobComment.id())).findFirst().orElseThrow();
        assertThat(bobOwnFromList.canDelete()).isTrue();
        BlogCommentDTO aliceFromBobsView = asBob.stream().filter(c -> c.id().equals(aliceComment.id())).findFirst().orElseThrow();
        assertThat(aliceFromBobsView.canDelete()).isFalse();

        // Anonymous viewer: neither comment is deletable.
        SecurityContextHolder.clearContext();
        List<BlogCommentDTO> anonymousView = blogCommentService.list(blogId, PageRequest.of(0, 20)).orElseThrow().getContent();
        assertThat(anonymousView).allMatch(c -> !c.canDelete());

        // Uninvolved third party cannot delete someone else's comment.
        loginAs("carol");
        assertThatThrownBy(() -> blogCommentService.delete(bobComment.id()))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("403");

        // Author deletes their own comment (not the blog owner).
        loginAs("alice");
        blogCommentService.delete(aliceComment.id());
        assertThat(blogRepository.findById(blogId).orElseThrow().getCommentCount()).isEqualTo(1L);

        // Blog owner deletes someone else's comment (not its author).
        loginAs("dave");
        blogCommentService.delete(bobComment.id());
        assertThat(blogRepository.findById(blogId).orElseThrow().getCommentCount()).isEqualTo(0L);
    }
}
