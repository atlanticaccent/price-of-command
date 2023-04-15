package com.laird

import com.fs.starfarer.api.BaseModPlugin
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.characters.OfficerDataAPI
import com.thoughtworks.xstream.XStream
import org.apache.log4j.Level

class OfficerExpansionPlugin : BaseModPlugin() {
    companion object {
        val logger = Global.getLogger(Companion::class.java).apply { level = Level.ALL }

        var officers: MutableList<OfficerDataAPI> = mutableListOf()
    }

    override fun onNewGameAfterTimePass() {
        super.onNewGameAfterTimePass()
    }

    override fun onGameLoad(newGame: Boolean) {
        super.onGameLoad(newGame)

        officers = Global.getSector().playerFleet.fleetData.officersCopy

        logger.debug("officers: $officers")

        Global.getSector().addTransientListener(PostBattleListener())

        // Showing that we can compile and run Java files as well.
        // This does nothing and can be deleted.
        if (Global.getSettings().isDevMode) {
            Global.getSector().addTransientScript(ExampleEveryFrameScript())
        }
    }

    override fun beforeGameSave() {
        super.beforeGameSave()
    }

    /**
     * Tell the XML serializer to use custom naming, so that moving or renaming classes doesn't break saves.
     */
    override fun configureXStream(x: XStream) {
        super.configureXStream(x)
    }
}