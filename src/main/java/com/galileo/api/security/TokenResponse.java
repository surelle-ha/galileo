package com.galileo.api.security;

public record TokenResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds
) {
}
