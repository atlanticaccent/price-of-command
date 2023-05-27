package com.officer_expansion.conditions

fun interface ConditionChecker {
    fun check(condition: Condition) : Outcome?
}