package net.ethannet.media;

import org.springframework.boot.SpringApplication;

public class TestMediaApiApplication {

  public static void main(String[] args) {
    SpringApplication.from(MediaApiApplication::main)
        .with(TestcontainersConfiguration.class)
        .run(args);
  }
}
