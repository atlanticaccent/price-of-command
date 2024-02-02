package com.price_of_command.platform.macos

import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.campaign.fleet.CampaignFleet
import com.fs.starfarer.campaign.fleet.FleetMember
import com.fs.starfarer.campaign.ui.S
import com.fs.starfarer.coreui.CaptainPickerDialog
import com.fs.starfarer.ui.interfacenew
import com.fs.starfarer.ui.newui.T
import com.fs.starfarer.ui.ooOo
import com.fs.starfarer.util.A.C
import com.price_of_command.platform.shared.ReflectionUtils
import com.price_of_command.platform.shared.ShipPickerWrapper

class MacShipPickerWrapper(
    private val inner: T, dialog: InteractionDialogAPI, fleet: CampaignFleet, dismiss: (Any, Any) -> Unit
) : ooOo by inner, S.o, ShipPickerWrapper(
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
        p1: com.fs.starfarer.ui.`super`.OoOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOO?,
        p2: C?
    ) {
        inner.fleetMemberClicked(p0, p1, p2)
        inner.dismiss(0)
        CaptainPickerDialog(fleet, p0, dialog as interfacenew, dismiss).show(0f, 0f)
    }
}