package com.jacobtread.kme.sessions.handlers

import com.jacobtread.blaze.*
import com.jacobtread.blaze.annotations.PacketHandler
import com.jacobtread.blaze.packet.Packet
import com.jacobtread.blaze.tdf.types.GroupTdf
import com.jacobtread.kme.Environment
import com.jacobtread.kme.data.LoginError
import com.jacobtread.kme.data.blaze.Commands
import com.jacobtread.kme.data.blaze.Components
import com.jacobtread.kme.exceptions.DatabaseException
import com.jacobtread.kme.sessions.Session
import com.jacobtread.kme.utils.hashPassword
import com.jacobtread.kme.utils.logging.Logger
import java.io.IOException

/**
 * Handles logins from clients using the origin system. This generates a
 * unqiue username for the provided origin token.
 *
 * @param packet The packet requesting origin login
 */
@PacketHandler(Components.AUTHENTICATION, Commands.ORIGIN_LOGIN)
fun Session.handleOriginLogin(packet: Packet) {
    val auth = packet.text("AUTH")
    val player = Environment.database.getOriginPlayer(auth)
    if (player == null) {
        // Failed to create origin account.
        push(LoginError.SERVER_UNAVAILABLE(packet))
        return
    }

    Logger.info("Authenticated Origin Account ${player.displayName}")

    doAuthenticate(player, packet, true)
}

/**
 * Handles logging out the currently
 * authenticated player
 *
 * @param packet The packet requesting logout
 */
@PacketHandler(Components.AUTHENTICATION, Commands.LOGOUT)
fun Session.handleLogout(packet: Packet) {
    push(packet.respond())
    val playerEntity = player ?: return
    Logger.info("Logged out player ${playerEntity.displayName}")
    setAuthenticatedPlayer(null)
}

/**
 * Handles getting an authentication token for the
 * Galaxy at war http server. This is currently just
 * using the player id hex value instead of an actual
 * token
 *
 * @param packet The packet requesting the auth token
 */
@PacketHandler(Components.AUTHENTICATION, Commands.GET_AUTH_TOKEN)
fun Session.handleGetAuthToken(packet: Packet) {
    val playerEntity = player ?: throw NotAuthenticatedException()
    push(packet.respond {
        text("AUTH", playerEntity.playerId.toString(16).uppercase())
    })
}

/**
 * Handles logging into an existing player account using
 * the email and password.
 *
 * @param packet The packet requesting login
 */
@PacketHandler(Components.AUTHENTICATION, Commands.LOGIN)
fun Session.handleLogin(packet: Packet) {
    val email: String = packet.text("MAIL")
    val password: String = packet.text("PASS")
    if (email.isBlank() || password.isBlank()) { // If we are missing email or password
        return push(LoginError.INVALID_ACCOUNT(packet))
    }

    // Regex for matching emails
    val emailRegex = Regex("^[\\p{L}\\p{N}._%+-]+@[\\p{L}\\p{N}.\\-]+\\.\\p{L}{2,}$")
    if (!email.matches(emailRegex)) { // If the email is not a valid email
        return push(LoginError.INVALID_EMAIL(packet))
    }

    try {
        val database = Environment.database

        // Retrieve the player with this email or send an email not found error
        val player = database.getPlayerByEmail(email) ?: return push(LoginError.EMAIL_NOT_FOUND(packet))

        // Compare the provided password with the hashed password of the player
        if (!player.isMatchingPassword(password)) { // If it's not the same password
            return push(LoginError.WRONG_PASSWORD(packet))
        }

        doAuthenticate(player, packet, false)
    } catch (e: DatabaseException) {
        Logger.warn("Failed to login player", e)
        push(LoginError.SERVER_UNAVAILABLE(packet))
    }
}

/**
 * Handles silent behind the scenes token authentication for clients which
 * have already previously entered their credentials.
 *
 * @param packet The packet requesting silent login
 */
