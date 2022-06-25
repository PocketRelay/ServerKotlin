package com.jacobtread.kme.servers

import com.jacobtread.kme.blaze.Commands
import com.jacobtread.kme.blaze.Components
import com.jacobtread.kme.blaze.Packet
import com.jacobtread.kme.blaze.annotations.PacketHandler
import com.jacobtread.kme.blaze.annotations.PacketProcessor
import io.netty.channel.SimpleChannelInboundHandler

@PacketProcessor
interface MainPacketProcessor {

    //region AUTHENTICATION

    @PacketHandler(Components.AUTHENTICATION, Commands.LIST_USER_ENTITLEMENTS_2)
    fun handleListUserEntitlements2(packet: Packet)

    @PacketHandler(Components.AUTHENTICATION, Commands.GET_AUTH_TOKEN)
    fun handleGetAuthToken(packet: Packet)

    @PacketHandler(Components.AUTHENTICATION, Commands.LOGIN)
    fun handleLogin(packet: Packet)

    @PacketHandler(Components.AUTHENTICATION, Commands.SILENT_LOGIN)
    fun handleSilentLogin(packet: Packet)

    @PacketHandler(Components.AUTHENTICATION, Commands.LOGIN_PERSONA)
    fun handleLoginPersona(packet: Packet)

    @PacketHandler(Components.AUTHENTICATION, Commands.ORIGIN_LOGIN)
    fun handleOriginLogin(packet: Packet)

    @PacketHandler(Components.AUTHENTICATION, Commands.CREATE_ACCOUNT)
    fun handleCreateAccount(packet: Packet)

    @PacketHandler(Components.AUTHENTICATION, Commands.LOGOUT)
    fun handleLogout(packet: Packet)

    @PacketHandler(Components.AUTHENTICATION, Commands.PASSWORD_FORGOT)
    fun handlePasswordForgot(packet: Packet)

    @PacketHandler(Components.AUTHENTICATION, Commands.GET_LEGAL_DOCS_INFO)
    fun handleGetLegalDocsInfo(packet: Packet)

    @PacketHandler(Components.AUTHENTICATION, Commands.GET_TERMS_OF_SERVICE_CONTENT)
    fun handleTermsOfServiceContent(packet: Packet)

    @PacketHandler(Components.AUTHENTICATION, Commands.GET_PRIVACY_POLICY_CONTENT)
    fun handlePrivacyPolicyContent(packet: Packet)

    //endregion

    //region GAME_MANAGER

    @PacketHandler(Components.GAME_MANAGER, Commands.CREATE_GAME)
    fun handleCreateGame(packet: Packet)

    @PacketHandler(Components.GAME_MANAGER, Commands.ADVANCE_GAME_STATE)
    fun handleAdvanceGameState(packet: Packet)

    @PacketHandler(Components.GAME_MANAGER, Commands.SET_GAME_SETTINGS)
    fun handleSetGameSettings(packet: Packet)

    @PacketHandler(Components.GAME_MANAGER, Commands.SET_GAME_ATTRIBUTES)
    fun handleSetGameAttributes(packet: Packet)

    @PacketHandler(Components.GAME_MANAGER, Commands.REMOVE_PLAYER)
    fun handleRemovePlayer(packet: Packet)

    @PacketHandler(Components.GAME_MANAGER, Commands.START_MATCHMAKING)
    fun handleStartMatchmaking(packet: Packet)

    @PacketHandler(Components.GAME_MANAGER, Commands.CANCEL_MATCHMAKING)
    fun handleCancelMatchmaking(packet: Packet)

    @PacketHandler(Components.GAME_MANAGER, Commands.UPDATE_MESH_CONNECTION)
    fun handleUpdateMeshConnection(packet: Packet)

    //endregion

    //region STATS

    @PacketHandler(Components.STATS, Commands.GET_LEADERBOARD_GROUP)
    fun handleLeaderboardGroup(packet: Packet)

    @PacketHandler(Components.STATS, Commands.GET_FILTERED_LEADERBOARD)
    fun handleFilteredLeaderboard(packet: Packet)

    @PacketHandler(Components.STATS, Commands.GET_LEADERBOARD_ENTITY_COUNT)
    fun handleLeaderboardEntityCount(packet: Packet)

    @PacketHandler(Components.STATS, Commands.GET_CENTERED_LEADERBOARD)
    fun handleCenteredLeadboard(packet: Packet)

    //endregion

    // region MESSAGING

    @PacketHandler(Components.MESSAGING, Commands.FETCH_MESSAGES)
    fun handleFetchMessages(packet: Packet)

    //endregion

    // region ASSOCIATION_LISTS

    @PacketHandler(Components.ASSOCIATION_LISTS, Commands.GET_LISTS)
    fun handleAssociationListGetLists(packet: Packet)

    //endregion

    // region GAME_REPORTING

    @PacketHandler(Components.GAME_REPORTING, Commands.SUBMIT_OFFLINE_GAME_REPORT)
    fun handleSubmitOfflineReport(packet: Packet)

    //endregion

    // region USER_SESSIONS

    @PacketHandler(Components.USER_SESSIONS, Commands.UPDATE_HARDWARE_FLAGS)
    fun updateHardwareFlag(packet: Packet)

    @PacketHandler(Components.USER_SESSIONS, Commands.UPDATE_NETWORK_INFO)
    fun updateSessionNetworkInfo(packet: Packet)

    @PacketHandler(Components.USER_SESSIONS, Commands.RESUME_SESSION)
    fun handleResumeSession(packet: Packet)

    //endregion

    // region UTIL

    @PacketHandler(Components.UTIL, Commands.FETCH_CLIENT_CONFIG)
    fun handleFetchClientConfig(packet: Packet)

    @PacketHandler(Components.UTIL, Commands.PING)
    fun handlePing(packet: Packet)

    @PacketHandler(Components.UTIL, Commands.PRE_AUTH)
    fun handlePreAuth(packet: Packet)

    @PacketHandler(Components.UTIL, Commands.POST_AUTH)
    fun handlePostAuth(packet: Packet)

    @PacketHandler(Components.UTIL, Commands.USER_SETTINGS_SAVE)
    fun handleUserSettingsSave(packet: Packet)

    @PacketHandler(Components.UTIL, Commands.USER_SETTINGS_LOAD_ALL)
    fun handleUserSettingsLoadAll(packet: Packet)

    @PacketHandler(Components.UTIL, Commands.SUSPEND_USER_PING)
    fun handleSuspendUserPing(packet: Packet)

    @PacketHandler(Components.UTIL, Commands.SET_CLIENT_METRICS)
    fun handleClientMetrics(packet: Packet)

    //endregion

}