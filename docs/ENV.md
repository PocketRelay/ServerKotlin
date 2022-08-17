# Environment Variables

[Back](../README.md)

It is possible to configure KME3 through environment variables. The following page contains
a list of all of those environment variables

> NOTE: If you leave out any environment variables, or they are not parsable from user error
> or some other reason then if `RELAY_ENVIRONMENT_CONFIG` is not true the value in the config
> file will be used otherwise the default value will be used instead

### Base Configuration

| Variable Name            | Description                                                                                                                                                                                              |
|--------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| RELAY_ENVIRONMENT_CONFIG   | If this is set to true then no config file will be used and only default + environment variables will be used  (true/false)                                                                              |
| RELAY_EXTERNAL_ADDRESS     | This is the address the address that the clients will use to connect to the main server. If you specify a value for this you will nee to ensure the value is accessible to the client                    |
| RELAY_MENU_MESSAGE         | This is the message displayed on the main menu                                                                                                                                                           |
| RELAY_NETTY_UNPOOLED       | This disables netty buffer pooling. This can greatly increase the amount of ununused ram being freed but with a large number of connections this could hurt performance (true/false) disabled by default |
| RELAY_RETRIEVE_OFFICIAL    | Determines whether the server should attempt to get the official information about an origin account when it logs in                                                                                     |
| RELAY_RETRIEVE_ORIGIN_DATA | If retriever is enabled the user characters, classes, etc. will be retrieved as well                                                                                                                     |

> NOTE: when using `RELAY_MENU_MESSAGE` you can use the following codes for special values:

| Code | Value                                  |
|------|----------------------------------------|
| {v}  | The version of KME that is running     |
| {n}  | The name of the logged in player       |
| {ip} | The ip address of the logged in player |

### Port Configuration

| Variable Name       | Description                                                                                                                                                           |
|---------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| RELAY_REDIRECTOR_PORT | The port to use for the redirector server. You shouldn't change this unless you are behind something forwarding port 42127 otherwise clients won't be able to connect |
| RELAY_MAIN_PORT       | The port for the main server. You can change this value to any port you are able to bind                                                                              |
| RELAY_HTTP_PORT       | The port for the HTTP server you can change this to any port you are able to bind                                                                                     |

### Logging

| Variable Name      | Description                                                               |
|--------------------|---------------------------------------------------------------------------|
| RELAY_LOGGER_LEVEL   | The level of logging to print to STDOUT (INFO, WARN, ERROR, FATAL, DEBUG) |
| RELAY_LOGGER_SAVE    | Whether to store logs in the logs directory (true/false)                  |
| RELAY_LOGGER_PACKETS | Whether to log incoming and outgoing packets (true/false)                 |

### Database Configuration

`RELAY_DATABASE_TYPE`: This environment variable is used to specify which type of database
should be used. The value that's specified in this environment variable determines which
database config will be used. The possible values for this are: mysql, and sqlite

#### SQLite Database Configuration

| Variable Name      | Description                                           |
|--------------------|-------------------------------------------------------|
| RELAY_SQLITE_FILE    | The file that SQLite should store the database within |

#### MySQL Database Configuration

| Variable Name        | Description                                      |
|----------------------|--------------------------------------------------|
| RELAY_MYSQL_HOST     | The host of address / domain of the MySQL server |
| RELAY_MYSQL_PORT     | The port of the MySQL server                     |
| RELAY_MYSQL_USER     | The user account for the MySQL server            |
| RELAY_MYSQL_PASSWORD | The account password for the MySQL server        |
| RELAY_MYSQL_DATABASE | The database to use in the MySQL server          |

### Galaxy at war Configuration

| Variable Name               | Description                                                                                                          |
|-----------------------------|----------------------------------------------------------------------------------------------------------------------|
| RELAY_GAW_READINESS_DECAY   | The amount of readiness level to decay each day from last update 0.5 = -1%.Set this value defaults to 0 for no decay |
| RELAY_GAW_ENABLE_PROMOTIONS | Whether to enable promotions in gaw (use true or false)                                                              |
