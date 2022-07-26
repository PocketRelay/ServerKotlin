-- Players Table
CREATE TABLE IF NOT EXISTS `players`
(
    `id`              INTEGER                      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `email`           VARCHAR(254)                 NOT NULL,
    `display_name`    VARCHAR(99)                  NOT NULL,
    `session_token`   VARCHAR(128) DEFAULT NULL,
    `password`        VARCHAR(128)                 NOT NULL,
    `credits`         INTEGER      DEFAULT 0       NOT NULL,
    `credits_spent`   INTEGER      DEFAULT 0       NOT NULL,
    `games_played`    INTEGER      DEFAULT 0       NOT NULL,
    `seconds_played`  BIGINT       DEFAULT 0       NOT NULL,
    `inventory`       TEXT         DEFAULT ''      NOT NULL,
    `csreward`        INTEGER      DEFAULT 0       NOT NULL,
    `face_codes`      TEXT         DEFAULT '20;'   NOT NULL,
    `new_item`        TEXT         DEFAULT '20;4;' NOT NULL,
    `completion`      TEXT         DEFAULT NULL,
    `progress`        TEXT         DEFAULT NULL,
    `cs_completion`   TEXT         DEFAULT NULL,
    `cs_timestamps_1` TEXT         DEFAULT NULL,
    `cs_timestamps_2` TEXT         DEFAULT NULL,
    `cs_timestamps_3` TEXT         DEFAULT NULL
);

-- Player Classes Table
CREATE TABLE IF NOT EXISTS `player_classes`
(
    `id`         INTEGER AUTO_INCREMENT PRIMARY KEY,
    `player_id`  INTEGER NOT NULL,
    `index`      INTEGER NOT NULL,
    `name`       TEXT    NOT NULL,
    `level`      INTEGER NOT NULL,
    `exp`        REAL    NOT NULL,
    `promotions` INTEGER NOT NULL,

    FOREIGN KEY (`player_id`) REFERENCES `players` (`id`)
);

-- Player Characters Table
CREATE TABLE IF NOT EXISTS `player_characters`
(
    `id`                INTEGER NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `player_id`         INTEGER NOT NULL,
    `index`             INTEGER NOT NULL,
    `kit_name`          TEXT    NOT NULL,
    `name`              TEXT    NOT NULL,
    `tint1`             INTEGER NOT NULL,
    `tint2`             INTEGER NOT NULL,
    `pattern`           INTEGER NOT NULL,
    `pattern_color`     INTEGER NOT NULL,
    `phong`             INTEGER NOT NULL,
    `emissive`          INTEGER NOT NULL,
    `skin_tone`         INTEGER NOT NULL,
    `seconds_played`    INTEGER NOT NULL,

    `timestamp_year`    INTEGER NOT NULL,
    `timestamp_month`   INTEGER NOT NULL,
    `timestamp_day`     INTEGER NOT NULL,
    `timestamp_seconds` INTEGER NOT NULL,

    `powers`            TEXT    NOT NULL,
    `hotkeys`           TEXT    NOT NULL,
    `weapons`           TEXT    NOT NULL,
    `weapon_mods`       TEXT    NOT NULL,

    `deployed`          BOOLEAN NOT NULL,
    `leveled_up`        BOOLEAN NOT NULL,

    FOREIGN KEY (`player_id`) REFERENCES `players` (`id`)
);

-- Galaxy At War Table
CREATE TABLE IF NOT EXISTS `player_gaw`
(
    `id`            INTEGER NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `player_id`     INTEGER NOT NULL,
    `last_modified` BIGINT  NOT NULL,
    `group_a`       INTEGER NOT NULL,
    `group_b`       INTEGER NOT NULL,
    `group_c`       INTEGER NOT NULL,
    `group_d`       INTEGER NOT NULL,
    `group_e`       INTEGER NOT NULL,

    FOREIGN KEY (`player_id`) REFERENCES `players` (`id`)
);

