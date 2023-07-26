package com.price_of_command.conditions

import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.OptionPanelAPI
import com.fs.starfarer.api.campaign.TextPanelAPI
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.characters.SkillSpecAPI
import com.fs.starfarer.api.fleet.FleetMemberStatusAPI
import com.fs.starfarer.api.impl.campaign.ids.Sounds
import com.fs.starfarer.api.impl.campaign.rulecmd.SetStoryOption
import com.fs.starfarer.api.impl.campaign.rulecmd.SetStoryOption.StoryOptionParams
import com.price_of_command.*
import com.price_of_command.conditions.overrides.BaseMutator
import com.price_of_command.conditions.overrides.ConditionMutator
import com.price_of_command.fleet_interaction.AfterActionReport
import lunalib.lunaSettings.LunaSettings
import org.magiclib.kotlin.getRoundedValueMaxOneAfterDecimal
import org.magiclib.kotlin.isUnremovable
import kotlin.random.Random

private val INJURY_RATE
    get() = LunaSettings.getFloat(modID, "injury_rate")?.div(100) ?: 0.5f
private val INJURY_BASE
    get() = LunaSettings.getFloat(modID, "injury_duration") ?: 10f
private val INJURY_VARIANCE
    get() = LunaSettings.getFloat(modID, "injury_variance") ?: 4f
private val INJURY_RANGE = INJURY_VARIANCE * 2
private val INJURY_MIN = INJURY_BASE - INJURY_VARIANCE

private val EXTEND_RATE
    get() = LunaSettings.getFloat(modID, "injury_extension_rate")?.div(100) ?: 0.5f
private val DEATH_RATE
    get() = LunaSettings.getFloat(modID, "death_rate")?.div(100) ?: 0.1f

private val IGNORE_LIST = arrayOf(
    "aptitude_combat",
    "aptitude_leadership",
    "aptitude_technology",
    "aptitude_industry",
    "pc_injury",
    "pc_grave_injury",
    "pc_fatigue",
)

abstract class Wound(
    officer: PersonAPI,
    startDate: Long,
    rootConditions: List<Condition>,
    resolveOnDeath: Boolean = true,
    resolveOnMutation: Boolean = true
) : ResolvableCondition(
    officer, startDate, Duration.Time(generateDuration(startDate)), rootConditions, resolveOnDeath, resolveOnMutation
) {
    companion object {
        @JvmStatic
        fun generateDuration(seed: Long): Float = INJURY_MIN + Random(seed).nextFloat() * INJURY_RANGE
    }

    override fun pastTense(): String = "injured"
}

val sufferStrings = listOf(
    "was engulfed by an explosion on the bridge",
    "was hit by shrapnel from an internal compartment failure",
    "was burned by a fire that broke out on the bridge",
    "was thrown headfirst into a bank of monitors",
    "was electrocuted by a power surge to their console",
    "was shredded by spalling from an armor penetrating shot",
    "was disturbed by a major phase-space fluctuation",
    "suffered acute decompression due to a temporary loss of atmosphere"
)

