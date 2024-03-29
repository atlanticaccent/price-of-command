package com.price_of_command

import com.price_of_command.conditions.*
import com.price_of_command.conditions.overrides.BaseMutator
import com.price_of_command.conditions.overrides.BasePrecondition
import com.price_of_command.conditions.overrides.ConditionGate
import com.price_of_command.conditions.overrides.ConditionMutator

@Suppress("unused", "NAME_SHADOWING")
internal interface OverrideManager {
    var preconditions: List<ConditionGate>
    var mutators: List<ConditionMutator>
    var postBattleListeners: List<PostBattleListener>

    fun addPreconditionOverride(gate: ConditionGate) {
        preconditions = preconditions.plus(gate)
    }

    fun addPreconditionOverride(completeImmediately: Boolean = false, gate: (Condition) -> Outcome?): ConditionGate {
        val gate = BasePrecondition(completeImmediately, block = gate)
        preconditions = preconditions.plus(gate)
        return gate
    }

    fun addPreconditionOverride(
        completeImmediately: Boolean = false, priority: Int, gate: (Condition) -> Outcome?
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
        completeImmediately: Boolean = false, mutation: (Condition) -> Condition?
    ): ConditionMutator {
        val mutator = BaseMutator(completeImmediately, block = mutation)
        mutators = mutators.plus(mutator)
        return mutator
    }

    fun addMutationOverride(
        completeImmediately: Boolean = false, priority: Int, mutation: (Condition) -> Condition?
    ): ConditionMutator {
        val mutator = BaseMutator(completeImmediately, priority, block = mutation)
        mutators = mutators.plus(mutator)
        return mutator
    }

    fun removeMutationOverride(mutation: ConditionMutator) {
        mutators = mutators.minus(mutation)
    }

    fun addDeathListener(listener: (Death) -> Outcome?) {
        addPreconditionOverride {
            if (it is Death && it.precondition() is Outcome.Applied<*>) {
                listener(it)
            } else {
                null
            }
        }
    }

    fun postDeathListener(listener: (Death) -> Unit) {
        addMutationOverride {
            if (it is Death) {
                listener(it)
            }
            null
        }
    }

    fun addPostBattleListener(listener: PostBattleListener) {
        postBattleListeners = postBattleListeners.plus(listener)
    }

    fun addPostBattleListener(listener: PBLLambda) {
        addPostBattleListener(BasePostBattleListener(listener))
    }
}