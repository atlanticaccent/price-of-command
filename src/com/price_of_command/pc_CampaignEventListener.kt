package com.price_of_command

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.BaseCampaignEventListener
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.PlanetAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.listeners.PlayerColonizationListener
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.combat.EngagementResultAPI
import com.fs.starfarer.api.impl.campaign.BattleAutoresolverPluginImpl
import com.fs.starfarer.api.impl.campaign.ids.Stats
import com.price_of_command.conditions.*
import com.price_of_command.conditions.overrides.ConditionMutator
import com.price_of_command.conditions.scars.personality_change.PersonalityChangeScar
import com.price_of_command.fleet_interaction.AfterActionReport
import lunalib.lunaSettings.LunaSettings
import kotlin.math.floor

private val FORCE_INJURY_HULL_DAMAGE
    get() = LunaSettings.getFloat(modID, "injury_force_damage_threshold")?.div(100)?.let { floor(1f - it) } ?: 0.2f
private val FORCE_INJURY_TOGGLE
    get() = LunaSettings.getBoolean(modID, "injury_force_damage_toggle") ?: true

private const val RESTORE_FLEET_ASSIGNMENTS = "pc_restore_fleet_assignments"
private const val FLEET_ASSIGNMENT_TO_RESTORE = "pc_fleet_assignments_to_restore"
class pc_CampaignEventListener : BaseCampaignEventListener(false), PlayerColonizationListener {
    companion object {
        var restoreFleetAssignments: Boolean
            get() = Global.getSector().memoryWithoutUpdate.escape()[RESTORE_FLEET_ASSIGNMENTS] as? Boolean ?: true
            set(value) {
                Global.getSector().memoryWithoutUpdate.escape()[RESTORE_FLEET_ASSIGNMENTS] = value
            }

        @Suppress("UNCHECKED_CAST")
        private var oldFleetAssignments: Map<String, PersonAPI?>?
            get() {
                return Global.getSector().memoryWithoutUpdate.escape()[FLEET_ASSIGNMENT_TO_RESTORE] as? Map<String, PersonAPI?>
            }
            set(value) {
                Global.getSector().memoryWithoutUpdate.escape()[FLEET_ASSIGNMENT_TO_RESTORE] = value
            }

        fun tryRestoreFleetAssignments(override: Boolean = false) {
            val fleetAssignment = oldFleetAssignments
            if ((restoreFleetAssignments || override) && fleetAssignment != null) {
                playerFleet().fleetData.membersInPriorityOrder.forEach {
                    val officer = fleetAssignment[it.id]
                    if (officer != null && !officer.hasTag(PoC_OFFICER_DEAD)) {
                        it.captain = officer
                        if (it.captain.isPlayer) {
                            it.isFlagship = true
                        } else {
                            it.isFlagship = false
                        }
                    } else {
                        it.captain = null
                        if (it.isFlagship) {
                            it.isFlagship = false
                        }
                    }
                }
            }
        }
    }

