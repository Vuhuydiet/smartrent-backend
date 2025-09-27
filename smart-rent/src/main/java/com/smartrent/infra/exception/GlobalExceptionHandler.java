package com.smartrent.infra.exception;

import com.smartrent.dto.response.ApiResponse;
import com.smartrent.infra.exception.model.DomainCode;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.nio.file.AccessDeniedException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@ControllerAdvice
public class GlobalExceptionHandler {

  private static final String MIN_ATTRIBUTE = "min";

  // Mapping of parameter names to custom messages
  private static final Map<String, String> PARAMETER_MESSAGES = new HashMap<>() {{
    put("postId", "REQUIRED_POST_ID");
    put("userId", "REQUIRED_USER_ID");
    put("reactId", "REQUIRED_REACT_ID");
    put("commentId", "REQUIRED_COMMENT_ID");
  }};

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Void>> handleUncategorizedException(Exception exception) {
    DomainCode domainCode = DomainCode.UNKNOWN_ERROR;
    return ResponseEntity.status(domainCode.getStatus())
        .body(ApiResponse.<Void>builder()
            .code(domainCode.getValue())
            .message(exception.getMessage())
            .build());
  }

  @ExceptionHandler(DomainException.class)
  public ResponseEntity<ApiResponse<Void>> handleDomainException(DomainException exception) {
    return ResponseEntity.status(exception.getDomainCode().getStatus())
        .body(ApiResponse.<Void>builder()
            .code(exception.getDomainCode().getValue())
            .message(exception.getMessage())
            .build());
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ApiResponse<Void>> handleConstraintViolationException(ConstraintViolationException exception) {
    DomainCode domainCode = DomainCode.INVALID_KEY;
    try {
      ConstraintViolation<?> constraintViolation = exception.getConstraintViolations().stream().findFirst().get();
      domainCode = DomainCode.valueOf(constraintViolation.getMessage());
    } catch (IllegalArgumentException ignored) {
    }

    ApiResponse<Void> apiResponse = new ApiResponse<>();
    apiResponse.setCode(domainCode.getValue());
    apiResponse.setMessage(domainCode.getMessage());
    return ResponseEntity.badRequest().body(apiResponse);
  }

  @ExceptionHandler(MissingServletRequestParameterException.class)
  public ResponseEntity<ApiResponse<Void>> handleMissingServletRequestParameter(
      MissingServletRequestParameterException exception) {
    DomainCode domainCode= DomainCode.valueOf(PARAMETER_MESSAGES.getOrDefault(exception.getParameterName(), "INVALID_KEY"));

    ApiResponse<Void> apiResponse = new ApiResponse<>();
    apiResponse.setCode(domainCode.getValue());
    apiResponse.setMessage(domainCode.getMessage());
    return ResponseEntity.badRequest().body(apiResponse);
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException() {
    DomainCode domainCode = DomainCode.UNAUTHORIZED;
    return ResponseEntity.status(domainCode.getStatus())
        .body(ApiResponse.<Void>builder()
            .code(domainCode.getValue())
            .message(domainCode.getMessage())
            .build());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValidException(MethodArgumentNotValidException exception) {
    DomainCode domainCode = DomainCode.INVALID_KEY;
    Map<String, Object> attributes = null;

    try {
      String errorMsg = exception.getFieldError().getDefaultMessage();
      domainCode = DomainCode.valueOf(errorMsg);

      ConstraintViolation constraintViolation =
          exception.getAllErrors().stream().findFirst().get().unwrap(ConstraintViolation.class);

      attributes = constraintViolation.getConstraintDescriptor().getAttributes();
    } catch (IllegalArgumentException ignored) {
    }

    ApiResponse<Void> apiResponse = new ApiResponse<>();

    apiResponse.setCode(domainCode.getValue());
    apiResponse.setMessage(
        Objects.isNull(attributes) ? domainCode.getMessage() : mapAttribute(domainCode.getMessage(), attributes));

    return ResponseEntity.badRequest().body(apiResponse);
  }

  private String mapAttribute(String message, Map<String, Object> attributes) {
    String minValue = String.valueOf(attributes.get(MIN_ATTRIBUTE));

    return message.replace("{}", minValue);
  }

}

