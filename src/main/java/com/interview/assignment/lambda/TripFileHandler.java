package com.interview.assignment.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.interview.assignment.config.LambdaModule;
import com.interview.assignment.service.TripProcessingService;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import org.slf4j.MDC;

public final class TripFileHandler implements RequestHandler<S3Event, Void> {
  private final TripProcessingService tripProcessingService;

  public TripFileHandler() {
    this(Guice.createInjector(new LambdaModule()).getInstance(TripProcessingService.class));
  }

  @Inject
  TripFileHandler(TripProcessingService tripProcessingService) {
    this.tripProcessingService = tripProcessingService;
  }

  @Override
  public Void handleRequest(S3Event event, Context context) {
    MDC.put("correlationId", context.getAwsRequestId());
    for (var record : event.getRecords()) {
      var bucket = record.getS3().getBucket().getName();
      var key = URLDecoder.decode(record.getS3().getObject().getKey(), StandardCharsets.UTF_8);
      var versionId = record.getS3().getObject().getVersionId();
      try {
        tripProcessingService.process(bucket, key, versionId);
      } catch (IOException exception) {
        throw new UncheckedIOException("Failed to process S3 object", exception);
      }
    }
    return null;
  }
}
