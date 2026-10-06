package com.asfaw.pastebin.paste.api;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PasteApiTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void createThenFetchRoundTrip() throws Exception {
        String body = mvc.perform(post("/api/pastes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "api paste", "content": "hello from the api"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        String id = JsonPath.read(body, "$.id");

        mvc.perform(get("/api/pastes/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("api paste"))
                .andExpect(jsonPath("$.content").value("hello from the api"))
                .andExpect(jsonPath("$.visibility").value("UNLISTED"));
    }

    @Test
    void blankContentIsRejectedWith400() throws Exception {
        mvc.perform(post("/api/pastes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content": "   "}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void missingPasteYields404ProblemDetail() throws Exception {
        mvc.perform(get("/api/pastes/doesNot1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void burnAfterReadIsGoneOnSecondFetch() throws Exception {
        String body = mvc.perform(post("/api/pastes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content": "secret", "burnAfterRead": true}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String id = JsonPath.read(body, "$.id");

        mvc.perform(get("/api/pastes/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.burned").value(true));

        mvc.perform(get("/api/pastes/" + id))
                .andExpect(status().isNotFound());
    }

    @Test
    void passwordProtectedPasteNeedsHeader() throws Exception {
        String body = mvc.perform(post("/api/pastes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content": "locked", "password": "s3cret"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String id = JsonPath.read(body, "$.id");

        mvc.perform(get("/api/pastes/" + id))
                .andExpect(status().isUnauthorized());

        mvc.perform(get("/api/pastes/" + id).header("X-Paste-Password", "wrong"))
                .andExpect(status().isUnauthorized());

        mvc.perform(get("/api/pastes/" + id).header("X-Paste-Password", "s3cret"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("locked"));
    }
}
