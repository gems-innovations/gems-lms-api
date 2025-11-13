package com.gems.education;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.gems.education", "com.gems.shared"})
public class EducationApplication {
  public static void main(String[] args) {
    SpringApplication.run(EducationApplication.class, args);
  }
}
