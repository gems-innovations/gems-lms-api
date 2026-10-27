package com.gems.education.application;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * The character draft a visitor of /crear attaches to an institution request (plan PR-9a, §A.3.3). The endpoint is
 * public, so the draft is untrusted: it is re-validated here against the closed catalog of the "época" theme
 * (taps-engine/themes/epoca/rig.js CATALOGO + COLORES, theme.json characters, scenery.js WORLD_NAMES) and stored in
 * canonical form, or dropped. Dropping it never fails the request: the person's message matters more.
 *
 * Form: {"v":1,"borradorId":"<id>","b":"<preset>","l":{look},"i":"<item>|null","w":"<world>"}; at most 2048 bytes.
 */
public final class InstitutionRequestDraft {

  public static final int MAX_BYTES = 2048;
  /** Markers of the block older clients append to {@code message} while they have no dedicated field. */
  public static final String BLOCK_START = "[gems-borrador v1]";
  public static final String BLOCK_END = "[/gems-borrador]";

  /** Ids of guardado.js `nuevoId`: time in base 36, a dash, 7+ base-36 chars (the web validates the same form). */
  private static final Pattern DRAFT_ID = Pattern.compile("^[0-9a-z]{1,12}-[0-9a-z]{7,12}$");
  private static final Pattern HEX_COLOR = Pattern.compile("^#[0-9a-fA-F]{6}$");

  private static final Set<String> KEYS = Set.of("v", "borradorId", "b", "l", "i", "w");
  /** theme.json characters (the 7 starting points) plus «sorpresa» (a look made by «Sorpréndeme»). */
  private static final Set<String> PRESETS = Set.of("grabador", "arqueologa", "senador", "escriba", "artesano",
    "investigadora", "periodista", "sorpresa");
  /** rig.js ITEMS. */
  private static final Set<String> ITEMS = Set.of("pluma", "cepillo", "pergamino", "libro", "lupa", "matraz", "antorcha", "mapa");
  /** scenery.js WORLDS. */
  private static final Set<String> WORLDS = Set.of("taller-londres", "excavacion-egipto", "foro-roma", "sala-medieval", "laboratorio-moderno",
    "biblioteca", "observatorio", "aula");
  /** rig.js CATALOGO. */
  private static final Map<String, List<String>> PIECES = Map.of(
    "hairStyle", List.of("corto", "recogido", "ondas", "largo", "bob", "rizos", "calvo"),
    "beard", List.of("none", "patillas", "bigote", "perilla", "barba"),
    "outfit", List.of("levita", "safari", "bata", "toga", "tunica", "medieval", "traje"),
    "sleeve", List.of("larga", "corta"),
    "hat", List.of("none", "copa", "safari", "bicornio", "laurel", "gorra", "boina"));
  /** rig.js COLORES. */
  private static final Set<String> COLORS = Set.of("skin", "skinShade", "lips", "blush", "hair", "brow", "coat", "coatShade",
    "shirt", "vest", "cravat", "trousers", "shoes", "belt", "accent", "hatColor", "hatBand", "glow");

  private static final ObjectMapper MAPPER = new ObjectMapper()
    .enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);

  private InstitutionRequestDraft() {
  }

  /**
   * The draft to store: the dedicated {@code draft} field when it is valid, otherwise the marked block inside
   * {@code message} (current clients), otherwise nothing. The message itself is never changed.
   */
  public static Optional<String> resolve(JsonNode draft, String message) {
    Optional<String> own = normalize(asText(draft));
    return own.isPresent() ? own : normalize(extractBlock(message));
  }

  /** The draft field as JSON text: a string is taken as is, an object is serialized, anything else is nothing. */
  static String asText(JsonNode draft) {
    if (draft == null || draft.isNull()) return null;
    if (draft.isTextual()) return draft.textValue();
    if (!draft.isObject()) return null;
    try {
      return MAPPER.writeValueAsString(draft);
    } catch (Exception e) {
      return null;
    }
  }

  /** The JSON between the markers of {@code message}, or null when there is no complete block. */
  static String extractBlock(String message) {
    if (message == null) return null;
    int start = message.indexOf(BLOCK_START);
    if (start < 0) return null;
    int end = message.indexOf(BLOCK_END, start + BLOCK_START.length());
    if (end < 0) return null;
    return message.substring(start + BLOCK_START.length(), end).trim();
  }

  /** Canonical JSON of a valid draft, or empty when the text is missing, malformed, oversized or off-catalog. */
  public static Optional<String> normalize(String raw) {
    if (raw == null || raw.isBlank()) return Optional.empty();
    if (raw.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) return Optional.empty();
    JsonNode root;
    try {
      root = MAPPER.readTree(raw);
    } catch (Exception e) {
      return Optional.empty();
    }
    if (root == null || !root.isObject() || !onlyKeys(root, KEYS)) return Optional.empty();

    JsonNode v = root.get("v");
    if (v == null || !v.isInt() || v.intValue() != 1) return Optional.empty();
    String id = text(root.get("borradorId"));
    if (id == null || !DRAFT_ID.matcher(id).matches()) return Optional.empty();
    // Like compartir.js normalizarLook: an unknown starting point is just «sorpresa» (the look carries everything).
    String base = text(root.get("b"));
    if (base == null || !PRESETS.contains(base)) base = "sorpresa";
    String world = text(root.get("w"));
    if (world == null || !WORLDS.contains(world)) return Optional.empty();
    JsonNode itemNode = root.get("i");
    String item = null;
    if (itemNode != null && !itemNode.isNull()) {
      item = text(itemNode);
      if (item == null || !ITEMS.contains(item)) return Optional.empty();
    }
    JsonNode look = root.get("l");
    if (look == null || !look.isObject()) return Optional.empty();
    ObjectNode cleanLook = cleanLook(look);
    if (cleanLook == null) return Optional.empty();

    ObjectNode out = MAPPER.createObjectNode();
    out.put("v", 1).put("borradorId", id).put("b", base).set("l", cleanLook);
    if (item == null) out.putNull("i"); else out.put("i", item);
    out.put("w", world);
    try {
      String json = MAPPER.writeValueAsString(out);
      return json.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES ? Optional.empty() : Optional.of(json);
    } catch (Exception e) {
      return Optional.empty();
    }
  }

  /**
   * The look with only known keys: pieces in their list, colours #rrggbb (a {@code null} colour means "absent", as
   * in rig.js validarLook, and is left out), «glasses» boolean. Null when anything else shows up.
   */
  private static ObjectNode cleanLook(JsonNode look) {
    ObjectNode out = MAPPER.createObjectNode();
    for (Map.Entry<String, JsonNode> e : look.properties()) {
      String k = e.getKey();
      JsonNode val = e.getValue();
      if (PIECES.containsKey(k)) {
        String s = text(val);
        if (s == null || !PIECES.get(k).contains(s)) return null;
        out.put(k, s);
      } else if (COLORS.contains(k)) {
        if (val.isNull()) continue;
        String s = text(val);
        if (s == null || !HEX_COLOR.matcher(s).matches()) return null;
        out.put(k, s);
      } else if ("glasses".equals(k)) {
        if (!val.isBoolean()) return null;
        out.put(k, val.booleanValue());
      } else {
        return null;
      }
    }
    return out;
  }

  private static boolean onlyKeys(JsonNode node, Set<String> allowed) {
    for (Map.Entry<String, JsonNode> e : node.properties()) if (!allowed.contains(e.getKey())) return false;
    return true;
  }

  private static String text(JsonNode n) {
    return n != null && n.isTextual() ? n.textValue() : null;
  }
}
