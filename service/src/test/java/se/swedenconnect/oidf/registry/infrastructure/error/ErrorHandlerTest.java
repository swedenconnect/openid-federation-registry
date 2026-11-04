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

package se.swedenconnect.oidf.registry.infrastructure.error;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataAccessException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;

import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import io.micrometer.tracing.Tracer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit test for the {@link ErrorHandler} class.
 *
 * @author Per Fredrik Plars
 */
class ErrorHandlerTest {

  @Test
  @DisplayName("Tests handling of MethodArgumentNotValidException")
  void handleMethodArgumentNotValid() {

    final Method method = TestRecord.class.getMethods()[0];
    final MethodParameter methodParameter = new MethodParameter(method, 0);

    final BindingResult bindingResult = new BeanPropertyBindingResult(new TestRecord("TEST"), "exampleDTO");
    bindingResult.addError(new FieldError("exampleDTO", "field1", null, false,
        null, null, "must not be null"));
    bindingResult.addError(new FieldError("exampleDTO", "field2", "invalid_value",
        false, null, null, "must be a valid email"));

    final MethodArgumentNotValidException exception =
        new MethodArgumentNotValidException(methodParameter, bindingResult);

    final Tracer tracer = mock(Tracer.class);
    when(tracer.currentSpan()).thenReturn(null);
    final ErrorHandler errorHandler = new ErrorHandler(Optional.of(tracer));

    final ResponseEntity<Object> responseEntity = errorHandler.handleMethodArgumentNotValid(
        exception, new HttpHeaders(), HttpStatusCode.valueOf(400), mock(WebRequest.class));

    assertThat(responseEntity).isNotNull();
    assertThat(responseEntity.getStatusCode().value()).isEqualTo(400);
    assertThat(responseEntity.getBody()).isNotNull();

    final ProblemDetail error = (ProblemDetail) responseEntity.getBody();

    assertThat(error.getProperties()).isNotEmpty();
    assertThat(error.getProperties().get("cause")).isInstanceOf(List.class);
    assertThat(((List<?>)error.getProperties().get("cause")).size()).isEqualTo(2);
  }

  @Test
  @DisplayName("MariaDB error 1020 from a concurrent write is answered with 409")
  void recordChangedSinceLastReadIsConflict() {
    final JpaSystemException exception = new JpaSystemException(new RuntimeException("could not execute statement",
        new SQLException("Record has changed since last read in table 'entities'", "HY000", 1020)));

    final ResponseEntity<Object> response = this.errorHandler().handle(exception, mock(WebRequest.class));

    assertThat(response.getStatusCode().value()).isEqualTo(409);
    assertThat(((ProblemDetail) response.getBody()).getDetail())
        .isEqualTo("The record was changed by another request, try again");
  }

  @Test
  @DisplayName("Lock conflicts that Spring translates are answered with 409")
  void concurrencyFailureIsConflict() {
    for (final DataAccessException exception : List.of(
        new CannotAcquireLockException("Lock wait timeout exceeded"),
        new ObjectOptimisticLockingFailureException("FederationEntity", "id"))) {
      assertThat(this.errorHandler().handle(exception, mock(WebRequest.class)).getStatusCode().value())
          .isEqualTo(409);
    }
  }

  @Test
  @DisplayName("Any other data access error is thrown on and handled as before")
  void otherDataAccessErrorIsThrownOn() {
    final JpaSystemException exception = new JpaSystemException(new RuntimeException("could not execute statement",
        new SQLException("Table 'entities' doesn't exist", "42S02", 1146)));

    assertThatThrownBy(() -> this.errorHandler().handle(exception, mock(WebRequest.class))).isSameAs(exception);
  }

  @Test
  @DisplayName("Through Spring MVC a write conflict gives 409 and any other data access error is not handled")
  void dataAccessErrorsThroughMvc() throws Exception {
    final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new FailingController())
        .setControllerAdvice(this.errorHandler())
        .build();

    mockMvc.perform(get("/fail/1020")).andExpect(status().isConflict());
    assertThatThrownBy(() -> mockMvc.perform(get("/fail/1146")))
        .hasRootCauseInstanceOf(SQLException.class)
        .rootCause().hasMessage("Table 'entities' doesn't exist");
  }

  @RestController
  static class FailingController {
    @GetMapping("/fail/{errorCode}")
    String fail(@PathVariable("errorCode") final int errorCode) {
      final String message = errorCode == 1020
          ? "Record has changed since last read in table 'entities'"
          : "Table 'entities' doesn't exist";
      throw new JpaSystemException(new RuntimeException("could not execute statement",
          new SQLException(message, "HY000", errorCode)));
    }
  }

  private ErrorHandler errorHandler() {
    final Tracer tracer = mock(Tracer.class);
    when(tracer.currentSpan()).thenReturn(null);
    return new ErrorHandler(Optional.of(tracer));
  }

  record TestRecord(String message) {
  }
}