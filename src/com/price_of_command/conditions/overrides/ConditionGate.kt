package com.price_of_command.conditions.overrides

import com.price_of_command.conditions.Condition
import com.price_of_command.conditions.Outcome

abstract class ConditionGate(var complete: Boolean = false) {
    fun preconditionWithPriority(condition: Condition) = precondition(condition)?.let { it to priority() }

    open fun priority() = 0

    abstract fun precondition(condition: Condition): Outcome?
}

class BasePrecondition(
    completeImmediately: Boolean = false,
    private val priority: Int = 0,
    val block: (Condition) -> Outcome?
) : ConditionGate(completeImmediately) {
    override fun precondition(condition: Condition): Outcome? = block(condition)

    override fun priority(): Int = priority
}