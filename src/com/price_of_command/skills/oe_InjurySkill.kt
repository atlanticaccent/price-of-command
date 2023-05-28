package com.price_of_command.skills

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.characters.*
import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.impl.campaign.skills.BaseSkillEffectDescription
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import com.price_of_command.ConditionManager
import com.price_of_command.conditions.Injury
import org.magiclib.kotlin.getRoundedValueMaxOneAfterDecimal
import java.awt.Color

class pc_InjurySkill {
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

            val suffix = skill.id.removePrefix("pc_injury_").toInt()

            ConditionManager.findByStats(stats)?.let { (officer, conditions) ->
                officer to conditions.filterIsInstance<Injury>().filter { it.injurySkillSuffix == suffix }
            }?.takeIf { (_, conditions) -> conditions.isNotEmpty() }?.also { (officer, conditions) ->
                // this is overcomplicated to account for mods that increase officer levels beyond 7
                val injuries = conditions.size
                val transformedText = if (injuries == 1) {
                    "an injury"
                } else {
                    "$injuries injuries"
                }
                info.addPara("${officer.nameString} has suffered $transformedText.", 0f)

                val settings = Global.getSettings()
                val disabledNames = conditions.map { settings.getSkillSpec(it.skill).name }
                val last = disabledNames.last()
                if (disabledNames.size > 1) {
                    val concatenated = disabledNames.subList(0, disabledNames.size - 1).joinToString(", ")
                    info.addPara(
                        "Until they recover they will not be able to use their skills in %s or %s.",
                        0f,
                        hc,
                        concatenated,
                        last
                    )
                    info.addSkillPanel(Global.getFactory().createPerson().overwriteSkills(conditions), 0f)
                    val rems = conditions.map { it.remaining().duration.getRoundedValueMaxOneAfterDecimal() }.sorted()
                    val remsConcat = rems.subList(0, rems.size - 1).joinToString(", ")
                    val remLast = rems.last()
                    info.addPara(
                        "They will recover from injuries in %s and %s days.", 2f, hc, remsConcat, remLast
                    )
                } else {
                    info.addPara(
                        "Until they recover they will not be able to use their skill in %s.", 0f, hc, last
                    )
                    info.addSkillPanel(Global.getFactory().createPerson().overwriteSkills(conditions), 0f)
                    info.addPara(
                        "They will recover in %s days.",
                        2f,
                        hc,
                        conditions.last().remaining().duration.getRoundedValueMaxOneAfterDecimal()
                    )
                }
            } ?: run {
                info.addPara("bugg", 0f)
            }
        }

        override fun apply(p0: MutableShipStatsAPI?, p1: ShipAPI.HullSize?, p2: String?, p3: Float) = Unit

        override fun unapply(p0: MutableShipStatsAPI?, p1: ShipAPI.HullSize?, p2: String?) = Unit
    }
}

fun PersonAPI.overwriteSkills(injuries: List<Injury>): PersonAPI {
    for (injury in injuries) {
        this.stats.setSkillLevel(injury.skill, injury.level.toFloat())
    }

    return this
}
