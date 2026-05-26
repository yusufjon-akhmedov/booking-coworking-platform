package uz.yusufjon.coworkingbooking.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.yusufjon.coworkingbooking.auth.entity.PasswordResetToken;

import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByToken(String token);

    boolean existsByToken(String token);
}
