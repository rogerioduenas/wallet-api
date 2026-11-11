package com.rogerio.wallet_api.web;

import com.rogerio.wallet_api.dto.response.BalanceResponse;
import com.rogerio.wallet_api.service.WalletService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/wallets")
public class WalletController {

  private final WalletService walletService;

  public WalletController(WalletService walletService) {
    this.walletService = walletService;
  }

  @GetMapping("/balance")
  public ResponseEntity<BalanceResponse> getBalance() {
    BalanceResponse response = walletService.getBalance();
    return ResponseEntity.ok(response);
  }
}
