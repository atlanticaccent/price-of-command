package com.officer_expansion

import com.fs.starfarer.api.campaign.BaseCampaignEventListener
import com.fs.starfarer.api.combat.EngagementResultAPI
import com.officer_expansion.conditions.Fatigue

object oe_CampaignEventListener : BaseCampaignEventListener(false) {
    override fun reportPlayerEngagement(result: EngagementResultAPI) {
        val deployedPlayerOfficers = if (result.didPlayerWin()) {
            result.winnerResult
        } else {
            result.loserResult
        }.deployed.map { it.captain }.filter { it.faction.isPlayerFaction && !it.isPlayer }

        for (officer in deployedPlayerOfficers) {
            if (!officer.isAICore) {
                if (!ConditionManager.fatigueOfficer(officer) || !Fatigue.fatigueEnabled()) {
                    ConditionManager.injureOfficer(officer)
                }
            }
        }
    }
}


