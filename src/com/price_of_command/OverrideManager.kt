package com.price_of_command

import com.price_of_command.conditions.Condition
import com.price_of_command.conditions.ConditionGate
import com.price_of_command.conditions.ConditionMutator
import com.price_of_command.conditions.Outcome

@Suppress("unused", "NAME_SHADOWING")
interface OverrideManager {
    var preconditions: List<ConditionGate>
    var mutators: List<ConditionMutator>

    fun addPreconditionOverride(gate: ConditionGate) {
        preconditions = preconditions.plus(gate)
    }

    fun addPreconditionOverride(oneOff: Boolean = false, gate: (Condition) -> Outcome?): ConditionGate {
        val gate = object : ConditionGate(oneOff) {
            override fun precondition(condition: Condition): Outcome? = gate(condition)
        }
        preconditions = preconditions.plus(gate)
        return gate
    }

    fun addPreconditionOverride(oneOff: Boolean = false, priority: Int, gate: (Condition) -> Outcome?): ConditionGate {
        val gate = object : ConditionGate(oneOff) {
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

    fun addMutationOverride(oneOff: Boolean = false, mutation: (Condition) -> Condition?): ConditionMutator {
        val mutator = object : ConditionMutator(oneOff) {
            override fun mutate(condition: Condition): Condition? = mutation(condition)
        }
        mutators = mutators.plus(mutator)
        return mutator
    }

    fun addMutationOverride(
        oneOff: Boolean = false,
        priority: Int,
        mutation: (Condition) -> Condition?
    ): ConditionMutator {
        val mutator = object : ConditionMutator(oneOff) {
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