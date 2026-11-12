package com.rogerio.wallet_api.mapper;

import com.rogerio.wallet_api.domain.model.Transaction;
import com.rogerio.wallet_api.dto.response.TransactionResponse;
import com.rogerio.wallet_api.dto.response.TransactionStatementResponse;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class TransactionMapper {

  public TransactionResponse toDto(Transaction transaction) {
    return new TransactionResponse(transaction.getId(), transaction.getCreatedAt());
  }

  public TransactionStatementResponse toStatementDto(Transaction transaction) {
    UUID recipientId = transaction.getRecipientWallet() != null ?
        transaction.getRecipientWallet().getId() : null;

    return new TransactionStatementResponse(
        transaction.getId(),
        transaction.getAmount(),
        transaction.getType(),
        recipientId,
        transaction.getCreatedAt()
    );
  }
}
