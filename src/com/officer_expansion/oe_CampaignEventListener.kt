package com.officer_expansion

import com.fs.starfarer.api.campaign.BaseCampaignEventListener
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.combat.EngagementResultAPI
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl

object oe_CampaignEventListener : BaseCampaignEventListener(false) {
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

    override fun reportShownInteractionDialog(dialog: InteractionDialogAPI) {
        logger().debug("${dialog.optionPanel}")
        logger().debug("${dialog.plugin is FleetInteractionDialogPluginImpl}")

        val plugin = dialog.plugin

        if (plugin is FleetInteractionDialogPluginImpl) {
            dialog.plugin = oe_InteractionDialogPluginImpl(dialog, dialog.plugin)

            FleetInteractionDialogPluginImpl.OptionId.values().forEach {
                dialog.optionPanel.addOptionConfirmation(it, oe_EvilOptionDelegate(dialog, it))
            }

            dialog.optionPanel.addOption(
                "Reassign captains",
                oe_FleetInteractionOptionDelegate.OPTION_ID,
                "Last minute reassignment of captains to ships"
            )

            dialog.optionPanel.addOptionConfirmation(
                oe_FleetInteractionOptionDelegate.OPTION_ID,
                oe_FleetInteractionOptionDelegate(dialog)
            )
        }
    }
}


