package com.officer_expansion

import com.fs.starfarer.api.impl.campaign.intel.MessageIntel
import com.fs.starfarer.api.util.Misc
import com.officer_expansion.conditions.Condition

class oe_RecoveryIntel private constructor(name: String) :
    MessageIntel("$name has recovered", Misc.getBasePlayerColor()) {
    companion object {
        fun formatConditions(cond: List<Condition>): String {
            val conditions = cond.distinctBy { it::class }
            return if (conditions.size <= 1) {
                conditions.first().pastTense()
            } else {
                "${
                    conditions.subList(0, conditions.size - 1).joinToString(", ") { it.pastTense() }
                } nor ${conditions.last().pastTense()}"
            }
        }
    }

    constructor(name: String, conditions: List<Condition>) : this(name) {
        val formattedConditions = formatConditions(conditions)
        addLine(
            "They are no longer $formattedConditions",
            Misc.getBasePlayerColor(),
            arrayOf(formattedConditions),
            Misc.getHighlightColor()
        )
    }
}