package com.interview.assignment.service;

import com.google.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.GetObjectTaggingRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectTaggingRequest;
import software.amazon.awssdk.services.s3.model.Tag;
import software.amazon.awssdk.services.s3.model.Tagging;

public class S3Service {

  private final S3Client s3Client;

  @Inject
  public S3Service(S3Client s3Client) {
    this.s3Client = s3Client;
  }

  public ResponseBytes<GetObjectResponse> getS3Object(String bucket, String key, String versionId) {
    var request = GetObjectRequest.builder().bucket(bucket).key(key).versionId(versionId).build();
    return s3Client.getObject(request, ResponseTransformer.toBytes());
  }

  public void putS3Object(String bucket, String key, String contents) {
    var request =
        PutObjectRequest.builder()
            .bucket(bucket)
            .key(key)
            .contentType("text/csv; charset=utf-8")
            .build();
    s3Client.putObject(request, RequestBody.fromString(contents, StandardCharsets.UTF_8));
  }

  public void markProcessed(String bucket, String key, String versionId) {
    var getRequest =
        GetObjectTaggingRequest.builder().bucket(bucket).key(key).versionId(versionId).build();
    var tags = new ArrayList<>(s3Client.getObjectTagging(getRequest).tagSet());
    tags.removeIf(tag -> tag.key().equals("status"));
    tags.add(Tag.builder().key("status").value("processed").build());

    var putRequest =
        PutObjectTaggingRequest.builder()
            .bucket(bucket)
            .key(key)
            .versionId(versionId)
            .tagging(Tagging.builder().tagSet(tags).build())
            .build();
    s3Client.putObjectTagging(putRequest);
  }
}
