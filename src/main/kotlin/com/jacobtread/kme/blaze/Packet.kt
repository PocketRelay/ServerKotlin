package com.jacobtread.kme.blaze

import io.netty.buffer.ByteBuf


class Packet(
    val length: Int,
    val component: Int,
    val command: Int,
    val error: Int,
    val qtype: Int,
    val id: Int,
    val content: ByteArray,
) {
    companion object {
        val CommandNames = mapOf(
            //Authentication Component
            0x0001000A to "createAccount",
            0x00010014 to "updateAccount",
            0x0001001C to "updateParentalEmail",
            0x0001001D to "listUserEntitlements2",
            0x0001001E to "getAccount",
            0x0001001F to "grantEntitlement",
            0x00010020 to "listEntitlements",
            0x00010021 to "hasEntitlement",
            0x00010022 to "getUseCount",
            0x00010023 to "decrementUseCount",
            0x00010024 to "getAuthToken",
            0x00010025 to "getHandoffToken",
            0x00010026 to "getPasswordRules",
            0x00010027 to "grantEntitlement2",
            0x00010028 to "login",
            0x00010029 to "acceptTos",
            0x0001002A to "getTosInfo",
            0x0001002B to "modifyEntitlement2",
            0x0001002C to "consumecode",
            0x0001002D to "passwordForgot",
            0x0001002E to "getTermsAndConditionsContent",
            0x0001002F to "getPrivacyPolicyContent",
            0x00010030 to "listPersonaEntitlements2",
            0x00010032 to "silentLogin",
            0x00010033 to "checkAgeReq",
            0x00010034 to "getOptIn",
            0x00010035 to "enableOptIn",
            0x00010036 to "disableOptIn",
            0x0001003C to "expressLogin",
            0x00010046 to "logout",
            0x00010050 to "createPersona",
            0x0001005A to "getPersona",
            0x00010064 to "listPersonas",
            0x0001006E to "loginPersona",
            0x00010078 to "logoutPersona",
            0x0001008C to "deletePersona",
            0x0001008D to "disablePersona",
            0x0001008F to "listDeviceAccounts",
            0x00010096 to "xboxCreateAccount",
            0x00010098 to "originLogin",
            0x000100A0 to "xboxAssociateAccount",
            0x000100AA to "xboxLogin",
            0x000100B4 to "ps3CreateAccount",
            0x000100BE to "ps3AssociateAccount",
            0x000100C8 to "ps3Login",
            0x000100D2 to "validateSessionKey",
            0x000100E6 to "createWalUserSession",
            0x000100F1 to "acceptLegalDocs",
            0x000100F2 to "getLegalDocsInfo",
            0x000100F6 to "getTermsOfServiceContent",
            0x0001012C to "deviceLoginGuest",
            // Game Manager Component
            0x00040001 to "createGame",
            0x00040002 to "destroyGame",
            0x00040003 to "advanceGameState",
            0x00040004 to "setGameSettings",
            0x00040005 to "setPlayerCapacity",
            0x00040006 to "setPresenceMode",
            0x00040007 to "setGameAttributes",
            0x00040008 to "setPlayerAttributes",
            0x00040009 to "joinGame",
            0x0004000B to "removePlayer",
            0x0004000D to "startMatchmaking",
            0x0004000E to "cancelMatchmaking",
            0x0004000F to "finalizeGameCreation",
            0x00040011 to "listGames",
            0x00040012 to "setPlayerCustomData",
            0x00040013 to "replayGame",
            0x00040014 to "returnDedicatedServerToPool",
            0x00040015 to "joinGameByGroup",
            0x00040016 to "leaveGameByGroup",
            0x00040017 to "migrateGame",
            0x00040018 to "updateGameHostMigrationStatus",
            0x00040019 to "resetDedicatedServer",
            0x0004001A to "updateGameSession",
            0x0004001B to "banPlayer",
            0x0004001D to "updateMeshConnection",
            0x0004001F to "removePlayerFromBannedList",
            0x00040020 to "clearBannedList",
            0x00040021 to "getBannedList",
            0x00040026 to "addQueuedPlayerToGame",
            0x00040027 to "updateGameName",
            0x00040028 to "ejectHost",
            0x00040050 to "*notifyGameUpdated",
            0x00040064 to "getGameListSnapshot",
            0x00040065 to "getGameListSubscription",
            0x00040066 to "destroyGameList",
            0x00040067 to "getFullGameData",
            0x00040068 to "getMatchmakingConfig",
            0x00040069 to "getGameDataFromId",
            0x0004006A to "addAdminPlayer",
            0x0004006B to "removeAdminPlayer",
            0x0004006C to "setPlayerTeam",
            0x0004006D to "changeGameTeamId",
            0x0004006E to "migrateAdminPlayer",
            0x0004006F to "getUserSetGameListSubscription",
            0x00040070 to "swapPlayersTeam",
            0x00040096 to "registerDynamicDedicatedServerCreator",
            0x00040097 to "unregisterDynamicDedicatedServerCreator",
            // Redirector Component
            0x00050001 to "getServerInstance",
            // Stats Component
            0x00070001 to "getStatDescs",
            0x00070002 to "getStats",
            0x00070003 to "getStatGroupList",
            0x00070004 to "getStatGroup",
            0x00070005 to "getStatsByGroup",
            0x00070006 to "getDateRange",
            0x00070007 to "getEntityCount",
            0x0007000A to "getLeaderboardGroup",
            0x0007000B to "getLeaderboardFolderGroup",
            0x0007000C to "getLeaderboard",
            0x0007000D to "getCenteredLeaderboard",
            0x0007000E to "getFilteredLeaderboard",
            0x0007000F to "getKeyScopesMap",
            0x00070010 to "getStatsByGroupAsync",
            0x00070011 to "getLeaderboardTreeAsync",
            0x00070012 to "getLeaderboardEntityCount",
            0x00070013 to "getStatCategoryList",
            0x00070014 to "getPeriodIds",
            0x00070015 to "getLeaderboardRaw",
            0x00070016 to "getCenteredLeaderboardRaw",
            0x00070017 to "getFilteredLeaderboardRaw",
            0x00070018 to "changeKeyscopeValue",
            // Util Component
            0x00090001 to "fetchClientConfig",
            0x00090002 to "ping",
            0x00090003 to "setClientData",
            0x00090004 to "localizeStrings",
            0x00090005 to "getTelemetryServer",
            0x00090006 to "getTickerServer",
            0x00090007 to "preAuth",
            0x00090008 to "postAuth",
            0x0009000A to "userSettingsLoad",
            0x0009000B to "userSettingsSave",
            0x0009000C to "userSettingsLoadAll",
            0x0009000E to "deleteUserSettings",
            0x00090014 to "filterForProfanity",
            0x00090015 to "fetchQosConfig",
            0x00090016 to "setClientMetrics",
            0x00090017 to "setConnectionState",
            0x00090018 to "getPssConfig",
            0x00090019 to "getUserOptions",
            0x0009001A to "setUserOptions",
            0x0009001B to "suspendUserPing",
            // Messaging Component
            0x000F0001 to "sendMessage",
            0x000F0002 to "fetchMessages",
            0x000F0003 to "purgeMessages",
            0x000F0004 to "touchMessages",
            0x000F0005 to "getMessages",
            // Association Lists Component
            0x00190001 to "addUsersToList",
            0x00190002 to "removeUsersFromList",
            0x00190003 to "clearLists",
            0x00190004 to "setUsersToList",
            0x00190005 to "getListForUser",
            0x00190006 to "getLists",
            0x00190007 to "subscribeToLists",
            0x00190008 to "unsubscribeFromLists",
            0x00190009 to "getConfigListsInfo",
            // Game Reporting Component
            0x001C0001 to "submitGameReport",
            0x001C0002 to "submitOfflineGameReport",
            0x001C0003 to "submitGameEvents",
            0x001C0004 to "getGameReportQuery",
            0x001C0005 to "getGameReportQueriesList",
            0x001C0006 to "getGameReports",
            0x001C0007 to "getGameReportView",
            0x001C0008 to "getGameReportViewInfo",
            0x001C0009 to "getGameReportViewInfoList",
            0x001C000A to "getGameReportTypes",
            0x001C000B to "updateMetric",
            0x001C000C to "getGameReportColumnInfo",
            0x001C000D to "getGameReportColumnValues",
            0x001C0064 to "submitTrustedMidGameReport",
            0x001C0065 to "submitTrustedEndGameReport",
            // User Sessions Component
            0x78020003 to "fetchExtendedData",
            0x78020005 to "updateExtendedDataAttribute",
            0x78020008 to "updateHardwareFlags",
            0x7802000C to "lookupUser",
            0x7802000D to "lookupUsers",
            0x7802000E to "lookupUsersByPrefix",
            0x78020014 to "updateNetworkInfo",
            0x78020017 to "lookupUserGeoIPData",
            0x78020018 to "overrideUserGeoIPData",
            0x78020019 to "updateUserSessionClientData",
            0x7802001A to "setUserInfoAttribute",
            0x7802001B to "resetUserGeoIPData",
            0x78020020 to "lookupUserSessionId",
            0x78020021 to "fetchLastLocaleUsedAndAuthError",
            0x78020022 to "fetchUserFirstLastAuthTime",
            0x78020023 to "resumeSession"
        )
        val ComponentNames = mapOf(
            0x1 to "Authentication",
            0x3 to "Example",
            0x4 to "Game Manager",
            0x5 to "Redirector",
            0x6 to "Play Groups",
            0x7 to "Stats",
            0x9 to "Util",
            0xA to "Census Data",
            0xB to "Clubs",
            0xC to "Game Report Legacy",
            0xD to "League",
            0xE to "Mail",
            0xF to "Messaging",
            0x14 to "Locker",
            0x15 to "Rooms",
            0x17 to "Tournaments",
            0x18 to "Commerce Info",
            0x19 to "Association Lists",
            0x1B to "GPS Content Controller",
            0x1C to "Game Reporting",
            0x7D0 to "Dynamic Filter",
            0x801 to "RSP Component",
            0x7802 to "User Sessions"
        )
        val NotificationNames = mapOf(
            0x0004000A to "NotifyMatchmakingFailed",
            0x0004000C to "NotifyMatchmakingAsyncStatus",
            0x0004000F to "NotifyGameCreated",
            0x00040010 to "NotifyGameRemoved",
            0x00040014 to "NotifyGameSetup",
            0x00040015 to "NotifyPlayerJoining",
            0x00040016 to "NotifyJoiningPlayerInitiateConnections",
            0x00040017 to "NotifyPlayerJoiningQueue",
            0x00040018 to "NotifyPlayerPromotedFromQueue",
            0x00040019 to "NotifyPlayerClaimingReservation",
            0x0004001E to "NotifyPlayerJoinCompleted",
            0x00040028 to "NotifyPlayerRemoved",
            0x0004003C to "NotifyHostMigrationFinished",
            0x00040046 to "NotifyHostMigrationStart",
            0x00040047 to "NotifyPlatformHostInitialized",
            0x00040050 to "NotifyGameAttribChange",
            0x0004005A to "NotifyPlayerAttribChange",
            0x0004005F to "NotifyPlayerCustomDataChange",
            0x00040064 to "NotifyGameStateChange",
            0x0004006E to "NotifyGameSettingsChange",
            0x0004006F to "NotifyGameCapacityChange",
            0x00040070 to "NotifyGameReset",
            0x00040071 to "NotifyGameReportingIdChange",
            0x00040073 to "NotifyGameSessionUpdated",
            0x00040074 to "NotifyGamePlayerStateChange",
            0x00040075 to "NotifyGamePlayerTeamChange",
            0x00040076 to "NotifyGameTeamIdChange",
            0x00040077 to "NotifyProcessQueue",
            0x00040078 to "NotifyPresenceModeChanged",
            0x00040079 to "NotifyGamePlayerQueuePositionChange",
            0x000400C9 to "NotifyGameListUpdate",
            0x000400CA to "NotifyAdminListChange",
            0x000400DC to "NotifyCreateDynamicDedicatedServerGame",
            0x000400E6 to "NotifyGameNameChange"
        )
    }

    fun componentName(): String = ComponentNames.getOrElse(component) { "Unknown" }
    fun commandName(): String = CommandNames.getOrElse(component) { "Unknown" }

    override fun toString(): String {
        return "Packet (Length: $length, Component: ${componentName()} ($component), Command: ${commandName()} ($command), Error; $error, QType: $qtype, Id: $id, Content: ${content.contentToString()})"
    }

}