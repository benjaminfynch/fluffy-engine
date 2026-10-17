package com.interview.assignment.lambda;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3BucketEntity;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3Entity;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3EventNotificationRecord;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3ObjectEntity;
import com.google.inject.Guice;
import com.google.inject.util.Modules;
import com.interview.assignment.config.LambdaModule;
import io.floci.testcontainers.FlociContainer;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.BucketVersioningStatus;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.Tag;

class TripFileHandlerFlociIT {
  private final FlociContainer floci = new FlociContainer("floci/floci:2.3.0");
  private S3Client s3Client;
  private TripFileHandler handler;

  @BeforeEach
  void setUp() {
    floci.start();
    s3Client =
        S3Client.builder()
            .endpointOverride(URI.create(floci.getEndpoint()))
            .region(Region.of(floci.getRegion()))
            .credentialsProvider(
                StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")))
            .forcePathStyle(true)
            .build();
    s3Client.createBucket(request -> request.bucket("tap-input"));
    s3Client.createBucket(request -> request.bucket("trip-output"));
    s3Client.putBucketVersioning(
        request ->
            request
                .bucket("tap-input")
                .versioningConfiguration(config -> config.status(BucketVersioningStatus.ENABLED)));
    handler = createHandler("trip-output");
  }

  @AfterEach
  void tearDown() {
    try {
      if (s3Client != null) {
        s3Client.close();
      }
    } finally {
      floci.stop();
    }
  }

  @Test
  void processesUploadedExampleAndStoresExpectedOutputAndProcessedTag() throws IOException {
    assertProcessesCsv(
        Files.readString(Path.of("examples/taps.csv")),
        Files.readString(Path.of("examples/trips.csv")));
  }

  @Test
  void writesOnlyTheHeaderForAHeaderOnlyInputFile() throws IOException {
    assertProcessesFixture("header-only");
  }

  @Test
  void sortsUnorderedTapsBeforeMatchingTrips() throws IOException {
    assertProcessesFixture("unsorted");
  }

  @Test
  void processesATripAcrossMidnight() throws IOException {
    assertProcessesFixture("midnight");
  }

  @Test
  void rejectsAnInvalidTimestampWithoutOutputOrProcessedTag() throws IOException {
    var versionId = uploadInput(fixture("invalid-timestamp-input.csv"));

    var exception =
        assertThrows(
            UncheckedIOException.class, () -> handler.handleRequest(event(versionId), null));

    assertEquals("Invalid tap at CSV record 1", exception.getCause().getMessage());
    assertNoOutputOrProcessedTag(versionId);
  }

  @Test
  void propagatesUploadFailureWithoutOutputOrProcessedTag() throws IOException {
    var versionId = uploadInput(Files.readString(Path.of("examples/taps.csv")));
    var handlerWithMissingOutputBucket = createHandler("missing-output");

    var exception =
        assertThrows(
            S3Exception.class,
            () -> handlerWithMissingOutputBucket.handleRequest(event(versionId), null));

    assertEquals("NoSuchBucket", exception.awsErrorDetails().errorCode());
    assertNoOutputOrProcessedTag(versionId);
  }

  private TripFileHandler createHandler(String outputBucket) {
    var module =
        Modules.override(new LambdaModule(outputBucket))
            .with(binder -> binder.bind(S3Client.class).toInstance(s3Client));
    return Guice.createInjector(module).getInstance(TripFileHandler.class);
  }

  private void assertProcessesFixture(String name) throws IOException {
    assertProcessesCsv(fixture(name + "-input.csv"), fixture(name + "-output.csv"));
  }

  private String fixture(String name) throws IOException {
    try (var input = getClass().getResourceAsStream("/integration/" + name)) {
      assertNotNull(input, "Missing fixture: " + name);
      return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  private void assertProcessesCsv(String input, String expectedOutput) {
    var versionId = uploadInput(input);

    handler.handleRequest(event(versionId), null);

    var outputKey =
        "incoming/tap data/" + URLEncoder.encode(versionId, StandardCharsets.UTF_8) + "/trips.csv";
    var output = s3Client.getObjectAsBytes(request -> request.bucket("trip-output").key(outputKey));
    assertEquals(expectedOutput, output.asUtf8String());
    assertEquals(
        List.of(Tag.builder().key("status").value("processed").build()), tagsFor(versionId));
  }

  private String uploadInput(String contents) {
    var upload =
        s3Client.putObject(
            request -> request.bucket("tap-input").key("incoming/tap data.csv"),
            RequestBody.fromString(contents, StandardCharsets.UTF_8));
    assertNotNull(upload.versionId());
    return upload.versionId();
  }

  private List<Tag> tagsFor(String versionId) {
    return s3Client
        .getObjectTagging(
            request ->
                request.bucket("tap-input").key("incoming/tap data.csv").versionId(versionId))
        .tagSet();
  }

  private void assertNoOutputOrProcessedTag(String versionId) {
    assertTrue(
        s3Client.listObjectsV2(request -> request.bucket("trip-output")).contents().isEmpty());
    assertEquals(List.of(), tagsFor(versionId));
  }

  private S3Event event(String versionId) {
    var bucket = new S3BucketEntity("tap-input", null, "arn:aws:s3:::tap-input");
    var object = new S3ObjectEntity("incoming%2Ftap+data.csv", null, null, versionId, null);
    var s3 = new S3Entity("test", bucket, object, "1.0");
    var record =
        new S3EventNotificationRecord(
            "ap-southeast-2",
            "ObjectCreated:Put",
            "aws:s3",
            "2023-01-24T16:30:00Z",
            "2.1",
            null,
            null,
            s3,
            null);
    return new S3Event(List.of(record));
  }
}
