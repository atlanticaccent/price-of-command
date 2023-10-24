package com.price_of_command.platform

import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.campaign.fleet.CampaignFleet
import com.price_of_command.forPlatform
import com.price_of_command.platform.linux.LinuxShipPickerWrapper
import com.price_of_command.platform.macos.MacShipPickerWrapper
import com.price_of_command.platform.shared.ShipPickerWrapper
import com.price_of_command.platform.windows.WindowsShipPickerWrapper

fun ShipPickerWrapper.Companion.reassign(target: Any, inner: Any, dialog: InteractionDialogAPI, fleet: CampaignFleet, dismiss: (Any, Any) -> Unit) {
    forPlatform(
        { WindowsShipPickerWrapper(inner, dialog, fleet, dismiss) },
        { LinuxShipPickerWrapper(inner, dialog, fleet, dismiss) },
        { MacShipPickerWrapper(inner, dialog, fleet, dismiss) },
    ).reassign(target)
}
