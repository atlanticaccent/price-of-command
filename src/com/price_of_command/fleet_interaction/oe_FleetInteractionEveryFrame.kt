package com.price_of_command.fleet_interaction

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.OptionPanelAPI
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl

object pc_FleetInteractionEveryFrame : EveryFrameScript {
    private fun shouldAppendOption(optionPanel: OptionPanelAPI): Boolean {
        return !optionPanel.hasOption(pc_FleetInteractionOptionDelegate.OPTION_ID) && FleetInteractionDialogPluginImpl.OptionId.values()
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
                pc_FleetInteractionOptionDelegate.OPTION_ID,
                "Last minute reassignment of captains to ships"
            )

            options.addOptionConfirmation(
                pc_FleetInteractionOptionDelegate.OPTION_ID, pc_FleetInteractionOptionDelegate(dialog)
            )
        }
    }
}