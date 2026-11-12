package com.rogerio.wallet_api.dto.response;

import com.rogerio.wallet_api.domain.model.enums.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionStatementResponse(
    UUID id,
    BigDecimal amount,
    TransactionType type,
    UUID recipientWalletId,
    Instant createdAt
) {
}
