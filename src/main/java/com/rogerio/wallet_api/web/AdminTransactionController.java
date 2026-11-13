package com.rogerio.wallet_api.web;

import com.rogerio.wallet_api.dto.response.TransactionStatementResponse;
import com.rogerio.wallet_api.service.TransactionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admins/wallets")
public class AdminTransactionController {

  private final TransactionService transactionService;

  public AdminTransactionController(TransactionService transactionService) {
    this.transactionService = transactionService;
  }

  @GetMapping("/{userId}/statement")
  public ResponseEntity<Page<TransactionStatementResponse>> statement(
      @PathVariable UUID userId,
      @PageableDefault(page = 0, size = 10) Pageable pageable) {

    Page<TransactionStatementResponse> response = transactionService
        .getStatementForAdmin(userId, pageable);

    return ResponseEntity.ok(response);
  }
}
