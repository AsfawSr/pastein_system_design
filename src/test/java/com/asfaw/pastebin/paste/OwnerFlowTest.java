package com.asfaw.pastebin.paste;

import com.asfaw.pastebin.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class OwnerFlowTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void loggedInUsersPasteShowsUpOnMyPastesPage() throws Exception {
        mvc.perform(post("/register")
                        .with(csrf())
                        .param("username", "owner1")
                        .param("password", "longenoughpw")
                        .param("confirmPassword", "longenoughpw"))
                .andExpect(status().is3xxRedirection());

        mvc.perform(post("/paste")
                        .with(user("owner1"))
                        .with(csrf())
                        .param("title", "owned paste")
                        .param("content", "owned content")
                        .param("expiry", "NEVER")
                        .param("visibility", "UNLISTED")
                        .param("language", "plaintext"))
                .andExpect(status().is3xxRedirection());

        mvc.perform(get("/mine").with(user("owner1")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("owned paste")));
    }

    @Test
    void anonymousUserIsRedirectedToLoginForMyPastes() throws Exception {
        mvc.perform(get("/mine"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void ownerCanEditAndNonOwnerGets403() throws Exception {
        mvc.perform(post("/register")
                        .with(csrf())
                        .param("username", "editor1")
                        .param("password", "longenoughpw")
                        .param("confirmPassword", "longenoughpw"))
                .andExpect(status().is3xxRedirection());

        var result = mvc.perform(post("/paste")
                        .with(user("editor1"))
                        .with(csrf())
                        .param("title", "editable")
                        .param("content", "before edit")
                        .param("expiry", "NEVER")
                        .param("visibility", "UNLISTED")
                        .param("language", "plaintext"))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        String location = result.getResponse().getRedirectedUrl();

        mvc.perform(post(location + "/edit")
                        .with(user("editor1"))
                        .with(csrf())
                        .param("title", "editable")
                        .param("content", "after edit")
                        .param("language", "plaintext"))
                .andExpect(status().is3xxRedirection());

        mvc.perform(get(location))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("after edit")));

        mvc.perform(get(location + "/edit").with(user("someoneelse")))
                .andExpect(status().isForbidden());
    }

    @Test
    void ownerCanDeleteTheirPaste() throws Exception {
        mvc.perform(post("/register")
                        .with(csrf())
                        .param("username", "deleter1")
                        .param("password", "longenoughpw")
                        .param("confirmPassword", "longenoughpw"))
                .andExpect(status().is3xxRedirection());

        var result = mvc.perform(post("/paste")
                        .with(user("deleter1"))
                        .with(csrf())
                        .param("title", "doomed")
                        .param("content", "delete me")
                        .param("expiry", "NEVER")
                        .param("visibility", "UNLISTED")
                        .param("language", "plaintext"))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        String location = result.getResponse().getRedirectedUrl();

        mvc.perform(post(location + "/delete").with(user("deleter1")).with(csrf()))
                .andExpect(status().is3xxRedirection());

        mvc.perform(get(location))
                .andExpect(status().isNotFound());
    }
}
