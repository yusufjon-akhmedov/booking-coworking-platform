package uz.yusufjon.coworkingbooking.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.yusufjon.coworkingbooking.auth.service.RefreshTokenService;
import uz.yusufjon.coworkingbooking.common.exception.BadRequestException;
import uz.yusufjon.coworkingbooking.common.exception.ResourceNotFoundException;
import uz.yusufjon.coworkingbooking.common.response.PageResponse;
import uz.yusufjon.coworkingbooking.user.dto.UserResponse;
import uz.yusufjon.coworkingbooking.user.entity.Role;
import uz.yusufjon.coworkingbooking.user.entity.User;
import uz.yusufjon.coworkingbooking.user.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(Long authenticatedUserId) {
        User user = userRepository.findById(authenticatedUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + authenticatedUserId));

        return mapToResponse(user);
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> getCustomers(Pageable pageable) {
        return PageResponse.from(
                userRepository.findAllByRole(Role.CUSTOMER, pageable).map(this::mapToResponse)
        );
    }

    @Transactional
    public UserResponse disableCustomer(Long userId) {
        User user = getCustomer(userId);
        user.setEnabled(false);
        refreshTokenService.revokeAllByUserId(user.getId());

        User savedUser = userRepository.save(user);
        return mapToResponse(savedUser);
    }

    @Transactional
    public UserResponse enableCustomer(Long userId) {
        User user = getCustomer(userId);
        user.setEnabled(true);

        User savedUser = userRepository.save(user);
        return mapToResponse(savedUser);
    }

    private User getCustomer(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        if (user.getRole() != Role.CUSTOMER) {
            throw new BadRequestException("Only customer accounts can be managed through this endpoint");
        }

        return user;
    }

    private UserResponse mapToResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .enabled(user.getEnabled())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
