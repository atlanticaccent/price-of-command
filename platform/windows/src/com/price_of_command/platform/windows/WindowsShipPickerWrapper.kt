package com.price_of_command.platform.windows

import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.campaign.fleet.CampaignFleet
import com.fs.starfarer.campaign.fleet.FleetMember
import com.fs.starfarer.campaign.ui.supersuper
import com.fs.starfarer.coreui.CaptainPickerDialog
import com.fs.starfarer.ui.A.OoOO
import com.fs.starfarer.ui.interfacenew
import com.fs.starfarer.ui.newui.newsuper
import com.fs.starfarer.ui.ooOo
import com.fs.starfarer.util.A.ooOO
import com.price_of_command.platform.shared.ReflectionUtils
import com.price_of_command.platform.shared.ShipPickerWrapper

class WindowsShipPickerWrapper(
    private val inner: newsuper, dialog: InteractionDialogAPI, fleet: CampaignFleet, dismiss: (Any, Any) -> Unit
) : ooOo by inner, supersuper.o, ShipPickerWrapper(
    dialog, fleet, dismiss
) {
    constructor(inner: Any, dialog: InteractionDialogAPI, fleet: CampaignFleet, dismiss: (Any, Any) -> Unit) : this(
        inner as newsuper,
        dialog,
        fleet,
        dismiss
    )

    override fun reassign(target: Any) {
        ReflectionUtils.set("ÕõOo00", target, this)
    }

    override fun fleetMemberClicked(p0: FleetMember?, p1: OoOO?, p2: ooOO?) {
        inner.fleetMemberClicked(p0, p1, p2)
        inner.dismiss(0)
        CaptainPickerDialog(fleet, p0, dialog as interfacenew, dismiss).show(0f, 0f)
    }
}