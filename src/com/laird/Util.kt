package com.laird

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignClockAPI
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.rules.HasMemory
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI.SkillLevelAPI
import com.fs.starfarer.api.characters.OfficerDataAPI
import com.fs.starfarer.api.characters.PersonAPI
import com.laird.conditions.Injury
import com.laird.memory.EscapedMemory
import org.apache.log4j.Level
import org.apache.log4j.Logger
import kotlin.math.roundToInt

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

fun SkillLevelAPI.toInjury(officer: PersonAPI, startDate: Long, duration: Float): Injury {
    return Injury(officer, this.skill.id, this.level.roundToInt(), startDate, duration)
}

fun Boolean.then(block: () -> Unit): Boolean {
    if (this) {
        block()
    }
    return this
}

fun clock(): CampaignClockAPI = Global.getSector().clock
