package com.gems.shared.web;

import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Optional pagination of list endpoints: without {@code page} the whole list is returned (as
 * before); with it, one page and the total in the {@value #TOTAL_HEADER} header.
 */
public final class Paging {
  public static final String TOTAL_HEADER = "X-Total-Count";
  public static final int DEFAULT_LIMIT = 20;
  public static final int MAX_LIMIT = 100;

  private Paging() {
  }

  /** Page size between 1 and {@link #MAX_LIMIT}. */
  public static int limit(Integer limit) {
    return limit == null ? DEFAULT_LIMIT : Math.max(1, Math.min(limit, MAX_LIMIT));
  }

  /** Zero-based offset of a one-based page. */
  public static long offset(int page, int limit) {
    return (long) (Math.max(page, 1) - 1) * limit;
  }

  public static <T> Mono<ResponseEntity<List<T>>> ok(List<T> items, long total) {
    return Mono.just(ResponseEntity.ok().header(TOTAL_HEADER, String.valueOf(total)).body(items));
  }

  /** Pages an already loaded list (fine for lists that are small or already read whole). */
  public static <T> Mono<ResponseEntity<List<T>>> of(Flux<T> all, Integer page, Integer limit) {
    if (page == null) return all.collectList().map(ResponseEntity::ok);
    int size = limit(limit);
    long offset = offset(page, size);
    return all.collectList().flatMap(list -> ok(
      list.stream().skip(offset).limit(size).toList(), list.size()));
  }
}
