package com.interview.assignment.csv;

import com.interview.assignment.model.Trip;
import java.io.IOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.util.List;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

public final class TripCsvWriter {
  private static final CSVFormat FORMAT =
      CSVFormat.DEFAULT
          .builder()
          .setHeader(
              "Started",
              "Finished",
              "DurationSecs",
              "FromStopId",
              "ToStopId",
              "ChargeAmount",
              "CompanyId",
              "BusID",
              "PAN",
              "Status")
          .setRecordSeparator("\n")
          .get();

  public void write(List<Trip> trips, Writer output) throws IOException {
    try (var printer = new CSVPrinter(output, FORMAT)) {
      for (var trip : trips) {
        writeTrip(trip, printer);
      }
    }
  }

  private void writeTrip(Trip trip, CSVPrinter printer) throws IOException {
    var on = trip.tapOn();
    var off = trip.tapOff();
    var duration = trip.durationSecs();
    printer.printRecord(
        CsvDateTime.FORMAT.format(on.timestamp()),
        off.map(tap -> CsvDateTime.FORMAT.format(tap.timestamp())).orElse(""),
        duration.isPresent() ? Long.toString(duration.getAsLong()) : "",
        on.stop().id(),
        off.map(tap -> tap.stop().id()).orElse(""),
        "$" + BigDecimal.valueOf(trip.chargeInCents(), 2).toPlainString(),
        on.companyId(),
        on.busId(),
        on.pan(),
        trip.status().name());
  }
}
