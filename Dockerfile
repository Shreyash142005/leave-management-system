FROM maven:3.9.6-eclipse-temurin-17

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jdk

WORKDIR /app

COPY --from=0 /app/target/*.jar app.jar

EXPOSE 8080

CMD ["java", "-jar", "app.jar"]
