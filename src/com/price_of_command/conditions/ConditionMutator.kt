package com.price_of_command.conditions

abstract class ConditionMutator(var complete: Boolean = false, val continuous: Boolean = false) {
    fun mutateWithPriority(condition: Condition) = mutate(condition)?.let { it to priority() }

    open fun priority(): Int = 0

    abstract fun mutate(condition: Condition): Condition?
}

class BaseMutator(
    complete: Boolean = false,
    continuous: Boolean = false,
    private val priority: Int = 0,
    val block: (Condition) -> Condition?
) : ConditionMutator(complete, continuous) {
    override fun mutate(condition: Condition): Condition? = block(condition)

    override fun priority(): Int = priority
}
