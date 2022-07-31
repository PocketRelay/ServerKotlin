# Hosting Setup Instructions

[Back](../README.md)

This document contains the instructions for hosting a KME3 server

## Docker

KME has a dockerfile in the root of the project this can be used to host the server within docker
which requires no extra setup. The docker server can be configured using [Environment Variables](docs/ENV.md)
if you are using docker then you can skip all other portions of this guide

## Prerequisites
- The server executable (This can be found in the releases [HERE](https://github.com/jacobtread/KME3/releases/latest))
- Minimum Java 11 (If you would like to use another Java version you must recompile KME against that version)
    - Oracle: ([https://www.oracle.com/java/technologies/downloads/](https://www.oracle.com/java/technologies/downloads/))

### 1. Preparing

Prepare a folder for you server. Depending on your configuration the server will generate both
a logs and a data folder along with a config.yml ensure that you have a location prepared for this
and make sure you set it to your working directory (Command is most likely `cd` but use your systems command)

### 2. Starting

To start the server run the following command inside your terminal
while in the same directory as the server.jar file

```java -jar server.jar```

If you have renamed the file you must replace server.jar with the new file name
to make starting up the server easier you can use a shell / bash script to run this
command for you.

### 3. All Done

That it your server is all up and running you can adjust the configuration file (config.yml)
to change the server settings.

> NOTE: You must restart the server to apply configuration changes

If you are looking for information on how to connect to the server now you can take a look at

[Client Setup](SETUP_CLIENT.md)

### Environment Variables

KME3 can be configured using environment variables. You can view a list of them at [Environment Variables](./ENV.md)

### Port Forwarding

In order for this server to be public available to non lan devices you will need
to forward the following ports

> NOTE: If you have modified any of these ports in your configuration file
> excluding the redirector port then you will need to forward the port you
> changed it too instead

- 42127 _(The redirector port)_
- 14219 _(The main server port this can be modified in the config)_
- 80 _(The http server port this can be modified in the config)_

If your router asks you to specific which protocol is being forwarded you 
should select `TCP` or `TCP/UDP` if that's not available

Search online for guides on port forwarding for your specific router 