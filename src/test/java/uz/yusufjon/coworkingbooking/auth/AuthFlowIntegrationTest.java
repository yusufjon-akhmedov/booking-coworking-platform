package uz.yusufjon.coworkingbooking.auth;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import uz.yusufjon.coworkingbooking.support.AbstractMockMvcIntegrationTest;
import uz.yusufjon.coworkingbooking.user.entity.Role;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthFlowIntegrationTest extends AbstractMockMvcIntegrationTest {

    @Test
    void registerLoginRefreshAndLogoutFlowWorks() throws Exception {
        String email = uniqueEmail("auth-flow");
        String password = "Secret123";

        AuthTokens registered = registerUser("Auth Flow User", email, password);
        AuthTokens loggedIn = loginUser(email, password);
        AuthTokens refreshed = refreshToken(loggedIn.refreshToken());

        assertThat(registered.accessToken()).isNotBlank();
        assertThat(loggedIn.refreshToken()).isNotBlank();
        assertThat(refreshed.refreshToken()).isNotEqualTo(loggedIn.refreshToken());

        logout(refreshed.accessToken(), refreshed.refreshToken());

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s"}
                                """.formatted(refreshed.refreshToken())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Refresh token is invalid or expired"));
    }

    @Test
    void refreshWithInvalidTokenReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"invalid-token"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Refresh token is invalid or expired"));
    }

    @Test
    void disabledUserCannotUseProtectedEndpointRefreshOrLogin() throws Exception {
        String customerEmail = uniqueEmail("disabled-customer");
        String customerPassword = "Secret123";
        AuthTokens customerTokens = registerUser("Disabled Customer", customerEmail, customerPassword);

        String adminEmail = uniqueEmail("admin");
        AuthTokens adminTokens = createAndLoginUser("Admin User", adminEmail, "Admin123", Role.ADMIN, true);

        mockMvc.perform(patch("/api/users/%d/disable".formatted(customerTokens.userId()))
                        .header("Authorization", bearer(adminTokens.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(false));

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", bearer(customerTokens.accessToken())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Unauthorized"));

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s"}
                                """.formatted(customerTokens.refreshToken())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Refresh token is invalid or expired"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(customerEmail, customerPassword)))
                .andExpect(status().isUnauthorized());
    }
}
