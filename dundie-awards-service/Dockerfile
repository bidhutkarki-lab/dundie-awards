FROM gradle:9.7.1-jdk17 AS build
WORKDIR /workspace
COPY settings.gradle build.gradle ./
COPY src ./src
RUN gradle bootJar --no-daemon

FROM eclipse-temurin:17-jre AS runtime
WORKDIR /app
RUN useradd --system --create-home --shell /usr/sbin/nologin spring
COPY --from=build /workspace/build/libs/*.jar app.jar
USER spring
EXPOSE 3000
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
