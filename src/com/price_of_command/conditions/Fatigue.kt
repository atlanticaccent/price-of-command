package com.price_of_command.conditions

import com.fs.starfarer.api.characters.PersonAPI
import com.price_of_command.ConditionManager
import com.price_of_command.conditions
import com.price_of_command.conditions.overrides.BaseMutator
import com.price_of_command.conditions.overrides.ConditionMutator
import com.price_of_command.modID
import lunalib.lunaSettings.LunaSettings
import kotlin.random.Random

private val FATIGUE_BASE
    get() = LunaSettings.getFloat(modID, "fatigue_duration") ?: 5f
private val FATIGUE_VARIANCE
    get() = LunaSettings.getFloat(modID, "fatigue_variance") ?: 2f
private val FATIGUE_RANGE = FATIGUE_VARIANCE * 2
private val FATIGUE_MIN = FATIGUE_BASE - FATIGUE_VARIANCE
val FATIGUE_CHANCE
    get() = LunaSettings.getFloat(modID, "fatigue_rate")?.div(100) ?: 1f

private val FATIGUE_EXTEND_RATE
    get() = LunaSettings.getFloat(modID, "fatigue_extension_rate")?.div(100) ?: 0.1f

open class Fatigue(
    officer: PersonAPI,
    startDate: Long,
    rootConditions: List<Condition>,
    resolveOnDeath: Boolean = false,
    resolveOnMutation: Boolean = false
) : TimedResolvableCondition(
    officer,
    startDate,
    rootConditions,
    Duration.Time(generateDuration(startDate)),
    resolveOnDeath,
    resolveOnMutation
) {
    companion object {
        @JvmStatic
        fun generateDuration(seed: Long): Float = FATIGUE_MIN + Random(seed).nextFloat() * FATIGUE_RANGE

        @JvmStatic
        fun fatigueEnabled() = LunaSettings.getBoolean(modID, "fatigue_toggle") ?: true
    }

    constructor(officer: PersonAPI, startDate: Long) : this(officer, startDate, emptyList())

    override fun tryResolve() {
        target.stats.setSkillLevel("pc_fatigue", 0f)
    }

    override fun precondition(): Outcome {
        if (target.isAICore) return Outcome.NOOP
        val conditions = target.conditions()
        return if (!fatigueEnabled()) {
            Outcome.Failed
        } else if (ConditionManager.rand.nextFloat() <= FATIGUE_CHANCE) {
            conditions.filterIsInstance<Fatigue>().firstOrNull()?.let {
                if (ConditionManager.rand.nextFloat() <= FATIGUE_EXTEND_RATE) {
                    Outcome.Applied(this)
                } else {
                    Outcome.Failed
                }
            } ?: run {
                if (conditions.filterIsInstance<Wound>().isEmpty()) {
                    Outcome.Applied(this)
                } else {
                    Outcome.Failed
                }
            }
        } else {
            Outcome.NOOP
        }
    }

    @NonPublic
    override fun inflict(): Outcome.Applied<Fatigue> {
        target.conditions().filterIsInstance<Fatigue>().firstOrNull()?.let {
            it.duration.duration += this.duration.duration
        }
        target.stats.setSkillLevel("pc_fatigue", 1f)
        return Outcome.Applied(this)
    }

    override fun failed(): LastingCondition = Injury(target, startDate, extendRootConditions())

    override fun pastTense() = "fatigued"
}

class ExtendFatigue private constructor(
    target: PersonAPI, startDate: Long, rootConditions: List<Condition>
) : ResolvableCondition(target, startDate, Duration.Time(0f), rootConditions, resolveSilently = true) {
    private var previousDuration = 0f
    private lateinit var extended: Fatigue

    override fun tryResolve() = Unit

    override fun precondition(): Outcome = Outcome.Applied(this)

    private fun tryExtendFatigue(target: PersonAPI): Outcome {
        val fatigue = target.conditions().filterIsInstance<Fatigue>().firstOrNull()

        return if (fatigue != null) {
            previousDuration = fatigue.remaining().duration
            fatigue.extendRandomly(ConditionManager.now)
            this.extended = fatigue
            Outcome.Applied(this)
        } else {
            Outcome.Applied(NullCondition(target, startDate))
        }
    }

    @NonPublic
    override fun inflict(): Outcome = tryExtendFatigue(target)

    override fun mutation(): ConditionMutator =
        BaseMutator(continuous = true, checkImmediately = false) { NullCondition(target, startDate) }

    override fun pastTense(): String = ""
}