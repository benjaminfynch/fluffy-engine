package com.interview.assignment.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.interview.assignment.service.S3Service;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TripFileHandler implements RequestHandler<S3Event, Void> {
  private static final Logger LOGGER = LoggerFactory.getLogger(TripFileHandler.class);
  private static final Injector INJECTOR = Guice.createInjector(new LambdaModule());

  private final S3Service s3Service;

  public TripFileHandler() {
    this(INJECTOR.getInstance(S3Service.class));
  }

  @Inject
  TripFileHandler(S3Service s3Service) {
    this.s3Service = s3Service;
  }

  @Override
  public Void handleRequest(S3Event event, Context context) {
    event
        .getRecords()
        .forEach(
            record -> {
              var bucket = record.getS3().getBucket().getName();
              var key =
                  URLDecoder.decode(record.getS3().getObject().getKey(), StandardCharsets.UTF_8);
              var contents = s3Service.getS3Object(bucket, key).asUtf8String();
              LOGGER.info("Read s3://{}/{}:\n{}", bucket, key, contents);
            });

    return null;
  }
}
