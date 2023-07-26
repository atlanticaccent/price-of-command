@file:Suppress("NAME_SHADOWING")

package com.price_of_command

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI
import com.fs.starfarer.api.characters.PersonAPI
import com.price_of_command.conditions.Condition
import com.price_of_command.conditions.Death
import com.price_of_command.conditions.PostBattleListener
import com.price_of_command.conditions.ResolvableCondition
import com.price_of_command.conditions.overrides.ConditionGate
import com.price_of_command.conditions.overrides.ConditionMutator
import com.price_of_command.fleet_interaction.AfterActionReport
import com.price_of_command.memorial.MemorialWall
import kotlin.random.Random

object ConditionManager : OverrideManager {
    const val CONDITION_MAP = "pc_persistent_condition_map"
    const val PRECONDITIONS = "pc_persistent_preconditions"
    const val MUTATORS = "pc_persistent_mutators"
    const val POST_BATTLE_LISTENERS = "pc_post_battle_listeners"

    val now: Long
        get() = Global.getSector().clock.timestamp
    val rand: Random by lazy { Random(now) }

    internal var conditionMap: Map<PersonAPI, List<Condition>> = emptyMap()
    override var preconditions: List<ConditionGate> = listOf()
    override var mutators: List<ConditionMutator> = listOf()
    override var postBattleListeners: List<PostBattleListener> = listOf()

    private var showMemorialWall: Boolean = false
    internal var afterActionReport: AfterActionReport<*>? = null

    @JvmStatic
    fun findByStats(stats: MutableCharacterStatsAPI): Pair<PersonAPI, List<Condition>>? =
        conditionMap.entries.find { (person, _) ->
            person.stats.equals(stats)
        }?.toPair()

    internal object pc_ConditionManagerEveryFrame : EveryFrameScript {
        override fun advance(p0: Float) {
            if (afterActionReport != null) {
                return
            }

            val mutations = mutableListOf<Condition>()
            conditionMap = conditionMap.mapValues { (target, extantConditions) ->
                val (removed, conditions) = extantConditions.partition { condition ->
                    condition.mutation()?.apply {
                        if (continuous) {
                            val mutation = Condition.mutationOverrides(condition) ?: mutate(condition)
                            if (mutation != null) {
                                if (condition is ResolvableCondition) {
                                    if (condition.resolveOnMutation) {
                                        condition.tryResolve()
                                    }
                                    condition.resolveSilently =
                                        condition.resolveSilently || condition.resolveSilentlyOnMutation
                                }
                                mutations.add(mutation)
                                return@partition true
                            }
                        }
                    }

                    // Ok to call `tryResolve` "again" after a mutation because we return early above
                    (condition is ResolvableCondition && condition.tryResolve()) || condition.expired
                }

                if (removed.isNotEmpty()) {
                    val notifyRemoved = removed.filter { it !is ResolvableCondition || !it.resolveSilently }
                    if (notifyRemoved.isNotEmpty()) {
                        logger().debug("Resolving ${notifyRemoved.map { it::class }}")
                        Global.getSector().campaignUI.addMessage(pc_RecoveryIntel(target.nameString, notifyRemoved))
                    }
                }

                conditions
            }

            for (mutation in mutations) {
                mutation.tryInflictAppend()
            }

            conditionMap = conditionMap.filterValues { it.isNotEmpty() }
        }

        override fun isDone(): Boolean = false

        override fun runWhilePaused(): Boolean = false
    }

    @JvmStatic
    fun appendCondition(officer: PersonAPI, condition: Condition): List<Condition> =
        appendCondition(officer, listOf(condition))

    @JvmStatic
    fun appendCondition(officer: PersonAPI, conditions: List<Condition>): List<Condition> {
        val conditions = conditionMap[officer]?.plus(conditions) ?: conditions
        conditionMap = conditionMap.plus(officer to conditions)
        return conditions
    }

    @JvmStatic
    fun removeCondition(condition: Condition): List<Condition> = conditionMap[condition.target]?.let {
        val conditions = it.minus(condition)
        conditionMap = conditionMap.plus(condition.target to conditions)
        conditions
    } ?: emptyList()

    @JvmStatic
    fun tryResolve(condition: ResolvableCondition): Boolean = condition.tryResolve().then {
        removeCondition(condition)
        if (!condition.resolveSilently) {
            logger().debug("Resolving $condition")
            Global.getSector().campaignUI.addMessage(pc_RecoveryIntel(condition.target.nameString, listOf(condition)))
        }
    }

    @JvmStatic
    fun killOfficer(officer: PersonAPI, condition: Death, deferResolve: Boolean = false) {
        val ship = playerFleet().fleetData.membersInPriorityOrder.find { it.captain == officer }
        var conditions = officer.conditions()

        playerFleet().fleetData.removeOfficer(officer)
        ship?.captain = null
        if (!deferResolve) {
            conditions.filterIsInstance<ResolvableCondition>().filter { it.resolveOnDeath }.forEach {
                it.expired = true
                it.tryResolve()
            }
            conditions = emptyList()
        }
        conditionMap = conditionMap.minus(officer)
        officer.addTag(PoC_OFFICER_DEAD)

        val deathLocation = playerFleet().containingLocation.addCustomEntity(null, "", "base_intel_icon", "neutral")
        deathLocation.setFixedLocation(playerFleet().location.x, playerFleet().location.y)

        val deathData = condition.toDeathData(ship, deathLocation, conditions)
        MemorialWall.getMemorial().addDeath(deathData)
    }

    @JvmStatic
    fun showMemorialWallNextFrame() {
        showMemorialWall = true
    }
}

fun PersonAPI.conditions(): List<Condition> = ConditionManager.conditionMap[this] ?: emptyList()
