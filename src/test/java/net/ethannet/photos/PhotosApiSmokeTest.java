package net.ethannet.photos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

/** Boots the whole app against a real Postgres (Testcontainers). */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class PhotosApiSmokeTest {

  @Autowired private MockMvc mvc;

  @Autowired private Flyway flyway;

  @Test
  void runsFlywayMigrationsWithoutErrors() {
    assertThat(flyway.validateWithResult().validationSuccessful).isTrue();
    assertThat(flyway.info().pending()).isEmpty();
  }

  @Test
  void reportsHealthyIncludingTheDatabase() throws Exception {
    mvc.perform(get("/actuator/health"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"));
  }

  @Test
  void servesTheHelloEndpoint() throws Exception {
    mvc.perform(get("/api/hello"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("Hello from the photos API!"));
  }
}