    private fun currentFleetAssignments(): Map<String, PersonAPI?> {
        return playerFleet().fleetData.membersListCopy.mapNotNull { it.id to it.captain }.toMap()
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
                playerOfficers().containsPerson(this) && !isPlayer && !isAICore
            }
        }

        var fatigueMutator: ConditionMutator? = null
        var appliedConditions = emptyList<Outcome.WithCondition<*>>()
        for (deployed in captainedShips) {
            val officer = deployed.captain
            val significantDamage =
                result.destroyed.contains(deployed) || result.disabled.contains(deployed) || (FORCE_INJURY_TOGGLE && deployed.status.hullFraction <= FORCE_INJURY_HULL_DAMAGE)

            val condition = if (significantDamage && officer.canBeInjured()) {
                val injury = Injury(officer, ConditionManager.now, emptyList())
                ConditionManager.addPreconditionOverride(true, Int.MAX_VALUE) {
                    (it == injury && injury.validTarget()).andThenOrNull {
                        it.precondition().noop { Outcome.Applied(it) }
                    }
                }
                injury
            } else {
                val fatigue = Fatigue(officer, ConditionManager.now)
                fatigueMutator = object : ConditionMutator() {
                    override fun priority(): Int = Int.MAX_VALUE

                    override fun mutate(condition: Condition): Condition? {
                        if (condition is Wound && condition.rootCondition == fatigue) {
                            this.complete = true
                            if (officer.conditions().filterIsInstance<Wound>()
                                    .isEmpty() && ConditionManager.rand.nextFloat() <= deployed.stats.crewLossMult.modifiedValue
                            ) {
                                return fatigue
                            }
                        }
                        return null
                    }
                }
                ConditionManager.addMutationOverride(fatigueMutator)
                fatigue
            }
            val outcome = condition.tryInflictAppend("Combat with $opposition", true)
            if (fatigueMutator != null) {
                ConditionManager.removeMutationOverride(fatigueMutator)
            }

            if (outcome is Outcome.Applied<*>) {
                when (outcome.condition) {
                    is Fatigue -> logger().debug("Fatigued officer ${officer.nameString}")
                    is Injury -> logger().debug("Failed to fatigue officer ${officer.nameString}, injured them instead")
                    is GraveInjury -> {
                        logger().debug("Failed to fatigue or injure officer ${officer.nameString}, gravely injuring them instead")
                        PersonalityChangeScar.generateApplicable(resultAPI, officer).let {
                            outcome.condition.scar(it)
                        }
                    }

                    is Wound -> logger().debug("Did not fatigue ${officer.nameString}, applied some kind of wound instead ${outcome.condition}")
                    else -> {
                        logger().debug("${officer.nameString} was not fatigued or injured when trying to fatigue. This is probably a bug")
                        logger().debug(outcome.condition)
                    }
                }
            } else if (outcome is Outcome.Terminal) {
                logger().debug(outcome)
            }

            if (outcome is Outcome.WithCondition<*> && (outcome is Outcome.Applied<*> || outcome is Outcome.Terminal)) {
                appliedConditions = appliedConditions.plus(outcome)
            }
        }

        for (officer in playerOfficers()) {
            val target = officer.person
            val ship = target.ship()
            val status = when (ship) {
                in result.disabled -> DeploymentStatus.Disabled
                in result.destroyed -> DeploymentStatus.Destroyed
                in captainedShips -> DeploymentStatus.Deployed
                else -> DeploymentStatus.Reserved
            }
            val conditions = ConditionManager.postBattleListeners.mapNotNull {
                it.postBattleCondition(
                    target, resultAPI, resultAPI.didPlayerWin(), status, ship?.status
                )?.tryInflictAppend("Combat with $opposition", true)
            }.filterIsInstance<Outcome.WithCondition<*>>()
            appliedConditions = appliedConditions.plus(conditions)
        }

        appliedConditions.mapNotNull { outcome ->
            val condition = outcome.condition
            if (condition is AfterActionReportable) {
                val ship = (condition as? Death)?.ship ?: captainedShips.first { ship ->
                    ship.captain == condition.target
                }
                AfterActionReport.ReportData(
                    condition, outcome, ship.status, result.destroyed.contains(ship), result.disabled.contains(ship)
                )
            } else {
                null
            }
        }.ifEmpty { null }?.run {
            ConditionManager.afterActionReport?.let {
                ConditionManager.afterActionReport = AfterActionReport(it.mergeUndecided(this))
            } ?: run {
                ConditionManager.afterActionReport = AfterActionReport(this)
            }
        }

        tryRestoreFleetAssignments()
    }

    override fun reportShownInteractionDialog(dialog: InteractionDialogAPI) {
        oldFleetAssignments = currentFleetAssignments()
    }

    override fun reportPlayerOpenedMarket(market: MarketAPI) {
        market.stats.dynamic.getMod(
            Stats.OFFICER_PROB_MOD
        ).modifyFlat(PoC_INCREASE_OFFICER_PROB_MULT, 0.4f)
    }

    override fun reportPlayerAbandonedColony(m: MarketAPI) = Unit

    override fun reportPlayerColonizedPlanet(planet: PlanetAPI) {
        planet.market.stats.dynamic.getMod(
            Stats.OFFICER_PROB_MOD
        ).modifyFlat(PoC_INCREASE_OFFICER_PROB_MULT, 0.4f)
    }
}
