package com.price_of_command.conditions.scars

import com.fs.starfarer.api.characters.PersonAPI
import com.price_of_command.conditions.Condition
import com.price_of_command.conditions.IndefiniteResolvableCondition
import com.price_of_command.conditions.scars.personality_change.Timid

abstract class Scar(
    target: PersonAPI,
    startDate: Long,
    rootConditions: List<Condition>,
) : IndefiniteResolvableCondition(target, startDate, rootConditions) {
    companion object {
        val scars: MutableList<Factory<*>> = mutableListOf()

        fun randomScar(target: PersonAPI, startDate: Long, rootConditions: List<Condition>) : Scar? {
            return scars.randomOrNull()?.build()
        }
    }

    abstract class Factory<T: Scar> {
        abstract fun build(): T
    }
}
