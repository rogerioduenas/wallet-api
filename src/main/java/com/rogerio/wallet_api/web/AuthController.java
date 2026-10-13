package com.rogerio.wallet_api.web;

import com.rogerio.wallet_api.dto.request.AuthLoginRequest;
import com.rogerio.wallet_api.dto.response.AuthTokenResponse;
import com.rogerio.wallet_api.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  @PostMapping("/login")
  public ResponseEntity<AuthTokenResponse> login(@Valid @RequestBody AuthLoginRequest request){
    AuthTokenResponse response = authService.login(request);
    return ResponseEntity.ok(response);
  }
}
