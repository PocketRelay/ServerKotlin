package com.jacobtread.kme.servers.http.routes

import com.jacobtread.kme.Environment
import com.jacobtread.kme.database.entities.GalaxyAtWarEntity
import com.jacobtread.kme.database.byId
import com.jacobtread.kme.database.entities.PlayerEntity
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
            element("canageup", 0)
            element("legaldochost")
            element("needslegaldoc", 0)
            element("pclogintoken", playerEntity.sessionToken)
            element("privacypolicyuri")
            element("sessioninfo") {
                element("blazeuserid", playerEntity.playerId)
                element("isfirstlogin", "0")
                element("sessionkey", playerEntity.playerId.toString(16))
                element("lastlogindatetime", time)
                element("email", playerEntity.email)
                element("personadetails") {
                    element("displayname", playerEntity.displayName)
                    element("lastauthenticated", time)
                    element("personaid", playerEntity.playerId)
                    element("status", "UNKNOWN")
                    element("extid", "0")
                    element("exttype", "BLAZE_EXTERNAL_REF_TYPE_UNKNOWN")
                }
                element("userid", playerEntity.playerId)
            }
            element("isoflegalcontactage", 0)
            element("toshost")
            element("termsofserviceuri")
            element("tosuri")
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
        element("ratings") {
            element("ratings", rating.groupA)
            element("ratings", rating.groupB)
            element("ratings", rating.groupC)
            element("ratings", rating.groupD)
            element("ratings", rating.groupE)
        }
        element("level", level)
        element("assets") {
            element("assets", promotions)
            repeat(9) { element("assets", 0) }
        }
    }
}