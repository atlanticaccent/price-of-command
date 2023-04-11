package com.laird

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.BaseCampaignEventListener
import com.fs.starfarer.api.combat.EngagementResultAPI
import kotlin.random.Random

class PostBattleListener : BaseCampaignEventListener(false) {
    override fun reportPlayerEngagement(result: EngagementResultAPI?) {
        val playerFleet = Global.getSector().playerFleet.fleetData

        val deployedPlayerOfficers =
            result?.battle?.playerSide?.flatMap { it.fleetData.officersCopy }.orEmpty().toSet().intersect(
                playerFleet.officersCopy.toSet()
            )

        val now = Global.getSector().clock.timestamp
        val rand = Random(now)
        for (officer in deployedPlayerOfficers) {
            val mem = playerFleet.getOfficerData(officer.person).person.memoryWithoutUpdate
            if (mem.contains("fatigued_from") || mem.contains("injured_from")) {
                if (rand.nextFloat() >= 0.5) {
                    val injuries = mem.getInt("injuries") + 1
                    mem.set("injuries", injuries)
                    mem.set("injured_from", now)

//                    officer.person.stats.re
                }
            }

            mem.set("fatigued_from", now)
        }
    }
}