package com.price_of_command.conditions

abstract class ConditionGate {
    fun preconditionWithPriority(condition: Condition) = precondition(condition)?.let { it to priority() }

    open fun priority() = 0

    abstract fun precondition(condition: Condition) : Outcome?
}