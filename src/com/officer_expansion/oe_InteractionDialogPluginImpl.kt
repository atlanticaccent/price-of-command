package com.officer_expansion

import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.InteractionDialogPlugin

class oe_InteractionDialogPluginImpl(private val dialog: InteractionDialogAPI, private val plugin: InteractionDialogPlugin) : InteractionDialogPlugin by plugin {
    override fun optionSelected(optionText: String?, optionData: Any?) {
        dialog.plugin = plugin
        dialog.optionPanel.addOption(
            "Reassign captains",
            oe_FleetInteractionOptionDelegate.OPTION_ID,
            "Last minute reassignment of captains to ships"
        )

        dialog.optionPanel.addOptionConfirmation(
            oe_FleetInteractionOptionDelegate.OPTION_ID,
            oe_FleetInteractionOptionDelegate(dialog)
        )
        plugin.optionSelected(optionText, optionData)
    }
}