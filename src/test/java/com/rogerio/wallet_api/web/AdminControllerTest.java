package com.rogerio.wallet_api.web;

import com.rogerio.wallet_api.domain.model.Role;
import com.rogerio.wallet_api.domain.model.Status;
import com.rogerio.wallet_api.domain.repository.UserRepository;
import com.rogerio.wallet_api.dto.request.AdminRegistrationRequest;
import com.rogerio.wallet_api.dto.request.UpdateStatusRequest;
import com.rogerio.wallet_api.dto.response.AdminRegistrationResponse;
import com.rogerio.wallet_api.dto.response.UserResponse;
import com.rogerio.wallet_api.exception.EmailAlreadyExistsException;
import com.rogerio.wallet_api.exception.UserBlockedException;
import com.rogerio.wallet_api.exception.UserNotFoundException;
import com.rogerio.wallet_api.service.AdminService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminController.class)
public class AdminControllerTest extends BaseWebTest {

  @MockitoBean
  private AdminService adminService;

  @MockitoBean
  private UserRepository userRepository;

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
            .content(objectMapper.writeValueAsString(VALID_REQUEST))
        .with(user("user@wallet.com").roles("ADMIN")))
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
            .content(objectMapper.writeValueAsString(VALID_REQUEST))
            .with(user("user@wallet.com").roles("ADMIN")))
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
            .content(objectMapper.writeValueAsString(invalidRequest))
            .with(user("user@wallet.com").roles("ADMIN")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title").value("Invalid Input Parameters"))
        .andExpect(jsonPath("$.invalidFields").isArray());

    then(adminService).shouldHaveNoInteractions();
  }

  @Test
  @DisplayName("Should return 200 OK with paginated users when user has ADMIN role")
  void givenAdminUser_whenListUsers_thenReturnPageOfUsers() throws Exception {
    // Given
    given(adminService.listUsers(any(Pageable.class)))
        .willReturn(new PageImpl<>(Collections.emptyList()));

    // When & Then
    mockMvc.perform(get("/api/v1/admins/users")
            .with(user("admin@email.com").roles("ADMIN")))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("Should return 403 Forbidden when user has CUSTOMER role")
  void givenCustomerUser_whenListUsers_thenReturnForbidden() throws Exception {
    // Given / When & Then
    mockMvc.perform(get("/api/v1/admins/users")
            .with(user("customer@email.com").roles("CUSTOMER")))
        .andExpect(status().isForbidden());

    then(adminService).shouldHaveNoInteractions();
  }

  @Test
  @DisplayName("Should return 401 Unauthorized when authentication token is missing")
  void givenUnauthenticatedUser_whenListUsers_thenReturnUnauthorized() throws Exception {
    // Given / When & Then
    mockMvc.perform(get("/api/v1/admins/users"))
        .andExpect(status().isUnauthorized());

    then(adminService).shouldHaveNoInteractions();
  }

  @Test
  @DisplayName("Should successfully update user status when request is valid")
  void givenValidIdAndStatus_whenUpdateStatus_thenReturnsOk() throws Exception {
    // Given
    UUID userId = UUID.randomUUID();
    var request = new UpdateStatusRequest(Status.BLOCKED);
    var response = new UserResponse(
        userId,
        "John Doe",
        "john@email.com",
        Role.ROLE_USER,
        Instant.now(),
        Status.BLOCKED
    );

    given(adminService.updateUserStatus(eq(userId), any(UpdateStatusRequest.class)))
        .willReturn(response);

    // When & Then
    mockMvc.perform(patch("/api/v1/admins/users/{id}/status", userId)
            .with(user("admin@email.com").roles("ADMIN"))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(userId.toString()))
        .andExpect(jsonPath("$.status").value("BLOCKED"));
  }

  @Test
  @DisplayName("Should return 404 Not Found when user does not exist")
  void givenNonExistentUserId_whenUpdateStatus_thenReturnsNotFound() throws Exception {
    // Given
    UUID userId = UUID.randomUUID();
    var request = new UpdateStatusRequest(Status.BLOCKED);

    given(adminService.updateUserStatus(eq(userId), any(UpdateStatusRequest.class)))
        .willThrow(new UserNotFoundException("User not found, please check the id and try again"));

    // When & Then
    mockMvc.perform(patch("/api/v1/admins/users/{id}/status", userId)
            .with(user("admin@email.com").roles("ADMIN"))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.title").value("User Not Found"))
        .andExpect(jsonPath("$.detail").value("User not found"));
  }

  @Test
  @DisplayName("Should return 403 Forbidden when user is blocked")
  void givenBlockedUser_whenUpdateStatus_thenReturnsForbidden() throws Exception {
    // Given
    UUID userId = UUID.randomUUID();
    var request = new UpdateStatusRequest(Status.ACTIVE);

    given(adminService.updateUserStatus(eq(userId), any(UpdateStatusRequest.class)))
        .willThrow(new UserBlockedException("User is blocked"));

    // When & Then
    mockMvc.perform(patch("/api/v1/admins/users/{id}/status", userId)
            .with(user("admin@email.com").roles("ADMIN"))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.title").value("User Blocked"))
        .andExpect(jsonPath("$.detail").value("User is blocked"));
  }

  @Test
  @DisplayName("Should return 400 Bad Request when status enum value is invalid")
  void givenInvalidEnumStatus_whenUpdateStatus_thenReturnsBadRequest() throws Exception {
    // Given
    UUID userId = UUID.randomUUID();
    String invalidRequestBody = "{\"status\":\"INVALID_STATUS\"}";

    // When & Then
    mockMvc.perform(patch("/api/v1/admins/users/{id}/status", userId)
            .with(user("admin@email.com").roles("ADMIN"))
            .contentType(MediaType.APPLICATION_JSON)
            .content(invalidRequestBody))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title").value("Bad Request"))
        .andExpect(jsonPath("$.detail").exists());
  }
}