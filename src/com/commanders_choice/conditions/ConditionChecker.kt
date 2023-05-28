package com.commanders_choice.conditions

fun interface ConditionChecker {
    fun check(condition: Condition) : Outcome?
}