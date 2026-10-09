package com.rogerio.wallet_api.mapper;

import com.rogerio.wallet_api.domain.model.User;
import com.rogerio.wallet_api.domain.model.Wallet;
import com.rogerio.wallet_api.dto.request.UserRegistrationRequest;
import com.rogerio.wallet_api.dto.response.UserRegistrationResponse;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class UserMapper {

  public User toEntity(UserRegistrationRequest request, String encodedPassword, Instant createdAt) {
    return new User(
        request.fullName(),
        request.email(),
        encodedPassword,
        createdAt
    );
  }

  public UserRegistrationResponse toDTO(User user, Wallet wallet) {
    return new UserRegistrationResponse(
        user.getId(),
        user.getFullName(),
        user.getEmail(),
        wallet.getId(),
        user.getCreatedAt()
    );
  }
}
