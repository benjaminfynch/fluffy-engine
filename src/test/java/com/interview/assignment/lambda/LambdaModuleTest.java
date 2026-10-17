package com.interview.assignment.lambda;

import static org.junit.jupiter.api.Assertions.assertSame;

import com.google.inject.Guice;
import com.interview.assignment.service.FareCalculator;
import com.interview.assignment.service.TripMatchingService;
import org.junit.jupiter.api.Test;

class LambdaModuleTest {
  @Test
  void providesCalculatorAndMatchingServiceAsSingletons() {
    var injector = Guice.createInjector(new LambdaModule());

    var calculator = injector.getInstance(FareCalculator.class);
    var matchingService = injector.getInstance(TripMatchingService.class);

    assertSame(calculator, injector.getInstance(FareCalculator.class));
    assertSame(matchingService, injector.getInstance(TripMatchingService.class));
  }
}
