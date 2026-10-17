package com.interview.assignment.lambda;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.google.inject.Guice;
import com.google.inject.Key;
import com.google.inject.name.Names;
import com.google.inject.util.Modules;
import com.interview.assignment.service.FareCalculator;
import com.interview.assignment.service.TripMatchingService;
import com.interview.assignment.service.TripProcessingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;

class LambdaModuleTest {
  @Test
  void providesCalculatorAndMatchingServiceAsSingletons() {
    var injector = Guice.createInjector(new LambdaModule());

    var calculator = injector.getInstance(FareCalculator.class);
    var matchingService = injector.getInstance(TripMatchingService.class);

    assertSame(calculator, injector.getInstance(FareCalculator.class));
    assertSame(matchingService, injector.getInstance(TripMatchingService.class));
  }

  @Test
  void wiresTheProcessorWithTheConfiguredOutputBucket() {
    var s3Client = mock(S3Client.class);
    var module =
        Modules.override(new LambdaModule("trip-output"))
            .with(binder -> binder.bind(S3Client.class).toInstance(s3Client));
    var injector = Guice.createInjector(module);

    assertNotNull(injector.getInstance(TripProcessingService.class));
    assertEquals(
        "trip-output", injector.getInstance(Key.get(String.class, Names.named("outputBucket"))));
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = " ")
  void requiresAnOutputBucket(String bucket) {
    var module = new LambdaModule(bucket);

    var exception = assertThrows(IllegalStateException.class, module::provideOutputBucket);

    assertEquals("TRIP_OUTPUT_BUCKET must be configured", exception.getMessage());
  }

  @Test
  void providesTheS3Client() {
    var builder = mock(S3ClientBuilder.class);
    var client = mock(S3Client.class);
    when(builder.build()).thenReturn(client);
    try (var s3 = mockStatic(S3Client.class)) {
      s3.when(S3Client::builder).thenReturn(builder);

      assertSame(client, new LambdaModule("trip-output").provideS3Client());
    }
  }
}
