package com.apidependencyradar.web;

import com.apidependencyradar.infrastructure.persistence.AppUserEntity;
import com.apidependencyradar.infrastructure.persistence.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppUserRepository userRepository;

    @Test
    void meWithoutAuthenticationReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void meWithAuthenticationReturnsUserProfile() throws Exception {
        String email = "authtest-" + UUID.randomUUID() + "@example.com";
        AppUserEntity savedUser = userRepository.saveAndFlush(
                new AppUserEntity(UUID.randomUUID().toString(), email, "hashed-password"));

        mockMvc.perform(get("/api/auth/me").with(user(email).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedUser.getId()))
                .andExpect(jsonPath("$.email").value(email));
    }

    @Test
    void registerWithValidDataReturnsCreated() throws Exception {
        String uniqueEmail = "newuser-" + UUID.randomUUID() + "@example.com";
        String payload = """
                {
                    "email": "%s",
                    "password": "Password123!@#"
                }
                """.formatted(uniqueEmail);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated());
    }

    @Test
    void registerWithDuplicateEmailReturnsBadRequest() throws Exception {
        String existingEmail = "duplicate-" + UUID.randomUUID() + "@example.com";
        userRepository.saveAndFlush(
                new AppUserEntity(UUID.randomUUID().toString(), existingEmail, "hashed-password"));

        String payload = """
                {
                    "email": "%s",
                    "password": "Password123!@#"
                }
                """.formatted(existingEmail);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }
}
