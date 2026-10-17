package com.interview.assignment.service;

import com.interview.assignment.model.JourneyKey;
import com.interview.assignment.model.Tap;
import com.interview.assignment.model.TapType;
import com.interview.assignment.model.Trip;
import com.interview.assignment.model.TripMatchingResult;
import com.interview.assignment.model.TripStatus;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TripMatchingService {
  private static final Logger LOGGER = LoggerFactory.getLogger(TripMatchingService.class);
  private static final Comparator<Tap> TAP_ORDER =
      Comparator.comparing(Tap::timestamp).thenComparingLong(Tap::id);

  private final FareCalculator fareCalculator;

  public TripMatchingService(FareCalculator fareCalculator) {
    this.fareCalculator = fareCalculator;
  }

  public TripMatchingResult match(List<Tap> taps) {
    var openTrips = new HashMap<JourneyKey, Tap>();
    var trips = new ArrayList<Trip>();
    int unmatchedOffCount = 0;

    for (var tap : taps.stream().sorted(TAP_ORDER).toList()) {
      var key = new JourneyKey(tap.pan(), tap.companyId(), tap.busId());

      if (tap.type() == TapType.ON) {
        handleTapOn(tap, key, openTrips, trips);
      } else if (!matchTapOff(tap, key, openTrips, trips)) {
        unmatchedOffCount++;
      }
    }

    openTrips.values().forEach(tapOn -> trips.add(incompleteTrip(tapOn)));
    trips.sort(Comparator.comparing(Trip::tapOn, TAP_ORDER));

    var result = new TripMatchingResult(trips, unmatchedOffCount);
    LOGGER.info(
        "Trip matching complete: taps={}, trips={}, unmatchedOffCount={}",
        taps.size(),
        result.trips().size(),
        result.unmatchedOffCount());
    return result;
  }

  private void handleTapOn(
      Tap tap, JourneyKey key, Map<JourneyKey, Tap> openTrips, List<Trip> trips) {
    var previousOn = openTrips.put(key, tap);
    if (previousOn != null) {
      trips.add(incompleteTrip(previousOn));
    }
  }

  private boolean matchTapOff(
      Tap tap, JourneyKey key, Map<JourneyKey, Tap> openTrips, List<Trip> trips) {
    var tapOn = openTrips.remove(key);
    if (tapOn == null) {
      return false;
    }

    trips.add(finishedTrip(tapOn, tap));
    return true;
  }

  private Trip finishedTrip(Tap tapOn, Tap tapOff) {
    var status = tapOn.stop() == tapOff.stop() ? TripStatus.CANCELLED : TripStatus.COMPLETED;
    var charge = fareCalculator.completedFareInCents(tapOn.stop(), tapOff.stop());
    return new Trip(tapOn, Optional.of(tapOff), charge, status);
  }

  private Trip incompleteTrip(Tap tapOn) {
    return new Trip(
        tapOn,
        Optional.empty(),
        fareCalculator.incompleteFareInCents(tapOn.stop()),
        TripStatus.INCOMPLETE);
  }
}
