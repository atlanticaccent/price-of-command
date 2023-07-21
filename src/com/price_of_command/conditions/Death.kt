package com.price_of_command.conditions

import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.OptionPanelAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.TextPanelAPI
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.fleet.FleetMemberStatusAPI
import com.fs.starfarer.api.impl.campaign.ids.Sounds
import com.fs.starfarer.api.impl.campaign.rulecmd.SetStoryOption
import com.fs.starfarer.api.util.Misc
import com.price_of_command.*
import com.price_of_command.fleet_interaction.AfterActionReport
import com.price_of_command.memorial.DeathData
import com.price_of_command.memorial.MemorialWall
import org.magiclib.kotlin.getRoundedValueMaxOneAfterDecimal

class Death(target: PersonAPI, startDate: Long, rootConditions: List<Condition>, var cause: String? = null) :
    Condition(target, startDate, rootConditions), AfterActionReportable {
    val conditionsOnDeath = target.conditions()
    val ship = target.ship()

    companion object {
        fun resurrect(target: PersonAPI, ship: FleetMemberAPI?) {
            target.removeTag(PoC_OFFICER_DEAD)
            playerFleet().fleetData.addOfficer(target)
            val deathData = MemorialWall.getMemorial().removeDeath(target)
            deathData?.conditionsOnDeath?.run {
                ConditionManager.appendCondition(target, this)
            }
            ship?.captain = target
        }

        fun setAvoidDeathStoryOption(
            condition: Death,
            optionPanel: OptionPanelAPI,
            name: String?,
            dialog: InteractionDialogAPI,
            delegate: SPDelegateFactory,
            textPanel: TextPanelAPI
        ) {
            val target = condition.target
            optionPanel.addOption("By some miracle, Officer $name escaped death...", AfterActionReportable.AVOID_DEATH)
            val storyOptionParams = SetStoryOption.StoryOptionParams(
                AfterActionReportable.AVOID_DEATH,
                2,
                "saveOfficer",
                Sounds.STORY_POINT_SPEND,
                "Saved Officer $name from death"
            )
            SetStoryOption.set(dialog, storyOptionParams, delegate(storyOptionParams) {
                target.resurrect(condition.ship)

                textPanel.addPara("It appears that, despite sustaining life threatening injuries, Officer $name managed to survive long enough for recovery teams to reach them.")
                val existingGraveInjury = target.conditions().filterIsInstance<GraveInjury>().firstOrNull()
                val graveInjury = GraveInjury(target, clock().timestamp, condition.extendRootConditions())
                if (existingGraveInjury != null) {
                    existingGraveInjury.expired = true
                    existingGraveInjury.resolveSilently = true
                    ConditionManager.tryResolve(existingGraveInjury)

                    textPanel.addPara("Your Chief Medical Officer reports that they have sustained a further Grave Injury from which they are expected to recover in ${graveInjury.duration.duration.getRoundedValueMaxOneAfterDecimal()} days.")
                } else {
                    textPanel.addPara("Your Chief Medical Officer reports that they have sustained a Grave Injury from which they are expected to recover in ${graveInjury.duration.duration.getRoundedValueMaxOneAfterDecimal()} days.")
                }
                textPanel.highlightFirstInLastPara(
                    graveInjury.duration.duration.getRoundedValueMaxOneAfterDecimal(), Misc.getHighlightColor()
                )

                graveInjury.tryInflictAppend()

                optionPanel.addOption(
                    "You wonder how much longer their luck can hold out....", AfterActionReport.REMOVE_SELF
                )
                optionPanel.setTooltip(AfterActionReport.REMOVE_SELF, "Return to the rest of the report")
            })
        }
    }

    override fun precondition(): Outcome = if (target.isPlayer) Outcome.NOOP
    else Outcome.Applied(this)

    @NonPublic
    override fun inflict(): Outcome = Outcome.Terminal(this)

    override fun pastTense(): String = "dead"

    fun toDeathData(ship: FleetMemberAPI?, location: SectorEntityToken, conditionsOnDeath: List<Condition>) =
        DeathData(target, clock().createClock(startDate), ship, location, cause, conditionsOnDeath)

    override fun generateReport(
        dialog: InteractionDialogAPI,
        textPanel: TextPanelAPI,
        optionPanel: OptionPanelAPI,
        outcome: Outcome,
        shipStatus: FleetMemberStatusAPI,
        disabled: Boolean,
        destroyed: Boolean,
        delegate: SPDelegateFactory
    ): Boolean {
        val name = target.nameString

        val (contextString, sufferString) = if (disabled || destroyed) "suffered critical damage and required the crew to abandon ship" to "was seen manning the conn to the last moment."
        else when (shipStatus.hullFraction * 100f) {
            in 0f..10f -> "took massive damage and was nearly disabled"
            in 10f..25f -> "took significant damage"
            in 25f..50f -> "was heavily damaged"
            in 50f..75f -> "was moderately damaged"
            in 75f..99f -> "was lightly damaged"
            else -> "took practically no damage"
        } to sufferStrings.random()

        textPanel.addPara("During the course of the battle, Officer ${target.possessive()} ship $contextString.")
        textPanel.addPara("$name $sufferString.")

        optionPanel.addOption(
            "Unfortunately, Officer $name succumbed to their wounds.", AfterActionReport.REMOVE_SELF
        )

        setAvoidDeathStoryOption(this, optionPanel, name, dialog, delegate, textPanel)

        return true
    }

    override fun statusInReport(): String = if (ConditionManager.rand.nextFloat() > 0.5) {
        "KIA"
    } else {
        "MIA"
    }
}

fun PersonAPI.resurrect(ship: FleetMemberAPI? = null) = Death.resurrect(this, ship)
