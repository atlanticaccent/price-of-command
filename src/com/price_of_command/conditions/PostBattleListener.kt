@file:Suppress("unused")

package com.price_of_command.conditions

import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.combat.EngagementResultAPI
import com.fs.starfarer.api.fleet.FleetMemberStatusAPI

enum class DeploymentStatus {
    Deployed, Disabled, Destroyed, Reserved
}

interface PostBattleListener {
    fun postBattleCondition(
        target: PersonAPI,
        resultAPI: EngagementResultAPI,
        won: Boolean,
        deployment: DeploymentStatus,
        status: FleetMemberStatusAPI?
    ): Condition?
}

class BasePostBattleListener(private val block: PBLLambda) : PostBattleListener {
    override fun postBattleCondition(
        target: PersonAPI,
        resultAPI: EngagementResultAPI,
        won: Boolean,
        deployment: DeploymentStatus,
        status: FleetMemberStatusAPI?
    ): Condition? = block(target, resultAPI, won, deployment, status)
}

typealias PBLLambda = (PersonAPI, EngagementResultAPI, Boolean, DeploymentStatus, FleetMemberStatusAPI?) -> Condition?
