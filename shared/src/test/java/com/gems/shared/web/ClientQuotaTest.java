package com.gems.shared.web;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientQuotaTest {

  /** A clock the test can move forward. */
  private static final class MovableClock extends Clock {
    private Instant now = Instant.parse("2026-01-01T00:00:00Z");

    void advance(Duration by) {
      now = now.plus(by);
    }

    @Override public ZoneId getZone() { return ZoneOffset.UTC; }

    @Override public Clock withZone(ZoneId zone) { return this; }

    @Override public Instant instant() { return now; }
  }

  @Test
  void eachClientGetsItsOwnQuotaPerWindow() {
    var clock = new MovableClock();
    var quota = new ClientQuota(2, 3600, clock);

    assertTrue(quota.tryAcquire("a"));
    assertTrue(quota.tryAcquire("a"));
    assertFalse(quota.tryAcquire("a"));
    assertTrue(quota.tryAcquire("b"));
  }

  @Test
  void theQuotaIsRestoredWhenTheWindowChanges() {
    var clock = new MovableClock();
    var quota = new ClientQuota(1, 3600, clock);

    assertTrue(quota.tryAcquire("a"));
    assertFalse(quota.tryAcquire("a"));
    clock.advance(Duration.ofHours(1));
    assertTrue(quota.tryAcquire("a"));
  }
}
