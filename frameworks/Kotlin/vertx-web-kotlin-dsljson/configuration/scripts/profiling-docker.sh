#!/bin/bash
set -euo pipefail

DURATION=${2:-30}

if [ -f /proc/cpuinfo ]; then
  CPU_COUNT=$(grep --count ^processor /proc/cpuinfo)
else
  CPU_COUNT=$(sysctl -n hw.ncpu)
fi

cleanup() {
  echo "Interrupted. Cleaning up..."
  docker stop vertx-prof >/dev/null 2>&1 || true
  kill -9 "$WRK_PID" 2>/dev/null || true
  exit 1
}

trap cleanup SIGINT

echo "Building image..."
docker build \
  -t vertx-web-kotlin-dsljson-profiling \
  -f configuration/scripts/vertx-web-kotlin-dsljson-profiling.dockerfile \
  .

docker rm -f vertx-prof >/dev/null 2>&1 || true

echo "Starting container..."
docker run -d --name vertx-prof \
  --cap-add=SYS_ADMIN \
  --privileged \
  -p 8080:8080 \
  vertx-web-kotlin-dsljson-profiling >/dev/null

sleep 3

echo "Warming up for 15s..."
wrk \
  -t4 \
  -c512 \
  -d15s \
  -H "Connection: keep-alive" \
  --timeout 8 \
  --latency \
  http://localhost:8080/plaintext

sleep 5

echo "Running wrk for ${DURATION}s..."
wrk \
  -t4 \
  -c512 \
  -d"${DURATION}"s \
  -H "Connection: keep-alive" \
  --timeout 8 \
  --latency \
  http://localhost:8080/plaintext &
WRK_PID=$!

echo "Starting async-profiler..."
docker exec vertx-prof \
  /opt/async-profiler/build/bin/asprof -e cpu -d "$DURATION" -f /app/flame.html $(docker exec vertx-prof pgrep java)

wait "$WRK_PID"

echo "Stopping container..."
docker stop vertx-prof >/dev/null

echo "Copying flamegraph saved to ./flame.html"
docker cp vertx-prof:/app/flame.html .
