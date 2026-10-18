package com.interview.assignment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MainTest {
  @TempDir Path directory;

  @Test
  void writesExpectedOutputForTheExampleInput() throws IOException {
    var output = directory.resolve("trips.csv");

    Main.main(new String[] {"examples/taps.csv", output.toString()});

    assertEquals(Files.readString(Path.of("examples/trips.csv")), Files.readString(output));
  }

  @Test
  void leavesExistingOutputUnchangedWhenInputIsInvalid() throws IOException {
    var input = directory.resolve("taps.csv");
    Files.copy(Path.of("src/test/resources/integration/invalid-timestamp-input.csv"), input);
    var output = directory.resolve("trips.csv");
    var existingOutput = "existing output\n";
    Files.writeString(output, existingOutput);

    assertThrows(
        IOException.class, () -> Main.main(new String[] {input.toString(), output.toString()}));

    assertEquals(existingOutput, Files.readString(output));
  }
}
