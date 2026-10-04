package com.gems.education.infrastructure.driven.files;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;

import java.net.URI;
import java.time.Duration;

@Configuration
@ConditionalOnProperty(name = "files.storage", havingValue = "s3")
public class S3FileStorageConfig {
  @Bean(destroyMethod = "close")
  S3AsyncClient s3AsyncClient(@Value("${files.s3.region}") String region,
                              @Value("${files.s3.endpoint:}") String endpoint,
                              @Value("${files.s3.path-style:false}") boolean pathStyle) {
    var builder = S3AsyncClient.builder()
      .region(Region.of(region))
      .credentialsProvider(DefaultCredentialsProvider.builder().build())
      .forcePathStyle(pathStyle)
      .httpClientBuilder(NettyNioAsyncHttpClient.builder()
        .connectionTimeout(Duration.ofSeconds(5))
        .readTimeout(Duration.ofSeconds(30)));
    if (StringUtils.hasText(endpoint)) builder.endpointOverride(URI.create(endpoint));
    return builder.build();
  }
}
