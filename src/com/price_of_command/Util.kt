package com.price_of_command

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignClockAPI
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin
import com.fs.starfarer.api.campaign.rules.HasMemory
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.characters.OfficerDataAPI
import com.fs.starfarer.api.ui.UIComponentAPI
import com.fs.starfarer.api.ui.UIPanelAPI
import com.price_of_command.memory.EscapedMemory
import com.price_of_command.relfection.ReflectionUtils
import org.apache.log4j.Level
import org.apache.log4j.Logger

fun Any.logger(): Logger {
    return Global.getLogger(this::class.java).apply { level = Level.ALL }
}

fun playerFleet(): CampaignFleetAPI {
    return Global.getSector().playerFleet
}

fun playerOfficers(): MutableList<OfficerDataAPI> {
    return playerFleet().fleetData.officersCopy
}

fun Long.toClock(): CampaignClockAPI {
    return Global.getSector().clock.createClock(this)
}

fun Long.toDateString(): String {
    return this.toClock().dateString
}

fun HasMemory.escapedMemory(): EscapedMemory {
    return this.memory.escape()
}

fun HasMemory.escapedWithoutUpdate(): EscapedMemory {
    return this.memoryWithoutUpdate.escape()
}

fun MemoryAPI.escape(): EscapedMemory {
    return EscapedMemory(this)
}

fun Boolean.then(block: () -> Unit): Boolean {
    if (this) {
        block()
    }
    return this
}

fun <T> Boolean.andThenOrNull(block: () -> T?): T? {
    return if (this) {
        block()
    } else {
        null
    }
}

fun clock(): CampaignClockAPI = Global.getSector().clock

fun IntelInfoPlugin.addToManager(notify: Boolean = false) {
    Global.getSector().intelManager.addIntel(this, !notify)
}

fun UIComponentAPI.getParent(): UIPanelAPI {
    return ReflectionUtils.invoke("getParent", this) as UIPanelAPI
}
