package com.price_of_command.conditions.scars

import com.fs.starfarer.api.characters.PersonAPI
import com.price_of_command.conditions.Condition
import com.price_of_command.conditions.IndefiniteResolvableCondition

abstract class Scar(
    target: PersonAPI,
    startDate: Long,
    rootConditions: List<Condition>,
) : IndefiniteResolvableCondition(target, startDate, rootConditions), ScarFactory {
    companion object {
        val scars: MutableList<ScarFactory> = mutableListOf()

        fun randomScar(target: PersonAPI, startDate: Long, rootConditions: List<Condition>): Scar? {
            return scars.randomOrNull()?.build(target, startDate, rootConditions)
        }
    }

    override fun build(target: PersonAPI, startDate: Long, rootConditions: List<Condition>): Scar = this
}

abstract class DemoralisingScar(target: PersonAPI, startDate: Long, rootConditions: List<Condition>) :
    Scar(target, startDate, rootConditions) {

}

interface ScarFactory {
    fun build(target: PersonAPI, startDate: Long, rootConditions: List<Condition>): Scar
}
