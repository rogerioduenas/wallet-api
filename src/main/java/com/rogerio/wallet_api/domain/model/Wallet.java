package com.rogerio.wallet_api.domain.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "wallets")
@Getter
@Setter
@NoArgsConstructor
public class Wallet {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", updatable = false, nullable = false)
  @Setter(AccessLevel.NONE)
  private UUID id;

  @Column(name = "balance", nullable = false, precision = 19, scale = 2)
  private BigDecimal balance;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false, unique = true)
  private User user;

  @Column(name = "created_at", nullable = false, updatable = false)
  @Setter(AccessLevel.NONE)
  private Instant createdAt;

  public Wallet(User user, Instant createdAt) {
    this.user = user;
    this.balance = BigDecimal.ZERO;
    this.createdAt = createdAt;
  }
}
