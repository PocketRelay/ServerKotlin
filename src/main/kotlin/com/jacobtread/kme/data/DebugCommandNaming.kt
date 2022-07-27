package com.jacobtread.kme.data

import com.jacobtread.blaze.debug.DebugNaming

class DebugCommandNaming : DebugNaming {
    override fun getCommandNames(): Map<Int, String> {
        return mapOf(
            0x10014 to "UPDATE_ACCOUNT",
            0x1001C to "UPDATE_PARENTAL_EMAIL",
            0x1001D to "LIST_USER_ENTITLEMENTS_2",
            0x1001E to "GET_ACCOUNT",
            0x1001F to "GRANT_ENTITLEMENT",
            0x10020 to "LIST_ENTITLEMENTS",
            0x10021 to "HAS_ENTITLEMENT",
            0x10022 to "GET_USE_COUNT",
            0x10023 to "DECREMENT_USE_COUNT",
            0x10024 to "GET_AUTH_TOKEN",
            0x10025 to "GET_HANDOFF_TOKEN",
            0x10026 to "GET_PASSWORD_RULES",
            0x10027 to "GRANT_ENTITLEMENT_2",
            0x10028 to "LOGIN",
            0x10029 to "ACCEPT_TOS",
            0x1002A to "GET_TOS_INFO",
            0x1002B to "MODIFY_ENTITLEMENT_2",
            0x1002C to "CONSUME_CODE",
            0x1002D to "PASSWORD_FORGOT",
            0x1002E to "GET_TOS_CONTENT",
            0x1002F to "GET_PRIVACY_POLICY_CONTENT",
            0x10030 to "LIST_PERSONA_ENTITLEMENTS_2",
            0x10032 to "SILENT_LOGIN",
            0x10033 to "CHECK_AGE_REQUIREMENT",
            0x10034 to "GET_OPT_IN",
            0x10035 to "ENABLE_OPT_IN",
            0x10036 to "DISABLE_OPT_IN",
            0x1003C to "EXPRESS_LOGIN",
            0x10046 to "LOGOUT",
            0x10050 to "CREATE_PERSONA",
            0x1005A to "GET_PERSONA",
            0x10064 to "LIST_PERSONAS",
            0x1006E to "LOGIN_PERSONA",
            0x10078 to "LOGOUT_PERSONA",
            0x1008C to "DELETE_PERSONA",
            0x1008D to "DISABLE_PERSONA",
            0x1008F to "LIST_DEVICE_ACCOUNTS",
            0x10096 to "XBOX_CREATE_ACCOUNT",
            0x10098 to "ORIGIN_LOGIN",
            0x100A0 to "XBOX_ASSOCIATE_ACCOUNT",
            0x100AA to "XBOX_LOGIN",
            0x100B4 to "PS3_CREATE_ACCOUNT",
            0x100BE to "PS3_ASSOCIATE_ACCOUNT",
            0x100C8 to "PS3_LOGIN",
            0x100D2 to "VALIDATE_SESSION_KEY",
            0x100E6 to "CREATE_WAL_USER_SESSION",
            0x100F1 to "ACCEPT_LEGAL_DOCS",
            0x100F2 to "GET_LEGAL_DOCS_INFO",
            0x100F6 to "GET_TERMS_OF_SERVICE_CONTENT",
            0x1012C to "DEVICE_LOGIN_GUEST",
            0x1000A to "CREATE_ACCOUNT",
            0x40001 to "CREATE_GAME",
            0x40002 to "DESTROY_GAME",
            0x40003 to "ADVANCE_GAME_STATE",
            0x40004 to "SET_GAME_SETTINGS",
            0x40005 to "SET_PLAYER_CAPACITY",
            0x40006 to "SET_PRESENCE_MODE",
            0x40007 to "SET_GAME_ATTRIBUTES",
            0x40008 to "SET_PLAYER_ATTRIBUTES",
            0x40009 to "JOIN_GAME",
            0x4000B to "REMOVE_PLAYER",
            0x4000D to "START_MATCHMAKING",
            0x4000E to "CANCEL_MATCHMAKING",
            0x4000F to "FINALIZE_GAME_CREATION",
            0x40011 to "LIST_GAMES",
            0x40012 to "SET_PLAYER_CUSTOM_DATA",
            0x40013 to "REPLAY_GAME",
            0x40014 to "RETURN_DEDICATED_SERVER_TO_POOL",
            0x40015 to "JOIN_GAME_BY_GROUP",
            0x40016 to "LEAVE_GAME_BY_GROUP",
            0x40017 to "MIGRATE_GAME",
            0x40018 to "UPDATE_GAME_HOST_MIGRATION_STATUS",
            0x40019 to "RESET_DEDICATED_SERVER",
            0x4001A to "UPDATE_GAME_SESSION",
            0x4001B to "BAN_PLAYER",
            0x4001D to "UPDATE_MESH_CONNECTION",
            0x4001F to "REMOVE_PLAYER_FROM_BANNED_LIST",
            0x40020 to "CLEAR_BANNED_LIST",
            0x40021 to "GET_BANNED_LIST",
            0x40026 to "ADD_QUEUED_PLAYER_TO_GAME",
            0x40027 to "UPDATE_GAME_NAME",
            0x40028 to "EJECT_HOST",
            0x40064 to "GET_GAME_LIST_SNAPSHOT",
            0x40065 to "GET_GAME_LIST_SUBSCRIPTION",
            0x40066 to "DESTROY_GAME_LIST",
            0x40067 to "GET_FULL_GAME_DATA",
            0x40068 to "GET_MATCH_MAKING_CONFIG",
            0x40069 to "GET_GAME_DATA_FROM_ID",
            0x4006A to "ADD_ADMIN_PLAYER",
            0x4006B to "REMOVE_ADMIN_PLAYER",
            0x4006C to "SET_PLAYER_TEAM",
            0x4006D to "CHANGE_GAME_TEAM_ID",
            0x4006E to "MIGRATE_ADMIN_PLAYER",
            0x4006F to "GET_USER_SET_GAME_LIST_SUBSCRIPTION",
            0x40070 to "SWAP_PLAYERS_TEAM",
            0x40096 to "REGISTER_DYNAMIC_DEDICATED_SERVER_CREATOR",
            0x40097 to "UNREGISTER_DYNAMIC_DEDICATED_SERVER_CREATOR",

            0x50001 to "GET_SERVER_INSTANCE",
            0x70001 to "GET_STAT_DESCS",
            0x70002 to "GET_STATS",
            0x70003 to "GET_STAT_GROUP_LIST",
            0x70004 to "GET_STAT_GROUP",
            0x70005 to "GET_STATS_BY_GROUP",
            0x70006 to "GET_DATE_RANGE",
            0x70007 to "GET_ENTITY_COUNT",
            0x7000A to "GET_LEADERBOARD_GROUP",
            0x7000B to "GET_LEADERBOARD_FOLDER_GROUP",
            0x7000C to "GET_LEADERBOARD",
            0x7000D to "GET_CENTERED_LEADERBOARD",
            0x7000E to "GET_FILTERED_LEADERBOARD",
            0x7000F to "GET_KEY_SCOPES_MAP",
            0x70010 to "GET_STATS_BY_GROUP_ASYNC",
            0x70011 to "GET_LEADERBOARD_TREE_ASYNC",
            0x70012 to "GET_LEADERBOARD_ENTITY_COUNT",
            0x70013 to "GET_STAT_CATEGORY_LIST",
            0x70014 to "GET_PERIOD_IDS",
            0x70015 to "GET_LEADERBOARD_RAW",
            0x70016 to "GET_CENTERED_LEADERBOARD_RAW",
            0x70017 to "GET_FILTERED_LEADERBOARD_RAW",
            0x70018 to "CHANGE_KEY_SCOPE_VALUE",
            0x90001 to "FETCH_CLIENT_CONFIG",
            0x90002 to "PING",
            0x90003 to "SET_CLIENT_DATA",
            0x90004 to "LOCALIZE_STRINGS",
            0x90005 to "GET_TELEMETRY_SERVER",
            0x90006 to "GET_TICKER_SERVER",
            0x90007 to "PRE_AUTH",
            0x90008 to "POST_AUTH",
            0x9000A to "USER_SETTINGS_LOAD",
            0x9000B to "USER_SETTINGS_SAVE",
            0x9000C to "USER_SETTINGS_LOAD_ALL",
            0x9000E to "DELETE_USER_SETTINGS",
            0x90014 to "FILTER_FOR_PROFANITY",
            0x90015 to "FETCH_QOS_CONFIG",
            0x90016 to "SET_CLIENT_METRICS",
            0x90017 to "SET_CONNECTION_STATE",
            0x90018 to "GET_PSS_CONFIG",
            0x90019 to "GET_USER_OPTIONS",
            0x9001A to "SET_USER_OPTIONS",
            0x9001B to "SUSPEND_USER_PING",
            0xF0002 to "FETCH_MESSAGES",
            0xF0003 to "PURGE_MESSAGES",
            0xF0004 to "TOUCH_MESSAGES",
            0xF0005 to "GET_MESSAGES",
            0x190001 to "ADD_USERS_TO_LIST",
            0x190001 to "REMOVE_USERS_FROM_LIST",
            0x190003 to "CLEAR_LISTS",
            0x190004 to "SET_USERS_TO_LIST",
            0x190005 to "GET_LIST_FOR_USER",
            0x190006 to "GET_LISTS",
            0x190007 to "SUBSCRIBE_TO_LISTS",
            0x190008 to "UNSUBSCRIBE_FROM_LISTS",
            0x190009 to "GET_CONFIG_LISTS_INFO",
            0x1C0001 to "SUBMIT_GAME_REPORT",
            0x1C0002 to "SUBMIT_OFFLINE_GAME_REPORT",
            0x1C0003 to "SUBMIT_GAME_EVENTS",
            0x1C0004 to "GET_GAME_REPORT_QUERY",
            0x1C0005 to "GET_GAME_REPORT_QUERIES_LIST",
            0x1C0006 to "GET_GAME_REPORTS",
            0x1C0007 to "GET_GAME_REPORT_VIEW",
            0x1C0008 to "GET_GAME_REPORT_VIEW_INFO",
            0x1C0009 to "GET_GAME_REPORT_VIEW_INFO_LIST",
            0x1C000A to "GET_GAME_REPORT_TYPES",
            0x1C000B to "UPDATE_METRIC",
            0x1C000C to "GET_GAME_REPORT_COLUMN_INFO",
            0x1C000D to "GET_GAME_REPORT_COLUMN_VALUES",
            0x1C0064 to "SUBMIT_TRUSTED_MID_GAME_REPORT",
            0x1C0065 to "SUBMIT_TRUSTED_END_GAME_REPORT",
            0x78020008 to "UPDATE_HARDWARE_FLAGS",
            0x7802000C to "LOOKUP_USER",
            0x7802000D to "LOOKUP_USERS",
            0x7802000E to "LOOKUP_USERS_BY_PREFIX",
            0x78020014 to "UPDATE_NETWORK_INFO",
            0x78020017 to "LOOKUP_USER_GEO_IP_DATA",
            0x78020018 to "OVERRIDE_USER_GEO_IP_DATA",
            0x78020019 to "UPDATE_USER_SESSION_CLIENT_DATA",
            0x7802001A to "SET_USER_INFO_ATTRIBUTE",
            0x7802001B to "RESET_USER_GEO_IP_DATA",
            0x78020020 to "LOOKUP_USER_SESSION_ID",
            0x78020021 to "FETCH_LAST_LOCALE_USED_AND_AUTH_ERROR",
            0x78020022 to "FETCH_USER_FIRST_LAST_AUTH_TIME",
            0x78020023 to "RESUME_SESSION",
        )
    }

