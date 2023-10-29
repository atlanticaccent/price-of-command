package com.price_of_command.fleet_interaction

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.OptionPanelAPI
import com.fs.starfarer.api.characters.OfficerDataAPI
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl
import com.fs.starfarer.api.ui.ButtonAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.price_of_command.*
import com.price_of_command.conditions.Injury
import com.price_of_command.platform.shared.ReflectionUtils

object pc_FleetInteractionEveryFrame : EveryFrameScript {
    private var fleetInteractionWasOpen = false
    var hack: TooltipMakerAPI? = null
        get() {
            val temp = field
            field = null
            return temp
        }

    private val vanillaIDs = listOf(
        FIDPIoption.LEAVE,
        FIDPIoption.ENGAGE,
        FIDPIoption.OPEN_COMM,
        FIDPIoption.PURSUE,
        FIDPIoption.CONTINUE_INTO_BATTLE,
        FIDPIoption.CONTINUE_ONGOING_BATTLE,
        FIDPIoption.INITIATE_BATTLE,
        FIDPIoption.ATTEMPT_TO_DISENGAGE,
        FIDPIoption.AUTORESOLVE_PURSUE,
        FIDPIoption.CRASH_MOTHBALL
    )

    private fun shouldAppendOption(optionPanel: OptionPanelAPI): Boolean {
        return !optionPanel.hasOption(pc_AutoClosingOptionDelegate.OPTION_ID) && vanillaIDs.any {
            optionPanel.hasOption(it)
        }
    }

    override fun isDone(): Boolean = false

    override fun runWhilePaused(): Boolean = true

    override fun advance(amount: Float) {
        rewriteLevelUpPicks(playerOfficers())

        hack?.let { story ->
            ReflectionUtils.invoke("getListener", story.getParent().getChildrenCopy().filterIsInstance<ButtonAPI>().first { it.text == "Cancel" })?.let { cancel ->
                ReflectionUtils.invoke("dismiss", cancel, 1)
            }
        }

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
            pc_CampaignEventListener.tryRestoreFleetAssignments()
        }
    }

    private fun rewriteLevelUpPicks(officers: List<OfficerDataAPI>) {
        for (officer in officers.filter { it.canLevelUp() }) {
            val blockedSkills = officer.person.conditions().filterIsInstance<Injury>()
            val repeatSkills = officer.skillPicks.count { blockedSkills.any { blocked -> blocked.skill == it } }

            if (repeatSkills > 0) {
                blockedSkills.forEach {
                    it.level?.toFloat()?.let { level -> officer.person.stats.setSkillLevel(it.skill, level) }
                }
                officer.makeSkillPicks()
                blockedSkills.forEach {
                    officer.person.stats.setSkillLevel(it.skill, 0f)
                }
            }
        }
    }
}

typealias FIDPIoption = FleetInteractionDialogPluginImpl.OptionId