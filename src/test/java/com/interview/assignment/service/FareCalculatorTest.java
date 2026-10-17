package com.interview.assignment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.interview.assignment.config.FareConfiguration;
import com.interview.assignment.model.Stop;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class FareCalculatorTest {
  private final FareCalculator calculator = new FareCalculator(FareConfiguration.faresInCents());

  @Test
  void rejectsACompletedTripWithoutAConfiguredFare() {
    var calculator = new FareCalculator(Map.of(Set.of(Stop.STOP_1, Stop.STOP_2), 325));

    var exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> calculator.completedFareInCents(Stop.STOP_1, Stop.STOP_3));

    assertEquals("No fare configured between STOP_1 and STOP_3", exception.getMessage());
  }

  @Test
  void rejectsAnIncompleteTripWithoutAnyFaresFromItsStartingStop() {
    var calculator = new FareCalculator(Map.of(Set.of(Stop.STOP_1, Stop.STOP_2), 325));

    var exception =
        assertThrows(
            IllegalArgumentException.class, () -> calculator.incompleteFareInCents(Stop.STOP_3));

    assertEquals("No fares configured from STOP_3", exception.getMessage());
  }

  @Test
  void rejectsNullStops() {
    assertThrows(
        NullPointerException.class, () -> calculator.completedFareInCents(null, Stop.STOP_1));
    assertThrows(
        NullPointerException.class, () -> calculator.completedFareInCents(Stop.STOP_1, null));
    assertThrows(NullPointerException.class, () -> calculator.incompleteFareInCents(null));
  }

  @Test
  void usesTheSuppliedFareScheduleForCompletedAndIncompleteTrips() {
    var calculator =
        new FareCalculator(
            Map.of(
                Set.of(Stop.STOP_1, Stop.STOP_2), 400,
                Set.of(Stop.STOP_1, Stop.STOP_3), 900));

    assertEquals(400, calculator.completedFareInCents(Stop.STOP_2, Stop.STOP_1));
    assertEquals(900, calculator.incompleteFareInCents(Stop.STOP_1));
  }

  @ParameterizedTest
  @CsvSource({
    "STOP_1, STOP_2, 325",
    "STOP_2, STOP_1, 325",
    "STOP_2, STOP_3, 550",
    "STOP_3, STOP_2, 550",
    "STOP_1, STOP_3, 730",
    "STOP_3, STOP_1, 730"
  })
  void chargesThePublishedFareInEitherDirection(Stop from, Stop to, int expectedCents) {
    assertEquals(expectedCents, calculator.completedFareInCents(from, to));
  }

  @ParameterizedTest
  @CsvSource({"STOP_1", "STOP_2", "STOP_3"})
  void chargesNothingForTravelToTheSameStop(Stop stop) {
    assertEquals(0, calculator.completedFareInCents(stop, stop));
  }

  @ParameterizedTest
  @CsvSource({"STOP_1, 730", "STOP_2, 550", "STOP_3, 730"})
  void chargesTheMaximumPossibleFareForAnIncompleteTrip(Stop from, int expectedCents) {
    assertEquals(expectedCents, calculator.incompleteFareInCents(from));
  }
}
