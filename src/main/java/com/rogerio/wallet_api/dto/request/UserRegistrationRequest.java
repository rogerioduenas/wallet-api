package com.rogerio.wallet_api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserRegistrationRequest(
    @NotBlank(message = "The name is mandatory")
    String fullName,

    @NotBlank(message = "The email is mandatory")
    @Email(message = "Invalid email format")
    String email,

    @NotBlank(message = "The password is mandatory")
    @Size(min = 8, message = "The password must be at least 8 characters long")
    String password) {
}
