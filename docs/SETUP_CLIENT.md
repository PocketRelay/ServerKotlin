# Client Setup Instructions

[Back](../README.md)

## Automatic
All the setups listed in this document can be done automatically using the patcher tool at
[https://github.com/GamerClassN7/KME3_Patcher](https://github.com/GamerClassN7/KME3_Patcher) this
is made by a friend on mine who is helping out with this project!

If you wish to manually do what this patcher does you can follow the steps below.


### 1. Getting Server Address

You need to obtain the IP address of the server you are trying to connect to this
will be referred to throughout this page as ${ADDRESS}

### 2. Setup Hosts File Redirect

In order for mass effect to know where to connect to you will need to add a hosts file redirection
to point the original mass effect redirector domain to the custom server ip address

> Information on hosts file is available at: [https://en.wikipedia.org/wiki/Hosts_%28file%29](https://en.wikipedia.org/wiki/Hosts_%28file%29)

Open notepad or some other text editor as administer and open the file
`C:\Windows\System32\drivers\etc\hosts`

Your file should look something like this:

```
# Copyright (c) 1993-2009 Microsoft Corp.
#
# This is a sample HOSTS file used by Microsoft TCP/IP for Windows.
#
# This file contains the mappings of IP addresses to host names. Each
# entry should be kept on an individual line. The IP address should
# be placed in the first column followed by the corresponding host name.
# The IP address and the host name should be separated by at least one
# space.
#
# Additionally, comments (such as these) may be inserted on individual
# lines or following the machine name denoted by a '#' symbol.
#
# For example:
#
#      102.54.94.97     rhino.acme.com          # source server
#       38.25.63.10     x.acme.com              # x client host

# localhost name resolution is handled within DNS itself.
#	127.0.0.1       localhost
#	::1             localhost
```

With maybe a few extra entries at the bottom.

You are going to want to add a new line below all the other entries that looks like so

```
${ADDRESS} gosredirector.ea.com
```

Don't forget to replace ${ADDRESS} with the IP address of your server

> NOTE: If the server you are trying to connect to is not using a custom domain or ip address
> for the "external address" then you will also need to add the following line aswell
> replacing ${ADDRESS} with the IP address of your server

```
${ADDRESS} kme.jacobtread.local
```

### 3. Patching your game

In order for mass effect 3 to be able to connect to the custom server you will need to disable the certificate verification
to do this there is a tool [https://github.com/Erik-JS/masseffect-binkw32](https://github.com/Erik-JS/masseffect-binkw32) which
does this automatically.

You can download this from: [https://github.com/Erik-JS/masseffect-binkw32/releases/latest](https://github.com/Erik-JS/masseffect-binkw32/releases/latest).
On this page you want to download the file named `me3_binkw32.zip`

1. Make a backup of your existing `blinkw32.dll` this will be stored in `%GAME_DIR%/Binaries/Win32`
2. Copy `blinkw23.dll` and `blinkw32.dll` from inside `me3_binkw32.zip` and paste them into `%GAME_DIR%/Binaries/Win32` and make sure you allow file replacing
3. All done

### 4. All done

You should now be able to connect to the server if you entered the address correctly, and you should be able
to create an online account.
