package com.price_of_command.fleet_interaction

import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.InteractionDialogPlugin
import com.fs.starfarer.api.campaign.OptionPanelAPI
import com.fs.starfarer.api.campaign.TextPanelAPI
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.combat.EngagementResultAPI
import com.fs.starfarer.api.fleet.FleetMemberStatusAPI
import com.price_of_command.ConditionManager
import com.price_of_command.conditions.Condition
import com.price_of_command.conditions.Outcome

class AfterActionReport(private val appliedConditions: Map<PersonAPI, ReportData>) : InteractionDialogPlugin {
    data class ReportData(val condition: Condition, val outcome: Outcome, val shipStatus: FleetMemberStatusAPI)

    private var dialog: InteractionDialogAPI? = null
    private var popMap: Map<PersonAPI, () -> Unit> = emptyMap()

    companion object {
        private const val GOTO_MAIN = "go_to_main"
    }

    override fun init(dialog: InteractionDialogAPI) {
        this.dialog = dialog
        dialog.promptText = "The report continues..."
        val textPanel = dialog.textPanel
        val optionPanel = dialog.optionPanel
        initTextPanel(textPanel)
        initOptionPanel(optionPanel)
        popMap = appliedConditions.map { (person, reportData) ->
            val (condition, outcome, shipStatus) = reportData

            val possessiveSuffix = if (person.nameString.last() != 's') {
                "'"
            } else {
                "'s"
            }
            val (contextString, sufferString) = when (shipStatus.hullFraction * 100f) {
                0f -> "suffered critical damage and required the crew to abandon ship" to "was seen manning the conn to the last moment."
                in 0f..10f -> "took massive damage and was nearly disabled" to "was at one point engulfed by a massive explosion that penetrated to the bridge"
                in 10f..25f -> "took significant damage" to "was hit by shrapnel from an internal compartment failure"
                in 25f..50f -> "was heavily damaged" to ""
                in 50f..75f -> "" to ""
                in 75f..99f -> "took an unlikely glancing blow that penetrated to the bridge" to ""
                else -> if (ConditionManager.rand.nextFloat() < 0.5) {
                    "suffered a freak high velocity micrometeorite impact" to ""
                } else {
                    "experienced a freak localized power surge to the captain's console" to ""
                }
            }
            val func = if (outcome is Outcome.Terminal) {
                {
                    textPanel.addPara("During the course of the battle, Officer ${person.nameString}$possessiveSuffix ship $contextString.")
                    textPanel.addPara("${person.nameString} $sufferString.")
                }
            } else {
                {

                }
            }
            condition.target to {
                textPanel.clear()
                optionPanel.clearOptions()

                func()

                optionPanel.addOption("Return to the rest of the report.", GOTO_MAIN)
            }
        }.toMap()
    }

    override fun optionSelected(optionText: String, optionData: Any?) {

    }

    override fun optionMousedOver(optionText: String, optionData: Any?) = Unit

    override fun advance(amount: Float) = Unit

    override fun backFromEngagement(battleResult: EngagementResultAPI) = Unit

    override fun getContext() = null

    override fun getMemoryMap() = null

    private fun initTextPanel(textPanel: TextPanelAPI) {
        textPanel.clear()
        textPanel.addPara("You review the after action report your second-in-command has prepared following your last deployment.")
        textPanel.addPara("")
    }

    private fun initOptionPanel(optionPanel: OptionPanelAPI) {
        optionPanel.clearOptions()
        for (target in appliedConditions.keys) {
            optionPanel.addOption(
                "You open the section on Officer ${target.nameString}",
                target.id + "open"
            )
        }
    }

    private fun initSection() {}
}