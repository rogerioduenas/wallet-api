package com.rogerio.wallet_api.infra.config;

import com.rogerio.wallet_api.domain.model.Role;
import com.rogerio.wallet_api.domain.model.User;
import com.rogerio.wallet_api.domain.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = {
    "app.admin.email=admin@email.com",
    "app.admin.password=password"
})
@Transactional
class MasterAdminSeederIntegrationTest {

  @Autowired
  private UserRepository userRepository;

  @Test
  @DisplayName("Should create master admin successfully on application startup when admin does not exist")
  void shouldCreateMasterAdminOnStartupWhenAdminDoesNotExist() {
    // Given
    String targetEmail = "admin@email.com";

    // When
    Optional<User> savedAdminOpt = userRepository.findByEmail(targetEmail);

    // Then
    assertThat(savedAdminOpt).isPresent();
    User admin = savedAdminOpt.get();
    assertThat(admin.getFullName()).isEqualTo("Master Admin");
    assertThat(admin.getEmail()).isEqualTo(targetEmail);
    assertThat(admin.getRole()).isEqualTo(Role.ROLE_ADMIN);
    assertThat(admin.getPassword()).isNotNull();
    assertThat(admin.getPassword()).isNotEqualTo("password");
  }

  @Test
  @DisplayName("Should maintain idempotency and prevent duplicate master admin creation")
  void shouldMaintainIdempotencyAndPreventDuplicateMasterAdminCreation() {
    // Given
    String targetEmail = "admin@email.com";

    // When
    long adminCount = userRepository.findAll().stream()
        .filter(user -> user.getEmail().equals(targetEmail))
        .count();

    // Then
    assertThat(adminCount).isEqualTo(1);
  }
}