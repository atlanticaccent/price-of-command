package com.laird

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
import org.magiclib.kotlin.getRoundedValueMaxOneAfterDecimal
import java.awt.Color

class oe_FatigueSkill {
    class Level0: DescriptionSkillEffect {
        override fun getString(): String {
            return "This officer is fatigued from a recent battle"
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

    class Level1: BaseSkillEffectDescription(), ShipSkillEffect {
        override fun createCustomDescription(
            stats: MutableCharacterStatsAPI?,
            skill: SkillSpecAPI?,
            info: TooltipMakerAPI,
            width: Float
        ) {
            init(stats, skill)
            val officer = playerOfficers().find { it.person.stats.equals(stats) }!!
            val fatiguedSince = officer.person.escapedWithoutUpdate().getLong(FATIGUED_FROM).toDateString()
            val fatiguedFor = officer.person.escapedWithoutUpdate().getExpire(FATIGUED_FROM).getRoundedValueMaxOneAfterDecimal()
            info.setParaOrbitronLarge()
            info.addPara("${officer.person.nameString} is fatigued for the next $fatiguedFor days.", 0f)
            info.setParaFontDefault()
            info.addPara("They have been fatigued since $fatiguedSince due to a previous battle", 1f)
        }

        override fun apply(p0: MutableShipStatsAPI?, p1: ShipAPI.HullSize?, p2: String?, p3: Float) {}

        override fun unapply(p0: MutableShipStatsAPI?, p1: ShipAPI.HullSize?, p2: String?) {}
    }
}