package com.rogerio.wallet_api.domain.repository;

import com.rogerio.wallet_api.domain.model.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WalletRepository extends JpaRepository<Wallet, Long> {
}
