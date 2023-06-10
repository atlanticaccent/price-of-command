package com.price_of_command

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.BaseCampaignEventListener
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.combat.EngagementResultAPI
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.impl.campaign.BattleAutoresolverPluginImpl
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

    override fun reportPlayerEngagement(resultAPI: EngagementResultAPI) {
        val (result, opposition) = if (resultAPI.didPlayerWin()) {
            resultAPI.winnerResult to resultAPI.loserResult.fleet.nameWithFactionKeepCase
        } else {
            resultAPI.loserResult to resultAPI.winnerResult.fleet.nameWithFactionKeepCase
        }
        val captainedShips = if (result is BattleAutoresolverPluginImpl.EngagementResultForFleetImpl) {
            result.deployed
        } else {
            result.allEverDeployedCopy.map { it.member }
        }.filter {
            it.captain.run {
                playerOfficers().containsPerson(this) && !isPlayer
            }
        }

        for (deployed in captainedShips) {
            val officer = deployed.captain
            val significantDamage = result.destroyed.contains(deployed) ||
                    result.disabled.contains(deployed) ||
                    deployed.status.hullFraction <= 0.2
            val condition = if (significantDamage) {
                ConditionManager.addPreconditionOverride(true) {
                    (it is Injury && it.target == officer).andThenOrNull {
                        it.precondition().noop { Outcome.Applied(it) }
                    }
                }
                Injury(officer, ConditionManager.now, emptyList())
            } else {
                Fatigue(officer, ConditionManager.now)
            }
            val outcome = condition.tryInflictAppend("Combat with $opposition")

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
            } else if (outcome is Outcome.Terminal) {
                logger().debug(outcome)
            }
        }

        tryRestoreFleetAssignments()
    }

    fun tryRestoreFleetAssignments() {
        val fleetAssignment = fleetAssignment
        if (restoreFleetAssignments && fleetAssignment != null) {
            playerFleet().fleetData.membersInPriorityOrder.forEach {
                val officer = fleetAssignment[it]
                if (officer != null && !officer.hasTag(PoC_OFFICER_DEAD)) {
                    it.captain = officer
                    if (it.captain.isPlayer) {
                        it.isFlagship = true
                    }
                } else {
                    it.captain = null
                }
            }
        }
    }
}


