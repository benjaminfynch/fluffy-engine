package com.interview.assignment.service;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

class S3ServiceTest {

  private final S3Client s3ClientMock = mock(S3Client.class);
  private final S3Service s3Service = new S3Service(s3ClientMock);

  @Test
  void returnsTheRequestedObject() {
    var request = GetObjectRequest.builder().bucket("tap-input").key("taps.csv").build();
    var response =
        ResponseBytes.fromByteArray(
            GetObjectResponse.builder().build(), "test contents".getBytes(StandardCharsets.UTF_8));
    when(s3ClientMock.getObject(
            eq(request),
            ArgumentMatchers
                .<ResponseTransformer<GetObjectResponse, ResponseBytes<GetObjectResponse>>>any()))
        .thenReturn(response);

    assertSame(response, s3Service.getS3Object("tap-input", "taps.csv"));
  }
}
