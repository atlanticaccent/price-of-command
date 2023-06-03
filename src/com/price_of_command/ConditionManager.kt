package com.price_of_command

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI
import com.fs.starfarer.api.characters.PersonAPI
import com.price_of_command.conditions.Condition
import com.price_of_command.conditions.ConditionGate
import com.price_of_command.conditions.ConditionMutator
import com.price_of_command.conditions.ResolvableCondition
import com.price_of_command.memorial.MemorialWall
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

    fun findByStats(stats: MutableCharacterStatsAPI): Pair<PersonAPI, List<Condition>>? =
        conditionMap.entries.find { (person, _) ->
            person.stats.equals(stats)
        }?.toPair()

    object pc_ConditionManagerEveryFrame : EveryFrameScript {
        override fun advance(p0: Float) {
            conditionMap = conditionMap.mapValues { (target, extantConditions) ->
                val (removed, conditions) = extantConditions.partition { condition ->
                    (condition is ResolvableCondition && condition.tryResolve()) || condition.expired
                }

                if (removed.isNotEmpty()) {
                    Global.getSector().campaignUI.addMessage(pc_RecoveryIntel(target.nameString, removed))
                }

                conditions
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

    fun killOfficer(officer: PersonAPI, condition: Condition) {
        val ship = playerFleet().fleetData.membersInPriorityOrder.find { it.captain == officer }

        playerFleet().fleetData.removeOfficer(officer)
        playerFleet().fleetData.membersInPriorityOrder.find { it.captain == officer }?.captain = null
        conditionMap = conditionMap.minus(officer)

        val deathLocation = playerFleet().containingLocation.addCustomEntity(null, "", "base_intel_icon", "neutral")
        deathLocation.setFixedLocation(playerFleet().location.x, playerFleet().location.y)

        MemorialWall.getMemorial().addDeath(officer, clock(), ship, deathLocation, condition)
    }
}

fun PersonAPI.conditions(): List<Condition> = ConditionManager.conditionMap[this] ?: emptyList()
