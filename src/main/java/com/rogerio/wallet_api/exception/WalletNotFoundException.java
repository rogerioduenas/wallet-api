package com.rogerio.wallet_api.exception;

public class WalletNotFoundException extends RuntimeException {
  public WalletNotFoundException(String message) {
    super(message);
  }
}
