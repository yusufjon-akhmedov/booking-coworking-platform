package uz.yusufjon.coworkingbooking.auth.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import uz.yusufjon.coworkingbooking.auth.dto.AuthResponse;
import uz.yusufjon.coworkingbooking.auth.dto.LoginRequest;
import uz.yusufjon.coworkingbooking.auth.dto.RefreshTokenRequest;
import uz.yusufjon.coworkingbooking.auth.dto.RegisterRequest;
import uz.yusufjon.coworkingbooking.auth.entity.RefreshToken;
import uz.yusufjon.coworkingbooking.common.exception.BadRequestException;
import uz.yusufjon.coworkingbooking.security.jwt.JwtService;
import uz.yusufjon.coworkingbooking.security.service.CustomUserDetails;
import uz.yusufjon.coworkingbooking.user.entity.Role;
import uz.yusufjon.coworkingbooking.user.entity.User;
import uz.yusufjon.coworkingbooking.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private AuthService authService;

    @Test
    void registerCreatesCustomerAndReturnsTokens() {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Alice Customer");
        request.setEmail("alice@example.com");
        request.setPassword("secret123");

        User savedUser = customer(1L, "Alice Customer", "alice@example.com", true);

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtService.generateToken(any(CustomUserDetails.class))).thenReturn("access-token");
        when(refreshTokenService.createRefreshToken(savedUser)).thenReturn("refresh-token");

        AuthResponse response = authService.register(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User persistedUser = userCaptor.getValue();
        assertThat(persistedUser.getFullName()).isEqualTo("Alice Customer");
        assertThat(persistedUser.getEmail()).isEqualTo("alice@example.com");
        assertThat(persistedUser.getPassword()).isEqualTo("encoded-password");
        assertThat(persistedUser.getRole()).isEqualTo(Role.CUSTOMER);
        assertThat(persistedUser.getEnabled()).isTrue();

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(response.getRole()).isEqualTo(Role.CUSTOMER.name());
    }

    @Test
    void registerWhenEmailAlreadyExistsThrowsBadRequestException() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("duplicate@example.com");

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Email is already in use");
    }

    @Test
    void loginAuthenticatesUserAndReturnsTokens() {
        LoginRequest request = new LoginRequest();
        request.setEmail("alice@example.com");
        request.setPassword("secret123");

        User user = customer(1L, "Alice Customer", "alice@example.com", true);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(mock(Authentication.class));
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(user));
        when(jwtService.generateToken(any(CustomUserDetails.class))).thenReturn("access-token");
        when(refreshTokenService.createRefreshToken(user)).thenReturn("refresh-token");

        AuthResponse response = authService.login(request);

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(response.getUserId()).isEqualTo(1L);
    }

    @Test
    void loginWhenAuthenticationManagerRejectsDisabledUserPropagatesException() {
        LoginRequest request = new LoginRequest();
        request.setEmail("disabled@example.com");
        request.setPassword("secret123");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new DisabledException("User is disabled"));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(DisabledException.class)
                .hasMessage("User is disabled");
    }

    @Test
    void refreshRevokesExistingTokenAndReturnsNewTokens() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("existing-refresh-token");

        User user = customer(1L, "Alice Customer", "alice@example.com", true);
        RefreshToken refreshToken = refreshToken(user, "existing-refresh-token", false, LocalDateTime.now().plusDays(1));

        when(refreshTokenService.findByToken(request.getRefreshToken())).thenReturn(Optional.of(refreshToken));
        when(refreshTokenService.isExpired(refreshToken)).thenReturn(false);
        when(jwtService.generateToken(any(CustomUserDetails.class))).thenReturn("new-access-token");
        when(refreshTokenService.createRefreshToken(user)).thenReturn("new-refresh-token");

        AuthResponse response = authService.refresh(request);

        verify(refreshTokenService).revoke(refreshToken);
        assertThat(response.getAccessToken()).isEqualTo("new-access-token");
        assertThat(response.getRefreshToken()).isEqualTo("new-refresh-token");
    }

    @Test
    void refreshWhenUserIsDisabledThrowsBadRequestException() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("existing-refresh-token");

        User user = customer(1L, "Disabled User", "disabled@example.com", false);
        RefreshToken refreshToken = refreshToken(user, "existing-refresh-token", false, LocalDateTime.now().plusDays(1));

        when(refreshTokenService.findByToken(request.getRefreshToken())).thenReturn(Optional.of(refreshToken));
        when(refreshTokenService.isExpired(refreshToken)).thenReturn(false);

        assertThatThrownBy(() -> authService.refresh(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("User account is disabled");

        verify(refreshTokenService, never()).revoke(refreshToken);
    }

    @Test
    void logoutRevokesRefreshTokenOwnedByAuthenticatedUser() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("refresh-token");

        User user = customer(5L, "Alice Customer", "alice@example.com", true);
        RefreshToken refreshToken = refreshToken(user, "refresh-token", false, LocalDateTime.now().plusDays(1));

        when(refreshTokenService.findByToken(request.getRefreshToken())).thenReturn(Optional.of(refreshToken));

        authService.logout(5L, request);

        verify(refreshTokenService).revoke(refreshToken);
    }

    private User customer(Long id, String fullName, String email, boolean enabled) {
        User user = new User();
        user.setId(id);
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPassword("encoded-password");
        user.setRole(Role.CUSTOMER);
        user.setEnabled(enabled);
        return user;
    }

    private RefreshToken refreshToken(User user, String tokenValue, boolean revoked, LocalDateTime expiresAt) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(tokenValue);
        refreshToken.setRevoked(revoked);
        refreshToken.setExpiresAt(expiresAt);
        return refreshToken;
    }
}
