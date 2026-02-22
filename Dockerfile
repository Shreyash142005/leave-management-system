# Use official OpenJDK 17 image
FROM eclipse-temurin:17-jdk

# Set working directory
WORKDIR /app

# Copy all project files
COPY . .

# Give permission to mvnw
RUN chmod +x mvnw

# Build the application
RUN ./mvnw clean package -DskipTests

# Expose the port (Render will use PORT env variable)
EXPOSE 8080

# Run the generated jar file
CMD ["java", "-jar", "target/*.jar"]
