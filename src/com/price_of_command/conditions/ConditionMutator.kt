package com.price_of_command.conditions

abstract class ConditionMutator(val oneOff: Boolean = false, val continuous: Boolean = false) {
    fun mutateWithPriority(condition: Condition) = mutate(condition)?.let { it to priority() }

    open fun priority(): Int = 0

    abstract fun mutate(condition: Condition): Condition?
}