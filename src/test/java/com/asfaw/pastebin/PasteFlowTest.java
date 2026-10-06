package com.asfaw.pastebin;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class PasteFlowTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void createViaFormThenViewRenderedPage() throws Exception {
        MvcResult result = mvc.perform(post("/paste")
                        .with(csrf())
                        .param("title", "flow test")
                        .param("content", "full stack content")
                        .param("expiry", "NEVER")
                        .param("visibility", "UNLISTED")
                        .param("language", "plaintext"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/p/*"))
                .andReturn();

        String location = result.getResponse().getRedirectedUrl();
        assertThat(location).isNotNull();

        mvc.perform(get(location))
                .andExpect(status().isOk())
                .andExpect(view().name("paste/view"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("full stack content")));
    }

    @Test
    void blankContentRedisplaysFormWithErrors() throws Exception {
        mvc.perform(post("/paste")
                        .with(csrf())
                        .param("title", "no content")
                        .param("content", "")
                        .param("expiry", "NEVER")
                        .param("visibility", "UNLISTED")
                        .param("language", "plaintext"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attributeHasFieldErrors("form", "content"));
    }

    @Test
    void missingPasteRenders404Page() throws Exception {
        mvc.perform(get("/p/missing99"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error/404"));
    }

    @Test
    void publicPasteAppearsInPublicList() throws Exception {
        mvc.perform(post("/paste")
                        .with(csrf())
                        .param("title", "shown in list")
                        .param("content", "public content")
                        .param("expiry", "NEVER")
                        .param("visibility", "PUBLIC")
                        .param("language", "plaintext"))
                .andExpect(status().is3xxRedirection());

        mvc.perform(get("/public"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("shown in list")));
    }

    @Test
    void protectedPasteShowsPasswordFormThenUnlocks() throws Exception {
        MvcResult result = mvc.perform(post("/paste")
                        .with(csrf())
                        .param("title", "locked")
                        .param("content", "secret content")
                        .param("expiry", "NEVER")
                        .param("visibility", "UNLISTED")
                        .param("language", "plaintext")
                        .param("password", "pw123"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        String location = result.getResponse().getRedirectedUrl();

        mvc.perform(get(location))
                .andExpect(status().isOk())
                .andExpect(view().name("paste/password"));

        mvc.perform(post(location + "/unlock").with(csrf()).param("password", "pw123"))
                .andExpect(status().isOk())
                .andExpect(view().name("paste/view"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("secret content")));
    }
}
