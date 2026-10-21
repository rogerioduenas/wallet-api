package com.rogerio.wallet_api.web;

import com.rogerio.wallet_api.dto.request.AdminRegistrationRequest;
import com.rogerio.wallet_api.dto.response.AdminRegistrationResponse;
import com.rogerio.wallet_api.dto.response.UserResponse;
import com.rogerio.wallet_api.service.AdminService;
import com.rogerio.wallet_api.web.util.UriHelper;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

  @GetMapping("/users")
  public ResponseEntity<Page<UserResponse>> getUsers(
      @PageableDefault(page = 0, size = 5, sort = "fullName") Pageable pageable) {
    Page<UserResponse> response = adminService.listUsers(pageable);
    return ResponseEntity.ok(response);
  }
}
