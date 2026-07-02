package com.techora.auth.dto.response;

public record AuthResponse(String accessToken) {
    public static AuthResponse from(String accessToken) {
        return new AuthResponse(accessToken);
    }
}
