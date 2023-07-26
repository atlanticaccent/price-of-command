package com.price_of_command.memorial

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignClockAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.ui.Alignment
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.SectorMapAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import com.price_of_command.addToManager
import com.price_of_command.conditions.Condition
import com.price_of_command.constrainWithRatio
import com.price_of_command.relfection.ReflectionUtils
import lunalib.lunaExtensions.addLunaSpriteElement
import lunalib.lunaUI.elements.LunaElement
import lunalib.lunaUI.elements.LunaSpriteElement
import org.magiclib.kotlin.getPersonalityName
import kotlin.math.absoluteValue

class MemorialWall private constructor() : BaseIntelPlugin() {
    companion object {
        const val MEMORIAL_INTEL_TAG = "Memorial Wall"
        const val portraitHeight = 96f
        const val rowHeight = 144f
        const val topOffset = -8f
        private const val MEMORIAL_ENTITY_ID = "pc_memorial_entity"

        @JvmStatic
        fun getMemorial(): MemorialWall {
            val manager = Global.getSector().intelManager
            return if (manager.hasIntelOfClass(MemorialWall::class.java)) {
                manager.getFirstIntel(MemorialWall::class.java) as MemorialWall
            } else {
                val memorial = MemorialWall()
                memorial.addToManager()
                memorial
            }
        }
    }

    private var theFallen: Map<DeathData, DisplayData> = emptyMap()

    fun addDeath(death: DeathData) {
        theFallen = theFallen.plus(death to DisplayData.DETAILS)
    }

    fun addDeath(
        person: PersonAPI,
        deathDate: CampaignClockAPI,
        ship: FleetMemberAPI?,
        deathLocation: SectorEntityToken?,
        causeOfDeath: Condition,
        conditionsOnDeath: List<Condition>,
    ) {
        addDeath(DeathData(person, deathDate, ship, deathLocation, causeOfDeath.pastTense(), conditionsOnDeath))
    }

    fun removeDeath(person: PersonAPI): DeathData? {
        val deathData = theFallen.keys.firstOrNull { it.person == person }
        return if (deathData != null) {
            theFallen = theFallen.minus(deathData)
            deathData
        } else {
            null
        }
    }

    override fun getIntelTags(map: SectorMapAPI): Set<String> = super.getIntelTags(map).plus(MEMORIAL_INTEL_TAG)

    override fun createIntelInfo(info: TooltipMakerAPI, mode: IntelInfoPlugin.ListInfoMode) {
        info.addPara("Memorial Wall", 0f)
    }

    override fun hasLargeDescription(): Boolean = true

