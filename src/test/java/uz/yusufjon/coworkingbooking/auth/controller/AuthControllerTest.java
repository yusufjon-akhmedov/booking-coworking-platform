package uz.yusufjon.coworkingbooking.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import uz.yusufjon.coworkingbooking.auth.dto.*;
import uz.yusufjon.coworkingbooking.auth.service.AuthService;
import uz.yusufjon.coworkingbooking.auth.service.PasswordResetService;
import uz.yusufjon.coworkingbooking.config.SecurityConfig;
import uz.yusufjon.coworkingbooking.security.JwtAccessDeniedHandler;
import uz.yusufjon.coworkingbooking.security.JwtAuthenticationEntryPoint;
import uz.yusufjon.coworkingbooking.security.jwt.JwtAuthenticationFilter;
import uz.yusufjon.coworkingbooking.security.jwt.JwtService;
import uz.yusufjon.coworkingbooking.security.service.CustomUserDetailsService;
import uz.yusufjon.coworkingbooking.user.entity.Role;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uz.yusufjon.coworkingbooking.support.SecurityTestUtils.authenticatedUser;

@WebMvcTest(controllers = AuthController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private PasswordResetService passwordResetService;


    @Test
    void forgotPasswordReturnsOkResponse() throws Exception {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("test@gmail.com");

        doNothing().when(passwordResetService).forgotPassword(any(ForgotPasswordRequest.class));

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message")
                        .value("If this email exists, password reset instructions have been sent"))
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(passwordResetService).forgotPassword(any(ForgotPasswordRequest.class));
    }

    @Test
    void forgotPasswordWhenEmailIsInvalidReturnsBadRequest() throws Exception {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("invalid-email");

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(passwordResetService);
    }

    @Test
    void resetPasswordReturnsOkResponse() throws Exception {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("valid-reset-token");
        request.setNewPassword("NewPass123!");

        doNothing().when(passwordResetService).resetPassword(any(ResetPasswordRequest.class));

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password has been reset successfully"))
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(passwordResetService).resetPassword(any(ResetPasswordRequest.class));
    }

    @Test
    void resetPasswordWhenRequestBodyIsInvalidReturnsBadRequest() throws Exception {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("");
        request.setNewPassword("123");

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(passwordResetService);
    }

    @Test
    void registerReturnsCreatedResponse() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Alice Customer");
        request.setEmail("alice@example.com");
        request.setPassword("secret123");

        when(authService.register(any(RegisterRequest.class))).thenReturn(authResponse());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("User registered successfully"))
                .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh-token"));
    }

    @Test
    void loginReturnsOkResponse() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("alice@example.com");
        request.setPassword("secret123");

        when(authService.login(any(LoginRequest.class))).thenReturn(authResponse());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Login successful"))
                .andExpect(jsonPath("$.data.userId").value(1));
    }

    @Test
    void refreshReturnsOkResponse() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("refresh-token");

        when(authService.refresh(any(RefreshTokenRequest.class))).thenReturn(authResponse());

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Token refreshed successfully"))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"));
    }

    @Test
    void refreshWhenRequestBodyIsInvalidReturnsBadRequest() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest();

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.data.refreshToken").value("Refresh token is required"));
    }

    @Test
    void logoutReturnsSuccessResponse() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("refresh-token");

        doNothing().when(authService).logout(any(Long.class), any(RefreshTokenRequest.class));

                mockMvc.perform(post("/api/auth/logout")
                        .with(authenticatedUser(1L, Role.CUSTOMER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logout successful"))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    private AuthResponse authResponse() {
        return AuthResponse.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .tokenType("Bearer")
                .userId(1L)
                .fullName("Alice Customer")
                .email("alice@example.com")
                .role(Role.CUSTOMER.name())
                .build();
    }
}
