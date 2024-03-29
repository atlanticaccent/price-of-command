package com.price_of_command

import com.fs.starfarer.api.impl.campaign.intel.MessageIntel
import com.fs.starfarer.api.util.Misc
import com.price_of_command.conditions.Condition

class pc_RecoveryIntel private constructor(name: String) :
    MessageIntel("$name has recovered", Misc.getBasePlayerColor()) {
    companion object {
        fun formatConditions(cond: List<Condition>, positive: Boolean = false): String {
            val conditions = cond.distinctBy { it::class }
            return if (conditions.size <= 1) {
                conditions.first().pastTense()
            } else {
                "${
                    conditions.subList(0, conditions.size - 1).joinToString(", ") { it.pastTense() }
                } ${if (positive) "and" else "nor"} ${conditions.last().pastTense()}"
            }
        }
    }

    constructor(name: String, conditions: List<Condition>, addedDuring: List<Condition>? = null) : this(name) {
        var formattedConditions = formatConditions(conditions)
        addLine(
            "They are no longer $formattedConditions",
            Misc.getBasePlayerColor(),
            arrayOf(formattedConditions),
            Misc.getHighlightColor()
        )
        addedDuring?.let {
            formattedConditions = formatConditions(addedDuring, true)
            addLine(
                "They are now $formattedConditions",
                Misc.getBasePlayerColor(),
                arrayOf(formattedConditions),
                Misc.getHighlightColor()
            )
        }
    }
}