package com.price_of_command.platform.macos

import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.campaign.fleet.CampaignFleet
import com.fs.starfarer.campaign.fleet.FleetMember
import com.fs.starfarer.campaign.ui.S
import com.fs.starfarer.coreui.CaptainPickerDialog
import com.fs.starfarer.ui.Objectsuper
import com.fs.starfarer.ui.newui.T
import com.fs.starfarer.ui.oOOO.new
import com.fs.starfarer.ui.z
import com.fs.starfarer.util.A.Object
import com.price_of_command.platform.shared.ReflectionUtils
import com.price_of_command.platform.shared.ShipPickerWrapper

class MacShipPickerWrapper(
    private val inner: T, dialog: InteractionDialogAPI, fleet: CampaignFleet, dismiss: (Any, Any) -> Unit
) : Objectsuper by inner, S.o, ShipPickerWrapper(
    dialog, fleet, dismiss
) {
    constructor(inner: Any, dialog: InteractionDialogAPI, fleet: CampaignFleet, dismiss: (Any, Any) -> Unit) : this(
        inner as T,
        dialog,
        fleet,
        dismiss
    )

    override fun reassign(target: Any) {
        ReflectionUtils.set("ÕõOo00", target, this)
    }

    override fun fleetMemberClicked(p0: FleetMember, p1: new, p2: Object) {
        inner.fleetMemberClicked(p0, p1, p2)
        inner.dismiss(0)
        CaptainPickerDialog(fleet, p0, dialog as z, dismiss).show(0f, 0f)
    }
}