# Networking Flow
This page contains my understanding of the flow of network requests that make up the client and server communications

### Initial Connection
The initial connection made by the client goes to `gosredirector.ea.com` on port `42127` this uses SSLv3

The `REDIRECTOR / GET_SERVER_INSTANCE` packet seems to be the only packet ever sent to this server.

The contents of that packet are
```
text("BSDK", "3.15.6.0") // Appears to be the Blaze SDK version
text("BTIM", "Dec 21 2012 12:47:10") // Unknown
text("CLNT", "MassEffect3-pc") // Appears to be the type of client
number("CLTP", 0x0) // Unknown
text("CSKU", "134845") // Unknown
text("CVER", "05427.124")  // Unknown
text("DSDK", "8.14.7.1") // Unknown
text("ENV", "prod") // Environment type?
optional("FPID", 0x7f, null) // Unknown
number("LOC", 0x656e4e5a) // Unknown
text("NAME", "masseffect-3-pc") // Some sort of key name for the type of server wanted?
text("PLAT", "Windows") // The platform the user is playing on
text("PROF", "standardSecure_v3") // Appears to represent SSLv3?
```

The server then responds to that with a `REDIRECTOR / GET_SERVER_INSTANCE` response with the following contents

```
// Address grouping
optional("ADDR", group("VALU") { // Value for this address
    // The host address this is usually a domain but appears to accept ips
    text("HOST", target.host)
    number("IP", target.address) // Appears to be used if the HOST is missing? Unsure 
    number("PORT", port) // The port of the main server
})

bool("SECU", false) // Whether the client shoud use SSLv3 for the main server
number("XDNS", 0x0) // Unknown
```

### Pre Authentication Configuration
The client then makes a connection to the main server of which the details were provided by the
redirector and sends a `UTIL / PRE_AUTH` packet which looks like this:
```
+group("CDAT") {
    number("IITO", 0x0)
    number("LANG", 0x656e4e5a)
    text("SVCN", "masseffect-3-pc")
    number("TYPE", 0x0)
}
+group("CINF") { // Client info?
    text("BSDK", "3.15.6.0") // Blaze SDK version
    text("BTIM", "Dec 21 2012 12:46:51")
    text("CLNT", "MassEffect3-pc") // Client type
    text("CSKU", "134845")
    text("CVER", "05427.124")
    text("DSDK", "8.14.7.1")
    text("ENV", "prod") // Environment
    number("LOC", 0x656e4e5a)
    text("MAC", "7c:10:c9:28:33:35") // Client mac address?
    text("PLAT", "Windows") // Client platform
}
+group("FCCR") {
    text("CFID", "BlazeSDK")
}
```

The server then responds to this with 
```
number("ANON", 0x0)
number("ASRC", 0x4a003)
list("CIDS", listOf(0x1, 0x19, 0x4, 0x1c, 0x7, 0x9, 0xf802, 0x7800, 0xf, 0x7801, 0x7802, 0x7803, 0x7805, 0x7806, 0x7d0))
text("CNGN", "")
+group("CONF") { // Appears to be a client configuration of sorts
    map("CONF", mapOf(
      "pingPeriod" to "15s", // Time in seconds between pings
      "voipHeadsetUpdateRate" to "1000",
      "xlspConnectionIdleTimeout" to "300",
    ))
}
text("INST", "masseffect-3-pc") // Type of server?
number("MINR", 0x0)
text("NASP", "cem_ea_id")
text("PILD", "")
text("PLAT", "pc")
text("PTAG", "")
+group("QOSS") {
    +group("BWPS") {
      text("PSA", "127.0.0.1") // Address
      number("PSP", 0x445e) // Port 
      text("SNA", "prod-sjc")
    }
    number("LNP", 0xa)
    map("LTPS", mapOf( // Appears to be server groupings of some sort?
      "ea-sjc" to group {
        text("PSA", "127.0.0.1") // Address
        number("PSP", 0x445e) // Port 
        text("SNA", "prod-sjc")
      },
      "rs-iad" to group {
        text("PSA", "127.0.0.1") // Address
        number("PSP", 0x445e) // Port 
        text("SNA", "rs-prod-iad")
      },
      "rs-lhr" to group {
        text("PSA", "127.0.0.1") // Address
        number("PSP", 0x445e) // Port 
        text("SNA", "rs-prod-lhr")
      },
    ))
    number("SVID", 0x45410805)
}
text("RSRC", "303107")
text("SVER", "Blaze 3.15.08.0 (CL# 750727)") // Blaze server version
```

The client will send `UTIL / PING` packets at the interval specified in the pre-auth config 
response they have no contents.
They should be responded to with the following
```
number("STIM", 0x6291dcfa) // The server time in seconds (epoch time)
```

### Other Configs
