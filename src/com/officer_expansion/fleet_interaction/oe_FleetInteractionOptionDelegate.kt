package com.officer_expansion.fleet_interaction

import com.fs.starfarer.api.campaign.BaseStoryPointActionDelegate
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import java.awt.Robot
import java.awt.event.KeyEvent

class oe_FleetInteractionOptionDelegate(private val dialog: InteractionDialogAPI) : BaseStoryPointActionDelegate() {
    companion object {
        const val OPTION_ID = "oe_fleet_interaction_dialog_sp_option"
    }

    override fun getRequiredStoryPoints(): Int = 0

    override fun withSPInfo(): Boolean = false

    override fun createDescription(info: TooltipMakerAPI) {
        dialog.showCustomDialog(800f, 500f, oe_ReassignOfficerCustomPanel())

        val robot = Robot()
        robot.keyPress(KeyEvent.VK_ESCAPE)
        robot.keyRelease(KeyEvent.VK_ESCAPE)
    }

    override fun getLogText(): String =
        "Why are we still here? Just to suffer? Every night, I can feel my leg... And my arm... even my fingers... The body I've lost... the comrades I've lost... won't stop hurting... It's like they're all still there. You feel it, too, don't you? I'm gonna make them give back our past!"
}
