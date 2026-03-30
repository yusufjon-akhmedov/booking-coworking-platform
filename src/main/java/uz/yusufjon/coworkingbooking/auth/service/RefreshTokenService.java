package uz.yusufjon.coworkingbooking.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.yusufjon.coworkingbooking.auth.entity.RefreshToken;
import uz.yusufjon.coworkingbooking.auth.repository.RefreshTokenRepository;
import uz.yusufjon.coworkingbooking.user.entity.User;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${app.jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    @Transactional
    public String createRefreshToken(User user) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(generateUniqueTokenValue());
        refreshToken.setExpiresAt(LocalDateTime.now().plusNanos(refreshTokenExpiration * 1_000_000L));
        refreshToken.setRevoked(false);

        return refreshTokenRepository.save(refreshToken).getToken();
    }

    @Transactional(readOnly = true)
    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    @Transactional
    public void revoke(RefreshToken refreshToken) {
        if (Boolean.TRUE.equals(refreshToken.getRevoked())) {
            return;
        }

        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);
    }

    @Transactional
    public void revokeAllByUserId(Long userId) {
        refreshTokenRepository.revokeAllByUserId(userId, LocalDateTime.now());
    }

    public boolean isExpired(RefreshToken refreshToken) {
        return !refreshToken.getExpiresAt().isAfter(LocalDateTime.now());
    }

    private String generateUniqueTokenValue() {
        String token;

        do {
            byte[] randomBytes = new byte[48];
            SECURE_RANDOM.nextBytes(randomBytes);
            token = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        } while (refreshTokenRepository.existsByToken(token));

        return token;
    }
}
