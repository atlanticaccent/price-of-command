package com.price_of_command.skills.scars

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.characters.CharacterStatsSkillEffect
import com.fs.starfarer.api.characters.DescriptionSkillEffect
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI
import com.fs.starfarer.api.characters.ShipSkillEffect
import com.fs.starfarer.api.characters.SkillSpecAPI
import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.impl.campaign.skills.BaseSkillEffectDescription
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import com.price_of_command.ConditionManager
import com.price_of_command.conditions.Injury
import com.price_of_command.conditions.scars.PersonalityChange
import com.price_of_command.skills.overwriteSkills
import org.magiclib.kotlin.getRoundedValueMaxOneAfterDecimal
import java.awt.Color

class Personality {
    abstract class Level0 : DescriptionSkillEffect {
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

    abstract class Level1 : BaseSkillEffectDescription(), CharacterStatsSkillEffect {
        override fun createCustomDescription(
            stats: MutableCharacterStatsAPI, skill: SkillSpecAPI, info: TooltipMakerAPI, width: Float
        ) {
            init(stats, skill)

            ConditionManager.findByStats(stats)?.let { (officer, conditions) ->
                officer to conditions.filterIsInstance<PersonalityChange>()
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
                    info.addSkillPanel(Global.getFactory().createPerson().overwriteSkills(conditions.map { it.skill to it.level?.toFloat() }), 0f)
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
                    info.addSkillPanel(Global.getFactory().createPerson().overwriteSkills(conditions.map { it.skill to it.level?.toFloat() }), 0f)
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

        override fun apply(stats: MutableCharacterStatsAPI, id: String, level: Float) {

        }

        override fun unapply(stats: MutableCharacterStatsAPI, id: String) {

        }
    }
}