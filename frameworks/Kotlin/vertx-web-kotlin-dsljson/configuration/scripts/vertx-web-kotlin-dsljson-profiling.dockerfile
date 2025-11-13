# --- Build stage with JDK 25 ---
FROM gradle:9.2-jdk25-corretto AS builder

WORKDIR /vertx-web-kotlin-dsljson

COPY src src
COPY buildSrc buildSrc
COPY build.gradle.kts build.gradle.kts
COPY settings.gradle.kts settings.gradle.kts
COPY gradle.properties gradle.properties
COPY gradle/libs.versions.toml gradle/libs.versions.toml

RUN gradle shadowJar --no-daemon

# --- Async Profiler build stage ---
FROM amazoncorretto:25 AS profiler

RUN dnf install -y git make gcc gcc-c++ libstdc++ libstdc++-static && \
    git clone --depth=1 https://github.com/async-profiler/async-profiler.git /opt/async-profiler && \
    cd /opt/async-profiler && \
    sed -i 's/-source 7 -target 7/-source 8 -target 8/' Makefile && \
    make -j"$(nproc)" && \
    rm -rf /var/cache/dnf

# --- Runtime stage using Amazon Corretto 25 ---
FROM amazoncorretto:25

RUN dnf install -y util-linux procps-ng && dnf clean all

WORKDIR /app

COPY --from=builder \
  /vertx-web-kotlin-dsljson/build/libs/vertx-web-kotlin-dsljson-benchmark-1.0.0-SNAPSHOT-fat.jar \
  vertx-web-kotlin-dsljson.jar

COPY --from=profiler \
  /opt/async-profiler \
  /opt/async-profiler

EXPOSE 8080

CMD java \
    -server \
    --enable-native-access=ALL-UNNAMED \
    --add-opens=java.base/java.lang=ALL-UNNAMED \
    -Xms2G \
    -Xmx2G \
    -XX:+AlwaysPreTouch \
    -XX:+UseParallelGC \
    -XX:+DisableExplicitGC \
    -XX:+EnableDynamicAgentLoading \
    -XX:InitialCodeCacheSize=512m \
    -XX:ReservedCodeCacheSize=512m \
    -XX:MaxInlineLevel=20 \
    -XX:-UseCodeCacheFlushing \
    -XX:AutoBoxCacheMax=10001 \
    -Djava.net.preferIPv4Stack=true \
    -Dvertx.disableMetrics=true \
    -Dvertx.disableDnsResolver=true \
    -Dvertx.disableMetrics=true \
    -Dvertx.disableWebsockets=true \
    -Dvertx.disableContextTimings=true \
    -Dvertx.cacheImmutableHttpResponseHeaders=true \
    -Dvertx.internCommonHttpRequestHeadersToLowerCase=true \
    -Dvertx.disableHttpHeadersValidation=true \
    -Dvertx.eventLoopPoolSize=$((`grep --count ^processor /proc/cpuinfo`)) \
    -Dio.netty.noUnsafe=false \
    -Dio.netty.buffer.checkBounds=false \
    -Dio.netty.buffer.checkAccessible=false \
    -jar /app/vertx-web-kotlin-dsljson.jar
