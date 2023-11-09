package com.price_of_command.conditions.scars.personality_change

import com.fs.starfarer.api.GameState
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.BaseHullMod
import com.fs.starfarer.api.combat.HullModEffect
import com.fs.starfarer.api.combat.ShipAIConfig
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.mission.FleetSide
import com.price_of_command.conditions


const val PERSONALITY_CHANGE_HMOD_ID = "poc_personality_change_hmod"

class PersonalityChangeHullmod : HullModEffect by BaseHullMod() {
    override fun advanceInCombat(ship: ShipAPI?, amount: Float) {
        if (ship == null) return
        if (Global.getCurrentState() != GameState.COMBAT) return

        val enemyFleetManager = Global.getCombatEngine().getFleetManager(FleetSide.ENEMY)
        val enemyCommander = enemyFleetManager.fleetCommander ?: enemyFleetManager.defaultCommander ?: enemyFleetManager.allFleetCommanders.firstOrNull()
        enemyCommander?.let { commander ->
            val enemyFaction = commander.faction.id
            if (ship.customData?.get("${PERSONALITY_CHANGE_HMOD_ID}_${ship.id}") == null) {
                ship.captain.conditions().filterIsInstance<PersonalityChangeScar>().firstOrNull()?.let { scar ->
                    if (scar.factionId == enemyFaction) {
                        overrideAI(ship, scar.personalityId)
                        ship.setCustomData("${PERSONALITY_CHANGE_HMOD_ID}_${ship.id}", true)
                    }
                }
            }
        }
    }

    // Credit to borgrel's AutomatedCommands
    private fun overrideAI(ship: ShipAPI, personality: String) {
        //Generate new AI if one doesn't exist
        if (ship.shipAI == null) {
            val config = ShipAIConfig()
            config.personalityOverride = personality
            Global.getSettings().createDefaultShipAI(ship, config)
        } else {
            ship.shipAI.config.personalityOverride = personality
            ship.shipAI.forceCircumstanceEvaluation() //needed to make AI change personality?
        }
    }
}