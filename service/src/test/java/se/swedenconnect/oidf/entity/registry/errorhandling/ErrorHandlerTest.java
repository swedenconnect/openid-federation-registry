package se.swedenconnect.oidf.entity.registry.errorhandling;

import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Testing Error handler
 *
 * @author Per Fredrik Plars
 */
class ErrorHandlerTest {


  record TestRecord(String message){

  }

  @Test
  void handleMethodArgumentNotValid() throws NoSuchMethodException {


    final Method method = TestRecord.class.getMethods()[0];
    MethodParameter methodParameter = new MethodParameter(method, 0);

    final BindingResult bindingResult = new BeanPropertyBindingResult(new TestRecord("TEST"), "exampleDTO");
    bindingResult.addError(new FieldError("exampleDTO", "field1", null, false,
        null, null, "must not be null"));
    bindingResult.addError(new FieldError("exampleDTO", "field2", "invalid_value",
        false, null, null, "must be a valid email"));

    final MethodArgumentNotValidException exception =
        new MethodArgumentNotValidException(methodParameter, bindingResult);


    final ErrorHandler errorHandler = new ErrorHandler();

    final ResponseEntity<Object> responseEntity =
        errorHandler.handleMethodArgumentNotValid(exception, null, HttpStatusCode.valueOf(400),null);

    assertEquals(400,responseEntity.getStatusCode().value());
    assertNotNull(responseEntity.getBody());

    final Map<String,String> error = (Map)responseEntity.getBody();

    assertEquals("bad_request",error.get("error"));
    assertEquals("exampleDTO.field1 -> null :must not be null|exampleDTO.field2 -> invalid_value :must be a valid email",error.get("error_description"));

  }
}