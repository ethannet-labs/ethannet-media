package net.ethannet.photos;

import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * A throwaway Postgres for tests and {@code TestPhotosApiApplication}. Same major as compose.yaml.
 *
 * <p>Its port is published on 127.0.0.1 only (on a random free port), so the database is never
 * reachable from the network.
 */
@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

  private static final int POSTGRES_PORT = 5432;

  @Bean
  @ServiceConnection
  PostgreSQLContainer postgresContainer() {
    PostgreSQLContainer postgres = new PostgreSQLContainer(DockerImageName.parse("postgres:18"));
    postgres.withCreateContainerCmdModifier(cmd ->
        cmd.getHostConfig()
            .withPortBindings(
                new PortBinding(
                    Ports.Binding.bindIp("127.0.0.1"), ExposedPort.tcp(POSTGRES_PORT))));
    return postgres;
  }
}
