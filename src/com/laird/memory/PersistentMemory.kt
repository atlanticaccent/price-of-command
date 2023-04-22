package com.laird.memory

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.util.IntervalTracker
import org.lwjgl.util.vector.Vector2f

class PersistentMemory(prefix: String): MemoryAPI, EveryFrameScript {
    private sealed class Value {
        class Timed(val value: Any, val timer: IntervalTracker): Value() {
            override fun getVal(): Any {
                return value
            }
        }
        class Raw(val value: Any): Value() {
            override fun getVal(): Any {
                return value
            }
        }

        abstract fun getVal(): Any
    }

    private class PrefixedAccessor(val prefix: String) {
        operator fun get(key: String): Any? = persistent()[prefix + key]

        operator fun set(key: String, value: Any) {
            persistent()[prefix + key] = value
        }

        fun remove(key: String) {
            persistent().remove(prefix + key)
        }
    }

    companion object {
        var init = false

        private fun persistent(): MutableMap<String, Any> {
            return Global.getSector().persistentData
        }
    }

    private val prefix = "oe_${prefix}_"

    private val mem = PrefixedAccessor(prefix)

    init {
        if (!init) {
            Global.getSector().addScript(this)
        }
    }

    private fun keys(): Set<String> {
        return persistent().keys.filter { it.startsWith(prefix) }.toSet()
    }

    override fun isDone() = false

    override fun runWhilePaused() = false

    override fun advance(amount: Float) {
        for (key in keys()) {
            val value = mem[key]
            if (value is Value.Timed) {
                value.timer.advance(amount)
                if (value.timer.intervalElapsed()) {
                    unset(key)
                }
            }
        }
    }

    override fun unset(key: String) {
        mem.remove(key)
    }

    override fun expire(key: String, days: Float) {
        val interval = IntervalTracker(days, days)
        val value = mem[key]
        if (value != null && value is Value) {
            mem[key] = Value.Timed(value.getVal(), interval)
        }
    }

    override fun contains(key: String): Boolean {
        return mem[key] != null
    }

    private fun <T: Any> cmp(key: String, other: T): Boolean {
        return mem[key]?.equals(other) ?: false
    }

    override fun `is`(key: String, other: Any): Boolean = cmp(key, other)

    override fun `is`(key: String, other: Float): Boolean = cmp(key, other)

    override fun `is`(key: String, other: Boolean): Boolean = cmp(key, other)

    override fun set(key: String, value: Any) {
        mem[key] = Value.Raw(value)
    }

    override fun set(key: String, value: Any, expire: Float) {
        mem[key] = Value.Timed(value, IntervalTracker(expire, expire))
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T: Any?> teg(key: String): T = mem[key] as T

    override fun get(key: String): Any? = teg(key)

    override fun getString(key: String): String = teg(key)

    override fun getInt(key: String): Int = teg(key)

    override fun getFloat(key: String): Float = teg(key)

    override fun getBoolean(key: String): Boolean = teg(key)

    override fun getLong(key: String): Long = teg(key)

    override fun getVector2f(key: String): Vector2f? = teg(key)

    override fun getEntity(key: String): SectorEntityToken? = teg(key)

    override fun getFleet(key: String): CampaignFleetAPI? = teg(key)

    override fun getKeys(): MutableCollection<String> {
        return keys().toMutableList()
    }

    override fun getExpire(key: String): Float {
        val value = mem[key]
        return if (value is Value.Timed) {
            value.timer.remaining
        } else {
            0f
        }
    }

    override fun clear() {
        persistent().keys.removeAll(keys())
    }

    override fun isEmpty(): Boolean = keys().isEmpty()

    override fun between(key: String, min: Float, max: Float): Boolean {
        TODO("Not yet implemented")
    }

    override fun addRequired(key: String, requiredKey: String) {
        TODO("Not yet implemented")
    }

    override fun removeRequired(key: String, requiredKey: String) {
        TODO("Not yet implemented")
    }

    override fun getRequired(key: String): MutableSet<String> {
        TODO("Not yet implemented")
    }

    override fun removeAllRequired(key: String) {
        TODO("Not yet implemented")
    }
}