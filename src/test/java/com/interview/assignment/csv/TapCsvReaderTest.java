package com.interview.assignment.csv;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.interview.assignment.model.Stop;
import com.interview.assignment.model.Tap;
import com.interview.assignment.model.TapType;
import java.io.IOException;
import java.io.StringReader;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class TapCsvReaderTest {
  private static final String HEADER = "ID, DateTimeUTC, TapType, StopId, CompanyId, BusID, PAN\n";
  private final TapCsvReader reader = new TapCsvReader();

  @Test
  void readsUtcTimestampsAndSpacesAfterCommas() throws IOException {
    var input = HEADER + "1, 22-01-2023 13:00:00, ON, Stop1, Company1, Bus37, 4111111111111111\n";

    var taps = reader.read(new StringReader(input));

    assertEquals(
        List.of(
            new Tap(
                1,
                Instant.parse("2023-01-22T13:00:00Z"),
                TapType.ON,
                Stop.STOP_1,
                "Company1",
                "Bus37",
                "4111111111111111")),
        taps);
  }

  @Test
  void readsQuotedFieldsContainingCommasAndQuotes() throws IOException {
    var input =
        HEADER
            + "1,22-01-2023 13:00:00,ON,Stop2,\"Transit, \"\"North\"\"\",Bus37,4111111111111111\n";

    var taps = reader.read(new StringReader(input));

    assertEquals("Transit, \"North\"", taps.getFirst().companyId());
  }

  @Test
  void returnsNoTapsForAHeaderOnlyFile() throws IOException {
    assertTrue(reader.read(new StringReader(HEADER)).isEmpty());
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "not-an-id,22-01-2023 13:00:00,ON,Stop1,Company1,Bus37,4111111111111111",
        "1,31-02-2023 13:00:00,ON,Stop1,Company1,Bus37,4111111111111111",
        "1,22-01-2023 13:00:00,UNKNOWN,Stop1,Company1,Bus37,4111111111111111",
        "1,22-01-2023 13:00:00,ON,Stop99,Company1,Bus37,4111111111111111"
      })
  void reportsTheRecordNumberForInvalidTapValues(String row) {
    var exception =
        assertThrows(IOException.class, () -> reader.read(new StringReader(HEADER + row)));

    assertEquals("Invalid tap at CSV record 1", exception.getMessage());
  }
}
