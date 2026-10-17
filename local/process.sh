#!/bin/sh
set -eu

input="/input/${1:-taps.csv}"
[ -f "$input" ] || { echo "Place your input CSV at local/input/${1:-taps.csv}" >&2; exit 1; }

aws_local() {
  aws --endpoint-url http://floci:4566 "$@"
}

for bucket in tap-input trip-output; do
  if ! aws_local s3api head-bucket --bucket "$bucket" >/dev/null 2>&1; then
    aws_local s3api create-bucket --bucket "$bucket" >/dev/null
  fi
done
aws_local s3api put-bucket-versioning --bucket tap-input --versioning-configuration Status=Enabled
version=$(aws_local s3api put-object --bucket tap-input --key taps.csv --body "$input" \
  --query VersionId --output text)

jq -n --arg version "$version" '{Records: [{
  eventVersion: "2.1", eventSource: "aws:s3", awsRegion: "ap-southeast-2",
  eventName: "ObjectCreated:Put", eventTime: "2023-01-24T16:30:00Z",
  s3: {s3SchemaVersion: "1.0", configurationId: "local",
    bucket: {name: "tap-input", arn: "arn:aws:s3:::tap-input"},
    object: {key: "taps.csv", versionId: $version}}
}]}' >/tmp/event.json

curl --silent --show-error --fail-with-body --max-time 60 \
  -H 'Content-Type: application/json' --data-binary @/tmp/event.json \
  http://lambda:8080/2015-03-31/functions/function/invocations >/tmp/response.json
if ! jq -e '. == null' /tmp/response.json >/dev/null; then
  cat /tmp/response.json >&2
  exit 1
fi

aws_local s3api get-object --bucket trip-output --key "taps/$version/trips.csv" \
  /output/trips.csv >/dev/null
echo "Output written to local/output/trips.csv"
