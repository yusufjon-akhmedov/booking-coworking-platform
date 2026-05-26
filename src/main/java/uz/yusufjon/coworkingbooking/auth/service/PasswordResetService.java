package uz.yusufjon.coworkingbooking.auth.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import uz.yusufjon.coworkingbooking.auth.dto.ForgotPasswordRequest;
import uz.yusufjon.coworkingbooking.auth.dto.ResetPasswordRequest;
import uz.yusufjon.coworkingbooking.auth.entity.PasswordResetToken;
import uz.yusufjon.coworkingbooking.auth.repository.PasswordResetTokenRepository;
import uz.yusufjon.coworkingbooking.common.exception.BadRequestException;
import uz.yusufjon.coworkingbooking.user.entity.User;
import uz.yusufjon.coworkingbooking.user.repository.UserRepository;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;
    private final RefreshTokenService refreshTokenService;

    @Value("${app.password-reset.email-enabled:true}")
    private boolean passwordResetEmailEnabled;

    @Value("${spring.mail.username}")
    private String mailFrom;

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
            PasswordResetToken resetToken = new PasswordResetToken();
            resetToken.setUser(user);
            resetToken.setToken(generateUniqueToken());
            resetToken.setExpiresAt(LocalDateTime.now().plusMinutes(15));
            resetToken.setUsed(false);

            PasswordResetToken savedToken = passwordResetTokenRepository.save(resetToken);

            log.info("Password reset token created for email: {}", user.getEmail());
            log.info("Password reset email enabled: {}", passwordResetEmailEnabled);

            if (passwordResetEmailEnabled) {
                log.info("Sending password reset email to: {}", user.getEmail());
                sendResetEmail(user.getEmail(), savedToken.getToken());
                log.info("Password reset email sent successfully to: {}", user.getEmail());
            } else {
                log.warn("Password reset email sending is disabled. Email: {}", user.getEmail());
            }
        });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(request.getToken())
                .orElseThrow( () -> new BadRequestException("Invalid or expired password reset token"));

        if (Boolean.TRUE.equals(resetToken.getUsed())) {
            throw new BadRequestException("Invalid or expired password reset token");
        }

        if (!resetToken.getExpiresAt().isAfter(LocalDateTime.now())) {
            throw new BadRequestException("Invalid or expired password reset token");
        }

        User user  = resetToken.getUser();

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        refreshTokenService.revokeAllByUserId(user.getId());
    }

    private void sendResetEmail(String to, String token) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailFrom);
        message.setTo(to);
        message.setSubject("Coworking Booking - Password reset");
        message.setText("""
            Password reset token:

            %s

            This token expires in 15 minutes.
            """.formatted(token));

        mailSender.send(message);
    }

    private String generateUniqueToken() {
        String token;

        do {
            byte[] randomBytes = new byte[48];
            SECURE_RANDOM.nextBytes(randomBytes);
            token = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        } while (passwordResetTokenRepository.existsByToken(token));

        return token;
    }
}
