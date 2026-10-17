package com.interview.assignment.csv;

import com.interview.assignment.model.Stop;
import com.interview.assignment.model.Tap;
import com.interview.assignment.model.TapType;
import java.io.IOException;
import java.io.Reader;
import java.time.DateTimeException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

public final class TapCsvReader {
  private static final CSVFormat FORMAT =
      CSVFormat.DEFAULT
          .builder()
          .setHeader()
          .setSkipHeaderRecord(true)
          .setIgnoreSurroundingSpaces(true)
          .get();

  public List<Tap> read(Reader input) throws IOException {
    try (var parser = FORMAT.parse(input)) {
      var taps = new ArrayList<Tap>();
      for (var record : parser.getRecords()) {
        taps.add(parseTap(record));
      }
      return List.copyOf(taps);
    }
  }

  private Tap parseTap(CSVRecord record) throws IOException {
    try {
      return new Tap(
          Long.parseLong(record.get("ID")),
          Instant.from(CsvDateTime.FORMAT.parse(record.get("DateTimeUTC"))),
          TapType.valueOf(record.get("TapType")),
          Stop.fromId(record.get("StopId")),
          record.get("CompanyId"),
          record.get("BusID"),
          record.get("PAN"));
    } catch (IllegalArgumentException | DateTimeException exception) {
      throw new IOException("Invalid tap at CSV record " + record.getRecordNumber(), exception);
    }
  }
}
