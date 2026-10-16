package com.rogerio.wallet_api.web;

import com.rogerio.wallet_api.dto.request.AdminRegistrationRequest;
import com.rogerio.wallet_api.dto.response.AdminRegistrationResponse;
import com.rogerio.wallet_api.service.AdminService;
import com.rogerio.wallet_api.web.util.UriHelper;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/admins")
public class AdminController {

  private final AdminService adminService;

  public AdminController(AdminService adminService) {
    this.adminService = adminService;
  }

  @PostMapping("/register")
  public ResponseEntity<AdminRegistrationResponse> register(@Valid @RequestBody AdminRegistrationRequest request) {
    AdminRegistrationResponse response = adminService.register(request);

    URI location = UriHelper.createResourceUri(
        "api/v1/admins/{id}", response.id());

    return ResponseEntity.created(location).body(response);
  }
}
