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
import lunalib.lunaExtensions.getList
import java.awt.Color

class oe_InjurySkill {
    class Level0: DescriptionSkillEffect {
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

    class Level1: BaseSkillEffectDescription(), ShipSkillEffect {
        override fun createCustomDescription(
            stats: MutableCharacterStatsAPI?,
            skill: SkillSpecAPI?,
            info: TooltipMakerAPI,
            width: Float
        ) {
            init(stats, skill)
            val officer = playerOfficers().find { it.person.stats.equals(stats) }!!
            val mem = officer.person.escapedWithoutUpdate()
            val id = officer.person.id
            val injuries = mem.getInt(INJURIES)
            val transformedText = if (injuries == 1) {
                "an injury"
            } else {
                "$injuries injuries"
            }
            info.setParaOrbitronLarge()
            info.addPara("${officer.person.nameString} has suffered $transformedText.", 0f)

            val disabledSkills: MutableList<String> = mem.getList<String>(DISABLED_SKILLS)?.toMutableList() ?: mutableListOf()
            val disabledNames = officer.person.stats.skillsCopy.filter { disabledSkills.contains(it.skill.id) }.map { it.skill.name }
            val last = disabledNames.last()
            info.addPara("Until they recover, they cannot use these previously learned skill(s):", 0f)
            if (disabledNames.size > 1) {
                val concatenated = disabledNames.subList(0, disabledNames.size - 1).joinToString(", ")
                info.addPara("%s and %s", 0f, hc, hc, concatenated, last)
            } else {
                info.addPara("%s", 0f, hc, hc, last)
            }

            val lastInjured = mem.getLong(INJURED_FROM).toDateString()
            info.setParaFontDefault()
            info.addPara("They were last injured on $lastInjured", 1f)
        }

        override fun apply(p0: MutableShipStatsAPI?, p1: ShipAPI.HullSize?, p2: String?, p3: Float) {}

        override fun unapply(p0: MutableShipStatsAPI?, p1: ShipAPI.HullSize?, p2: String?) {}
    }
}