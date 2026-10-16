package com.interview.assignment.lambda;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.interview.assignment.service.S3Service;
import software.amazon.awssdk.services.s3.S3Client;

public final class LambdaModule extends AbstractModule {
    @Provides
    @Singleton
    S3Client provideS3Client() {
        return S3Client.builder().build();
    }
}