open class Injury private constructor(
    officer: PersonAPI,
    startDate: Long,
    val injurySkillSuffix: Int,
    rootConditions: List<Condition>,
) : Wound(officer, startDate, rootConditions), AfterActionReportable {
    private var _skill: String? = null
    val skill: String
        get() = _skill ?: throw IllegalStateException("Injury Skill ID Not Set")
    private var _level: Int? = null
    val level: Int
        get() = _level ?: throw IllegalStateException("Injury Skill Level Not Set")
    private val skillSpec: SkillSpecAPI by lazy { settings().getSkillSpec(skill) }

    companion object {
        private val suffixRange = (1..7)

        fun pickInjurySuffix(officer: PersonAPI): Int {
            val taken = officer.conditions().filterIsInstance<Injury>().map { it.injurySkillSuffix }.toSet()
            val available = suffixRange.subtract(taken)
            return available.randomOrNull() ?: suffixRange.random()
        }
    }

    constructor(officer: PersonAPI, startDate: Long, rootConditions: List<Condition>) : this(
        officer, startDate, pickInjurySuffix(officer), rootConditions
    )

    constructor(officer: PersonAPI, skill: String, level: Int, startDate: Long, rootConditions: List<Condition>) : this(
        officer, startDate, rootConditions
    ) {
        _skill = skill
        _level = level
    }

    override fun tryResolve(): Boolean = super.tryResolve().then {
        target.stats.setSkillLevel(skill, level.toFloat())
        target.stats.decreaseSkill("pc_injury_$injurySkillSuffix")
    }

    private fun getEligibleSkills() = target.stats.skillsCopy.filter {
        !IGNORE_LIST.contains(it.skill.id) && it.level > 0 && !it.skill.isPermanent && (OfficerExpansionPlugin.vanillaSkills.contains(
            it.skill.id
        ) || OfficerExpansionPlugin.modSkillWhitelist.contains(it.skill.id) || it.skill.tags.contains(
            PoC_SKILL_WHITELIST_TAG
        ))
    }

    private fun invalidTarget(): Boolean = !validTarget()

    fun validTarget(): Boolean = target.canBeInjured()

    override fun precondition(): Outcome {
        if (invalidTarget()) return Outcome.NOOP
        val conditions = target.conditions()
        val skills = getEligibleSkills()
        if (conditions.any { it is Fatigue || it is Wound } || !Fatigue.fatigueEnabled()) {
            if (skills.isNotEmpty()) {
                if (ConditionManager.rand.nextFloat() <= INJURY_RATE) {
                    return Outcome.Applied(this)
                }
            } else {
                return Outcome.Failed
            }
        }

        return Outcome.NOOP
    }

    @NonPublic
    override fun inflict(): Outcome.Applied<Injury> {
        val skills = getEligibleSkills()
        val removed = skills.random()

        _skill = removed.skill.id
        _level = removed.level.toInt()

        target.stats.setSkillLevel("pc_fatigue", 0f)

        val injuries =
            target.conditions().filterIsInstance<Injury>().filter { it.injurySkillSuffix == injurySkillSuffix }.size + 1

        target.stats.setSkillLevel("pc_injury_$injurySkillSuffix", injuries.toFloat())

        target.stats.setSkillLevel(skill, 0f)

        return Outcome.Applied(this)
    }

    override fun failed(): Condition = ExtendWounds(target, startDate, extendRootConditions())

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
        val name = this.target.nameString

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

        if (outcome is Outcome.Terminal) {
            optionPanel.addOption(
                "Unfortunately, Officer $name succumbed to their wounds.", AfterActionReport.REMOVE_SELF
            )

            Death.setAvoidDeathStoryOption(outcome.condition, optionPanel, name, dialog, delegate, textPanel)
        } else {
            optionPanel.addOption(
                "Officer $name suffered an injury requiring an estimated ${this.duration.duration.getRoundedValueMaxOneAfterDecimal()} days to recover.",
                AfterActionReport.REMOVE_SELF
            )
            optionPanel.setTooltip(
                AfterActionReport.REMOVE_SELF,
                "This injury will render them unable to use their skill in ${skillSpec.name}"
            )
            optionPanel.setTooltipHighlights(
                AfterActionReport.REMOVE_SELF, skillSpec.name
            )

            optionPanel.addOption(
                "By some miracle, Officer $name managed to avoid being injured...",
                AfterActionReportable.AVOID_CONSEQUENCE
            )
            val storyOptionParams = StoryOptionParams(
                AfterActionReportable.AVOID_CONSEQUENCE,
                1,
                "avoidConsequence",
                Sounds.STORY_POINT_SPEND,
                "Saved Officer $name from injury"
            )
            SetStoryOption.set(dialog, storyOptionParams, delegate(storyOptionParams) {
                val token = if (target.conditions().filterIsInstance<Wound>().isNotEmpty()) {
                    "any further injuries"
                } else {
                    "being injured at all"
                }
                textPanel.addPara("It appears that despite the odds, Officer $name managed to avoid $token.")

                optionPanel.addOption(
                    "You hope their miraculous survival doesn't go to their head.", AfterActionReport.REMOVE_SELF
                )
                optionPanel.setTooltip(AfterActionReport.REMOVE_SELF, "Return to the rest of the report")
            })
        }

        return true
    }
}

