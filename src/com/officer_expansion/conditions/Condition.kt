@file:Suppress("OPT_IN_IS_NOT_ENABLED")

package com.officer_expansion.conditions

import com.fs.starfarer.api.characters.PersonAPI
import com.officer_expansion.ConditionManager
import com.officer_expansion.clock
import com.officer_expansion.then

abstract class Condition(val target: PersonAPI, val startDate: Long, open val duration: Duration) {
    companion object {
        val mutators: MutableList<ConditionMutator> = mutableListOf()
        val preconditions: MutableList<(Condition) -> Outcome?> = mutableListOf()
    }

    var expired = false

    sealed class Duration {
        object Indefinite : Duration()
        class Time(var duration: Float) : Duration()
    }

    open fun remaining(): Duration = when (val duration = duration) {
        is Duration.Time -> Duration.Time(duration.duration - clock().getElapsedDaysSince(startDate))
        else -> Duration.Indefinite
    }

    private fun preconditionOverrides(): Outcome {
        val result = run result@{
            preconditions.mapNotNull { it(this) }.reduceOrNull { acc, value ->
                when (value) {
                    is Outcome.Failed -> return@result Outcome.Failed
                    is Outcome.NOOP -> acc
                    else -> value
                }
            }
        }

        return result ?: Outcome.NOOP
    }

    abstract fun precondition(): Outcome

    @NonPublic
    abstract fun inflict(): Outcome

    private fun mutationOverrides(): Condition? =
        mutators.mapNotNull { it.mutateWithPriority(this) }.maxByOrNull { it.second }?.first

    open fun mutation(): Condition? = null

    open fun failed(): Condition? = null

    abstract fun pastTense(): String

    @OptIn(NonPublic::class)
    fun tryInflictAppend(): Outcome {
        val outcome = preconditionOverrides().noop { precondition() }
            .failed { return@tryInflictAppend failed()?.tryInflictAppend() ?: Outcome.NOOP }
//            .terminal { /* DIE */ }
            .applied {
                val mutation = mutationOverrides() ?: mutation()
                if (mutation != null) return@tryInflictAppend mutation.tryInflictAppend()
                else inflict()
            }
        (outcome as? Outcome.Applied<*>)?.let { ConditionManager.appendCondition(this.target, this) }
        return outcome
    }
}

sealed class Outcome {
    object NOOP : Outcome()
    class Applied<T : Condition>(val condition: T) : Outcome()
    object Failed : Outcome()
    class Terminal<T : Condition>(val condition: T) : Outcome()

    inline fun noop(block: Outcome.() -> Outcome): Outcome = (this as? NOOP)?.block() ?: this

    inline fun applied(block: Outcome.() -> Outcome): Outcome = (this as? Applied<*>)?.block() ?: this

    inline fun failed(block: Outcome.() -> Outcome): Outcome = (this as? Failed)?.block() ?: this

    inline fun terminal(block: Outcome.() -> Outcome): Outcome = (this as? Terminal<*>)?.block() ?: this
}

abstract class ResolvableCondition(target: PersonAPI, startDate: Long, override val duration: Duration.Time) :
    Condition(target, startDate, duration) {
    open fun tryResolve(): Boolean =
        expired || (clock().getElapsedDaysSince(startDate) >= duration.duration).then { expired = true }

    override fun remaining(): Duration.Time = Duration.Time(duration.duration - clock().getElapsedDaysSince(startDate))

    fun extendRandomly(seed: Long) {
        duration.duration += Wound.generateDuration(seed)
    }
}

@RequiresOptIn(message = "random bullshit, ignore me")
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class NonPublic
