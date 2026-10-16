package com.rogerio.wallet_api.web;

import com.rogerio.wallet_api.domain.model.Role;
import com.rogerio.wallet_api.dto.request.AdminRegistrationRequest;
import com.rogerio.wallet_api.dto.response.AdminRegistrationResponse;
import com.rogerio.wallet_api.exception.EmailAlreadyExistsException;
import com.rogerio.wallet_api.service.AdminService;
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

@WebMvcTest(AdminController.class)
public class AdminControllerTest extends BaseWebTest {

  @MockitoBean
  private AdminService adminService;

  private final String VALID_NAME = "Big Boss";
  private final String VALID_EMAIL = "admin@email.com";
  private final String VALID_PASSWORD = "password";
  private final AdminRegistrationRequest VALID_REQUEST = new AdminRegistrationRequest(
      VALID_NAME, VALID_EMAIL, VALID_PASSWORD);

  @Test
  @DisplayName("Should return 201 Created and response body when request is valid")
  void givenValidRequest_whenRegister_thenReturnCreated() throws Exception {
    // Given
    UUID adminId = UUID.randomUUID();
    Instant createdAt = Instant.parse("2026-10-07T10:00:00Z");
    AdminRegistrationResponse response = new AdminRegistrationResponse(
        adminId, VALID_NAME, VALID_EMAIL, Role.ROLE_ADMIN, createdAt);

    given(adminService.register(any(AdminRegistrationRequest.class))).willReturn(response);

    // When & Then
    mockMvc.perform(post("/api/v1/admins/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(VALID_REQUEST)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(adminId.toString()))
        .andExpect(jsonPath("$.fullName").value(VALID_NAME))
        .andExpect(jsonPath("$.email").value(VALID_EMAIL))
        .andExpect(jsonPath("$.role").value(Role.ROLE_ADMIN.toString()))
        .andExpect(jsonPath("$.createdAt").value(createdAt.toString()));

    then(adminService).should().register(any(AdminRegistrationRequest.class));
  }

  @Test
  @DisplayName("Should return 409 Conflict when email already exists")
  void givenExistingEmail_whenRegister_thenReturnConflict() throws Exception {
    // Given
    given(adminService.register(any(AdminRegistrationRequest.class)))
        .willThrow(new EmailAlreadyExistsException("Email already registered in the system"));

    // When & Then
    mockMvc.perform(post("/api/v1/admins/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(VALID_REQUEST)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.detail").value("Email already registered in the system"));

    then(adminService).should().register(VALID_REQUEST);
  }

  @Test
  @DisplayName("Should return 400 Bad Request when request body is invalid")
  void givenInvalidRequest_whenRegister_thenReturnBadRequest() throws Exception {
    // Given
    var invalidRequest = new AdminRegistrationRequest("", "invalid-email", "");

    // When & Then
    mockMvc.perform(post("/api/v1/admins/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(invalidRequest)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title").value("Invalid Input Parameters"))
        .andExpect(jsonPath("$.invalidFields").isArray());

    then(adminService).shouldHaveNoInteractions();
  }
}