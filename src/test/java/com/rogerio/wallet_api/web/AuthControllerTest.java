package com.rogerio.wallet_api.web;

import com.rogerio.wallet_api.dto.request.AuthLoginRequest;
import com.rogerio.wallet_api.dto.response.AuthTokenResponse;
import com.rogerio.wallet_api.exception.InvalidCredentialsException;
import com.rogerio.wallet_api.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
public class AuthControllerTest extends BaseWebTest {

  @MockitoBean
  private AuthService authService;

  private final String VALID_EMAIL = "valid@email.com";
  private final String VALID_PASSWORD = "password";
  private final AuthLoginRequest VALID_REQUEST = new AuthLoginRequest(VALID_EMAIL, VALID_PASSWORD);

  @Test
  @DisplayName("Should return 200 OK and token response when credentials are valid")
  void givenValidCredentials_whenLogin_thenReturnsTokenResponse() throws Exception {
    // Given
    AuthTokenResponse response = new AuthTokenResponse("token", "Bearer", "role", 1L);

    given(authService.login(any(AuthLoginRequest.class))).willReturn(response);

    // When
    mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(VALID_REQUEST)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").value("token"))
        .andExpect(jsonPath("$.type").value("Bearer"))
        .andExpect(jsonPath("$.role").value("role"))
        .andExpect(jsonPath("$.expiresIn").value(1L));

    // Then
    then(authService).should().login(any(AuthLoginRequest.class));
  }

  @Test
  @DisplayName("Should return 401 Unauthorized when credentials are invalid")
  void givenInvalidCredentials_whenLogin_thenReturnsUnauthorized() throws Exception {
    // Given
    given(authService.login(any(AuthLoginRequest.class))).willThrow(new InvalidCredentialsException("Invalid email or password"));

    // When & Then
    mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(VALID_REQUEST)))
        .andExpect(status().isUnauthorized());

    then(authService).should().login(any(AuthLoginRequest.class));
  }

  @Test
  @DisplayName("givenBlankEmail_whenLogin_thenReturnsBadRequest")
  void givenBlankEmail_whenLogin_thenReturnsBadRequest() throws Exception {
    // Given
    AuthLoginRequest invalidRequest = new AuthLoginRequest("", "password");

    // When & Then
    mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(invalidRequest)))
        .andExpect(status().isBadRequest());

    then(authService).shouldHaveNoInteractions();
  }
}