    override fun getComponentNames(): Map<Int, String> {
        return mapOf(
            Components.AUTHENTICATION to "AUTHENTICATION",
            Components.EXAMPLE to "EXAMPLE",
            Components.GAME_MANAGER to "GAME_MANAGER",
            Components.REDIRECTOR to "REDIRECTOR",
            Components.PLAY_GROUPS to "PLAY_GROUPS",
            Components.STATS to "STATS",
            Components.UTIL to "UTIL",
            Components.CENSUS_DATA to "CENSUS_DATA",
            Components.CLUBS to "CLUBS",
            Components.GAME_REPORT_LEGACY to "GAME_REPORT_LEGACY",
            Components.LEAGUE to "LEAGUE",
            Components.MAIL to "MAIL",
            Components.MESSAGING to "MESSAGING",
            Components.LOCKER to "LOCKER",
            Components.ROOMS to "ROOMS",
            Components.TOURNAMENTS to "TOURNAMENTS",
            Components.COMMERCE_INFO to "COMMERCE_INFO",
            Components.ASSOCIATION_LISTS to "ASSOCIATION_LISTS",
            Components.GPS_CONTENT_CONTROLLER to "GPS_CONTENT_CONTROLLER",
            Components.GAME_REPORTING to "GAME_REPORTING",
            Components.DYNAMIC_FILTER to "DYNAMIC_FILTER",
            Components.RSP to "RSP",
            Components.USER_SESSIONS to "USER_SESSIONS",
        )
    }

