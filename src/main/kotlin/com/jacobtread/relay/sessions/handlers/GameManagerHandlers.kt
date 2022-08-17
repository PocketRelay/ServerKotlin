package com.jacobtread.relay.sessions.handlers

import com.jacobtread.blaze.*
import com.jacobtread.blaze.annotations.PacketHandler
import com.jacobtread.blaze.packet.Packet
import com.jacobtread.blaze.tdf.types.GroupTdf
import com.jacobtread.relay.data.blaze.Commands
import com.jacobtread.relay.data.blaze.Components
import com.jacobtread.relay.game.Game
import com.jacobtread.relay.game.match.MatchRuleSet
import com.jacobtread.relay.game.match.Matchmaking
import com.jacobtread.relay.sessions.Session
import com.jacobtread.relay.utils.logging.Logger

/**
 * Handles the creation of a new game using the
 * attributes provided by the client
 *
 * @param packet The packet creating the game
 */
@PacketHandler(Components.GAME_MANAGER, Commands.CREATE_GAME)
fun Session.handleCreateGame(packet: Packet) {
    val attributes = packet.map<String, String>("ATTR") // Get the provided users attributes
    val game = Game.create(this, attributes) // Create a new game

    // Get the host networking values from the HNET list
    val hostNetworking = packet.list<GroupTdf>("HNET")

    // First value is always the HNET value
    val netGroup = hostNetworking.firstOrNull()
    if (netGroup != null) setNetworkingFromGroup(netGroup)

    push(packet.respond { number("GID", game.id) }) // Send the user session

    game.join(this)

    Matchmaking.onGameCreated(game)
}

/**
 * Handles updating the state of a game. States are not
 * currently documented at this point. Further investation
 * needs to happen to understand what each id means
 *
 * @param packet The packet updating the game state
 */
@PacketHandler(Components.GAME_MANAGER, Commands.ADVANCE_GAME_STATE)
fun Session.handleAdvanceGameState(packet: Packet) {
    val gameId = packet.ulong("GID")
    val gameState = packet.int("GSTA")
    val game = Game.getById(gameId)
    game?.setGameState(gameState)
    push(packet.respond())
}

/**
 * Handles updating the game setting.
 *
 * Needs further investigation for proper documentation
 *
 * @param packet
 */
@PacketHandler(Components.GAME_MANAGER, Commands.SET_GAME_SETTINGS)
fun Session.handleSetGameSettings(packet: Packet) {
    val gameId = packet.ulong("GID")
    val setting = packet.int("GSET")
    val game = Game.getById(gameId)
    game?.setGameSetting(setting)
    push(packet.respond())
}

/**
 * Handles updating a games attributes based on the newly
 * provided attributes from the client. This packet is
 * recieved when things like the enemy type or map change
 *
 * @param packet The packet that is setting the game attributes
 */
@PacketHandler(Components.GAME_MANAGER, Commands.SET_GAME_ATTRIBUTES)
fun Session.handleSetGameAttributes(packet: Packet) {
    val gameId = packet.ulong("GID")
    val attributes = packet.map<String, String>("ATTR")
    val game = Game.getById(gameId)
    game?.setAttributes(attributes)
    push(packet.respond())
}

/**
 * Handles removing a player from a game based on that
 * player's ID and the game ID
 *
 * @param packet The packet requesting the player ID
 */
@PacketHandler(Components.GAME_MANAGER, Commands.REMOVE_PLAYER)
fun Session.handleRemovePlayer(packet: Packet) {
    val playerId = packet.int("PID")
    val gameId = packet.ulong("GID")
    val game = Game.getById(gameId)
    game?.removePlayerById(playerId)
    push(packet.respond())
}

/**
 * Handles matchmaking for players. Currently, this implementation
 * works if a game already exists but the queue system currently
 * does not work properly
 *
 * @param packet The packet requesting the matchmaking start
 */
@PacketHandler(Components.GAME_MANAGER, Commands.START_MATCHMAKING)
fun Session.handleStartMatchmaking(packet: Packet) {
    val playerEntity = player ?: throw NotAuthenticatedException()
    Logger.info("Player ${playerEntity.displayName} started match making")
    val ruleSet = MatchRuleSet(packet)
    val game = Matchmaking.getMatchOrQueue(this, ruleSet)
    push(packet.respond { number("MSID", matchmakingId) })
    if (game != null) {
        Logger.info("Found matching game for player ${playerEntity.displayName}")
        game.join(this)
    }
}

/**
 * Handles the player cancelling a matchmaking request. Removes the player
 * from the matchmaking queue along with any games they managed to get into
 * in the process.
 *
 * @param packet The packet requesting the matchmaking cancel
 */
@PacketHandler(Components.GAME_MANAGER, Commands.CANCEL_MATCHMAKING)
fun Session.handleCancelMatchmaking(packet: Packet) {
    val playerEntity = player ?: throw NotAuthenticatedException()
    Logger.info("Player ${playerEntity.displayName} cancelled match making")
    Matchmaking.removeFromQueue(this)
    removeFromGame()
    push(packet.respond())
}


/**
 * Handles updating a connection state across the mesh network between players
 * and hosts of a game.
 *
 * @param packet The packet requesting the mesh update
 */
@PacketHandler(Components.GAME_MANAGER, Commands.UPDATE_MESH_CONNECTION)
fun Session.handleUpdateMeshConnection(packet: Packet) {
    val gameId = packet.ulong("GID")
    push(packet.respond())
    val game = Game.getById(gameId) ?: return
    game.updateMeshConnection(this)
}
