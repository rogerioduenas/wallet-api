package com.rogerio.wallet_api.service;

import com.rogerio.wallet_api.domain.model.Role;
import com.rogerio.wallet_api.domain.model.User;
import com.rogerio.wallet_api.domain.repository.UserRepository;
import com.rogerio.wallet_api.dto.request.AdminRegistrationRequest;
import com.rogerio.wallet_api.dto.response.AdminRegistrationResponse;
import com.rogerio.wallet_api.dto.response.UserResponse;
import com.rogerio.wallet_api.exception.EmailAlreadyExistsException;
import com.rogerio.wallet_api.mapper.AdminMapper;
import com.rogerio.wallet_api.mapper.UserMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Clock;

@Service
public class AdminService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final AdminMapper adminMapper;
  private final UserMapper userMapper;
  private final Clock clock;


  public AdminService(
      UserRepository userRepository,
      PasswordEncoder passwordEncoder,
      AdminMapper adminMapper, UserMapper userMapper,
      Clock clock) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.adminMapper = adminMapper;
    this.userMapper = userMapper;
    this.clock = clock;
  }

  public AdminRegistrationResponse register(AdminRegistrationRequest request) {
    if (userRepository.existsByEmail(request.email())) {
      throw new EmailAlreadyExistsException("Email already registered in the system");
    }

    String encodedPassword = passwordEncoder.encode(request.password());
    User user = adminMapper.toEntity(request, encodedPassword, clock.instant());
    user.setRole(Role.ROLE_ADMIN);

    User savedUser = userRepository.save(user);

    return adminMapper.toDTO(savedUser);
  }

  public Page<UserResponse> listUsers(Pageable pageable) {
    Page<User> users = userRepository.findAll(pageable);
    return users.map(userMapper::toDto);
  }
}
