#!/bin/bash
set -e

IMAGE=$1

if [ -z "$IMAGE" ]; then
    echo "Usage: trivy-scan.sh <image:tag>"
    exit 1
fi

echo "Scanning $IMAGE..."

# LOW, MEDIUM — report only
trivy image --exit-code 0 --severity LOW,MEDIUM --format table "$IMAGE"

# HIGH, CRITICAL — fail pipeline
trivy image --exit-code 1 --severity HIGH,CRITICAL --format table "$IMAGE"

# Full report saved as txt
trivy image \
    --exit-code 0 \
    --severity LOW,MEDIUM,HIGH,CRITICAL \
    --format table \
    --output trivy-report.txt \
    "$IMAGE" || true
