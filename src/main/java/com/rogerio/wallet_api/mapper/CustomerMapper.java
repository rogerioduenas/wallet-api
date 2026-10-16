package com.rogerio.wallet_api.mapper;

import com.rogerio.wallet_api.domain.model.User;
import com.rogerio.wallet_api.domain.model.Wallet;
import com.rogerio.wallet_api.dto.request.CustomerRegistrationRequest;
import com.rogerio.wallet_api.dto.response.CustomerRegistrationResponse;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class CustomerMapper {

  public User toEntity(CustomerRegistrationRequest request, String encodedPassword, Instant createdAt) {
    return new User(
        request.fullName(),
        request.email(),
        encodedPassword,
        createdAt
    );
  }

  public CustomerRegistrationResponse toDTO(User user, Wallet wallet) {
    return new CustomerRegistrationResponse(
        user.getId(),
        user.getFullName(),
        user.getEmail(),
        wallet.getId(),
        user.getCreatedAt()
    );
  }
}
