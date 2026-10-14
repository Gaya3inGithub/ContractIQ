# Multi-stage Dockerfile for ContractIQ
# Stage 1: Build WAR with Maven and Java 21
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Cache dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Build application
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime with Apache Tomcat 10.1 and Java 21
FROM tomcat:10.1-jdk21-temurin
LABEL maintainer="ContractIQ Engineering Team"

# Remove default ROOT application
RUN rm -rf /usr/local/tomcat/webapps/*

# Copy WAR artifact as ROOT application for easy base URL access
COPY --from=build /app/target/ContractIQ.war /usr/local/tomcat/webapps/ROOT.war

# Create storage directory for uploads
RUN mkdir -p /app/uploads && chmod 777 /app/uploads

# Runtime configuration. Supply DB_PASSWORD only through the runtime environment.
ENV DB_URL=jdbc:mysql://db:3306/contractiq_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8
ENV DB_USER=root
ENV UPLOAD_DIR=/app/uploads
ENV MAX_FILE_SIZE_MB=15
ENV LLM_API_KEY=""
ENV LLM_API_URL="https://api.openai.com/v1/chat/completions"
ENV LLM_MODEL="gpt-4o-mini"
ENV DEMO_MODE=false
ENV PORT=8080

EXPOSE 8080

CMD ["sh", "-c", "sed -i \"s/port=\\\"8080\\\"/port=\\\"${PORT}\\\"/\" /usr/local/tomcat/conf/server.xml && exec catalina.sh run"]
