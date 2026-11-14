package net.ethannet.media.hello;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import net.ethannet.media.api.model.HelloResponse;
import org.junit.jupiter.api.Test;

class HelloResponseBuilderTest {

  private static final Instant NOW = Instant.parse("2026-11-14T12:34:56.789Z");

  @Test
  void returnsTheGreeting() {
    HelloResponse response = builderAt(Clock.fixed(NOW, ZoneOffset.UTC)).build();

    assertThat(response.getMessage()).isEqualTo("Hello from the media API!");
  }

  @Test
  void timestampsTheResponseWithTheClockTimeInUtc() {
    HelloResponse response = builderAt(Clock.fixed(NOW, ZoneId.of("America/New_York"))).build();

    assertThat(response.getTimestamp()).isEqualTo(OffsetDateTime.parse("2026-11-14T12:34:56.789Z"));
    assertThat(response.getTimestamp().getOffset()).isEqualTo(ZoneOffset.UTC);
  }

  private static HelloResponseBuilder builderAt(Clock clock) {
    return new HelloResponseBuilder(clock);
  }
}
