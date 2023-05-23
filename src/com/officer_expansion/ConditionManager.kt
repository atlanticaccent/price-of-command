package com.officer_expansion

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI
import com.fs.starfarer.api.characters.PersonAPI
import com.officer_expansion.conditions.*
import kotlin.random.Random

object ConditionManager {
    val now: Long
        get() = Global.getSector().clock.timestamp
    val rand: Random by lazy { Random(now) }

    var conditionMap: Map<PersonAPI, List<Condition>> = emptyMap()

    fun findByStats(stats: MutableCharacterStatsAPI): Pair<PersonAPI, List<Condition>>? =
        conditionMap.entries.find { (person, _) ->
            person.stats.equals(stats)
        }?.toPair()

    object oe_ConditionManagerEveryFrame : EveryFrameScript {
        override fun advance(p0: Float) {
            conditionMap = conditionMap.mapValues { (target, extantConditions) ->
                val (removed, conditions) = extantConditions.partition { condition ->
                    if (condition is ResolvableCondition) {
                        val resolved = condition.tryResolve()
                        resolved
                    } else {
                        condition.expired
                    }
                }

                if (removed.isNotEmpty()) {
                    Global.getSector().campaignUI.addMessage(oe_RecoveryIntel(target.nameString, removed))
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
