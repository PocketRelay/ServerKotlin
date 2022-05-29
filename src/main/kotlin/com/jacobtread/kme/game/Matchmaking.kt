package com.jacobtread.kme.game

import com.jacobtread.kme.blaze.TdfContainer
import com.jacobtread.kme.blaze.group
import com.jacobtread.kme.blaze.list
import com.jacobtread.kme.blaze.tdf.GroupTdf
import com.jacobtread.kme.blaze.text
import java.util.*
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

object Matchmaking {

    enum class Rules(val key: String, val attrKey: String) {
        MAP("ME3_gameMapMatchRule", "ME3map"),
        ENEMY("ME3_gameEnemyTypeRule", "ME3gameEnemyType"),
        DIFFICULTY("ME3_gameDifficultyRule", "ME3gameDifficulty");

        companion object {
            fun getByKey(key: String): Rules? = values().firstOrNull { it.key == key }
        }
    }

    class RuleSet {
        private val rules = EnumMap<Rules, String>(Rules::class.java)

        operator fun set(type: Rules, value: String) {
            rules[type] = value
        }

        fun validate(attrs: Map<String, String>): Boolean {
            return Rules.values().all {
                val ruleValue = rules[it]
                val attrValue = attrs[it.attrKey]
                if (ruleValue == "abstain") {
                    true
                } else {
                    attrValue == ruleValue
                }
            }
        }

        companion object {
            fun extract(container: TdfContainer): RuleSet {
                val ruleSet = RuleSet()
                val crit = container.group("CRIT")
                val ruleList = crit.list<GroupTdf>("RLST")
                ruleList.forEach { group ->
                    val name = group.text("NAME")
                    val values = group.list<String>("VALU")
                    val rule = Rules.getByKey(name)
                    if (rule != null && values.isNotEmpty()) {
                        ruleSet[rule] = values.first()
                    }
                }
                return ruleSet
            }
        }
    }

    private val waitingPlayers = HashMap<PlayerSession, RuleSet>()
    private val waitingLock = ReentrantReadWriteLock()

    fun onNewGameCreated(game: Game) {
        val attributes = game.getAttributes()
        waitingLock.read {
            val iterator = waitingPlayers.iterator()
            while (iterator.hasNext()) {
                val (session, ruleSet) = iterator.next()
                if (ruleSet.validate(attributes)) {
                    game.join(session)
                    session.send(game.host.createSessionDetails())
                    game.getActivePlayers().forEach {
                        session.send(it.createSessionDetails())
                    }
                    session.send(game.createPoolPacket(false))
                    waitingLock.write {
                        session.matchmaking = false
                        iterator.remove()
                    }
                }
            }
        }
    }

    fun getMatchOrQueue(session: PlayerSession, ruleSet: RuleSet): Game? {
        val game = GameManager.tryFindGame { ruleSet.validate(it.getAttributes()) }
        if (game != null) return game
        session.matchmaking = true
        waitingLock.write { waitingPlayers[session] = ruleSet }
        return null
    }

    fun removeFromQueue(session: PlayerSession) {
        session.matchmaking = false
        waitingLock.write { waitingPlayers.remove(session) }
    }

}