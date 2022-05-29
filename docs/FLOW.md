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

The client will then request the `ME3_DATA` config using the `UTIL / FETCH_CLIENT_CONFIG` packet
the contents of this packet look like
```
text("CFID", "ME3_DATA")
```
And the server will respond to this with the following

```
  map("CONF", mapOf(
    "GAW_SERVER_BASE_URL" to "http://127.0.0.1/gaw", // Galaxy at war http server base url
    "IMG_MNGR_BASE_URL" to "http://127.0.0.1/content/", // Image hosting http server base url
    "IMG_MNGR_MAX_BYTES" to "1048576", // Max image size in bytes
    "IMG_MNGR_MAX_IMAGES" to "5", // max images count
    "JOB_THROTTLE_0" to "0", // Some sort of throttling?
    "JOB_THROTTLE_1" to "0", // Some sort of throttling?
    "JOB_THROTTLE_2" to "0", // Some sort of throttling?
    "MATCH_MAKING_RULES_VERSION" to "5",
    "MULTIPLAYER_PROTOCOL_VERSION" to "3",
    // Telemetry configurations?
    "TEL_DISABLE" to "AD,AF,AG,AI,AL,AM,AN,AO,AQ,AR,AS,AW,AX,AZ,BA,BB,BD,BF,BH,BI,BJ,BM,BN,BO,BR,BS,BT,BV,BW,BY,BZ,CC,CD,CF,CG,CI,CK,CL,CM,CN,CO,CR,CU,CV,CX,DJ,DM,DO,DZ,EC,EG,EH,ER,ET,FJ,FK,FM,FO,GA,GD,GE,GF,GG,GH,GI,GL,GM,GN,GP,GQ,GS,GT,GU,GW,GY,HM,HN,HT,ID,IL,IM,IN,IO,IQ,IR,IS,JE,JM,JO,KE,KG,KH,KI,KM,KN,KP,KR,KW,KY,KZ,LA,LB,LC,LI,LK,LR,LS,LY,MA,MC,MD,ME,MG,MH,ML,MM,MN,MO,MP,MQ,MR,MS,MU,MV,MW,MY,MZ,NA,NC,NE,NF,NG,NI,NP,NR,NU,OM,PA,PE,PF,PG,PH,PK,PM,PN,PS,PW,PY,QA,RE,RS,RW,SA,SB,SC,SD,SG,SH,SJ,SL,SM,SN,SO,SR,ST,SV,SY,SZ,TC,TD,TF,TG,TH,TJ,TK,TL,TM,TN,TO,TT,TV,TZ,UA,UG,UM,UY,UZ,VA,VC,VE,VG,VN,VU,WF,WS,YE,YT,ZM,ZW,ZZ",
    "TEL_DOMAIN" to "pc/masseffect-3-pc-anon",
    "TEL_FILTER" to "-UION/****",
    "TEL_PORT" to "9988",
    "TEL_SEND_DELAY" to "15000",
    "TEL_SEND_PCT" to "75",
    "TEL_SERVER" to "159.153.235.32",
  ))
```

The client will then retrieve the `ME3_MSG` config using the same packet as before just
with the `CFID` changed to `ME3_MSG` this is to fetch messages such as promotions etc
the server will respond with:

```
  map("CONF", mapOf(
    // Date when this message shouldn't be displayed anymore
    "MSG_1_endDate" to "10:03:2025",
    // Image (will be retrieved from the images http server)
    "MSG_1_image" to "Promo_n7.dds",
    // Message and message in all other languages
    "MSG_1_message" to "KME Server is working!!",
    "MSG_1_message_de" to "KME Server is working!!",
    "MSG_1_message_es" to "KME Server is working!!",
    "MSG_1_message_fr" to "KME Server is working!!",
    "MSG_1_message_it" to "KME Server is working!!",
    "MSG_1_message_ja" to "KME Server is working!!",
    "MSG_1_message_pl" to "KME Server is working!!",
    "MSG_1_message_ru" to "KME Server is working!!",
    // Priority of this message? probabbly order in which to show them
    "MSG_1_priority" to "201",
    
   // Title of the message along with it in all other languages
    "MSG_1_title" to "KME Server",
    "MSG_1_title_de" to "KME Server",
    "MSG_1_title_es" to "KME Server",
    "MSG_1_title_fr" to "KME Server",
    "MSG_1_title_it" to "KME Server",
    "MSG_1_title_ja" to "KME Server",
    "MSG_1_title_pl" to "KME Server",
    "MSG_1_title_ru" to "KME Server",
    // Tracking ID used to keep track of which offers have been dismissed
    "MSG_1_trackingId" to "7",
    // Appears to be type of message needs further exploring
    "MSG_1_type" to "8",
  ))
```

