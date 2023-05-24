package com.officer_expansion.conditions

import com.fs.starfarer.api.characters.PersonAPI
import com.officer_expansion.ConditionManager
import com.officer_expansion.OfficerExpansionPlugin
import com.officer_expansion.conditions
import com.officer_expansion.then
import lunalib.lunaSettings.LunaSettings
import kotlin.random.Random

private val FATIGUE_BASE
    get() = LunaSettings.getFloat(OfficerExpansionPlugin.modID, "fatigue_duration") ?: 5f
private val FATIGUE_VARIANCE
    get() = LunaSettings.getFloat(OfficerExpansionPlugin.modID, "fatigue_variance") ?: 2f
private val FATIGUE_RANGE = FATIGUE_VARIANCE * 2
private val FATIGUE_MIN = FATIGUE_BASE - FATIGUE_VARIANCE
private val FATIGUE_CHANCE
    get() = LunaSettings.getFloat(OfficerExpansionPlugin.modID, "fatigue_rate")?.div(100) ?: 1f

private val FATIGUE_EXTEND_RATE
    get() = LunaSettings.getFloat(OfficerExpansionPlugin.modID, "fatigue_extension_rate")?.div(100) ?: 0.1f

class Fatigue(
    officer: PersonAPI, startDate: Long
) : ResolvableCondition(officer, startDate, Duration.Time(generateDuration(startDate))) {
    companion object {
        fun generateDuration(seed: Long): Float = FATIGUE_MIN + Random(seed).nextFloat() * FATIGUE_RANGE
    }

    override fun tryResolve(): Boolean {
        return super.tryResolve().then {
            target.stats.setSkillLevel("oe_fatigue", 0f)
        }
    }

    @NonPublic
    override fun tryInflict(): Outcome = run {
        val conditions = target.conditions()
        if (ConditionManager.rand.nextFloat() <= FATIGUE_CHANCE) {
            conditions.filterIsInstance<Fatigue>().firstOrNull()?.let {
                if (ConditionManager.rand.nextFloat() <= FATIGUE_EXTEND_RATE) {
                    it.duration.duration += this.duration.duration

                    Outcome.NOOP
                } else {
                    Outcome.Failed
                }
            } ?: run {
                if (conditions.filterIsInstance<Wound>().isEmpty()) {
                    target.stats.setSkillLevel("oe_fatigue", 1f)
                    Outcome.Applied(this)
                } else {
                    Outcome.Failed
                }
            }
        } else {
            Outcome.NOOP
        }
    }

    override fun pastTense() = "fatigued"
}