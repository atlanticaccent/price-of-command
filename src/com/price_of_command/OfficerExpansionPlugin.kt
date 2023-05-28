package com.price_of_command

import com.fs.starfarer.api.BaseModPlugin
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.loading.SkillSpec
import com.price_of_command.conditions.Condition
import com.price_of_command.fleet_interaction.pc_FleetInteractionEveryFrame
import com.price_of_command.relfection.ReflectionUtils
import com.thoughtworks.xstream.XStream

class OfficerExpansionPlugin : BaseModPlugin() {
    companion object {
        const val modID = "officer_expansion"
    }

    override fun onGameLoad(newGame: Boolean) {
        super.onGameLoad(newGame)

        @Suppress("UNCHECKED_CAST") val savedInjuries =
            Global.getSector().memoryWithoutUpdate.escape()[ConditionManager.CONDITION_MAP] as? MutableMap<PersonAPI, MutableList<Condition>>
                ?: mutableMapOf()
        ConditionManager.conditionMap = savedInjuries

        Global.getSector().addTransientListener(pc_CampaignEventListener)
        Global.getSector().addTransientScript(ConditionManager.pc_ConditionManagerEveryFrame)
        Global.getSector().addTransientScript(pc_FleetInteractionEveryFrame)

        val settings = Global.getSettings()
        settings.skillIds.map { settings.getSkillSpec(it) }.filter { it.tags.contains("officer_expansion") }.forEach {
            ReflectionUtils.set("Ó00000", it as SkillSpec, "pc_condition")
        }
    }

    override fun beforeGameSave() {
        super.beforeGameSave()

        Global.getSector().memoryWithoutUpdate.escape()[ConditionManager.CONDITION_MAP] = ConditionManager.conditionMap
    }

    /**
     * Tell the XML serializer to use custom naming, so that moving or renaming classes doesn't break saves.
     */
    override fun configureXStream(x: XStream) {
        super.configureXStream(x)
    }
}
