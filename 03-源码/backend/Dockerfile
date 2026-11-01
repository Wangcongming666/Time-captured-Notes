FROM maven:3.9.16-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B -ntp dependency:go-offline
COPY src src
RUN mvn -B -ntp package -DskipTests
FROM eclipse-temurin:21-jre
RUN useradd --system --uid 10001 app && mkdir -p /data/media && chown app /data/media
WORKDIR /app
COPY --from=build /build/target/notes-1.0.0-SNAPSHOT.jar app.jar
USER app
EXPOSE 8080 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
