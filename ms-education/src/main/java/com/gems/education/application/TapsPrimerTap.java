package com.gems.education.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.List;

/**
 * «Mi primer Tap»: el contenido con el que nace el Tap importado desde el borrador de /crear (PR-10 §6). El adjunto solo
 * trae el personaje (punto de partida {@code b}, objeto {@code i} y lugar {@code w}), así que las cinco preguntas y los
 * resultados son de ejemplo, del tema «época», para que el creador tenga algo que jugar y editar enseguida. El look
 * personalizado no viaja en el MVP (el motor usa el personaje base; queda para PR-16/17).
 *
 * <p>Lo que sale de aquí vuelve a pasar por {@link TapsPayloadValidador} antes de guardarse: no hay atajo.
 */
public final class TapsPrimerTap {
  public static final String TITULO = "Mi primer Tap";
  public static final String KICKER = "Historia";
  /** «sorpresa» (look hecho con «Sorpréndeme») no es un anfitrión del motor: se usa este punto de partida. */
  public static final String ANFITRION_POR_DEFECTO = "grabador";

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private record Pregunta(String q, List<String> opciones, int respuesta, String porque) {}

  private static final List<Pregunta> PREGUNTAS = List.of(
    new Pregunta("¿Qué civilización construyó las pirámides de Guiza?", List.of("Egipcia", "Romana", "Griega", "Maya"), 0,
      "Las pirámides de Guiza se levantaron en el Antiguo Egipto hace más de cuatro mil años."),
    new Pregunta("¿Qué ciudad fue el centro del Imperio romano?", List.of("Atenas", "Roma", "Cartago", "Alejandría"), 1,
      "Roma fue la capital del imperio que dominó el Mediterráneo durante siglos."),
    new Pregunta("¿Qué invento permitió copiar libros en serie en el siglo XV?", List.of("La brújula", "El telescopio", "La imprenta", "La pólvora"), 2,
      "La imprenta de tipos móviles multiplicó los libros y abarató el saber."),
    new Pregunta("¿Qué instrumento sirve para observar las estrellas?", List.of("Microscopio", "Matraz", "Lupa", "Telescopio"), 3,
      "El telescopio acerca lo lejano; el microscopio agranda lo diminuto."),
    new Pregunta("¿Dónde se guardaban los pergaminos en la Antigüedad?", List.of("Bibliotecas", "Foros", "Talleres", "Puertos"), 0,
      "Bibliotecas como la de Alejandría reunían miles de rollos de papiro y pergamino."));

  private TapsPrimerTap() {
  }

  /**
   * El contenido del primer Tap a partir del adjunto ya canónico de {@link InstitutionRequestDraft#normalize}. Solo lee
   * {@code b}, {@code i} y {@code w}; el look se ignora.
   */
  public static ObjectNode desdeAdjunto(JsonNode adjunto) {
    String host = texto(adjunto.get("b"));
    if (host == null || !TapsCatalogoEpoca.ANFITRIONES.contains(host)) host = ANFITRION_POR_DEFECTO;
    String world = texto(adjunto.get("w"));
    if (world == null || !TapsCatalogoEpoca.MUNDOS.contains(world)) throw new IllegalArgumentException("Lugar fuera del catálogo");
    String item = texto(adjunto.get("i"));

    ObjectNode scene = MAPPER.createObjectNode().put("world", world).put("host", host);
    if (item != null && TapsCatalogoEpoca.OBJETOS.contains(item)) scene.put("item", item);

    ArrayNode questions = MAPPER.createArrayNode();
    for (Pregunta p : PREGUNTAS) {
      ObjectNode q = MAPPER.createObjectNode().put("q", p.q());
      ArrayNode options = MAPPER.createArrayNode();
      p.opciones().forEach(options::add);
      q.set("options", options);
      q.put("answer", p.respuesta()).put("why", p.porque());
      questions.add(q);
    }

    ArrayNode results = MAPPER.createArrayNode();
    results.add(MAPPER.createObjectNode().put("min", 0).put("title", "Aprendiz").put("line", "Cada partida enseña algo; vuelve a intentarlo."));
    results.add(MAPPER.createObjectNode().put("min", 3).put("title", "Cronista").put("line", "Ya conoces buena parte de la historia."));
    results.add(MAPPER.createObjectNode().put("min", 5).put("title", "Sabio de la época").put("line", "Impecable: ninguna pregunta se te escapó."));

    ObjectNode tap = MAPPER.createObjectNode();
    tap.put("v", 1).put("theme", TapsCatalogoEpoca.TEMA).put("title", TITULO).put("kicker", KICKER);
    tap.set("scene", scene);
    tap.set("questions", questions);
    tap.set("results", results);
    tap.put("topic", TapsPayloadValidador.TOPIC);
    return tap;
  }

  private static String texto(JsonNode n) {
    return n != null && n.isTextual() ? n.textValue() : null;
  }
}
