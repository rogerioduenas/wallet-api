package com.rogerio.wallet_api.e2e;

import com.rogerio.wallet_api.config.security.SecurityConfig;
import com.rogerio.wallet_api.config.security.TokenAuthenticationFilter;
import com.rogerio.wallet_api.domain.model.Role;
import com.rogerio.wallet_api.domain.model.Status;
import com.rogerio.wallet_api.domain.model.User;
import com.rogerio.wallet_api.domain.repository.UserRepository;
import com.rogerio.wallet_api.domain.repository.WalletRepository;
import com.rogerio.wallet_api.dto.request.AdminRegistrationRequest;
import com.rogerio.wallet_api.service.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import({SecurityConfig.class, TokenAuthenticationFilter.class})
class AdminRegistrationE2ETest extends BaseE2ETest {

  private static final String TOKEN_ADMIN_EMAIL = "token-admin@email.com";

  @Autowired
  private WalletRepository walletRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private TokenService tokenService;

  @BeforeEach
  void setUp() {
    walletRepository.deleteAllInBatch();
    userRepository.deleteAllInBatch();
  }

  private String createPersistedAdminToken() {
    User tokenAdmin = new User("Token Admin", TOKEN_ADMIN_EMAIL, "irrelevant-hash", Instant.now());
    tokenAdmin.setRole(Role.ROLE_ADMIN);
    userRepository.save(tokenAdmin);
    return tokenService.generateToken(TOKEN_ADMIN_EMAIL, Role.ROLE_ADMIN.name());
  }

  @Test
  @DisplayName("Should successfully register a new admin when valid request and valid admin token are provided")
  void givenValidRequestAndToken_whenRegisterAdmin_thenCreateAdminSuccessfully() throws Exception {
    // Given
    String adminToken = createPersistedAdminToken();
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
    User savedAdmin = userRepository.findByEmail("admin@email.com").orElseThrow();
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
    User customer = new User("Customer", "customer@email.com", "irrelevant-hash", Instant.now());
    userRepository.save(customer);
    String customerToken = tokenService.generateToken(customer.getEmail(), Role.ROLE_USER.name());

    // When & Then
    mockMvc.perform(post("/api/v1/admins/register")
            .header("Authorization", "Bearer " + customerToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.title").value("Access Denied"));

    assertThat(userRepository.existsByEmail("admin@email.com")).isFalse();
  }

  @Test
  @DisplayName("Should return 409 Conflict when attempting to register an admin with an existing email")
  void givenExistingEmail_whenRegisterDuplicateAdmin_thenReturnConflict() throws Exception {
    // Given
    String adminToken = createPersistedAdminToken();
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

    assertThat(userRepository.count()).isEqualTo(2); // token admin + registered admin
  }

  @Test
  @DisplayName("Should return 401 Unauthorized when token belongs to a blocked user")
  void givenBlockedUserToken_whenRegisterAdmin_thenReturnUnauthorized() throws Exception {
    // Given
    String adminToken = createPersistedAdminToken();
    User tokenAdmin = userRepository.findByEmail(TOKEN_ADMIN_EMAIL).orElseThrow();
    tokenAdmin.setStatus(Status.BLOCKED);
    userRepository.save(tokenAdmin);

    AdminRegistrationRequest request = new AdminRegistrationRequest(
        "User Admin", "admin@email.com", "password"
    );

    // When & Then
    mockMvc.perform(post("/api/v1/admins/register")
            .header("Authorization", "Bearer " + adminToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isUnauthorized());

    assertThat(userRepository.existsByEmail("admin@email.com")).isFalse();
  }
}