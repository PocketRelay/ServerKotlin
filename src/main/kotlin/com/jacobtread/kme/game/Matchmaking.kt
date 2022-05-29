package com.jacobtread.kme.game

import com.jacobtread.kme.blaze.TdfContainer
import com.jacobtread.kme.blaze.group
import com.jacobtread.kme.blaze.list
import com.jacobtread.kme.blaze.tdf.GroupTdf
import com.jacobtread.kme.blaze.text
import java.util.*

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

        fun isIgnored(type: Rules): Boolean = this[type] == "abstain"

        operator fun set(type: Rules, value: String) {
            rules[type] = value
        }

        operator fun get(type: Rules): String? = rules[type]

        fun isValidAttributes(attrs: Map<String, String>) {

        }
    }

    fun extractRuleSet(container: TdfContainer): RuleSet {
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