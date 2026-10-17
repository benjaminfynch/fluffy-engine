package com.interview.assignment.config;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.interview.assignment.service.FareCalculator;
import com.interview.assignment.service.TripMatchingService;
import software.amazon.awssdk.services.s3.S3Client;

public final class LambdaModule extends AbstractModule {
  private final String outputBucket;

  public LambdaModule() {
    this(System.getenv("TRIP_OUTPUT_BUCKET"));
  }

  public LambdaModule(String outputBucket) {
    this.outputBucket = outputBucket;
  }

  @Provides
  @Named("outputBucket")
  public String provideOutputBucket() {
    if (outputBucket == null || outputBucket.isBlank()) {
      throw new IllegalStateException("TRIP_OUTPUT_BUCKET must be configured");
    }
    return outputBucket;
  }

  @Provides
  @Singleton
  public TripMatchingService provideTripMatchingService(FareCalculator fareCalculator) {
    return new TripMatchingService(fareCalculator);
  }

  @Provides
  @Singleton
  public FareCalculator provideFareCalculator() {
    return new FareCalculator(FareConfiguration.faresInCents());
  }

  @Provides
  @Singleton
  public S3Client provideS3Client() {
    return S3Client.builder().build();
  }
}
