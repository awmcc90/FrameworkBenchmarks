#!/bin/bash

PID=${1:-$(pgrep -n -f 'java.*vertx-web-kotlin-dsljson-benchmark')}
DURATION=${2:-10}
OUTPUT=${3:-flamegraph.html}
ASYNC_PROFILER_DIR=~/async-profiler/build/bin
PROFILER="$ASYNC_PROFILER_DIR/asprof"

if [ -z "$PID" ]; then
  echo "Usage: $0 <pid> [duration] [output.html]"
  exit 1
fi

if [ -f /proc/cpuinfo ]; then
  CPU_COUNT=$(grep --count ^processor /proc/cpuinfo)
else
  CPU_COUNT=$(sysctl -n hw.ncpu)
fi

cleanup() {
  echo "Interrupted. Cleaning up..."
  kill -9 "$WRK_PID" "$PROFILER_PID" 2>/dev/null
  exit 1
}

trap cleanup SIGINT

# Start wrk with higher concurrency and pipelining
# Add pipelining with: -s ./configuration/scripts/pipeline.lua \
wrk \
  -t"$CPU_COUNT" \
  -c512 \
  -d"${DURATION}"s \
  -H "Connection: keep-alive" \
  --timeout 8 \
  --latency \
  http://localhost:8080/plaintext &
WRK_PID=$!

# Start async profiler
"$PROFILER" -f "$OUTPUT" -d "$DURATION" "$PID" &
PROFILER_PID=$!

wait "$WRK_PID" "$PROFILER_PID"
echo "Flamegraph saved to $OUTPUT"
