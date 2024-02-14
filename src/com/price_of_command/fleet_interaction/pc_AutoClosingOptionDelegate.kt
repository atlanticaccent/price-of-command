package com.price_of_command.fleet_interaction

import com.fs.starfarer.api.campaign.BaseStoryPointActionDelegate
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.campaign.CampaignEngine
import com.fs.starfarer.campaign.fleet.CampaignFleet
import com.price_of_command.OfficerExpansionPlugin
import com.price_of_command.playerFleet
import com.price_of_command.reflection.ReflectionUtils
import org.magiclib.kotlin.isAutomated

open class pc_AutoClosingOptionDelegate(val block: () -> Unit) :
    BaseStoryPointActionDelegate() {
    companion object {
        const val OPTION_ID = "pc_fleet_interaction_dialog_sp_option"
    }

    override fun getRequiredStoryPoints(): Int = 0

    override fun withSPInfo(): Boolean = false

    override fun createDescription(info: TooltipMakerAPI) {
        block()

        pc_FleetInteractionEveryFrame.hack = info
    }

    override fun getLogText(): String =
        "Why are we still here? Just to suffer? Every night, I can feel my leg... And my arm... even my fingers... The body I've lost... the comrades I've lost... won't stop hurting... It's like they're all still there. You feel it, too, don't you? I'm gonna make them give back our past!"
}

class pc_ReassignOfficerOptionDelegate(dialog: InteractionDialogAPI) : pc_AutoClosingOptionDelegate({
    val fleet = CampaignEngine.getInstance().playerFleet
    val validShips = playerFleet().fleetData.membersInPriorityOrder.filter {
        !it.isAutomated() && it.canBeDeployedForCombat() && !it.isFighterWing
    }
    val clazz =
        OfficerExpansionPlugin.classLoader.loadClass("com.price_of_command.fleet_interaction.ReassignOfficerReflector")
    val reassignOfficerReflector = ReflectionUtils.getConstructor(
        clazz,
        InteractionDialogAPI::class.java,
        CampaignFleet::class.java,
        List::class.java
    )!!.invokeWithArguments(dialog, fleet, validShips)
    ReflectionUtils.invoke("showPicker", reassignOfficerReflector)
})
