package uz.yusufjon.coworkingbooking.user.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import uz.yusufjon.coworkingbooking.auth.service.RefreshTokenService;
import uz.yusufjon.coworkingbooking.common.exception.BadRequestException;
import uz.yusufjon.coworkingbooking.common.response.PageResponse;
import uz.yusufjon.coworkingbooking.user.dto.UserResponse;
import uz.yusufjon.coworkingbooking.user.entity.Role;
import uz.yusufjon.coworkingbooking.user.entity.User;
import uz.yusufjon.coworkingbooking.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private UserService userService;

    @Test
    void getCurrentUserReturnsMappedProfile() {
        User user = customer(1L, true);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserResponse response = userService.getCurrentUser(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getEmail()).isEqualTo("customer1@example.com");
        assertThat(response.getRole()).isEqualTo(Role.CUSTOMER.name());
    }

    @Test
    void getCustomersReturnsPagedCustomerResponses() {
        User first = customer(1L, true);
        User second = customer(2L, false);

        when(userRepository.findAllByRole(Role.CUSTOMER, PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of(first, second), PageRequest.of(0, 20), 2));

        PageResponse<UserResponse> response = userService.getCustomers(PageRequest.of(0, 20));

        assertThat(response.getContent()).hasSize(2);
        assertThat(response.getContent()).extracting(UserResponse::getEnabled).containsExactly(true, false);
    }

    @Test
    void disableCustomerRevokesRefreshTokensAndReturnsDisabledUser() {
        User user = customer(1L, true);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        UserResponse response = userService.disableCustomer(1L);

        verify(refreshTokenService).revokeAllByUserId(1L);
        assertThat(response.getEnabled()).isFalse();
    }

    @Test
    void enableCustomerReturnsEnabledUser() {
        User user = customer(1L, false);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        UserResponse response = userService.enableCustomer(1L);

        assertThat(response.getEnabled()).isTrue();
    }

    @Test
    void disableCustomerWhenTargetIsAdminThrowsBadRequestException() {
        User admin = admin(99L);

        when(userRepository.findById(99L)).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> userService.disableCustomer(99L))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Only customer accounts can be managed through this endpoint");
    }

    private User customer(Long id, boolean enabled) {
        User user = new User();
        user.setId(id);
        user.setFullName("Customer " + id);
        user.setEmail("customer" + id + "@example.com");
        user.setPassword("encoded-password");
        user.setRole(Role.CUSTOMER);
        user.setEnabled(enabled);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return user;
    }

    private User admin(Long id) {
        User user = new User();
        user.setId(id);
        user.setFullName("Admin");
        user.setEmail("admin@example.com");
        user.setPassword("encoded-password");
        user.setRole(Role.ADMIN);
        user.setEnabled(true);
        return user;
    }
}
