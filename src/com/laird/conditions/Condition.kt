package com.laird.conditions

import com.fs.starfarer.api.characters.PersonAPI
import com.laird.clock
import com.laird.then
import java.lang.ref.WeakReference

sealed class Condition(target: PersonAPI, val startDate: Long, open val duration: Duration) {
    val target = WeakReference(target)
    var expired = false

    sealed class Duration {
        object Indefinite : Duration()
        class Time(val duration: Float) : Duration()
    }

    open fun remaining(): Duration = when (val duration = duration) {
        is Duration.Time -> Duration.Time(duration.duration - clock().getElapsedDaysSince(startDate))
        else -> Duration.Indefinite
    }

    abstract fun pastTense(): String
}

sealed class ResolvableCondition(target: PersonAPI, startDate: Long, override val duration: Duration.Time) :
    Condition(target, startDate, duration) {
    open fun tryResolve(): Boolean =
        (clock().getElapsedDaysSince(startDate) >= duration.duration).then { expired = true }

    override fun remaining(): Duration.Time = this.duration
}