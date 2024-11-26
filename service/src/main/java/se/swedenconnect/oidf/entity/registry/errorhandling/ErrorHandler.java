/*
 * Copyright 2024 Sweden Connect
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
 * limitations under the License.
 *
 */

package se.swedenconnect.oidf.entity.registry.errorhandling;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Map;
import java.util.Optional;

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
   * @param e Injected IllegalArgumentException
   * @param request WebRequest
   * @return Response object with error and error_description
   */
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Object> handle(IllegalArgumentException e,final WebRequest request){
    return handleExceptionInternal(e,
        ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(400),e.getMessage()),
        new HttpHeaders(),
        HttpStatusCode.valueOf(400),
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
  protected ResponseEntity<Object> createResponseEntity(@Nullable Object body, HttpHeaders headers,
      HttpStatusCode statusCode, WebRequest request) {
    String error = HttpStatus.valueOf(statusCode.value()).getReasonPhrase();
    String errorDescription = "Unknown server error";
    if (body instanceof ProblemDetail) {
      error = Optional.ofNullable(((ProblemDetail) body).getTitle())
          .orElse("server_error").toLowerCase().replace(' ', '_');
      errorDescription = ((ProblemDetail) body).getDetail();
    }
    return new ResponseEntity<>(
        Map.of("error", error, "error_description", errorDescription), headers, statusCode);
  }

}
