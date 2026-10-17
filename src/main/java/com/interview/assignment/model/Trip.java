package com.interview.assignment.model;

import java.time.Duration;
import java.util.Optional;
import java.util.OptionalLong;

public record Trip(Tap tapOn, Optional<Tap> tapOff, int chargeInCents, TripStatus status) {
  public OptionalLong durationSecs() {
    return tapOff
        .map(
            tap ->
                OptionalLong.of(Duration.between(tapOn.timestamp(), tap.timestamp()).getSeconds()))
        .orElseGet(OptionalLong::empty);
  }
}
