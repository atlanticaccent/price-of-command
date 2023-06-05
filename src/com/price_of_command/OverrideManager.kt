package com.price_of_command

import com.price_of_command.conditions.*

@Suppress("unused", "NAME_SHADOWING")
interface OverrideManager {
    var preconditions: List<ConditionGate>
    var mutators: List<ConditionMutator>

    fun addPreconditionOverride(gate: ConditionGate) {
        preconditions = preconditions.plus(gate)
    }

    fun addPreconditionOverride(completeImmediately: Boolean, gate: (Condition) -> Outcome?): ConditionGate {
        val gate = object : ConditionGate(completeImmediately) {
            override fun precondition(condition: Condition): Outcome? = gate(condition)
        }
        preconditions = preconditions.plus(gate)
        return gate
    }

    fun addPreconditionOverride(
        completeImmediately: Boolean = false,
        priority: Int,
        gate: (Condition) -> Outcome?
    ): ConditionGate {
        val gate = object : ConditionGate(completeImmediately) {
            override fun priority(): Int = priority

            override fun precondition(condition: Condition): Outcome? = gate(condition)
        }
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
        val mutator = BaseMutator(complete = completeImmediately) { mutation(it) }
        mutators = mutators.plus(mutator)
        return mutator
    }

    fun addMutationOverride(
        completeImmediately: Boolean = false,
        priority: Int,
        mutation: (Condition) -> Condition?
    ): ConditionMutator {
        val mutator = object : ConditionMutator(completeImmediately) {
            override fun priority(): Int = priority

            override fun mutate(condition: Condition): Condition? = mutation(condition)
        }
        mutators = mutators.plus(mutator)
        return mutator
    }

    fun removeMutationOverride(mutation: ConditionMutator) {
        mutators = mutators.minus(mutation)
    }
}