@PacketHandler(Components.AUTHENTICATION, Commands.SILENT_LOGIN)
fun Session.handleSilentLogin(packet: Packet) {
    val pid = packet.int("PID")
    val auth = packet.text("AUTH")
    try {
        val database = Environment.database
        // Find the player with a matching ID or send an INVALID_ACCOUNT error
        val player = database.getPlayerById(pid) ?: return push(LoginError.INVALID_ACCOUNT(packet))
        // If the session token's don't match send INVALID_ACCOUNT error
        if (!player.isSessionToken(auth)) return push(LoginError.INVALID_SESSION(packet))

        doAuthenticate(player, packet, true)
    } catch (e: IOException) {
        Logger.warn("Failed silent login", e)
        push(LoginError.SERVER_UNAVAILABLE(packet))
    }
}


/**
 * Handles the creation of a new account using the email
 * and password provided by the client
 *
 * @param packet The packet requesting account creation
 */
@PacketHandler(Components.AUTHENTICATION, Commands.CREATE_ACCOUNT)
fun Session.handleCreateAccount(packet: Packet) {
    val email: String = packet.text("MAIL")
    val password: String = packet.text("PASS")
    try {
        val database = Environment.database
        if (database.isEmailTaken(email)) {
            push(LoginError.EMAIL_ALREADY_IN_USE(packet))
            return
        }

        val hashedPassword = hashPassword(password)
        val player = database.createPlayer(email, hashedPassword)
        doAuthenticate(player, packet, false)
    } catch (e: DatabaseException) {
        Logger.warn("Failed to create account", e)
        push(LoginError.SERVER_UNAVAILABLE(packet))
    }
}

/**
 * Handles logging into personas. On the official EA servers this
 * would actually be handling with persona data but here it just
 * uses the player data instead
 *
 * @param packet The packet rquesting the login of a persona
 */
@PacketHandler(Components.AUTHENTICATION, Commands.LOGIN_PERSONA)
fun Session.handleLoginPersona(packet: Packet) {
    push(packet.respond { appendDetailsTo(this) })
    updateSessionFor(this)
}


/**
 * In the real EA implementation this handles sending out
 * password reset emails. This could be implemented in the
 * future but at the moment it only prints the email to the
 * output
 *
 * @param packet The packet requesting a forgot password email
 */
@PacketHandler(Components.AUTHENTICATION, Commands.PASSWORD_FORGOT)
fun Session.handlePasswordForgot(packet: Packet) {
    val mail = packet.text("MAIL") // The email of the account that wants a reset
    Logger.info("Recieved password reset for $mail")
    push(packet.respond())
}

/**
 * Functionality unknown
 *
 * Needs further investigation
 *
 * @param packet
 */
@PacketHandler(Components.AUTHENTICATION, Commands.GET_LEGAL_DOCS_INFO)
fun Session.handleGetLegalDocsInfo(packet: Packet) {
    push(packet.respond {
        number("EAMC", 0x0)
        text("LHST", "")
        number("PMC", 0x0)
        text("PPUI", "")
        text("TSUI", "")
    })
}

/**
 * Handles serving the terms of service content for displaying on the account login / creation
 * screen. The terms of service are rendered as an HTML markup so HTML tags can be used in this
 *
 * @param packet The packet requesting the terms of service content
 */
@PacketHandler(Components.AUTHENTICATION, Commands.GET_TERMS_OF_SERVICE_CONTENT)
fun Session.handleTermsOfServiceContent(packet: Packet) {
    // Terms of service is represented as HTML this is currently a placeholder value
    // in the future Ideally this would be editable from the web control
    val content = """
            <div style="font-family: Calibri; margin: 4px;"><h1>This is a terms of service placeholder</h1></div>
        """.trimIndent()
    push(packet.respond {
        // This is the URL of the page source this is prefixed by https://tos.ea.com/legalapp
        text("LDVC", "webterms/au/en/pc/default/09082020/02042022")
        number("TCOL", 0xdaed)
        text("TCOT", content) // The HTML contents of this legal doc
    })
}

