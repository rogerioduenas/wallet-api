package com.rogerio.wallet_api.infra.config;

import com.rogerio.wallet_api.domain.model.Role;
import com.rogerio.wallet_api.domain.model.User;
import com.rogerio.wallet_api.domain.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class MasterAdminSeeder implements CommandLineRunner {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  @Value("${app.admin.email}")
  private String adminEmail;

  @Value("${app.admin.password}")
  private String adminPassword;

  public MasterAdminSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
  }

  @Override
  @Transactional
  public void run(String... args) {
    if (userRepository.findByEmail(adminEmail).isEmpty()) {
      User admin = new User(
          "Master Admin",
          adminEmail,
          passwordEncoder.encode(adminPassword),
          Instant.now()
      );
      admin.setRole(Role.ROLE_ADMIN);

      userRepository.save(admin);
      System.out.println(">>> [SECURITY] Master Admin created successfully via CommandLineRunner!");
    } else {
      System.out.println(">>> [SECURITY] Master Admin already exists in the database. Skipping seed.");
    }
  }
}
