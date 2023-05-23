package com.officer_expansion

import com.fs.starfarer.api.campaign.BaseStoryPointActionDelegate
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl.OptionId
import com.fs.starfarer.api.ui.TooltipMakerAPI
import java.awt.Robot
import java.awt.event.KeyEvent

class oe_EvilOptionDelegate(private val dialog: InteractionDialogAPI, private val optionId: OptionId) : BaseStoryPointActionDelegate() {
    companion object {
        const val OPTION_ID = "oe_fleet_interaction_dialog_sp_option_evil"
    }

    override fun confirm() {
        // code here
    }

    override fun getRequiredStoryPoints(): Int = 0

    override fun withSPInfo(): Boolean = false

    override fun createDescription(info: TooltipMakerAPI?) {
        Robot().apply { keyPress(KeyEvent.VK_SPACE); keyRelease(KeyEvent.VK_SPACE) }
    }

    override fun getLogText(): String = "Such a lust for revenge! Whoooooo?"
}