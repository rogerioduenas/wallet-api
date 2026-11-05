package com.rogerio.wallet_api.web;

import com.rogerio.wallet_api.dto.request.DepositRequest;
import com.rogerio.wallet_api.dto.request.WithdrawRequest;
import com.rogerio.wallet_api.dto.response.TransactionResponse;
import com.rogerio.wallet_api.service.TransactionService;
import com.rogerio.wallet_api.web.util.UriHelper;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

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
}
