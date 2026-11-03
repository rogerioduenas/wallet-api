package com.rogerio.wallet_api.dto.response;

import java.time.Instant;
import java.util.UUID;

public record TransactionResponse(
    UUID id,
    Instant createdAt
) {
}
