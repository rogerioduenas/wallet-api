package com.rogerio.wallet_api.web;

import com.rogerio.wallet_api.domain.repository.UserRepository;
import com.rogerio.wallet_api.dto.request.CustomerRegistrationRequest;
import com.rogerio.wallet_api.dto.response.CustomerRegistrationResponse;
import com.rogerio.wallet_api.exception.EmailAlreadyExistsException;
import com.rogerio.wallet_api.service.CustomerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CustomerController.class)
public class CustomerControllerTest extends BaseWebTest {

  @MockitoBean
  private CustomerService customerService;

  @MockitoBean
  private UserRepository userRepository;

  private final String VALID_NAME = "Mike";
  private final String VALID_EMAIL = "valid@email.com";
  private final String VALID_PASSWORD = "password";
  private final CustomerRegistrationRequest VALID_REQUEST = new CustomerRegistrationRequest(
      VALID_NAME,
      VALID_EMAIL,
      VALID_PASSWORD);

  @Test
  @DisplayName("Should return 201 Created and response body when request is valid")
  void givenValidRequest_whenRegister_thenReturnCreated() throws Exception {
    // Given
    UUID userId = UUID.randomUUID();
    UUID walletId = UUID.randomUUID();
    Instant createdAt = Instant.parse("2026-10-07T10:00:00Z");
    CustomerRegistrationResponse response = new CustomerRegistrationResponse(userId, VALID_NAME, VALID_EMAIL, walletId, createdAt);

    given(customerService.register(any(CustomerRegistrationRequest.class))).willReturn(response);

    // When & Then
    mockMvc.perform(post("/api/v1/customers/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(VALID_REQUEST)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(userId.toString()))
        .andExpect(jsonPath("$.fullName").value(VALID_NAME))
        .andExpect(jsonPath("$.email").value(VALID_EMAIL))
        .andExpect(jsonPath("$.walletId").value(walletId.toString()));

    then(customerService).should().register(VALID_REQUEST);
  }

  @Test
  @DisplayName("Should return 409 Conflict when email already exists")
  void givenExistingEmail_whenRegister_thenReturnConflict() throws Exception {
    // Given
    given(customerService.register(any(CustomerRegistrationRequest.class)))
        .willThrow(new EmailAlreadyExistsException("Email already registered in the system"));

    // When & Then
    mockMvc.perform(post("/api/v1/customers/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(VALID_REQUEST)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.detail").value("Email already registered in the system"));

    then(customerService).should().register(VALID_REQUEST);
  }

  @Test
  @DisplayName("Should return 400 Bad Request when request body is invalid")
  void givenInvalidRequest_whenRegister_thenReturnBadRequest() throws Exception {
    // Given
    var invalidRequest = new CustomerRegistrationRequest("", "invalid-email", "");

    // When & Then
    mockMvc.perform(post("/api/v1/customers/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(invalidRequest)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title").value("Invalid Input Parameters")).andExpect(jsonPath("$.invalidFields").isArray());

    then(customerService).shouldHaveNoInteractions();
  }
}
