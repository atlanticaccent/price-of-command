package com.price_of_command.platform.linux

import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.campaign.fleet.CampaignFleet
import com.fs.starfarer.campaign.fleet.FleetMember
import com.fs.starfarer.campaign.ui.S
import com.fs.starfarer.coreui.CaptainPickerDialog
import com.fs.starfarer.ui.A.B
import com.fs.starfarer.ui.newui.OoOo
import com.fs.starfarer.ui.ooOo
import com.fs.starfarer.ui.z
import com.price_of_command.platform.shared.ReflectionUtils
import com.price_of_command.platform.shared.ShipPickerWrapper

class LinuxShipPickerWrapper(
    private val inner: OoOo, dialog: InteractionDialogAPI, fleet: CampaignFleet, dismiss: (Any, Any) -> Unit
) : ooOo by inner, S.o, ShipPickerWrapper(
    dialog, fleet, dismiss
) {
    constructor(inner: Any, dialog: InteractionDialogAPI, fleet: CampaignFleet, dismiss: (Any, Any) -> Unit) : this(
        inner as OoOo,
        dialog,
        fleet,
        dismiss
    )

    override fun reassign(target: Any) {
        ReflectionUtils.set("int.void\$do", target, this)
    }

    override fun fleetMemberClicked(p0: FleetMember?, p1: B?, p2: com.fs.starfarer.util.`super`.C?) {
        inner.fleetMemberClicked(p0, p1, p2)
        inner.dismiss(0)
        CaptainPickerDialog(fleet, p0, dialog as z, dismiss).show(0f, 0f)
    }
}