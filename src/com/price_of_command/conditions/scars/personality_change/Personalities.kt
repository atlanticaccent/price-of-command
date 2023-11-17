package com.price_of_command.conditions.scars.personality_change

import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.combat.HullModEffect
import com.fs.starfarer.api.impl.campaign.ids.Personalities
import com.price_of_command.conditions.Condition

abstract class PersonalityChangeStub<T : PersonalityChangeScar>(
    originalPersonality: String,
    target: PersonAPI,
    startDate: Long,
    rootConditions: List<Condition>,
    factionID: String,
    personalityId: String
) : PersonalityChangeScar(originalPersonality, target, startDate, rootConditions, factionID, personalityId)

abstract class SpecStub<T : PersonalityChangeScar>(marker: Class<T>) : Level1<T>(marker), HullModEffect by PersonalityChangeHullmod()

class Timid(
    originalPersonality: String, target: PersonAPI, startDate: Long, rootConditions: List<Condition>, factionID: String
) : PersonalityChangeStub<Timid>(
    originalPersonality, target, startDate, rootConditions, factionID, Personalities.TIMID
) {
    class Spec : SpecStub<Timid>(Timid::class.java)
}

class Cautious(
    originalPersonality: String, target: PersonAPI, startDate: Long, rootConditions: List<Condition>, factionID: String
) : PersonalityChangeStub<Cautious>(
    originalPersonality, target, startDate, rootConditions, factionID, Personalities.CAUTIOUS
) {
    class Spec : SpecStub<Cautious>(Cautious::class.java)
}

class Steady(
    originalPersonality: String, target: PersonAPI, startDate: Long, rootConditions: List<Condition>, factionID: String
) : PersonalityChangeStub<Steady>(
    originalPersonality, target, startDate, rootConditions, factionID, Personalities.STEADY
) {
    class Spec : SpecStub<Steady>(Steady::class.java)
}

class Aggressive(
    originalPersonality: String, target: PersonAPI, startDate: Long, rootConditions: List<Condition>, factionID: String
) : PersonalityChangeStub<Aggressive>(
    originalPersonality, target, startDate, rootConditions, factionID, Personalities.AGGRESSIVE
) {
    class Spec : SpecStub<Aggressive>(Aggressive::class.java)
}

class Reckless(
    originalPersonality: String, target: PersonAPI, startDate: Long, rootConditions: List<Condition>, factionID: String
) : PersonalityChangeStub<Reckless>(
    originalPersonality, target, startDate, rootConditions, factionID, Personalities.RECKLESS
) {
    class Spec : SpecStub<Reckless>(Reckless::class.java)
}