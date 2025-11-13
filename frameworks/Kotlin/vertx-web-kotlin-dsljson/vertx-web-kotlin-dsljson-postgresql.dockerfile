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

# --- Runtime stage using Amazon Corretto 25 ---
FROM amazoncorretto:25

WORKDIR /app

COPY --from=builder \
  /vertx-web-kotlin-dsljson/build/libs/vertx-web-kotlin-dsljson-benchmark-1.0.0-SNAPSHOT-fat.jar \
  vertx-web-kotlin-dsljson.jar

EXPOSE 8080

CMD java \
  -server \
  --enable-native-access=ALL-UNNAMED \
  --add-opens=java.base/java.lang=ALL-UNNAMED \
  -Xms2G \
  -Xmx2G \
  -XX:+AlwaysPreTouch \
  -XX:+UseParallelGC \
  -XX:+EnableDynamicAgentLoading \
  -XX:InitialCodeCacheSize=512m \
  -XX:ReservedCodeCacheSize=512m \
  -XX:MaxInlineLevel=20 \
  -XX:+UseNUMA \
  -XX:-UseCodeCacheFlushing \
  -XX:AutoBoxCacheMax=10001 \
  -Djava.net.preferIPv4Stack=true \
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
  -Dio.netty.allocator.type=pooled \
  -Dio.netty.recycler.maxCapacityPerThread=4096 \
  -Dio.netty.recycler.maxSharedCapacityFactor=2 \
  -Dio.netty.recycler.linkCapacity=4096 \
  -Dio.netty.recycler.ratio=8 \
  -Dio.netty.leakDetection.level=disabled \
  -Dio.netty.maxDirectMemory=0 \
  -Dio.netty.threadLocalDirectBufferSize=0 \
  -Dtfb.hasDB=true \
  -jar /app/vertx-web-kotlin-dsljson.jar
