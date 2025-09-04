FROM maven:3.9.11-eclipse-temurin-21-alpine AS builder

WORKDIR /opt/app

COPY pom.xml .
RUN mvn dependency:go-offline

COPY src ./src

RUN mvn clean package -DskipTests



FROM eclipse-temurin:21

WORKDIR /opt/app

COPY --from=builder /opt/app/target/cloudfilestorage.jar cloudfilestorage.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "cloudfilestorage.jar"]