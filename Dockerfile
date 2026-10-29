# ---- Stage 1: build the executable JAR ----
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy only the POM first so the dependency layer is cached until pom.xml changes
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ---- Stage 2: slim runtime image ----
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Run as an unprivileged user instead of root
RUN addgroup -S app && adduser -S app -G app

COPY --from=build --chown=app:app /app/target/calculator.jar app.jar

USER app
EXPOSE 8080

ENV JAVA_OPTS=""
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
