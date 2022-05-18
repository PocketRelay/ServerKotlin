# KME3

Mass Effect 3 Server Emulator 

Written in Kotlin this aims to be a production capable version of the [https://github.com/PrivateServerEmulator/ME3PSE](https://github.com/PrivateServerEmulator/ME3PSE) server
with the aim to improve it and take it to the level of a server that can be used to host public matches with a proper backend that stores account information in a database etc

This server is quite far from completion but is steadily progressing. 

## Working so far
- [x] Working Server & Client Connections
- [x] Parsing and reading packets 
- [x] Clean and easy builders for creating packets
  - (similar to compose uses a DSL making it super easy to use)
- [x] Initial database structure
- [x] Create Account with server (Partial Auth flow not entirely working)
  - [x] Store user in database
- [x] Login 
  - [x] Access users in database and retrieve them
  - [x] Silent login / Token based login
- [x] Main menu message