package com.officer_expansion.conditions

import com.fs.starfarer.api.characters.PersonAPI
import com.officer_expansion.ConditionManager
import com.officer_expansion.clock
import com.officer_expansion.then

sealed class Condition(val target: PersonAPI, val startDate: Long, open val duration: Duration) {
    var expired = false

    sealed class Duration {
        object Indefinite : Duration()
        class Time(var duration: Float) : Duration()
    }

    open fun remaining(): Duration = when (val duration = duration) {
        is Duration.Time -> Duration.Time(duration.duration - clock().getElapsedDaysSince(startDate))
        else -> Duration.Indefinite
    }

    @NonPublic
    abstract fun tryInflict(): Outcome

    abstract fun pastTense(): String
}

sealed class Outcome {
    object NOOP : Outcome()
    class Applied<T : Condition>(val condition: T) : Outcome()
    object Failed : Outcome()

    fun failed(block: () -> Outcome) : Outcome {
        return if (this is Failed) {
            block()
        } else {
            this
        }
    }
}

sealed class ResolvableCondition(target: PersonAPI, startDate: Long, override val duration: Duration.Time) :
    Condition(target, startDate, duration) {
    open fun tryResolve(): Boolean =
        (clock().getElapsedDaysSince(startDate) >= duration.duration).then { expired = true }

    override fun remaining(): Duration.Time = Duration.Time(duration.duration - clock().getElapsedDaysSince(startDate))

    fun extendRandomly(seed: Long) {
        duration.duration += Wound.generateDuration(seed)
    }
}

@OptIn(NonPublic::class)
fun <T : Condition> T.tryInflictAppend(): Outcome = run {
    val outcome = tryInflict()
    if (outcome is Outcome.Applied<*>) ConditionManager.appendCondition(this.target, this)
    outcome
}

@RequiresOptIn(message = "random bullshit, ignore me")
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class NonPublic
