package com.rogerio.wallet_api.web;

import com.rogerio.wallet_api.dto.request.UserRegistrationRequest;
import com.rogerio.wallet_api.dto.response.UserRegistrationResponse;
import com.rogerio.wallet_api.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

  private final UserService userService;

  public UserController(UserService userService) {
    this.userService = userService;
  }

  @PostMapping("/register")
  public ResponseEntity<UserRegistrationResponse> register(@Valid @RequestBody UserRegistrationRequest request) {
    UserRegistrationResponse response = userService.register(request);

    URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
        .path("/api/v1/users/{id}")
        .buildAndExpand(response.id())
        .toUri();

    return ResponseEntity.created(location).body(response);
  }
}
