package com.price_of_command.skills.scars

import com.fs.starfarer.api.GameState
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.characters.CharacterStatsSkillEffect
import com.fs.starfarer.api.characters.DescriptionSkillEffect
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.characters.ShipSkillEffect
import com.fs.starfarer.api.characters.SkillEffectType
import com.fs.starfarer.api.characters.SkillSpecAPI
import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.impl.campaign.skills.BaseSkillEffectDescription
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import com.price_of_command.ConditionManager
import com.price_of_command.andThenOrNull
import com.price_of_command.conditions.Condition
import com.price_of_command.conditions.Injury
import com.price_of_command.conditions.scars.*
import com.price_of_command.conditions.scars.Aggressive
import com.price_of_command.conditions.scars.Cautious
import com.price_of_command.conditions.scars.Reckless
import com.price_of_command.conditions.scars.Steady
import com.price_of_command.conditions.scars.Timid
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

    abstract class Level1<T: PersonalityChange>(private val marker: Class<T>) : BaseSkillEffectDescription(), ShipSkillEffect {
        private fun getTarget(stats: MutableCharacterStatsAPI): Pair<PersonAPI, List<T>>? = ConditionManager.findByStats(stats)?.let {
            filterConditions(it)
        }

        @Suppress("UNCHECKED_CAST")
        private fun filterConditions(data: Pair<PersonAPI, List<Condition>>): Pair<PersonAPI, List<T>>? {
            val (officer, conditions) = data
            return conditions.filter { it::class.java == marker }.map { it as T }.let { filtered ->
                filtered.isNotEmpty().andThenOrNull {
                    officer to filtered
                }
            }
        }

        override fun createCustomDescription(
            stats: MutableCharacterStatsAPI, skill: SkillSpecAPI, info: TooltipMakerAPI, width: Float
        ) {
            init(stats, skill)

            getTarget(stats)?.takeIf { (_, conditions) -> conditions.isNotEmpty() }?.also { (officer, conditions) ->
                val settings = Global.getSettings()


            } ?: run {
                info.addPara("bugg", 0f)
            }
        }

        override fun apply(stats: MutableShipStatsAPI, hullSize: ShipAPI.HullSize?, id: String, level: Float) {
            val shipMember = stats.fleetMember
            val captain = shipMember.captain

            if (Global.getCurrentState() != GameState.COMBAT) return

            val factionID = Global.getCombatEngine().context.otherFleet.faction.id

            ConditionManager.conditionMap[captain]?.let { conditions ->
                filterConditions(captain to conditions)?.let { (_, filtered) ->
                    filtered.firstOrNull { it.factionID == factionID }?.let {
                        captain.setPersonality(it.personalityID)
                    }
                }
            }
        }

        override fun unapply(stats: MutableShipStatsAPI, hullSize: ShipAPI.HullSize, id: String) {
            val shipMember = stats.fleetMember
            val captain = shipMember.captain

            ConditionManager.conditionMap[captain]?.let { conditions ->
                filterConditions(captain to conditions)?.let { (_, filtered) ->
                    val applicable = filtered.first()
                    captain.setPersonality(applicable.originalPersonality)
                }
            }
        }
    }
}

class Timid : Personality.Level1<Timid>(Timid::class.java)

class Cautious : Personality.Level1<Cautious>(Cautious::class.java)

class Steady : Personality.Level1<Steady>(Steady::class.java)

class Aggressive : Personality.Level1<Aggressive>(Aggressive::class.java)

class Reckless : Personality.Level1<Reckless>(Reckless::class.java)
