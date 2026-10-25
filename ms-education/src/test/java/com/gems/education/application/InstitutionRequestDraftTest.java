package com.gems.education.application;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class InstitutionRequestDraftTest {

  static final String VALID = "{\"v\":1,\"borradorId\":\"mh3k2a9f-0abc123\",\"b\":\"grabador\","
    + "\"l\":{\"skin\":\"#c99a76\",\"hairStyle\":\"corto\",\"beard\":\"none\",\"glasses\":true,\"outfit\":\"levita\"},"
    + "\"i\":\"pluma\",\"w\":\"taller-londres\"}";

  @Test
  void aceptaUnBorradorValidoYLoDevuelveCanonico() {
    Optional<String> r = InstitutionRequestDraft.normalize(VALID);
    assertTrue(r.isPresent());
    assertEquals(VALID, r.get());
    // Different key order and spaces produce the same canonical text.
    String desordenado = "{ \"w\":\"taller-londres\", \"i\":\"pluma\", \"l\":{\"skin\":\"#c99a76\",\"hairStyle\":\"corto\",\"beard\":\"none\",\"glasses\":true,\"outfit\":\"levita\"},"
      + "\"b\":\"grabador\",\"borradorId\":\"mh3k2a9f-0abc123\",\"v\":1 }";
    assertEquals(VALID, InstitutionRequestDraft.normalize(desordenado).orElseThrow());
  }

  @Test
  void aceptaSinObjetoYConSorpresa() {
    String s = "{\"v\":1,\"borradorId\":\"mh3k2a9f-0abc123\",\"b\":\"sorpresa\",\"l\":{},\"i\":null,\"w\":\"foro-roma\"}";
    assertEquals(s, InstitutionRequestDraft.normalize(s).orElseThrow());
    String sinI = "{\"v\":1,\"borradorId\":\"mh3k2a9f-0abc123\",\"b\":\"sorpresa\",\"l\":{},\"w\":\"foro-roma\"}";
    assertEquals(s, InstitutionRequestDraft.normalize(sinI).orElseThrow());
  }

  @Test
  void rechazaLoMalFormadoOVacio() {
    assertTrue(InstitutionRequestDraft.normalize(null).isEmpty());
    assertTrue(InstitutionRequestDraft.normalize("  ").isEmpty());
    assertTrue(InstitutionRequestDraft.normalize("{v:1").isEmpty());
    assertTrue(InstitutionRequestDraft.normalize("[1,2]").isEmpty());
    assertTrue(InstitutionRequestDraft.normalize("\"texto\"").isEmpty());
  }

  @Test
  void rechazaLoFalsificado() {
    // Unknown top-level key, wrong version, forged id, off-catalog pieces, bad colours, nulls, html.
    assertTrue(InstitutionRequestDraft.normalize(VALID.replace("\"v\":1", "\"v\":1,\"admin\":true")).isEmpty());
    assertTrue(InstitutionRequestDraft.normalize(VALID.replace("\"v\":1", "\"v\":2")).isEmpty());
    assertTrue(InstitutionRequestDraft.normalize(VALID.replace("\"v\":1", "\"v\":\"1\"")).isEmpty());
    assertTrue(InstitutionRequestDraft.normalize(VALID.replace("mh3k2a9f-0abc123", "../etc/passwd")).isEmpty());
    assertTrue(InstitutionRequestDraft.normalize(VALID.replace("mh3k2a9f-0abc123", "ABCDEF-ABCDEFG")).isEmpty());
    assertTrue(InstitutionRequestDraft.normalize(VALID.replace("mh3k2a9f-0abc123", "abc")).isEmpty());
    assertTrue(InstitutionRequestDraft.normalize(VALID.replace("grabador", "<script>")).isEmpty());
    assertTrue(InstitutionRequestDraft.normalize(VALID.replace("pluma", "espada")).isEmpty());
    assertTrue(InstitutionRequestDraft.normalize(VALID.replace("taller-londres", "marte")).isEmpty());
    assertTrue(InstitutionRequestDraft.normalize(VALID.replace("\"corto\"", "\"mohicano\"")).isEmpty());
    assertTrue(InstitutionRequestDraft.normalize(VALID.replace("#c99a76", "red")).isEmpty());
    assertTrue(InstitutionRequestDraft.normalize(VALID.replace("#c99a76", "#c99a76;background:url(x)")).isEmpty());
    assertTrue(InstitutionRequestDraft.normalize(VALID.replace("\"#c99a76\"", "null")).isEmpty());
    assertTrue(InstitutionRequestDraft.normalize(VALID.replace("\"glasses\":true", "\"glasses\":\"true\"")).isEmpty());
    assertTrue(InstitutionRequestDraft.normalize(VALID.replace("\"skin\"", "\"onload\"")).isEmpty());
    assertTrue(InstitutionRequestDraft.normalize(VALID.replace("\"l\":{", "\"l\":{\"skin\":\"#000000\",")).isEmpty(), "duplicate key");
    assertTrue(InstitutionRequestDraft.normalize(VALID.replace("\"i\":\"pluma\"", "\"i\":7")).isEmpty());
    assertTrue(InstitutionRequestDraft.normalize(VALID.replace("\"i\":\"pluma\",", "")
      .replace("\"w\":\"taller-londres\"", "\"w\":null")).isEmpty());
  }

  @Test
  void rechazaLoEnorme() {
    String relleno = "\"glow\":\"#" + "a".repeat(6) + "\",";
    StringBuilder grande = new StringBuilder("{\"v\":1,\"borradorId\":\"mh3k2a9f-0abc123\",\"b\":\"grabador\",\"l\":{");
    while (grande.length() < 2100) grande.append(relleno);
    grande.append("\"skin\":\"#000000\"},\"w\":\"foro-roma\"}");
    assertTrue(InstitutionRequestDraft.normalize(grande.toString()).isEmpty());
    // Padded with whitespace it is also over the limit even if the content would be fine.
    assertTrue(InstitutionRequestDraft.normalize(VALID + " ".repeat(2048)).isEmpty());
  }

  @Test
  void sacaElBloqueDelMensajeYDejaElMensajeIntacto() {
    String mensaje = "Hola, queremos un espacio.\n\n" + InstitutionRequestDraft.BLOCK_START + "\n" + VALID + "\n" + InstitutionRequestDraft.BLOCK_END;
    assertEquals(VALID, InstitutionRequestDraft.resolve(null, mensaje).orElseThrow());
    // A dedicated field wins over the block; an invalid field falls back to the block.
    String otro = VALID.replace("grabador", "escriba");
    assertEquals(otro, InstitutionRequestDraft.resolve(otro, mensaje).orElseThrow());
    assertEquals(VALID, InstitutionRequestDraft.resolve("{bad", mensaje).orElseThrow());
    // Block without its end, or with forged content, yields nothing.
    assertTrue(InstitutionRequestDraft.resolve(null, InstitutionRequestDraft.BLOCK_START + VALID).isEmpty());
    assertTrue(InstitutionRequestDraft.resolve(null, InstitutionRequestDraft.BLOCK_START + "{\"v\":1}" + InstitutionRequestDraft.BLOCK_END).isEmpty());
  }

  @Test
  void sinBorradorNoHayNada() {
    assertTrue(InstitutionRequestDraft.resolve(null, null).isEmpty());
    assertTrue(InstitutionRequestDraft.resolve("", "Solo un mensaje normal").isEmpty());
  }
}
