package com.asfaw.pastebin.apikey;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class ApiKeyFlowTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserService userService;

    @Test
    void keyManagementRequiresAuthentication() throws Exception {
        mvc.perform(get("/api/keys")).andExpect(status().isUnauthorized());
    }

    @Test
    void issueUseListAndRevokeKey() throws Exception {
        userService.register("keyuser", "longenoughpw");
        String jwt = obtainJwt("keyuser", "longenoughpw");

        String issued = mvc.perform(post("/api/keys")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"label": "ci-script"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.key").value(org.hamcrest.Matchers.startsWith("pb_")))
                .andReturn().getResponse().getContentAsString();
        String plaintextKey = JsonPath.read(issued, "$.key");
        int keyId = JsonPath.read(issued, "$.id");

        // the key authenticates paste creation and links ownership
        String paste = mvc.perform(post("/api/pastes")
                        .header("X-Api-Key", plaintextKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content": "created with api key"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        assertThat((String) JsonPath.read(paste, "$.id")).isNotBlank();

        // listing never exposes the key material
        mvc.perform(get("/api/keys").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].label").value("ci-script"))
                .andExpect(jsonPath("$[0].key").doesNotExist());

        mvc.perform(delete("/api/keys/" + keyId).header("Authorization", "Bearer " + jwt))
                .andExpect(status().isNoContent());

        mvc.perform(post("/api/pastes")
                        .header("X-Api-Key", plaintextKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content": "revoked key"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void cannotRevokeSomeoneElsesKey() throws Exception {
        userService.register("keyowner2", "longenoughpw");
        userService.register("intruder2", "longenoughpw");

        String ownerJwt = obtainJwt("keyowner2", "longenoughpw");
        String issued = mvc.perform(post("/api/keys")
                        .header("Authorization", "Bearer " + ownerJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"label": "private"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        int keyId = JsonPath.read(issued, "$.id");

        String intruderJwt = obtainJwt("intruder2", "longenoughpw");
        mvc.perform(delete("/api/keys/" + keyId).header("Authorization", "Bearer " + intruderJwt))
                .andExpect(status().isForbidden());
    }

    private String obtainJwt(String username, String password) throws Exception {
        String body = mvc.perform(post("/api/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\": \"%s\", \"password\": \"%s\"}".formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.token");
    }
}
