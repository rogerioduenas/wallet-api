package com.rogerio.wallet_api.e2e;

import com.rogerio.wallet_api.config.security.SecurityConfig;
import com.rogerio.wallet_api.config.security.TokenAuthenticationFilter;
import com.rogerio.wallet_api.domain.model.Role;
import com.rogerio.wallet_api.domain.model.User;
import com.rogerio.wallet_api.domain.repository.UserRepository;
import com.rogerio.wallet_api.dto.request.AdminRegistrationRequest;
import com.rogerio.wallet_api.service.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import({SecurityConfig.class, TokenAuthenticationFilter.class})
class AdminRegistrationE2ETest extends BaseE2ETest {

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private TokenService tokenService;

  private String adminToken;

  @BeforeEach
  void setUp() {
    userRepository.deleteAllInBatch();
    adminToken = tokenService.generateToken("admin@email.com", Role.ROLE_ADMIN.name());
  }

  @Test
  @DisplayName("Should successfully register a new admin when valid request and valid admin token are provided")
  void givenValidRequestAndToken_whenRegisterAdmin_thenCreateAdminSuccessfully() throws Exception {
    // Given
    AdminRegistrationRequest request = new AdminRegistrationRequest(
        "User Admin", "admin@email.com", "password"
    );

    // When & Then
    mockMvc.perform(post("/api/v1/admins/register")
            .header("Authorization", "Bearer " + adminToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.fullName").value("User Admin"))
        .andExpect(jsonPath("$.email").value("admin@email.com"))
        .andExpect(jsonPath("$.role").value("ROLE_ADMIN"))
        .andExpect(jsonPath("$.createdAt").exists());

    // Database Assertions
    List<User> users = userRepository.findAll();
    assertThat(users).hasSize(1);
    User savedAdmin = users.get(0);
    assertThat(savedAdmin.getFullName()).isEqualTo("User Admin");
    assertThat(savedAdmin.getEmail()).isEqualTo("admin@email.com");
    assertThat(savedAdmin.getRole()).isEqualTo(Role.ROLE_ADMIN);
    assertThat(savedAdmin.getPassword()).startsWith("$2a$");
  }

  @Test
  @DisplayName("Should return 401 Unauthorized when token is missing")
  void givenMissingToken_whenRegisterAdmin_thenReturnUnauthorized() throws Exception {
    // Given
    AdminRegistrationRequest request = new AdminRegistrationRequest(
        "User Admin", "admin@email.com", "password"
    );

    // When & Then
    mockMvc.perform(post("/api/v1/admins/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.title").value("Authentication Failed"));

    assertThat(userRepository.count()).isZero();
  }

  @Test
  @DisplayName("Should return 401 Unauthorized when token is invalid or expired")
  void givenInvalidToken_whenRegisterAdmin_thenReturnUnauthorized() throws Exception {
    // Given
    AdminRegistrationRequest request = new AdminRegistrationRequest(
        "User Admin", "admin@email.com", "password"
    );
    String invalidToken = "invalid_token";

    // When & Then
    mockMvc.perform(post("/api/v1/admins/register")
            .header("Authorization", "Bearer " + invalidToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.title").value("Authentication Failed"));

    assertThat(userRepository.count()).isZero();
  }

  @Test
  @DisplayName("Should return 403 Forbidden when user has ROLE_USER instead of ADMIN")
  void givenCustomerRoleToken_whenRegisterAdmin_thenReturnForbidden() throws Exception {
    // Given
    AdminRegistrationRequest request = new AdminRegistrationRequest(
        "User Admin", "admin@email.com", "password"
    );
    String customerToken = tokenService.generateToken("customer@email.com", Role.ROLE_USER.name());

    // When & Then
    mockMvc.perform(post("/api/v1/admins/register")
            .header("Authorization", "Bearer " + customerToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.title").value("Access Denied"));

    assertThat(userRepository.count()).isZero();
  }

  @Test
  @DisplayName("Should return 409 Conflict when attempting to register an admin with an existing email")
  void givenExistingEmail_whenRegisterDuplicateAdmin_thenReturnConflict() throws Exception {
    // Given
    String existingEmail = "admin@email.com";
    AdminRegistrationRequest initialRequest = new AdminRegistrationRequest(
        "User Admin", existingEmail, "password"
    );

    // When & Then
    mockMvc.perform(post("/api/v1/admins/register")
            .header("Authorization", "Bearer " + adminToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(initialRequest)))
        .andExpect(status().isCreated());

    AdminRegistrationRequest duplicateRequest = new AdminRegistrationRequest(
        "Another Name", existingEmail, "password"
    );

    mockMvc.perform(post("/api/v1/admins/register")
            .header("Authorization", "Bearer " + adminToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(duplicateRequest)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.title").value("Resource Conflict"))
        .andExpect(jsonPath("$.detail").value("Email already registered in the system"));

    assertThat(userRepository.count()).isEqualTo(1);
  }
}
