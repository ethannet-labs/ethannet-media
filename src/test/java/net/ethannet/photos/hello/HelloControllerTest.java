package net.ethannet.photos.hello;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import net.ethannet.photos.api.model.HelloResponse;
import net.ethannet.photos.config.CorsConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
    controllers = HelloController.class,
    properties = "app.cors.allowed-origins=http://ui.test")
@Import(CorsConfig.class)
class HelloControllerTest {

  @Autowired private MockMvc mvc;

  @MockitoBean private HelloResponseBuilder builder;

  @Test
  void returnsTheBuiltGreetingAsJson() throws Exception {
    given(builder.build())
        .willReturn(
            new HelloResponse("Hi there", OffsetDateTime.parse("2026-11-14T12:34:56.789Z")));

    mvc.perform(get("/api/hello"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.message").value("Hi there"))
        .andExpect(jsonPath("$.timestamp").value("2026-11-14T12:34:56.789Z"))
        .andExpect(jsonPath("$.length()").value(2));
  }

  @Test
  void allowsTheConfiguredUiOrigin() throws Exception {
    mvc.perform(
            options("/api/hello")
                .header("Origin", "http://ui.test")
                .header("Access-Control-Request-Method", "GET"))
        .andExpect(status().isOk())
        .andExpect(header().string("Access-Control-Allow-Origin", "http://ui.test"));
  }

  @Test
  void rejectsOtherOrigins() throws Exception {
    mvc.perform(
            options("/api/hello")
                .header("Origin", "http://evil.test")
                .header("Access-Control-Request-Method", "GET"))
        .andExpect(status().isForbidden());
  }
}
