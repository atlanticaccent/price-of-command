package com.laird

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.BaseCampaignEventListener
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.combat.EngagementResultAPI
import lunalib.lunaExtensions.getList
import kotlin.random.Random

class PostBattleListener : BaseCampaignEventListener(false) {
    companion object {
        const val INJURY_RATE = 0.5
        const val INJURY_TIME = 10
        const val FATIGUE_BASE = 5f
        const val FATIGUE_VARIANCE = 2f
        const val FATIGUE_RANGE = FATIGUE_VARIANCE * 2
        const val FATIGUE_MIN = FATIGUE_BASE - FATIGUE_VARIANCE
        const val FATIGUE_MAX = FATIGUE_BASE + FATIGUE_VARIANCE
        private val IGNORE_LIST = arrayOf(
            "aptitude_combat",
            "aptitude_leadership",
            "aptitude_technology",
            "aptitude_industry",
            "oe_injury",
            "oe_fatigue",
        )

        fun injureOfficer(officer: PersonAPI, when_injured: Long): Boolean {
            val mem = officer.escapedWithoutUpdate()

            Global.getSector().persistentData
            val injuries = mem.getInt(INJURIES) + 1
            mem.set(INJURIES, injuries)
            mem.set(INJURED_FROM, when_injured)

            val skills = officer.stats.skillsCopy.filter { !IGNORE_LIST.contains(it.skill.id) && it.level > 0 }
            return if (skills.isNotEmpty()) {
                val remove = skills.random()

                logger().debug("Removing ${remove.skill.name}")
                officer.id
                officer.stats.setSkillLevel(remove.skill.id, 0f)
                val disabledSkills: MutableList<String> = mem.getList<String>(DISABLED_SKILLS)?.toMutableList() ?: mutableListOf()
                disabledSkills.add(remove.skill.id)
                mem.set(DISABLED_SKILLS, disabledSkills)

                officer.stats.setSkillLevel("oe_injury", 1f)
                true
            } else {
                false
            }
        }
    }

    override fun reportPlayerEngagement(result: EngagementResultAPI) {
        val deployedPlayerOfficers = if (result.didPlayerWin()) {
            result.winnerResult
        } else {
            result.loserResult
        }.deployed.map { it.captain }.filter { it.faction.isPlayerFaction }

        val now = Global.getSector().clock.timestamp
        val rand = Random(now)
        for (officer in deployedPlayerOfficers) {
            val mem = officer.escapedWithoutUpdate()
            if (mem.contains(FATIGUED_FROM) || mem.contains(INJURED_FROM)) {
                if (rand.nextFloat() >= INJURY_RATE) {
                    injureOfficer(officer, now)
                }
            }

            val variedFatigue = FATIGUE_MIN + rand.nextFloat() * FATIGUE_VARIANCE
            mem.set(FATIGUED_FROM, now)
            mem.expire(FATIGUED_FROM, variedFatigue)
            officer.stats.setSkillLevel("oe_fatigue", 1f)
        }
    }
}