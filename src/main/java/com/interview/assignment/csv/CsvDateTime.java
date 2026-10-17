package com.interview.assignment.csv;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;

final class CsvDateTime {
  static final DateTimeFormatter FORMAT =
      DateTimeFormatter.ofPattern("dd-MM-uuuu HH:mm:ss")
          .withResolverStyle(ResolverStyle.STRICT)
          .withZone(ZoneOffset.UTC);

  private CsvDateTime() {}
}
