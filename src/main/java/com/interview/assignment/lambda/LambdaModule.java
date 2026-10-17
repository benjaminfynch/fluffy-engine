package com.interview.assignment.lambda;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.interview.assignment.config.FareConfiguration;
import com.interview.assignment.service.FareCalculator;
import com.interview.assignment.service.TripMatchingService;
import software.amazon.awssdk.services.s3.S3Client;

public final class LambdaModule extends AbstractModule {
  private final String outputBucket;

  public LambdaModule() {
    this(System.getenv("TRIP_OUTPUT_BUCKET"));
  }

  LambdaModule(String outputBucket) {
    this.outputBucket = outputBucket;
  }

  @Provides
  @Named("outputBucket")
  String provideOutputBucket() {
    if (outputBucket == null || outputBucket.isBlank()) {
      throw new IllegalStateException("TRIP_OUTPUT_BUCKET must be configured");
    }
    return outputBucket;
  }

  @Provides
  @Singleton
  TripMatchingService provideTripMatchingService(FareCalculator fareCalculator) {
    return new TripMatchingService(fareCalculator);
  }

  @Provides
  @Singleton
  FareCalculator provideFareCalculator() {
    return new FareCalculator(FareConfiguration.faresInCents());
  }

  @Provides
  @Singleton
  S3Client provideS3Client() {
    return S3Client.builder().build();
  }
}
