package com.officer_expansion

import com.fs.starfarer.api.campaign.BaseCampaignEventListener
import com.fs.starfarer.api.combat.EngagementResultAPI
import com.officer_expansion.conditions.*

object oe_CampaignEventListener : BaseCampaignEventListener(false) {
    override fun reportPlayerEngagement(result: EngagementResultAPI) {
        val deployedPlayerOfficers = if (result.didPlayerWin()) {
            result.winnerResult
        } else {
            result.loserResult
        }.deployed.map { it.captain }.filter { it.faction.isPlayerFaction && !it.isPlayer }

        for (officer in deployedPlayerOfficers) {
            if (!officer.isAICore) {
                val fatigue = Fatigue(officer, ConditionManager.now)
                val outcome = fatigue.tryInflictAppend()

                if (outcome is Outcome.Applied<*>) {
                    when (outcome.condition) {
                        is Fatigue -> logger().debug("Fatigued officer ${officer.nameString}")
                        is Injury -> logger().debug("Failed to fatigue officer ${officer.nameString}, injured them instead")
                        is GraveInjury -> logger().debug("Failed to fatigue or injure officer ${officer.nameString}, gravely injuring them instead")
                        is Wound -> logger().debug("Did not fatigue ${officer.nameString}, applied some kind of wound instead ${outcome.condition}")
                        else -> {
                            logger().debug("${officer.nameString} was not fatigued or injured when trying to fatigue. This is probably a bug")
                            logger().debug(outcome.condition)
                        }
                    }
                }
                // TODO: outcome can be terminal
            }
        }
    }
}


