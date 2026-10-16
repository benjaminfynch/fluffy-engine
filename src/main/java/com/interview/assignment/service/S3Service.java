package com.interview.assignment.service;

import com.google.inject.Inject;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

public class S3Service {

    private final S3Client s3Client;

    @Inject
    public S3Service(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    public ResponseBytes<GetObjectResponse> getS3Object(String bucket, String key) {
        var request = GetObjectRequest.builder().bucket(bucket).key(key).build();
        return s3Client.getObject(request, ResponseTransformer.toBytes());
    }
}
