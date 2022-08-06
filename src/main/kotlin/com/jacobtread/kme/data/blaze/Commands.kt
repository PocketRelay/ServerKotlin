package com.jacobtread.kme.data.blaze

@Suppress("unused")
object Commands {
    //region AUTHENTICATION

    const val UPDATE_ACCOUNT = 0x14
    const val UPDATE_PARENTAL_EMAIL = 0x1C
    const val LIST_USER_ENTITLEMENTS_2 = 0x1D //FC
    const val GET_ACCOUNT = 0x1E
    const val GRANT_ENTITLEMENT = 0x1F
    const val LIST_ENTITLEMENTS = 0x20
    const val HAS_ENTITLEMENT = 0x21
    const val GET_USE_COUNT = 0x22
    const val DECREMENT_USE_COUNT = 0x23
    const val GET_AUTH_TOKEN = 0x24 // FC
    const val GET_HANDOFF_TOKEN = 0x25
    const val GET_PASSWORD_RULES = 0x26
    const val GRANT_ENTITLEMENT_2 = 0x27
    const val LOGIN = 0x28 // FC
    const val ACCEPT_TOS = 0x29
    const val GET_TOS_INFO = 0x2A
    const val MODIFY_ENTITLEMENT_2 = 0x2B
    const val CONSUME_CODE = 0x2C
    const val PASSWORD_FORGOT = 0x2D // FC
    const val GET_TOS_CONTENT = 0x2E
    const val GET_PRIVACY_POLICY_CONTENT = 0x2F // FC
    const val LIST_PERSONA_ENTITLEMENTS_2 = 0x30
    const val SILENT_LOGIN = 0x32 // FC
    const val CHECK_AGE_REQUIREMENT = 0x33
    const val GET_OPT_IN = 0x34
    const val ENABLE_OPT_IN = 0x35
    const val DISABLE_OPT_IN = 0x36
    const val EXPRESS_LOGIN = 0x3C
    const val LOGOUT = 0x46 // FC
    const val CREATE_PERSONA = 0x50
    const val GET_PERSONA = 0x5A
    const val LIST_PERSONAS = 0x64
    const val LOGIN_PERSONA = 0x6E // FC
    const val LOGOUT_PERSONA = 0x78
    const val DELETE_PERSONA = 0x8C
    const val DISABLE_PERSONA = 0x8D
    const val LIST_DEVICE_ACCOUNTS = 0x8F
    const val XBOX_CREATE_ACCOUNT = 0x96
    const val ORIGIN_LOGIN = 0x98 // FC
    const val XBOX_ASSOCIATE_ACCOUNT = 0xA0
    const val XBOX_LOGIN = 0xAA
    const val PS3_CREATE_ACCOUNT = 0xB4
    const val PS3_ASSOCIATE_ACCOUNT = 0xBE
    const val PS3_LOGIN = 0xC8
    const val VALIDATE_SESSION_KEY = 0xD2
    const val CREATE_WAL_USER_SESSION = 0xE6
    const val ACCEPT_LEGAL_DOCS = 0xF1 // FC
    const val GET_LEGAL_DOCS_INFO = 0xF2 // FC
    const val GET_TERMS_OF_SERVICE_CONTENT = 0xF6 // FC
    const val DEVICE_LOGIN_GUEST = 0x12C
    const val CREATE_ACCOUNT = 0xA // FC

    //endregion

    //region GAME_MANAGER

