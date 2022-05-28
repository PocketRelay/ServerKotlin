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