/**
 * Handles serving the privacy policy content for displaying on
 * the account login / creation screen. The privacy policy is
 * rendered as an HTML markup so HTML tags can be used in this
 *
 * The current response is just a placeholder
 *
 * @param packet The packet requesting the privacy policy content
 */
@PacketHandler(Components.AUTHENTICATION, Commands.GET_PRIVACY_POLICY_CONTENT)
fun Session.handlePrivacyPolicyContent(packet: Packet) {
    // THe privacy policy is represented as HTML this is currently a placeholder value
    // in the future Ideally this would be editable from the web control
    val content = """
            <div style="font-family: Calibri; margin: 4px;"><h1>This is a privacy policy placeholder</h1></div>
        """.trimIndent()
    push(packet.respond {
        // This is the URL of the page source this is prefixed by https://tos.ea.com/legalapp
        text("LDVC", "webprivacy/au/en/pc/default/08202020/02042022")
        number("TCOL", 0xc99c)
        text("TCOT", content) // The HTML contents of this legal doc
    })
}

@PacketHandler(Components.AUTHENTICATION, Commands.GET_PASSWORD_RULES)
fun Session.handleGetPasswordRules(packet: Packet) {
    push(packet.respond {
        number("MAXS", 64)
        number("MINS", 4)
        // Valid characters
        text("VDCH", "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789[]`!@#$%^&*()_={}:;<>+-',.~?/|\\")
    })
}

/**
 * Handles retrieving the user entitlements list for
 * the authenticated player
 *
 * @param packet The packet requesting user entitlements
 */
