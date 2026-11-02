FROM maven:3.9.16-eclipse-temurin-21 AS build
WORKDIR /build
ARG MAVEN_OPTS
ENV MAVEN_OPTS=${MAVEN_OPTS}
COPY pom.xml .
COPY src src
RUN --mount=type=cache,target=/root/.m2 mvn -B -ntp package -DskipTests
FROM eclipse-temurin:21-jre
RUN apt-get update && apt-get install -y --no-install-recommends ffmpeg && rm -rf /var/lib/apt/lists/*
RUN useradd --system --uid 10001 app && mkdir -p /data/media && chown app /data/media
WORKDIR /app
COPY --from=build /build/target/notes-1.0.0-SNAPSHOT.jar app.jar
USER app
EXPOSE 8080 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
