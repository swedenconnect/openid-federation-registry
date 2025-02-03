/*
 * Copyright 2025 Sweden Connect
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

package se.swedenconnect.oidf.entity.registry.errorhandling;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import se.swedenconnect.oidf.entity.registry.validation.PropertyValidationFailException;

import java.util.List;
import java.util.Map;

import static se.swedenconnect.oidf.entity.registry.errorhandling.ErrorTypes.INVALID_PARAMETER;

/**
 * Error Handler ControllerAdvice.
 *
 * All error response are transformed according to: { "error":"server_error", "error_description":"Human understandable
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

  @ExceptionHandler(PropertyValidationFailException.class)
  public ResponseEntity<Object> handle(final PropertyValidationFailException e, final WebRequest request) {
    final ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(400), e.getMessage());
    problemDetail.setProperty("cause", List.of(
        Map.of("field", e.getFiledName(), "detail", e.getValidationFailMessage()))
    );
    problemDetail.setType(INVALID_PARAMETER);

    return this.handleExceptionInternal(e,
        problemDetail,
        new HttpHeaders(),
        HttpStatusCode.valueOf(problemDetail.getStatus()),
        request);
  }

  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
      final MethodArgumentNotValidException e,
      final HttpHeaders headers,
      final HttpStatusCode status,
      final WebRequest request) {

    final ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(400),
        "MethodArgumentNotValidException");
    problemDetail.setType(INVALID_PARAMETER);

    final List<Map<String, String>> detailProblem =
        e.getBindingResult().getFieldErrors().stream().map((FieldError error) -> {
          final String fieldName = error.getObjectName() + "." + error.getField();
          final String rejectedValue = error.getRejectedValue() == null ? "null" : error.getRejectedValue().toString();
          final String message = error.getDefaultMessage();
          return Map.of("filed", fieldName, "detail", message, "rejectedValue", rejectedValue);
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
      final Exception ex,
      final Object body,
      final HttpHeaders headers,
      final HttpStatusCode statusCode,
      final WebRequest request) {

    if (statusCode.is5xxServerError()) {
      log.error("Error serving request:'%s'".formatted(ex.getMessage()), ex);
    }
    return super.handleExceptionInternal(ex, body, headers, statusCode, request);
  }

  @Override
  protected ResponseEntity<Object> createResponseEntity(@Nullable final Object body, final HttpHeaders headers,
      final HttpStatusCode statusCode, final WebRequest request) {
    return new ResponseEntity<>(body, headers, statusCode);

  }

}
