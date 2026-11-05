FROM eclipse-temurin:21-jdk AS build
WORKDIR /src

COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline


COPY src src
RUN ./mvnw -B -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app

RUN useradd --create-home appuser
COPY --from=build /src/target/*.jar app.jar

ARG APP_VERSION=1.0.0
ENV APP_VERSION=${APP_VERSION}
# will break it
ENV MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=info
USER appuser
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]