package com.price_of_command

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.Global
import lunalib.lunaExtensions.showInteractionDialog

object ForceOpenNextFrame : EveryFrameScript {
    override fun isDone(): Boolean = false

    override fun runWhilePaused(): Boolean = true

    override fun advance(amount: Float) {
        ConditionManager.afterActionReport?.let {
            if (!Global.getSector().campaignUI.isShowingDialog) {
                playerFleet().showInteractionDialog(it)
                ConditionManager.afterActionReport = null
            }
        }
    }
}