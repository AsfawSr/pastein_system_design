package com.asfaw.pastebin.user;

import com.asfaw.pastebin.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class AuthFlowTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void registerThenLoginSucceeds() throws Exception {
        mvc.perform(post("/register")
                        .with(csrf())
                        .param("username", "flowuser")
                        .param("password", "longenoughpw")
                        .param("confirmPassword", "longenoughpw"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        mvc.perform(formLogin("/login").user("flowuser").password("longenoughpw"))
                .andExpect(authenticated().withUsername("flowuser"));
    }

    @Test
    void wrongPasswordStaysUnauthenticated() throws Exception {
        mvc.perform(formLogin("/login").user("ghost").password("wrongwrong"))
                .andExpect(unauthenticated());
    }

    @Test
    void mismatchedPasswordsRedisplayRegistrationForm() throws Exception {
        mvc.perform(post("/register")
                        .with(csrf())
                        .param("username", "mismatch")
                        .param("password", "longenoughpw")
                        .param("confirmPassword", "different"))
                .andExpect(status().isOk());
    }
}
