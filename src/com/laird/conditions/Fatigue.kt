package com.laird.conditions

import com.fs.starfarer.api.characters.PersonAPI
import com.laird.then

class Fatigue(
    officer: PersonAPI, startDate: Long, duration: Float
) : ResolvableCondition(officer, startDate, Duration.Time(duration)) {
    override fun tryResolve(): Boolean {
        return super.tryResolve().then {
            target.get()?.stats?.setSkillLevel("oe_fatigue", 0f)
        }
    }

    override fun pastTense() = "fatigued"
}