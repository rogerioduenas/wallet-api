package com.rogerio.wallet_api.web;

import com.rogerio.wallet_api.dto.request.CustomerRegistrationRequest;
import com.rogerio.wallet_api.dto.response.CustomerRegistrationResponse;
import com.rogerio.wallet_api.service.CustomerService;
import com.rogerio.wallet_api.web.util.UriHelper;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/customers")
public class CustomerController {

  private final CustomerService customerService;

  public CustomerController(CustomerService customerService) {
    this.customerService = customerService;
  }

  @PostMapping("/register")
  public ResponseEntity<CustomerRegistrationResponse> register(@Valid @RequestBody CustomerRegistrationRequest request) {
    CustomerRegistrationResponse response = customerService.register(request);

    URI location = UriHelper.createResourceUri(
        "/api/v1/customers/{id}", response.id());

    return ResponseEntity.created(location).body(response);
  }
}
