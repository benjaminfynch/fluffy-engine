package com.interview.assignment.lambda;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.interview.assignment.config.FareConfiguration;
import com.interview.assignment.service.FareCalculator;
import com.interview.assignment.service.TripMatchingService;
import software.amazon.awssdk.services.s3.S3Client;

public final class LambdaModule extends AbstractModule {
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
