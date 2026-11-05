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

  /** Un estilo con todo: degradado, marco, paleta, letra, música y dos capas (texto y sticker). */
  static ObjectNode estiloCompleto() throws Exception {
    return (ObjectNode) JSON.readTree("""
      {"fondo":{"tipo":"degradado","de":"#FF0000","a":"#0000ff","angulo":90.4,"fuerza":0.5},
       "marco":"neon","paleta":"oceano","letra":"pixel","musica":"wonder-loop",
       "capas":[{"tipo":"texto","texto":"  Hola  Roma ","x":10,"y":20,"escala":1.5,"giro":-10,"color":"#ABCDEF"},
                {"tipo":"sticker","id":"estrella"}]}""");
  }

  private ObjectNode conEstilo(JsonNode estilo) {
    ObjectNode t = tapValido();
    t.set("estilo", estilo);
    return t;
  }

  @Test
  void unEstiloCompletoSaleCanonicoYSeRevalidaIgual() throws Exception {
    TapsPayloadValidador.Tap out = validador.validar(conEstilo(estiloCompleto()));
    JsonNode n = JSON.readTree(out.json());
    assertEquals(java.util.List.of("v", "theme", "title", "kicker", "scene", "questions", "results", "topic", "estilo"),
      n.propertyStream().map(java.util.Map.Entry::getKey).toList(), "estilo va después de topic");
    JsonNode e = n.get("estilo");
    assertEquals(JSON.readTree("""
      {"fondo":{"tipo":"degradado","de":"#ff0000","a":"#0000ff","angulo":90,"fuerza":0.5},
       "marco":"neon","paleta":"oceano","letra":"pixel",
       "capas":[{"tipo":"texto","texto":"Hola Roma","x":10.0,"y":20.0,"escala":1.5,"giro":-10,"color":"#abcdef"},
                {"tipo":"sticker","id":"estrella","x":50.0,"y":50.0,"escala":1.0,"giro":0,"color":"#ffffff"}],
       "musica":"wonder-loop"}"""), e);
    assertEquals(out.json(), validador.validar(out.json()).json(), "el canónico guardado se revalida idéntico");
  }

  @Test
  void fondosColorYPatronConValoresPorDefecto() throws Exception {
    JsonNode e = JSON.readTree(validador.validar(conEstilo(JSON.readTree(
      "{\"fondo\":{\"tipo\":\"patron\",\"id\":\"rayas\"}}"))).json()).get("estilo");
    assertEquals(JSON.readTree("{\"fondo\":{\"tipo\":\"patron\",\"id\":\"rayas\",\"color\":\"#ffffff\",\"fuerza\":0.35}}"), e);
    e = JSON.readTree(validador.validar(conEstilo(JSON.readTree(
      "{\"fondo\":{\"tipo\":\"color\",\"color\":\"#123456\",\"fuerza\":0.1}}"))).json()).get("estilo");
    assertEquals(0.1, e.get("fondo").get("fuerza").doubleValue());
    rechaza(conEstilo(JSON.readTree("{\"fondo\":{\"tipo\":\"color\"}}")), "color sin color");
    rechaza(conEstilo(JSON.readTree("{\"fondo\":{\"tipo\":\"degradado\",\"de\":\"#000000\"}}")), "degradado sin a");
  }

  @Test
  void sinEstiloOEstiloVacioNoCambiaLaSalida() throws Exception {
    String base = validador.validar(tapValido()).json();
    assertFalse(JSON.readTree(base).has("estilo"));
    assertEquals(base, validador.validar(conEstilo(JSON.createObjectNode())).json());
    assertEquals(base, validador.validar(conEstilo(JSON.nullNode())).json());
    assertEquals(base, validador.validar(conEstilo(JSON.readTree("{\"capas\":[]}"))).json());
  }

  @Test
  void elEstiloRechazaLoQueNoEstaEnElCatalogo() throws Exception {
    for (String malo : new String[] {
      "{\"marco\":\"fuego\"}", "{\"paleta\":\"__proto__\"}", "{\"letra\":\"comic\"}", "{\"musica\":\"https://x.y/a.mp3\"}",
      "{\"marco\":1}", "{\"__proto__\":{}}", "{\"fondo\":{\"tipo\":\"imagen\",\"url\":\"x\"}}",
      "{\"fondo\":{\"tipo\":\"patron\",\"id\":\"constructor\"}}", "{\"capas\":[{\"tipo\":\"sticker\",\"id\":\"calavera\"}]}",
      "{\"capas\":[{\"tipo\":\"video\"}]}", "{\"capas\":[\"x\"]}", "{\"capas\":{}}", "\"neon\"", "[]"}) {
      rechaza(conEstilo(JSON.readTree(malo)), malo);
    }
  }

  @Test
  void elEstiloRechazaColoresQueNoSonHex() throws Exception {
    for (String color : new String[] {"url(x)", "#fff", "red", "#12345g", "#1234567", "javascript:alert(1)"}) {
      rechaza(conEstilo(JSON.createObjectNode().set("fondo",
        JSON.createObjectNode().put("tipo", "color").put("color", color))), "fondo " + color);
      rechaza(conEstilo(JSON.createObjectNode().set("capas", JSON.createArrayNode().add(
        JSON.createObjectNode().put("tipo", "sticker").put("id", "rayo").put("color", color)))), "capa " + color);
    }
  }

  @Test
  void elEstiloRechazaClavesDesconocidasEnCadaNivel() throws Exception {
    for (String malo : new String[] {"{\"marco\":\"neon\",\"css\":\"x\"}",
      "{\"fondo\":{\"tipo\":\"color\",\"color\":\"#000000\",\"id\":\"puntos\"}}",
      "{\"capas\":[{\"tipo\":\"sticker\",\"id\":\"rayo\",\"texto\":\"hola\"}]}",
      "{\"capas\":[{\"tipo\":\"texto\",\"texto\":\"hola\",\"href\":\"x\"}]}"}) {
      rechaza(conEstilo(JSON.readTree(malo)), malo);
    }
  }

  @Test
  void numerosDelEstiloFueraDeRangoONoNumericos() throws Exception {
    for (String malo : new String[] {"{\"fondo\":{\"tipo\":\"color\",\"color\":\"#000000\",\"fuerza\":0.8}}",
      "{\"fondo\":{\"tipo\":\"color\",\"color\":\"#000000\",\"fuerza\":0.05}}",
      "{\"fondo\":{\"tipo\":\"degradado\",\"de\":\"#000000\",\"a\":\"#ffffff\",\"angulo\":361}}",
      "{\"capas\":[{\"tipo\":\"sticker\",\"id\":\"rayo\",\"x\":101}]}", "{\"capas\":[{\"tipo\":\"sticker\",\"id\":\"rayo\",\"y\":-1}]}",
      "{\"capas\":[{\"tipo\":\"sticker\",\"id\":\"rayo\",\"escala\":3}]}", "{\"capas\":[{\"tipo\":\"sticker\",\"id\":\"rayo\",\"giro\":46}]}",
      "{\"capas\":[{\"tipo\":\"sticker\",\"id\":\"rayo\",\"x\":\"10\"}]}"}) {
      rechaza(conEstilo(JSON.readTree(malo)), malo);
    }
    pasa(conEstilo(JSON.readTree("{\"capas\":[{\"tipo\":\"sticker\",\"id\":\"rayo\",\"x\":0,\"y\":100,\"escala\":2.5,\"giro\":-45}]}")));
  }

  @Test
  void masDeSeisCapasSeRechaza() {
    ArrayNode capas = JSON.createArrayNode();
    for (int i = 0; i < TapsCatalogoEstilo.MAX_CAPAS; i++) capas.add(JSON.createObjectNode().put("tipo", "sticker").put("id", "rayo"));
    pasa(conEstilo(JSON.createObjectNode().set("capas", capas)));
    capas.add(JSON.createObjectNode().put("tipo", "sticker").put("id", "rayo"));
    rechaza(conEstilo(JSON.createObjectNode().set("capas", capas)), "7 capas");
  }

  @Test
  void elTextoDeCapaPasaPorElMismoFiltro() {
    TapsPayloadValidador v = new TapsPayloadValidador(java.util.Set.of("tonto"));
    for (String malo : new String[] {"mira www.algo.com", "ana@correo.com", "<b>x</b>", "qué tonto", "x".repeat(41), "  "}) {
      ObjectNode t = conEstilo(JSON.createObjectNode().set("capas", JSON.createArrayNode().add(
        JSON.createObjectNode().put("tipo", "texto").put("texto", malo))));
      IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> v.validar(t), malo);
      assertTrue(e.getMessage().contains("texto 1"), e.getMessage());
      assertFalse(e.getMessage().contains("algo.com"));
    }
  }

  @Test
  void laProfundidadDelEstiloCabeEnElLimite() throws Exception {
    // root → estilo → capas → capa: 4 niveles, justo el máximo.
    ObjectNode t = conEstilo(estiloCompleto());
    assertEquals(TapsPayloadValidador.MAX_PROFUNDIDAD, TapsPayloadValidador.profundidad(t));
    pasa(t);
    ((ObjectNode) t.get("estilo").get("capas").get(0)).set("x", JSON.createArrayNode().add(1));
    rechaza(t, "un nivel más");
  }

  @Test
  void elCatalogoDeEstiloEsElDeLaWeb() {
    assertEquals(6, TapsCatalogoEstilo.MARCOS.size());
    assertEquals(6, TapsCatalogoEstilo.PALETAS.size());
    assertEquals(2, TapsCatalogoEstilo.LETRAS.size());
    assertEquals(4, TapsCatalogoEstilo.PATRONES.size());
    assertEquals(9, TapsCatalogoEstilo.MUSICAS.size());
    assertEquals(16, TapsCatalogoEstilo.STICKERS.size());
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
