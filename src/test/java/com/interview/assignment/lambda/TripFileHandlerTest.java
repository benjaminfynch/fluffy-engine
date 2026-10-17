package com.interview.assignment.lambda;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3BucketEntity;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3Entity;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3EventNotificationRecord;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3ObjectEntity;
import com.interview.assignment.service.S3Service;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.MockMakers;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

class TripFileHandlerTest {
  @Test
  void readsTheObjectUsingTheDecodedKey() {
    var s3Service = mock(S3Service.class, withSettings().mockMaker(MockMakers.SUBCLASS));
    var response =
        ResponseBytes.fromByteArray(
            GetObjectResponse.builder().build(), "test contents".getBytes(StandardCharsets.UTF_8));
    when(s3Service.getS3Object("tap-input", "incoming/tap data.csv")).thenReturn(response);
    var bucket = new S3BucketEntity("tap-input", null, "arn:aws:s3:::tap-input");
    var object = new S3ObjectEntity("incoming%2Ftap+data.csv", 13L, null, null);
    var s3 = new S3Entity("test", bucket, object, "1.0");
    var record =
        new S3EventNotificationRecord(
            "ap-southeast-2",
            "ObjectCreated:Put",
            "aws:s3",
            "2023-01-22T13:00:00Z",
            "2.1",
            null,
            null,
            s3,
            null);
    var event = new S3Event(List.of(record));
    var handler = new TripFileHandler(s3Service);

    assertNull(handler.handleRequest(event, null));

    verify(s3Service).getS3Object("tap-input", "incoming/tap data.csv");
  }

  @Test
  void constructsTheHandlerUsingGuice() {
    var previousRegion = System.getProperty("aws.region");
    try {
      System.setProperty("aws.region", "ap-southeast-2");
      assertNotNull(new TripFileHandler());
    } finally {
      if (previousRegion == null) {
        System.clearProperty("aws.region");
      } else {
        System.setProperty("aws.region", previousRegion);
      }
    }
  }
}
