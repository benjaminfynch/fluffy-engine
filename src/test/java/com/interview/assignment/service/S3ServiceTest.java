package com.interview.assignment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.GetObjectTaggingRequest;
import software.amazon.awssdk.services.s3.model.GetObjectTaggingResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectTaggingRequest;
import software.amazon.awssdk.services.s3.model.Tag;
import software.amazon.awssdk.services.s3.model.Tagging;

class S3ServiceTest {

  private final S3Client s3ClientMock = mock(S3Client.class);
  private final S3Service s3Service = new S3Service(s3ClientMock);

  @Test
  void returnsTheRequestedObject() {
    var request =
        GetObjectRequest.builder()
            .bucket("tap-input")
            .key("taps.csv")
            .versionId("version-1")
            .build();
    var response =
        ResponseBytes.fromByteArray(
            GetObjectResponse.builder().build(), "test contents".getBytes(StandardCharsets.UTF_8));
    when(s3ClientMock.getObject(
            eq(request),
            ArgumentMatchers
                .<ResponseTransformer<GetObjectResponse, ResponseBytes<GetObjectResponse>>>any()))
        .thenReturn(response);

    assertSame(response, s3Service.getS3Object("tap-input", "taps.csv", "version-1"));
  }

  @Test
  void marksTheRequestedVersionProcessedAndPreservesOtherTags() {
    var existingTag = Tag.builder().key("source").value("test").build();
    var getRequest =
        GetObjectTaggingRequest.builder()
            .bucket("tap-input")
            .key("taps.csv")
            .versionId("version-1")
            .build();
    when(s3ClientMock.getObjectTagging(getRequest))
        .thenReturn(
            GetObjectTaggingResponse.builder()
                .tagSet(List.of(existingTag, Tag.builder().key("status").value("pending").build()))
                .build());

    s3Service.markProcessed("tap-input", "taps.csv", "version-1");

    verify(s3ClientMock)
        .putObjectTagging(
            PutObjectTaggingRequest.builder()
                .bucket("tap-input")
                .key("taps.csv")
                .versionId("version-1")
                .tagging(
                    Tagging.builder()
                        .tagSet(existingTag, Tag.builder().key("status").value("processed").build())
                        .build())
                .build());
  }

  @Test
  void uploadsCsvContentsAsUtf8() throws IOException {
    var request =
        PutObjectRequest.builder()
            .bucket("trip-output")
            .key("taps.csv/trips.csv")
            .contentType("text/csv; charset=utf-8")
            .build();
    var contents = "Company-é\n";

    s3Service.putS3Object("trip-output", "taps.csv/trips.csv", contents);

    var body = ArgumentCaptor.forClass(RequestBody.class);
    verify(s3ClientMock).putObject(eq(request), body.capture());
    try (var stream = body.getValue().contentStreamProvider().newStream()) {
      assertEquals(contents, new String(stream.readAllBytes(), StandardCharsets.UTF_8));
    }
  }
}
