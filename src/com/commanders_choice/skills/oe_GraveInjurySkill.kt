package com.commanders_choice.skills

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.characters.DescriptionSkillEffect
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI
import com.fs.starfarer.api.characters.ShipSkillEffect
import com.fs.starfarer.api.characters.SkillSpecAPI
import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.impl.campaign.skills.BaseSkillEffectDescription
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import com.commanders_choice.ConditionManager
import com.commanders_choice.conditions.GraveInjury
import org.magiclib.kotlin.getRoundedValueMaxOneAfterDecimal
import java.awt.Color

class oe_GraveInjurySkill {
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

    class Level1 : BaseSkillEffectDescription(), ShipSkillEffect {
        override fun createCustomDescription(
            stats: MutableCharacterStatsAPI, skill: SkillSpecAPI, info: TooltipMakerAPI, width: Float
        ) {
            init(stats, skill)

            ConditionManager.findByStats(stats)?.let { (officer, conditions) ->
                conditions.filterIsInstance<GraveInjury>().firstOrNull()?.let { officer to it }
            }?.also { (officer, injury) ->
                info.addPara("${officer.nameString} has suffered a %s.", 0f, tc, hc, "grave injury")
                info.addPara(
                    "They will recover in %s days.",
                    0f,
                    hc,
                    injury.remaining().duration.getRoundedValueMaxOneAfterDecimal()
                )
            } ?: run {
                info.addPara("bugg", 0f)
            }
        }

        override fun apply(p0: MutableShipStatsAPI?, p1: ShipAPI.HullSize?, p2: String?, p3: Float) = Unit

        override fun unapply(p0: MutableShipStatsAPI?, p1: ShipAPI.HullSize?, p2: String?) = Unit
    }
}