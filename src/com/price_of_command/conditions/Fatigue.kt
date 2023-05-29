package com.price_of_command.conditions

import com.fs.starfarer.api.characters.PersonAPI
import com.price_of_command.ConditionManager
import com.price_of_command.OfficerExpansionPlugin
import com.price_of_command.conditions
import com.price_of_command.then
import lunalib.lunaSettings.LunaSettings
import kotlin.random.Random

private val FATIGUE_BASE
    get() = LunaSettings.getFloat(OfficerExpansionPlugin.modID, "fatigue_duration") ?: 5f
private val FATIGUE_VARIANCE
    get() = LunaSettings.getFloat(OfficerExpansionPlugin.modID, "fatigue_variance") ?: 2f
private val FATIGUE_RANGE = FATIGUE_VARIANCE * 2
private val FATIGUE_MIN = FATIGUE_BASE - FATIGUE_VARIANCE
val FATIGUE_CHANCE
    get() = LunaSettings.getFloat(OfficerExpansionPlugin.modID, "fatigue_rate")?.div(100) ?: 1f

private val FATIGUE_EXTEND_RATE
    get() = LunaSettings.getFloat(OfficerExpansionPlugin.modID, "fatigue_extension_rate")?.div(100) ?: 0.1f

class Fatigue(
    officer: PersonAPI, startDate: Long
) : ResolvableCondition(officer, startDate, Duration.Time(generateDuration(startDate))) {
    companion object {
        fun generateDuration(seed: Long): Float = FATIGUE_MIN + Random(seed).nextFloat() * FATIGUE_RANGE

        fun fatigueEnabled() = LunaSettings.getBoolean(OfficerExpansionPlugin.modID, "fatigue_toggle") ?: true
    }

    override fun tryResolve(): Boolean {
        return super.tryResolve().then {
            target.stats.setSkillLevel("pc_fatigue", 0f)
        }
    }

    override fun precondition(): Outcome {
        val conditions = target.conditions()
        return if (!fatigueEnabled()) {
            Outcome.Failed
        } else if (ConditionManager.rand.nextFloat() <= FATIGUE_CHANCE) {
            conditions.filterIsInstance<Fatigue>().firstOrNull()?.let {
                if (ConditionManager.rand.nextFloat() <= FATIGUE_EXTEND_RATE) {
                    it.duration.duration += this.duration.duration

                    Outcome.NOOP
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
        target.stats.setSkillLevel("pc_fatigue", 1f)
        return Outcome.Applied(this)
    }

    override fun failed(): LastingCondition = Injury(target, startDate)

    override fun pastTense() = "fatigued"
}