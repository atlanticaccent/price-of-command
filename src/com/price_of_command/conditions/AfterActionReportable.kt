package com.price_of_command.conditions

import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.OptionPanelAPI
import com.fs.starfarer.api.campaign.StoryPointActionDelegate
import com.fs.starfarer.api.campaign.TextPanelAPI
import com.fs.starfarer.api.fleet.FleetMemberStatusAPI
import com.fs.starfarer.api.impl.campaign.rulecmd.SetStoryOption.BaseOptionStoryPointActionDelegate
import com.fs.starfarer.api.impl.campaign.rulecmd.SetStoryOption.StoryOptionParams
import com.price_of_command.ConditionManager
import java.util.*

interface AfterActionReportable {
    /**
     * Constants defined here are always passed to the implementor and not handled by the AfterActionReport
     */
    companion object {
        const val AVOID_DEATH = "aa_report_avoid_death"
        const val AVOID_CONSEQUENCE = "aa_report_avoid_consequences"
    }

    /**
     * Called to populate an after action report page detailing this condition, and the options the player can take (if
     * any) against/to prevent the condition.
     *
     * @param outcome Provided as Conditions do not track their outcomes (may change in the future)
     * @param shipStatus Useful for determining if the Condition target's ship has taken damage, etc
     *
     * @return Boolean indicating whether to automatically populate the option panel with an option to allow the player
     * to back out without committing to a course of action
     */
    fun generateReport(
        dialog: InteractionDialogAPI,
        textPanel: TextPanelAPI,
        optionPanel: OptionPanelAPI,
        outcome: Outcome,
        shipStatus: FleetMemberStatusAPI,
        disabled: Boolean,
        destroyed: Boolean,
        delegate: (StoryOptionParams, () -> Unit) -> StoryPointActionDelegate = { options, func ->
            object : BaseOptionStoryPointActionDelegate(dialog, options) {
                override fun confirm() {
                    textPanel.clear()
                    optionPanel.clearOptions()
                    dialog.promptText = ""

                    func()
                }
            }
        }
    ): Boolean

    fun optionSelected(dialog: InteractionDialogAPI, optionText: String, optionData: Any) {
        if (optionData == AVOID_CONSEQUENCE) {
            if (this is ResolvableCondition) {
                this.expired = true
                this.resolveSilently = true
                this.tryResolve()
                ConditionManager.removeCondition(this)
            }
        }
    }

    /**
     * By default, if implementor is a Condition will return the past tense capitalised, or "Unknown"
     * @return A string indicating the "status" of an officer afflicted with this condition. IE: "Injured" or "KIA"
     */
    fun statusInReport(): String = (this as? Condition)?.pastTense()
        ?.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() } ?: "Unknown"
}

interface BaseAfterActionReportable : AfterActionReportable {
    override fun generateReport(
        dialog: InteractionDialogAPI,
        textPanel: TextPanelAPI,
        optionPanel: OptionPanelAPI,
        outcome: Outcome,
        shipStatus: FleetMemberStatusAPI,
        disabled: Boolean,
        destroyed: Boolean,
        delegate: (StoryOptionParams, () -> Unit) -> StoryPointActionDelegate
    ): Boolean = generateReport(dialog, textPanel, optionPanel, outcome, shipStatus, disabled, destroyed)

    fun generateReport(
        dialog: InteractionDialogAPI,
        textPanel: TextPanelAPI,
        optionPanel: OptionPanelAPI,
        outcome: Outcome,
        shipStatus: FleetMemberStatusAPI,
        disabled: Boolean,
        destroyed: Boolean,
    ): Boolean
}

typealias SPDelegateFactory = (StoryOptionParams, () -> Unit) -> StoryPointActionDelegate