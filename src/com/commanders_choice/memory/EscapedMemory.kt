package com.commanders_choice.memory

import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import org.lwjgl.util.vector.Vector2f

class EscapedMemory(private val base: MemoryAPI): MemoryAPI by base {
    private fun escape(key: String?): String {
        return "\$$key"
    }

    override fun unset(p0: String?) {
        val key = escape(p0)
        return this.base.unset(key)
    }

    override fun expire(p0: String?, p1: Float) {
        val key = escape(p0)
        return this.base.expire(key, p1)
    }

    override fun contains(p0: String?): Boolean {
        val key = escape(p0)
        return this.base.contains(key)
    }

    override fun `is`(p0: String?, p1: Any?): Boolean {
        val key = escape(p0)
        return this.base.`is`(key, p1)
    }

    override fun `is`(p0: String?, p1: Float): Boolean {
        val key = escape(p0)
        return this.base.`is`(key, p1)
    }

    override fun `is`(p0: String?, p1: Boolean): Boolean {
        val key = escape(p0)
        return this.base.`is`(key, p1)
    }

    override fun set(p0: String?, p1: Any?) {
        val key = escape(p0)
        return this.base.set(key, p1)
    }

    override fun set(p0: String?, p1: Any?, p2: Float) {
        val key = escape(p0)
        return this.base.set(key, p1, p2)
    }

    override fun get(p0: String?): Any? {
        val key = escape(p0)
        return this.base.get(key)
    }

    override fun getString(p0: String?): String? {
        val key = escape(p0)
        return this.base.getString(key)
    }

    override fun getFloat(p0: String?): Float {
        val key = escape(p0)
        return this.base.getFloat(key)
    }

    override fun getBoolean(p0: String?): Boolean {
        val key = escape(p0)
        return this.base.getBoolean(key)
    }

    override fun getLong(p0: String?): Long {
        val key = escape(p0)
        return this.base.getLong(key)
    }

    override fun getVector2f(p0: String?): Vector2f? {
        val key = escape(p0)
        return this.base.getVector2f(key)
    }

    override fun getEntity(p0: String?): SectorEntityToken? {
        val key = escape(p0)
        return this.base.getEntity(key)
    }

    override fun getFleet(p0: String?): CampaignFleetAPI? {
        val key = escape(p0)
        return this.base.getFleet(key)
    }

    override fun between(p0: String?, p1: Float, p2: Float): Boolean {
        val key = escape(p0)
        return this.base.between(key, p1, p2)
    }

    override fun getExpire(p0: String?): Float {
        val key = escape(p0)
        return this.base.getExpire(key)
    }

    override fun addRequired(p0: String?, p1: String?) {
        val key = escape(p0)
        val req = escape(p1)
        return this.base.addRequired(key, req)
    }

    override fun removeRequired(p0: String?, p1: String?) {
        val key = escape(p0)
        val req = escape(p1)
        return this.base.removeRequired(key, req)
    }

    override fun getRequired(p0: String?): MutableSet<String>? {
        val key = escape(p0)
        return this.base.getRequired(key)
    }

    override fun removeAllRequired(p0: String?) {
        val key = escape(p0)
        return this.base.removeAllRequired(key)
    }

    override fun getInt(p0: String?): Int {
        val key = escape(p0)
        return this.base.getInt(key)
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> getList(key: String): List<T>? {
        return this.base.get(escape(key)) as? List<T>?
    }
}
