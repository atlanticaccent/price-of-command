package com.price_of_command

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI
import com.fs.starfarer.api.characters.PersonAPI
import com.price_of_command.conditions.*
import kotlin.random.Random

object ConditionManager {
    const val CONDITION_MAP = "pc_persistent_condition_map"

    val now: Long
        get() = Global.getSector().clock.timestamp
    val rand: Random by lazy { Random(now) }

    var conditionMap: Map<PersonAPI, List<Condition>> = emptyMap()
    internal val preconditions: MutableList<ConditionGate> = mutableListOf()
    internal val mutators: MutableList<ConditionMutator> = mutableListOf()

    fun addPreconditionOverride(gate: ConditionGate) = preconditions.add(gate)

    fun addPreconditionOverride(gate: (Condition) -> Outcome?) {
        preconditions.add(object : ConditionGate() {
            override fun precondition(condition: Condition): Outcome? = gate(condition)
        })
    }

    fun addPreconditionOverride(gate: (Condition) -> Outcome?, priority: Int) {
        preconditions.add(object : ConditionGate() {
            override fun priority(): Int = priority

            override fun precondition(condition: Condition): Outcome? = gate(condition)
        })
    }

    fun addMutationOverride(mutation: ConditionMutator) = mutators.add(mutation)

    fun addMutationOverride(mutation: (Condition) -> Condition?) {
        mutators.add(object : ConditionMutator() {
            override fun mutate(condition: Condition): Condition? = mutation(condition)
        })
    }

    fun addMutationOverride(mutation: (Condition) -> Condition?, priority: Int) {
        mutators.add(object : ConditionMutator() {
            override fun priority(): Int = priority

            override fun mutate(condition: Condition): Condition? = mutation(condition)
        })
    }

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

    fun fatigueOfficer(officer: PersonAPI): Boolean {
        val fatigue = Fatigue(officer, now)

        return when (fatigue.tryInflictAppend()) {
            is Outcome.Failed -> false
            else -> true
        }
    }

    fun injureOfficer(officer: PersonAPI): Boolean {
        val injury = Injury(officer, now)

        return when (injury.tryInflictAppend().failed { Wound.tryExtendWounds(officer) }
            .failed { GraveInjury(officer, now).tryInflictAppend() }) {
            is Outcome.Failed -> false
            else -> true
        }
    }
}

fun PersonAPI.conditions(): List<Condition> = ConditionManager.conditionMap[this] ?: emptyList()
