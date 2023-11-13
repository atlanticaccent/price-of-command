package com.price_of_command.conditions

import com.fs.starfarer.api.characters.MutableCharacterStatsAPI
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.characters.ShipSkillEffect
import com.fs.starfarer.api.characters.SkillSpecAPI
import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.MutableStat
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.StatBonus
import com.fs.starfarer.api.impl.campaign.skills.BaseSkillEffectDescription
import com.fs.starfarer.api.ui.TooltipMakerAPI

@Suppress("SpellCheckingInspection")
class LowMorale(
    target: PersonAPI,
    startDate: Long,
    duration: Duration,
    rootConditions: List<Condition>
) : ResolvableCondition(
    target,
    startDate,
    duration,
    rootConditions,
    resolveSilently = true,
    resolveSilentlyOnMutation = true
) {
    override fun tryResolve() {
        target.stats.setSkillLevel("pc_low_morale", 0f)
    }

    override fun precondition(): Outcome = Outcome.Applied(this)

    @NonPublic
    override fun inflict(): Outcome {
        target.stats.setSkillLevel("pc_low_morale", 1f)
        return Outcome.Applied(this)
    }

    override fun pastTense(): String = "demoralised"

    class Skill : BaseSkillEffectDescription(), ShipSkillEffect {
        override fun createCustomDescription(
            stats: MutableCharacterStatsAPI,
            skill: SkillSpecAPI?,
            info: TooltipMakerAPI,
            width: Float
        ) {
            init(stats, skill)

            info.addPara("This officer is suffering from Low Morale.", 0f)
            info.addPara("As such, poor command of this ship results in:", 2f)
            info.setBulletedListMode("    - ")
            info.addPara("Peak CR Duration reduced by 50%", hc, 2f)
            info.addPara("Autofire Weapon Accuracy reduced by 50%", hc, 2f)
            info.addPara("Crew loses increased by 100%", hc, 2f)
            info.addPara("Max speed reduced by 30%", hc, 2f)
            info.addPara("Weapon malfunction chance increased by 100%", hc, 2f)
            info.setBulletedListMode(null)

            info.addPara("You can restore their morale by:", 2f)

        }

        sealed class StatWrapper {
            class Mutable(private val stat: MutableStat) : StatWrapper() {
                override fun modifyMult(source: String, value: Float) = stat.modifyMult(source, value)
                override fun unmodifyMult(source: String) = stat.unmodifyMult(source)
            }
            class Bonus(private val stat: StatBonus) : StatWrapper() {
                override fun modifyMult(source: String, value: Float) = stat.modifyMult(source, value)
                override fun unmodifyMult(source: String) = stat.unmodifyMult(source)
            }

            abstract fun modifyMult(source: String, value: Float)

            abstract fun unmodifyMult(source: String)
        }

        private fun statsToNerf(stats: MutableShipStatsAPI) : Map<StatWrapper, Float> = mapOf(
            StatWrapper.Bonus(stats.peakCRDuration) to 0.5f,
            StatWrapper.Mutable(stats.autofireAimAccuracy) to 0.7f,
            StatWrapper.Mutable(stats.crewLossMult) to 2f,
            StatWrapper.Mutable(stats.maxSpeed) to 0.7f,
            StatWrapper.Mutable(stats.weaponMalfunctionChance) to 2f
        )

        override fun apply(stats: MutableShipStatsAPI, hullSize: ShipAPI.HullSize?, id: String?, level: Float) {
            statsToNerf(stats).forEach { (stat, mult) ->
                stat.modifyMult("pc_low_morale", mult)
            }
        }

        override fun unapply(stats: MutableShipStatsAPI, hullSize: ShipAPI.HullSize?, id: String?) {
            statsToNerf(stats).forEach { (stat, _) ->
                stat.unmodifyMult("pc_low_morale")
            }
        }
    }
}
