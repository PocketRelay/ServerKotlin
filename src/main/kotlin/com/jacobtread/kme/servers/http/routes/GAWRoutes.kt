package com.jacobtread.kme.servers.http.routes

import com.jacobtread.kme.Environment
import com.jacobtread.kme.database.data.GalaxyAtWarData
import com.jacobtread.kme.database.data.Player
import com.jacobtread.kme.exceptions.DatabaseException
import com.jacobtread.kme.servers.http.router.*
import com.jacobtread.kme.tools.unixTimeSeconds
import com.jacobtread.kme.logging.Logger
import io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST
import io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR

/**
 * routeGroupGAW Adds routing for the galaxy at war
 * api endpoints group (/gaw)
 */
fun RoutingGroup.routeGroupGAW() {
    group("gaw") {
        routeAuthentication()
        routeRatings()
        routeIncreaseRatings()
    }
}

/**
 * routeAuthentication Adds the route which handles Galaxy at War authentication
 * this authentication isn't implemented properly. This should be using
 * a session token, but currently it just accepts the userID
 */
private fun RoutingGroup.routeAuthentication() {
    get("authentication/sharedTokenLogin") {
        val playerId = queryInt("auth", 16)
        try {
            val database = Environment.database
            val player = database.getPlayerById(playerId) ?: return@get response(BAD_REQUEST)
            Logger.debug("Authenticated GAW User ${player.displayName}")
            val time = unixTimeSeconds()
            responseXml("fulllogin") {
                textNode("canageup", 0)
                node("legaldochost")
                textNode("needslegaldoc", 0)
                textNode("pclogintoken", player.getSessionToken())
                node("privacypolicyuri")
                node("sessioninfo") {
                    textNode("blazeuserid", player.playerId)
                    textNode("isfirstlogin", "0")
                    textNode("sessionkey", player.playerId.toString(16))
                    textNode("lastlogindatetime", time)
                    textNode("email", player.email)
                    node("personadetails") {
                        textNode("displayname", player.displayName)
                        textNode("lastauthenticated", time)
                        textNode("personaid", player.playerId)
                        textNode("status", "UNKNOWN")
                        textNode("extid", "0")
                        textNode("exttype", "BLAZE_EXTERNAL_REF_TYPE_UNKNOWN")
                    }
                    textNode("userid", player.playerId)
                }
                textNode("isoflegalcontactage", 0)
                node("toshost")
                node("termsofserviceuri")
                node("tosuri")
            }
        } catch (e: DatabaseException) {
            Logger.warn("Failed to authenticate gaw", e)
            response(INTERNAL_SERVER_ERROR)
        }
    }
}

/**
 * routeRatings Adds the route which handles Galaxy at war ratings
 * for the players with the provided player ID
 */
private fun RoutingGroup.routeRatings() {
    get("galaxyatwar/getRatings/:id") {
        val playerId = paramInt("id", 16)
        try {
            val database = Environment.database
            val player = database.getPlayerById(playerId)
                ?: return@get response(BAD_REQUEST)
            val rating = player.getGalaxyAtWarData()
            respondRatings(player, rating)
        } catch (e: DatabaseException) {
            Logger.warn("Failed to get route ratings")
            response(INTERNAL_SERVER_ERROR)
        }
    }
}

/**
 * routeIncreaseRatings Adds the route which handles Galaxy at War
 * ratings increasing this is call by the ME3 client
 */
private fun RoutingGroup.routeIncreaseRatings() {
    get("galaxyatwar/increaseRatings/:id") {
        try {
            val database = Environment.database
            val playerId = paramInt("id", 16)
            val player = database.getPlayerById(playerId) ?: return@get response(BAD_REQUEST)
            val rating = player.getGalaxyAtWarData()
            rating.add(
                queryInt("rinc|0", default = 0),
                queryInt("rinc|1", default = 0),
                queryInt("rinc|2", default = 0),
                queryInt("rinc|3", default = 0),
                queryInt("rinc|4", default = 0)
            )
            database.setGalaxyAtWarData(player, rating)

            respondRatings(player, rating)
        } catch (e: DatabaseException) {
            Logger.warn("Failed to increase ratings", e)
            response(INTERNAL_SERVER_ERROR)
        }
    }
}

/**
 * respondRatings Responds to the provided request with the galaxy at war
 * ratings for the provided player
 *
 * @param player The player to use the data from
 * @param rating The player galaxy at war rating data
 * @return The created ratings response
 */
private fun respondRatings(player: Player, rating: GalaxyAtWarData): HttpResponse {
    val level = rating.average
    val promotions: Int = if (Environment.gawEnabledPromotions) player.getTotalPromotions() else 0
    return responseXml("galaxyatwargetratings") {
        node("ratings") {
            textNode("ratings", rating.groupA)
            textNode("ratings", rating.groupB)
            textNode("ratings", rating.groupC)
            textNode("ratings", rating.groupD)
            textNode("ratings", rating.groupE)
        }
        textNode("level", level)
        node("assets") {
            textNode("assets", promotions)
            repeat(9) { textNode("assets", 0) }
        }
    }
}

