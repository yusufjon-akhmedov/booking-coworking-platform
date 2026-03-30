package uz.yusufjon.coworkingbooking.auth.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthResponse {

    private String accessToken;
    private String tokenType;
    private Long userId;
    private String fullName;
    private String email;
    private String role;
}