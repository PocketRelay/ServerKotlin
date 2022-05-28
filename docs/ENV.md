# Environment Variables

[Back](../README.md)

It is possible to configure KME3 through environment variables. The following page contains
a list of all of those environment variables

> NOTE: If you leave out any environment variables, or they are not parsable from user error
> or some other reason then if `KME_ENVIRONMENT_CONFIG` is not true the value in the config
> file will be used otherwise the default value will be used instead

### Base Configuration

| Variable Name          | Description                                                                                                                                                                           |
|------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| KME_ENVIRONMENT_CONFIG | If this is set to true then no config file will be used and only default + environment variables will be used  (true/false)                                                           |
| KME_EXTERNAL_ADDRESS   | This is the address the address that the clients will use to connect to the main server. If you specify a value for this you will nee to ensure the value is accessible to the client |
| KME_MENU_MESSAGE       | This is the message displayed on the main menu                                                                                                                                        |

> NOTE: when using `KME_MENU_MESSAGE` you can use the following codes for special values:

| Code | Value                                  |
|------|----------------------------------------|
| {v}  | The version of KME that is running     |
| {n}  | The name of the logged in player       |
| {ip} | The ip address of the logged in player |

### Port Configuration

| Variable Name       | Description                                                                                                                                                           |
|---------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| KME_REDIRECTOR_PORT | The port to use for the redirector server. You shouldn't change this unless you are behind something forwarding port 42127 otherwise clients won't be able to connect |
| KME_MAIN_PORT       | The port for the main server. You can change this value to any port you are able to bind                                                                              |
| KME_TICKER_PORT     | The port for the ticker server. You can change this to any value you are able to bind                                                                                 |
| KME_TELEMETRY_PORT  | The port for the telemetry server. You can change this to any value you are able to bind                                                                              |
| KME_HTTP_PORT       | The port for the HTTP server you can change this to any port you are able to bind                                                                                     |

### Logging

| Variable Name      | Description                                                               |
|--------------------|---------------------------------------------------------------------------|
| KME_LOGGER_LEVEL   | The level of logging to print to STDOUT (INFO, WARN, ERROR, FATAL, DEBUG) |
| KME_LOGGER_SAVE    | Whether to store logs in the logs directory (true/false)                  |
| KME_LOGGER_PACKETS | Whether to log incoming and outgoing packets (true/false)                 |


### Database Configuration

`KME_DATABASE_TYPE`: This environment variable is used to specify which type of database
should be used. The value that's specified in this environment variable determines which
database config will be used. The possible values for this are: mysql, and sqlite

#### SQLite Database Configuration

| Variable Name      | Description                                           |
|--------------------|-------------------------------------------------------|
| KME_SQLITE_FILE    | The file that SQLite should store the database within |

#### MySQL Database Configuration

| Variable Name      | Description                                      |
|--------------------|--------------------------------------------------|
| KME_MYSQL_HOST     | The host of address / domain of the MySQL server |
| KME_MYSQL_PORT     | The port of the MySQL server                     |
| KME_MYSQL_USER     | The user account for the MySQL server            |
| KME_MYSQL_PASSWORD | The account password for the MySQL server        |
| KME_MYSQL_DATABASE | The database to use in the MySQL server          |

### Galaxy at war Configuration

| Variable Name             | Description                                                                                                          |
|---------------------------|----------------------------------------------------------------------------------------------------------------------|
| KME_GAW_READINESS_DECAY   | The amount of readiness level to decay each day from last update 0.5 = -1%.Set this value defaults to 0 for no decay |
| KME_GAW_ENABLE_PROMOTIONS | Whether to enable promotions in gaw (use true or false)                                                              |

### Panel Configuration

| Variable Name      | Description                                                 |
|--------------------|-------------------------------------------------------------|
| KME_PANEL_ENABLED  | Whether the management panel should be enabled (true/false) |
| KME_PANEL_USERNAME | The username required for accessing the panel               |
| KME_PANEL_PASSWORD | The password required for accessing the panel               |
