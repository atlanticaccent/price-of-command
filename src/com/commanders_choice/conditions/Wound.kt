package com.commanders_choice.conditions

import com.fs.starfarer.api.characters.PersonAPI
import com.commanders_choice.*
import lunalib.lunaSettings.LunaSettings
import kotlin.random.Random

private val INJURY_RATE
    get() = LunaSettings.getFloat(OfficerExpansionPlugin.modID, "injury_rate")?.div(100) ?: 0.5f
private val INJURY_BASE
    get() = LunaSettings.getFloat(OfficerExpansionPlugin.modID, "injury_duration") ?: 10f
private val INJURY_VARIANCE
    get() = LunaSettings.getFloat(OfficerExpansionPlugin.modID, "injury_variance") ?: 4f
private val INJURY_RANGE = INJURY_VARIANCE * 2
private val INJURY_MIN = INJURY_BASE - INJURY_VARIANCE

private val EXTEND_RATE
    get() = LunaSettings.getFloat(OfficerExpansionPlugin.modID, "injury_extension_rate")?.div(100) ?: 0.5f

private val IGNORE_LIST = arrayOf(
    "aptitude_combat",
    "aptitude_leadership",
    "aptitude_technology",
    "aptitude_industry",
    "oe_injury",
    "oe_grave_injury",
    "oe_fatigue",
)

abstract class Wound(
    officer: PersonAPI, startDate: Long
) : ResolvableCondition(officer, startDate, Duration.Time(generateDuration(startDate))) {
    companion object {
        fun generateDuration(seed: Long): Float = INJURY_MIN + Random(seed).nextFloat() * INJURY_RANGE

        fun tryExtendWounds(target: PersonAPI): Outcome {
            val wounds = target.conditions().filterIsInstance<Wound>()

            return if (wounds.isNotEmpty() && ConditionManager.rand.nextFloat() >= EXTEND_RATE) {
                val extended = wounds.random()
                extended.extendRandomly(ConditionManager.now)
                Outcome.Applied(extended)
            } else {
                Outcome.Failed
            }
        }
    }

    override fun pastTense(): String = "injured"
}

open class Injury private constructor(
    officer: PersonAPI, startDate: Long, val injurySkillSuffix: Int, private var testOverride: Boolean = false
) : Wound(officer, startDate) {
    private var _skill: String? = null
    val skill: String
        get() = _skill ?: throw IllegalStateException("Injury Skill ID Not Set")
    private var _level: Int? = null
    val level: Int
        get() = _level ?: throw IllegalStateException("Injury Skill Level Not Set")

    companion object {
        private val suffixRange = (1..7)

        fun pickInjurySuffix(officer: PersonAPI): Int {
            val taken = officer.conditions().filterIsInstance<Injury>().map { it.injurySkillSuffix }.toSet()
            val available = suffixRange.subtract(taken)
            return available.randomOrNull() ?: suffixRange.random()
        }
    }

    constructor(officer: PersonAPI, startDate: Long) : this(officer, startDate, pickInjurySuffix(officer))

    constructor(officer: PersonAPI, skill: String, level: Int, startDate: Long) : this(officer, startDate) {
        _skill = skill
        _level = level
    }

    override fun tryResolve(): Boolean = super.tryResolve().then {
        target.stats.setSkillLevel(skill, level.toFloat())
        target.stats.decreaseSkill("oe_injury_$injurySkillSuffix")
    }

    override fun precondition(): Outcome {
        val conditions = target.conditions()
        val skills =
            target.stats.skillsCopy.filter { !IGNORE_LIST.contains(it.skill.id) && it.level > 0 && !it.skill.isPermanent }
        if (conditions.any { it is Fatigue || it is Wound } || !Fatigue.fatigueEnabled()) {
            if (skills.isNotEmpty()) {
                if (ConditionManager.rand.nextFloat() <= INJURY_RATE || testOverride) {
                    return Outcome.Applied(this)
                }
            } else {
                return Outcome.Failed
            }
        }

        return Outcome.NOOP
    }

    @NonPublic
    override fun inflict() : Outcome.Applied<Injury> {
        val skills =
            target.stats.skillsCopy.filter { !IGNORE_LIST.contains(it.skill.id) && it.level > 0 && !it.skill.isPermanent }
        val removed = skills.random()

        _skill = removed.skill.id
        _level = removed.level.toInt()

        target.stats.setSkillLevel("oe_fatigue", 0f)

        val injuries = target.conditions().filterIsInstance<Injury>()
            .filter { it.injurySkillSuffix == injurySkillSuffix }.size + 1

        target.stats.setSkillLevel("oe_injury_$injurySkillSuffix", injuries.toFloat())

        target.stats.setSkillLevel(skill, 0f)

        return Outcome.Applied(this)
    }

    override fun failed(): Condition = GraveInjury(target, startDate)
}

class GraveInjury(target: PersonAPI, startDate: Long) : Wound(target, startDate) {
    override fun tryResolve(): Boolean = super.tryResolve().then {
        // TODO inflict a scar when resolved
        target.stats.setSkillLevel("oe_grave_injury", 0f)
    }

    override fun precondition(): Outcome {
        // TODO chance to fail if already gravely injured
        return Outcome.Applied(this)
    }

    @NonPublic
    override fun inflict(): Outcome.Applied<GraveInjury> {
        target.stats.setSkillLevel("oe_grave_injury", 1f)

        // TODO chance to fail if already gravely injured
        return Outcome.Applied(this)
    }
}
