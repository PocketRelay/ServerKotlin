package com.jacobtread.kme.servers.http.routes

import com.jacobtread.kme.Environment
import com.jacobtread.kme.database.byId
import com.jacobtread.kme.database.old.entities.GalaxyAtWarEntity
import com.jacobtread.kme.database.old.entities.PlayerEntity
import com.jacobtread.kme.servers.http.router.*
import com.jacobtread.kme.tools.unixTimeSeconds
import com.jacobtread.kme.utils.logging.Logger
import io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST

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
        val playerEntity = PlayerEntity.byId(playerId) ?: return@get response(BAD_REQUEST)
        Logger.debug("Authenticated GAW User ${playerEntity.displayName}")
        val time = unixTimeSeconds()
        responseXml("fulllogin") {
            textNode("canageup", 0)
            node("legaldochost")
            textNode("needslegaldoc", 0)
            textNode("pclogintoken", playerEntity.sessionToken)
            node("privacypolicyuri")
            node("sessioninfo") {
                textNode("blazeuserid", playerEntity.playerId)
                textNode("isfirstlogin", "0")
                textNode("sessionkey", playerEntity.playerId.toString(16))
                textNode("lastlogindatetime", time)
                textNode("email", playerEntity.email)
                node("personadetails") {
                    textNode("displayname", playerEntity.displayName)
                    textNode("lastauthenticated", time)
                    textNode("personaid", playerEntity.playerId)
                    textNode("status", "UNKNOWN")
                    textNode("extid", "0")
                    textNode("exttype", "BLAZE_EXTERNAL_REF_TYPE_UNKNOWN")
                }
                textNode("userid", playerEntity.playerId)
            }
            textNode("isoflegalcontactage", 0)
            node("toshost")
            node("termsofserviceuri")
            node("tosuri")
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
        val playerEntity = PlayerEntity.byId(playerId)
            ?: return@get response(BAD_REQUEST)
        val rating = playerEntity.galaxyAtWar
        respondRatings(playerEntity, rating)
    }
}

/**
 * routeIncreaseRatings Adds the route which handles Galaxy at War
 * ratings increasing this is call by the ME3 client
 */
private fun RoutingGroup.routeIncreaseRatings() {
    get("galaxyatwar/increaseRatings/:id") {
        val playerId = paramInt("id", 16)
        val playerEntity = PlayerEntity.byId(playerId) ?: return@get response(BAD_REQUEST)
        val rating = playerEntity.galaxyAtWar
        rating.add(
            queryInt("rinc|0", default = 0),
            queryInt("rinc|1", default = 0),
            queryInt("rinc|2", default = 0),
            queryInt("rinc|3", default = 0),
            queryInt("rinc|4", default = 0)
        )
        respondRatings(playerEntity, rating)
    }
}

/**
 * respondRatings Responds to the provided request with the galaxy at war
 * ratings for the provided player
 *
 * @param playerEntity The player to use the data from
 * @param rating The player galaxy at war rating data
 * @return The created ratings response
 */
private fun respondRatings(playerEntity: PlayerEntity, rating: GalaxyAtWarEntity): HttpResponse {
    val level = rating.average
    val promotions = if (Environment.gawEnabledPromotions) playerEntity.totalPromotions else 0
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