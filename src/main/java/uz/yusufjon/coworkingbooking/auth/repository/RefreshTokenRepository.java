package uz.yusufjon.coworkingbooking.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uz.yusufjon.coworkingbooking.auth.entity.RefreshToken;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    boolean existsByToken(String token);

    @Modifying
    @Query("""
            update RefreshToken rt
            set rt.revoked = true,
                rt.updatedAt = :updatedAt
            where rt.user.id = :userId
              and rt.revoked = false
            """)
    int revokeAllByUserId(@Param("userId") Long userId, @Param("updatedAt") LocalDateTime updatedAt);
}
