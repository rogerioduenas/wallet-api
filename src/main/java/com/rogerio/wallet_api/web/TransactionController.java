package com.rogerio.wallet_api.web;

import com.rogerio.wallet_api.domain.model.enums.TransactionType;
import com.rogerio.wallet_api.dto.request.DepositRequest;
import com.rogerio.wallet_api.dto.request.TransferRequest;
import com.rogerio.wallet_api.dto.request.WithdrawRequest;
import com.rogerio.wallet_api.dto.response.TransactionResponse;
import com.rogerio.wallet_api.dto.response.TransactionStatementResponse;
import com.rogerio.wallet_api.service.TransactionService;
import com.rogerio.wallet_api.web.util.UriHelper;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.Instant;

@RestController
@RequestMapping("/api/v1/wallets")
public class TransactionController {

  private final TransactionService transactionService;

  public TransactionController(TransactionService transactionService) {
    this.transactionService = transactionService;
  }

  @PostMapping("/deposit")
  public ResponseEntity<TransactionResponse> deposit(@Valid @RequestBody DepositRequest request) {
    TransactionResponse response = transactionService.deposit(request);

    URI location = UriHelper.createResourceUri(
        "/api/v1/transactions/{id}", response.id());

    return ResponseEntity.created(location).body(response);
  }

  @PostMapping("/withdraw")
  public ResponseEntity<TransactionResponse> withdraw(@Valid @RequestBody WithdrawRequest request) {
    TransactionResponse response = transactionService.withdraw(request);

    URI location = UriHelper.createResourceUri(
        "/api/v1/transactions/{id}", response.id());

    return ResponseEntity.created(location).body(response);
  }

  @PostMapping("/transfer")
  public ResponseEntity<TransactionResponse> transfer(@Valid @RequestBody TransferRequest request) {
    TransactionResponse response = transactionService.transfer(request);

    URI location = UriHelper.createResourceUri(
        "/api/v1/transactions/{id}", response.id()
    );

    return ResponseEntity.created(location).body(response);
  }

  @GetMapping("/statement")
  public ResponseEntity<Page<TransactionStatementResponse>> statement(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,
      @RequestParam(required = false) TransactionType type,
      @PageableDefault(page = 0, size = 3) Pageable pageable) {
    Page<TransactionStatementResponse> response = transactionService.getCustomerStatement(startDate, endDate, type, pageable);

    return ResponseEntity.ok(response);
  }
}
