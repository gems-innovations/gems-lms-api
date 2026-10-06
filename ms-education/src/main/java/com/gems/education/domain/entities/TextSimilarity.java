package com.gems.education.domain.entities;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Similarity between submissions of the same assignment: texts are normalised (no markdown,
 * accents, punctuation or case) and cut into overlapping groups of {@link #SHINGLE} words; the
 * score is the share of groups both texts have (Jaccard). Rewording a few words barely lowers it,
 * while two independent answers to the same question rarely share long word sequences.
 * It only compares submissions with each other, not with external sources.
 */
public final class TextSimilarity {
  public static final int SHINGLE = 5;
  /** Shorter answers are too generic to compare. */
  public static final int MIN_WORDS = 30;

  private TextSimilarity() {
  }

  public record Match(Long submissionId, Long otherSubmissionId, double score, String sharedExcerpt) {
  }

  public static List<String> words(String text) {
    if (text == null) return List.of();
    String plain = Normalizer.normalize(text, Normalizer.Form.NFD)
      .replaceAll("\\p{M}", "")
      .replaceAll("```[\\s\\S]*?```", " ")
      .replaceAll("!?\\[([^\\]]*)]\\([^)]*\\)", "$1")
      .replaceAll("[^\\p{L}\\p{N}]+", " ")
      .toLowerCase()
      .trim();
    return plain.isEmpty() ? List.of() : List.of(plain.split("\\s+"));
  }

  public static Set<String> shingles(List<String> words) {
    Set<String> out = new HashSet<>();
    for (int i = 0; i + SHINGLE <= words.size(); i++) out.add(String.join(" ", words.subList(i, i + SHINGLE)));
    return out;
  }

  public static double jaccard(Set<String> a, Set<String> b) {
    if (a.isEmpty() || b.isEmpty()) return 0;
    Set<String> small = a.size() <= b.size() ? a : b;
    Set<String> large = small == a ? b : a;
    long shared = small.stream().filter(large::contains).count();
    return (double) shared / (a.size() + b.size() - shared);
  }

  /** Pairs at or above the threshold, most similar first. texts: submissionId → text. */
  public static List<Match> matches(Map<Long, String> texts, double threshold) {
    List<Long> ids = new ArrayList<>(texts.keySet());
    List<List<String>> words = ids.stream().map(id -> words(texts.get(id))).toList();
    List<Set<String>> sets = words.stream().map(TextSimilarity::shingles).toList();
    List<Match> out = new ArrayList<>();
    for (int i = 0; i < ids.size(); i++) {
      if (words.get(i).size() < MIN_WORDS) continue;
      for (int j = i + 1; j < ids.size(); j++) {
        if (words.get(j).size() < MIN_WORDS) continue;
        double score = jaccard(sets.get(i), sets.get(j));
        if (score >= threshold) {
          out.add(new Match(ids.get(i), ids.get(j), Math.round(score * 1000) / 1000.0, excerpt(words.get(i), sets.get(j))));
        }
      }
    }
    out.sort((x, y) -> Double.compare(y.score(), x.score()));
    return out;
  }

  /** The longest run of words of a that also appears in b, to show the teacher what repeats. */
  static String excerpt(List<String> a, Set<String> bShingles) {
    int bestStart = -1;
    int bestLen = 0;
    int start = -1;
    for (int i = 0; i + SHINGLE <= a.size(); i++) {
      boolean shared = bShingles.contains(String.join(" ", a.subList(i, i + SHINGLE)));
      if (shared && start < 0) start = i;
      if ((!shared || i + SHINGLE == a.size()) && start >= 0) {
        int end = shared ? i + SHINGLE : i - 1 + SHINGLE;
        if (end - start > bestLen) { bestLen = end - start; bestStart = start; }
        start = -1;
      }
    }
    if (bestStart < 0) return "";
    List<String> run = a.subList(bestStart, Math.min(a.size(), bestStart + Math.min(bestLen, 40)));
    return String.join(" ", run) + (bestLen > 40 ? "…" : "");
  }
}
