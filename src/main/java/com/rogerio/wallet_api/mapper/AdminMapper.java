package com.rogerio.wallet_api.mapper;

import com.rogerio.wallet_api.domain.model.User;
import com.rogerio.wallet_api.dto.request.AdminRegistrationRequest;
import com.rogerio.wallet_api.dto.response.AdminRegistrationResponse;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class AdminMapper {

  public User toEntity(AdminRegistrationRequest request, String encodedPassword, Instant createdAt) {
    return new User(
        request.fullName(),
        request.email(),
        encodedPassword,
        createdAt
    );
  }

  public AdminRegistrationResponse toDTO(User user) {
    return new AdminRegistrationResponse(
        user.getId(),
        user.getFullName(),
        user.getEmail(),
        user.getRole(),
        user.getCreatedAt()
    );
  }
}
