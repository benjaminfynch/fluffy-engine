# Run locally

Requires only Docker with Docker Compose, such as Docker Desktop. No AWS account
or locally installed Java, Maven, SAM CLI, AWS CLI, or Python is needed.

1. Place a CSV file at `local/input/taps.csv`.
2. From the repository root, run:

   ```sh
   ./local/run.sh
   ```

3. Open `local/output/trips.csv`. Each successful run replaces this output file.

For a different input filename:

```sh
./local/run.sh my-taps.csv
```

Use `examples/taps.csv` as a sample input. With that file, the output should match
`examples/trips.csv`.

Compose builds the application in Docker, runs the existing handler in AWS's Java 21
Lambda image, and uses Floci for S3. Small shell helpers upload the file, explicitly
invoke the Lambda with an S3 event, and download the result. This does not deploy the
SAM stack or test automatic S3 notifications. No application business logic is duplicated.

Input and output files are ignored by Git. The wrapper automatically stops and removes
the Compose containers and network after success, failure, or interruption. Floci data
is temporary; your local input and output files are kept. Lambda logs are printed on
failure before cleanup. Images and build caches are kept to speed up subsequent runs.
The first run downloads images and dependencies and will take longer.
