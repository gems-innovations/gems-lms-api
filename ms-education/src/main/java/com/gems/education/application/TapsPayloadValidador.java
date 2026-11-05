package com.gems.education.application;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.Arrays;
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
 * topic?:'creador', estilo?}} ({@code estilo}: catálogo {@link TapsCatalogoEstilo}); pregunta {@code {q, options, answer, why, tipo?:'clasica'|'duo'|'vf'}}; resultado
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

  private static final Set<String> CLAVES = Set.of("v", "theme", "title", "kicker", "scene", "questions", "results", "topic",
    "estilo");
  private static final Set<String> CLAVES_ESTILO = Set.of("fondo", "marco", "paleta", "letra", "capas", "musica");
  private static final Map<String, Set<String>> CLAVES_FONDO = Map.of(
    "color", Set.of("tipo", "color", "fuerza"),
    "degradado", Set.of("tipo", "de", "a", "angulo", "fuerza"),
    "patron", Set.of("tipo", "id", "color", "fuerza"));
  private static final Set<String> CLAVES_CAPA_TEXTO = Set.of("tipo", "texto", "x", "y", "escala", "giro", "color");
  private static final Set<String> CLAVES_CAPA_STICKER = Set.of("tipo", "id", "x", "y", "escala", "giro", "color");
  private static final Pattern HEX = Pattern.compile("#[0-9a-fA-F]{6}");
  private static final String BLANCO = "#ffffff";
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
  /** Solo comentarios: nueve dígitos con hasta tres separadores entre cada uno («300 - 123 - 4567»). */
  private static final Pattern TELEFONO_SUELTO = Pattern.compile("(?:\\+?\\d[\\s().\\-_/]{0,3}){9,}");
  private static final Pattern DOMINIO_CONTACTO = Pattern.compile("\\b[a-z0-9-]+\\.(me|gg|lat|to|cc|site|online|chat|club)\\b",
    Pattern.CASE_INSENSITIVE);
  private static final Pattern USUARIO = Pattern.compile("(?<![\\p{L}\\p{N}])@[\\p{L}\\p{N}_.]{3,}");
  private static final Set<String> APPS_CONTACTO = Set.of("whatsapp", "wasap", "wsp", "telegram", "insta", "instagram", "snapchat",
    "discord");
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
    ObjectNode estilo = estilo(root.get("estilo"));

    ObjectNode out = MAPPER.createObjectNode();
    out.put("v", 1).put("theme", TapsCatalogoEpoca.TEMA).put("title", title).put("kicker", kicker);
    out.set("scene", scene);
    out.set("questions", questions);
    out.set("results", results);
    out.put("topic", TOPIC);
    if (estilo != null) out.set("estilo", estilo);
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
   * La personalización (mismas reglas que {@code validarEstilo} de la web, taps.estilo.ts), pero más estricta: lo que la web
   * descartaría en silencio aquí es un 400. Sin nada que guardar devuelve null y el canónico no lleva {@code estilo}.
   */
  private ObjectNode estilo(JsonNode e) {
    if (e == null || e.isNull()) return null;
    if (!e.isObject()) throw error("El estilo no es válido");
    soloClaves(e, CLAVES_ESTILO, "estilo");
    ObjectNode out = MAPPER.createObjectNode();
    if (presente(e.get("fondo"))) out.set("fondo", fondo(e.get("fondo")));
    if (presente(e.get("marco"))) out.put("marco", delCatalogo(e.get("marco"), TapsCatalogoEstilo.MARCOS, "Marco"));
    if (presente(e.get("paleta"))) out.put("paleta", delCatalogo(e.get("paleta"), TapsCatalogoEstilo.PALETAS, "Paleta"));
    if (presente(e.get("letra"))) out.put("letra", delCatalogo(e.get("letra"), TapsCatalogoEstilo.LETRAS, "Letra"));
    if (presente(e.get("capas"))) {
      ArrayNode capas = capas(e.get("capas"));
      if (!capas.isEmpty()) out.set("capas", capas);
    }
    if (presente(e.get("musica"))) out.put("musica", delCatalogo(e.get("musica"), TapsCatalogoEstilo.MUSICAS, "Música"));
    return out.isEmpty() ? null : out;
  }

  private static ObjectNode fondo(JsonNode f) {
    if (!f.isObject()) throw error("El fondo no es válido");
    String tipo = texto(f.get("tipo"));
    Set<String> claves = tipo == null ? null : CLAVES_FONDO.get(tipo);
    if (claves == null) throw error("Tipo de fondo no admitido");
    soloClaves(f, claves, "fondo");
    ObjectNode out = MAPPER.createObjectNode().put("tipo", tipo);
    switch (tipo) {
      case "color" -> out.put("color", color(f.get("color"), null, "fondo"));
      case "degradado" -> out.put("de", color(f.get("de"), null, "fondo")).put("a", color(f.get("a"), null, "fondo"))
        .put("angulo", (int) Math.round(numero(f.get("angulo"), 0, 360, 180, "ángulo del fondo")));
      default -> out.put("id", delCatalogo(f.get("id"), TapsCatalogoEstilo.PATRONES, "Patrón"))
        .put("color", color(f.get("color"), BLANCO, "fondo"));
    }
    return out.put("fuerza", numero(f.get("fuerza"), 0.1, 0.7, 0.35, "fuerza del fondo"));
  }

  private ArrayNode capas(JsonNode capas) {
    if (!capas.isArray()) throw error("Las capas no son válidas");
    if (capas.size() > TapsCatalogoEstilo.MAX_CAPAS) throw error("Máximo " + TapsCatalogoEstilo.MAX_CAPAS + " capas");
    ArrayNode out = MAPPER.createArrayNode();
    int n = 0;
    for (JsonNode c : capas) {
      n++;
      String capa = "capa " + n;
      if (!c.isObject()) throw error("La " + capa + " no es válida");
      String tipo = texto(c.get("tipo"));
      boolean esTexto = "texto".equals(tipo);
      if (!esTexto && !"sticker".equals(tipo)) throw error("Tipo de la " + capa + " no admitido");
      soloClaves(c, esTexto ? CLAVES_CAPA_TEXTO : CLAVES_CAPA_STICKER, capa);
      ObjectNode o = MAPPER.createObjectNode().put("tipo", tipo);
      if (esTexto) o.put("texto", textoLimpio(c.get("texto"), 1, TapsCatalogoEstilo.MAX_TEXTO_CAPA, "texto " + n));
      else o.put("id", delCatalogo(c.get("id"), TapsCatalogoEstilo.STICKERS, "Sticker de la " + capa));
      o.put("x", numero(c.get("x"), 0, 100, 50, "x de la " + capa))
        .put("y", numero(c.get("y"), 0, 100, 50, "y de la " + capa))
        .put("escala", numero(c.get("escala"), 0.5, 2.5, 1, "escala de la " + capa))
        .put("giro", (int) Math.round(numero(c.get("giro"), -45, 45, 0, "giro de la " + capa)))
        .put("color", color(c.get("color"), BLANCO, capa));
      out.add(o);
    }
    return out;
  }

  private static boolean presente(JsonNode n) {
    return n != null && !n.isNull();
  }

  private static String delCatalogo(JsonNode n, Set<String> catalogo, String campo) {
    String s = texto(n);
    if (s == null || !catalogo.contains(s)) throw error(campo + " fuera del catálogo");
    return s;
  }

  /** {@code #rrggbb}, guardado en minúsculas; ausente vale {@code def} (o falta, si no hay). */
  private static String color(JsonNode n, String def, String campo) {
    if (!presente(n)) {
      if (def == null) throw error("Falta el color en " + campo);
      return def;
    }
    String s = texto(n);
    if (s == null || !HEX.matcher(s).matches()) throw error("Color no válido en " + campo);
    return s.toLowerCase(Locale.ROOT);
  }

  /** Un número dentro de [min, max]; ausente vale {@code def}. */
  private static double numero(JsonNode n, double min, double max, double def, String campo) {
    if (!presente(n)) return def;
    if (!n.isNumber() || !(n.doubleValue() >= min && n.doubleValue() <= max)) throw error("Valor fuera de rango: " + campo);
    return n.doubleValue();
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

  /**
   * Un comentario (texto libre entre usuarios, y hay menores): {@link #textoLimpio} y además sin datos de contacto escritos para
   * saltarse ese filtro: dígitos o letras de ancho completo (NFKC), teléfonos con separadores dobles, «@usuario», dominios
   * como t.me o discord.gg y nombres de apps de mensajería. Va aparte porque {@link #textoLimpio} también revalida los Taps
   * ya publicados, y estas reglas no deben rechazarlos.
   */
  String comentarioLimpio(String texto, int max, String campo) {
    String s = textoLimpio(texto == null ? null : TextNode.valueOf(texto), 1, max, campo);
    String k = Normalizer.normalize(s, Normalizer.Form.NFKC);
    if (HTML.matcher(k).find() || URL.matcher(k).find() || CORREO.matcher(k).find() || tieneBloqueada(k)) {
      throw error("No se admite ese texto en " + campo);
    }
    if (TELEFONO_SUELTO.matcher(k).find() || DOMINIO_CONTACTO.matcher(k).find() || USUARIO.matcher(k).find()
      || Arrays.stream(PALABRA.split(simplificar(k))).anyMatch(APPS_CONTACTO::contains)) {
      throw error("No se admiten datos de contacto en " + campo);
    }
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
