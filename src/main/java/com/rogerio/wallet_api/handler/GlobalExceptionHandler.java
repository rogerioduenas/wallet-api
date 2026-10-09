package com.rogerio.wallet_api.handler;

import com.rogerio.wallet_api.exception.EmailAlreadyExistsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    record InvalidParam(String name, String reason) {}

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