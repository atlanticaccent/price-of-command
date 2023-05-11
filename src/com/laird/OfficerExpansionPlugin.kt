package com.laird

import com.fs.starfarer.api.BaseModPlugin
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.characters.OfficerDataAPI
import com.laird.conditions.Condition
import com.thoughtworks.xstream.XStream

class OfficerExpansionPlugin : BaseModPlugin() {
    companion object {
        var officers: MutableList<OfficerDataAPI> = mutableListOf()
    }

    override fun onNewGameAfterTimePass() {
        super.onNewGameAfterTimePass()
    }

    override fun onGameLoad(newGame: Boolean) {
        super.onGameLoad(newGame)

        officers = Global.getSector().playerFleet.fleetData.officersCopy

        logger().debug("officers: $officers")

        @Suppress("UNCHECKED_CAST")
        val savedInjuries = Global.getSector().memoryWithoutUpdate.escape()[CONDITION_MAP] as? MutableMap<String, MutableList<Condition>> ?: mutableMapOf()
        ConditionManager.conditionMap.run {
            clear()
            putAll(savedInjuries)
        }

        Global.getSector().addTransientListener(oe_PostBattleListener)
        Global.getSector().addTransientScript(ConditionManager.oe_ConditionManagerEveryFrame)
    }

    override fun beforeGameSave() {
        super.beforeGameSave()

        Global.getSector().memoryWithoutUpdate.escape()[CONDITION_MAP] = ConditionManager.conditionMap
    }

    /**
     * Tell the XML serializer to use custom naming, so that moving or renaming classes doesn't break saves.
     */
    override fun configureXStream(x: XStream) {
        super.configureXStream(x)
    }
}