package com.interview.assignment.service;

import com.interview.assignment.model.Stop;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class FareCalculator {
  private final Map<Set<Stop>, Integer> faresInCents;

  public FareCalculator(Map<Set<Stop>, Integer> faresInCents) {
    this.faresInCents = Map.copyOf(faresInCents);
  }

  public int completedFareInCents(Stop from, Stop to) {
    Objects.requireNonNull(from, "from");
    Objects.requireNonNull(to, "to");

    if (from == to) {
      return 0;
    }

    var fare = faresInCents.get(Set.of(from, to));
    if (fare == null) {
      throw new IllegalArgumentException("No fare configured between " + from + " and " + to);
    }
    return fare;
  }

  public int incompleteFareInCents(Stop from) {
    Objects.requireNonNull(from, "from");

    return faresInCents.entrySet().stream()
        .filter(entry -> entry.getKey().contains(from))
        .mapToInt(Map.Entry::getValue)
        .max()
        .orElseThrow(() -> new IllegalArgumentException("No fares configured from " + from));
  }
}
