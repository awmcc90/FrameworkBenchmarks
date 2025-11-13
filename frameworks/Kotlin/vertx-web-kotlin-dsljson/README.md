# Vert.x-Web Kotlin Dsljson Benchmarking Test

Vert.x-Web in Kotlin with Dsljson serialization

The code is written as a realistic server implementation:
- Code is organized logically into packages
- Repositories are created for each database entity to handler all operations pertaining to a specific table
- Handlers map to the logical entities which they serve
- JSON serialization is provided via Dsljson
  - Doing this effectively required a custom Vert.x Buffer implementation that also extended OutputStream in order to rely on the efficient Vert.x heap memory pool instead of building a novel implementation.

## Test URLs

### JSON

http://localhost:8080/json

### PLAINTEXT

http://localhost:8080/plaintext

### DB

http://localhost:8080/db

### QUERY

http://localhost:8080/query?queries=

### UPDATE

http://localhost:8080/update?queries=

### FORTUNES

http://localhost:8080/fortunes


Docker Profiling:
### Build

```shell
docker build -t vertx-web-kotlin-dsljson-profiling -f vertx-web-kotlin-dsljson-profiling.dockerfile .
```

### Profiling
```shell
docker exec -it $(docker ps -qf name=vertx-web-kotlin-dsljson-profiling) \
  /opt/async-profiler/profiler.sh -e cpu -d 30 -f /app/flame.html $(pgrep java)
```