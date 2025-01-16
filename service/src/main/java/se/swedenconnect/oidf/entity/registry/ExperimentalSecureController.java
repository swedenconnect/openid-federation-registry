package se.swedenconnect.oidf.entity.registry;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/secure")
public class ExperimentalSecureController {

  @GetMapping("/policies")
  public String policiesRead() {
    return "Read UUID: " + this.generateUuid();
  }

  @PostMapping("/policies")
  public String policiesWrite() {
    return "Write UUID: " + this.generateUuid();
  }

  @GetMapping
  public String handleGetRequest() {
    return "GET response with UUID: " + this.generateUuid();
  }

  @PostMapping
  public String handlePostRequest() {
    return "POST response with UUID: " + this.generateUuid();
  }

  public String generateUuid() {
    return java.util.UUID.randomUUID().toString();
  }
}
