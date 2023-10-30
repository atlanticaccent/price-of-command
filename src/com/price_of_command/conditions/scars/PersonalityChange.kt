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
    target: PersonAPI,
    startDate: Long,
    rootConditions: List<Condition>,
    val factionID: String,
    val personalityID: String
) : Scar(
    target, startDate, rootConditions
) {
    companion object {
        val personalities: List<PersonalityAPI> by lazy {
            listOf(
                Personalities.TIMID,
                Personalities.CAUTIOUS,
                Personalities.STEADY,
                Personalities.AGGRESSIVE,
                Personalities.RECKLESS
            ).let {
                val person = Global.getSettings().createPerson()
                it.map { id ->
                    person.setPersonality(id)
                    person.personalityAPI
                }
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
        target.stats.setSkillLevel("pc_scar_$this.personalityID", 1f)
        return Outcome.Applied(this)
    }

    override fun pastTense(): String = "mentally scarred"
}

class Timid(target: PersonAPI, startDate: Long, rootConditions: List<Condition>, factionID: String) :
    PersonalityChange(target, startDate, rootConditions, factionID, Personalities.TIMID)

class Cautious(target: PersonAPI, startDate: Long, rootConditions: List<Condition>, factionID: String) :
    PersonalityChange(target, startDate, rootConditions, factionID, Personalities.CAUTIOUS)

class Steady(target: PersonAPI, startDate: Long, rootConditions: List<Condition>, factionID: String) :
    PersonalityChange(target, startDate, rootConditions, factionID, Personalities.STEADY)

class Aggressive(target: PersonAPI, startDate: Long, rootConditions: List<Condition>, factionID: String) :
    PersonalityChange(target, startDate, rootConditions, factionID, Personalities.AGGRESSIVE)

class Reckless(target: PersonAPI, startDate: Long, rootConditions: List<Condition>, factionID: String) :
    PersonalityChange(target, startDate, rootConditions, factionID, Personalities.RECKLESS)