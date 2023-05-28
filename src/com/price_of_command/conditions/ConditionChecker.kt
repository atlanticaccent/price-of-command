package com.price_of_command.conditions

fun interface ConditionChecker {
    fun check(condition: Condition) : Outcome?
}