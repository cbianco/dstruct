ARG MAVEN_PROFILE="default"
# Build stage: compile the application
FROM maven:3.9-eclipse-temurin-25 AS build
ARG MAVEN_PROFILE
WORKDIR /build
# Copy pom.xml first to leverage Docker layer caching
COPY pom.xml .
RUN mvn dependency:go-offline -P ${MAVEN_PROFILE} -B

# Copy source code and build
COPY src ./src
RUN mvn package -DskipTests -P ${MAVEN_PROFILE} -B

# Runtime stage: minimal JRE image
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

# Create non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Copy only the compiled JAR from build stage
COPY --from=build /build/target/dstruct-*.jar dstruct.jar

# Document the exposed port
EXPOSE 4242

# Run the application
CMD ["java", "-cp", "dstruct.jar", "dev.dstruct.Main"]