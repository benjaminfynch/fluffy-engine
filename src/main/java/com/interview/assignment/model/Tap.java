package com.interview.assignment.model;

import java.time.Instant;

public record Tap(
    long id,
    Instant timestamp,
    TapType type,
    Stop stop,
    String companyId,
    String busId,
    String pan) {}
