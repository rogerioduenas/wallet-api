package com.rogerio.wallet_api.mapper;

import com.rogerio.wallet_api.domain.model.Transaction;
import com.rogerio.wallet_api.dto.response.TransactionResponse;
import org.springframework.stereotype.Component;

@Component
public class TransactionMapper {

  public TransactionResponse toDto(Transaction transaction) {
    return new TransactionResponse(transaction.getId(), transaction.getCreatedAt());
  }
}