    const val CREATE_GAME = 0x1 // FC
    const val DESTROY_GAME = 0x2
    const val ADVANCE_GAME_STATE = 0x3 // FC
    const val SET_GAME_SETTINGS = 0x4 // FC
    const val SET_PLAYER_CAPACITY = 0x5
    const val SET_PRESENCE_MODE = 0x6
    const val SET_GAME_ATTRIBUTES = 0x7
    const val SET_PLAYER_ATTRIBUTES = 0x8
    const val JOIN_GAME = 0x9
    const val REMOVE_PLAYER = 0xB // FC
    const val START_MATCHMAKING = 0xD // FC
    const val CANCEL_MATCHMAKING = 0xE // FC
    const val FINALIZE_GAME_CREATION = 0xF // FC
    const val LIST_GAMES = 0x11
    const val SET_PLAYER_CUSTOM_DATA = 0x12
    const val REPLAY_GAME = 0x13
    const val RETURN_DEDICATED_SERVER_TO_POOL = 0x14
    const val JOIN_GAME_BY_GROUP = 0x15
    const val LEAVE_GAME_BY_GROUP = 0x16
    const val MIGRATE_GAME = 0x17
    const val UPDATE_GAME_HOST_MIGRATION_STATUS = 0x18
    const val RESET_DEDICATED_SERVER = 0x19
    const val UPDATE_GAME_SESSION = 0x1A
    const val BAN_PLAYER = 0x1B
    const val UPDATE_MESH_CONNECTION = 0x1D // FC
    const val REMOVE_PLAYER_FROM_BANNED_LIST = 0x1F
    const val CLEAR_BANNED_LIST = 0x20
    const val GET_BANNED_LIST = 0x21
    const val ADD_QUEUED_PLAYER_TO_GAME = 0x26
    const val UPDATE_GAME_NAME = 0x27
    const val EJECT_HOST = 0x28
    const val NOTIFY_GAME_UPDATED = 0x50 // US
    const val GET_GAME_LIST_SNAPSHOT = 0x64
    const val GET_GAME_LIST_SUBSCRIPTION = 0x65
    const val DESTROY_GAME_LIST = 0x66
    const val GET_FULL_GAME_DATA = 0x67
    const val GET_MATCH_MAKING_CONFIG = 0x68
    const val GET_GAME_DATA_FROM_ID = 0x69
    const val ADD_ADMIN_PLAYER = 0x6A // FC
    const val REMOVE_ADMIN_PLAYER = 0x6B
    const val SET_PLAYER_TEAM = 0x6C
    const val CHANGE_GAME_TEAM_ID = 0x6D
    const val MIGRATE_ADMIN_PLAYER = 0x6E
    const val GET_USER_SET_GAME_LIST_SUBSCRIPTION = 0x6F
    const val SWAP_PLAYERS_TEAM = 0x70
    const val REGISTER_DYNAMIC_DEDICATED_SERVER_CREATOR = 0x96
    const val UNREGISTER_DYNAMIC_DEDICATED_SERVER_CREATOR = 0x97

