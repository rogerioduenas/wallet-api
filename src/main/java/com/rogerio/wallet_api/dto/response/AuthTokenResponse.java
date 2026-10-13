package com.rogerio.wallet_api.dto.response;

public record AuthTokenResponse(
    String token,
    String type,
    String role,
    long expiresIn
) {
}
