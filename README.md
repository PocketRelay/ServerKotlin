# KME3

Mass Effect 3 Server Emulator 

Written in Kotlin this aims to be a production capable version of the [https://github.com/PrivateServerEmulator/ME3PSE](https://github.com/PrivateServerEmulator/ME3PSE) server
with the aim to improve it and take it to the level of a server that can be used to host public matches with a proper backend that stores account information in a database etc

This server is quite far from completion but is steadily progressing. 

### IT WORKS!!!
You can actually play games on it. However there are some things outside of the games that don't work yet like the shop

## Connection requirements
In order to be able to connect to the server the following entry needs to be made in the client machine host file
gosredirector.ea.com and this needs to point to the IP address of the host
I plan on making a tool for doing this automatically

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
  - NOTE: Doesn't actually take user preferences into account just joins first not full server
- [x] Playing games!

## Whats not working
- [ ] The store....

## Planned
- [ ] Leaderboard support
- [ ] Web panel interface

## Credits
[https://github.com/PrivateServerEmulator/ME3PSE](https://github.com/PrivateServerEmulator/ME3PSE)
Sourced certificate and many game assets from this project
