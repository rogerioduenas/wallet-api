package com.rogerio.wallet_api.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", updatable = false, nullable = false)
  @Setter(AccessLevel.NONE)
  private UUID id;

  @Column(name = "full_name", nullable = false, length = 150)
  private String fullName;

  @Column(name = "email", nullable = false, unique = true, length = 150)
  private String email;

  @Column(name = "password", nullable = false)
  private String password;

  @Column(name = "created_at", nullable = false, updatable = false)
  @Setter(AccessLevel.NONE)
  private Instant createdAt;

  public User(String fullName, String email, String password, Instant createdAt) {
    this.fullName = fullName;
    this.email = email;
    this.password = password;
    this.createdAt = createdAt;
  }
}
