package com.price_of_command.fleet_interaction

import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.InteractionDialogPlugin
import com.fs.starfarer.api.campaign.OptionPanelAPI
import com.fs.starfarer.api.campaign.TextPanelAPI
import com.fs.starfarer.api.combat.EngagementResultAPI
import com.fs.starfarer.api.fleet.FleetMemberStatusAPI
import com.price_of_command.andThenOrNull
import com.price_of_command.conditions.AfterActionReportable
import com.price_of_command.conditions.Condition
import com.price_of_command.conditions.Outcome

class AfterActionReport<T>(reportData: List<ReportData<T>>) :
    InteractionDialogPlugin where T : Condition, T : AfterActionReportable {
    private val dataHashes = reportData.map { it.hashCode() }

    data class ReportData<T>(
        val condition: T,
        val outcome: Outcome,
        val shipStatus: FleetMemberStatusAPI,
        val disabled: Boolean,
        val destroyed: Boolean
    ) where T : Condition, T : AfterActionReportable {
        fun generateReport(dialog: InteractionDialogAPI, textPanel: TextPanelAPI, optionPanel: OptionPanelAPI) =
            condition.generateReport(dialog, textPanel, optionPanel, outcome, shipStatus, disabled, destroyed)
    }

    private lateinit var dialog: InteractionDialogAPI
    private var popMap: Map<ReportData<T>, () -> Unit> = reportData.associateWith { reportData ->
        {
            val textPanel = dialog.textPanel
            val optionPanel = dialog.optionPanel
            textPanel.clear()
            optionPanel.clearOptions()

            val reconsider = reportData.generateReport(dialog, textPanel, optionPanel)
            if (reconsider) {
                optionPanel.addOption(
                    "Consider the rest of the report before drawing any permanent conclusions.",
                    GOTO_MAIN
                )
            }
        }
    }
    private var currentEntry: ReportData<T>? = null

    companion object {
        const val GOTO_MAIN = "aa_report_go_to_main"
        const val REMOVE_SELF = "aa_report_remove_self"
        const val FINISH_AND_CLOSE = "aa_report_finish"
    }

    override fun init(dialog: InteractionDialogAPI) {
        this.dialog = dialog
        dialog.promptText = "The report continues..."
        val textPanel = dialog.textPanel
        val optionPanel = dialog.optionPanel
        initTextPanel(textPanel)
        initOptionPanel(optionPanel)
    }

    override fun optionSelected(optionText: String, optionData: Any) {
        dialog.promptText = "The report continues..."
        when (optionData) {
            FINISH_AND_CLOSE -> dialog.dismiss()
            GOTO_MAIN -> resetDialog()
            REMOVE_SELF -> currentEntry?.let { entry ->
                popMap = popMap.filterKeys { it != entry }
                currentEntry = null
                resetDialog()
            }

            in dataHashes -> {
                popMap.firstNotNullOfOrNull { (it.key.hashCode() == optionData).andThenOrNull { it } }
                    ?.let { (entry, generateReport) ->
                        currentEntry = entry
                        generateReport()
                    }
            }

            else -> currentEntry?.condition?.optionSelected(dialog, optionText, optionData)
        }
    }

    private fun resetDialog() {
        initTextPanel(dialog.textPanel)
        initOptionPanel(dialog.optionPanel)
    }

    override fun optionMousedOver(optionText: String?, optionData: Any?) {
    }

    override fun advance(amount: Float) {
    }

    override fun backFromEngagement(battleResult: EngagementResultAPI) {
    }

    override fun getContext() = null

    override fun getMemoryMap() = null

    private fun initTextPanel(textPanel: TextPanelAPI) {
        textPanel.clear()
        textPanel.addPara("You review the after action report your second-in-command has prepared following your last deployment.")
    }

    private fun initOptionPanel(optionPanel: OptionPanelAPI) {
        optionPanel.clearOptions()
        if (popMap.isNotEmpty()) {
            for (data in popMap.keys) {
                optionPanel.addOption(
                    "You open the section on Officer ${data.condition.target.nameString}", data.hashCode()
                )
            }
        } else {
            optionPanel.addOption(
                "You finish reviewing the report and return to the rest of your duties.",
                FINISH_AND_CLOSE
            )
        }
    }
}