package com.rogerio.wallet_api.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record BalanceResponse(UUID customerId, BigDecimal balance) {
}
