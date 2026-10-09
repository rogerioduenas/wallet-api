package com.rogerio.wallet_api.handler;

import java.net.URI;

public final class ErrorTypes {

  private static final String BASE_URL = "/errors/";

  public static final URI EMAIL_ALREADY_EXISTS = URI.create(BASE_URL + "email-already-exists");
  public static final URI INVALID_PARAMS = URI.create(BASE_URL + "invalid-parameters");
  public static final URI INTERNAL_SERVER_ERROR = URI.create(BASE_URL + "internal-server-error");

  private ErrorTypes() {}
}
