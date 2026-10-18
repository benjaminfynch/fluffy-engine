package com.interview.assignment;

import com.interview.assignment.config.FareConfiguration;
import com.interview.assignment.csv.TapCsvReader;
import com.interview.assignment.csv.TripCsvWriter;
import com.interview.assignment.model.TripMatchingResult;
import com.interview.assignment.service.FareCalculator;
import com.interview.assignment.service.TripMatchingService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main {
  public static void main(String[] args) throws IOException {
    if (args.length != 2) {
      System.err.println("Usage: Main <taps.csv> <trips.csv>");
      System.exit(2);
    }
    var matcher = new TripMatchingService(new FareCalculator(FareConfiguration.faresInCents()));
    TripMatchingResult result;
    try (var in = Files.newBufferedReader(Path.of(args[0]))) {
      result = matcher.match(new TapCsvReader().read(in));
    }
    try (var out = Files.newBufferedWriter(Path.of(args[1]))) {
      new TripCsvWriter().write(result.trips(), out);
    }
  }
}
