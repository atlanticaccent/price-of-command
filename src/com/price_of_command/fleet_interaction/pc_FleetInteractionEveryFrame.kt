package com.price_of_command.fleet_interaction

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.OptionPanelAPI
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl
import com.price_of_command.pc_CampaignEventListener

object pc_FleetInteractionEveryFrame : EveryFrameScript {
    var fleetInteractionWasOpen = false

    private fun shouldAppendOption(optionPanel: OptionPanelAPI): Boolean {
        return !optionPanel.hasOption(pc_AutoClosingOptionDelegate.OPTION_ID) && FleetInteractionDialogPluginImpl.OptionId.values()
            .any {
                optionPanel.hasOption(it)
            }
    }

    override fun isDone(): Boolean = false

    override fun runWhilePaused(): Boolean = true

    override fun advance(amount: Float) {
        val dialog = Global.getSector().campaignUI.currentInteractionDialog
        if (dialog != null && dialog.plugin is FleetInteractionDialogPluginImpl && shouldAppendOption(dialog.optionPanel)) {
            val options = dialog.optionPanel

            dialog.optionPanel.addOption(
                "Reassign captains",
                pc_AutoClosingOptionDelegate.OPTION_ID,
                "Last minute reassignment of captains to ships"
            )

            options.addOptionConfirmation(
                pc_AutoClosingOptionDelegate.OPTION_ID, pc_ReassignOfficerOptionDelegate(dialog)
            )

            dialog.setOptionOnEscape("foo", pc_AutoClosingOptionDelegate.OPTION_ID)

            fleetInteractionWasOpen = true
        }
        if (dialog == null && fleetInteractionWasOpen) {
            fleetInteractionWasOpen = false
            pc_CampaignEventListener.restoreFleetAssignments()
        }
    }
}