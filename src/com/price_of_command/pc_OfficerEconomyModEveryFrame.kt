package com.price_of_command

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.ids.Stats
import org.magiclib.kotlin.getMarketsInLocation

object pc_OfficerEconomyModEveryFrame : EveryFrameScript {
    private var markets: List<MarketAPI> = emptyList()

    override fun advance(amount: Float) {
        val currentMarkets = Global.getSector().economy.starSystemsWithMarkets.flatMap { it.getMarketsInLocation() }
        if (markets != currentMarkets) {
            currentMarkets.forEach {
                it.stats.dynamic.getMod(
                    Stats.OFFICER_PROB_MOD
                ).modifyFlat(PoC_INCREASE_OFFICER_PROB_MULT, 0.4f)
            }
            markets = currentMarkets
        }
    }

    override fun isDone(): Boolean = false

    override fun runWhilePaused(): Boolean = true
}