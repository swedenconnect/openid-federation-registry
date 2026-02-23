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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

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

    final ErrorHandler errorHandler = new ErrorHandler();

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

  record TestRecord(String message) {
  }
}