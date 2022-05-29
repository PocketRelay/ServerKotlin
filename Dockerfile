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
FROM openjdk:17-alpine

# Use environment variable based config only shouldn't
# be changed unless you want an on disk config aswell
ENV KME_ENVIRONMENT_CONFIG=true

# External address for clients to access this server through
ENV KME_EXTERNAL_ADDRESS="383933-gosprapp396.ea.com"

# Server ports
ENV KME_REDIRECTOR_PORT=42127
ENV KME_MAIN_PORT=14219
ENV DISCARD_PORT=9988
ENV KME_HTTP_PORT=80

# Panel config
ENV KME_PANEL_ENABLED=true
ENV KME_PANEL_USERNAME=admin
ENV KME_PANEL_PASSWORD=admin

# Database config
ENV KME_DATABASE_TYPE=sqlite

# SQLite config if using SQLite database
ENV KME_SQLITE_FILE_PATH="data/app.db"

# MySQL config if using MySQL database
ENV KME_MYSQL_HOST="127.0.0.1"
ENV KME_MYSQL_PORT=3306
ENV KME_MYSQL_USER="root"
ENV KME_MYSQL_PASSWORD="password"
ENV KME_MYSQL_DATABASE="kme"

# Message to display in main menu
ENV KME_MENU_MESSAGE="<font color='#B2B2B2'>KME3</font> - <font color='#FFFF66'>Logged as: {n}</font>"

# Galaxy at war config
ENV KME_GAW_READINESS_DECAY=0.0
ENV KME_GAW_ENABLE_PROMOTIONS=true

# Logging config
ENV KME_LOGGER_LEVEL=INFO
ENV KME_LOGGER_SAVE=true
ENV KME_LOGGER_PACKETS=false

# Exposing the ports for all the servers
EXPOSE ${KME_REDIRECTOR_PORT}
EXPOSE ${KME_MAIN_PORT}
EXPOSE ${KME_TICKER_PORT}
EXPOSE ${KME_TELEMETRY_PORT}
EXPOSE ${KME_HTTP_PORT}

# Create an app directory
RUN mkdir "/app"

# Set the working directory
WORKDIR /app

# Copy the compiled JAR from the build step
COPY --from=build /home/gradle/src/build/libs/server.jar server.jar

VOLUME ["/run"]
WORKDIR /run

# Provide the entry point for starting the JAR
ENTRYPOINT ["java", "-jar", "/app/server.jar"]
