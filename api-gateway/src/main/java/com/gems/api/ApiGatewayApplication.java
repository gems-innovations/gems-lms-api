package com.gems.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.gems.api.config.MicroserviceConfig;

@SpringBootApplication
@EnableConfigurationProperties(MicroserviceConfig.class)
public class ApiGatewayApplication {
  public static void main(String[] args) {
    SpringApplication.run(ApiGatewayApplication.class);
  }
}