class GraveInjury(target: PersonAPI, startDate: Long, rootConditions: List<Condition>) :
    Wound(target, startDate, rootConditions), AfterActionReportable {
    override fun tryResolve(): Boolean = super.tryResolve().then {
        // TODO inflict a scar when resolved
        target.stats.setSkillLevel("pc_grave_injury", 0f)
    }

    override fun precondition(): Outcome {
        if (target.conditions()
                .any { it is GraveInjury } && rootCondition !is Fatigue && ConditionManager.rand.nextFloat() >= (1 - DEATH_RATE)
        ) {
            return Outcome.Failed
        }
        return Outcome.Applied(this)
    }

    @NonPublic
    override fun inflict(): Outcome.Applied<GraveInjury> {
        target.conditions().filterIsInstance<GraveInjury>().firstOrNull()?.extendRandomly(this.startDate) ?: run {
            target.stats.setSkillLevel(
                "pc_grave_injury", 1f
            )
        }

        return Outcome.Applied(this)
    }

    override fun failed(): Condition = Death(target, startDate, rootConditions)

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
            "Officer $name suffered a grave injury requiring an estimated ${this.duration.duration.getRoundedValueMaxOneAfterDecimal()} days to recover.",
            AfterActionReport.REMOVE_SELF
        )
        optionPanel.setTooltip(
            AfterActionReport.REMOVE_SELF, "This Grave Injury has a chance of leaving a lasting Scar."
        )

        optionPanel.addOption(
            "By some miracle, Officer $name managed to avoid being injured...", AfterActionReportable.AVOID_CONSEQUENCE
        )
        val storyOptionParams = StoryOptionParams(
            AfterActionReportable.AVOID_CONSEQUENCE,
            1,
            "avoidConsequence",
            Sounds.STORY_POINT_SPEND,
            "Saved Officer $name from injury"
        )
        SetStoryOption.set(dialog, storyOptionParams, delegate(storyOptionParams) {
            val token = if (target.conditions().filterIsInstance<Wound>().isNotEmpty()) {
                "any further injuries"
            } else {
                "being injured at all"
            }
            textPanel.addPara("It appears that despite the odds, Officer $name managed to avoid $token.")

            optionPanel.addOption(
                "You hope their miraculous survival doesn't go to their head.", AfterActionReport.REMOVE_SELF
            )
            optionPanel.setTooltip(AfterActionReport.REMOVE_SELF, "Return to the rest of the report")
        })

        return true
    }

    override fun statusInReport(): String = "Gravely Injured"
}

class ExtendWounds private constructor(
    target: PersonAPI,
    startDate: Long,
    rootConditions: List<Condition>,
    private var reporter: BaseAfterActionReportable? = null
) : ResolvableCondition(target, startDate, Duration.Time(0f), rootConditions, resolveSilently = true),
    AfterActionReportable by reporter!! {
    private var previousDuration = 0f
    private lateinit var extended: Wound

    constructor(target: PersonAPI, startDate: Long, rootConditions: List<Condition>) : this(
        target, startDate, rootConditions, null
    )

    init {
        reporter = object : BaseAfterActionReportable() {
            override fun generateReport(
                dialog: InteractionDialogAPI,
                textPanel: TextPanelAPI,
                optionPanel: OptionPanelAPI,
                outcome: Outcome,
                shipStatus: FleetMemberStatusAPI,
                disabled: Boolean,
                destroyed: Boolean,
            ): Boolean {
                val name = target.nameString
                val remaining = this@ExtendWounds.extended.remaining().duration.getRoundedValueMaxOneAfterDecimal()

                textPanel.addPara("It appears that during the last encounter one of Officer $name's injuries was notably exacerbated.")
                textPanel.addPara("As such, their injury that would have taken ${this@ExtendWounds.previousDuration.getRoundedValueMaxOneAfterDecimal()} days to heal will now take $remaining days to heal.")

                optionPanel.addOption("Return to the rest of the report.", AfterActionReport.REMOVE_SELF)

                return false
            }

            override fun statusInReport(): String = "Injury Deteriorated"
        }
    }

    override fun precondition(): Outcome = if (ConditionManager.rand.nextFloat() <= EXTEND_RATE) {
        Outcome.Applied(this)
    } else {
        Outcome.Failed
    }

    override fun failed(): Condition = GraveInjury(target, startDate, extendRootConditions())

    private fun tryExtendWounds(target: PersonAPI): Outcome {
        val wounds = target.conditions().filterIsInstance<Wound>()

        return if (wounds.isNotEmpty()) {
            val extended = wounds.random()
            previousDuration = extended.remaining().duration
            extended.extendRandomly(ConditionManager.now)
            this.extended = extended
            Outcome.Applied(this)
        } else {
            Outcome.Failed
        }
    }

    @NonPublic
    override fun inflict(): Outcome = tryExtendWounds(target)

    override fun mutation(): ConditionMutator =
        BaseMutator(continuous = true, checkImmediately = false) { NullCondition(target, startDate) }

    override fun pastTense(): String = ""
}

fun PersonAPI.canBeInjured(): Boolean = !immuneToInjury()

fun PersonAPI.immuneToInjury(): Boolean = isAICore || isUnremovable() || isPlayer || tags.contains(PoC_OFFICER_IMMORTAL)
