package com.laird

import com.fs.starfarer.api.campaign.BaseCampaignEventListener
import com.fs.starfarer.api.combat.EngagementResultAPI

object oe_PostBattleListener : BaseCampaignEventListener(false) {
    override fun reportPlayerEngagement(result: EngagementResultAPI) {
        val deployedPlayerOfficers = if (result.didPlayerWin()) {
            result.winnerResult
        } else {
            result.loserResult
        }.deployed.map { it.captain }.filter { it.faction.isPlayerFaction }

        for (officer in deployedPlayerOfficers) {
            if (!officer.isAICore) {
                ConditionManager.fatigueOfficer(officer)
            }
        }
    }
}