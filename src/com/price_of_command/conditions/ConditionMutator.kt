package com.price_of_command.conditions

abstract class ConditionMutator(var complete: Boolean = false, val continuous: Boolean = false) {
    fun mutateWithPriority(condition: Condition) = mutate(condition)?.let { it to priority() }

    open fun priority(): Int = 0

    abstract fun mutate(condition: Condition): Condition?
}

class BaseMutator(
    completeImmediately: Boolean = false,
    private val priority: Int = 0,
    continuous: Boolean = false,
    val block: (Condition) -> Condition?
) : ConditionMutator(completeImmediately, continuous) {
    override fun mutate(condition: Condition): Condition? = block(condition)

    override fun priority(): Int = priority
}
