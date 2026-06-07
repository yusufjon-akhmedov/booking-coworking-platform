package uz.yusufjon.coworkingbooking.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.*;
import uz.yusufjon.coworkingbooking.common.openapi.AdminOnlyApiResponses;
import uz.yusufjon.coworkingbooking.common.openapi.CommonApiResponses;
import uz.yusufjon.coworkingbooking.common.response.ApiResponse;
import uz.yusufjon.coworkingbooking.common.response.PageResponse;
import uz.yusufjon.coworkingbooking.security.service.CustomUserDetails;
import uz.yusufjon.coworkingbooking.user.dto.UserResponse;
import uz.yusufjon.coworkingbooking.user.service.UserService;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Current profile and admin-only user management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get current user profile", description = "Returns the profile of the authenticated user.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User profile fetched successfully")
    @CommonApiResponses
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        UserResponse response = userService.getCurrentUser(currentUser.getUser().getId());
        return ResponseEntity.ok(new ApiResponse<>("User profile fetched successfully", response));
    }

    @GetMapping("/customers")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List customers", description = "Returns a paginated list of customer accounts. Admin only.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Customers fetched successfully")
    @AdminOnlyApiResponses
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> getCustomers(
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PageResponse<UserResponse> response = userService.getCustomers(pageable);
        return ResponseEntity.ok(new ApiResponse<>("Customers fetched successfully", response));
    }

    @PatchMapping("/{id}/disable")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Disable a customer", description = "Disables a customer account and revokes their refresh tokens. Admin only.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User disabled successfully")
    @AdminOnlyApiResponses
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found", content = @Content)
    public ResponseEntity<ApiResponse<UserResponse>> disableCustomer(@PathVariable Long id) {
        UserResponse response = userService.disableCustomer(id);
        return ResponseEntity.ok(new ApiResponse<>("User disabled successfully", response));
    }

    @PatchMapping("/{id}/enable")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Enable a customer", description = "Re-enables a previously disabled customer account. Admin only.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User enabled successfully")
    @AdminOnlyApiResponses
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found", content = @Content)
    public ResponseEntity<ApiResponse<UserResponse>> enableCustomer(@PathVariable Long id) {
        UserResponse response = userService.enableCustomer(id);
        return ResponseEntity.ok(new ApiResponse<>("User enabled successfully", response));
    }
}
