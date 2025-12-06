# Use an official OpenJDK runtime as a parent image
FROM amazoncorretto:17.0.17

# Set the working directory in the container
WORKDIR /app

# Copy the executable JAR file from the target directory to the /app directory in the container
COPY target/auth-session-redis-0.0.1-SNAPSHOT.jar app.jar

# Expose the port that your app runs on
EXPOSE 5060

# Command to run the executable JAR file
ENTRYPOINT ["java", "-jar", "app.jar"]