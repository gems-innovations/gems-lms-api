package com.gems.shared.web;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Los {@link LocalDateTime} son hora local del servidor sin zona; el navegador los leía como su propia hora local y en
 * Bogotá salían corridos 5 h (como pasó con las notificaciones, #34). Al salir llevan el desfase del servidor: con el
 * servidor en UTC, {@code 2026-11-04T15:00} sale como {@code 2026-11-04T15:00Z}.
 * Al entrar se acepta con o sin desfase: la web y los otros servicios devuelven lo que recibieron.
 *
 * <p>Es un {@code Module} de Spring: Boot lo registra en el ObjectMapper de cada servicio (también en {@code @WebFluxTest}).
 */
@Component
public class FechasConZona extends SimpleModule {

  public FechasConZona() {
    super("fechas-con-zona");
    addSerializer(LocalDateTime.class, new JsonSerializer<>() {
      @Override
      public void serialize(LocalDateTime v, JsonGenerator g, SerializerProvider p) throws IOException {
        g.writeString(v.atZone(ZoneId.systemDefault()).toOffsetDateTime().toString());
      }
    });
    addDeserializer(LocalDateTime.class, new JsonDeserializer<>() {
      @Override
      public LocalDateTime deserialize(JsonParser p, DeserializationContext c) throws IOException {
        String s = p.getValueAsString();
        if (s == null || s.isBlank()) return null;
        try {
          var t = DateTimeFormatter.ISO_DATE_TIME.parseBest(s.strip(), OffsetDateTime::from, LocalDateTime::from);
          return t instanceof OffsetDateTime o ? o.atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime() : (LocalDateTime) t;
        } catch (java.time.format.DateTimeParseException e) {
          return (LocalDateTime) c.handleWeirdStringValue(LocalDateTime.class, s, "Fecha no válida");
        }
      }
    });
  }
}
