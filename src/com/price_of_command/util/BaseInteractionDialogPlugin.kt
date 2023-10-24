package com.price_of_command.util

import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.InteractionDialogPlugin
import com.fs.starfarer.api.combat.EngagementResultAPI

class BaseInteractionDialogPlugin(private val init: (InteractionDialogAPI) -> Unit) : InteractionDialogPlugin {
    private var onOptionSelected: (BaseInteractionDialogPlugin.(String, Any?) -> Unit)? = null
    lateinit var dialog: InteractionDialogAPI

    override fun init(dialog: InteractionDialogAPI) {
        this.dialog = dialog
        init.invoke(dialog)
    }

    override fun optionSelected(optionText: String, optionData: Any?) {
        onOptionSelected?.invoke(this, optionText, optionData)
    }

    override fun optionMousedOver(optionText: String?, optionData: Any?) {}

    override fun advance(amount: Float) {}

    override fun backFromEngagement(battleResult: EngagementResultAPI?) {}

    override fun getContext() = null

    override fun getMemoryMap() = null

    fun withOnOptionSelected(block: BaseInteractionDialogPlugin.(String, Any?) -> Unit): BaseInteractionDialogPlugin {
        onOptionSelected = block
        return this
    }
}