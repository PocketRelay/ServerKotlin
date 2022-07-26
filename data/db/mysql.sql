create table players
(
    id              int                          not null primary key autoincrement ,
    display_name    varchar(99)                  not null,
    session_token   varchar(128) default NULL    null,
    password        varchar(128)                 not null,
    credits         int          default 0       not null,
    credits_spent   int          default 0       not null,
    games_played    int          default 0       not null,
    seconds_played  long         default 0       not null,
    inventory       text         default ''      not null,
    csreward        int          default 0       not null,
    face_codes      text         default '20;'   not null,
    new_item        text         default '20;4;' not null,
    completion      text         default NULL    null,
    progress        text         default NULL    null,
    cs_completion   text         default NULL    null,
    cs_timestamps_1 text         default NULL    null,
    cs_timestamps_2 text         default NULL    null,
    cs_timestamps_3 text         default NULL    null
);

