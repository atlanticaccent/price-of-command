package com.price_of_command

import com.fs.starfarer.api.Global

const val modID = "price_of_command"
const val PoC_SKILL_WHITELIST_TAG = "pc_skill_opt_in"
const val PoC_OFFICER_DEAD = "pc_dead"
const val PoC_INCREASE_OFFICER_PROB_MULT = "pc_increase_officer_prob_mult"
const val PoC_OFFICER_IMMORTAL = "pc_immortal"

val CUSTOM_PANEL_WIDTH by lazy { Global.getSettings().screenWidth * (2f / 3f) }
val CUSTOM_PANEL_HEIGHT by lazy { Global.getSettings().screenHeight * (2f / 3f) }
