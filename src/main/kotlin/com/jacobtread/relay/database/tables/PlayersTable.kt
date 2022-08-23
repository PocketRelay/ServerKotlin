package com.jacobtread.relay.database.tables

import com.jacobtread.relay.data.retriever.OriginDetailsRetriever
import com.jacobtread.relay.database.Database
import com.jacobtread.relay.database.Table
import com.jacobtread.relay.database.asList
import com.jacobtread.relay.database.models.Player
import org.intellij.lang.annotations.Language
import java.sql.ResultSet
import java.util.concurrent.CompletableFuture as Future

object PlayersTable : Table {

    @Language("MySQL")
    override fun sql(): String = """
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


    fun isEmailTaken(email: String): Future<Boolean> {
        return Database
            .exists("SELECT `id` FROM `players` WHERE email = ? LIMIT 1") {
                setString(1, email)
            }
    }

    private fun ResultSet.asPlayer(): Player? {
        if (!next()) return null
        return Player(
            playerId = getInt("id"),
            email = getString("email"),
            displayName = getString("display_name"),
            password = getString("password"),
            sessionToken = getString("session_token"),
            credits = getInt("credits"),
            creditsSpent = getInt("credits_spent"),
            gamesPlayed = getInt("games_played"),
            secondsPlayed = getLong("seconds_played"),
            inventory = getString("inventory"),
            csReward = getInt("csreward"),
            faceCodes = getString("face_codes"),
            newItem = getString("new_item"),
            completion = getString("completion"),
            progress = getString("progress"),
            cscompletion = getString("cs_completion"),
            cstimestamps1 = getString("cs_timestamps_1"),
            cstimestamps2 = getString("cs_timestamps_2"),
            cstimestamps3 = getString("cs_timestamps_3"),
        )
    }

    fun getList(offset: Int, count: Int): Future<List<Player>> {
        return Database
            .query("SELECT * FROM `players` LIMIT $count OFFSET $offset")
            .thenApply { outer -> outer.asList { inner -> inner.asPlayer() } }
    }

    fun getByID(id: Int): Future<Player?> {
        return Database
            .query("SELECT * FROM `players` WHERE `id` = ? LIMIT 1") {
                setInt(1, id)
            }
            .thenApply { it.asPlayer() }
    }

    fun getByEmail(email: String): Future<Player?> {
        return Database
            .query("SELECT * FROM `players` WHERE `email` = ? LIMIT 1") {
                setString(1, email)
            }
            .thenApply { it.asPlayer() }
    }

    fun getBySessionToken(sessionToken: String): Future<Player?> {
        return Database
            .query("SELECT * FROM `players` WHERE `session_token` = ? AND `origin` = ? LIMIT 1") {
                setString(1, sessionToken)
                setBoolean(2, false)
            }
            .thenApply { it.asPlayer() }
    }

    fun setSessionToken(player: Player, sessionToken: String): Future<Void> {
        return Database
            .update("UPDATE `players` SET `session_token` = ? WHERE `id` = ?") {
                setString(1, sessionToken)
                setInt(2, player.playerId)
            }
    }

    fun setPlayerFully(player: Player): Future<Void> {
        return Database.update(
            """
            UPDATE `players` SET 
            `session_token` = ?, `credits` = ?, `credits_spent` = ?,
            `games_played` = ?, `seconds_played` = ?, `inventory` = ?,
            `csreward` = ?, `face_codes` = ?, `new_item` = ?, `completion` = ?,
            `progress` = ?, `cs_completion` = ?, `cs_timestamps_1` = ?, `cs_timestamps_2` = ?,
            `cs_timestamps_3` = ?, `display_name` = ?
            WHERE `id` = ?
            """.trimIndent(),
        ) {
            setString(1, player.getNullableSessionToken())
            setInt(2, player.credits)
            setInt(3, player.creditsSpent)
            setInt(4, player.gamesPlayed)
            setLong(5, player.secondsPlayed)
            setString(6, player.inventory)
            setInt(7, player.csReward)
            setString(8, player.faceCodes)
            setString(9, player.newItem)
            setString(10, player.completion)
            setString(11, player.progress)
            setString(12, player.cscompletion)
            setString(13, player.cstimestamps1)
            setString(14, player.cstimestamps2)
            setString(15, player.cstimestamps3)
            setString(16, player.displayName)
            setInt(17, player.playerId)
        }
    }

    fun setPlayerPartial(player: Player, key: String): Future<Void> {
        return when (key) {
            "Base" -> Database.update(
                """
                UPDATE `players` SET 
                `credits` = ?, `credits_spent` = ?, 
                `games_played` = ?, `seconds_played` = ?,
                `inventory` = ?
                WHERE `id` = ?
                """.trimIndent()
            ) {
                setInt(1, player.credits)
                setInt(2, player.creditsSpent)
                setInt(3, player.gamesPlayed)
                setLong(4, player.secondsPlayed)
                setString(5, player.inventory)
                setInt(6, player.playerId)
            }

            "FaceCodes" -> Database.update("UPDATE `players` SET `face_codes` = ? WHERE `id` = ?") {
                setString(1, player.faceCodes)
                setInt(2, player.playerId)
            }

            "NewItem" -> Database.update("UPDATE `players` SET `new_item` = ? WHERE `id` = ?") {
                setString(1, player.newItem)
                setInt(2, player.playerId)
            }

            "csreward" -> Database.update("UPDATE `players` SET `csreward` = ? WHERE `id` = ?") {
                setInt(1, player.csReward)
                setInt(2, player.playerId)
            }

            "Completion" -> Database.update("UPDATE `players` SET `completion` = ? WHERE `id` = ?") {
                setString(1, player.completion)
                setInt(2, player.playerId)
            }

            "Progress" -> Database.update("UPDATE `players` SET `progress` = ? WHERE `id` = ?") {
                setString(1, player.progress)
                setInt(2, player.playerId)
            }

            "cscompletion" -> Database.update("UPDATE `players` SET `cs_completion` = ? WHERE `id` = ?") {
                setString(1, player.cscompletion)
                setInt(2, player.playerId)
            }

            "cstimestamps" -> Database.update("UPDATE `players` SET `cs_timestamps_1` = ? WHERE `id` = ?") {
                setString(1, player.cstimestamps1)
                setInt(2, player.playerId)
            }

            "cstimestamps2" -> Database.update("UPDATE `players` SET `cs_timestamps_2` = ? WHERE `id` = ?") {
                setString(1, player.cstimestamps2)
                setInt(2, player.playerId)
            }

            "cstimestamps3" -> Database.update("UPDATE `players` SET `cs_timestamps_3` = ? WHERE `id` = ?") {
                setString(1, player.cstimestamps3)
                setInt(2, player.playerId)
            }

            else -> Future.completedFuture(null)
        }
    }

    fun createBasic(
        email: String,
        displayName: String,
        hashedPassword: String,
        origin: Boolean,
    ): Future<Player> {
        return Database
            .updateWithKeys("INSERT INTO `players` (`email`, `display_name`, `password`, `inventory`, `origin`) VALUES (?, ?, ?, ?, ?)") {
                setString(1, email)
                setString(2, displayName) // Display name
                setString(3, hashedPassword) // Password
                setString(4, "") // Inventory
                setBoolean(5, origin)
            }
            .thenCompose { keys ->
                val future = Future<Player>()
                if (!keys.next()) {
                    future.completeExceptionally(null)
                } else {
                    val id = keys.getInt(1)
                    future.complete(Player(id, email, displayName, hashedPassword))
                }
                future
            }
    }

    fun getByOrigin(token: String): Future<Player?> {
        val details = OriginDetailsRetriever.retrieve(token) ?: return Future.completedFuture(null)
        return Database
            .query("SELECT * FROM `players` WHERE  `email` = ? AND `origin` = ? LIMIT 1") {
                setString(1, details.email)
                setBoolean(2, true)
            }
            .thenCompose {
                val player = it.asPlayer()
                if (player == null) {
                    createOrigin(details)
                } else {
                    Future.completedFuture(player)
                }
            }
    }

    fun createOrigin(details: OriginDetailsRetriever.OriginDetails): Future<Player> {
        return createBasic(
            details.email,
            details.displayName,
            "",
            true
        ).thenApplyAsync { player ->
            val dataMap = details.dataMap
            if (dataMap.isNotEmpty()) {
                player.setPlayerDataBulk(dataMap)
            }
            player
        }
    }

    fun hasOriginData(details: OriginDetailsRetriever.OriginDetails): Future<Boolean> {
        return Database
            .exists("SELECT * FROM `players` WHERE `email` = ? AND `origin` = ? LIMIT 1") {
                setString(1, details.email)
                setBoolean(2, true)
            }
    }
}