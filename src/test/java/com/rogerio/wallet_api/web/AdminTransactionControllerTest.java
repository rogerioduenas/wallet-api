package com.rogerio.wallet_api.web;

import com.rogerio.wallet_api.domain.model.enums.TransactionType;
import com.rogerio.wallet_api.dto.response.TransactionStatementResponse;
import com.rogerio.wallet_api.service.TransactionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminTransactionController.class)
public class AdminTransactionControllerTest extends BaseWebTest {

  @MockitoBean
  private TransactionService transactionService;

  @Test
  @DisplayName("Should return 200 OK with paginated statement when admin request is valid")
  void givenValidParameters_whenGetAdminStatement_thenReturnsPagedStatement() throws Exception {
    // Given
    UUID userId = UUID.randomUUID();
    Pageable pageable = PageRequest.of(0, 3);
    TransactionStatementResponse statementResponse = new TransactionStatementResponse(
        UUID.randomUUID(),
        BigDecimal.valueOf(100.00),
        TransactionType.DEPOSIT,
        null,
        Instant.now()
    );
    Page<TransactionStatementResponse> pagedResponse = new PageImpl<>(List.of(statementResponse), pageable, 1);

    given(transactionService.getStatementForAdmin(eq(userId), any(Pageable.class)))
        .willReturn(pagedResponse);

    // When & Then
    mockMvc.perform(get("/api/v1/admins/wallets/{userId}/statement", userId)
            .param("page", "0")
            .param("size", "3")
            .with(user("admin@email.com").roles("ADMIN")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].id").value(statementResponse.id().toString()))
        .andExpect(jsonPath("$.content[0].amount").value(statementResponse.amount().doubleValue()))
        .andExpect(jsonPath("$.content[0].type").value(statementResponse.type().toString()))
        .andExpect(jsonPath("$.totalElements").value(1));

    then(transactionService).should().getStatementForAdmin(eq(userId), any(Pageable.class));
  }

  @Test
  @DisplayName("Should return 200 OK with empty page when admin statement has no transactions")
  void givenNoTransactions_whenGetAdminStatement_thenReturnsEmptyPagedStatement() throws Exception {
    // Given
    UUID userId = UUID.randomUUID();
    Pageable pageable = PageRequest.of(0, 3);
    Page<TransactionStatementResponse> emptyPagedResponse = new PageImpl<>(List.of(), pageable, 0);

    given(transactionService.getStatementForAdmin(eq(userId), any(Pageable.class)))
        .willReturn(emptyPagedResponse);

    // When & Then
    mockMvc.perform(get("/api/v1/admins/wallets/{userId}/statement", userId)
            .with(user("admin@email.com").roles("ADMIN")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isEmpty())
        .andExpect(jsonPath("$.totalElements").value(0));

    then(transactionService).should().getStatementForAdmin(eq(userId), any(Pageable.class));
  }

  @Test
  @DisplayName("Should return 403 Forbidden when regular user attempts to access admin statement endpoint")
  void givenRegularUser_whenGetAdminStatement_thenReturnsForbidden() throws Exception {
    // Given
    UUID userId = UUID.randomUUID();

    // When & Then
    mockMvc.perform(get("/api/v1/admins/wallets/{userId}/statement", userId)
            .with(user("user@email.com").roles("USER")))
        .andExpect(status().isForbidden());

    then(transactionService).shouldHaveNoInteractions();
  }
}
