package net.ethannet.media.hello;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import net.ethannet.media.api.model.HelloResponse;
import org.springframework.stereotype.Component;

/**
 * Builds the {@code GET /api/hello} greeting. Pure apart from the injected {@link Clock}, so it can
 * be tested without HTTP or Spring.
 */
@Component
public class HelloResponseBuilder {

  static final String MESSAGE = "Hello from the media API!";

  private final Clock clock;

  public HelloResponseBuilder(Clock clock) {
    this.clock = clock;
  }

  /** The greeting, timestamped with the clock's current instant in UTC. */
  public HelloResponse build() {
    return new HelloResponse(MESSAGE, OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC));
  }
}
