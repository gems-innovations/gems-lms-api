package com.gems.education.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

public class TapsPayloadValidadorTest {
  private static final ObjectMapper JSON = new ObjectMapper();
  private final TapsPayloadValidador validador = TapsPayloadValidador.delClasspath();

  /** Un Tap válido mínimo, como objeto editable (lo usan también las pruebas del caso de uso y del controller). */
  public static ObjectNode tapValido() {
    ObjectNode t = JSON.createObjectNode();
    t.put("v", 1).put("theme", "epoca").put("title", "Roma antigua").put("kicker", "Historia");
    t.set("scene", JSON.createObjectNode().put("world", "foro-roma").put("host", "senador").put("item", "pergamino").put("shot", "izquierda"));
    ArrayNode qs = JSON.createArrayNode();
    for (int i = 1; i <= 5; i++) {
      ObjectNode q = JSON.createObjectNode().put("q", "¿Pregunta número " + i + "?").put("answer", 1).put("why", "Porque sí, en el año 1789-1799.");
      q.set("options", JSON.createArrayNode().add("Una").add("Dos").add("Tres").add("Cuatro"));
      qs.add(q);
    }
    t.set("questions", qs);
    ArrayNode rs = JSON.createArrayNode();
    rs.add(JSON.createObjectNode().put("min", 0).put("title", "Aprendiz").put("line", "Sigue intentando"));
    rs.add(JSON.createObjectNode().put("min", 3).put("title", "Cronista").put("line", "Casi perfecto"));
    rs.add(JSON.createObjectNode().put("min", 5).put("title", "Senador").put("line", "Impecable"));
    t.set("results", rs);
    return t;
  }

  static String json(JsonNode n) {
    return n.toString();
  }

  private void pasa(JsonNode tap) {
    assertDoesNotThrow(() -> validador.validar(tap));
  }

