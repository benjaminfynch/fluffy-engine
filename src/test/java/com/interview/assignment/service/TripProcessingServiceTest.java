package com.interview.assignment.service;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.interview.assignment.csv.TapCsvReader;
import com.interview.assignment.csv.TripCsvWriter;
import com.interview.assignment.model.Tap;
import com.interview.assignment.model.Trip;
import com.interview.assignment.model.TripMatchingResult;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

class TripProcessingServiceTest {
  private final S3Service s3Service = mock(S3Service.class);
  private final TapCsvReader reader = mock(TapCsvReader.class);
  private final TripMatchingService matcher = mock(TripMatchingService.class);
  private final TripCsvWriter writer = mock(TripCsvWriter.class);
  private final TripProcessingService service =
      new TripProcessingService(s3Service, reader, matcher, writer, "trip-output");

  @ParameterizedTest
  @CsvSource({
    "batch/taps.csv, version-1, batch/taps/version-1/trips.csv",
    "batch/upload, version/with+symbols, batch/upload/version%2Fwith%2Bsymbols/trips.csv"
  })
  void uploadsToTheSameOutputKeyWhenProcessingTheSameInputVersionTwice(
      String inputKey, String versionId, String outputKey) throws IOException {
    var response =
        ResponseBytes.fromByteArray(
            GetObjectResponse.builder().build(), "input csv".getBytes(StandardCharsets.UTF_8));
    when(s3Service.getS3Object("tap-input", inputKey, versionId)).thenReturn(response);
    var taps = List.of(mock(Tap.class));
    var trips = List.of(mock(Trip.class));
    when(reader.read(any(Reader.class))).thenReturn(taps);
    when(matcher.match(taps)).thenReturn(new TripMatchingResult(trips, 1));
    doAnswer(
            invocation -> {
              invocation.getArgument(1, Writer.class).write("output csv");
              return null;
            })
        .when(writer)
        .write(eq(trips), any(Writer.class));

    service.process("tap-input", inputKey, versionId);
    service.process("tap-input", inputKey, versionId);

    verify(s3Service, times(2)).putS3Object("trip-output", outputKey, "output csv");
    var ordered = inOrder(s3Service);
    ordered.verify(s3Service).putS3Object("trip-output", outputKey, "output csv");
    ordered.verify(s3Service).markProcessed("tap-input", inputKey, versionId);
    ordered.verify(s3Service).putS3Object("trip-output", outputKey, "output csv");
    ordered.verify(s3Service).markProcessed("tap-input", inputKey, versionId);
  }

  @Test
  void doesNotMarkProcessedWhenUploadFails() throws IOException {
    when(s3Service.getS3Object("tap-input", "taps.csv", "version-1"))
        .thenReturn(ResponseBytes.fromByteArray(GetObjectResponse.builder().build(), new byte[0]));
    when(reader.read(any(Reader.class))).thenReturn(List.of());
    when(matcher.match(List.of())).thenReturn(new TripMatchingResult(List.of(), 0));
    var failure = new IllegalStateException("Upload failed");
    doThrow(failure).when(s3Service).putS3Object("trip-output", "taps/version-1/trips.csv", "");

    assertSame(
        failure,
        assertThrows(
            IllegalStateException.class,
            () -> service.process("tap-input", "taps.csv", "version-1")));

    verify(s3Service, never()).markProcessed(any(), any(), any());
  }

  @Test
  void propagatesDownloadFailuresWithoutProcessingOrUploading() {
    var failure = new IllegalStateException("S3 unavailable");
    when(s3Service.getS3Object("tap-input", "taps.csv", "version-1")).thenThrow(failure);

    assertSame(
        failure,
        assertThrows(
            IllegalStateException.class,
            () -> service.process("tap-input", "taps.csv", "version-1")));

    verifyNoInteractions(reader, matcher, writer);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" ", "null"})
  void rejectsMissingVersionIdsBeforeAccessingS3(String versionId) {
    assertThrows(
        IllegalArgumentException.class, () -> service.process("tap-input", "taps.csv", versionId));

    verifyNoInteractions(s3Service, reader, matcher, writer);
  }
}
