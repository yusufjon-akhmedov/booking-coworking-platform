package uz.yusufjon.coworkingbooking.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.yusufjon.coworkingbooking.auth.dto.AuthResponse;
import uz.yusufjon.coworkingbooking.auth.dto.LoginRequest;
import uz.yusufjon.coworkingbooking.auth.dto.RegisterRequest;
import uz.yusufjon.coworkingbooking.common.exception.BadRequestException;
import uz.yusufjon.coworkingbooking.security.jwt.JwtService;
import uz.yusufjon.coworkingbooking.security.service.CustomUserDetails;
import uz.yusufjon.coworkingbooking.user.entity.Role;
import uz.yusufjon.coworkingbooking.user.entity.User;
import uz.yusufjon.coworkingbooking.user.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email is already in use");
        }

        User user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.CUSTOMER);
        user.setEnabled(true);

        User savedUser = userRepository.save(user);
        CustomUserDetails userDetails = new CustomUserDetails(savedUser);
        String token = jwtService.generateToken(userDetails);

        return buildAuthResponse(savedUser, token);
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("Invalid email or password"));

        String token = jwtService.generateToken(new CustomUserDetails(user));
        return buildAuthResponse(user, token);
    }

    private AuthResponse buildAuthResponse(User user, String token) {
        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .userId(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }
}