    override fun createLargeDescription(panel: CustomPanelAPI, maxWidth: Float, height: Float) {
        val width = maxWidth * (2f / 3f)
        ReflectionUtils.invoke("setWidth", panel, width)
        val info = panel.createUIElement(width, height, true)
        val maxTitleWidth = theFallen.keys.maxOfOrNull { info.computeStringWidth(it.person.nameString) * 1.5f } ?: 0f

        for ((deathData, displayData) in theFallen) {
            val portrait = info.addLunaSpriteElement(
                deathData.person.portraitSprite, LunaSpriteElement.ScalingTypes.NONE, portraitHeight, portraitHeight
            ).constrainWithRatio(portraitHeight)
            if (portrait.position.x != 0f) {
                portrait.position.setXAlignOffset(-portrait.position.x)
            }

            val mid = info.beginSubTooltip(width - portrait.width)
            mid.setTitleOrbitronLarge()
            val title = mid.addTitle(deathData.person.nameString)
            val titleAnchor = mid.addSpacer(0f)
            mid.addSpacer(6f)

            val heightSoFar = mid.heightSoFar

            val detailsCheckWidth = info.computeStringWidth("Details") + 18f
            val detailsCheckboxTooltip = mid.beginSubTooltip(detailsCheckWidth)
            val detailsAreaCheckbox = AreaCheckbox(
                detailsCheckboxTooltip, detailsCheckWidth, 20f, "Details", displayData == DisplayData.DETAILS
            )
            mid.endSubTooltip()
            mid.addCustomDoNotSetPosition(detailsCheckboxTooltip).position.rightOfTop(titleAnchor, maxTitleWidth)
                .setYAlignOffset(title.position.height - 2f)

            val tooltip = when (displayData) {
                DisplayData.DETAILS -> {
                    val detailsTooltip = mid.beginSubTooltip(width - portrait.width)
                    detailsTooltip.addPara(
                        "Personality: ${deathData.person.getPersonalityName()}",
                        0f,
                        Misc.getHighlightColor(),
                        deathData.person.getPersonalityName()
                    )
                    detailsTooltip.addPara(
                        "Died: ${deathData.date.shortDate}", 0f, Misc.getHighlightColor(), deathData.date.shortDate
                    )
                    detailsTooltip.addPara(
                        "Cause of Death: ${deathData.causeOfDeath()}",
                        0f,
                        Misc.getHighlightColor(),
                        deathData.causeOfDeath()
                    )
                    detailsTooltip.addPara(
                        "Place of Death: ${deathData.placeOfDeath()}",
                        0f,
                        Misc.getHighlightColor(),
                        deathData.placeOfDeath()
                    )
                    mid.endSubTooltip()
                    mid.addCustom(detailsTooltip, 0f)

                    detailsTooltip
                }

                DisplayData.SKILLS -> {
                    val skillTooltip = mid.beginSubTooltip(width - portrait.width)
                    skillTooltip.addSkillPanel(deathData.person, 0f)
                    mid.endSubTooltip()
                    mid.addCustom(skillTooltip, 0f)

                    skillTooltip
                }

                DisplayData.SHIP -> {
                    val shipTooltip = mid.beginSubTooltip(width - portrait.width)
                    shipTooltip.addShipList(1, 1, rowHeight, Misc.getBasePlayerColor(), listOf(deathData.ship), 0f)
                    mid.endSubTooltip()
                    mid.addCustom(shipTooltip, 0f)

                    shipTooltip
                }
            }

            val skillCheckWidth = info.computeStringWidth("Skills") + 12f
            val skillCheckboxTooltip = mid.beginSubTooltip(skillCheckWidth)
            val skillAreaCheckbox = AreaCheckbox(
                skillCheckboxTooltip, skillCheckWidth, 20f, "Skills", displayData == DisplayData.SKILLS
            )
            mid.endSubTooltip()
            mid.addCustomDoNotSetPosition(skillCheckboxTooltip).position.rightOfMid(detailsCheckboxTooltip, 4f)

            var shipAreaCheckbox: AreaCheckbox? = null
            if (deathData.ship != null) {
                val shipCheckWidth = info.computeStringWidth("Final Ship") + 12f
                val shipCheckboxTooltip = mid.beginSubTooltip(shipCheckWidth)
                shipAreaCheckbox = AreaCheckbox(
                    shipCheckboxTooltip, shipCheckWidth, 20f, "Final Ship", displayData == DisplayData.SHIP
                )
                mid.endSubTooltip()
                mid.addCustomDoNotSetPosition(shipCheckboxTooltip).position.rightOfMid(skillCheckboxTooltip, 4f)

                shipAreaCheckbox.onClick {
                    detailsAreaCheckbox.toggleValAndBackground(false)
                    skillAreaCheckbox.toggleValAndBackground(false)
                    theFallen = theFallen.plus(deathData to DisplayData.SHIP)
                    panel.intelUI.updateUIForItem(this)
                }
            }

            detailsAreaCheckbox.onClick {
                skillAreaCheckbox.toggleValAndBackground(false)
                shipAreaCheckbox?.toggleValAndBackground(false)
                theFallen = theFallen.plus(deathData to DisplayData.DETAILS)
                panel.intelUI.updateUIForItem(this)
            }

            skillAreaCheckbox.onClick {
                detailsAreaCheckbox.toggleValAndBackground(false)
                shipAreaCheckbox?.toggleValAndBackground(false)
                theFallen = theFallen.plus(deathData to DisplayData.SKILLS)
                panel.intelUI.updateUIForItem(this)
            }

            val currentHeight = topOffset.absoluteValue + heightSoFar + tooltip.heightSoFar
            if (currentHeight > portrait.height) {
                info.addSpacer(currentHeight - portrait.height)
            }

            info.endSubTooltip()
            info.addCustomDoNotSetPosition(mid).position.rightOfTop(portrait.elementPanel, 6f)
            val divider = info.createRect(Misc.getBrightPlayerColor(), 1f)
            info.addCustom(divider, 0f).position.setSize(width, 1f)
            info.addSpacer(6f)
        }

        panel.addUIElement(info)
    }

    override fun getIcon(): String = Global.getSettings().getSpriteName("pc_intel", "memorial")

    override fun hasImportantButton(): Boolean = false

    override fun shouldRemoveIntel(): Boolean = false

    override fun isNew(): Boolean = false
}

class AreaCheckbox private constructor(
    tooltipMakerAPI: TooltipMakerAPI, width: Float, height: Float, var value: Boolean
) : LunaElement(tooltipMakerAPI, width, height) {
    constructor(tooltipMakerAPI: TooltipMakerAPI, width: Float, height: Float, text: String, default: Boolean) : this(
        tooltipMakerAPI,
        width,
        height,
        default,
    ) {
        this.innerElement.addSpacer(1f)
        this.addText(text)
        this.paragraph?.apply {
            setAlignment(Alignment.MID)
            setColor(Misc.getBrightPlayerColor())
        }
        toggleBackground()
        this.backgroundColor = Misc.getDarkPlayerColor()
    }

    override fun onClick(input: InputEventAPI) {
        super.onClick(input)
        toggleValAndBackground(!value)
    }

    override fun onHoverEnter(input: InputEventAPI) {
        super.onHoverEnter(input)
        this.renderBackground = true
        this.backgroundColor = Misc.getDarkPlayerColor().brighter()
    }

    override fun onHoverExit(input: InputEventAPI) {
        super.onHoverExit(input)
        toggleBackground()
        this.backgroundColor = Misc.getDarkPlayerColor()
    }

    fun toggleValAndBackground(new: Boolean) {
        value = new
        toggleBackground()
    }

    private fun toggleBackground() {
        this.renderBackground = value
    }
}

enum class DisplayData {
    DETAILS, SKILLS, SHIP
}
