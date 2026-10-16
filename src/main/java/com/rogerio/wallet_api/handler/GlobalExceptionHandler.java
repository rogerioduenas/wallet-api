package com.rogerio.wallet_api.handler;

import com.rogerio.wallet_api.exception.EmailAlreadyExistsException;
import com.rogerio.wallet_api.exception.InvalidCredentialsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(EmailAlreadyExistsException.class)
  public ProblemDetail handleEmailAlreadyExists(EmailAlreadyExistsException ex) {
    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    problemDetail.setTitle("Resource Conflict");
    problemDetail.setType(ErrorTypes.EMAIL_ALREADY_EXISTS);
    problemDetail.setProperty("timestamp", Instant.now());

    return problemDetail;
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ProblemDetail handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
    record InvalidParam(String name, String reason) {
    }

    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
        HttpStatus.BAD_REQUEST,
        "One or more fields are invalid. Please check the 'invalidFields' property."
    );

    problemDetail.setTitle("Invalid Input Parameters");
    problemDetail.setType(ErrorTypes.INVALID_PARAMS);
    problemDetail.setProperty("timestamp", Instant.now());

    List<InvalidParam> invalidFields = ex.getBindingResult()
        .getFieldErrors()
        .stream()
        .map(error -> new InvalidParam(error.getField(), error.getDefaultMessage()))
        .toList();

    problemDetail.setProperty("invalidFields", invalidFields);

    return problemDetail;
  }

  @ExceptionHandler(InvalidCredentialsException.class)
  public ProblemDetail handleInvalidCredentialsException(InvalidCredentialsException ex) {
    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
        HttpStatus.UNAUTHORIZED,
        ex.getMessage()
    );

    problemDetail.setTitle("Invalid Credentials");
    problemDetail.setType(ErrorTypes.INVALID_CREDENTIALS);
    problemDetail.setProperty("timestamp", Instant.now());

    return problemDetail;
  }

  @ExceptionHandler(AuthenticationException.class)
  public ProblemDetail handleAuthenticationException(AuthenticationException ex) {
    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
        HttpStatus.UNAUTHORIZED,
        ex.getMessage()
    );
    problemDetail.setTitle("Authentication Failed");
    problemDetail.setType(ErrorTypes.UNAUTHORIZED);
    problemDetail.setProperty("timestamp", Instant.now());

    return problemDetail;
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ProblemDetail handleAccessDeniedException(AccessDeniedException ex) {
    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
        HttpStatus.FORBIDDEN,
        ex.getMessage()
    );

    problemDetail.setTitle("Access Denied");
    problemDetail.setType(ErrorTypes.FORBIDDEN);
    problemDetail.setProperty("timestamp", Instant.now());

    return problemDetail;
  }

  @ExceptionHandler(Exception.class)
  public ProblemDetail handleUncaughtException(Exception ex) {
    log.error("Unhandled exception caught in GlobalExceptionHandler", ex);

    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "An unexpected internal error occurred. Please contact support if the issue persists."
    );

    problemDetail.setTitle("Internal Server Error");
    problemDetail.setType(ErrorTypes.INTERNAL_SERVER_ERROR);
    problemDetail.setProperty("timestamp", Instant.now());

    return problemDetail;
  }
}