# Gradle caching layer for faster rebuilds
FROM gradle:7.4.2-jdk17-alpine AS cache
RUN mkdir -p /home/gradle/cache_home
ENV GRADLE_USER_HOME /home/gradle/cache_home

WORKDIR /home/gradle/app

# Building depends on constants generation

COPY build.gradle.kts gradle.properties settings.gradle.kts ./

RUN gradle clean build -i

# Building with gradle 7.4.2 and JDK 17
FROM gradle:7.4.2-jdk17-alpine AS build
# Copy cached gradle dependencies
COPY --from=cache /home/gradle/cache_home /home/gradle/.gradle

# Copy the local source code to a folder on the image
COPY --chown=gradle:gradle . /home/gradle/app/

# Set the working directory
WORKDIR /home/gradle/app

# Build the shadow JAR
RUN gradle shadowJar --no-daemon

# Run step uses OpenJDK 17 alpine
FROM openjdk:17-alpine

# Use environment variable based config only shouldn't
# be changed unless you want an on disk config as well
ENV RELAY_ENVIRONMENT_CONFIG=true

# External address for clients to access this server through
# this is the address that the redirector will tell the client
# that the main server is hosted at
ENV RELAY_EXTERNAL_ADDRESS="kme.jacobtread.local"

# Server ports
ENV RELAY_REDIRECTOR_PORT=42127
ENV RELAY_MAIN_PORT=14219
ENV RELAY_HTTP_PORT=80

# Database config
ENV RELAY_DATABASE_TYPE=sqlite

# SQLite config if using SQLite database
ENV RELAY_SQLITE_FILE_PATH="data/app.db"

# MySQL config if using MySQL database
ENV RELAY_MYSQL_HOST="127.0.0.1"
ENV RELAY_MYSQL_PORT=3306
ENV RELAY_MYSQL_USER="root"
ENV RELAY_MYSQL_PASSWORD="password"
ENV RELAY_MYSQL_DATABASE="kme"

# Message to display in main menu
ENV RELAY_MENU_MESSAGE="<font color='#B2B2B2'>KME3</font> - <font color='#FFFF66'>Logged as: {n}</font>"

# Galaxy at war config
ENV RELAY_GAW_READINESS_DECAY=0.0
ENV RELAY_GAW_ENABLE_PROMOTIONS=true

# Logging config
ENV RELAY_LOGGER_LEVEL=INFO
ENV RELAY_LOGGER_SAVE=true
ENV RELAY_LOGGER_PACKETS=false

# Retriever Config
ENV RELAY_RETRIEVE_OFFICIAL=true
ENV RELAY_RETRIEVE_ORIGIN_DATA=true

ENV RELAY_MITM_ENABLED=false

# Exposing the ports for all the servers
EXPOSE ${RELAY_REDIRECTOR_PORT}
EXPOSE ${RELAY_MAIN_PORT}
EXPOSE ${RELAY_HTTP_PORT}

# Create an app directory
RUN mkdir "/app"

# Set the working directory
WORKDIR /app

# Copy the compiled JAR from the build step
COPY --from=build /home/gradle/app/build/libs/server.jar server.jar

VOLUME ["/run"]
WORKDIR /run

# Provide the entry point for starting the JAR
ENTRYPOINT ["java", "-jar", "/app/server.jar"]
