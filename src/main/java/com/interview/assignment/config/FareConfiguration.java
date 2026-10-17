package com.interview.assignment.config;

import com.interview.assignment.model.Stop;
import java.util.Map;
import java.util.Set;

public final class FareConfiguration {
  private FareConfiguration() {}

  public static Map<Set<Stop>, Integer> faresInCents() {
    return Map.of(
        Set.of(Stop.STOP_1, Stop.STOP_2), 325,
        Set.of(Stop.STOP_2, Stop.STOP_3), 550,
        Set.of(Stop.STOP_1, Stop.STOP_3), 730);
  }
}
