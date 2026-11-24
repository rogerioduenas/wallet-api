package com.rogerio.wallet_api.mapper;

import com.rogerio.wallet_api.domain.model.Transaction;
import com.rogerio.wallet_api.domain.model.enums.TransactionDirection;
import com.rogerio.wallet_api.domain.model.enums.TransactionType;
import com.rogerio.wallet_api.dto.response.TransactionResponse;
import com.rogerio.wallet_api.dto.response.TransactionStatementResponse;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class TransactionMapper {

  public TransactionResponse toDto(Transaction transaction) {
    return new TransactionResponse(transaction.getId(), transaction.getCreatedAt());
  }

  public TransactionStatementResponse toStatementDto(Transaction transaction, UUID queriedWalletId) {
    UUID recipientId = transaction.getRecipientWallet() != null
        ? transaction.getRecipientWallet().getId()
        : null;

    TransactionDirection direction = resolveDirection(transaction, recipientId, queriedWalletId);

    return new TransactionStatementResponse(
        transaction.getId(),
        transaction.getAmount(),
        transaction.getType(),
        direction,
        recipientId,
        transaction.getCreatedAt()
    );
  }

  private TransactionDirection resolveDirection(
      Transaction transaction, UUID recipientId, UUID queriedWalletId) {
    if (transaction.getType() == TransactionType.DEPOSIT) {
      return TransactionDirection.IN;
    }

    if (transaction.getType() == TransactionType.WITHDRAW) {
      return TransactionDirection.OUT;
    }

    if (queriedWalletId != null && queriedWalletId.equals(recipientId)) {
      return TransactionDirection.IN;
    }

    return TransactionDirection.OUT;
  }
}
