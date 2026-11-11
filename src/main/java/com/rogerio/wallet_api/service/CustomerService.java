package com.rogerio.wallet_api.service;

import com.rogerio.wallet_api.domain.model.User;
import com.rogerio.wallet_api.domain.model.Wallet;
import com.rogerio.wallet_api.domain.repository.UserRepository;
import com.rogerio.wallet_api.domain.repository.WalletRepository;
import com.rogerio.wallet_api.dto.request.CustomerRegistrationRequest;
import com.rogerio.wallet_api.dto.response.CustomerRegistrationResponse;
import com.rogerio.wallet_api.exception.EmailAlreadyExistsException;
import com.rogerio.wallet_api.mapper.CustomerMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
public class CustomerService {

  private final UserRepository userRepository;
  private final WalletService walletService;
  private final Clock clock;
  private final PasswordEncoder passwordEncoder;
  private final CustomerMapper customerMapper;

  public CustomerService(UserRepository userRepository,
                         WalletService walletService,
                         Clock clock,
                         PasswordEncoder passwordEncoder,
                         CustomerMapper customerMapper) {
    this.userRepository = userRepository;
    this.walletService = walletService;
    this.clock = clock;
    this.passwordEncoder = passwordEncoder;
    this.customerMapper = customerMapper;
  }

  @Transactional(rollbackFor = Exception.class)
  public CustomerRegistrationResponse register(CustomerRegistrationRequest request) {
    if (userRepository.existsByEmail(request.email())) {
      throw new EmailAlreadyExistsException("Email already registered in the system");
    }

    String encodedPassword = passwordEncoder.encode(request.password());
    User user = customerMapper.toEntity(request, encodedPassword, clock.instant());

    User savedUser = userRepository.save(user);

    Wallet savedWallet = walletService.createWalletForUser(savedUser);

    return customerMapper.toDTO(savedUser, savedWallet);
  }
}