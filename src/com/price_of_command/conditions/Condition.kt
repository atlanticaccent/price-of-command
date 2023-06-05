@file:Suppress("OPT_IN_IS_NOT_ENABLED")

package com.price_of_command.conditions

import com.fs.starfarer.api.characters.PersonAPI
import com.price_of_command.ConditionManager
import com.price_of_command.andThenOrNull
import com.price_of_command.clock
import com.price_of_command.then

abstract class Condition(val target: PersonAPI, val startDate: Long, var rootConditions: List<Condition>) {
    var expired = false
    val rootCondition
        get() = rootConditions.firstOrNull()

    private fun preconditionOverrides(): Outcome {
        val result = run result@{
            val preconditions = ConditionManager.preconditions.mapNotNull { it.preconditionWithPriority(this) }
            val max = preconditions.maxOfOrNull { it.second }
            preconditions.mapNotNull { (it.second == max).andThenOrNull { it.first } }.reduceOrNull { acc, value ->
                when (value) {
                    is Outcome.Failed -> return@result Outcome.Failed
                    is Outcome.NOOP -> acc
                    else -> value
                }
            }
        }
        ConditionManager.preconditions = ConditionManager.preconditions.filter { !it.oneOff }

        return result ?: Outcome.NOOP
    }

    abstract fun precondition(): Outcome

    @NonPublic
    abstract fun inflict(): Outcome

    private fun mutationOverrides(): Condition? {
        val result = ConditionManager.mutators.mapNotNull { it.mutateWithPriority(this) }.maxByOrNull { it.second }?.first
        ConditionManager.mutators = ConditionManager.mutators.filter { !it.oneOff }
        return result
    }

    open fun mutation(): Condition? = null

    open fun failed(): Condition? = null

    abstract fun pastTense(): String

    @OptIn(NonPublic::class)
    fun tryInflictAppend(): Outcome {
        val outcome = preconditionOverrides()
            .noop { precondition() }
            .failed { return@tryInflictAppend failed()?.tryInflictAppend() ?: Outcome.NOOP }
            .applied {
                val mutation = mutationOverrides() ?: mutation()
                val res = inflict()
                if (mutation != null) return@tryInflictAppend mutation.tryInflictAppend()
                else res
            }
        when (outcome) {
            is Outcome.Applied<*> -> ConditionManager.appendCondition(this.target, this)
            is Outcome.Terminal<*> -> ConditionManager.killOfficer(this.target, this)
            else -> {}
        }
        return outcome
    }

    fun extendRootConditions() = this.rootConditions.plus(this)
}

abstract class LastingCondition(
    target: PersonAPI,
    startDate: Long,
    open val duration: Duration,
    rootConditions: List<Condition>
) : Condition(target, startDate, rootConditions) {
    sealed class Duration {
        object Indefinite : Duration()
        class Time(var duration: Float) : Duration()
    }

    open fun remaining(): Duration = when (val duration = duration) {
        is Duration.Time -> Duration.Time(duration.duration - clock().getElapsedDaysSince(startDate))
        else -> Duration.Indefinite
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

abstract class ResolvableCondition(
    target: PersonAPI,
    startDate: Long,
    override val duration: Duration.Time,
    val expireOnDeath: Boolean = true,
    rootConditions: List<Condition>
) :
    LastingCondition(target, startDate, duration, rootConditions) {
    open fun tryResolve(): Boolean =
        expired || (clock().getElapsedDaysSince(startDate) >= duration.duration).then { expired = true }

    override fun remaining(): Duration.Time = Duration.Time(duration.duration - clock().getElapsedDaysSince(startDate))

    fun extendRandomly(seed: Long) {
        duration.duration += Wound.generateDuration(seed)
    }
}

class NullCondition(target: PersonAPI, startDate: Long) : Condition(target, startDate, emptyList()) {
    override fun precondition(): Outcome = Outcome.NOOP

    @NonPublic
    override fun inflict(): Outcome = Outcome.NOOP

    override fun pastTense(): String = "noop"
}

@RequiresOptIn(message = "random bullshit, ignore me")
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class NonPublic
