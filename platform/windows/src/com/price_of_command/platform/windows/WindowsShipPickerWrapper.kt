package com.price_of_command.platform.windows

import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.campaign.fleet.CampaignFleet
import com.fs.starfarer.campaign.fleet.FleetMember
import com.fs.starfarer.campaign.ui.S
import com.fs.starfarer.coreui.CaptainPickerDialog
import com.fs.starfarer.ui.U
import com.fs.starfarer.ui.newui.T
import com.fs.starfarer.ui.o000
import com.price_of_command.platform.shared.ReflectionUtils
import com.price_of_command.platform.shared.ShipPickerWrapper

class WindowsShipPickerWrapper(
    private val inner: T, dialog: InteractionDialogAPI, fleet: CampaignFleet, dismiss: (Any, Any) -> Unit
) : U by inner, S.o, ShipPickerWrapper(
    dialog, fleet, dismiss
) {
    constructor(inner: Any, dialog: InteractionDialogAPI, fleet: CampaignFleet, dismiss: (Any, Any) -> Unit) : this(
        inner as T,
        dialog,
        fleet,
        dismiss
    )

    override fun reassign(target: Any) {
        ReflectionUtils.set("ÓöOo00", target, this)
    }

    override fun fleetMemberClicked(
        p0: FleetMember?,
        p1: com.fs.starfarer.ui.`super`.B?,
        p2: com.fs.starfarer.util.`super`.C?
    ) {
        inner.fleetMemberClicked(p0, p1, p2)
        inner.dismiss(0)
        CaptainPickerDialog(fleet, p0, dialog as o000, dismiss).show(0f, 0f)
    }
}