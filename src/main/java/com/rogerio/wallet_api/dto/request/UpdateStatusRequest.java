package com.rogerio.wallet_api.dto.request;

import com.rogerio.wallet_api.domain.model.enums.Status;
import jakarta.validation.constraints.NotNull;

public record UpdateStatusRequest(
    @NotNull(message = "The status is mandatory")
    Status status
) {}