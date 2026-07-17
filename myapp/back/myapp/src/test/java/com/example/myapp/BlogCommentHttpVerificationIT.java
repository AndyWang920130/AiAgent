package com.example.myapp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.myapp.domain.Blog;
import com.example.myapp.domain.User;
import com.example.myapp.repository.BlogRepository;
import com.example.myapp.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Exercises the comment feature purely at the HTTP layer: real login (no forged tokens),
 * real controller mappings, real JSON serialization — end-to-end through the actual REST API.
 */
@SpringBootTest
@AutoConfigureMockMvc
class BlogCommentHttpVerificationIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BlogRepository blogRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private void seedUser(String login) {
        userRepository.save(new User()
            .login(login)
            .realName(login)
            .nickName(login)
            .email(login + "@example.com")
            .password(passwordEncoder.encode("Password123!"))
            .deleted(false));
    }

    private String loginAndGetToken(String login) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + login + "\",\"password\":\"Password123!\"}"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("token").asText();
    }

    @Test
    void commentEndpointsWorkOverRealHttpWithRealLogin() throws Exception {
        seedUser("http_alice");
        seedUser("http_bob");
        String aliceToken = loginAndGetToken("http_alice");
        String bobToken = loginAndGetToken("http_bob");

        Blog blog = new Blog();
        blog.setTitle("HTTP verification post");
        blog.setContent("content");
        blog.setAuthor("http_alice");
        blog = blogRepository.save(blog);
        Long blogId = blog.getId();

        // Alice posts a comment.
        String createBody = mockMvc.perform(post("/api/v1/blogs/{blogId}/comments", blogId)
                .header("Authorization", "Bearer " + aliceToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"Hello from HTTP alice\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.username").value("http_alice"))
            .andExpect(jsonPath("$.canDelete").value(true))
            .andReturn().getResponse().getContentAsString();
        long commentId = objectMapper.readTree(createBody).get("id").asLong();

        // Listing as bob shows the comment but canDelete is false for a non-author, non-owner.
        mockMvc.perform(get("/api/v1/blogs/{blogId}/comments", blogId)
                .header("Authorization", "Bearer " + bobToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].canDelete").value(false));

        // Bob cannot delete alice's comment.
        mockMvc.perform(delete("/api/v1/comments/{commentId}", commentId)
                .header("Authorization", "Bearer " + bobToken))
            .andExpect(status().isForbidden());

        // Blog author (and comment author) deletes it successfully.
        mockMvc.perform(delete("/api/v1/comments/{commentId}", commentId)
                .header("Authorization", "Bearer " + aliceToken))
            .andExpect(status().isNoContent());

        assertThat(blogRepository.findById(blogId).orElseThrow().getCommentCount()).isEqualTo(0L);
    }
}
