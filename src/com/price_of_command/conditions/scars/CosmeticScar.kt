package com.price_of_command.conditions.scars

import com.fs.starfarer.api.characters.LevelBasedEffect
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.characters.ShipSkillEffect
import com.fs.starfarer.api.characters.SkillSpecAPI
import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.impl.campaign.skills.BaseSkillEffectDescription
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.price_of_command.conditions
import com.price_of_command.conditions.Condition
import com.price_of_command.conditions.NonPublic
import com.price_of_command.conditions.Outcome

class CosmeticScar(target: PersonAPI, startDate: Long, rootConditions: List<Condition>) :
    Scar(target, startDate, rootConditions) {
    companion object : ScarFactory {
        override fun build(target: PersonAPI, startDate: Long, rootConditions: List<Condition>): Scar {
            return CosmeticScar(target, startDate, rootConditions)
        }
    }

    override fun tryResolve() {
        target.stats.setSkillLevel("pc_scar_cosmetic", 0f)
    }

    override fun precondition(): Outcome {
        return if (target.conditions().filterIsInstance<CosmeticScar>().isNotEmpty()) {
            Outcome.NOOP
        } else {
            Outcome.Applied(this)
        }
    }

    @NonPublic
    override fun inflict(): Outcome {
        target.stats.setSkillLevel("pc_scar_cosmetic", 1f)
        return Outcome.Applied(this)
    }

    override fun pastTense(): String = "physically scarred"

    class Level1 : BaseSkillEffectDescription(), ShipSkillEffect {
        override fun createCustomDescription(
            stats: MutableCharacterStatsAPI?,
            skill: SkillSpecAPI?,
            info: TooltipMakerAPI,
            width: Float
        ) {
            init(stats, skill)

            info.addPara("This officer has a physical scar that is impossible to hide.", 0f)
            info.addPara(
                "For the average spacer, this is nothing unusual. Many see physical scars as badges of pride, demonstrating a life devoted to the void.",
                2f
            )
            info.addPara("However, the average down-weller may flinch at the sight of such horrific injuries.", 2f)
            info.addPara("-1 Charisma", hc, 2f)
        }

        override fun apply(stats: MutableShipStatsAPI, hullSize: ShipAPI.HullSize?, id: String?, level: Float) = Unit

        override fun unapply(stats: MutableShipStatsAPI?, hullSize: ShipAPI.HullSize?, id: String?) = Unit
    }
}