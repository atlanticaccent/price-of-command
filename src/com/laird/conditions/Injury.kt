package com.laird.conditions

import com.fs.starfarer.api.characters.PersonAPI
import com.laird.then

class Injury(
    officer: PersonAPI, val skill: String, private val level: Int, startDate: Long, duration: Float
) : ResolvableCondition(officer, startDate, Duration.Time(duration)) {
    override fun tryResolve(): Boolean = super.tryResolve().then {
        target.get()?.stats?.also {
            it.setSkillLevel(skill, level.toFloat())
            it.decreaseSkill("oe_injury")
        }
    }

    override fun pastTense() = "injured"
}
