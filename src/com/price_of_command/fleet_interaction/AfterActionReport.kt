package com.price_of_command.fleet_interaction

import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.InteractionDialogPlugin
import com.fs.starfarer.api.campaign.OptionPanelAPI
import com.fs.starfarer.api.campaign.TextPanelAPI
import com.fs.starfarer.api.combat.EngagementResultAPI
import com.fs.starfarer.api.fleet.FleetMemberStatusAPI
import com.fs.starfarer.api.util.Misc
import com.price_of_command.andThenOrNull
import com.price_of_command.conditions
import com.price_of_command.conditions.AfterActionReportable
import com.price_of_command.conditions.Condition
import com.price_of_command.conditions.Outcome

class AfterActionReport<T>(private var undecided: List<ReportData<T>>) :
    InteractionDialogPlugin where T : Condition, T : AfterActionReportable {
    private val dataHashes = undecided.map { it.hashCode() }

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
    private var currentEntry: ReportData<T>? = null
    private var decided = mutableMapOf<T, Boolean>()

    companion object {
        const val GOTO_MAIN = "aa_report_go_to_main"
        const val REMOVE_SELF = "aa_report_remove_self"
        const val FINISH_AND_CLOSE = "aa_report_finish"
    }

    @Suppress("UNCHECKED_CAST")
    fun <K> mergeUndecided(other: List<ReportData<K>>): List<ReportData<K>> where K : Condition, K : AfterActionReportable {
        return (this.undecided as List<ReportData<K>>).plus(other)
    }

    override fun init(dialog: InteractionDialogAPI) {
        this.dialog = dialog
        dialog.promptText = "The report continues..."
        resetDialog()
    }

    override fun optionSelected(optionText: String, optionData: Any) {
        dialog.promptText = "The report continues..."
        dialog.hideVisualPanel()
        when (optionData) {
            FINISH_AND_CLOSE -> dialog.dismiss()
            GOTO_MAIN -> resetDialog()
            REMOVE_SELF -> currentEntry?.let { entry ->
                undecided = undecided.filter { it != entry }
                val kept = entry.condition.target.conditions().contains(entry.condition)
                decided[entry.condition] = kept
                currentEntry = null
                resetDialog()
            }

            in dataHashes -> {
                undecided.firstNotNullOfOrNull { (it.hashCode() == optionData).andThenOrNull { it } }
                    ?.let { entry ->
                        currentEntry = entry
                        val textPanel = dialog.textPanel
                        val optionPanel = dialog.optionPanel
                        textPanel.clear()
                        optionPanel.clearOptions()
                        dialog.showVisualPanel()
                        dialog.visualPanel.showPersonInfo(entry.condition.target, true, true)

                        val reconsider = entry.generateReport(dialog, textPanel, optionPanel)
                        if (reconsider) {
                            optionPanel.addOption(
                                "Consider the rest of the report before drawing any permanent conclusions.", GOTO_MAIN
                            )
                        }
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

        val tooltip = textPanel.beginTooltip()
        if (undecided.isNotEmpty() || decided.isNotEmpty()) {
            val width = (undecided.map { tooltip.computeStringWidth(it.condition.target.nameString) }
                .plus(decided.keys.map { tooltip.computeStringWidth(it.target.nameString) }).maxOrNull() ?: 200f) * 3f

            tooltip.beginGrid(width, 1)
            tooltip.setGridFont("orbitron20")
            tooltip.setGridLabelColor(Misc.getBrightPlayerColor())
            tooltip.addToGrid(0, 0, "Officer", "Status", Misc.getBrightPlayerColor())
            tooltip.setGridLabelColor(Misc.getTextColor())
            for ((i, entry) in undecided.iterator().withIndex()) {
                tooltip.addToGrid(0, i + 1, entry.condition.target.nameString, entry.condition.statusInReport())
            }
            for ((i, entry) in decided.entries.withIndex()) {
                val (condition, kept) = entry
                tooltip.addToGrid(
                    0, i + undecided.size + 1, condition.target.nameString, if (kept) {
                        condition.statusInReport()
                    } else {
                        "No New Conditions"
                    }
                )
            }
            tooltip.addGrid(8f)
        }

        textPanel.addTooltip()
    }

    private fun initOptionPanel(optionPanel: OptionPanelAPI) {
        optionPanel.clearOptions()
        if (undecided.isNotEmpty()) {
            for (data in undecided) {
                optionPanel.addOption(
                    "You open the section on Officer ${data.condition.target.nameString}", data.hashCode()
                )
            }
        } else {
            optionPanel.addOption(
                "You finish reviewing the report and return to the rest of your duties.", FINISH_AND_CLOSE
            )
        }
    }
}