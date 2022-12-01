![Logo](data/logo.png)

![License](https://img.shields.io/github/license/jacobtread/PocketRelay?style=for-the-badge)
[![Gradle Build](https://img.shields.io/github/workflow/status/jacobtread/PocketRelay/gradle-build?style=for-the-badge)](https://github.com/jacobtread/PocketRelay/actions/workflows/gradle.yml)

## 📌 Archive Notice
This version of the server is no longer being maintained and is being replaced by the new
server [Here (https://github.com/PocketRelay/ServerRust)](https://github.com/PocketRelay/ServerRust). This version will
no longer recieve updates. 

This verison of the server

[Discord Server (https://discord.gg/yvycWW8RgR)](https://discord.gg/yvycWW8RgR)

Pocket Relay is a custom private server for Mass Effect 3 emulating the functionatlity of the official EA servers
but allowing you to host your own closed off private server or ever a server for playing together over lan

So far this project has become a very well performing server and is constantly being improved and optimized in my free time.
There are some bugs which need to be ironed out but at the moment it's a perfectly usable server

## EA / BioWare Notice
All code in this repository is authored by Jacobtread and none is taken from BioWare games. This code has been 
produced from studying the protocol of the official servers and emulating its functionality. This program is
in no way or form supported, endorsed, or provided by BioWare or Electronic Arts.

## Links
- [**Latest Release** - The latest server jar release](https://github.com/jacobtread/PocketRelay/releases/latest) 
- [**Client Setup** - Setting up yourself to connect to KME3 servers](docs/SETUP_CLIENT.md)
- [**Hosting Setup** - Setting up yourself to host a KME3 servers](docs/SETUP_HOSTER.md)

## Connection requirements

In order to be able to connect to the server the following entry needs to be made in the client machine host file
gosredirector.ea.com and this needs to point to the IP address of the host
I plan on making a tool for doing this automatically

> Note: if you play other EA games such as Battlefield this may affect your ability to connect
> to the servers for those games, so you will need to remove this redirect before playing them

## Planned

- [ ] Web panel interface
- [ ] Leaderboard support

## Credits

1. [https://github.com/GamerClassN7/](https://github.com/GamerClassN7/) For providing me with an Origin copy of Mass Effect 3 and creating a patcher tool as well as supporting me with the project and helping test
2. [https://github.com/PrivateServerEmulator/ME3PSE](https://github.com/PrivateServerEmulator/ME3PSE) Sourced certificate and many game assets from this project
3. [https://github.com/Erik-JS/masseffect-binkw32](https://github.com/Erik-JS/masseffect-binkw32) Game patcher

