package com.price_of_command.conditions.scars

import com.fs.starfarer.api.characters.PersonAPI
import com.price_of_command.conditions.Condition
import com.price_of_command.conditions.IndefiniteResolvableCondition

abstract class Scar(
    target: PersonAPI,
    startDate: Long,
    rootConditions: List<Condition>,
) : IndefiniteResolvableCondition(target, startDate, rootConditions)
