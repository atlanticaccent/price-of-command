package com.price_of_command.memorial

import com.fs.starfarer.api.campaign.CampaignClockAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.price_of_command.conditions.Condition

data class DeathData(
    val person: PersonAPI,
    val date: CampaignClockAPI,
    val ship: FleetMemberAPI?,
    val location: SectorEntityToken?,
    val cause: String?,
    val conditionsOnDeath: List<Condition>,
) {
    fun causeOfDeath(): String = cause ?: "Unknown"

    fun placeOfDeath(): String = location?.containingLocation?.name ?: "Unknown"
}