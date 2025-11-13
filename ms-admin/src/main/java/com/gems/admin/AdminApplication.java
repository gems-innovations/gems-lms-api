package com.gems.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.gems.admin", "com.gems.shared"})
public class AdminApplication {
  public static void main(String[] args) {
    SpringApplication.run(AdminApplication.class, args);
  }
}
