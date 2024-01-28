package com.price_of_command.conditions.scars.personality_change

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.characters.PersonalityAPI
import com.fs.starfarer.api.combat.EngagementResultAPI
import com.fs.starfarer.api.impl.campaign.ids.Personalities
import com.price_of_command.conditions
import com.price_of_command.conditions.Condition
import com.price_of_command.conditions.NonPublic
import com.price_of_command.conditions.Outcome
import com.price_of_command.conditions.scars.Scar
import com.price_of_command.conditions.scars.ScarFactory

class PersonalityChangeScar(
    val originalPersonality: String,
    target: PersonAPI,
    startDate: Long,
    rootConditions: List<Condition>,
    val factionId: String,
    val personality: SpecStub<*>
) : Scar(
    target, startDate, rootConditions
) {
    companion object {
        val personalities: Map<Class<out SpecStub<*>>, PersonalityAPI> by lazy {
            mapOf(
                Timid::class.java to Personalities.TIMID,
                Cautious::class.java to Personalities.CAUTIOUS,
                Steady::class.java to Personalities.STEADY,
                Aggressive::class.java to Personalities.AGGRESSIVE,
                Reckless::class.java to Personalities.RECKLESS,
            ).mapValues { (_, id) ->
                val person = Global.getSettings().createPerson()
                person.setPersonality(id)
                person.personalityAPI
            }
        }

        /**
         * Step change must be at least two in a given direction, but will always skip Steady
         * ie: Timid may become Aggressive or Reckless, but Steady _must_ become either Timid or Reckless.
         */
        private fun stepChange(original: PersonalityAPI): SpecStub<*> =
            when (original.id) {
                Personalities.TIMID, Personalities.CAUTIOUS -> listOf(Aggressive(), Reckless())
                Personalities.STEADY -> listOf(Timid(), Reckless())
                Personalities.AGGRESSIVE, Personalities.RECKLESS -> listOf(Timid(), Cautious())
                else -> throw Exception("Only vanilla personalities are supported")
            }.random()

        fun generateApplicable(resultAPI: EngagementResultAPI, officer: PersonAPI): ScarFactory {
            val opposition = if (resultAPI.didPlayerWin()) {
                resultAPI.loserResult.fleet.faction.id
            } else {
                resultAPI.winnerResult.fleet.faction.id
            }
            val originalPersonality = officer.personalityAPI
            val newPersonality = stepChange(originalPersonality)
            return object : ScarFactory {
                override fun build(target: PersonAPI, startDate: Long, rootConditions: List<Condition>): Scar =
                    PersonalityChangeScar(
                        target.personalityAPI.id,
                        target,
                        startDate,
                        rootConditions,
                        opposition,
                        newPersonality
                    )
            }
        }
    }

    val faction = Global.getSector().getFaction(factionId)

    override fun precondition(): Outcome {
        val exists = target.conditions().filterIsInstance<PersonalityChangeScar>().any {
            it.factionId == this.factionId
        }
        return if (exists) {
            Outcome.Failed
        } else {
            Outcome.Applied(this)
        }
    }

    @NonPublic
    override fun inflict(): Outcome {
        target.stats.setSkillLevel("pc_scar_${personality.id.lowercase()}", 1f)
        return Outcome.Applied(this)
    }

    override fun tryResolve() {
        target.stats.setSkillLevel("pc_scar_${personality.id.lowercase()}", 0f)
    }

    override fun pastTense(): String = "mentally scarred"
}
