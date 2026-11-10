package com.rogerio.wallet_api.exception;

public class SelfTransferException extends RuntimeException {
  public SelfTransferException(String message) {
    super(message);
  }
}
