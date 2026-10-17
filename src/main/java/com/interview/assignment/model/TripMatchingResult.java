package com.interview.assignment.model;

import java.util.List;

public record TripMatchingResult(List<Trip> trips, int unmatchedOffCount) {
  public TripMatchingResult {
    trips = List.copyOf(trips);
  }
}
