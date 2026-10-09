package com.rogerio.wallet_api.e2e;

import com.rogerio.wallet_api.domain.model.User;
import com.rogerio.wallet_api.domain.model.Wallet;
import com.rogerio.wallet_api.domain.repository.UserRepository;
import com.rogerio.wallet_api.domain.repository.WalletRepository;
import com.rogerio.wallet_api.dto.request.UserRegistrationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserRegistrationE2ETest extends BaseE2ETest {

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private WalletRepository walletRepository;

  @BeforeEach
  void setUp() {
    walletRepository.deleteAllInBatch();
    userRepository.deleteAllInBatch();
  }

  @Test
  @DisplayName("Should successfully register a new user, create a wallet, and persist them in the database")
  void givenValidRegistrationRequest_whenRegisterUser_thenCreateUserAndWallet() throws Exception {
    // Given
    UserRegistrationRequest request = new UserRegistrationRequest("Mike", "mike@email.com", "password");

    // When
    mockMvc.perform(post("/api/v1/users/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.fullName").value("Mike"))
        .andExpect(jsonPath("$.email").value("mike@email.com"))
        .andExpect(jsonPath("$.walletId").exists());

    // Then
    User savedUser = userRepository.findAll().get(0);
    assertThat(savedUser.getFullName()).isEqualTo("Mike");
    assertThat(savedUser.getEmail()).isEqualTo("mike@email.com");
    assertThat(savedUser.getPassword()).startsWith("$2a$");

    List<Wallet> wallets = walletRepository.findAll();
    assertThat(wallets).hasSize(1);
    assertThat(wallets.getFirst().getUser().getId()).isEqualTo(savedUser.getId());
  }

  @Test
  @DisplayName("Should return 409 Conflict when attempting to register a user with an existing email")
  void givenExistingUserEmail_whenRegisterDuplicateUser_thenReturnConflict() throws Exception {
    // Given
    String existingEmail = "mike@email.com";
    var initialRequest = new UserRegistrationRequest("Mike", existingEmail, "password");

    mockMvc.perform(post("/api/v1/users/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(initialRequest)))
        .andExpect(status().isCreated());

    var duplicateRequest = new UserRegistrationRequest("Anna", existingEmail, "password");

    // When
    var response = mockMvc.perform(post("/api/v1/users/register")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(duplicateRequest)));

    // Then
    response
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.title").value("Resource Conflict"))
        .andExpect(jsonPath("$.detail").value("Email already registered in the system"));

    assertThat(userRepository.count()).isEqualTo(1);
  }
}
