package net.ethannet.photos.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@code app.cors.*}: browser origins allowed to call {@code /api/**}.
 *
 * @param allowedOrigins exact origins such as {@code http://localhost:3000}; set with the
 *     comma-separated {@code CORS_ALLOWED_ORIGINS} env var
 */
@ConfigurationProperties("app.cors")
public record CorsProperties(List<String> allowedOrigins) {

  public CorsProperties {
    allowedOrigins = allowedOrigins == null ? List.of() : List.copyOf(allowedOrigins);
  }
}
