package com.price_of_command.conditions.scars.personality_change

import com.fs.starfarer.api.characters.MutableCharacterStatsAPI
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.characters.ShipSkillEffect
import com.fs.starfarer.api.characters.SkillSpecAPI
import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.impl.campaign.skills.BaseSkillEffectDescription
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.price_of_command.ConditionManager
import com.price_of_command.andThenOrNull
import com.price_of_command.conditions.Condition
import java.util.*

abstract class Level1<T : SpecStub<T>>(private val marker: Class<T>) : BaseSkillEffectDescription(),
    ShipSkillEffect {
    private fun getTarget(stats: MutableCharacterStatsAPI): Pair<PersonAPI, List<PersonalityChangeScar>>? =
        ConditionManager.findByStats(stats)?.let {
            filterConditions(it)
        }

    private fun filterConditions(data: Pair<PersonAPI, List<Condition>>): Pair<PersonAPI, List<PersonalityChangeScar>>? {
        val (officer, conditions) = data
        return conditions.filterIsInstance<PersonalityChangeScar>().filter { it.personality::class.java == marker }
            .let { filtered ->
                filtered.isNotEmpty().andThenOrNull {
                    officer to filtered
                }
            }
    }

    override fun createCustomDescription(
        stats: MutableCharacterStatsAPI, skill: SkillSpecAPI, info: TooltipMakerAPI, width: Float
    ) {
        getTarget(stats)?.also { (officer, conditions) ->
            init(stats, skill)

            val factionNames = conditions.map { name ->
                name.faction.displayName.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(
                        Locale.getDefault()
                    ) else it.toString()
                }
            }
            val concat = if (conditions.size > 1) {
                factionNames.joinToString(limit = factionNames.size - 1, truncated = "")
                    .plus(", and ${factionNames.last()}")
            } else {
                factionNames.first()
            }

            info.addPara(
                "This officer has suffered significant mental trauma at the hands of the $concat",
                0f,
                hc,
                concat
            )
            val newPersonality = PersonalityChangeScar.personalities[marker]?.displayName
            val originalPersonality = officer.personalityAPI.displayName
            info.addPara(
                "When in battle, they will adopt a $newPersonality personality instead of being $originalPersonality.",
                2f,
                hc,
                newPersonality,
                originalPersonality
            )
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
