package com.gems.shared.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;

class FechasConZonaTest {
  // Igual que Boot: primero los módulos conocidos (JavaTimeModule) y luego los Module del contexto; el nuestro manda.
  private final ObjectMapper json = Jackson2ObjectMapperBuilder.json().modulesToInstall(new FechasConZona()).build();
  private record Con(LocalDateTime at) {}

  @Test
  void saleConElDesfaseDelServidorYEntraConOSinEl() throws Exception {
    LocalDateTime at = LocalDateTime.of(2026, 11, 4, 15, 30);
    String salida = json.writeValueAsString(new Con(at));
    String texto = json.readTree(salida).get("at").asText();
    assertEquals(at.atZone(ZoneId.systemDefault()).toInstant(), OffsetDateTime.parse(texto).toInstant());

    assertEquals(at, json.readValue(salida, Con.class).at());                              // ida y vuelta
    assertEquals(at, json.readValue("{\"at\":\"2026-11-04T15:30:00\"}", Con.class).at());  // sin desfase, como antes
    assertNull(json.readValue("{\"at\":null}", Con.class).at());
    assertThrows(Exception.class, () -> json.readValue("{\"at\":\"mañana\"}", Con.class));
  }
}
