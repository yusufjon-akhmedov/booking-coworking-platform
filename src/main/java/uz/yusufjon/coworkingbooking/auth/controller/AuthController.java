package uz.yusufjon.coworkingbooking.auth.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uz.yusufjon.coworkingbooking.auth.dto.AuthResponse;
import uz.yusufjon.coworkingbooking.auth.dto.LoginRequest;
import uz.yusufjon.coworkingbooking.auth.dto.RegisterRequest;
import uz.yusufjon.coworkingbooking.auth.service.AuthService;
import uz.yusufjon.coworkingbooking.common.response.ApiResponse;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>("User registered successfully", response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request
    ) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(new ApiResponse<>("Login successful", response));
    }
}