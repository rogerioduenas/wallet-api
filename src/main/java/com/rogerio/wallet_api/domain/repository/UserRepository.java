package com.rogerio.wallet_api.domain.repository;

import com.rogerio.wallet_api.domain.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
  boolean existsByEmail(String email);
  Optional<User> findByEmail(String email);
  Page<User> findAll(Pageable pageable);
}
