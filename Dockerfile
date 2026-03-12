<<<<<<< HEAD
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app

COPY settings.xml /root/.m2/settings.xml
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
=======
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app

COPY settings.xml /root/.m2/settings.xml
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
>>>>>>> 4ffa9561589c88bb39f5fc3ae2d42d159543398b
