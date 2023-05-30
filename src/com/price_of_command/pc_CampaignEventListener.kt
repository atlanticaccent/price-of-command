package com.price_of_command

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.BaseCampaignEventListener
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.combat.EngagementResultAPI
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.price_of_command.conditions.*

object pc_CampaignEventListener : BaseCampaignEventListener(false) {
    private const val RESTORE_FLEET_ASSIGNMENTS = "pc_restore_fleet_assignments"
    private const val FLEET_ASSIGNMENT_TO_RESTORE = "pc_fleet_assignments_to_restore"

    var restoreFleetAssignments: Boolean
        get() = Global.getSector().memoryWithoutUpdate.escape()[RESTORE_FLEET_ASSIGNMENTS] as? Boolean ?: true
        set(value) {
            Global.getSector().memoryWithoutUpdate.escape()[RESTORE_FLEET_ASSIGNMENTS] = value
        }
    @Suppress("UNCHECKED_CAST")
    var fleetAssignment: Map<FleetMemberAPI, PersonAPI>?
        get() = Global.getSector().memoryWithoutUpdate.escape()[FLEET_ASSIGNMENT_TO_RESTORE] as? Map<FleetMemberAPI, PersonAPI>
        set(value) {
            Global.getSector().memoryWithoutUpdate.escape()[FLEET_ASSIGNMENT_TO_RESTORE] = value
        }

    override fun reportPlayerEngagement(result: EngagementResultAPI) {
        val deployedPlayerOfficers = if (result.didPlayerWin()) {
            result.winnerResult
        } else {
            result.loserResult
        }.deployed.map { it.captain }.filter { it.faction.isPlayerFaction && !it.isPlayer }

        restoreFleetAssignments()

        for (officer in deployedPlayerOfficers) {
            if (!officer.isAICore) {
                val fatigue = Fatigue(officer, ConditionManager.now)
                val outcome = fatigue.tryInflictAppend()

                if (outcome is Outcome.Applied<*>) {
                    when (outcome.condition) {
                        is Fatigue -> logger().debug("Fatigued officer ${officer.nameString}")
                        is Injury -> logger().debug("Failed to fatigue officer ${officer.nameString}, injured them instead")
                        is GraveInjury -> logger().debug("Failed to fatigue or injure officer ${officer.nameString}, gravely injuring them instead")
                        is Wound -> logger().debug("Did not fatigue ${officer.nameString}, applied some kind of wound instead ${outcome.condition}")
                        else -> {
                            logger().debug("${officer.nameString} was not fatigued or injured when trying to fatigue. This is probably a bug")
                            logger().debug(outcome.condition)
                        }
                    }
                }
                // TODO: outcome can be terminal
            }
        }
    }

    fun restoreFleetAssignments() {
        val fleetAssignment = fleetAssignment
        if (restoreFleetAssignments && fleetAssignment != null) {
            playerFleet().fleetData.membersInPriorityOrder.forEach {
                it.captain = fleetAssignment[it]
                if (it.captain.isPlayer) {
                    it.isFlagship = true
                }
            }
        }
    }
}


