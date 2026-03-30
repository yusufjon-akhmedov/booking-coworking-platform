package uz.yusufjon.coworkingbooking.support;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import uz.yusufjon.coworkingbooking.security.service.CustomUserDetails;
import uz.yusufjon.coworkingbooking.user.entity.Role;
import uz.yusufjon.coworkingbooking.user.entity.User;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

public final class SecurityTestUtils {

    private SecurityTestUtils() {
    }

    public static RequestPostProcessor authenticatedUser(Long id, Role role) {
        User user = new User();
        user.setId(id);
        user.setFullName(role.name() + " User");
        user.setEmail(role.name().toLowerCase() + id + "@example.com");
        user.setPassword("encoded-password");
        user.setRole(role);
        user.setEnabled(true);

        CustomUserDetails userDetails = new CustomUserDetails(user);
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );

        return authentication(authenticationToken);
    }
}
