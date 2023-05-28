package com.price_of_command.skills

import com.fs.starfarer.api.characters.DescriptionSkillEffect
import com.fs.starfarer.api.impl.campaign.skills.AptitudeDesc
import com.fs.starfarer.api.util.Misc
import java.awt.Color

class pc_AptitudeConditionDesc {
    class Level0: DescriptionSkillEffect by AptitudeDesc.Level0() {
        override fun getString(): String = "    - Skills in this category represent the overall status of an officer\n" +
                "    - Each skill generally indicates some condition influencing the performance of this officer\n" +
                "    - The most common is Fatigue, suffered by officers following combat and which puts them at greater risk of Injury\n" +
                "    - Injuries deprive officers of skills. You can think of their skills (or level) as their total Health Points\n" +
                "        - An officer that can take no more injuries is at risk of dying! Permanently!\n" +
                "            - You can't die, of course. Funny, that....\n" +
                "    - As you continue your journey, you make discover other statuses that can affect your officers."

        override fun getHighlightColors(): Array<Color> {
            val highlight = Misc.getHighlightColor()
            val negative = Misc.getNegativeHighlightColor()
            return arrayOf(highlight, highlight, highlight, highlight, negative)
        }

        override fun getHighlights(): Array<String> = arrayOf("Fatigue", "Injury", "Injuries", "skills", "dying! Permanently!")
    }
}