| Msg Type   | What                                        |
|------------|---------------------------------------------|
| 0          | Main menu tab across from galaxy at war     |
| 1, 4, 5, 6 | Main menu messages bottom scrolling message |
| 8          | Shopping / Promotional messages?            |

> It appears that this isn't always correct?

Then client then requests entitlement using the same packet but with the `CFID` set
to `ME3_ENT`

The server will respond with a large list of known entitlements these are not unique to each
player and will stay the same.

The client then retrieves the "DIME" config this appears to contain details about items in the shop
the `CFID` requesting this is `ME3_DIME` and the server response looks like this

```
  map("CONF", mapOf(
    "Config" to "<?xml version="1.0" encoding="utf-8"?>
<dime xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:noNamespaceSchemaLocation="dimecfg.xsd">
    <configFormatVersion>1</configFormatVersion>
    <fileVersion>1</fileVersion>
    <games>
        <game>
            <index>0</index>
            <name>Mass Effect 3</name>
            <pc>
                <storeId>10285</storeId>
                <catalogIdRealCurrency>10398</catalogIdRealCurrency>
                <catalogIdVirtualCurrency>10397</catalogIdVirtualCurrency>
                <categoryId>13339</categoryId>
                <groupName>ME3PCContent</groupName>
                <lockboxURL>https://origin.checkout.ea.com/lockbox-ui/checkout</lockboxURL>
                <successURL>http://bioware.com</successURL>
                <failURL>http://bioware.com</failURL>
                <virtualCurrency>_BW</virtualCurrency>
            </pc>
        </game>
        <primaryGameIndex>0</primaryGameIndex>
    </games>
    <content>
        <consumables>
            <consumable><uniqueId>0x9EB74</uniqueId><name>N7 Equipment Pack</name><gameIndex>0</gameIndex><metaData/><pc><offerId>OFB-MASS:50162</offerId><purchaseable>true</purchaseable><advertise>true</advertise><itemId>OFB-MASS:50162</itemId><entitlementTag>ME3_REPACK_N7_EQUIPMENT</entitlementTag><useVirtualCurrency>true</useVirtualCurrency><groupNameIndex>0</groupNameIndex></pc><dimeVersion>0</dimeVersion></consumable>
            <consumable><uniqueId>0x9EBD8</uniqueId><name>Arsenal Pack</name><gameIndex>0</gameIndex><metaData/><pc><offerId>OFB-MASS:50165</offerId><purchaseable>true</purchaseable><advertise>true</advertise><itemId>OFB-MASS:50165</itemId><entitlementTag>ME3_REPACK_ARSENAL</entitlementTag><useVirtualCurrency>true</useVirtualCurrency><groupNameIndex>0</groupNameIndex></pc><dimeVersion>0</dimeVersion></consumable>
            <consumable><uniqueId>0x9EC3C</uniqueId><name>Reserves Pack</name><gameIndex>0</gameIndex><metaData/><pc><offerId>OFB-MASS:50166</offerId><purchaseable>true</purchaseable><advertise>true</advertise><itemId>OFB-MASS:50166</itemId><entitlementTag>ME3_REPACK_RESERVES</entitlementTag><useVirtualCurrency>true</useVirtualCurrency><groupNameIndex>0</groupNameIndex></pc><dimeVersion>0</dimeVersion></consumable>
            <consumable><uniqueId>0x30D40</uniqueId><name>ME3 RePack Silver</name><gameIndex>0</gameIndex><metaData/><pc><offerId>OFB-MASS:46192</offerId><purchaseable>true</purchaseable><advertise>true</advertise><itemId>OFB-MASS:46192</itemId><entitlementTag>ME3_REPACK_SILVER_PACK</entitlementTag><useVirtualCurrency>true</useVirtualCurrency><groupNameIndex>0</groupNameIndex></pc><dimeVersion>0</dimeVersion></consumable>
            <consumable><uniqueId>0x3D090</uniqueId><name>ME3 RePack Silver Premium</name><gameIndex>0</gameIndex><metaData/><pc><offerId>OFB-MASS:48173</offerId><purchaseable>true</purchaseable><advertise>true</advertise><itemId>OFB-MASS:48173</itemId><entitlementTag>ME3_REPACK_SILVER_PREMIUM_PACK</entitlementTag><useVirtualCurrency>true</useVirtualCurrency><groupNameIndex>0</groupNameIndex></pc><dimeVersion>0</dimeVersion></consumable>
            <consumable><uniqueId>0x493E0</uniqueId><name>ME3 RePack Gold</name><gameIndex>0</gameIndex><metaData/><pc><offerId>OFB-MASS:46193</offerId><purchaseable>true</purchaseable><advertise>true</advertise><itemId>OFB-MASS:46193</itemId><entitlementTag>ME3_REPACK_GOLD_PACK</entitlementTag><useVirtualCurrency>true</useVirtualCurrency><groupNameIndex>0</groupNameIndex></pc><dimeVersion>0</dimeVersion></consumable>
            <consumable><uniqueId>0x55730</uniqueId><name>ME3 RePack Gold Premium</name><gameIndex>0</gameIndex><metaData/><pc><offerId>OFB-MASS:48177</offerId><purchaseable>true</purchaseable><advertise>true</advertise><itemId>OFB-MASS:48177</itemId><entitlementTag>ME3_REPACK_GOLD_PREMIUM_PACK</entitlementTag><useVirtualCurrency>true</useVirtualCurrency><groupNameIndex>0</groupNameIndex></pc><dimeVersion>0</dimeVersion></consumable>
            <consumable><uniqueId>0x495D4</uniqueId><name>ME3 RePack Gold Jumbo</name><gameIndex>0</gameIndex><metaData/><pc><offerId>OFB-MASS:48189</offerId><purchaseable>true</purchaseable><advertise>true</advertise><itemId>OFB-MASS:48189</itemId><entitlementTag>ME3_REPACK_GOLD_JUMBO_PACK</entitlementTag><useVirtualCurrency>true</useVirtualCurrency><groupNameIndex>0</groupNameIndex></pc><dimeVersion>0</dimeVersion></consumable>
            <consumable><uniqueId>0x61A80</uniqueId><name>ME3 RePack Platinum</name><gameIndex>0</gameIndex><metaData/><pc><offerId>OFB-MASS:46194</offerId><purchaseable>true</purchaseable><advertise>true</advertise><itemId>OFB-MASS:46194</itemId><entitlementTag>ME3_REPACK_PLATINUM_PACK</entitlementTag><useVirtualCurrency>true</useVirtualCurrency><groupNameIndex>0</groupNameIndex></pc><dimeVersion>0</dimeVersion></consumable>
            <consumable><uniqueId>0x61A82</uniqueId><name>ME3 RePack Platinum Deal 2</name><gameIndex>0</gameIndex><metaData/><pc><offerId>OFB-MASS:48194</offerId><purchaseable>true</purchaseable><advertise>true</advertise><itemId>OFB-MASS:48194</itemId><entitlementTag>ME3_REPACK_PLATINUM_PACK</entitlementTag><useVirtualCurrency>true</useVirtualCurrency><groupNameIndex>0</groupNameIndex></pc><dimeVersion>0</dimeVersion></consumable>
            <consumable><uniqueId>0x7A120</uniqueId><name>ME3 RePack Supplies</name><gameIndex>0</gameIndex><metaData/><pc><offerId>OFB-MASS:48196</offerId><purchaseable>true</purchaseable><advertise>true</advertise><itemId>OFB-MASS:48196</itemId><entitlementTag>ME3_REPACK_SUPPLIES_PACK</entitlementTag><useVirtualCurrency>true</useVirtualCurrency><groupNameIndex>0</groupNameIndex></pc><dimeVersion>0</dimeVersion></consumable>
            <consumable><uniqueId>0x7A314</uniqueId><name>ME3 RePack Supplies Jumbo</name><gameIndex>0</gameIndex><metaData/><pc><offerId>OFB-MASS:48199</offerId><purchaseable>true</purchaseable><advertise>true</advertise><itemId>OFB-MASS:48199</itemId><entitlementTag>ME3_REPACK_SUPPLIES_JUMBO_PACK</entitlementTag><useVirtualCurrency>true</useVirtualCurrency><groupNameIndex>0</groupNameIndex></pc><dimeVersion>0</dimeVersion></consumable>
            <consumable><uniqueId>0x7A508</uniqueId><name>ME3 RePack Mystery Character</name><gameIndex>0</gameIndex><metaData/><pc><offerId>OFB-MASS:48201</offerId><purchaseable>true</purchaseable><advertise>true</advertise><itemId>OFB-MASS:48201</itemId><entitlementTag>ME3_REPACK_MYSTERY_CHARACTER</entitlementTag><useVirtualCurrency>true</useVirtualCurrency><groupNameIndex>0</groupNameIndex></pc><dimeVersion>0</dimeVersion></consumable>
            <consumable><uniqueId>0x7A8F0</uniqueId><name>ME3 RePack Mystery Weapon</name><gameIndex>0</gameIndex><metaData/><pc><offerId>OFB-MASS:48202</offerId><purchaseable>true</purchaseable><advertise>true</advertise><itemId>OFB-MASS:48202</itemId><entitlementTag>ME3_REPACK_MYSTERY_WEAPON</entitlementTag><useVirtualCurrency>true</useVirtualCurrency><groupNameIndex>0</groupNameIndex></pc><dimeVersion>0</dimeVersion></consumable>
            <consumable><uniqueId>0x7ACD8</uniqueId><name>ME3 RePack Mystery Mod</name><gameIndex>0</gameIndex><metaData/><pc><offerId>OFB-MASS:48203</offerId><purchaseable>true</purchaseable><advertise>true</advertise><itemId>OFB-MASS:48203</itemId><entitlementTag>ME3_REPACK_MYSTERY_MOD</entitlementTag><useVirtualCurrency>true</useVirtualCurrency><groupNameIndex>0</groupNameIndex></pc><dimeVersion>0</dimeVersion></consumable>
            <consumable><uniqueId>0x9EB12</uniqueId><name>ME3 RePack Resurgence Deal 2</name><gameIndex>0</gameIndex><metaData/><pc><offerId>OFB-MASS:48326</offerId><purchaseable>true</purchaseable><advertise>true</advertise><itemId>OFB-MASS:48326</itemId><entitlementTag>ME3_REPACK_RESURGENCE_01</entitlementTag><useVirtualCurrency>true</useVirtualCurrency><groupNameIndex>0</groupNameIndex></pc><dimeVersion>0</dimeVersion></consumable>
            <consumable><uniqueId>0x9EB13</uniqueId><name>ME3 RePack Rebellion</name><gameIndex>0</gameIndex><metaData/><pc><offerId>OFB-MASS:49749</offerId><purchaseable>true</purchaseable><advertise>true</advertise><itemId>OFB-MASS:49749</itemId><entitlementTag>ME3_REPACK_REBELLION</entitlementTag><useVirtualCurrency>true</useVirtualCurrency><groupNameIndex>0</groupNameIndex></pc><dimeVersion>0</dimeVersion></consumable>
            <consumable><uniqueId>0x9EB14</uniqueId><name>ME3 RePack Rebellion Deal 1</name><gameIndex>0</gameIndex><metaData/><pc><offerId>OFB-MASS:49750</offerId><purchaseable>true</purchaseable><advertise>true</advertise><itemId>OFB-MASS:49750</itemId><entitlementTag>ME3_REPACK_REBELLION</entitlementTag><useVirtualCurrency>true</useVirtualCurrency><groupNameIndex>0</groupNameIndex></pc><dimeVersion>0</dimeVersion></consumable>
            <consumable><uniqueId>0x9EB15</uniqueId><name>ME3 RePack Rebellion Deal 2</name><gameIndex>0</gameIndex><metaData/><pc><offerId>OFB-MASS:49751</offerId><purchaseable>true</purchaseable><advertise>true</advertise><itemId>OFB-MASS:49751</itemId><entitlementTag>ME3_REPACK_REBELLION</entitlementTag><useVirtualCurrency>true</useVirtualCurrency><groupNameIndex>0</groupNameIndex></pc><dimeVersion>0</dimeVersion></consumable>
        </consumables>
    </content>
</dime>",
  ))
```


### Authentication

If the client has logged in before it will attempt to log in using the previously provided session token
it will do this using the `AUTHENTICATION / SILENT_LOGIN` packet which has the following structure

```
text("AUTH", "SESSION_TOKEN") // The session token
number("PID", 0x1) // The ID of the player 
number("TYPE", 0x2) // Unknown
```

If this session token is no longer valid an error packet will be sent with the error type and
the contents of 

```  
text("PNAM", "")
number("UID", 0x0)
```

Then a `AUTHENTICAITON / LOGOUT` packet will also be sent 