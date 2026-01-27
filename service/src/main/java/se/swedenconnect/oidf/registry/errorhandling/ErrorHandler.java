/*
 * Copyright 2026 Sweden Connect
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package se.swedenconnect.oidf.registry.errorhandling;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import se.swedenconnect.oidf.registry.validation.PropertyValidationFailException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static se.swedenconnect.oidf.registry.errorhandling.ErrorTypes.DATA_CONSTRAINT;
import static se.swedenconnect.oidf.registry.errorhandling.ErrorTypes.INVALID_PARAMETER;

/**
 * Error Handler ControllerAdvice.
 * <p>
 * All error responses are transformed according to: { "error":"server_error", "error_description":"Human understandable
 * description of the problem" }
 *
 * @author Per Fredrik Plars
 */
@Slf4j
@ControllerAdvice
public class ErrorHandler extends ResponseEntityExceptionHandler {

  /**
   * Mapping IllegalArgumentException to 400 BAD_REQUEST
   *
   * @param e Injected IllegalArgumentException
   * @param request WebRequest
   * @return Response object with error and error_description
   */
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Object> handle(final IllegalArgumentException e, final WebRequest request) {
    return this.handleExceptionInternal(e,
        ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(400), e.getMessage()),
        new HttpHeaders(),
        HttpStatusCode.valueOf(400),
        request);
  }

  /**
   * Handles {@code PropertyValidationFailException} types of exceptions and maps them to a response with an HTTP 400
   * Bad Request status. The response includes a problem detail structure with specific error information about the
   * validation failure.
   *
   * @param e the {@code PropertyValidationFailException} raised due to a validation failure
   * @param request the web request during which the exception was raised
   * @return a {@code ResponseEntity} object containing the problem detail with the error type, cause, and other
   *     information
   */
  @ExceptionHandler(PropertyValidationFailException.class)
  public ResponseEntity<Object> handle(final PropertyValidationFailException e, final WebRequest request) {
    final ProblemDetail problemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(400), e.getMessage());
    problemDetail.setProperty("cause", List.of(
        Map.of("field", e.getFiledName(),
            "detail", e.getValidationFailMessage(),
            "inputValue", Optional.ofNullable(e.getInputValue()).orElse("")))
    );
    problemDetail.setType(INVALID_PARAMETER.errorURI);

    return this.handleExceptionInternal(e,
        problemDetail,
        new HttpHeaders(),
        HttpStatusCode.valueOf(problemDetail.getStatus()),
        request);
  }

  /**
   * Handles exceptions of type {@link RegistryServerException} by constructing a {@link ProblemDetail} object
   * containing error information and returning it within a {@link ResponseEntity}.
   *
   * @param e the {@link RegistryServerException} that was thrown
   * @param request the {@link WebRequest} during which the exception occurred
   * @return a {@link ResponseEntity} containing a {@link ProblemDetail} object with details about the error and the
   *     appropriate HTTP status code
   */
  @ExceptionHandler(RegistryServerException.class)
  public ResponseEntity<Object> handle(final RegistryServerException e, final WebRequest request) {

    final ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(e.getHttpCode()),
        e.getMessage());

    problemDetail.setType(e.getErrorTypes().errorURI);

    return this.handleExceptionInternal(e,
        problemDetail,
        new HttpHeaders(),
        HttpStatusCode.valueOf(problemDetail.getStatus()),
        request);
  }

  /**
   * Handles {@code DataIntegrityViolationException} which typically occurs when a database constraint is violated
   * (e.g., trying to delete a parent entity that still has children).
   *
   * @param e the exception
   * @param request the {@link WebRequest} during which the exception occurred
   * @return a {@link ResponseEntity} with 400 Bad Request
   */
  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<Object> handle(final DataIntegrityViolationException e, final WebRequest request) {
    log.info("Data integrity violation exception:" + e.getMessage());
    final ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(400),
        "Dataconstraint violation error, consult serverlogs");
    problemDetail.setType(DATA_CONSTRAINT.errorURI);

    return this.handleExceptionInternal(e,
        problemDetail,
        new HttpHeaders(),
        HttpStatusCode.valueOf(problemDetail.getStatus()),
        request);
  }

  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
      final MethodArgumentNotValidException e,
      @Nonnull final HttpHeaders headers,
      @Nonnull final HttpStatusCode status,
      @Nonnull final WebRequest request) {

    final ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(400),
        "MethodArgumentNotValidException");
    problemDetail.setType(INVALID_PARAMETER.errorURI);

    final List<Map<String, String>> detailProblem =
        e.getBindingResult().getFieldErrors().stream().map((FieldError error) -> {
          final String fieldName = error.getObjectName() + "." + error.getField();
          final String rejectedValue = error.getRejectedValue() == null ? "null" : error.getRejectedValue().toString();
          final String message = error.getDefaultMessage() == null ? "" : error.getDefaultMessage();
          return Map.of("field", fieldName, "detail", message, "rejectedValue", rejectedValue);
        }).toList();
    problemDetail.setProperty("cause", detailProblem);

    return this.handleExceptionInternal(e,
        problemDetail,
        new HttpHeaders(),
        HttpStatusCode.valueOf(problemDetail.getStatus()),
        request);
  }

  @Override
  protected ResponseEntity<Object> handleExceptionInternal(
      @Nonnull final Exception ex,
      @Nullable final Object body,
      @Nonnull final HttpHeaders headers,
      @Nonnull final HttpStatusCode statusCode,
      @Nonnull final WebRequest request) {

    if (statusCode.is5xxServerError()) {
      log.error("Error serving request:'%s'".formatted(ex.getMessage()), ex);
    }
    return super.handleExceptionInternal(ex, body, headers, statusCode, request);
  }

}
