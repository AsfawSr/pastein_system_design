package com.asfaw.pastebin.user.api;

import com.asfaw.pastebin.TestcontainersConfiguration;
import com.asfaw.pastebin.user.UserService;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class JwtApiTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserService userService;

    @Test
    void tokenEndpointIssuesJwtForValidCredentials() throws Exception {
        userService.register("apiuser1", "longenoughpw");

        mvc.perform(post("/api/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username": "apiuser1", "password": "longenoughpw"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.expiresAt").isNotEmpty());
    }

    @Test
    void tokenEndpointRejectsBadCredentials() throws Exception {
        mvc.perform(post("/api/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username": "nobody", "password": "wrongwrong"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void bearerTokenLinksCreatedPasteToOwner() throws Exception {
        userService.register("apiowner", "longenoughpw");

        String tokenBody = mvc.perform(post("/api/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username": "apiowner", "password": "longenoughpw"}
                                """))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(tokenBody, "$.token");

        String pasteBody = mvc.perform(post("/api/pastes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content": "owned via api"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(pasteBody, "$.id");

        // owner can see it on the My Pastes page via session auth
        mvc.perform(get("/mine").with(
                        org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("apiowner")))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(org.hamcrest.Matchers.containsString(id)));
    }

    @Test
    void garbageBearerTokenIsRejected() throws Exception {
        mvc.perform(post("/api/pastes")
                        .header("Authorization", "Bearer not-a-real-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content": "should fail"}
                                """))
                .andExpect(status().isUnauthorized());
    }
}
