package com.gems.education.application;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Valida y canoniza el contenido de un Tap de creador (PR-10 §2). Misma filosofía estricta que
 * {@link InstitutionRequestDraft}: claves cerradas, duplicados prohibidos, tamaño y profundidad acotados, catálogo cerrado
 * ({@link TapsCatalogoEpoca}) y textos limpios (sin controles, sin HTML, sin URLs ni datos de contacto, sin palabras de
 * {@code taps/bloqueados.txt}). Devuelve el JSON canónico que se guarda, con solo lo que la API conoce: la web lo
 * interpola como datos (Angular escapa), nunca como HTML.
 *
 * <p>Forma: {@code {v:1, theme:'epoca', title, kicker, scene:{world,host,item?,shot?}, questions[5], results[3..5],
 * topic?:'creador'}}; pregunta {@code {q, options, answer, why, tipo?:'clasica'|'duo'|'vf'}}; resultado
 * {@code {min, title, line}}. Los límites de texto copian los del banco ({@code generar-taps.mjs LIMITES}).
 */
public final class TapsPayloadValidador {
  public static final int MAX_BYTES = 16 * 1024;
  public static final int MAX_PROFUNDIDAD = 4;
  public static final String RECURSO_BLOQUEADOS = "taps/bloqueados.txt";

  public static final int TITULO = 60;
  public static final int KICKER = 30;
  public static final int PREGUNTA = 120;
  public static final int OPCION = 22;
  public static final int EXPLICACION = 90;
  public static final int RESULTADO_TITULO = 40;
  public static final int RESULTADO_LINEA = 90;
  public static final int PREGUNTAS = 5;
  public static final int RESULTADOS_MIN = 3;
  public static final int RESULTADOS_MAX = 5;

  private static final Set<String> CLAVES = Set.of("v", "theme", "title", "kicker", "scene", "questions", "results", "topic");
  private static final Set<String> CLAVES_ESCENA = Set.of("world", "host", "item", "shot");
  /** {@code id} se admite (y se ignora) para que el JSON canónico guardado pase otra vez por el validador al publicar. */
  private static final Set<String> CLAVES_PREGUNTA = Set.of("id", "q", "options", "answer", "why", "tipo");
  private static final Set<String> CLAVES_RESULTADO = Set.of("min", "title", "line");
  private static final Set<String> TIPOS = Set.of("clasica", "duo", "vf");
  private static final List<String> VF = List.of("Verdadero", "Falso");
  public static final String TOPIC = "creador";

  /** Caracteres de control (salvo ninguno: un Tap se escribe en una línea) y marcas de dirección/invisibles. */
  private static final Pattern CONTROLES = Pattern.compile("[\\p{Cntrl}\\p{Cf}\\p{Co}\\p{Cn}]");
  private static final Pattern HTML = Pattern.compile("[<>]|&#|&[a-zA-Z]+;|javascript:|data:|vbscript:", Pattern.CASE_INSENSITIVE);
  private static final Pattern URL = Pattern.compile("https?://|www\\.|[a-z0-9-]+\\.(com|net|org|co|io|info|xyz|app|link|ly|gl|tv)\\b",
    Pattern.CASE_INSENSITIVE);
  private static final Pattern CORREO = Pattern.compile("[^\\s@]+@[^\\s@]+\\.[^\\s@]+");
  /** Nueve o más dígitos seguidos (con separadores sueltos): un rango de años como 1789-1799 no llega. */
  private static final Pattern TELEFONO = Pattern.compile("(?:\\+?\\d[\\s().-]?){9,}");
  private static final Pattern PALABRA = Pattern.compile("[^\\p{L}\\p{N}]+");
  /** Cualquier espacio Unicode (también el duro, U+00A0) se reduce a uno normal. */
  private static final Pattern ESPACIOS = Pattern.compile("\\s+", Pattern.UNICODE_CHARACTER_CLASS);

