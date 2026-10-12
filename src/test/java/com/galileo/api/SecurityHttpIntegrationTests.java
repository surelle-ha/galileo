package com.galileo.api;

import com.galileo.api.role.RoleName;
import com.galileo.api.security.AuthenticationService;
import com.galileo.api.security.LoginRequest;
import com.galileo.api.user.UserCreateRequest;
import com.galileo.api.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SecurityHttpIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private AuthenticationService authenticationService;

    @Test
    void loginIsPublicButUserDirectoryRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/auth/login")
                        .header("X-Correlation-Id", "security-test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "admin@test.local",
                                  "password": "TestAdmin123!"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Correlation-Id", "security-test"))
                .andExpect(header().string("X-Service-Name", "GalileoAPI"))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty());
    }

    @Test
    void ordinaryUserCanChangeOwnPasswordButCannotDeleteUsers() throws Exception {
        var user = userService.create(new UserCreateRequest(
                "Grace",
                "Hopper",
                "grace@galileo.test",
                "InitialPassword123!",
                "Engineer",
                null,
                Set.of(RoleName.USER)
        ));
        String token = authenticationService.login(
                new LoginRequest("grace@galileo.test", "InitialPassword123!"))
                .accessToken();

        mockMvc.perform(patch("/api/v1/users/{id}/password", user.id())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"newPassword": "ChangedPassword123!"}
                                """))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/v1/users/{id}", user.id())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }
}
