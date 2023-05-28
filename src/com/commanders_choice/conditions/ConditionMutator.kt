package com.commanders_choice.conditions

abstract class ConditionMutator(private val continuous: Boolean = false) {
    open fun mutateWithPriority(condition: Condition) = mutate(condition)?.let { it to 0 }

    abstract fun mutate(condition: Condition): Condition?
}