  private static final ObjectMapper MAPPER = new ObjectMapper()
    .enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);

  private final Set<String> bloqueadas;

  public TapsPayloadValidador(Set<String> bloqueadas) {
    this.bloqueadas = Set.copyOf(bloqueadas);
  }

  /** Con la lista de {@code taps/bloqueados.txt} del classpath. */
  public static TapsPayloadValidador delClasspath() {
    InputStream in = TapsPayloadValidador.class.getClassLoader().getResourceAsStream(RECURSO_BLOQUEADOS);
    if (in == null) throw new IllegalStateException("Falta el recurso " + RECURSO_BLOQUEADOS);
    return new TapsPayloadValidador(leer(in));
  }

  static Set<String> leer(InputStream in) {
    Set<String> palabras = new HashSet<>();
    try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
      String linea;
      while ((linea = r.readLine()) != null) {
        linea = linea.trim();
        if (linea.isEmpty() || linea.startsWith("#")) continue;
        palabras.add(simplificar(linea));
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    return palabras;
  }

  /** Resultado: el JSON canónico (lo que se guarda) y el título ya limpio (columna propia). */
  public record Tap(String json, String titulo) {}

  /**
   * Valida {@code raw} (texto JSON) y devuelve la forma canónica. Lanza {@link IllegalArgumentException} con un mensaje
   * corto y sin eco del contenido cuando algo no cumple (la API responde 400).
   */
  public Tap validar(String raw) {
    if (raw == null || raw.isBlank()) throw error("El Tap está vacío");
    if (raw.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) throw error("El Tap supera los 16 KB");
    JsonNode root;
    try {
      root = MAPPER.readTree(raw);
    } catch (Exception e) {
      throw error("El Tap no es un JSON válido");
    }
    return validar(root);
  }

  /** Igual que {@link #validar(String)} sobre un árbol ya leído (el cuerpo de la petición). */
  public Tap validar(JsonNode root) {
    if (root == null || !root.isObject()) throw error("El Tap debe ser un objeto");
    if (root.toString().getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) throw error("El Tap supera los 16 KB");
    if (profundidad(root) > MAX_PROFUNDIDAD) throw error("El Tap tiene demasiados niveles");
    soloClaves(root, CLAVES, "Tap");

    JsonNode v = root.get("v");
    if (v == null || !v.isInt() || v.intValue() != 1) throw error("Versión de formato desconocida");
    String theme = texto(root.get("theme"));
    if (!TapsCatalogoEpoca.TEMA.equals(theme)) throw error("Tema no admitido");
    if (root.has("topic") && !TOPIC.equals(texto(root.get("topic")))) throw error("Categoría no admitida");

    String title = textoLimpio(root.get("title"), 1, TITULO, "título");
    String kicker = textoLimpio(root.get("kicker"), 1, KICKER, "subtítulo");
    ObjectNode scene = escena(root.get("scene"));
    ArrayNode questions = preguntas(root.get("questions"));
    ArrayNode results = resultados(root.get("results"));

    ObjectNode out = MAPPER.createObjectNode();
    out.put("v", 1).put("theme", TapsCatalogoEpoca.TEMA).put("title", title).put("kicker", kicker);
    out.set("scene", scene);
    out.set("questions", questions);
    out.set("results", results);
    out.put("topic", TOPIC);
    try {
      String json = MAPPER.writeValueAsString(out);
      if (json.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) throw error("El Tap supera los 16 KB");
      return new Tap(json, title);
    } catch (IllegalArgumentException e) {
      throw e;
    } catch (Exception e) {
      throw error("El Tap no se pudo guardar");
    }
  }

  private ObjectNode escena(JsonNode scene) {
    if (scene == null || !scene.isObject()) throw error("Falta la escena");
    soloClaves(scene, CLAVES_ESCENA, "escena");
    String world = texto(scene.get("world"));
    if (world == null || !TapsCatalogoEpoca.MUNDOS.contains(world)) throw error("Lugar fuera del catálogo");
    String host = texto(scene.get("host"));
    if (host == null || !TapsCatalogoEpoca.ANFITRIONES.contains(host)) throw error("Personaje fuera del catálogo");
    ObjectNode out = MAPPER.createObjectNode().put("world", world).put("host", host);
    JsonNode item = scene.get("item");
    if (item != null && !item.isNull()) {
      String s = texto(item);
      if (s == null || !TapsCatalogoEpoca.OBJETOS.contains(s)) throw error("Objeto fuera del catálogo");
      out.put("item", s);
    }
    JsonNode shot = scene.get("shot");
    if (shot != null && !shot.isNull()) {
      String s = texto(shot);
      if (s == null || !TapsCatalogoEpoca.ENCUADRES.contains(s)) throw error("Encuadre fuera del catálogo");
      out.put("shot", s);
    }
    return out;
  }

  private ArrayNode preguntas(JsonNode questions) {
    if (questions == null || !questions.isArray() || questions.size() != PREGUNTAS)
      throw error("Un Tap tiene exactamente " + PREGUNTAS + " preguntas");
    ArrayNode out = MAPPER.createArrayNode();
    int n = 0;
    for (JsonNode q : questions) {
      n++;
      if (!q.isObject()) throw error("La pregunta " + n + " no es válida");
      soloClaves(q, CLAVES_PREGUNTA, "pregunta " + n);
      String tipo = "clasica";
      JsonNode tipoNode = q.get("tipo");
      if (tipoNode != null && !tipoNode.isNull()) {
        tipo = texto(tipoNode);
        if (tipo == null || !TIPOS.contains(tipo)) throw error("Formato de la pregunta " + n + " no admitido");
      }
      String enunciado = textoLimpio(q.get("q"), 1, PREGUNTA, "pregunta " + n);
      JsonNode opts = q.get("options");
      int esperadas = "clasica".equals(tipo) ? 4 : 2;
      if (opts == null || !opts.isArray() || opts.size() != esperadas)
        throw error("La pregunta " + n + " necesita " + esperadas + " opciones");
      ArrayNode options = MAPPER.createArrayNode();
      Set<String> vistas = new HashSet<>();
      int i = 0;
      for (JsonNode o : opts) {
        i++;
        String opcion = textoLimpio(o, 1, OPCION, "opción " + i + " de la pregunta " + n);
        if (!vistas.add(simplificar(opcion))) throw error("La pregunta " + n + " repite una opción");
        options.add(opcion);
      }
      if ("vf".equals(tipo)) {
        List<String> dadas = List.of(options.get(0).textValue(), options.get(1).textValue());
        if (!VF.equals(dadas)) throw error("La pregunta " + n + " de verdadero o falso debe tener las opciones Verdadero y Falso");
      }
      JsonNode answer = q.get("answer");
      if (answer == null || !answer.isInt() || answer.intValue() < 0 || answer.intValue() >= esperadas)
        throw error("La respuesta de la pregunta " + n + " no señala una opción");
      String why = textoLimpio(q.get("why"), 1, EXPLICACION, "explicación de la pregunta " + n);

      ObjectNode item = MAPPER.createObjectNode();
      item.put("id", "p" + n).put("q", enunciado);
      item.set("options", options);
      if (!"clasica".equals(tipo)) item.put("tipo", tipo);
      item.put("answer", answer.intValue()).put("why", why);
      out.add(item);
    }
    return out;
  }

  private ArrayNode resultados(JsonNode results) {
    if (results == null || !results.isArray() || results.size() < RESULTADOS_MIN || results.size() > RESULTADOS_MAX)
      throw error("Un Tap tiene entre " + RESULTADOS_MIN + " y " + RESULTADOS_MAX + " resultados");
    ArrayNode out = MAPPER.createArrayNode();
    int anterior = -1;
    int n = 0;
    for (JsonNode r : results) {
      n++;
      if (!r.isObject()) throw error("El resultado " + n + " no es válido");
      soloClaves(r, CLAVES_RESULTADO, "resultado " + n);
      JsonNode min = r.get("min");
      if (min == null || !min.isInt() || min.intValue() < 0 || min.intValue() > PREGUNTAS)
        throw error("El mínimo del resultado " + n + " debe estar entre 0 y " + PREGUNTAS);
      if (n == 1 && min.intValue() != 0) throw error("El primer resultado empieza en 0 aciertos");
      if (min.intValue() <= anterior) throw error("Los resultados van de menos a más aciertos, sin repetir");
      anterior = min.intValue();
      String title = textoLimpio(r.get("title"), 1, RESULTADO_TITULO, "título del resultado " + n);
      String line = textoLimpio(r.get("line"), 1, RESULTADO_LINEA, "texto del resultado " + n);
      out.add(MAPPER.createObjectNode().put("min", min.intValue()).put("title", title).put("line", line));
    }
    return out;
  }

  /**
   * Un texto visible: cadena, recortada, de {@code min} a {@code max} puntos de código, sin controles ni HTML, sin URLs,
   * correos o teléfonos y sin palabras bloqueadas. El mensaje de error nombra el campo, nunca el contenido.
   */
  String textoLimpio(JsonNode node, int min, int max, String campo) {
    String s = texto(node);
    if (s == null) throw error("Falta " + campo);
    s = ESPACIOS.matcher(Normalizer.normalize(s, Normalizer.Form.NFC)).replaceAll(" ").trim();
    int largo = s.codePointCount(0, s.length());
    if (largo < min) throw error("Falta " + campo);
    if (largo > max) throw error("Demasiado largo: " + campo + " (máximo " + max + ")");
    if (CONTROLES.matcher(s).find()) throw error("Caracteres no admitidos en " + campo);
    if (HTML.matcher(s).find()) throw error("No se admite HTML en " + campo);
    if (URL.matcher(s).find()) throw error("No se admiten enlaces en " + campo);
    if (CORREO.matcher(s).find()) throw error("No se admiten correos en " + campo);
    if (TELEFONO.matcher(s).find()) throw error("No se admiten teléfonos en " + campo);
    if (tieneBloqueada(s)) throw error("Lenguaje no admitido en " + campo);
    return s;
  }

  boolean tieneBloqueada(String s) {
    if (bloqueadas.isEmpty()) return false;
    for (String palabra : PALABRA.split(simplificar(s))) {
      if (!palabra.isEmpty() && bloqueadas.contains(palabra)) return true;
    }
    return false;
  }

  /** Minúsculas sin tildes, para comparar palabras. */
  static String simplificar(String s) {
    String plano = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
    return plano.toLowerCase(Locale.ROOT).trim();
  }

  private static void soloClaves(JsonNode node, Set<String> permitidas, String donde) {
    for (Map.Entry<String, JsonNode> e : node.properties()) {
      if (!permitidas.contains(e.getKey())) throw error("Campo desconocido en " + donde);
    }
  }

  static int profundidad(JsonNode node) {
    if (!node.isContainerNode()) return 0;
    int max = 0;
    for (JsonNode hijo : node) max = Math.max(max, profundidad(hijo));
    return 1 + max;
  }

  private static String texto(JsonNode n) {
    return n != null && n.isTextual() ? n.textValue() : null;
  }

  private static IllegalArgumentException error(String mensaje) {
    return new IllegalArgumentException(mensaje);
  }
}
