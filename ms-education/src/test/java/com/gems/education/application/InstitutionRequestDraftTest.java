package com.gems.education.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.TextNode;
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
  void aceptaLosMundosNuevosDelCreador() {
    // scenery.js WORLDS: biblioteca, observatorio y aula se aceptan tal cual; uno desconocido sigue sin draft.
    for (String w : new String[] {"biblioteca", "observatorio", "aula"}) {
      String s = VALID.replace("taller-londres", w);
      assertEquals(s, InstitutionRequestDraft.normalize(s).orElseThrow(), w);
    }
    assertTrue(InstitutionRequestDraft.normalize(VALID.replace("taller-londres", "planetario")).isEmpty());
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
    // An unknown starting point is not forged data, just «sorpresa» (as compartir.js does).
    assertEquals(VALID.replace("grabador", "sorpresa"), InstitutionRequestDraft.normalize(VALID.replace("grabador", "<script>")).orElseThrow());
    assertTrue(InstitutionRequestDraft.normalize(VALID.replace("\"grabador\"", "7")).isPresent());
    assertTrue(InstitutionRequestDraft.normalize(VALID.replace("pluma", "espada")).isEmpty());
    assertTrue(InstitutionRequestDraft.normalize(VALID.replace("taller-londres", "marte")).isEmpty());
    assertTrue(InstitutionRequestDraft.normalize(VALID.replace("\"corto\"", "\"mohicano\"")).isEmpty());
    assertTrue(InstitutionRequestDraft.normalize(VALID.replace("#c99a76", "red")).isEmpty());
    assertTrue(InstitutionRequestDraft.normalize(VALID.replace("#c99a76", "#c99a76;background:url(x)")).isEmpty());
    // A null colour means "absent" (rig.js validarLook lets it through): it is dropped from the canonical text.
    assertEquals(VALID.replace("\"skin\":\"#c99a76\",", ""), InstitutionRequestDraft.normalize(VALID.replace("\"#c99a76\"", "null")).orElseThrow());
    assertTrue(InstitutionRequestDraft.normalize(VALID.replace("\"corto\"", "null")).isEmpty(), "null piece");
    assertTrue(InstitutionRequestDraft.normalize(VALID.replace("\"glasses\":true", "\"glasses\":null")).isEmpty(), "null glasses");
    assertTrue(InstitutionRequestDraft.normalize(VALID.replace("\"glasses\":true", "\"glasses\":\"true\"")).isEmpty());
    assertTrue(InstitutionRequestDraft.normalize(VALID.replace("\"skin\"", "\"onload\"")).isEmpty());
    assertTrue(InstitutionRequestDraft.normalize(VALID.replace("\"l\":{", "\"l\":{\"skin\":\"#000000\",")).isEmpty(), "duplicate key");
    assertTrue(InstitutionRequestDraft.normalize(VALID.replace("\"i\":\"pluma\"", "\"i\":7")).isEmpty());
    assertTrue(InstitutionRequestDraft.normalize(VALID.replace("\"i\":\"pluma\",", "")
      .replace("\"w\":\"taller-londres\"", "\"w\":null")).isEmpty());
  }

  /** A well-formed, valid draft padded with whitespace past 2048 bytes (the limit is on the text received). */
  static String huge() {
    String padded = VALID.replace("\"v\":1,", "\"v\":1," + " ".repeat(2048));
    assertTrue(padded.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 2048);
    return padded;
  }

  @Test
  void rechazaLoEnorme() {
    assertTrue(InstitutionRequestDraft.normalize(huge()).isEmpty());
    // Just under the limit, the same content is fine (so it is the size that rejects it, not the content).
    String justFits = VALID.replace("\"v\":1,", "\"v\":1," + " ".repeat(2048 - VALID.length()));
    assertEquals(2048, justFits.getBytes(java.nio.charset.StandardCharsets.UTF_8).length);
    assertEquals(VALID, InstitutionRequestDraft.normalize(justFits).orElseThrow());
  }

  @Test
  void sacaElBloqueDelMensajeYDejaElMensajeIntacto() throws Exception {
    String mensaje = "Hola, queremos un espacio.\n\n" + InstitutionRequestDraft.BLOCK_START + "\n" + VALID + "\n" + InstitutionRequestDraft.BLOCK_END;
    assertEquals(VALID, InstitutionRequestDraft.resolve(null, mensaje).orElseThrow());
    // A dedicated field wins over the block (as text or as an object); an invalid field falls back to the block.
    String otro = VALID.replace("grabador", "escriba");
    assertEquals(otro, InstitutionRequestDraft.resolve(new TextNode(otro), mensaje).orElseThrow());
    assertEquals(otro, InstitutionRequestDraft.resolve(new ObjectMapper().readTree(otro), mensaje).orElseThrow());
    assertEquals(VALID, InstitutionRequestDraft.resolve(new TextNode("{bad"), mensaje).orElseThrow());
    assertEquals(VALID, InstitutionRequestDraft.resolve(new ObjectMapper().readTree("[1]"), mensaje).orElseThrow());
    assertEquals(VALID, InstitutionRequestDraft.resolve(new ObjectMapper().readTree("123"), mensaje).orElseThrow());
    // Block without its end, or with forged content, yields nothing.
    assertTrue(InstitutionRequestDraft.resolve(null, InstitutionRequestDraft.BLOCK_START + VALID).isEmpty());
    assertTrue(InstitutionRequestDraft.resolve(null, InstitutionRequestDraft.BLOCK_START + "{\"v\":1}" + InstitutionRequestDraft.BLOCK_END).isEmpty());
  }

  @Test
  void sinBorradorNoHayNada() throws Exception {
    assertTrue(InstitutionRequestDraft.resolve(null, null).isEmpty());
    assertTrue(InstitutionRequestDraft.resolve(NullNode.getInstance(), "Solo un mensaje normal").isEmpty());
    assertTrue(InstitutionRequestDraft.resolve(new TextNode(""), "Solo un mensaje normal").isEmpty());
    assertTrue(InstitutionRequestDraft.resolve(new ObjectMapper().readTree("{}"), null).isEmpty());
  }
}
