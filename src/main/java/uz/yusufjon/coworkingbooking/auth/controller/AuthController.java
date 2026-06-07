package uz.yusufjon.coworkingbooking.auth.controller;

import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import uz.yusufjon.coworkingbooking.auth.dto.*;
import uz.yusufjon.coworkingbooking.auth.service.AuthService;
import uz.yusufjon.coworkingbooking.auth.service.PasswordResetService;
import uz.yusufjon.coworkingbooking.common.openapi.LoginApiResponses;
import uz.yusufjon.coworkingbooking.common.openapi.LogoutApiResponses;
import uz.yusufjon.coworkingbooking.common.openapi.RefreshTokenApiResponses;
import uz.yusufjon.coworkingbooking.common.openapi.RegisterApiResponses;
import uz.yusufjon.coworkingbooking.common.response.ApiResponse;
import uz.yusufjon.coworkingbooking.security.service.CustomUserDetails;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Registration, login, token refresh, and logout endpoints")
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request
    ) {
        passwordResetService.forgotPassword(request);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        "If this email exists, password reset instructions have been sent",
                        null
                )
        );
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request
    ) {
        passwordResetService.resetPassword(request);

        return ResponseEntity.ok(
                new ApiResponse<>("Password has been reset successfully", null)
        );
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new customer account", description = "Creates a new customer and returns access and refresh tokens.")
    @RegisterApiResponses
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>("User registered successfully", response));
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate a user", description = "Authenticates a user with email and password and returns access and refresh tokens.")
    @LoginApiResponses
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request
    ) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(new ApiResponse<>("Login successful", response));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh JWT tokens", description = "Rotates the refresh token and issues a new access token pair.")
    @RefreshTokenApiResponses
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @Valid @RequestBody RefreshTokenRequest request
    ) {
        AuthResponse response = authService.refresh(request);
        return ResponseEntity.ok(new ApiResponse<>("Token refreshed successfully", response));
    }

    @PostMapping("/logout")
    @Operation(
            summary = "Logout the current user",
            description = "Revokes the supplied refresh token for the authenticated user.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @LogoutApiResponses
    public ResponseEntity<ApiResponse<String>> logout(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @Valid @RequestBody RefreshTokenRequest request
    ) {
        authService.logout(currentUser.getUser().getId(), request);
        return ResponseEntity.ok(new ApiResponse<>("Logout successful", null));
    }
}
