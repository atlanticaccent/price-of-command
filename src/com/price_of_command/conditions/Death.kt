package com.price_of_command.conditions

import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.price_of_command.clock
import com.price_of_command.memorial.DeathData

class Death(target: PersonAPI, startDate: Long, rootConditions: List<Condition>, var cause: String? = null) :
    Condition(target, startDate, rootConditions) {
    override fun precondition(): Outcome = if (target.isPlayer) Outcome.NOOP
    else Outcome.Applied(this)

    @NonPublic
    override fun inflict(): Outcome = Outcome.Terminal(this)

    override fun pastTense(): String = "dead"

    fun toDeathData(ship: FleetMemberAPI?, location: SectorEntityToken) =
        DeathData(target, clock().createClock(startDate), ship, location, cause)
}