package com.price_of_command.conditions.scars

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.characters.PersonalityAPI
import com.fs.starfarer.api.impl.campaign.ids.Personalities
import com.price_of_command.conditions
import com.price_of_command.conditions.Condition
import com.price_of_command.conditions.NonPublic
import com.price_of_command.conditions.Outcome

sealed class PersonalityChange(
    val originalPersonality: String,
    target: PersonAPI,
    startDate: Long,
    rootConditions: List<Condition>,
    val factionID: String,
    val personalityID: String
) : Scar(
    target, startDate, rootConditions
) {
    companion object {
        val personalities: Map<Class<out PersonalityChange>, PersonalityAPI> by lazy {
            mapOf(
                Timid::class.java to Personalities.TIMID,
                Cautious::class.java to Personalities.CAUTIOUS,
                Steady::class.java to Personalities.STEADY,
                Aggressive::class.java to Personalities.AGGRESSIVE,
                Reckless::class.java to Personalities.RECKLESS
            ).mapValues { (_, id) ->
                val person = Global.getSettings().createPerson()
                person.setPersonality(id)
                person.personalityAPI
            }
        }
    }

    val faction = Global.getSector().getFaction(factionID)

    override fun precondition(): Outcome {
        val exists = target.conditions().filterIsInstance<PersonalityChange>().any {
            it::class == this::class && it.factionID == this.factionID
        }
        return if (exists) {
            Outcome.Failed
        } else {
            Outcome.Applied(this)
        }
    }

    @NonPublic
    override fun inflict(): Outcome {
        target.stats.setSkillLevel("pc_scar_${personalityID.lowercase()}", 1f)
        return Outcome.Applied(this)
    }

    override fun pastTense(): String = "mentally scarred"
}

class Timid(originalPersonality: String, target: PersonAPI, startDate: Long, rootConditions: List<Condition>, factionID: String) :
    PersonalityChange(originalPersonality, target, startDate, rootConditions, factionID, Personalities.TIMID)

class Cautious(originalPersonality: String, target: PersonAPI, startDate: Long, rootConditions: List<Condition>, factionID: String) :
    PersonalityChange(originalPersonality, target, startDate, rootConditions, factionID, Personalities.CAUTIOUS)

class Steady(originalPersonality: String, target: PersonAPI, startDate: Long, rootConditions: List<Condition>, factionID: String) :
    PersonalityChange(originalPersonality, target, startDate, rootConditions, factionID, Personalities.STEADY)

class Aggressive(originalPersonality: String, target: PersonAPI, startDate: Long, rootConditions: List<Condition>, factionID: String) :
    PersonalityChange(originalPersonality, target, startDate, rootConditions, factionID, Personalities.AGGRESSIVE)

class Reckless(originalPersonality: String, target: PersonAPI, startDate: Long, rootConditions: List<Condition>, factionID: String) :
    PersonalityChange(originalPersonality, target, startDate, rootConditions, factionID, Personalities.RECKLESS)