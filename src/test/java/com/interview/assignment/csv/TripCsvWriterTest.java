package com.interview.assignment.csv;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.interview.assignment.model.Stop;
import com.interview.assignment.model.Tap;
import com.interview.assignment.model.TapType;
import com.interview.assignment.model.Trip;
import com.interview.assignment.model.TripStatus;
import java.io.IOException;
import java.io.StringWriter;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class TripCsvWriterTest {
  private final TripCsvWriter writer = new TripCsvWriter();

  @Test
  void writesOnlyTheHeaderWhenThereAreNoTrips() throws IOException {
    var output = new StringWriter();

    writer.write(List.of(), output);

    assertEquals(
        "Started,Finished,DurationSecs,FromStopId,ToStopId,ChargeAmount,CompanyId,BusID,PAN,Status\n",
        output.toString());
  }

  @Test
  void leavesFinishedDurationAndDestinationBlankForIncompleteTrips() throws IOException {
    var on =
        new Tap(
            1,
            Instant.parse("2023-01-22T13:00:00Z"),
            TapType.ON,
            Stop.STOP_1,
            "Company1",
            "Bus37",
            "4111111111111111");
    var output = new StringWriter();

    writer.write(List.of(new Trip(on, Optional.empty(), 730, TripStatus.INCOMPLETE)), output);

    assertEquals(
        "22-01-2023 13:00:00,,,Stop1,,$7.30,Company1,Bus37,4111111111111111,INCOMPLETE",
        output.toString().lines().toList().get(1));
  }

  @Test
  void escapesQuotedFieldsAndWritesUtf8Text() throws IOException {
    var on =
        new Tap(
            1,
            Instant.parse("2023-01-22T13:00:00Z"),
            TapType.ON,
            Stop.STOP_1,
            "Transit, \"Nord\"",
            "Bus-é",
            "4111111111111111");
    var off =
        new Tap(
            2,
            Instant.parse("2023-01-22T13:05:00Z"),
            TapType.OFF,
            Stop.STOP_2,
            on.companyId(),
            on.busId(),
            on.pan());
    var output = new StringWriter();

    writer.write(List.of(new Trip(on, Optional.of(off), 325, TripStatus.COMPLETED)), output);

    assertEquals(
        "22-01-2023 13:00:00,22-01-2023 13:05:00,300,Stop1,Stop2,$3.25,\"Transit, \"\"Nord\"\"\",Bus-é,4111111111111111,COMPLETED",
        output.toString().lines().toList().get(1));
  }
}
