package com.price_of_command

import com.fs.starfarer.api.BaseModPlugin
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.loading.SkillSpec
import com.price_of_command.conditions.LastingCondition
import com.price_of_command.conditions.overrides.ConditionGate
import com.price_of_command.conditions.overrides.ConditionMutator
import com.price_of_command.fleet_interaction.pc_FleetInteractionEveryFrame
import com.price_of_command.relfection.ReflectionUtils
import com.thoughtworks.xstream.XStream
import org.json.JSONObject
import org.magiclib.kotlin.map

class OfficerExpansionPlugin : BaseModPlugin() {
    companion object {
        const val modID = "price_of_command"
        const val PoC_SKILL_WHITELIST_TAG = "pc_skill_opt_in"
        const val PoC_OFFICER_DEAD = "pc_dead"

        var vanillaSkills = emptyList<String>()
        var modSkillWhitelist = emptyList<String>()
    }

    override fun onApplicationLoad() {
        val settings = Global.getSettings()
        settings.skillIds.map { settings.getSkillSpec(it) }
            .filter { it.tags.any { tag -> setOf("pc_quirk", "pc_condition").contains(tag) } }.forEach {
                ReflectionUtils.set("Ó00000", it as SkillSpec, "pc_condition")
            }

        vanillaSkills = settings.loadCSV("data/characters/skills/skill_data.csv", false).map { it: JSONObject ->
            it.getString("id")
        }
        modSkillWhitelist =
            settings.loadCSV("data/characters/skills/poc_skill_whitelist.csv", true).map { it: JSONObject ->
                it.getString("id")
            }
    }

    @Suppress("UNCHECKED_CAST")
    override fun onGameLoad(newGame: Boolean) {
        super.onGameLoad(newGame)

        val memory = Global.getSector().memoryWithoutUpdate.escape()
        ConditionManager.conditionMap =
            memory[ConditionManager.CONDITION_MAP] as? Map<PersonAPI, List<LastingCondition>> ?: mapOf()
        ConditionManager.preconditions = memory[ConditionManager.PRECONDITIONS] as? List<ConditionGate> ?: listOf()
        ConditionManager.mutators = memory[ConditionManager.MUTATORS] as? List<ConditionMutator> ?: listOf()

        Global.getSector().addTransientListener(pc_CampaignEventListener)
        Global.getSector().addTransientScript(ConditionManager.pc_ConditionManagerEveryFrame)
        Global.getSector().addTransientScript(pc_FleetInteractionEveryFrame)
        Global.getSector().addTransientScript(pc_OfficerEconomyModEveryFrame)
    }

    override fun beforeGameSave() {
        super.beforeGameSave()

        val memory = Global.getSector().memoryWithoutUpdate.escape()
        memory[ConditionManager.CONDITION_MAP] = ConditionManager.conditionMap
        memory[ConditionManager.PRECONDITIONS] = ConditionManager.preconditions
        memory[ConditionManager.MUTATORS] = ConditionManager.mutators
    }

    /**
     * Tell the XML serializer to use custom naming, so that moving or renaming classes doesn't break saves.
     */
    override fun configureXStream(x: XStream) {
        super.configureXStream(x)
    }
}
