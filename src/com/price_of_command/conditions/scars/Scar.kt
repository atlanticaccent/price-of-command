package com.price_of_command.conditions.scars

import com.fs.starfarer.api.characters.PersonAPI
import com.price_of_command.conditions.Condition
import com.price_of_command.conditions.LastingCondition

abstract class Scar(target: PersonAPI, startDate: Long, rootConditions: List<Condition>) :
    LastingCondition(target, startDate, Duration.Indefinite, rootConditions)
