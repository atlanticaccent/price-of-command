package com.price_of_command.fleet_interaction

import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin
import com.fs.starfarer.api.campaign.FleetMemberPickerListener
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.ui.ButtonAPI
import com.fs.starfarer.api.ui.UIPanelAPI
import com.fs.starfarer.campaign.fleet.CampaignFleet
import com.fs.starfarer.campaign.fleet.FleetMember
import com.price_of_command.*
import com.price_of_command.platform.reassign
import com.price_of_command.platform.shared.ShipPickerWrapper
import com.price_of_command.platform.shared.ReflectionUtils

private const val opad = 10f

class ReassignOfficerReflector private constructor(
    private val dialog: InteractionDialogAPI,
    private val campaignFleet: CampaignFleet,
    private val validShips: List<FleetMemberAPI>,
    private val originalOfficerAssignments: Map<FleetMemberAPI, PersonAPI>
) {
    companion object {
        @JvmStatic
        fun fleetAssignments(): Map<FleetMemberAPI, PersonAPI> {
            return playerFleet().fleetData.membersListCopy.mapNotNull { it.captain?.run { it to this } }.toMap()
        }
    }

    private var checkbox: ButtonAPI? = null

    constructor(dialog: InteractionDialogAPI, campaignFleet: CampaignFleet, validShips: List<FleetMemberAPI>) : this(
        dialog, campaignFleet, validShips, fleetAssignments()
    )

    internal fun showPicker() {
        dialog.showFleetMemberPickerDialog(null,
            "Ok",
            "Cancel",
            3,
            7,
            116f,
            false,
            false,
            validShips,
            object : FleetMemberPickerListener {
                override fun pickedFleetMembers(members: List<FleetMemberAPI>) = Unit

                override fun cancelledFleetMemberPicking() {
                    logger().debug("Pick cancelled")
                    playerFleet().fleetData.membersInPriorityOrder.forEach {
                        it.captain = originalOfficerAssignments[it]
                        if (it.captain.isPlayer) {
                            it.isFlagship = true
                        }
                    }
                    setFleetAssignmentRestore()
                }
            })
        try {
            val pickRoot =
                (dialog as UIPanelAPI).getChildrenCopy().filterIsInstance<UIPanelAPI>().last().getChildrenCopy().first()
            val pickPanel = ReflectionUtils.invoke("getCurr", pickRoot) as UIPanelAPI
            val checkPanel = settings().createCustom(100f, 30f, BaseCustomUIPanelPlugin())
            val checkTooltip = checkPanel.createUIElement(100f, 30f, false)
            val checkboxString = "Restore fleet assignments after battle or closing dialog"
            val checkboxWidth = checkTooltip.computeStringWidth(checkboxString)
            checkbox = checkTooltip.addCheckbox(
                checkboxWidth, 16f, checkboxString, "pc_restore_fleet_assignments", ButtonAPI.UICheckboxSize.SMALL, opad
            ).apply {
                isChecked = pc_CampaignEventListener.restoreFleetAssignments
            }
            checkPanel.addUIElement(checkTooltip)
            pickPanel.addComponent(checkPanel).inBL(8f, 10f)

            val buttons = pickPanel.getChildrenCopy().filterIsInstance<ButtonAPI>()
            buttons.first { it.text.lowercase().contains("cancel") }.setOpacity(0f)
            val inner_listener = ReflectionUtils.invoke("getListener", buttons[0])!!

            val ships =
                pickPanel.getChildrenCopy()[4].getChildrenCopy()[0].getChildrenCopy()[0].getChildrenCopy()[0].getChildrenCopy()
            val target_listener = ReflectionUtils.invoke("getListener", ships.first())!!

            ShipPickerWrapper.reassign(
                target_listener,
                inner_listener,
                dialog,
                campaignFleet
            ) { _, _ ->
                showPicker()
            }
        } catch (e: Exception) {
            logger().debug(e)
        }
    }

    private fun setFleetAssignmentRestore() {
        pc_CampaignEventListener.restoreFleetAssignments = checkbox?.isChecked ?: true
        if (pc_CampaignEventListener.restoreFleetAssignments) {
            pc_CampaignEventListener.fleetAssignment = originalOfficerAssignments
        }
    }
}
