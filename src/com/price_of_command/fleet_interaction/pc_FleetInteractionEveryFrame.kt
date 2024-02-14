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
import com.price_of_command.reflection.ReflectionUtils
import org.lwjgl.input.Keyboard

@Suppress("UNCHECKED_CAST")
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
            ReflectionUtils.invoke("getListener",
                story.getParent().getChildrenCopy().filterIsInstance<ButtonAPI>().first { it.text == "Cancel" })
                ?.let { cancel ->
                    ReflectionUtils.invoke("dismiss", cancel, 1)
                }
        }

        val dialog = Global.getSector().campaignUI.currentInteractionDialog
        if (dialog != null && dialog.plugin is FleetInteractionDialogPluginImpl && shouldAppendOption(dialog.optionPanel)) {
            val options = dialog.optionPanel

            options.addOption(
                "Reassign captains",
                pc_AutoClosingOptionDelegate.OPTION_ID,
                "Last minute reassignment of captains to ships"
            )

            val originalOptions = options.savedOptionList

            val newOptions = originalOptions.toMutableList()
            val added = newOptions.removeLast()
            newOptions.add(1, added)

            val oldOptionMap = (ReflectionUtils.invoke(
                "getButtonToItemMap", options
            ) as Map<Any?, Any?>).toMap()

            options.restoreSavedOptions(newOptions)

            val idMethodName = ReflectionUtils.getMethodOfReturnType(originalOptions.first()!!, "".javaClass)!!
            fun Any?.id() = ReflectionUtils.invoke(idMethodName, this!!)
            val originalMap = originalOptions.associateBy { it.id() }
            val optionMap = ReflectionUtils.invoke("getButtonToItemMap", options) as MutableMap<Any?, Any?>
            for (key in optionMap.keys) {
                val item = optionMap[key]!!
                val id = item.id()
                val newItem = originalMap[id]
                optionMap[key] = newItem

                val oldKey = oldOptionMap.entries.first { (_, entry) -> entry.id() == id }.key!!

                val firstArgClassOfAltShortcut = ReflectionUtils.getMethodArguments("setAltShortcut", oldKey)!![0]
                val optionHandlingScriptField = ReflectionUtils.findFieldWithMethodName(oldKey, "focusLost")!!
                val optionHandlingScriptObj = optionHandlingScriptField.get(oldKey)!!
                val keyHandlerFields =
                    ReflectionUtils.findFieldsOfType(optionHandlingScriptObj, firstArgClassOfAltShortcut)
                keyHandlerFields.mapNotNull { it.get(optionHandlingScriptObj) }.forEach { keyHandlerObj ->
                        val keyField = ReflectionUtils.findFieldsOfType(keyHandlerObj, Int::class.java)[0]
                        val actualKey = keyField.get(keyHandlerObj) as Int
                        if (!(Keyboard.KEY_1..Keyboard.KEY_9).contains(actualKey)) {
                            ReflectionUtils.invoke("setAltShortcut", key!!, keyHandlerObj, true)
                        }
                    }
            }

            options.addOptionConfirmation(
                pc_AutoClosingOptionDelegate.OPTION_ID, pc_ReassignOfficerOptionDelegate(dialog)
            )

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