    override fun getNotifyNames(): Map<Int, String> {
        return mapOf(
            0x4001E to "NOTIFY_PLAYER_JOIN_COMPLETED",
            0x40050 to "NOTIFY_GAME_UPDATED",
            0x40074 to "NOTIFY_GAME_PLAYER_STATE_CHANGE",
            0x400CA to "NOTIFY_ADMIN_LIST_CHANGE",
            0x4000A to "NOTIFY_MATCHMAKING_FAILED",
            0x4000C to "NOTIFY_MATCHMAKING_ASYNC_STATUS",
            0x4000F to "NOTIFY_GAME_CREATED",
            0x40010 to "NOTIFY_GAME_REMOVED",
            0x40014 to "NOTIFY_GAME_SETUP",
            0x40015 to "NOTIFY_PLAYER_JOINING",
            0x40016 to "NOTIFY_JOINING_PLAYER_INITIATE_CONNECTIONS",
            0x40017 to "NOTIFY_PLAYER_JOINING_QUEUE",
            0x40018 to "NOTIFY_PLAYER_PROMOTED_FROM_QUEUE",
            0x40019 to "NOTIFY_PLAYER_CLAIMING_RESEERVATION",
            0x4001E to "NOTIFY_PLAYER_JOIN_COMPLETED",
            0x40028 to "NOTIFY_PLAYER_REMOVED",
            0x4003C to "NOTIFY_HOST_MIGRATION_FINISHED",
            0x40046 to "NOTIFY_HOST_MIGRATION_START",
            0x40047 to "NOTIFY_PLATFORM_HOST_INITIALIZED",
            0x40050 to "NOTIFY_GAME_ATTRIB_CHANGE",
            0x4005A to "NOTIFY_PLAYER_ATTRIB_CHANGE",
            0x4005F to "NOTIFY_PLAYER_CUSTOM_DATA_CHANGE",
            0x40064 to "NOTIFY_GAME_STATE_CHANGE",
            0x4006E to "NOTIFY_GAME_SETTINGS_CHANGE",
            0x4006F to "NOTIFY_GAME_CAPACITY_CHANGE",
            0x40070 to "NOTIFY_GAME_RESET",
            0x40071 to "NOTIFY_GAME_REPORTING_ID_CHANGE",
            0x40073 to "NOTIFY_GAME_SESSION_UPDATED",
            0x40074 to "NOTIFY_GAME_PLAYER_STATE_CHANGE",
            0x40075 to "NOTIFY_GAME_PLAYER_TEAM_CHANGE",
            0x40076 to "NOTIFY_GAME_TEAM_ID_CHANGE",
            0x40077 to "NOTIFY_PROCESS_QUEUE",
            0x40078 to "NOTIFY_PRESENCE_MODE_CHANGED",
            0x40079 to "NOTIFY_GAME_PLAYER_QUEUE_POSITION_CHANGE",
            0x400C9 to "NOTIFY_GAME_LIST_UPDATE",
            0x400CA to "NOTIFY_ADMIN_LIST_CHANGE",
            0x400DC to "NOTIFY_CREATE_DYNAMIC_DEDICATED_SERVER_GAME",
            0x400E6 to "NOTIFY_GAME_NAME_CHANGE",
            0xF0001 to "SEND_MESSAGE",
            0x1C0072 to "GAME_REPORT_RESULT_72",
            0x78020001 to "SET_SESSION",
            0x78020002 to "SESSION_DETAILS",
            0x78020003 to "FETCH_EXTENDED_DATA",
            0x78020005 to "UPDATE_EXTENDED_DATA_ATTRIBUTE",
        )
    }


}