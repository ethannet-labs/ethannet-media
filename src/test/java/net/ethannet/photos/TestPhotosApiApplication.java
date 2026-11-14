package net.ethannet.photos;

import org.springframework.boot.SpringApplication;

public class TestPhotosApiApplication {

  public static void main(String[] args) {
    SpringApplication.from(PhotosApiApplication::main)
        .with(TestcontainersConfiguration.class)
        .run(args);
  }
}
