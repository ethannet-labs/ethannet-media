package net.ethannet.photos.hello;

import net.ethannet.photos.api.HelloApi;
import net.ethannet.photos.api.model.HelloResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code GET /api/hello}. The route and response type come from the generated {@link HelloApi};
 * this class only delegates to {@link HelloResponseBuilder}.
 */
@RestController
public class HelloController implements HelloApi {

  private final HelloResponseBuilder builder;

  public HelloController(HelloResponseBuilder builder) {
    this.builder = builder;
  }

  @Override
  public ResponseEntity<HelloResponse> getHello() {
    return ResponseEntity.ok(builder.build());
  }
}
