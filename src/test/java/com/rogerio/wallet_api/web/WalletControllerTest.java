package com.rogerio.wallet_api.web;

import com.rogerio.wallet_api.dto.response.BalanceResponse;
import com.rogerio.wallet_api.exception.WalletNotFoundException;
import com.rogerio.wallet_api.service.WalletService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WalletController.class)
public class WalletControllerTest extends BaseWebTest {

  @MockitoBean
  private WalletService walletService;

  @Test
  @DisplayName("Should return 200 OK and balance response when authenticated user has a valid wallet")
  void givenAuthenticatedUserWithWallet_whenGetBalance_thenReturn200AndBalanceResponse() throws Exception {
    // Given
    BalanceResponse response = new BalanceResponse(UUID.randomUUID(), BigDecimal.TEN);

    given(walletService.getBalance()).willReturn(response);

    // When & Then
    mockMvc.perform(get("/api/v1/wallets/balance")
            .with(user("email@email.com").roles("USER")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.customerId").value(response.customerId().toString()))
        .andExpect(jsonPath("$.balance").value(response.balance().toString()));
  }

  @Test
  @DisplayName("Should return proper error status when wallet is not found for authenticated user")
  void givenAuthenticatedUserWithoutWallet_whenGetBalance_thenReturnNotFoundError() throws Exception {
    // Given
    given(walletService.getBalance())
        .willThrow(new WalletNotFoundException("Wallet not found"));

    // When & Then
    mockMvc.perform(get("/api/v1/wallets/balance")
        .with(user("email@email.com").roles("USER")))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("Should return 401 Unauthorized when request lacks valid authentication")
  void givenUnauthenticatedUser_whenGetBalance_thenReturn401Unauthorized() throws Exception {
     // When & Then
    mockMvc.perform(get("/api/v1/wallets/balance"))
        .andExpect(status().isUnauthorized());
  }
}
