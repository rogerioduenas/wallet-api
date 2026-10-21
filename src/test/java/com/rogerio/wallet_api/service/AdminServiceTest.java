package com.rogerio.wallet_api.service;

import com.rogerio.wallet_api.domain.model.Role;
import com.rogerio.wallet_api.domain.model.User;
import com.rogerio.wallet_api.domain.repository.UserRepository;
import com.rogerio.wallet_api.dto.request.AdminRegistrationRequest;
import com.rogerio.wallet_api.dto.response.AdminRegistrationResponse;
import com.rogerio.wallet_api.exception.EmailAlreadyExistsException;
import com.rogerio.wallet_api.mapper.AdminMapper;
import com.rogerio.wallet_api.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
public class AdminServiceTest {

  @Mock
  UserRepository userRepository;

  @Mock
  PasswordEncoder passwordEncoder;

  @Mock
  UserMapper userMapper;

  @Mock
  AdminMapper adminMapper;

  private final Instant fixedInstant = Instant.parse("2026-10-15T10:00:00Z");
  private final Clock clock = Clock.fixed(fixedInstant, ZoneId.of("UTC"));

  private AdminService adminService;

  @BeforeEach
  void setUp() {
    adminService = new AdminService(userRepository, passwordEncoder, adminMapper, userMapper, clock);
  }

  @Test
  @DisplayName("Should successfully register a new admin user")
  void givenValidRequest_whenRegisterAdmin_thenCreateAdminSuccessfully() {
    // Given
    var request = new AdminRegistrationRequest(
        "Admin Boss",
        "admin@email.com",
        "password"
    );
    UUID adminId = UUID.randomUUID();

    User savedAdmin = new User("Admin Boss", "admin@email.com", "encodedPassword", fixedInstant);
    savedAdmin.setRole(Role.ROLE_ADMIN);
    ReflectionTestUtils.setField(savedAdmin, "id", adminId);

    AdminRegistrationResponse expectedResponse = new AdminRegistrationResponse(
        adminId, "Admin Boss", "admin@email.com", Role.ROLE_ADMIN, fixedInstant
    );

    given(userRepository.existsByEmail(request.email())).willReturn(false);
    given(passwordEncoder.encode(request.password())).willReturn("encodedPassword");
    given(adminMapper.toEntity(any(), any(), any())).willReturn(savedAdmin);
    given(userRepository.save(any(User.class))).willReturn(savedAdmin);
    given(adminMapper.toDTO(savedAdmin)).willReturn(expectedResponse);

    // When
    AdminRegistrationResponse response = adminService.register(request);

    // Then
    assertThat(response).isNotNull();
    assertThat(response.email()).isEqualTo("admin@email.com");
    assertThat(response.role()).isEqualTo(Role.ROLE_ADMIN);
  }

  @Test
  @DisplayName("Should throw EmailAlreadyExistsException when email is already taken")
  void givenExistingEmail_whenRegisterAdmin_thenThrowException() {
    // Given
    var request = new AdminRegistrationRequest("Admin Boss", "admin@email.com", "password123");
    given(userRepository.existsByEmail(request.email())).willReturn(true);

    // When & Then
    assertThatThrownBy(() -> adminService.register(request))
        .isInstanceOf(EmailAlreadyExistsException.class)
        .hasMessage("Email already registered in the system");
  }
}
