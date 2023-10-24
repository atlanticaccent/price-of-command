package com.price_of_command.platform.shared

import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.campaign.fleet.CampaignFleet

abstract class ShipPickerWrapper(val dialog: InteractionDialogAPI, val fleet: CampaignFleet, val dismiss: (Any, Any) -> Unit) {
    abstract fun reassign(target: Any)

    companion object
}