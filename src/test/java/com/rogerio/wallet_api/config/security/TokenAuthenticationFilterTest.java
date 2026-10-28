package com.rogerio.wallet_api.config.security;

import com.rogerio.wallet_api.domain.model.Role;
import com.rogerio.wallet_api.domain.model.Status;
import com.rogerio.wallet_api.domain.model.User;
import com.rogerio.wallet_api.domain.repository.UserRepository;
import com.rogerio.wallet_api.service.AdminService;
import com.rogerio.wallet_api.service.TokenService;
import com.rogerio.wallet_api.web.AdminController;
import com.rogerio.wallet_api.web.BaseWebTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminController.class)
class TokenAuthenticationFilterTest extends BaseWebTest {

  private static final String TOKEN = "valid-jwt-token";
  private static final String EMAIL = "user@email.com";

  @MockitoBean
  private AdminService adminService;

  @MockitoBean
  private UserRepository userRepository;

  private User userWith(Role role, Status status) {
    User user = new User("Test User", EMAIL, "irrelevant-hash", Instant.now());
    user.setRole(role);
    user.setStatus(status);
    return user;
  }

  private void givenValidToken() {
    given(tokenService.isTokenValid(TOKEN)).willReturn(true);
    given(tokenService.extractUserEmail(TOKEN)).willReturn(EMAIL);
  }

  @Test
  @DisplayName("Should return 401 Unauthorized when token is missing or invalid")
  void givenMissingOrInvalidToken_whenAccessProtectedEndpoint_thenReturnUnauthorized() throws Exception {
    // Given
    String invalidToken = "invalid-jwt-token";
    given(tokenService.isTokenValid(invalidToken)).willReturn(false);

    // When & Then
    mockMvc.perform(post("/api/v1/admins/register")
            .header("Authorization", "Bearer " + invalidToken)
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.title").value("Authentication Failed"))
        .andExpect(jsonPath("$.type").value("/errors/unauthorized"));

    verifyNoInteractions(userRepository);
  }

  @Test
  @DisplayName("Should return 403 Forbidden when user lacks ADMIN role")
  void givenUserWithoutAdminRole_whenAccessProtectedEndpoint_thenReturnForbidden() throws Exception {
    // When & Then
    mockMvc.perform(post("/api/v1/admins/register")
            .with(user("customer@email.com").roles("USER"))
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.title").value("Access Denied"))
        .andExpect(jsonPath("$.type").value("/errors/forbidden"));
  }

  @Test
  @DisplayName("Should return 403 when token is valid but user is BLOCKED")
  void givenValidTokenAndBlockedUser_whenAccessProtectedEndpoint_thenReturnUnauthorized() throws Exception {
    // Given
    givenValidToken();
    given(userRepository.findByEmail(EMAIL))
        .willReturn(Optional.of(userWith(Role.ROLE_ADMIN, Status.BLOCKED)));

    // When & Then
    mockMvc.perform(get("/api/v1/admins/users")
            .header("Authorization", "Bearer " + TOKEN))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.title").value("User Blocked"));
  }

  @Test
  @DisplayName("Should return 401 when token is valid but user no longer exists")
  void givenValidTokenAndNonExistentUser_whenAccessProtectedEndpoint_thenReturnUnauthorized() throws Exception {
    // Given
    givenValidToken();
    given(userRepository.findByEmail(EMAIL)).willReturn(Optional.empty());

    // When & Then
    mockMvc.perform(get("/api/v1/admins/users")
            .header("Authorization", "Bearer " + TOKEN))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("Should not query the database when the Authorization header is missing")
  void givenMissingHeader_whenAccessProtectedEndpoint_thenReturnUnauthorizedWithoutDbLookup() throws Exception {
    // When & Then
    mockMvc.perform(get("/api/v1/admins/users"))
        .andExpect(status().isUnauthorized());

    verifyNoInteractions(userRepository);
  }
}