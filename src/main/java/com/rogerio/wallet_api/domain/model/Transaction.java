package com.rogerio.wallet_api.domain.model;

import com.rogerio.wallet_api.domain.model.enums.TransactionType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transactions")
@Getter
@NoArgsConstructor
public class Transaction {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "wallet_id", referencedColumnName = "id", nullable = false)
  private Wallet wallet;

  @Column(name = "amount", nullable = false, updatable = false, precision = 19, scale = 2)
  private BigDecimal amount;

  @Enumerated(EnumType.STRING)
  @Column(name = "type",  nullable = false, updatable = false)
  private TransactionType type;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  public Transaction(Wallet wallet, BigDecimal amount, TransactionType type, Instant createdAt) {
    this.wallet = wallet;
    this.amount = amount;
    this.type = type;
    this.createdAt = createdAt;
  }
}
