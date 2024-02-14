@file:Suppress("unused")

package com.price_of_command.fleet_interaction

import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin
import com.fs.starfarer.api.campaign.FleetMemberPickerListener
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.ui.ButtonAPI
import com.fs.starfarer.api.ui.UIPanelAPI
import com.fs.starfarer.campaign.fleet.CampaignFleet
import com.price_of_command.*
import com.price_of_command.fleet_interaction.ship_picker.ShipPickHandler
import com.price_of_command.reflection.ReflectionUtils

private const val opad = 10f

class ReassignOfficerReflector(
    private val dialog: InteractionDialogAPI,
    private val campaignFleet: CampaignFleet,
    private val validShips: List<FleetMemberAPI>,
) {
    private var checkbox: ButtonAPI? = null

    fun showPicker() {
        dialog.showFleetMemberPickerDialog(
            null,
            "Ok",
            "Cancel",
            3,
            7,
            116f,
            false,
            false,
            validShips,
            object : FleetMemberPickerListener {
                override fun pickedFleetMembers(members: List<FleetMemberAPI>) {
                    setFleetAssignmentRestore()
                }

                override fun cancelledFleetMemberPicking() {
                    logger().debug("Pick cancelled")
                    pc_CampaignEventListener.tryRestoreFleetAssignments(true)
                    setFleetAssignmentRestore()
                }
            })
        try {
            val pickRoot =
                (dialog as UIPanelAPI).getChildrenCopy().filterIsInstance<UIPanelAPI>().last().getChildrenCopy().first()
            val pickPanel = ReflectionUtils.invoke("getCurr", pickRoot) as UIPanelAPI
            val checkPanel = settings().createCustom(100f, 30f, object : BaseCustomUIPanelPlugin() {
                override fun buttonPressed(buttonId: Any) {
                    (buttonId as? String)?.let {
                        if (it == "pc_restore_fleet_assignments") {
                            pc_CampaignEventListener.restoreFleetAssignments = checkbox?.isChecked ?: false
                        }
                    }
                }
            })
            val checkTooltip = checkPanel.createUIElement(100f, 30f, false)
            val checkboxString = "Restore fleet assignments after battle or leaving peacefully"
            val checkboxWidth = checkTooltip.computeStringWidth(checkboxString)
            checkbox = checkTooltip.addCheckbox(
                checkboxWidth, 16f, checkboxString, "pc_restore_fleet_assignments", ButtonAPI.UICheckboxSize.SMALL, opad
            ).apply {
                isChecked = pc_CampaignEventListener.restoreFleetAssignments
            }
            checkPanel.addUIElement(checkTooltip)
            pickPanel.addComponent(checkPanel).inBL(8f, 10f)

            val button = pickPanel.getChildrenCopy().filterIsInstance<ButtonAPI>().first()
            val inner_listener = ReflectionUtils.invoke("getListener", button)!!

            val ships =
                pickPanel.getChildrenCopy()[4].getChildrenCopy()[0].getChildrenCopy()[0].getChildrenCopy()[0].getChildrenCopy()
            val target_listener = ReflectionUtils.invoke("getListener", ships.first())!!

            val proxy = ShipPickHandler(inner_listener, dialog, campaignFleet) { _, _ ->
                showPicker()
            }.build(inner_listener.javaClass.interfaces)

            for (clazz in inner_listener.javaClass.interfaces) {
                val fields = ReflectionUtils.getFieldsOfType(target_listener, clazz)
                if (fields.size == 1) {
                    ReflectionUtils.set(fields[0], target_listener, clazz.cast(proxy))
                }
            }
        } catch (e: Exception) {
            logger().debug(e)
        }
    }

    private fun setFleetAssignmentRestore() {
        pc_CampaignEventListener.restoreFleetAssignments = checkbox?.isChecked ?: true
    }
}
