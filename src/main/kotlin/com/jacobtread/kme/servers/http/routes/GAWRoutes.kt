package com.jacobtread.kme.servers.http.routes

import com.jacobtread.kme.Config
import com.jacobtread.kme.database.Player
import com.jacobtread.kme.database.PlayerGalaxyAtWar
import com.jacobtread.kme.servers.http.WrappedRequest
import com.jacobtread.kme.servers.http.router.GroupRoute
import com.jacobtread.kme.servers.http.router.RouteFunction
import com.jacobtread.kme.servers.http.router.RoutingGroup
import com.jacobtread.kme.servers.http.router.group
import com.jacobtread.kme.utils.logging.Logger
import com.jacobtread.kme.utils.unixTimeSeconds
import io.netty.handler.codec.http.HttpResponseStatus
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.math.min

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
    get("authentication/sharedTokenLogin") { _, request ->
        val playerId = request.queryInt("auth", 16)
        val player = Player.getById(playerId)
            ?: return@get request.response(HttpResponseStatus.BAD_REQUEST)
        Logger.debug("Authenticated GAW User ${player.displayName}")
        val time = unixTimeSeconds()
        @Suppress("SpellCheckingInspection")
        request.xml("fulllogin") {
            element("canageup", 0)
            element("legaldochost")
            element("needslegaldoc", 0)
            element("pclogintoken", player.sessionToken)
            element("privacypolicyuri")
            element("sessioninfo") {
                element("blazeuserid", player.playerId)
                element("isfirstlogin", "0")
                element("sessionkey", player.playerId)
                element("lastlogindatetime", time)
                element("email", player.email)
                element("personadetails") {
                    element("displayname", player.displayName)
                    element("lastauthenticated", time)
                    element("personaid", player.playerId)
                    element("status", "UNKNOWN")
                    element("extid", "0")
                    element("exttype", "BLAZE_EXTERNAL_REF_TYPE_UNKNOWN")
                }
                element("userid", player.playerId)
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
    get("galaxyatwar/getRatings/:id") { config, request ->
        val playerId = request.paramInt("id", 16)
        val player = Player.getById(playerId)
            ?: return@get request.response(HttpResponseStatus.BAD_REQUEST)
        val rating = player.getOrCreateGAW(config.gaw)
        respondRatings(config, request, player, rating)
    }
}

/**
 * routeIncreaseRatings Adds the route which handles Galaxy at War
 * ratings increasing this is call by the ME3 client
 */
private fun RoutingGroup.routeIncreaseRatings() {
    get("galaxyatwar/increaseRatings/:id") { config, request ->
        val playerId = request.paramInt("id", 16)
        val player = Player.getById(playerId)
            ?: return@get request.response(HttpResponseStatus.BAD_REQUEST)
        val rating = player.getOrCreateGAW(config.gaw)
        val maxValue = 10099
        transaction {
            rating.apply {
                timestamp = unixTimeSeconds()
                a = min(maxValue, a + request.queryInt("rinc|0", default=0))
                b = min(maxValue, b + request.queryInt("rinc|1", default=0))
                c = min(maxValue, c + request.queryInt("rinc|2", default=0))
                d = min(maxValue, d + request.queryInt("rinc|3", default=0))
                e = min(maxValue, e + request.queryInt("rinc|4", default=0))
            }
        }
        respondRatings(config, request, player, rating)
    }
}

/**
 * respondRatings Responds to the provided request with the galaxy at war
 * ratings for the provided player
 *
 * @param config The server configuration
 * @param request The request to respond to
 * @param player The player to use the data from
 * @param rating The player galaxy at war rating data
 */
private fun respondRatings(
    config: Config,
    request: WrappedRequest,
    player: Player,
    rating: PlayerGalaxyAtWar,
) {
    val level = rating.average()
    val promotions = if (config.gaw.enablePromotions) player.getTotalPromotions() else 0
    @Suppress("SpellCheckingInspection")
    request.xml("galaxyatwargetratings") {
        element("ratings") {
            element("ratings", rating.a)
            element("ratings", rating.b)
            element("ratings", rating.c)
            element("ratings", rating.d)
            element("ratings", rating.e)
        }
        element("level", level)
        element("assets") {
            element("assets", promotions)
            repeat(9) { element("assets", 0) }
        }
    }
}