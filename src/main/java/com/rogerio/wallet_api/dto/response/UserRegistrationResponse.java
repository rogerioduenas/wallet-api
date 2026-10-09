package com.rogerio.wallet_api.dto.response;

import java.time.Instant;
import java.util.UUID;

public record UserRegistrationResponse(
    UUID id,
    String fullName,
    String email,
    UUID walletId,
    Instant createdAt
) {
}
