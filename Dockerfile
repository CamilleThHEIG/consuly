# Use an official OpenJDK runtime as a parent image
FROM eclipse-temurin:21-jre

# Set the working directory in the container
WORKDIR /app

# Copy the JAR file into the container at /app
COPY target/Consuly-1.0-SNAPSHOT.jar /app/Consuly-1.0-SNAPSHOT.jar

# Expose the port 4446
EXPOSE 4446

ENTRYPOINT ["java", "-jar", "/app/Consuly-1.0-SNAPSHOT.jar"]
CMD ["server"]