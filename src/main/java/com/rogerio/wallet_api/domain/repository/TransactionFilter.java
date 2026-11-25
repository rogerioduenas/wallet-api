package com.rogerio.wallet_api.domain.repository;

import com.rogerio.wallet_api.domain.model.Transaction;
import com.rogerio.wallet_api.domain.model.Wallet;
import com.rogerio.wallet_api.domain.model.enums.TransactionType;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.UUID;

public class TransactionFilter {

  public static Specification<Transaction> belongsToWallet(UUID walletId) {
    return (root, query, cb) -> {
      Join<Transaction, Wallet> recipientJoin = root.join("recipientWallet", JoinType.LEFT);

      return cb.or(
          cb.equal(root.get("wallet").get("id"), walletId),
          cb.equal(recipientJoin.get("id"), walletId)
      );
    };
  }

  public static Specification<Transaction> createdAfter(Instant startDate) {
    return (root, query, cb) -> startDate == null ? null : cb.greaterThanOrEqualTo(root.get("createdAt"), startDate);
  }

  public static Specification<Transaction> createdBefore(Instant endDate) {
    return (root, query, cb) -> endDate == null ? null : cb.lessThanOrEqualTo(root.get("createdAt"), endDate);
  }

  public static Specification<Transaction> hasType(TransactionType type) {
    return (root, query, cb) -> type == null ? null : cb.equal(root.get("type"), type);
  }
}
