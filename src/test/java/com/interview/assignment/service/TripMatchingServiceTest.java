package com.interview.assignment.service;

import static com.interview.assignment.model.Stop.STOP_1;
import static com.interview.assignment.model.Stop.STOP_2;
import static com.interview.assignment.model.Stop.STOP_3;
import static com.interview.assignment.model.TapType.OFF;
import static com.interview.assignment.model.TapType.ON;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.interview.assignment.config.FareConfiguration;
import com.interview.assignment.model.Stop;
import com.interview.assignment.model.Tap;
import com.interview.assignment.model.TapType;
import com.interview.assignment.model.Trip;
import com.interview.assignment.model.TripMatchingResult;
import com.interview.assignment.model.TripStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class TripMatchingServiceTest {
  private final TripMatchingService matcher =
      new TripMatchingService(new FareCalculator(FareConfiguration.faresInCents()));

  @Test
  void completesATripAndCalculatesItsDuration() {
    var on = tap(1, "2023-01-22T13:00:00Z", ON, STOP_1);
    var off = tap(2, "2023-01-22T13:05:00Z", OFF, STOP_2);

    var result = matcher.match(List.of(on, off));

    assertEquals(
        List.of(new Trip(on, Optional.of(off), 325, TripStatus.COMPLETED)), result.trips());
    assertEquals(OptionalLong.of(300), result.trips().getFirst().durationSecs());
    assertEquals(0, result.unmatchedOffCount());
  }

  @Test
  void cancelsATripWhenBothTapsAreAtTheSameStop() {
    var on = tap(1, "2023-01-22T13:00:00Z", ON, STOP_1);
    var off = tap(2, "2023-01-22T13:02:00Z", OFF, STOP_1);

    var result = matcher.match(List.of(on, off));

    assertEquals(List.of(new Trip(on, Optional.of(off), 0, TripStatus.CANCELLED)), result.trips());
    assertEquals(OptionalLong.of(120), result.trips().getFirst().durationSecs());
  }

  @ParameterizedTest
  @CsvSource({"STOP_1, 730", "STOP_2, 550", "STOP_3, 730"})
  void finishesUnmatchedTapOnsAsIncomplete(Stop stop, int expectedCharge) {
    var on = tap(1, "2023-01-22T13:00:00Z", ON, stop);

    var result = matcher.match(List.of(on));

    assertEquals(
        List.of(new Trip(on, Optional.empty(), expectedCharge, TripStatus.INCOMPLETE)),
        result.trips());
    assertEquals(OptionalLong.empty(), result.trips().getFirst().durationSecs());
  }

  @Test
  void sortsInputBeforeMatchingWithoutModifyingTheInput() {
    var on = tap(1, "2023-01-22T13:00:00Z", ON, STOP_1);
    var off = tap(2, "2023-01-22T13:05:00Z", OFF, STOP_2);

    var result = matcher.match(List.of(off, on));

    assertEquals(
        List.of(new Trip(on, Optional.of(off), 325, TripStatus.COMPLETED)), result.trips());
  }

  @Test
  void usesTapIdToOrderTapsWithIdenticalTimestamps() {
    var on = tap(1, "2023-01-22T13:00:00Z", ON, STOP_1);
    var off = tap(2, "2023-01-22T13:00:00Z", OFF, STOP_2);

    var result = matcher.match(List.of(off, on));

    assertEquals(
        List.of(new Trip(on, Optional.of(off), 325, TripStatus.COMPLETED)), result.trips());
    assertEquals(OptionalLong.of(0), result.trips().getFirst().durationSecs());
  }

  @Test
  void matchesTripsForDifferentPassengers() {
    var passengerAOn = tap(1, "2023-01-22T13:00:00Z", ON, STOP_1);
    var passengerBOn =
        new Tap(
            2,
            Instant.parse("2023-01-22T13:05:00Z"),
            ON,
            STOP_2,
            "Company1",
            "Bus37",
            "passenger-b");
    var passengerAOff = tap(3, "2023-01-22T13:05:00Z", OFF, STOP_2);
    var passengerBOff =
        new Tap(
            4,
            Instant.parse("2023-01-22T13:10:00Z"),
            OFF,
            STOP_3,
            "Company1",
            "Bus37",
            "passenger-b");

    var result = matcher.match(List.of(passengerAOn, passengerBOn, passengerAOff, passengerBOff));

    assertEquals(
        List.of(
            new Trip(passengerAOn, Optional.of(passengerAOff), 325, TripStatus.COMPLETED),
            new Trip(passengerBOn, Optional.of(passengerBOff), 550, TripStatus.COMPLETED)),
        result.trips());
  }

  @ParameterizedTest(name = "ON at Company1/Bus37 cannot match OFF at {0}/{1}")
  @CsvSource({"Company1, Bus38", "Company2, Bus37"})
  void doesNotMatchTapOffFromADifferentBusOrCompany(String company, String bus) {
    var on = tap(1, "2023-01-22T13:00:00Z", ON, STOP_1);
    var unrelatedOff =
        new Tap(2, Instant.parse("2023-01-22T14:00:00Z"), OFF, STOP_2, company, bus, "passenger-a");

    var result = matcher.match(List.of(on, unrelatedOff));

    assertEquals(
        List.of(new Trip(on, Optional.empty(), 730, TripStatus.INCOMPLETE)), result.trips());
    assertEquals(1, result.unmatchedOffCount());
  }

  @Test
  void closesThePreviousTripAsIncompleteWhenAnotherTapOnArrives() {
    var firstOn = tap(1, "2023-01-22T13:00:00Z", ON, STOP_2);
    var secondOn = tap(2, "2023-01-22T13:01:00Z", ON, STOP_1);
    var off = tap(3, "2023-01-22T13:05:00Z", OFF, STOP_3);

    var result = matcher.match(List.of(firstOn, secondOn, off));

    assertEquals(
        List.of(
            new Trip(firstOn, Optional.empty(), 550, TripStatus.INCOMPLETE),
            new Trip(secondOn, Optional.of(off), 730, TripStatus.COMPLETED)),
        result.trips());
  }

  @Test
  void ignoresTapOffWithoutTapOn() {
    var off = tap(1, "2023-01-22T12:00:00Z", OFF, STOP_3);

    var result = matcher.match(List.of(off));

    assertTrue(result.trips().isEmpty());
    assertEquals(1, result.unmatchedOffCount());
  }

  @Test
  void doesNotReuseTapOnAfterTripIsCompleted() {
    var on = tap(1, "2023-01-22T13:00:00Z", ON, STOP_1);
    var off = tap(2, "2023-01-22T13:05:00Z", OFF, STOP_2);
    var extraOff = tap(3, "2023-01-22T13:06:00Z", OFF, STOP_3);

    var result = matcher.match(List.of(on, off, extraOff));

    assertEquals(
        List.of(new Trip(on, Optional.of(off), 325, TripStatus.COMPLETED)), result.trips());
    assertEquals(1, result.unmatchedOffCount());
  }

  @Test
  void matchesTripAcrossMidnight() {
    var on = tap(1, "2023-01-22T23:59:00Z", ON, STOP_3);
    var off = tap(2, "2023-01-23T00:01:00Z", OFF, STOP_1);

    var result = matcher.match(List.of(on, off));

    assertEquals(
        List.of(new Trip(on, Optional.of(off), 730, TripStatus.COMPLETED)), result.trips());
    assertEquals(OptionalLong.of(120), result.trips().getFirst().durationSecs());
  }

  @Test
  void matchesMultipleTripsForTheSamePassenger() {
    var firstOn = tap(1, "2023-01-22T08:00:00Z", ON, STOP_1);
    var firstOff = tap(2, "2023-01-22T08:05:00Z", OFF, STOP_2);
    var secondOn = tap(3, "2023-01-22T17:00:00Z", ON, STOP_2);
    var secondOff = tap(4, "2023-01-22T17:10:00Z", OFF, STOP_3);

    var result = matcher.match(List.of(firstOn, firstOff, secondOn, secondOff));

    assertEquals(
        List.of(
            new Trip(firstOn, Optional.of(firstOff), 325, TripStatus.COMPLETED),
            new Trip(secondOn, Optional.of(secondOff), 550, TripStatus.COMPLETED)),
        result.trips());
  }

  @Test
  void returnsNoTripsForAnEmptyBatch() {
    var expected = new TripMatchingResult(List.of(), 0);
    var actual = matcher.match(List.of());
    assertEquals(expected, actual);
  }

  @Test
  void doesNotRetainOpenTripsBetweenCalls() {
    matcher.match(List.of(tap(1, "2023-01-22T13:00:00Z", ON, STOP_1)));

    var result = matcher.match(List.of(tap(2, "2023-01-22T13:05:00Z", OFF, STOP_2)));

    assertTrue(result.trips().isEmpty());
    assertEquals(1, result.unmatchedOffCount());
  }

  @Test
  void producesTheThreeTripsFromTheSpecificationExample() {
    var firstOn =
        new Tap(
            1,
            Instant.parse("2023-01-22T13:00:00Z"),
            ON,
            STOP_1,
            "Company1",
            "Bus37",
            "5500005555555559");
    var firstOff =
        new Tap(
            2,
            Instant.parse("2023-01-22T13:05:00Z"),
            OFF,
            STOP_2,
            "Company1",
            "Bus37",
            "5500005555555559");
    var incompleteOn =
        new Tap(
            3,
            Instant.parse("2023-01-22T09:20:00Z"),
            ON,
            STOP_3,
            "Company1",
            "Bus36",
            "4111111111111111");
    var cancelledOn =
        new Tap(
            4,
            Instant.parse("2023-01-23T08:00:00Z"),
            ON,
            STOP_1,
            "Company1",
            "Bus37",
            "4111111111111111");
    var cancelledOff =
        new Tap(
            5,
            Instant.parse("2023-01-23T08:02:00Z"),
            OFF,
            STOP_1,
            "Company1",
            "Bus37",
            "4111111111111111");
    var orphanOff =
        new Tap(
            6,
            Instant.parse("2023-01-24T16:30:00Z"),
            OFF,
            STOP_2,
            "Company1",
            "Bus37",
            "5500005555555559");

    var result =
        matcher.match(
            List.of(firstOn, firstOff, incompleteOn, cancelledOn, cancelledOff, orphanOff));

    assertEquals(
        List.of(
            new Trip(incompleteOn, Optional.empty(), 730, TripStatus.INCOMPLETE),
            new Trip(firstOn, Optional.of(firstOff), 325, TripStatus.COMPLETED),
            new Trip(cancelledOn, Optional.of(cancelledOff), 0, TripStatus.CANCELLED)),
        result.trips());
    assertEquals(1, result.unmatchedOffCount());
  }

  private Tap tap(long id, String timestamp, TapType type, Stop stop) {
    return new Tap(id, Instant.parse(timestamp), type, stop, "Company1", "Bus37", "passenger-a");
  }
}