@PacketHandler(Components.AUTHENTICATION, Commands.LIST_USER_ENTITLEMENTS_2)
fun Session.handleListUserEntitlements2(packet: Packet) {
    val etag = packet.text("ETAG")
    if (etag.isNotEmpty()) { // Empty responses for packets with ETAG's
        return push(packet.respond())
    }

    // Respond with the entitlements
    push(packet.respond {
        val pcTag = "ME3PCOffers"
        val generalTag = "ME3GenOffers"

        list(
            "NLST", listOf(
                // Project 10 = $10 Multiplayer Pass (Entitlement Required For Online Access)
                createEntitlement(pcTag, 0xec5090c43f, "303107", 2, "DR:229644400", "PROJECT10_CODE_CONSUMED", 1),
                createEntitlement(pcTag, 0xec3e4d793f, "304141", 2, "DR:230773600", "PROJECT10_CODE_CONSUMED_LE1", 1),

                // Jeeze so many online pass entitlements
                createEntitlement(pcTag, 0xec50b255ff, "300241", 2, "OFB-MASS:44370", "ONLINE_ACCESS", 1),
                createEntitlement(pcTag, 0xec50a620ff, "300241", 2, "OFB-MASS:49465", "ONLINE_ACCESS", 1),
                createEntitlement(pcTag, 0xec508db6ff, "303107", 2, "DR:229644400", "ONLINE_ACCESS", 1),
                createEntitlement(pcTag, 0xec3e5393bf, "300241", 2, "OFB-EAST:46112", "ONLINE_ACCESS", 1),
                createEntitlement(pcTag, 0xec3e50867f, "304141", 2, "DR:230773600", "ONLINE_ACCESS", 1),
                createEntitlement(generalTag, 0xec4495bfff, "303107", 0, "", "ONLINE_ACCESS_GAW_PC", 1),
                createEntitlement(generalTag, 0xea234c3e7f, "303107", 2, "", "ONLINE_ACCESS_GAW_XBL2", 1),

                // Singleplayer DLC
                createEntitlement(pcTag, 0xec3e62d5ff, "300241", 2, "OFB-MASS:51074", "ME3_PRC_EXTENDEDCUT", 5), // Extended Cut DLC
                createEntitlement(pcTag, 0xec50b5633f, "300241", 2, "OFB-MASS:44370", "ME3_PRC_PROTHEAN", 5), // From Ashes DLC
                createEntitlement(pcTag, 0xec3e56a0ff, "300241", 2, "OFB-EAST:46112", "ME3_PRC_PROTHEAN", 5), // From Ashes DLC
                createEntitlement(pcTag, 0xec50b8707f, "300241", 2, "OFB-MASS:52001", "ME3_PRC_LEVIATHAN", 5), // Leviathan DLC
                createEntitlement(pcTag, 0xec50ac3b7f, "300241", 2, "OFB-MASS:55146", "ME3_PRC_OMEGA", 5), // Omega DLC
                createEntitlement(pcTag, 0xec50af48bf, "300241", 2, "OFB-MASS:57550", "ME3_PRC_CITADEL", 5), // Citadel DLC
                createEntitlement(pcTag, 0xec5093d17f, "300241", 2, "OFB-EAST:58040", "MET_BONUS_CONTENT_DW", 5),

                // Singleplayer Packs
                createEntitlement(pcTag, 0xec50bb7dbf, "300241", 2, "OFB-MASS:56984", "ME3_MTX_APP01", 5), // Alternate Appearance Pack 1
                createEntitlement(pcTag, 0xec5099ebff, "300241", 2, "OFB-MASS:49032", "ME3_MTX_GUN01", 5), // Firefight Pack
                createEntitlement(pcTag, 0xec50c1983f, "300241", 2, "OFB-MASS:55147", "ME3_MTX_GUN02", 5), // Groundside Resistance Pack

                // Multiplayer DLC
                createEntitlement(pcTag, 0xec50a0067f, "300241", 2, "OFB-MASS:47872", "ME3_PRC_RESURGENCE", 5), // Resurgence DLC
                createEntitlement(pcTag, 0xec50a92e3f, "300241", 2, "OFB-MASS:49465", "ME3_PRC_REBELLION", 5), // Rebellion DLC
                createEntitlement(pcTag, 0xec5096debf, "300241", 2, "OFB-MASS:51073", "ME3_PRC_EARTH", 5), // Earth DLC
                createEntitlement(pcTag, 0xec509cf93f, "300241", 2, "OFB-MASS:52000", "ME3_PRC_GOBIG", 5), // Retaliation DLC
                createEntitlement(pcTag, 0xec50a313bf, "300241", 2, "OFB-MASS:59712", "ME3_PRC_MP5", 5), // Recokoning DLC

                // Collectors Edition
                createEntitlement(pcTag, 0xec3e5fc8bf, "300241", 2, "OFB-MASS:46484", "ME3_MTX_COLLECTORS_EDITION", 5),
                createEntitlement(pcTag, 0xec3e5cbb7f, "300241", 2, "OFB-MASS:46483", "ME3_MTX_DIGITAL_ART_BOOKS", 5),
                createEntitlement(generalTag, 0xec3e59ae3f, "300241", 2, "OFB-MASS:46482", "ME3_MTX_SOUNDTRACK", 5),

                // Darkhorse Redeem Code (Character boosters and Collector Assault Rifle)
                createEntitlement(pcTag, 0xec50be8aff, "300241", 2, "OFB-MASS:61524", "ME3_PRC_DARKHORSECOMIC", 5),
            )
        )
    })
}

private fun createEntitlement(
    name: String,
    id: Long,
    pjid: String,
    prca: Int,
    prid: String,
    tag: String,
    type: Int,
): GroupTdf {
    return group {
        text("DEVI", "")
        text("GDAY", "2012-12-15T16:15Z")
        text("GNAM", name)
        number("ID", id)
        number("ISCO", 0)
        number("PID", 0)
        text("PJID", pjid)
        number("PRCA", prca)
        text("PRID", prid)
        number("STAT", 1)
        number("STRC", 0)
        text("TAG", tag)
        text("TDAY", "")
        number("TYPE", type)
        number("UCNT", 0)
        number("VER", 0)
    }
}
