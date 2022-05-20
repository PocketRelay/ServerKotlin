# Gradle caching layer for faster rebuilds
FROM gradle:7.4.2-jdk17-alpine AS cache
RUN mkdir -p /home/gradle/cache_home
ENV GRADLE_USER_HOME /home/gradle/cache_home
COPY build.gradle.kts gradle.properties settings.gradle.kts /home/gradle/app/
WORKDIR /home/gradle/app
RUN gradle clean build -i --stacktrace

# Building with gradle 7.4.2 and JDK 17
FROM gradle:7.4.2-jdk17-alpine AS build
# Copy cached gradle dependencies
COPY --from=cache /home/gradle/cache_home /home/gradle/.gradle

# Copy the local source code to a folder on the image
COPY --chown=gradle:gradle . /home/gradle/src

# Set the working directory
WORKDIR /home/gradle/src

# Build the shadow JAR
RUN gradle shadowJar --no-daemon

# Run step uses OpenJDK 17 alpine
FROM openjdk:17-alpine AS run

# Create an app directory
RUN mkdir "/app"

# Set the working directory
WORKDIR /app

# Copy the compiled JAR from the build step
COPY --from=build /home/gradle/src/build/libs/server.jar server.jar

# Exposing the ports for all the servers
EXPOSE 42127
EXPOSE 14219
EXPOSE 8999
EXPOSE 9988
EXPOSE 80

# Provide the entry point for starting the JAR
ENTRYPOINT ["java", "-jar", "server.jar"]
