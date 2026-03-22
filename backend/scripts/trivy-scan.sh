#!/bin/bash
set -e

IMAGE=$1

if [ -z "$IMAGE" ]; then
    echo "Usage: trivy-scan.sh <image:tag>"
    exit 1
fi

echo "Scanning $IMAGE..."

# Full report saved as txt
trivy image \
    --exit-code 0 \
    --severity LOW,MEDIUM,HIGH,CRITICAL \
    --format table \
    --output trivy-report.txt \
    "$IMAGE"

# Fail pipeline if HIGH or CRITICAL found
trivy image \
    --exit-code 1 \
    --severity HIGH,CRITICAL \
    --format table \
    "$IMAGE"
