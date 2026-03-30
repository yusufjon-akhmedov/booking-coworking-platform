package uz.yusufjon.coworkingbooking.auth.controller;

import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import uz.yusufjon.coworkingbooking.auth.dto.AuthResponse;
import uz.yusufjon.coworkingbooking.auth.dto.LoginRequest;
import uz.yusufjon.coworkingbooking.auth.dto.RefreshTokenRequest;
import uz.yusufjon.coworkingbooking.auth.dto.RegisterRequest;
import uz.yusufjon.coworkingbooking.auth.service.AuthService;
import uz.yusufjon.coworkingbooking.common.response.ApiResponse;
import uz.yusufjon.coworkingbooking.security.service.CustomUserDetails;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Registration, login, token refresh, and logout endpoints")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Register a new customer account", description = "Creates a new customer and returns access and refresh tokens.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "User registered successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed or email already exists", content = @Content)
    })
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>("User registered successfully", response));
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate a user", description = "Authenticates a user with email and password and returns access and refresh tokens.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Login successful"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid credentials or disabled user", content = @Content)
    })
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request
    ) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(new ApiResponse<>("Login successful", response));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh JWT tokens", description = "Rotates the refresh token and issues a new access token pair.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Token refreshed successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed or refresh token is invalid", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Refresh token expired or user disabled", content = @Content)
    })
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
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Logout successful"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Refresh token does not belong to the current user", content = @Content)
    })
    public ResponseEntity<ApiResponse<String>> logout(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @Valid @RequestBody RefreshTokenRequest request
    ) {
        authService.logout(currentUser.getUser().getId(), request);
        return ResponseEntity.ok(new ApiResponse<>("Logout successful", null));
    }
}
