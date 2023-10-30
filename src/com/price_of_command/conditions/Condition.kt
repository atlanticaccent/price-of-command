@file:Suppress("OPT_IN_IS_NOT_ENABLED")

package com.price_of_command.conditions

import com.fs.starfarer.api.characters.PersonAPI
import com.price_of_command.*
import com.price_of_command.conditions.overrides.ConditionMutator

abstract class Condition(val target: PersonAPI, val startDate: Long, var rootConditions: List<Condition>) {
    var expired = false
    val rootCondition
        get() = rootConditions.firstOrNull()

    companion object {
        internal fun mutationOverrides(condition: Condition, checkImmediately: Boolean? = null): Condition? {
            val mutators = if (checkImmediately != null) {
                ConditionManager.mutators.filter { it.checkImmediately == checkImmediately }
            } else {
                ConditionManager.mutators
            }
            val result = mutators.mapNotNull { it.mutateWithPriority(condition) }.maxByOrNull { it.second }?.first
            ConditionManager.mutators = ConditionManager.mutators.filter { !it.complete }
            return result
        }
    }

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
        ConditionManager.preconditions = ConditionManager.preconditions.filter { !it.complete }

        return result ?: Outcome.NOOP
    }

    abstract fun precondition(): Outcome

    @NonPublic
    abstract fun inflict(): Outcome

    @Suppress("SameParameterValue")
    private fun mutationOverrides(checkImmediately: Boolean? = null): Condition? =
        Companion.mutationOverrides(this, checkImmediately)

    open fun mutation(): ConditionMutator? = null

    open fun failed(): Condition? = null

    abstract fun pastTense(): String

    @OptIn(NonPublic::class)
    fun tryInflictAppend(causeIfDeath: String? = null, deferDeathResolve: Boolean = false): Outcome {
        val outcome = preconditionOverrides().noop { precondition() }.failed {
            return@tryInflictAppend failed()?.tryInflictAppend(causeIfDeath, deferDeathResolve) ?: Outcome.NOOP
        }.applied {
            val mutation = mutationOverrides(true) ?: mutation()?.takeIf { it.checkImmediately }?.mutate(this@Condition)
            val res = inflict()
            if (mutation != null) return@tryInflictAppend mutation.tryInflictAppend(causeIfDeath, deferDeathResolve)
            else res
        }
        when (outcome) {
            is Outcome.Applied<*> -> ConditionManager.appendCondition(this.target, this)
            is Outcome.Terminal -> {
                outcome.condition.cause = causeIfDeath
                ConditionManager.killOfficer(this.target, outcome.condition, deferDeathResolve)
            }

            else -> {}
        }
        return outcome
    }

    fun extendRootConditions() = this.rootConditions.plus(this)
}

abstract class LastingCondition(
    target: PersonAPI, startDate: Long, open val duration: Duration, rootConditions: List<Condition>
) : Condition(target, startDate, rootConditions) {
    sealed class Duration {
        object Indefinite : Duration()
        class Time(var duration: Float) : Duration()

        companion object {
            @Suppress("unused")
            @JvmStatic
            fun indefinite() = Indefinite
        }
    }

    open fun remaining(): Duration = when (val duration = duration) {
        is Duration.Time -> Duration.Time(duration.duration - clock().getElapsedDaysSince(startDate))
        else -> Duration.Indefinite
    }
}

sealed class Outcome {
    abstract class WithCondition<T : Condition>(val condition: T) : Outcome()

    object NOOP : Outcome()
    class Applied<T : Condition>(condition: T) : WithCondition<T>(condition)
    object Failed : Outcome()
    class Terminal(condition: Death) : WithCondition<Death>(condition)

    inline fun noop(block: Outcome.() -> Outcome): Outcome = (this as? NOOP)?.block() ?: this

    inline fun applied(block: Outcome.() -> Outcome): Outcome = (this as? Applied<*>)?.block() ?: this

    inline fun failed(block: Outcome.() -> Outcome): Outcome = (this as? Failed)?.block() ?: this

    inline fun terminal(block: Outcome.() -> Outcome): Outcome = (this as? Terminal)?.block() ?: this
}

abstract class ResolvableCondition(
    target: PersonAPI,
    startDate: Long,
    duration: Duration,
    rootConditions: List<Condition>,
    var resolveOnDeath: Boolean = true,
    var resolveOnMutation: Boolean = true,
    var resolveSilently: Boolean = false,
    var resolveSilentlyOnMutation: Boolean = false,
) : LastingCondition(target, startDate, duration, rootConditions) {
    open fun tryResolve(): Boolean = expired || when (val duration = duration) {
        is Duration.Time -> (clock().getElapsedDaysSince(startDate) >= duration.duration).then { expired = true }
        is Duration.Indefinite -> false
    }
}

abstract class IndefiniteResolvableCondition(
    target: PersonAPI,
    startDate: Long,
    rootConditions: List<Condition>,
    override val duration: Duration.Indefinite = Duration.indefinite(),
    resolveOnDeath: Boolean = true,
    resolveOnMutation: Boolean = true,
    resolveSilently: Boolean = false,
    resolveSilentlyOnMutation: Boolean = false,
) : ResolvableCondition(
    target,
    startDate,
    duration,
    rootConditions,
    resolveOnDeath,
    resolveOnMutation,
    resolveSilently,
    resolveSilentlyOnMutation
)

abstract class TimedResolvableCondition(
    target: PersonAPI,
    startDate: Long,
    rootConditions: List<Condition>,
    override val duration: Duration.Time,
    resolveOnDeath: Boolean = true,
    resolveOnMutation: Boolean = true,
    resolveSilently: Boolean = false,
    resolveSilentlyOnMutation: Boolean = false,
) : ResolvableCondition(
    target,
    startDate,
    duration,
    rootConditions,
    resolveOnDeath,
    resolveOnMutation,
    resolveSilently,
    resolveSilentlyOnMutation
) {
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

fun PersonAPI.ship() = playerFleet().fleetData.membersListCopy.firstOrNull { it.captain == this }
