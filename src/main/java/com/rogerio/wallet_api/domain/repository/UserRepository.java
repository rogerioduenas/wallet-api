package com.rogerio.wallet_api.domain.repository;

import com.rogerio.wallet_api.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
  boolean existsByEmail(String email);
}
