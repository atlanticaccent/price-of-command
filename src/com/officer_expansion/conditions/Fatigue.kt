package com.officer_expansion.conditions

import com.fs.starfarer.api.characters.PersonAPI
import com.officer_expansion.conditions
import com.officer_expansion.then
import kotlin.random.Random

private const val FATIGUE_BASE = 5f
private const val FATIGUE_VARIANCE = 2f
private const val FATIGUE_RANGE = FATIGUE_VARIANCE * 2
private const val FATIGUE_MIN = FATIGUE_BASE - FATIGUE_VARIANCE
const val FATIGUE_MAX = FATIGUE_BASE + FATIGUE_VARIANCE

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
        conditions.filterIsInstance<Fatigue>().firstOrNull()?.let {
            it.duration.duration += this.duration.duration

            Outcome.NOOP
        } ?: run {
            if (conditions.filterIsInstance<Wound>().isEmpty()) {
                target.stats.setSkillLevel("oe_fatigue", 1f)
                Outcome.Applied(this)
            } else {
                Outcome.Failed
            }
        }
    }

    override fun pastTense() = "fatigued"
}