package com.asfaw.pastebin.ratelimit;

import com.asfaw.pastebin.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "pastebin.rate-limit.capacity=2")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class RateLimitFlowTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void postsBeyondCapacityGet429WhileGetsStayUnlimited() throws Exception {
        for (int i = 0; i < 2; i++) {
            mvc.perform(post("/paste")
                            .with(csrf())
                            .param("content", "allowed " + i)
                            .param("expiry", "NEVER")
                            .param("visibility", "UNLISTED")
                            .param("language", "plaintext"))
                    .andExpect(status().is3xxRedirection());
        }

        mvc.perform(post("/paste")
                        .with(csrf())
                        .param("content", "over the limit")
                        .param("expiry", "NEVER")
                        .param("visibility", "UNLISTED")
                        .param("language", "plaintext"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));

        mvc.perform(get("/public")).andExpect(status().isOk());
    }
}
