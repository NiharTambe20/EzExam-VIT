# =============================================================================
# Stage 1: Build the Maven WAR application using Java 17
# =============================================================================
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy Maven descriptor and source code
COPY pom.xml ./
COPY src ./src

# Package the Web Application Archive (WAR)
# Skips unit tests during image build (tests run in CI/pre-commit verification)
RUN mvn clean package -DskipTests

# =============================================================================
# Stage 2: Apache Tomcat 10.1 Runtime Environment
# =============================================================================
FROM tomcat:10.1.34-jdk17-temurin
WORKDIR /usr/local/tomcat

# Remove default sample web applications (ROOT, docs, examples, manager)
RUN rm -rf webapps/*

# Deploy the application as ROOT.war so endpoints are mapped directly to /api/*
# (eliminating the /ezexam context prefix)
COPY --from=build /app/target/ezexam.war webapps/ROOT.war

# Default fallback port (Render injects its own PORT environment variable at runtime)
ENV PORT=8080
EXPOSE 8080

# Dynamically substitute the port in conf/server.xml and start Tomcat in foreground
CMD ["sh", "-c", "sed -i \"s/port=\\\"8080\\\"/port=\\\"${PORT:-8080}\\\"/g\" conf/server.xml && catalina.sh run"]
