package com.rogerio.wallet_api.mapper;

import com.rogerio.wallet_api.domain.model.User;
import com.rogerio.wallet_api.dto.response.UserResponse;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

  public UserResponse toDto(User user) {
    return new UserResponse(
        user.getId(),
        user.getFullName(),
        user.getEmail(),
        user.getRole(),
        user.getCreatedAt(),
        user.getStatus()
    );
  }
}
