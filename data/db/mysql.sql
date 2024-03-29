-- Players Table
CREATE TABLE IF NOT EXISTS `players`
(
    `id`              INT(255)               NOT NULL AUTO_INCREMENT,
    `email`           VARCHAR(254)           NOT NULL,
    `display_name`    VARCHAR(99)            NOT NULL,
    `session_token`   VARCHAR(254) DEFAULT NULL,
    `password`        VARCHAR(128)           NOT NULL,
    `origin`          BOOLEAN                NOT NULL,

    `credits`         INT(255)     DEFAULT 0 NOT NULL,
    `credits_spent`   INT(255)     DEFAULT 0 NOT NULL,
    `games_played`    INT(255)     DEFAULT 0 NOT NULL,
    `seconds_played`  BIGINT(255)  DEFAULT 0 NOT NULL,
    `inventory`       TEXT                   NOT NULL,
    `csreward`        INT(6)       DEFAULT 0 NOT NULL,
    `face_codes`      TEXT         DEFAULT NULL,
    `new_item`        TEXT         DEFAULT NULL,
    `completion`      TEXT         DEFAULT NULL,
    `progress`        TEXT         DEFAULT NULL,
    `cs_completion`   TEXT         DEFAULT NULL,
    `cs_timestamps_1` TEXT         DEFAULT NULL,
    `cs_timestamps_2` TEXT         DEFAULT NULL,
    `cs_timestamps_3` TEXT         DEFAULT NULL,

    PRIMARY KEY (`id`)
);

-- Player Classes Table
CREATE TABLE IF NOT EXISTS `player_classes`
(
    `id`         INT(255) NOT NULL AUTO_INCREMENT,
    `player_id`  INT(255) NOT NULL,
    `index`      INT(2)   NOT NULL,
    `name`       TEXT     NOT NULL,
    `level`      INT(3)   NOT NULL,
    `exp`        FLOAT(4) NOT NULL,
    `promotions` INT(255) NOT NULL,

    PRIMARY KEY (`id`),
    FOREIGN KEY (`player_id`) REFERENCES `players` (`id`)
);

-- Player Characters Table
CREATE TABLE IF NOT EXISTS `player_characters`
(
    `id`                INT(255)    NOT NULL AUTO_INCREMENT,
    `player_id`         INT(255)    NOT NULL,
    `index`             INT(3)      NOT NULL,
    `kit_name`          TEXT        NOT NULL,
    `name`              TEXT        NOT NULL,
    `tint1`             INT(4)      NOT NULL,
    `tint2`             INT(4)      NOT NULL,
    `pattern`           INT(4)      NOT NULL,
    `pattern_color`     INT(4)      NOT NULL,
    `phong`             INT(4)      NOT NULL,
    `emissive`          INT(4)      NOT NULL,
    `skin_tone`         INT(4)      NOT NULL,
    `seconds_played`    BIGINT(255) NOT NULL,

    `timestamp_year`    INT(255)    NOT NULL,
    `timestamp_month`   INT(255)    NOT NULL,
    `timestamp_day`     INT(255)    NOT NULL,
    `timestamp_seconds` INT(255)    NOT NULL,

    `powers`            TEXT        NOT NULL,
    `hotkeys`           TEXT        NOT NULL,
    `weapons`           TEXT        NOT NULL,
    `weapon_mods`       TEXT        NOT NULL,

    `deployed`          BOOLEAN     NOT NULL,
    `leveled_up`        BOOLEAN     NOT NULL,

    PRIMARY KEY (`id`),
    FOREIGN KEY (`player_id`) REFERENCES `players` (`id`)
);

-- SPLIT

-- Galaxy At War Table
CREATE TABLE IF NOT EXISTS `player_gaw`
(
    `id`            INT(255)    NOT NULL AUTO_INCREMENT,
    `player_id`     INT(255)    NOT NULL,
    `last_modified` BIGINT(255) NOT NULL,
    `group_a`       INT(8)      NOT NULL,
    `group_b`       INT(8)      NOT NULL,
    `group_c`       INT(8)      NOT NULL,
    `group_d`       INT(8)      NOT NULL,
    `group_e`       INT(8)      NOT NULL,

    PRIMARY KEY (`id`),
    FOREIGN KEY (`player_id`) REFERENCES `players` (`id`)
);