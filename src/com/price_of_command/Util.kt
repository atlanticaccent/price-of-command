@file:Suppress("unused")

package com.price_of_command

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin
import com.fs.starfarer.api.campaign.CampaignClockAPI
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin
import com.fs.starfarer.api.campaign.rules.HasMemory
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.characters.OfficerDataAPI
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.UIComponentAPI
import com.fs.starfarer.api.ui.UIPanelAPI
import com.price_of_command.memory.EscapedMemory
import com.price_of_command.relfection.ReflectionUtils
import lunalib.lunaUI.elements.LunaSpriteElement
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

inline fun <T> Boolean.andThenOrNull(block: () -> T?): T? {
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

fun LunaSpriteElement.constrainWithRatio(constraint: Float): LunaSpriteElement {
    return this.constrainWithRatio(constraint, constraint)
}

fun LunaSpriteElement.constrainWithRatio(constrainX: Float, constrainY: Float): LunaSpriteElement {
    val sprite = getSprite()
    if (sprite.width > constrainX || sprite.height > constrainY) {
        if (sprite.width > sprite.height) {
            sprite.height = (sprite.height / sprite.width) * constrainX
            sprite.width = constrainX
        } else {
            sprite.width = (sprite.width / sprite.height) * constrainY
            sprite.height = constrainY
        }
    }
    return this
}

fun createCustom(width: Float, height: Float, plugin: CustomUIPanelPlugin): CustomPanelAPI =
    Global.getSettings().createCustom(width, height, plugin)

fun createCustom(width: Float, height: Float) = createCustom(width, height, BaseCustomUIPanelPlugin())

fun UIComponentAPI.setOpacity(value: Float) {
    ReflectionUtils.invoke("setOpacity", this, value)
}

fun List<OfficerDataAPI>.containsPerson(person: PersonAPI): Boolean = this.find { it.person == person } != null
