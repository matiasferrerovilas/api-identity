# ---- Build stage: compila el binario nativo (glibc, dinámico) ----
FROM ghcr.io/graalvm/native-image-community:25 AS build

WORKDIR /workspace

COPY gradlew build.gradle settings.gradle ./
COPY gradle/ gradle/
RUN ./gradlew --no-daemon --version

COPY checkstyle/ checkstyle/
COPY src/ src/

RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew --no-daemon nativeCompile

# ---- Libs stage: libz.so.1 compatible con la glibc de distroless (Debian 12) ----
FROM debian:12-slim AS libs
RUN apt-get update && apt-get install -y --no-install-recommends zlib1g \
    && rm -rf /var/lib/apt/lists/*

# ---- Runtime stage: solo el binario nativo, sin JVM ----
FROM gcr.io/distroless/base-debian12:nonroot

WORKDIR /app

COPY --from=libs /usr/lib/aarch64-linux-gnu/libz.so.1 /usr/lib/aarch64-linux-gnu/libz.so.1
COPY --from=build /workspace/build/native/nativeCompile/application ./application

ENV SPRING_PROFILES_ACTIVE=prod

EXPOSE 8080

ENTRYPOINT ["/app/application"]
