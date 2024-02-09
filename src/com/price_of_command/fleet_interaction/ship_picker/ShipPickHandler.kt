package com.price_of_command.fleet_interaction.ship_picker

import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.campaign.fleet.CampaignFleet
import com.fs.starfarer.coreui.CaptainPickerDialog
import com.price_of_command.reflection.ReflectionUtils
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

class ShipPickHandler(private val inner: Any, val dialog: InteractionDialogAPI, val fleet: CampaignFleet, val dismiss: (Any, Any) -> Unit) : InvocationHandler {
    override fun invoke(proxy: Any, method: Method, args: Array<out Any>): Any? {
        val res = ReflectionUtils.invoke(method.name, inner, *args)
        return if (method.name == "fleetMemberClicked") {
            ReflectionUtils.invoke("dismiss", inner, 0)

            val captainPickerDialogConstructor = CaptainPickerDialog::class.java.constructors.first()
            val dismissClass = captainPickerDialogConstructor.parameterTypes.last()
            val dismissProxy = Proxy.newProxyInstance(dismissClass.classLoader, arrayOf(dismissClass)) { _, _, dismissArgs ->
                dismiss(dismissArgs[0], dismissArgs[1])
            }

            (captainPickerDialogConstructor.newInstance(fleet, args[0], dialog, dismissClass.cast(dismissProxy)) as CaptainPickerDialog).show(0f, 0f)
            null
        } else {
            res
        }
    }

    fun build(interfaces: Array<Class<*>>): Any {
        return Proxy.newProxyInstance(interfaces.first().classLoader, interfaces, this)
    }
}
