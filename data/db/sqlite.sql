CREATE TABLE `players`
(
    `id`              INTEGER
        CONSTRAINT players_pk
            PRIMARY KEY AUTOINCREMENT,
    `display_name`    TEXT NOT NULL,
    `session_token`   TEXT    DEFAULT NULL,
    `password`        TEXT,
    `credits`         INTEGER DEFAULT 0,
    `credits_sepnt`   INTEGER DEFAULT 0,
    `games_played`    INTEGER DEFAULT 0,
    `seconds_played`  INTEGER DEFAULT 0,
    `inventory`       TEXT,
    `csreward`        INTEGER DEFAULT 0,
    `face_codes`      TEXT    DEFAULT '20;',
    `new_item`        TEXT    DEFAULT '20;4;',
    `completion`      TEXT    DEFAULT NULL,
    `progress`        TEXT    DEFAULT NULL,
    `cs_completion`   TEXT    DEFAULT NULL,
    `cs_timestamps_1` TEXT    DEFAULT NULL,
    `cs_timestamps_2` TEXT    DEFAULT NULL,
    `cs_timestamps_3` TEXT    DEFAULT NULL
);

create table `player_classes`
(
    `id`         INTEGER
        CONSTRAINT player_classes_pk
            PRIMARY KEY AUTOINCREMENT,
    `player_id`  INTEGER
        constraint player_classes_players_id_fk
            references players(`id`) NOT NULL,
    `index`      INTEGER       NOT NULL,
    `name`       text          NOT NULL,
    `level`      INTEGER       NOT NULL,
    `exp`        REAL          NOT NULL,
    `promotions` INTEGER       NOT NULL
);






