package com.price_of_command.memorial

import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin
import com.fs.starfarer.api.campaign.CustomVisualDialogDelegate
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.ui.Alignment
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.util.Misc
import com.price_of_command.*
import lunalib.lunaExtensions.addLunaSpriteElement
import lunalib.lunaUI.elements.LunaSpriteElement
import org.lwjgl.input.Keyboard

class DeathMessage(private val deathData: DeathData, dialog: InteractionDialogAPI) : CustomVisualDialogDelegate {
    companion object {
        const val CLOSE = "close"
        const val GOTO = "goto"
        const val BUTTON_WIDTH_MULT = 1.5f
    }

    private val plugin = DeathMessageCustomUIPanelPlugin(dialog)

    override fun init(
        customPanelAPI: CustomPanelAPI,
        callbacks: CustomVisualDialogDelegate.DialogCallbacks
    ) {
        val info = customPanelAPI.createUIElement(CUSTOM_PANEL_WIDTH, CUSTOM_PANEL_HEIGHT, false)
        customPanelAPI.addUIElement(info).inTL(0f, 0f)

        val titleTooltip = info.beginSubTooltip(CUSTOM_PANEL_WIDTH)
        titleTooltip.setTitleOrbitronVeryLarge()
        val title = titleTooltip.addTitle("In Memoriam")
        title.setAlignment(Alignment.MID)
        title.italicize()
        info.endSubTooltip()
        info.addCustom(titleTooltip, CUSTOM_PANEL_HEIGHT / 5f)

        info.addSpacer(10f)

        val spriteTooltip = info.beginSubTooltip(CUSTOM_PANEL_WIDTH)
        val sprite = spriteTooltip.addLunaSpriteElement(
            deathData.person.portraitSprite,
            LunaSpriteElement.ScalingTypes.STRETCH_SPRITE,
            CUSTOM_PANEL_HEIGHT / 3f,
            CUSTOM_PANEL_HEIGHT / 3f
        ).constrainWithRatio(CUSTOM_PANEL_HEIGHT / 3f)
        sprite.position.setXAlignOffset((CUSTOM_PANEL_WIDTH / 2f) - (sprite.width / 2f))
        sprite.borderColor = Misc.getDarkPlayerColor()
        sprite.renderBorder = true
        info.endSubTooltip()
        info.addCustom(spriteTooltip, 0f)

        info.setParaOrbitronVeryLarge()
        info.addPara("Officer ${deathData.person.nameString}", 10f).setAlignment(Alignment.MID)
        info.addPara("They will be missed", 10f).setAlignment(Alignment.MID)
        info.addPara("Unknown - ${deathData.date.dateString}", 10f).setAlignment(Alignment.MID)

        info.setButtonFontOrbitron20()
        val closeText = "Close"
        val closeButton =
            info.addButton(
                closeText,
                CLOSE,
                (info.computeStringWidth(closeText) + info.computeStringWidth("[Esc]")) * BUTTON_WIDTH_MULT + 10f,
                22f,
                0f
            )
        closeButton.setShortcut(Keyboard.KEY_ESCAPE, false)
        info.addCustomDoNotSetPosition(closeButton).position.inBR(10f, -CUSTOM_PANEL_HEIGHT + 10f)
        val gotoText = "Go to Memorial Wall"
        val openButton = info.addButton(gotoText, GOTO, info.computeStringWidth(gotoText) * BUTTON_WIDTH_MULT, 22f, 0f)
        info.addCustomDoNotSetPosition(openButton).position.leftOfMid(closeButton, 10f)
    }

    override fun getCustomPanelPlugin(): CustomUIPanelPlugin = plugin

    override fun getNoiseAlpha(): Float = 0f

    override fun advance(amount: Float) = Unit

    override fun reportDismissed(option: Int) {

    }

    class DeathMessageCustomUIPanelPlugin(private val dialog: InteractionDialogAPI) : BaseCustomUIPanelPlugin() {
        override fun buttonPressed(buttonId: Any) {
            logger().debug(buttonId)
            when (buttonId) {
                GOTO -> {
                    dialog.dismiss()
                    ConditionManager.showMemorialWallNextFrame()
                }

                CLOSE -> dialog.dismiss()
            }
        }

//        override fun processInput(events: MutableList<InputEventAPI>) {
//            super.processInput(events)
//            if (events.any { it.isKeyUpEvent && it.eventValue == Keyboard.KEY_ESCAPE })
//        }
    }
}
