package uz.yusufjon.coworkingbooking.user.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import uz.yusufjon.coworkingbooking.common.response.ApiResponse;
import uz.yusufjon.coworkingbooking.common.response.PageResponse;
import uz.yusufjon.coworkingbooking.security.service.CustomUserDetails;
import uz.yusufjon.coworkingbooking.user.dto.UserResponse;
import uz.yusufjon.coworkingbooking.user.service.UserService;
import org.springframework.data.domain.Pageable;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        UserResponse response = userService.getCurrentUser(currentUser.getUser().getId());
        return ResponseEntity.ok(new ApiResponse<>("User profile fetched successfully", response));
    }

    @GetMapping("/customers")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> getCustomers(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PageResponse<UserResponse> response = userService.getCustomers(pageable);
        return ResponseEntity.ok(new ApiResponse<>("Customers fetched successfully", response));
    }

    @PatchMapping("/{id}/disable")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> disableCustomer(@PathVariable Long id) {
        UserResponse response = userService.disableCustomer(id);
        return ResponseEntity.ok(new ApiResponse<>("User disabled successfully", response));
    }

    @PatchMapping("/{id}/enable")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> enableCustomer(@PathVariable Long id) {
        UserResponse response = userService.enableCustomer(id);
        return ResponseEntity.ok(new ApiResponse<>("User enabled successfully", response));
    }
}
