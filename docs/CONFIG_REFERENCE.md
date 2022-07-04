# Config Reference

[Back](../README.md)

This document contains the explaination for all the different values in the config.json file

## External address (externalAddress)

This address should be an ip or hostname which points to the server that is hosting KME
this address must be reachable by the clients connecting, so it must be either a valid IP
address or a hostname that points to it.

The default for this is 383933-gosprapp396.ea.com when the default value is used its required
that this value be redirected in the users hosts file

## Ports  (ports)

The values in this object represents the differents ports that each of the child
servers should be bound on. These port values must be integer values

| Key        | What                                                                                                                                                                      |
|------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| redirector | This is the port which the redirector server is bound to                                                                                                                  |
| main       | This is the port which the main game server is bound to                                                                                                                   |
| discard    | This is the port that the discard server is bound to this port is not very important, so it can be given a low priority number as anything sent to it is simply discarded |
| http       | This is the port of the http server which both the panel and the game images are hosted on                                                                                |

> NOTE: You should **NOT** change the port of the redirector server to anything
> other than the default 42127 unless you are behind a proxy or inside a container
> that is forwarding from 42127 beacuse this is the port that the mass effect client
> trys to make a connection to

## Logging (logging)

This is the configuration for the application logging this defines whichs levels of logging
should be used as well as other settings for logging

| Key     | What                                                                                                                |
|---------|---------------------------------------------------------------------------------------------------------------------|
| level   | This is the logging level which should be used (INFO, WARN, ERROR, FATAL, DEBUG)                                    |
| save    | This option determines whether log files will be saved in log files                                                 |
| packets | This options determines whether the information of incomming and outgoing packets should be printed to the console. |

> NOTE: I recommend disabling packets logging when using MITM mode because MITM logs packets
> as well which will **GREATLY** clog up your console with confusing messages

## Menu Message (menuMessage)

This is the message displayed on the main menu you can use special formatting codes
here in order to include dynamic information which is replaced by the server

| Code | What                                         |
|------|----------------------------------------------|
| {v}  | Replaced with the version of the KME server  |
| {n}  | Replaced with the authenticated players name |
| {ip} | Replaced with the connections IP address     |

## Database (database)

This configuration section contains the database information that determines which database to use
as well as the credentials

| Key      | What                                                                                        |
|----------|---------------------------------------------------------------------------------------------|
| type     | This is the type of database that should be used (MySQL, or SQLite) name is case-insensitve |
| host     | The host of the database server *(MySQL Only)*                                              |
| port     | The port of the database server  *(MySQL Only)*                                             |
| user     | The username for the database server *(MySQL Only)*                                         |
| password | The password for the database server *(MySQL Only)*                                         |
| database | The name of the database *(MySQL Only)*                                                     |
| file     | The database file path *(SQLite Only)*  This path is from the current working directory     |

## Panel (panel)

This configuration section contains the settings for the HTTP web panel

| Key      | What                                 |
|----------|--------------------------------------|
| enabled  | Whether the panel is enabled         |
| username | The username for accessing the panel |
| password | The password for accessing the panel |

## Galaxy at war (gaw)

This configuration contains the galaxy at war settings

| Key                 | What                                                                                                                  |
|---------------------|-----------------------------------------------------------------------------------------------------------------------|
| readinessDailyDecay | The amount of readiness level to decay each day from last update 0.5 = -1%. Set this value defaults to 0 for no decay |
| enablePromotions    | Whether to count promotions on the galaxy at war                                                                      |

## Man In The Middle (mitm)

This configuration section contains the settings for the Man-in-the-middle mode which creates a connection
to the official server and tunnels the connections throught itself decoding and logging all the traffic

| Key     | What                                                                       |
|---------|----------------------------------------------------------------------------|
| enabled | Whether the Man-in-the-middle mode should be used                          |
| host    | The host address of the official mass effect game server                   |
| port    | The port of the official mass effect game server                           |
| secure  | Whether the connection to the official game server needs to use SSL or not |
