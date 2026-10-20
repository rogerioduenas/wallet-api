package com.rogerio.wallet_api.dto.response;

import com.rogerio.wallet_api.domain.model.Role;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
    UUID id,
    String fullName,
    String email,
    Role role,
    Instant createdAt
) {
}
