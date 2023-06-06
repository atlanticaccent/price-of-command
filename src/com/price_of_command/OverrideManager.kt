package com.price_of_command

import com.price_of_command.conditions.Condition
import com.price_of_command.conditions.Outcome
import com.price_of_command.conditions.overrides.BaseMutator
import com.price_of_command.conditions.overrides.BasePrecondition
import com.price_of_command.conditions.overrides.ConditionGate
import com.price_of_command.conditions.overrides.ConditionMutator

@Suppress("unused", "NAME_SHADOWING")
interface OverrideManager {
    var preconditions: List<ConditionGate>
    var mutators: List<ConditionMutator>

    fun addPreconditionOverride(gate: ConditionGate) {
        preconditions = preconditions.plus(gate)
    }

    fun addPreconditionOverride(completeImmediately: Boolean = false, gate: (Condition) -> Outcome?): ConditionGate {
        val gate = BasePrecondition(completeImmediately, block = gate)
        preconditions = preconditions.plus(gate)
        return gate
    }

    fun addPreconditionOverride(
        completeImmediately: Boolean = false,
        priority: Int,
        gate: (Condition) -> Outcome?
    ): ConditionGate {
        val gate = BasePrecondition(completeImmediately, priority, block = gate)
        preconditions = preconditions.plus(gate)
        return gate
    }

    fun removePreconditionOverride(gate: ConditionGate) {
        preconditions = preconditions.minus(gate)
    }

    fun addMutationOverride(mutation: ConditionMutator) {
        mutators = mutators.plus(mutation)
    }

    fun addMutationOverride(
        completeImmediately: Boolean = false,
        mutation: (Condition) -> Condition?
    ): ConditionMutator {
        val mutator = BaseMutator(completeImmediately, block = mutation)
        mutators = mutators.plus(mutator)
        return mutator
    }

    fun addMutationOverride(
        completeImmediately: Boolean = false,
        priority: Int,
        mutation: (Condition) -> Condition?
    ): ConditionMutator {
        val mutator = BaseMutator(completeImmediately, priority, block = mutation)
        mutators = mutators.plus(mutator)
        return mutator
    }

    fun removeMutationOverride(mutation: ConditionMutator) {
        mutators = mutators.minus(mutation)
    }
}