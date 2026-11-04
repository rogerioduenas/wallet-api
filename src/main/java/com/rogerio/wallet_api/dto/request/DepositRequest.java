package com.rogerio.wallet_api.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record DepositRequest(
    @NotNull(message = "Amount is mandatory")
    @Positive(message = "Amount must be greater than 0")
    BigDecimal amount
) {}
