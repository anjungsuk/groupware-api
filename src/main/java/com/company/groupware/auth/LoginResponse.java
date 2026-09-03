package com.company.groupware.auth;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        AuthUserResponse user
) {
}
