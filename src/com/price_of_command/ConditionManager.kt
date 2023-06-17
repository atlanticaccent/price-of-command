package com.price_of_command

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CoreUITabId
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI
import com.fs.starfarer.api.characters.PersonAPI
import com.price_of_command.conditions.Condition
import com.price_of_command.conditions.Death
import com.price_of_command.conditions.ResolvableCondition
import com.price_of_command.conditions.overrides.ConditionGate
import com.price_of_command.conditions.overrides.ConditionMutator
import com.price_of_command.memorial.DeathMessage
import com.price_of_command.memorial.MemorialWall
import com.price_of_command.util.BaseInteractionDialogPlugin
import lunalib.lunaExtensions.showInteractionDialog
import kotlin.random.Random

object ConditionManager : OverrideManager {
    const val CONDITION_MAP = "pc_persistent_condition_map"
    const val PRECONDITIONS = "pc_persistent_preconditions"
    const val MUTATORS = "pc_persistent_mutators"

    val now: Long
        get() = Global.getSector().clock.timestamp
    val rand: Random by lazy { Random(now) }

    internal var conditionMap: Map<PersonAPI, List<Condition>> = emptyMap()
    override var preconditions: List<ConditionGate> = listOf()
    override var mutators: List<ConditionMutator> = listOf()

    private var showMemorialWall: Boolean = false

    fun findByStats(stats: MutableCharacterStatsAPI): Pair<PersonAPI, List<Condition>>? =
        conditionMap.entries.find { (person, _) ->
            person.stats.equals(stats)
        }?.toPair()

    object pc_ConditionManagerEveryFrame : EveryFrameScript {
        override fun advance(p0: Float) {
            val mutations = mutableListOf<Condition>()
            conditionMap = conditionMap.mapValues { (target, extantConditions) ->
                val (removed, conditions) = extantConditions.partition { condition ->
                    condition.mutation()?.apply {
                        if (continuous) {
                            val mutation = mutate(condition)
                            if (condition is ResolvableCondition && condition.resolveOnMutation) {
                                condition.tryResolve()
                            }
                            if (mutation != null) {
                                mutations.add(mutation)
                            }
                            return@partition true
                        }
                    }

                    (condition is ResolvableCondition && condition.tryResolve()) || condition.expired
                }

                if (removed.isNotEmpty()) {
                    val notifyRemoved = removed.filter { (it !is ResolvableCondition || !it.silenceResolveOnMutation) }
                    if (notifyRemoved.isNotEmpty()) {
                        Global.getSector().campaignUI.addMessage(pc_RecoveryIntel(target.nameString, notifyRemoved))
                    }
                }

                conditions
            }

            for (mutation in mutations) {
                mutation.tryInflictAppend()
            }

            if (showMemorialWall) {
                Global.getSector().campaignUI.showCoreUITab(CoreUITabId.INTEL, MemorialWall.getMemorial())
                showMemorialWall = false
            }
        }

        override fun isDone(): Boolean = false

        override fun runWhilePaused(): Boolean = false
    }

    fun appendCondition(officer: PersonAPI, condition: Condition): List<Condition> {
        val conditions = conditionMap[officer]?.plus(condition) ?: listOf(condition)
        conditionMap = conditionMap.plus(officer to conditions)
        return conditions
    }

    fun killOfficer(officer: PersonAPI, condition: Death) {
        val ship = playerFleet().fleetData.membersInPriorityOrder.find { it.captain == officer }

        playerFleet().fleetData.removeOfficer(officer)
        ship?.captain = null
        officer.conditions().filterIsInstance<ResolvableCondition>().filter { it.resolveOnDeath }.forEach {
            it.expired = true
            it.tryResolve()
        }
        conditionMap = conditionMap.minus(officer)
        officer.addTag(PoC_OFFICER_DEAD)

        val deathLocation = playerFleet().containingLocation.addCustomEntity(null, "", "base_intel_icon", "neutral")
        deathLocation.setFixedLocation(playerFleet().location.x, playerFleet().location.y)

        val deathData = condition.toDeathData(ship, deathLocation)
        MemorialWall.getMemorial().addDeath(deathData)

        MemorialWall.getMemorialEntity().showInteractionDialog(BaseInteractionDialogPlugin { dialog ->
//            dialog.textPanel.addPara("")
            dialog.promptText = ""
            dialog.showCustomVisualDialog(
                CUSTOM_PANEL_WIDTH,
                CUSTOM_PANEL_HEIGHT,
                DeathMessage(deathData, dialog)
            )
        }.withOptionSelected { _, _ ->
//            this.dialog?.let {
//                it.
//            }
        })
        // TODO ADD INTEL NOTIFICATION (MAYBE POPUP?)
    }

    fun showMemorialWallNextFrame() {
        showMemorialWall = true
    }
}

fun PersonAPI.conditions(): List<Condition> = ConditionManager.conditionMap[this] ?: emptyList()
