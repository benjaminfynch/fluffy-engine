# Transit trip processor

Java 21 application that converts tap-on/tap-off CSV records into trips and fares.
Runs as an S3-triggered AWS Lambda, with a Docker-only option for local use.

## Design approach

The solution is structured as a small production-style, event-driven application:
files arrive in S3, trigger a Lambda, and produce output in a separate bucket. The
SAM template defines the infrastructure alongside the code so deployments are
repeatable. Core parsing, matching, and fare calculation remain independent of AWS.

## Run locally

Requires Docker with Compose, such as Docker Desktop. No AWS account is needed.

```sh
cp examples/taps.csv local/input/taps.csv
./local/run.sh
```

The result is written to `local/output/trips.csv`, replacing any previous output.
To process another file in `local/input/`, run `./local/run.sh my-taps.csv`.
Containers are removed automatically; local files are kept and ignored by Git.
The first run downloads images and dependencies.

This runs the existing handler in AWS's Java 21 Lambda container with Floci for S3.
It sends an S3 event explicitly; it does not deploy the SAM stack or test notification wiring.
See [local instructions](local/README.md) for details.

## Tests

Requires Java 21. The Maven wrapper downloads Maven automatically.

```sh
./mvnw verify                 # Unit tests, formatting, lint, and coverage checks
./mvnw -Pintegration verify   # Also runs Floci integration tests; requires Docker
```

The integration tests cover the example file, header-only input, unordered taps,
midnight travel, invalid timestamps, and output-upload failure. They use real SDK
requests to Floci but invoke the handler directly.

JaCoCo reports are generated at `target/site/jacoco/index.html`; the build requires
at least 90% line coverage. PR checks run the build, integration tests, and SAM
template validation as independent jobs.

## Assumptions

- Input may be unordered; taps are sorted by UTC timestamp, then tap ID.
- Trips are matched by PAN, company, and bus. Matching may cross midnight.
- Tap-offs without an open trip are ignored and counted in logs.
- A second tap-on for the same passenger, company, and bus closes the previous
  open trip as incomplete.
- Files are independent: taps are not matched across files. Output is ordered
  by trip start, then tap ID.

See [example input](examples/taps.csv) and [expected output](examples/trips.csv).

## AWS deployment

The repository also has a GitHub deployment workflow using OIDC.
It runs tests and template validation before deploying. Local running and testing
do not require AWS access or deployment configuration.

[`template.yaml`](template.yaml) defines the versioned input bucket, output bucket,
Lambda, permissions, notification, and log group. Stack outputs provide bucket names.
Upload a CSV to `TapInputBucketName` to trigger processing.

The handler reads the exact uploaded version, writes
`<input-key-without-.csv>/<encoded-version-id>/trips.csv` to the output bucket, then
tags the input version `status=processed`. The original input remains in place.
Buckets and logs are retained when the stack is deleted; AWS resources may incur charges.
