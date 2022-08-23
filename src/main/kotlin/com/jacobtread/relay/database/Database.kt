package com.jacobtread.relay.database

import com.jacobtread.relay.exceptions.DatabaseException
import com.jacobtread.relay.utils.Future
import com.jacobtread.relay.utils.VoidFuture
import com.jacobtread.relay.utils.logging.Logger
import org.intellij.lang.annotations.Language
import java.sql.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.regex.Pattern

object Database {

    internal lateinit var connection: Connection
    private var executor = Executors.newSingleThreadExecutor()

    /**
     * Initializes the database setting the [connection] and
     * creating the tables on the database.
     *
     * @param connection The database connection
     * @param sqlite whether to transform the creation queries for SQLite
     */
    internal fun init(connection: Connection, sqlite: Boolean) {
        Database.connection = connection
        try {
            val statement = connection.createStatement()
            statement.use {
                /**
                 * SQLite can execute multiple create table querys in a single
                 * update, so they are combined and then transformed for SQLite
                 * using [transformTableSQLite]
                 */
                if (sqlite) {
                    val queryBuilder = StringBuilder()
                    queryBuilder.appendLine(playersTable())
                    queryBuilder.appendLine(playerClassesTable())
                    queryBuilder.appendLine(playerCharactersTable())
                    queryBuilder.appendLine(playerGAWTable())
                    val query = transformTableSQLite(queryBuilder.toString())
                    statement.executeUpdate(query)
                } else {
                    statement.executeUpdate(playersTable())
                    statement.executeUpdate(playerClassesTable())
                    statement.executeUpdate(playerCharactersTable())
                    statement.executeUpdate(playerGAWTable())
                }
            }
        } catch (e: SQLException) {
            Logger.fatal("Failed to initialize database", e)
        }
    }

    private inline fun executeCatching(future: Future<*>, crossinline action: () -> Unit) {
        executor.execute {
            try {
                action()
            } catch (e: SQLException) {
                future.completeExceptionally(e)
            } catch (e: DatabaseException) {
                future.completeExceptionally(e)
            }
        }
    }

    fun executeQuery(
        @Language("MySQL") query: String,
        setup: (PreparedStatement.() -> Unit)? = null,
    ): Future<ResultSet> {
        val future = Future<ResultSet>()
        executeCatching(future) {
            val statement = connection.prepareStatement(query)
            statement.use {
                if (setup != null) {
                    setup(statement)
                }
                val resultSet = statement.executeQuery()
                future.complete(resultSet)
            }
        }
        return future
    }

    fun executeExists(
        @Language("MySQL") query: String,
        setup: (PreparedStatement.() -> Unit)? = null,
    ): Future<Boolean> {
        val future = Future<Boolean>()
        executeCatching(future) {
            val statement = connection.prepareStatement(query)
            statement.use {
                if (setup != null) {
                    setup(statement)
                }
                val resultSet = statement.executeQuery()
                future.complete(resultSet.next())
            }
        }
        return future
    }

    fun executeUpdate(
        @Language("MySQL") query: String,
        setup: PreparedStatement.() -> Unit,
    ): Future<PreparedStatement> {
        val future = Future<PreparedStatement>()
        executeCatching(future) {
            val statement = connection.prepareStatement(query)
            statement.use {
                setup(statement)
                statement.executeUpdate()
                future.complete(statement)
            }
        }
        return future
    }

    fun executeUpdateWithKeys(
        @Language("MySQL") query: String,
        setup: PreparedStatement.() -> Unit,
    ): Future<ResultSet> {
        val future = Future<ResultSet>()
        executeCatching(future) {
            val statement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)
            statement.use {
                setup(statement)
                statement.executeUpdate()
                future.complete(statement.generatedKeys)
            }
        }
        return future
    }

    fun executeUpdateVoid(
        @Language("MySQL") query: String,
        setup: PreparedStatement.() -> Unit,
    ): VoidFuture {
        val future = VoidFuture()
        executeCatching(future) {
            val statement = connection.prepareStatement(query)
            statement.use {
                setup(statement)
                statement.executeUpdate()
                future.complete(null)
            }
        }
        return future
    }

    @Language("MySQL")
    private fun playersTable(): String = """
        -- Players Table
        CREATE TABLE IF NOT EXISTS `players`
        (
            `id`              INT(255)               NOT NULL PRIMARY KEY AUTO_INCREMENT,
            `email`           VARCHAR(254)           NOT NULL,
            `display_name`    VARCHAR(99)            NOT NULL,
            `session_token`   VARCHAR(254) DEFAULT NULL,
            `origin`          BOOLEAN                NOT NULL,
            `password`        VARCHAR(128)           NOT NULL,

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
            `cs_timestamps_3` TEXT         DEFAULT NULL
        );
    """.trimIndent()

    @Language("MySQL")
    private fun playerClassesTable(): String = """
        -- Player Classes Table
        CREATE TABLE IF NOT EXISTS `player_classes`
        (
            `id`         INT(255) NOT NULL PRIMARY KEY AUTO_INCREMENT,
            `player_id`  INT(255) NOT NULL,
            `index`      INT(2)   NOT NULL,
            `name`       TEXT     NOT NULL,
            `level`      INT(3)   NOT NULL,
            `exp`        FLOAT(4) NOT NULL,
            `promotions` INT(255) NOT NULL,
        
            FOREIGN KEY (`player_id`) REFERENCES `players` (`id`)
        );
    """.trimIndent()

    @Language("MySQL")
    private fun playerCharactersTable(): String = """
         -- Player Characters Table
        CREATE TABLE IF NOT EXISTS `player_characters`
        (
            `id`                INT(255)    NOT NULL PRIMARY KEY AUTO_INCREMENT,
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
        
            FOREIGN KEY (`player_id`) REFERENCES `players` (`id`)
        );
    """.trimIndent()

    @Language("MySQL")
    private fun playerGAWTable(): String = """
        -- Galaxy At War Table
        CREATE TABLE IF NOT EXISTS `player_gaw`
        (
            `id`            INT(255)    NOT NULL PRIMARY KEY AUTO_INCREMENT,
            `player_id`     INT(255)    NOT NULL,
            `last_modified` BIGINT(255) NOT NULL,
            `group_a`       INT(8)      NOT NULL,
            `group_b`       INT(8)      NOT NULL,
            `group_c`       INT(8)      NOT NULL,
            `group_d`       INT(8)      NOT NULL,
            `group_e`       INT(8)      NOT NULL,

            FOREIGN KEY (`player_id`) REFERENCES `players` (`id`)
        );
    """.trimIndent()

    /**
     * Transform the specified SQL create table query for
     * use in SQLite which is as simple as removing all the
     * lengths from the data types and transforming them
     * to their SQLite form and removing the underscore
     * from AUTO_INCREMENT
     *
     *  - BIGING, INT, BOOLEAN -> INTEGER
     *  - FLOAT -> REAL
     *  - VARCHAR -> TEXT
     *
     * @param query The query to transform
     * @return The transformed query
     */
    private fun transformTableSQLite(query: String): String {
        val regex = Pattern.compile("(BIGINT|INT|BOOLEAN|VARCHAR|FLOAT)(\\(\\d+\\))?")
        val matcher = regex.matcher(query)

        var newQuery = matcher.replaceAll {
            when (val type = it.group(1)) {
                "BIGINT", "INT", "BOOLEAN" -> "INTEGER"
                "FLOAT" -> "REAL"
                "VARCHAR" -> "TEXT"
                else -> type
            }
        }

        newQuery = newQuery.replace("AUTO_INCREMENT", "AUTOINCREMENT")
        return newQuery
    }
}