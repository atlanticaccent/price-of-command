package com.officer_expansion.fleet_interaction

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.BaseCustomDialogDelegate
import com.fs.starfarer.api.campaign.CustomDialogDelegate
import com.fs.starfarer.api.campaign.FleetMemberPickerListener
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.util.Misc
import com.officer_expansion.logger
import com.officer_expansion.playerFleet
import com.officer_expansion.playerOfficers
import lunalib.lunaExtensions.addLunaElement
import lunalib.lunaExtensions.addLunaSpriteElement
import lunalib.lunaUI.elements.LunaSpriteElement
import org.magiclib.kotlin.isAutomated

private const val pad = 2f
private const val opad = 10f

private const val rowHeight = 96f

class oe_ReassignOfficerCustomPanel private constructor(private val originalOfficerAssignments: Map<FleetMemberAPI, PersonAPI>) :
    BaseCustomDialogDelegate() {
    companion object {
        fun fleetAssignments(): Map<FleetMemberAPI, PersonAPI> {
            return playerFleet().fleetData.membersListCopy.mapNotNull { it.captain?.run { it to this } }.toMap()
        }
    }

    constructor() : this(fleetAssignments())

    override fun createCustomDialog(panel: CustomPanelAPI, callback: CustomDialogDelegate.CustomDialogCallback) {
        val info = panel.createUIElement(800f, 500f, true)
        panel.addUIElement(info).inTL(0f, 0f)
        panel.bringComponentToTop(info)

        val nameWidth = playerOfficers().maxOf { info.computeStringWidth(it.person.nameString) } + (2 * opad)
        for (officer in playerOfficers()) {
            val ele = info.addLunaElement(780f, rowHeight)
            ele.enableTransparency = true

            val portrait = ele.innerElement.addLunaSpriteElement(
                officer.person.portraitSprite, LunaSpriteElement.ScalingTypes.NONE, rowHeight, rowHeight
            ).constrainWithRatio(rowHeight)

            val nameSkill = ele.innerElement.addLunaElement(nameWidth, 12f)
            ele.innerElement.addCustomDoNotSetPosition(nameSkill.elementPanel).position.rightOfTop(
                portrait.elementPanel, 0f
            )
            nameSkill.innerElement.addSpacer(8f)
            nameSkill.addText(officer.person.nameString)
            nameSkill.innerElement.addSkillPanel(officer.person, pad)
            nameSkill.renderBackground = false
            nameSkill.renderBorder = false
            nameSkill.enableTransparency = true

            ele.onClick {
                logger().debug("${officer.person.nameString} clicked")
                val oldShip = playerFleet().fleetData.getMemberWithCaptain(officer.person)
                Global.getSector().campaignUI.currentInteractionDialog.showFleetMemberPickerDialog("Select ship for this officer to captain:",
                    "Ok",
                    "Cancel",
                    3,
                    7,
                    58f,
                    false,
                    false,
                    playerFleet().fleetData.membersInPriorityOrder.filter {
                        !it.isAutomated() && it.canBeDeployedForCombat() && !it.isFighterWing && (!it.isFlagship || oldShip != null)
                    },
                    object : FleetMemberPickerListener {
                        override fun pickedFleetMembers(members: List<FleetMemberAPI>) {
                            if (members.isNotEmpty()) {
                                val picked = members.first()
                                if (oldShip != null) {
                                    if (picked.isFlagship) {
                                        picked.isFlagship = false
                                        oldShip.isFlagship = true
                                    }
                                    if (picked.captain != null) {
                                        val displacedCaptain = picked.captain
                                        if (displacedCaptain != null) {
                                            oldShip.captain = displacedCaptain
                                        }
                                    } else {
                                        oldShip.captain = null
                                    }
                                }
                                picked.captain = officer.person
                                logger().debug("Picked ${picked.shipName}")
                            }
                        }

                        override fun cancelledFleetMemberPicking() {
                            logger().debug("Pick cancelled")
                        }
                    })
            }
            ele.onHoverEnter { ele.backgroundColor = Misc.getDarkPlayerColor().brighter() }
            ele.onHoverExit { ele.backgroundColor = Misc.getDarkPlayerColor().darker() }
        }

    }

    override fun hasCancelButton(): Boolean = true

    override fun customDialogConfirm() {
        logger().debug("customDialogConfirm() called")
    }

    override fun customDialogCancel() {
        logger().debug("Cancelled, reverting officer assignments")
        originalOfficerAssignments.forEach { (ship, captain) ->
            ship.captain = captain
        }
    }
}

fun LunaSpriteElement.constrainWithRatio(constraint: Float): LunaSpriteElement {
    return this.constrainWithRatio(constraint, constraint)
}

fun LunaSpriteElement.constrainWithRatio(constrainX: Float, constrainY: Float): LunaSpriteElement {
    val sprite = getSprite()
    if (sprite.width > constrainX || sprite.height > constrainY) {
        if (sprite.width > sprite.height) {
            sprite.height = (sprite.height / sprite.width) * constrainX
            sprite.width = constrainX
        } else {
            sprite.width = (sprite.width / sprite.height) * constrainY
            sprite.height = constrainY
        }
    }
    return this
}