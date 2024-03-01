package com.price_of_command.market_interaction

import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BaseBarEventWithPerson
import com.fs.starfarer.api.impl.campaign.intel.bar.events.historian.HistorianBarEvent
import com.fs.starfarer.api.util.Misc

class ScarTreatmentEvent : BaseBarEventWithPerson() {
    override fun shouldShowAtMarket(market: MarketAPI?): Boolean {
        regen(market)
        return Misc.random.nextFloat() > HistorianBarEvent.PROB_TO_SHOW && super.shouldShowAtMarket(market)
    }

    override fun getPersonRank(): String = "doctor"

    override fun getPersonPost(): String = ""
}