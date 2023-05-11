package com.laird

import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.laird.conditions.Condition

class oe_RecoveryIntel(private val name: String, private val conditions: List<Condition>): BaseIntelPlugin() {
    var seen = false
    var endWhenSeen = false

    override fun endAfterDelay() {
        if (!seen) {
            endWhenSeen = true
            return  // don't do anything if not seen by player
        }
        super.endAfterDelay()
    }

    override fun reportMadeVisibleToPlayer() {
        seen = true
        if (endWhenSeen) {
            endAfterDelay()
        }
    }

    override fun createSmallDescription(info: TooltipMakerAPI, width: Float, height: Float) {
        val last = conditions.last().pastTense()
        if (conditions.size > 1) {
            val concat = conditions.slice(0 until conditions.lastIndex).joinToString(", ")
            info.addPara("They are no longer $concat nor $last", 2f)
        } else {
            info.addPara("They are no longer $last", 2f)
        }
    }

    override fun getSmallDescriptionTitle() = "$name has recovered"

    override fun getBaseDaysAfterEnd() = 15f
}