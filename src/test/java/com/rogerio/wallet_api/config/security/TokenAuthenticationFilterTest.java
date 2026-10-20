package com.rogerio.wallet_api.config.security;

import com.rogerio.wallet_api.service.AdminService;
import com.rogerio.wallet_api.service.TokenService;
import com.rogerio.wallet_api.web.AdminController;
import com.rogerio.wallet_api.web.BaseWebTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminController.class)
class TokenAuthenticationFilterTest extends BaseWebTest {

  @MockitoBean
  private AdminService adminService;

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
}
