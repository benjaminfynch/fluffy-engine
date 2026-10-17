package com.interview.assignment.service;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.interview.assignment.csv.TapCsvReader;
import com.interview.assignment.csv.TripCsvWriter;
import com.interview.assignment.model.Tap;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TripProcessingService {
  private static final Logger LOGGER = LoggerFactory.getLogger(TripProcessingService.class);

  private final S3Service s3Service;
  private final TapCsvReader tapCsvReader;
  private final TripMatchingService tripMatchingService;
  private final TripCsvWriter tripCsvWriter;
  private final String outputBucket;

  @Inject
  public TripProcessingService(
      S3Service s3Service,
      TapCsvReader tapCsvReader,
      TripMatchingService tripMatchingService,
      TripCsvWriter tripCsvWriter,
      @Named("outputBucket") String outputBucket) {
    this.s3Service = s3Service;
    this.tapCsvReader = tapCsvReader;
    this.tripMatchingService = tripMatchingService;
    this.tripCsvWriter = tripCsvWriter;
    this.outputBucket = outputBucket;
  }

  public void process(String inputBucket, String inputKey, String versionId) throws IOException {
    if (versionId == null || versionId.isBlank() || versionId.equals("null")) {
      throw new IllegalArgumentException("An S3 object version ID is required");
    }
    LOGGER.info("Processing file {} version {}", inputKey, versionId);

    var taps = getObjectContentsAndMapTaps(inputBucket, inputKey, versionId);

    var matchedTrips = tripMatchingService.match(taps);
    var csvOutput = new StringWriter();
    tripCsvWriter.write(matchedTrips.trips(), csvOutput);

    var outputKey = uploadResultAndMarkProcessed(inputKey, versionId, inputBucket, csvOutput);

    LOGGER.info(
        "Successfully processed {} into s3://{}/{}: trips={}, unmatchedOffCount={}",
        inputKey,
        outputBucket,
        outputKey,
        matchedTrips.trips().size(),
        matchedTrips.unmatchedOffCount());
  }

  private String outputKey(String inputKey, String versionId) {
    var baseKey =
        inputKey.endsWith(".csv") ? inputKey.substring(0, inputKey.length() - 4) : inputKey;
    return baseKey + "/" + URLEncoder.encode(versionId, StandardCharsets.UTF_8) + "/trips.csv";
  }

  private List<Tap> getObjectContentsAndMapTaps(
      String inputBucket, String inputKey, String versionId) throws IOException {
    var contents = s3Service.getS3Object(inputBucket, inputKey, versionId).asUtf8String();
    return tapCsvReader.read(new StringReader(contents));
  }

  private String uploadResultAndMarkProcessed(
      String inputKey, String versionId, String inputBucket, StringWriter output) {
    var outputKey = outputKey(inputKey, versionId);
    s3Service.putS3Object(outputBucket, outputKey, output.toString());
    s3Service.markProcessed(inputBucket, inputKey, versionId);
    return outputKey;
  }
}
