package com.laird

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.Global

class oe_SkillBlockEFS : EveryFrameScript {
    var done = false

    override fun isDone(): Boolean {
        return this.done
    }

    override fun runWhilePaused(): Boolean {
        return false
    }

    override fun advance(amount: Float) {

    }
}