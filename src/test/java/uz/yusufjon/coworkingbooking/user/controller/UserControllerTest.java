package uz.yusufjon.coworkingbooking.user.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import uz.yusufjon.coworkingbooking.common.response.PageResponse;
import uz.yusufjon.coworkingbooking.config.SecurityConfig;
import uz.yusufjon.coworkingbooking.security.JwtAccessDeniedHandler;
import uz.yusufjon.coworkingbooking.security.JwtAuthenticationEntryPoint;
import uz.yusufjon.coworkingbooking.security.jwt.JwtAuthenticationFilter;
import uz.yusufjon.coworkingbooking.security.jwt.JwtService;
import uz.yusufjon.coworkingbooking.security.service.CustomUserDetailsService;
import uz.yusufjon.coworkingbooking.user.dto.UserResponse;
import uz.yusufjon.coworkingbooking.user.entity.Role;
import uz.yusufjon.coworkingbooking.user.service.UserService;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uz.yusufjon.coworkingbooking.support.SecurityTestUtils.authenticatedUser;

@WebMvcTest(controllers = UserController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class})
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void authenticatedUserCanGetOwnProfile() throws Exception {
        when(userService.getCurrentUser(1L)).thenReturn(userResponse(1L, true));

        mockMvc.perform(get("/api/users/me")
                        .with(authenticatedUser(1L, Role.CUSTOMER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User profile fetched successfully"))
                .andExpect(jsonPath("$.data.email").value("user1@example.com"));
    }

    @Test
    void adminCanGetCustomerList() throws Exception {
        when(userService.getCustomers(any()))
                .thenReturn(new PageResponse<>(List.of(userResponse(2L, true)), 0, 20, 1, 1, true, true));

        mockMvc.perform(get("/api/users/customers")
                        .with(authenticatedUser(99L, Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Customers fetched successfully"))
                .andExpect(jsonPath("$.data.content[0].id").value(2));
    }

    @Test
    void nonAdminGetsForbiddenForCustomerList() throws Exception {
        mockMvc.perform(get("/api/users/customers")
                        .with(authenticatedUser(1L, Role.CUSTOMER)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Forbidden"));
    }

    @Test
    void adminCanDisableUser() throws Exception {
        when(userService.disableCustomer(anyLong())).thenReturn(userResponse(2L, false));

        mockMvc.perform(patch("/api/users/2/disable")
                        .with(authenticatedUser(99L, Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User disabled successfully"))
                .andExpect(jsonPath("$.data.enabled").value(false));
    }

    @Test
    void adminCanEnableUser() throws Exception {
        when(userService.enableCustomer(anyLong())).thenReturn(userResponse(2L, true));

        mockMvc.perform(patch("/api/users/2/enable")
                        .with(authenticatedUser(99L, Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User enabled successfully"))
                .andExpect(jsonPath("$.data.enabled").value(true));
    }

    private UserResponse userResponse(Long id, boolean enabled) {
        return UserResponse.builder()
                .id(id)
                .fullName("User " + id)
                .email("user" + id + "@example.com")
                .role(Role.CUSTOMER.name())
                .enabled(enabled)
                .createdAt(LocalDateTime.of(2026, 3, 30, 9, 0))
                .build();
    }
}
