package com.jacobtread.kme.game.match

import com.jacobtread.blaze.TdfContainer
import com.jacobtread.blaze.group
import com.jacobtread.blaze.list
import com.jacobtread.blaze.tdf.GroupTdf
import com.jacobtread.blaze.text
import java.util.*

/**
 * MatchRuleSet Represents a map of rules to their values which
 * can be compared against a game attribute map to check if the
 * game matches the rule-set
 *
 * @constructor Create empty MatchRuleSet
 */
class MatchRuleSet(container: TdfContainer) {
    /**
     * rules The map of rule types to the values of the rule
     */
    private val rules = EnumMap<MatchRules, String>(MatchRules::class.java)

    /**
     * Initializes the match rules from the provided tdf
     * container. Sets the rules on the underlying rule map
     */
    init {
        val crit = container.group("CRIT")
        val ruleList = crit.list<GroupTdf>("RLST") // Rule list
        ruleList.forEach { group ->
            val name = group.text("NAME") // Name of the rule (key)
            val values = group.list<String>("VALU") // Value of the rule
            val rule = MatchRules.getByKey(name) // Find the rule in the known rules
            if (rule != null && values.isNotEmpty()) {
                rules[rule] = values.first() // Set the rule
            }
        }
    }

    /**
     * validate Validates the provided game attribute map
     * against the rule set checking if the attributes match
     *
     * @param attrs The game attributes
     * @return Whether the game attributes match this rule set
     */
    fun validate(attrs: Map<String, String>): Boolean {
        return MatchRules.values().all {
            val ruleValue = rules[it]
            val attrValue = attrs[it.attrKey]
            if (ruleValue == "abstain") {
                true
            } else {
                attrValue == ruleValue
            }
        }
    }
}