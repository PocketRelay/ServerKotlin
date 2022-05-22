package com.jacobtread.kme.servers.http.controllers

import com.jacobtread.kme.database.Player
import com.jacobtread.kme.database.PlayerSettingsBase
import com.jacobtread.kme.servers.http.router.GroupRoute
import com.jacobtread.kme.servers.http.router.RouteFunction
import com.jacobtread.kme.servers.http.router.groupedRoute
import io.netty.handler.codec.http.HttpResponseStatus
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.transactions.transaction

object APIController : GroupRoute("api") {

    @Serializable
    data class PlayerSerial(
        val id: Int,
        val email: String,
        val displayName: String,
        val settings: PlayerSettingsBase,
    )

    private val PlayersList = RouteFunction { _, request ->
        val limit = request.queryInt("limit", default = 10)
        val offset = request.queryInt("offset", default = 0)
        val playersList = transaction {
            Player.all()
                .limit(limit, (offset * limit).toLong())
                .map {
                    PlayerSerial(
                        it.playerId,
                        it.email,
                        it.displayName,
                        it.getSettingsBase()
                    )
                }
        }
        request.json(playersList)
    }

    private val GetPlayerSettings = RouteFunction { _, request ->
        val playerId = request.queryInt("id")
        val player = Player.getById(playerId)
            ?: return@RouteFunction request.response(HttpResponseStatus.BAD_REQUEST)
        request.json(player.createSettingsMap())
    }

    private val SetPlayerSettings = RouteFunction { _, request ->
        val playerId = request.queryInt("id")
        val player = Player.getById(playerId)
            ?: return@RouteFunction request.response(HttpResponseStatus.BAD_REQUEST)
        val settings = request.contentJson<Map<String, String>>()
        settings?.forEach { player.setSetting(it.key, it.value) }
    }

    private val UpdatePlayer = RouteFunction { _, request ->
        val player = request.contentJson<PlayerSerial>()
            ?: return@RouteFunction request.response(HttpResponseStatus.BAD_REQUEST)
        val existing = Player.getById(player.id)
            ?: return@RouteFunction request.response(HttpResponseStatus.BAD_REQUEST)
        transaction {
            existing.displayName = player.displayName
            existing.email = player.email
            if (existing.settingsBase != null) {
                val existingBase = existing.getSettingsBase()
                val clone = PlayerSettingsBase(
                    player.settings.credits,
                    existingBase.c,
                    existingBase.d,
                    player.settings.creditsSpent,
                    existingBase.e,
                    player.settings.gamesPlayed,
                    player.settings.secondsPlayed,
                    existingBase.f,
                    player.settings.inventory
                )
                existing.settingsBase = clone.mapValue()
            }
        }
        request.response(HttpResponseStatus.OK)
    }

    init {
        get("players", PlayersList)
        get("playerSettings", GetPlayerSettings)

        post("updatePlayer", UpdatePlayer)
        post("setPlayerSettings", SetPlayerSettings)
    }
}