    const val NOTIFY_MATCHMAKING_FAILED = 0xA // US
    const val NOTIFY_MATCHMAKING_ASYNC_STATUS = 0xC // US
    const val NOTIFY_GAME_CREATED = 0xF // FC
    const val NOTIFY_GAME_REMOVED = 0x10
    const val NOTIFY_GAME_SETUP = 0x14 // US
    const val NOTIFY_PLAYER_JOINING = 0x15 // US
    const val NOTIFY_JOINING_PLAYER_INITIATE_CONNECTIONS = 0x16
    const val NOTIFY_PLAYER_JOINING_QUEUE = 0x17
    const val NOTIFY_PLAYER_PROMOTED_FROM_QUEUE = 0x18 // FC
    const val NOTIFY_PLAYER_CLAIMING_RESEERVATION = 0x19
    const val NOTIFY_PLAYER_JOIN_COMPLETED = 0x1E // US
    const val NOTIFY_PLAYER_REMOVED = 0x28 // US
    const val NOTIFY_HOST_MIGRATION_FINISHED = 0x3C // US
    const val NOTIFY_HOST_MIGRATION_START = 0x46// US
    const val NOTIFY_PLATFORM_HOST_INITIALIZED = 0x47
    const val NOTIFY_GAME_ATTRIB_CHANGE = 0x50 // US
    const val NOTIFY_PLAYER_ATTRIB_CHANGE = 0x5A
    const val NOTIFY_PLAYER_CUSTOM_DATA_CHANGE = 0x5F
    const val NOTIFY_GAME_STATE_CHANGE = 0x64 // US
    const val NOTIFY_GAME_SETTINGS_CHANGE = 0x6E // US
    const val NOTIFY_GAME_CAPACITY_CHANGE = 0x6F // NE
    const val NOTIFY_GAME_RESET = 0x70
    const val NOTIFY_GAME_REPORTING_ID_CHANGE = 0x71
    const val NOTIFY_GAME_SESSION_UPDATED = 0x73
    const val NOTIFY_GAME_PLAYER_STATE_CHANGE = 0x74 // US
    const val NOTIFY_GAME_PLAYER_TEAM_CHANGE = 0x75 // NE
    const val NOTIFY_GAME_TEAM_ID_CHANGE = 0x76 // NE
    const val NOTIFY_PROCESS_QUEUE = 0x77
    const val NOTIFY_PRESENCE_MODE_CHANGED = 0x78
    const val NOTIFY_GAME_PLAYER_QUEUE_POSITION_CHANGE = 0x79
    const val NOTIFY_GAME_LIST_UPDATE = 0xC9
    const val NOTIFY_ADMIN_LIST_CHANGE = 0xCA // US
    const val NOTIFY_CREATE_DYNAMIC_DEDICATED_SERVER_GAME = 0xDC
    const val NOTIFY_GAME_NAME_CHANGE = 0xE6 // NE

    //endregion

    //region REDIRECTOR

    const val GET_SERVER_INSTANCE = 0x1 // FC

    //endregion

    //region STATS

    const val GET_STAT_DESCS = 0x1
    const val GET_STATS = 0x2
    const val GET_STAT_GROUP_LIST = 0x3
    const val GET_STAT_GROUP = 0x4
    const val GET_STATS_BY_GROUP = 0x5
    const val GET_DATE_RANGE = 0x6
    const val GET_ENTITY_COUNT = 0x7
    const val GET_LEADERBOARD_GROUP = 0xA // FC
    const val GET_LEADERBOARD_FOLDER_GROUP = 0xB
    const val GET_LEADERBOARD = 0xC // FC
    const val GET_CENTERED_LEADERBOARD = 0xD // FC
    const val GET_FILTERED_LEADERBOARD = 0xE // FC
    const val GET_KEY_SCOPES_MAP = 0xF
    const val GET_STATS_BY_GROUP_ASYNC = 0x10
    const val GET_LEADERBOARD_TREE_ASYNC = 0x11
    const val GET_LEADERBOARD_ENTITY_COUNT = 0x12
    const val GET_STAT_CATEGORY_LIST = 0x13
    const val GET_PERIOD_IDS = 0x14
    const val GET_LEADERBOARD_RAW = 0x15
    const val GET_CENTERED_LEADERBOARD_RAW = 0x16
    const val GET_FILTERED_LEADERBOARD_RAW = 0x17
    const val CHANGE_KEY_SCOPE_VALUE = 0x18

    //endregion

    //region UTIL

    const val FETCH_CLIENT_CONFIG = 0x1 // FC
    const val PING = 0x2 // FC
    const val SET_CLIENT_DATA = 0x3
    const val LOCALIZE_STRINGS = 0x4
    const val GET_TELEMETRY_SERVER = 0x5
    const val GET_TICKER_SERVER = 0x6
    const val PRE_AUTH = 0x7 // FC
    const val POST_AUTH = 0x8 // FC
    const val USER_SETTINGS_LOAD = 0xA // FC
    const val USER_SETTINGS_SAVE = 0xB // FC
    const val USER_SETTINGS_LOAD_ALL = 0xC // FC
    const val DELETE_USER_SETTINGS = 0xE
    const val FILTER_FOR_PROFANITY = 0x14
    const val FETCH_QOS_CONFIG = 0x15
    const val SET_CLIENT_METRICS = 0x16 // FC
    const val SET_CONNECTION_STATE = 0x17
    const val GET_PSS_CONFIG = 0x18
    const val GET_USER_OPTIONS = 0x19
    const val SET_USER_OPTIONS = 0x1A
    const val SUSPEND_USER_PING = 0x1B // FC

