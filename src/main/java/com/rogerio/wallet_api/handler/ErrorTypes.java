package com.rogerio.wallet_api.handler;

import java.net.URI;

public final class ErrorTypes {

  private static final String BASE_URL = "/errors/";

  public static final URI EMAIL_ALREADY_EXISTS = URI.create(BASE_URL + "email-already-exists");
  public static final URI INVALID_PARAMS = URI.create(BASE_URL + "invalid-parameters");
  public static final URI INTERNAL_SERVER_ERROR = URI.create(BASE_URL + "internal-server-error");
  public static final URI INVALID_CREDENTIALS = URI.create(BASE_URL + "invalid-credentials");
  public static final URI UNAUTHORIZED = URI.create(BASE_URL + "unauthorized");
  public static final URI FORBIDDEN = URI.create(BASE_URL + "forbidden");
  public static final URI NOT_FOUND = URI.create(BASE_URL + "user-not-found");
  public static final URI INVALID_REQUEST_BODY = URI.create(BASE_URL + "invalid-request-body");

  private ErrorTypes() {}
}
