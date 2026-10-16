package com.rogerio.wallet_api.web.util;

import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

public class UriHelper {

  private UriHelper() {}

  public static URI createResourceUri(String pathTemplate, UUID resourceId) {
    return ServletUriComponentsBuilder.fromCurrentContextPath()
        .path(pathTemplate)
        .buildAndExpand(resourceId)
        .toUri();
  }
}
