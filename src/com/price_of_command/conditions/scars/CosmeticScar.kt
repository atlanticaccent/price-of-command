package com.price_of_command.conditions.scars

import com.fs.starfarer.api.characters.PersonAPI
import com.price_of_command.conditions
import com.price_of_command.conditions.Condition
import com.price_of_command.conditions.NonPublic
import com.price_of_command.conditions.Outcome

class CosmeticScar(target: PersonAPI, startDate: Long, rootConditions: List<Condition>) :
    Scar(target, startDate, rootConditions) {
    override fun tryResolve() {

    }

    override fun precondition(): Outcome {
        return if (target.conditions().filterIsInstance<CosmeticScar>().isNotEmpty()) {
            Outcome.NOOP
        } else {
            Outcome.Applied(this)
        }
    }

    @NonPublic
    override fun inflict(): Outcome {
        TODO("Not yet implemented")
    }

    override fun pastTense(): String {
        TODO("Not yet implemented")
    }
}