    //endregion

    //region MESSAGING

    const val SEND_MESSAGE = 0x1 // US
    const val FETCH_MESSAGES = 0x2 // FC
    const val PURGE_MESSAGES = 0x3
    const val TOUCH_MESSAGES = 0x4
    const val GET_MESSAGES = 0x5

    //endregion

    //region ASSOCIATION_LISTS

    const val ADD_USERS_TO_LIST = 0x1
    const val REMOVE_USERS_FROM_LIST = 0x1
    const val CLEAR_LISTS = 0x3
    const val SET_USERS_TO_LIST = 0x4 // FC
    const val GET_LIST_FOR_USER = 0x5
    const val GET_LISTS = 0x6 // FC
    const val SUBSCRIBE_TO_LISTS = 0x7
    const val UNSUBSCRIBE_FROM_LISTS = 0x8
    const val GET_CONFIG_LISTS_INFO = 0x9

    //endregion

    //region GAME_REPORTING

    const val SUBMIT_GAME_REPORT = 0x1
    const val SUBMIT_OFFLINE_GAME_REPORT = 0x2 // FC
    const val SUBMIT_GAME_EVENTS = 0x3
    const val GET_GAME_REPORT_QUERY = 0x4
    const val GET_GAME_REPORT_QUERIES_LIST = 0x5
    const val GET_GAME_REPORTS = 0x6
    const val GET_GAME_REPORT_VIEW = 0x7
    const val GET_GAME_REPORT_VIEW_INFO = 0x8
    const val GET_GAME_REPORT_VIEW_INFO_LIST = 0x9
    const val GET_GAME_REPORT_TYPES = 0xA
    const val UPDATE_METRIC = 0xB
    const val GET_GAME_REPORT_COLUMN_INFO = 0xC
    const val GET_GAME_REPORT_COLUMN_VALUES = 0xD
    const val SUBMIT_TRUSTED_MID_GAME_REPORT = 0x64
    const val SUBMIT_TRUSTED_END_GAME_REPORT = 0x65
    const val NOTIFY_GAME_REPORT_SUBMITTED = 0x72 // US

    //endregion

    //region USER_SESSIONS

    const val SET_SESSION = 0x1 // US
    const val SESSION_DETAILS = 0x2 // US
    const val FETCH_EXTENDED_DATA = 0x3 // US
    const val UPDATE_EXTENDED_DATA_ATTRIBUTE = 0x5 // US
    const val UPDATE_HARDWARE_FLAGS = 0x8 // FC
    const val LOOKUP_USER = 0xC
    const val LOOKUP_USERS = 0xD
    const val LOOKUP_USERS_BY_PREFIX = 0xE
    const val UPDATE_NETWORK_INFO = 0x14 // FC
    const val LOOKUP_USER_GEO_IP_DATA = 0x17
    const val OVERRIDE_USER_GEO_IP_DATA = 0x18
    const val UPDATE_USER_SESSION_CLIENT_DATA = 0x19
    const val SET_USER_INFO_ATTRIBUTE = 0x1A
    const val RESET_USER_GEO_IP_DATA = 0x1B
    const val LOOKUP_USER_SESSION_ID = 0x20
    const val FETCH_LAST_LOCALE_USED_AND_AUTH_ERROR = 0x21
    const val FETCH_USER_FIRST_LAST_AUTH_TIME = 0x22
    const val RESUME_SESSION = 0x23 // FC

    //endregion
}
