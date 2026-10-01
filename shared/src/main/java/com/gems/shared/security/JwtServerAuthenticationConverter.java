package com.gems.shared.security;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Extracts the raw JWT from the Authorization header so it can be handed to
 * {@link JwtReactiveAuthenticationManager}. The token itself is not validated here.
 */
public class JwtServerAuthenticationConverter implements ServerAuthenticationConverter {

  @Override
  public Mono<Authentication> convert(ServerWebExchange exchange) {
    List<String> headers = exchange.getRequest().getHeaders().get(AuthConstants.AUTHORIZATION_HEADER);
    if (headers == null || headers.isEmpty()) {
      return Mono.empty();
    }

    String header = headers.getFirst();
    if (header == null || !header.startsWith(AuthConstants.BEARER_PREFIX)) {
      return Mono.empty();
    }

    String token = header.substring(AuthConstants.BEARER_PREFIX.length());
    return Mono.just(new UsernamePasswordAuthenticationToken(token, token));
  }
}
