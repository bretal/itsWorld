package com.itsworld.community;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class FollowIntegrationTest {
    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("its_world")
            .withInitScript("schema-test.sql");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("jwt.secret", () -> "itsworld-integration-test-secret-key-2026-0123456789-abcdefghijklmnopqrstuvwxyz");
        registry.add("minio.endpoint", () -> "http://localhost:9000");
        registry.add("minio.access-key", () -> "test-access");
        registry.add("minio.secret-key", () -> "test-secret");
    }

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

    @Test
    void searchesPublicNicknamesAndReturnsFollowingState() throws Exception {
        int viewerId = createUser("search_viewer");
        int exactMatch = createUser("search_exact");
        int partialMatch = createUser("search_partial");
        int disabled = createUser("search_disabled");
        jdbc.update("UPDATE sys_user SET nick_name = '团子' WHERE id IN (?, ?)", viewerId, exactMatch);
        jdbc.update("UPDATE sys_user SET nick_name = '糯米团子%' WHERE id = ?", partialMatch);
        jdbc.update("UPDATE sys_user SET nick_name = '团子', status = 0 WHERE id = ?", disabled);
        jdbc.update("INSERT INTO user_follow(follower_id, following_id) VALUES (?, ?), (?, ?)",
                viewerId, exactMatch, partialMatch, viewerId);
        var viewer = user("search_viewer");

        mvc.perform(get("/api/community/users/search").param("keyword", " 团子 ").param("size", "1").with(viewer))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.records[0].id").value(exactMatch))
                .andExpect(jsonPath("$.data.records[0].isFollowing").value(true));
        mvc.perform(get("/api/community/users/search").param("keyword", "%").with(viewer))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].id").value(partialMatch))
                .andExpect(jsonPath("$.data.records[0].isFollowing").value(false))
                .andExpect(jsonPath("$.data.records[0].followsMe").value(true));
        mvc.perform(get("/api/community/users/search").param("keyword", "search_exact").with(viewer))
                .andExpect(jsonPath("$.data.total").value(0));
    }

    @Test
    void paginatesFansAndRefreshesRelationshipAndFeedAfterMutations() throws Exception {
        long owner = createUser("follow_owner");
        int firstFan = createUser("follow_first");
        int latestFan = createUser("follow_latest");
        jdbc.update("INSERT INTO user_follow(follower_id, following_id) VALUES (?, ?), (?, ?)",
                firstFan, owner, latestFan, owner);
        jdbc.update("INSERT INTO community_post(user_id, title, content, status) VALUES (?, 'title', 'body', 1)",
                latestFan);
        var viewer = user("follow_owner");

        mvc.perform(get("/api/community/followers/page").param("size", "1").with(viewer))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.totalPages").value(2))
                .andExpect(jsonPath("$.data.records[0].id").value(latestFan))
                .andExpect(jsonPath("$.data.records[0].nickName").value("follow_latest"))
                .andExpect(jsonPath("$.data.records[0].isFollowing").value(false))
                .andExpect(jsonPath("$.data.records[0].followsMe").value(true));

        mvc.perform(post("/api/community/follow/{id}", latestFan).with(viewer))
                .andExpect(jsonPath("$.code").value(0));
        mvc.perform(get("/api/community/following/page").with(viewer))
                .andExpect(jsonPath("$.data.records[0].id").value(latestFan))
                .andExpect(jsonPath("$.data.records[0].isFollowing").value(true))
                .andExpect(jsonPath("$.data.records[0].followsMe").value(true));
        mvc.perform(get("/api/community/following").with(viewer))
                .andExpect(jsonPath("$.data[0]").value(latestFan));
        mvc.perform(get("/api/community/follow/stats").with(viewer))
                .andExpect(jsonPath("$.data.followingCount").value(1))
                .andExpect(jsonPath("$.data.followerCount").value(2));
        long postId = jdbc.queryForObject("SELECT id FROM community_post WHERE user_id = ?", Long.class, latestFan);
        jdbc.update("INSERT INTO community_like(post_id, user_id) VALUES (?, ?)", postId, owner);
        mvc.perform(get("/api/community/page").param("type", "recommend").with(viewer))
                .andExpect(jsonPath("$.data.records[0].username").value("follow_latest"))
                .andExpect(jsonPath("$.data.records[0].isLiked").value(true));
        mvc.perform(get("/api/community/page").param("type", "following").with(viewer))
                .andExpect(jsonPath("$.data.records[0].isFollowing").value(true))
                .andExpect(jsonPath("$.data.records[0].isLiked").value(true));

        mvc.perform(delete("/api/community/follow/{id}", latestFan).with(viewer))
                .andExpect(jsonPath("$.code").value(0));
        mvc.perform(get("/api/community/page").param("type", "following").with(viewer))
                .andExpect(jsonPath("$.data.total").value(0));
        mvc.perform(get("/api/community/followers/page").with(viewer))
                .andExpect(jsonPath("$.data.records[0].isFollowing").value(false))
                .andExpect(jsonPath("$.data.records[0].followsMe").value(true));
    }

    private int createUser(String username) {
        jdbc.update("INSERT INTO sys_user(username, password_hash) VALUES (?, 'test')", username);
        return jdbc.queryForObject("SELECT id FROM sys_user WHERE username = ?", Integer.class, username);
    }
}
