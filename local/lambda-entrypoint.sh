#!/bin/sh
set -eu

address=$(getent ahostsv4 floci)
export AWS_ENDPOINT_URL_S3="http://${address%% *}:4566"
exec /lambda-entrypoint.sh "$@"
