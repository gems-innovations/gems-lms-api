package com.gems.shared.web;

import java.time.Clock;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Allows each client (an address) a number of uses per fixed time window, kept in memory: a cheap
 * brake for public endpoints that create data (guest sessions, institution requests). Each service
 * instance counts on its own. Counters of past windows are dropped once too many clients are
 * tracked, so the map cannot grow without bound.
 */
public final class ClientQuota {
  private static final int MAX_TRACKED_CLIENTS = 10_000;

  private final int maxPerWindow;
  private final long windowSeconds;
  private final Clock clock;
  private final ConcurrentHashMap<String, long[]> usage = new ConcurrentHashMap<>();

  public ClientQuota(int maxPerWindow, long windowSeconds) {
    this(maxPerWindow, windowSeconds, Clock.systemUTC());
  }

  ClientQuota(int maxPerWindow, long windowSeconds, Clock clock) {
    this.maxPerWindow = maxPerWindow;
    this.windowSeconds = windowSeconds;
    this.clock = clock;
  }

  /** Counts one use by {@code client}; false when it has used up its quota for the current window. */
  public boolean tryAcquire(String client) {
    long window = clock.instant().getEpochSecond() / windowSeconds;
    if (usage.size() > MAX_TRACKED_CLIENTS) usage.values().removeIf(entry -> entry[0] != window);
    long[] entry = usage.compute(client, (key, current) ->
      current == null || current[0] != window ? new long[] {window, 1} : new long[] {window, current[1] + 1});
    return entry[1] <= maxPerWindow;
  }
}
