# Layered Spring Boot image.
#
# The build happens on the host (./gradlew buildAll); this Dockerfile only
# unpacks the executable jar into layers so that a code change rebuilds one
# small layer instead of the whole 20 MB jar.
#
# Layering requires the build to run in a JRE that can run `jarmode=tools`.
# The jar bundles spring-boot-jarmode-tools, so no network access is needed
# for the extraction itself.

FROM bellsoft/liberica-openjre-debian:25-cds AS builder
WORKDIR /builder

# Must match `version` in build.gradle. Only one jar exists in build/libs/
# because `jar` is disabled there.
ARG JAR_FILE=build/libs/moc-registry-0.1.0.jar
COPY ${JAR_FILE} application.jar

# Produces extracted/{dependencies,spring-boot-loader,snapshot-dependencies,application}
RUN java -Djarmode=tools -jar application.jar extract --layers --destination extracted

FROM bellsoft/liberica-openjre-debian:25-cds

# curl is not in the base image, but the healthcheck needs an HTTP client.
RUN apt-get update \
 && apt-get install -y --no-install-recommends curl \
 && rm -rf /var/lib/apt/lists/*

WORKDIR /application

# One COPY per layer: from least to most likely to change.
COPY --from=builder /builder/extracted/dependencies/ ./
COPY --from=builder /builder/extracted/spring-boot-loader/ ./
COPY --from=builder /builder/extracted/snapshot-dependencies/ ./
COPY --from=builder /builder/extracted/application/ ./

EXPOSE 8080

HEALTHCHECK --interval=10s --timeout=3s --start-period=30s --retries=12 \
  CMD curl -fsS http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "application.jar"]
