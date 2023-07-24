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
import com.fs.starfarer.coreui.CaptainPickerDialog
import com.fs.starfarer.ui.interfacenew
import com.price_of_command.*
import com.price_of_command.relfection.ReflectionUtils
import java.awt.Robot

private const val pad = 2f
private const val opad = 10f

private const val rowHeight = 96f
private const val width = 780f

class pc_ReassignOfficerCustomPanel private constructor(
    private val dialog: InteractionDialogAPI,
    private val campaignFleet: CampaignFleet,
    private val validShips: List<FleetMemberAPI>,
    private val originalOfficerAssignments: Map<FleetMemberAPI, PersonAPI>
) {
    companion object {
        fun fleetAssignments(): Map<FleetMemberAPI, PersonAPI> {
            return playerFleet().fleetData.membersListCopy.mapNotNull { it.captain?.run { it to this } }.toMap()
        }
    }

    private var checkbox: ButtonAPI? = null

    constructor(dialog: InteractionDialogAPI, campaignFleet: CampaignFleet, validShips: List<FleetMemberAPI>) : this(
        dialog, campaignFleet, validShips, fleetAssignments()
    )

    internal fun showPicker() {
        val robot = Robot()
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
                override fun pickedFleetMembers(members: List<FleetMemberAPI>) {
                    if (members.isNotEmpty()) {
                        val picked = members.first() as FleetMember
                        val picker = object : CaptainPickerDialog(campaignFleet, picked, dialog as interfacenew, null) {
                            override fun actionPerformed(p0: Any?, p1: Any?) {
                                super.actionPerformed(p0, p1)
                                if (this.isBeingDismissed) {
                                    showPicker()
                                }
                            }
                        }
                        picker.show(0f, 0f)
                        logger().debug("Picked ${picked.shipName}")
                    } else {
                        setFleetAssignmentRestore()
                    }
                }

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

            val system = System.getProperty("os.name")
            when {
                system.contains("windows", ignoreCase = true) -> {
                    val ships =
                        pickPanel.getChildrenCopy()[4].getChildrenCopy()[0].getChildrenCopy()[0].getChildrenCopy()[0].getChildrenCopy()
                    ships.forEach {
                        val oldListener = ReflectionUtils.invoke("getListener", it)!!

                        val script =
                            settings().scriptClassLoader.loadClass("data.scripts.ship_picker_listener.windows.Listener")
                        val listener = ReflectionUtils.instantiate(script, oldListener)!!
                        ReflectionUtils.invoke("attach", listener, it)
                    }
                }
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
