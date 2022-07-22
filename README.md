# KME3

![License](https://img.shields.io/github/license/jacobtread/KME3?style=for-the-badge)
[![Gradle Build](https://img.shields.io/github/workflow/status/jacobtread/KME3/gradle-build?style=for-the-badge)](https://github.com/jacobtread/KME3/actions/workflows/gradle.yml)
![Total Lines](https://img.shields.io/tokei/lines/github/jacobtread/KME3?style=for-the-badge)

Mass Effect 3 Server Emulator

Written in Kotlin this aims to be a production capable version of the [https://github.com/PrivateServerEmulator/ME3PSE](https://github.com/PrivateServerEmulator/ME3PSE) server
with the aim to improve it and take it to the level of a server that can be used to host public matches with a proper backend that stores account information in a database etc

## Setup

**Client Setup** You can find the setup guide for connecting to KME servers [Here](docs/SETUP_CLIENT.md)

**Hosting Setup**: You can find the setup guide for running your own KME servers [Here](docs/SETUP_HOSTER.md)

## Configuration Reference

For information about the configuration file and what each setting in the file means see [Here](docs/CONFIG_REFERENCE.md)

## Connection requirements

In order to be able to connect to the server the following entry needs to be made in the client machine host file
gosredirector.ea.com and this needs to point to the IP address of the host
I plan on making a tool for doing this automatically

> Note: if you play other EA games such as Battlefield this may affect your ability to connect
> to the servers for those games, so you will need to remove this redirect before playing them

## Working so far

- [x] Working Server & Client Connections
- [x] Parsing and reading packets
- [x] Clean and easy builders for creating packets
    - (similar to compose uses a DSL making it super easy to use)
- [x] Initial database structure
- [x] Create Account with server
    - [x] Store user in database
- [x] Login
    - [x] Access users in database and retrieve them
    - [x] Silent login / Token based login
- [x] Main menu message
- [x] Character storage and selection
- [x] Galaxy at war
- [x] Matchmaking
    - NOTE: Some match settings are respected but not all currently private matches are not respected
- [x] Playing games!
- [x] In game store

## Planned

- [ ] Web panel interface
    - This has been started and is at [https://github.com/jacobtread/KME3Web](https://github.com/jacobtread/KME3Web) but will be included by default
- [ ] Leaderboard support

## Credits

1. [https://github.com/PrivateServerEmulator/ME3PSE](https://github.com/PrivateServerEmulator/ME3PSE) Sourced certificate and many game assets from this project
2. [https://github.com/Erik-JS/masseffect-binkw32](https://github.com/Erik-JS/masseffect-binkw32) Game patcher
