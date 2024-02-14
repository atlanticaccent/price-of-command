package com.price_of_command

import com.fs.starfarer.api.BaseModPlugin
import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.SettingsAPI
import com.fs.starfarer.api.campaign.CampaignEventListener
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BarEventManager
import com.fs.starfarer.loading.SkillSpec
import com.price_of_command.conditions.LastingCondition
import com.price_of_command.conditions.PostBattleListener
import com.price_of_command.conditions.overrides.ConditionGate
import com.price_of_command.conditions.overrides.ConditionMutator
import com.price_of_command.fleet_interaction.pc_FleetInteractionEveryFrame
import com.price_of_command.reflection.ReflectionUtils
import com.price_of_command.reflection.ReflectionLoader
import com.thoughtworks.xstream.XStream
import lunalib.lunaSettings.LunaSettings
import org.json.JSONObject
import org.magiclib.kotlin.map
import java.net.URL
import java.nio.file.Paths

class OfficerExpansionPlugin : BaseModPlugin() {
    companion object {
        internal var vanillaSkills = emptyList<String>()
        internal var modSkillWhitelist = emptyList<String>()
        internal var aptitudeFieldName: String? = null
        internal val classLoader: ReflectionLoader by lazy {
            val url: URL = try {
                OfficerExpansionPlugin::class.java.getProtectionDomain().codeSource.location
            } catch (e: SecurityException) {
                try {
                    Paths.get("../mods/**/price_of_command.jar").toUri().toURL()
                } catch (ex: Exception) {
                    logger().error("Could not convert jar path to URL; exiting", ex)
                    throw Exception("Could not build custom classloader")
                }
            }

            ReflectionLoader(url, OfficerExpansionPlugin::class.java.getClassLoader())
        }
    }

    override fun onApplicationLoad() {
        val settings = Global.getSettings()
        overrideAptitudes(settings)
        loadCSVs(settings)
    }

    @Suppress("UNCHECKED_CAST")
    override fun onGameLoad(newGame: Boolean) {
        super.onGameLoad(newGame)

        val memory = Global.getSector().memoryWithoutUpdate.escape()
        ConditionManager.conditionMap =
            memory[ConditionManager.CONDITION_MAP] as? Map<PersonAPI, List<LastingCondition>> ?: mapOf()
        ConditionManager.preconditions = memory[ConditionManager.PRECONDITIONS] as? List<ConditionGate> ?: listOf()
        ConditionManager.mutators = memory[ConditionManager.MUTATORS] as? List<ConditionMutator> ?: listOf()
        ConditionManager.postBattleListeners =
            memory[ConditionManager.POST_BATTLE_LISTENERS] as? List<PostBattleListener> ?: listOf()

        Global.getSector().run {
            addTransientListener(pc_CampaignEventListener)
            addTransientScript(ConditionManager.pc_ConditionManagerEveryFrame)
            addTransientScript(pc_FleetInteractionEveryFrame)
            addTransientScript(ForceOpenNextFrame)
            listenerManager.addListener(pc_CampaignEventListener, true)
        }

        val baseOfficers = Global.getSector().playerStats.officerNumber.modifiedValue
        val increasedBase = LunaSettings.getInt(modID, "minimum_roster_size") ?: 12
        if (baseOfficers < increasedBase) {
            Global.getSector().playerStats.officerNumber.modifyFlat(
                "poc_increase_base_officers", increasedBase - baseOfficers
            )
        }

        val barManager = BarEventManager.getInstance()
    }

    override fun beforeGameSave() {
        super.beforeGameSave()

        val memory = Global.getSector().memoryWithoutUpdate.escape()
        memory[ConditionManager.CONDITION_MAP] = ConditionManager.conditionMap
        memory[ConditionManager.PRECONDITIONS] = ConditionManager.preconditions
        memory[ConditionManager.MUTATORS] = ConditionManager.mutators
        memory[ConditionManager.POST_BATTLE_LISTENERS] = ConditionManager.postBattleListeners
    }

    /**
     * Tell the XML serializer to use custom naming, so that moving or renaming classes doesn't break saves.
     */
    override fun configureXStream(x: XStream) {
        super.configureXStream(x)
    }

    override fun onDevModeF8Reload() {
        this.onApplicationLoad()
    }

    private fun overrideAptitudes(settings: SettingsAPI) {
        settings.skillIds.map { settings.getSkillSpec(it) }
            .filter { it.tags.any { tag -> tag in setOf(PoC_TRAIT_TAG, PoC_CONDITION_TAG) } }.forEach {
                if (aptitudeFieldName == null) {
                    val fields = ReflectionUtils.getFieldsOfType(it as SkillSpec, String::class.java)
                    aptitudeFieldName =
                        fields.firstOrNull { fieldName -> ReflectionUtils.get(fieldName, it) == it.governingAptitudeId }
                }
                aptitudeFieldName?.run { ReflectionUtils.set(this, it, "pc_condition") }
            }
    }

    private fun loadCSVs(settings: SettingsAPI) {
        vanillaSkills = settings.loadCSV("data/characters/skills/skill_data.csv", false).map { it: JSONObject ->
            it.getString("id")
        }
        modSkillWhitelist =
            settings.loadCSV("data/characters/skills/poc_skill_whitelist.csv", true).map { it: JSONObject ->
                it.getString("id")
            }
    }
}