  private void rechaza(JsonNode tap, String motivo) {
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> validador.validar(tap), motivo);
    assertFalse(e.getMessage().isBlank());
  }

  @Test
  void unTapValidoSaleCanonicoConIdsDePreguntaYTopicCreador() throws Exception {
    TapsPayloadValidador.Tap out = validador.validar(tapValido());
    JsonNode n = JSON.readTree(out.json());
    assertEquals("Roma antigua", out.titulo());
    assertEquals("creador", n.get("topic").asText());
    assertEquals("epoca", n.get("theme").asText());
    assertEquals("p1", n.get("questions").get(0).get("id").asText());
    assertEquals("p5", n.get("questions").get(4).get("id").asText());
    // El canónico guardado vuelve a pasar por el validador al publicar y sale idéntico (el id se ignora y se regenera).
    assertEquals(out.json(), validador.validar(out.json()).json());
    assertFalse(n.get("questions").get(0).has("tipo"), "clásica no lleva tipo");
    assertEquals("izquierda", n.get("scene").get("shot").asText());
    // Misma salida desde texto.
    assertEquals(out.json(), validador.validar(json(tapValido())).json());
  }

  @Test
  void recortaYColapsaEspaciosYNormalizaUnicode() throws Exception {
    ObjectNode t = tapValido();
    t.put("title", "  Roma \t\u00A0 antigua  ");
    // «é» descompuesta (e + acento) pasa a NFC.
    t.put("kicker", "Histo\u0301rico");
    JsonNode n = JSON.readTree(validador.validar(t).json());
    assertEquals("Roma antigua", n.get("title").asText());
    assertEquals("Hist\u00F3rico", n.get("kicker").asText());
  }

  @Test
  void rechazaClavesDesconocidasEnCualquierNivel() {
    ObjectNode t = tapValido();
    t.put("hook", true);
    rechaza(t, "clave extra en la raíz");
    t = tapValido();
    ((ObjectNode) t.get("scene")).put("enter", "camina");
    rechaza(t, "clave extra en la escena");
    t = tapValido();
    ((ObjectNode) t.get("questions").get(0)).put("more", "x");
    rechaza(t, "clave extra en la pregunta");
    t = tapValido();
    ((ObjectNode) t.get("results").get(0)).put("lines", "x");
    rechaza(t, "clave extra en el resultado");
  }

  @Test
  void rechazaVersionTemaYTopicDistintos() {
    ObjectNode t = tapValido();
    t.put("v", 2);
    rechaza(t, "v");
    t = tapValido();
    t.put("theme", "minecraft");
    rechaza(t, "tema");
    t = tapValido();
    t.put("topic", "historia");
    rechaza(t, "topic");
  }

  @Test
  void laEscenaSoloAdmiteElCatalogoEpoca() {
    ObjectNode t = tapValido();
    ((ObjectNode) t.get("scene")).put("world", "cueva");
    rechaza(t, "mundo");
    t = tapValido();
    ((ObjectNode) t.get("scene")).put("host", "sofi");
    rechaza(t, "anfitri\u00F3n");
    t = tapValido();
    ((ObjectNode) t.get("scene")).put("item", "pico");
    rechaza(t, "objeto");
    t = tapValido();
    ((ObjectNode) t.get("scene")).put("shot", "orbita");
    rechaza(t, "encuadre");
    t = tapValido();
    ((ObjectNode) t.get("scene")).remove("item");
    ((ObjectNode) t.get("scene")).remove("shot");
    pasa(t);
  }

  @Test
  void respetaLosLimitesDelBanco() {
    ObjectNode t = tapValido();
    t.put("title", "x".repeat(61));
    rechaza(t, "título 61");
    t = tapValido();
    t.put("title", "x".repeat(60));
    pasa(t);
    ObjectNode t2 = tapValido();
    t2.put("kicker", "x".repeat(31));
    rechaza(t2, "kicker");
    ObjectNode t3 = tapValido();
    ((ObjectNode) t3.get("questions").get(2)).put("q", "x".repeat(121));
    rechaza(t3, "pregunta");
    ObjectNode t4 = tapValido();
    ((ArrayNode) t4.get("questions").get(0).get("options")).set(0, "x".repeat(23));
    rechaza(t4, "opci\u00F3n");
    ObjectNode t5 = tapValido();
    ((ObjectNode) t5.get("questions").get(0)).put("why", "x".repeat(91));
    rechaza(t5, "explicaci\u00F3n");
    ObjectNode t6 = tapValido();
    ((ObjectNode) t6.get("results").get(0)).put("title", "x".repeat(41));
    rechaza(t6, "título de resultado");
    ObjectNode t7 = tapValido();
    ((ObjectNode) t7.get("results").get(0)).put("line", "x".repeat(91));
    rechaza(t7, "línea de resultado");
    ObjectNode t8 = tapValido();
    t8.put("title", "");
    rechaza(t8, "título vacío");
  }

  @Test
  void losLargosSeMidenEnPuntosDeCodigo() {
    ObjectNode t = tapValido();
    t.put("kicker", "😀".repeat(30));
    assertDoesNotThrow(() -> validador.validar(t));
  }

  @Test
  void exactamenteCincoPreguntasYDeTresACincoResultados() {
    ObjectNode t = tapValido();
    ((ArrayNode) t.get("questions")).remove(4);
    rechaza(t, "4 preguntas");
    t = tapValido();
    ((ArrayNode) t.get("questions")).add(t.get("questions").get(0));
    rechaza(t, "6 preguntas");
    t = tapValido();
    ((ArrayNode) t.get("results")).remove(2);
    rechaza(t, "2 resultados");
    t = tapValido();
    ArrayNode rs = (ArrayNode) t.get("results");
    rs.insert(1, JSON.createObjectNode().put("min", 1).put("title", "A").put("line", "a"));
    rs.insert(2, JSON.createObjectNode().put("min", 2).put("title", "B").put("line", "b"));
    rs.insert(3, JSON.createObjectNode().put("min", 4).put("title", "C").put("line", "c"));
    rechaza(t, "6 resultados");
  }

  @Test
  void losResultadosEmpiezanEnCeroYSubenSinRepetir() {
    ObjectNode t = tapValido();
    ((ObjectNode) t.get("results").get(0)).put("min", 1);
    rechaza(t, "primer min 1");
    t = tapValido();
    ((ObjectNode) t.get("results").get(1)).put("min", 0);
    rechaza(t, "repetido");
    t = tapValido();
    ((ObjectNode) t.get("results").get(2)).put("min", 6);
    rechaza(t, "más de 5");
  }

  @Test
  void opcionesSegunFormatoYRespuestaDentroDelRango() throws Exception {
    ObjectNode t = tapValido();
    ((ObjectNode) t.get("questions").get(0)).put("answer", 4);
    rechaza(t, "answer fuera");
    t = tapValido();
    ((ObjectNode) t.get("questions").get(0)).put("answer", -1);
    rechaza(t, "answer negativo");
    t = tapValido();
    ((ObjectNode) t.get("questions").get(0)).put("answer", "1");
    rechaza(t, "answer texto");
    // duo: 2 opciones
    t = tapValido();
    ObjectNode duo = (ObjectNode) t.get("questions").get(1);
    duo.put("tipo", "duo");
    rechaza(t, "duo con 4");
    duo.set("options", JSON.createArrayNode().add("Sí").add("No"));
    duo.put("answer", 1);
    JsonNode n = JSON.readTree(validador.validar(t).json());
    assertEquals("duo", n.get("questions").get(1).get("tipo").asText());
    // vf: exactamente Verdadero / Falso
    t = tapValido();
    ObjectNode vf = (ObjectNode) t.get("questions").get(2);
    vf.put("tipo", "vf").put("answer", 0);
    vf.set("options", JSON.createArrayNode().add("Cierto").add("Falso"));
    rechaza(t, "vf con otras opciones");
    vf.set("options", JSON.createArrayNode().add("Verdadero").add("Falso"));
    pasa(t);
    // paso (historia) no entra en el MVP
    ObjectNode t2 = tapValido();
    ((ObjectNode) t2.get("questions").get(0)).put("tipo", "paso");
    rechaza(t2, "paso");
    // opciones repetidas
    ObjectNode t3 = tapValido();
    ((ArrayNode) t3.get("questions").get(0).get("options")).set(1, "UNA");
    rechaza(t3, "opci\u00F3n repetida (sin distinguir mayúsculas)");
  }

  @Test
  void rechazaHtmlScriptsYControles() {
    for (String malo : new String[] {"<b>Roma</b>", "Roma <script>", "a > b", "&lt;Roma", "&#60;", "javascript:alert(1)",
        "Ro" + (char) 0x0000 + "ma", "Ro" + (char) 0x202E + "ma", "Ro" + (char) 0x200B + "ma"}) {
      ObjectNode t = tapValido();
      t.put("title", malo);
      rechaza(t, malo);
      ObjectNode q = tapValido();
      ((ArrayNode) q.get("questions").get(0).get("options")).set(0, malo);
      rechaza(q, "opci\u00F3n " + malo);
    }
  }

  @Test
  void rechazaEnlacesCorreosYTelefonos() {
    for (String malo : new String[] {"Mira https://x.y/z", "visita www.algo.com", "entra en algo.com ya", "ver bit.ly ahora",
        "escribe a ana@correo.com", "llama al 300 123 4567", "+57 3001234567", "cel 3001234567"}) {
      ObjectNode t = tapValido();
      ((ObjectNode) t.get("questions").get(0)).put("why", malo);
      rechaza(t, malo);
    }
    // Años, rangos y cifras cortas sí pasan.
    for (String bueno : new String[] {"Entre 1789 y 1799", "1789-1799", "p. ej. 3,14 y 2,71", "Ley 100 de 1993"}) {
      ObjectNode t = tapValido();
      ((ObjectNode) t.get("questions").get(0)).put("why", bueno);
      assertDoesNotThrow(() -> validador.validar(t), bueno);
    }
  }

  @Test
  void rechazaPalabrasBloqueadasPorPalabraCompletaYSinTildes() {
    TapsPayloadValidador v = new TapsPayloadValidador(java.util.Set.of("tonto"));
    ObjectNode t = tapValido();
    t.put("title", "Qué TONTO");
    assertThrows(IllegalArgumentException.class, () -> v.validar(t));
    ObjectNode t2 = tapValido();
    t2.put("title", "Tont\u00F3, dijo");
    assertThrows(IllegalArgumentException.class, () -> v.validar(t2));
    ObjectNode t3 = tapValido();
    t3.put("title", "Tontorr\u00F3n");
    assertDoesNotThrow(() -> v.validar(t3), "solo palabra completa");
    // La lista real del classpath también actúa.
    ObjectNode t4 = tapValido();
    ((ObjectNode) t4.get("results").get(0)).put("title", "Eres un idiota");
    rechaza(t4, "lista real");
  }

  @Test
  void elMensajeDeErrorNoRepiteElContenido() {
    ObjectNode t = tapValido();
    t.put("title", "<script>alert('secreto-xyz')</script>");
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> validador.validar(t));
    assertFalse(e.getMessage().contains("secreto-xyz"));
  }

  @Test
  void rechazaTamanoYProfundidadExcesivos() {
    String grande = "{\"v\":1,\"title\":\"" + "x".repeat(TapsPayloadValidador.MAX_BYTES) + "\"}";
    assertTrue(grande.getBytes(StandardCharsets.UTF_8).length > TapsPayloadValidador.MAX_BYTES);
    assertThrows(IllegalArgumentException.class, () -> validador.validar(grande));
    ObjectNode t = tapValido();
    ((ObjectNode) t.get("scene")).set("item", JSON.createObjectNode().set("a", JSON.createObjectNode().set("b", JSON.createArrayNode())));
    assertThrows(IllegalArgumentException.class, () -> validador.validar(t));
    assertThrows(IllegalArgumentException.class, () -> validador.validar("{no es json"));
    assertThrows(IllegalArgumentException.class, () -> validador.validar("[]"));
    assertThrows(IllegalArgumentException.class, () -> validador.validar("{\"v\":1,\"v\":1}"), "duplicados");
    assertThrows(IllegalArgumentException.class, () -> validador.validar((String) null));
  }

  @Test
  void elCatalogoEsElDelTemaEpoca() {
    assertEquals(8, TapsCatalogoEpoca.MUNDOS.size());
    // Los 3 escenarios nuevos del creador web (scenery.js WORLDS) están en el catálogo.
    assertTrue(TapsCatalogoEpoca.MUNDOS.containsAll(java.util.Set.of("biblioteca", "observatorio", "aula")));
    assertEquals(7, TapsCatalogoEpoca.ANFITRIONES.size());
    assertEquals(8, TapsCatalogoEpoca.OBJETOS.size());
    assertEquals(6, TapsCatalogoEpoca.ENCUADRES.size());
  }
}
