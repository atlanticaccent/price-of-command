package com.laird

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI
import com.fs.starfarer.api.characters.PersonAPI
import com.laird.conditions.Condition
import com.laird.conditions.Fatigue
import com.laird.conditions.Injury
import com.laird.conditions.ResolvableCondition
import java.util.*
import kotlin.random.Random

object ConditionManager {
    private val now: Long
        get() = Global.getSector().clock.timestamp
    private val rand: Random by lazy { Random(now) }

    private const val FATIGUE_BASE = 5f
    private const val FATIGUE_VARIANCE = 2f
    private const val FATIGUE_RANGE = FATIGUE_VARIANCE * 2
    private const val FATIGUE_MIN = FATIGUE_BASE - FATIGUE_VARIANCE
    const val FATIGUE_MAX = FATIGUE_BASE + FATIGUE_VARIANCE

    private const val INJURY_RATE = 0.5
    private const val INJURY_BASE = 10
    private const val INJURY_VARIANCE = 4f
    private const val INJURY_RANGE = INJURY_VARIANCE * 2f
    private const val INJURY_MIN = INJURY_BASE - INJURY_RANGE

    private val IGNORE_LIST = arrayOf(
        "aptitude_combat",
        "aptitude_leadership",
        "aptitude_technology",
        "aptitude_industry",
        "oe_injury",
        "oe_fatigue",
    )

    // TODO: 03/05/2023 replace with WeakHashMap-like concept
    val conditionMap: MutableMap<String, MutableList<Condition>> = mutableMapOf()
    val weakOfficerSet: MutableSet<PersonAPI> = Collections.newSetFromMap(WeakHashMap())

    fun findByStats(stats: MutableCharacterStatsAPI): Pair<PersonAPI, List<Condition>>? {
        val conditions = conditionMap.values.find {
            it.firstOrNull()?.target?.get()?.stats?.equals(stats) ?: false
        }?.toList()

        val person = conditions?.firstOrNull()?.target?.get()

        return if (person != null) {
            Pair(person, conditions)
        } else {
            null
        }
    }

    object oe_ConditionManagerEveryFrame : EveryFrameScript {
        override fun advance(p0: Float) {
            conditionMap.entries.removeIf { (_, conditions) ->
                val removed = mutableListOf<Condition>()
                conditions.removeIf { condition ->
                    if (condition is ResolvableCondition) {
                        condition.tryResolve()
                    } else {
                        condition.expired
                    }.then { removed.add(condition) }
                }

                if (removed.isNotEmpty()) {
                    val name = removed.first().target.get()?.nameString ?: "bugg dogg"
                    Global.getSector().intelManager.queueIntel(oe_RecoveryIntel(name, removed))
                }

                conditions.isEmpty()
            }
        }

        override fun isDone(): Boolean = false

        override fun runWhilePaused(): Boolean = false
    }

    fun fatigueOfficer(officer: PersonAPI) {
        conditionMap[officer.id]?.any { it is Fatigue || it is Injury }?.then {
            if (rand.nextFloat() >= INJURY_RATE) {
                injureOfficer(officer)
            }
        }

        val duration = FATIGUE_MIN + rand.nextFloat() * FATIGUE_RANGE
        conditionMap.getOrPut(officer.id) { mutableListOf() }.add(Fatigue(officer, now, duration))

        officer.stats.setSkillLevel("oe_fatigue", 1f)
    }

    fun injureOfficer(officer: PersonAPI): Boolean {
        val skills =
            officer.stats.skillsCopy.filter { !IGNORE_LIST.contains(it.skill.id) && it.level > 0 && !it.skill.isPermanent }
        return if (skills.isNotEmpty()) {
            val duration = INJURY_MIN + rand.nextFloat() * INJURY_RANGE
            val removed = skills.random().toInjury(officer, now, duration)

            logger().debug("Removing ${removed.skill}")

            officer.stats.setSkillLevel(removed.skill, 0f)

            val injuries = conditionMap.getOrPut(officer.id) { mutableListOf() }.run {
                add(removed)
                size
            }

            officer.stats.setSkillLevel("oe_injury", injuries.toFloat())
            true
        } else {
            false
        }
    }
}