package com.price_of_command.conditions.scars.personality_change

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.characters.*
import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.impl.campaign.skills.BaseSkillEffectDescription
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import com.price_of_command.ConditionManager
import com.price_of_command.andThenOrNull
import com.price_of_command.conditions.Condition
import java.awt.Color

class Level0 : DescriptionSkillEffect {
    override fun getString(): String {
        return "This officer has been injured"
    }

    override fun getHighlights(): Array<String> {
        return arrayOf("" + Global.getSettings().getInt("officerMaxLevel"))
    }

    override fun getHighlightColors(): Array<Color> {
        val h = Misc.getDarkHighlightColor()
        return arrayOf(h)
    }

    override fun getTextColor(): Color? {
        return null
    }
}

abstract class Level1<T : PersonalityChangeScar> : CustomSkillDescription by BaseSkillEffectDescription(), ShipSkillEffect {
    private fun getTarget(stats: MutableCharacterStatsAPI): Pair<PersonAPI, List<T>>? =
        ConditionManager.findByStats(stats)?.let {
            filterConditions(it)
        }

    @Suppress("UNCHECKED_CAST")
    private fun filterConditions(data: Pair<PersonAPI, List<Condition>>): Pair<PersonAPI, List<T>>? {
        val (officer, conditions) = data
        return conditions.filter { it::class.java == this::class.java }.map { it as T }.let { filtered ->
            filtered.isNotEmpty().andThenOrNull {
                officer to filtered
            }
        }
    }

    override fun createCustomDescription(
        stats: MutableCharacterStatsAPI, skill: SkillSpecAPI, info: TooltipMakerAPI, width: Float
    ) {
        getTarget(stats)?.takeIf { (_, conditions) -> conditions.isNotEmpty() }?.also { (officer, conditions) ->
            val settings = Global.getSettings()


        } ?: run {
            info.addPara("bugg", 0f)
        }
    }

    override fun apply(stats: MutableShipStatsAPI, hullSize: ShipAPI.HullSize?, id: String, level: Float) {
        stats.fleetMember?.variant?.apply {
            addPermaMod("ScarPersonalityChanger")
        }
    }

    override fun unapply(stats: MutableShipStatsAPI, hullSize: ShipAPI.HullSize, id: String) {
        stats.fleetMember?.variant?.apply {
            removePermaMod("ScarPersonalityChanger")
        }
    }
}
