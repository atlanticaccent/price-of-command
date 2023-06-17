package com.price_of_command.util

import com.fs.starfarer.api.campaign.CustomDialogDelegate
import com.fs.starfarer.api.campaign.CustomDialogDelegate.CustomDialogCallback
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin
import com.fs.starfarer.api.campaign.CustomVisualDialogDelegate
import com.fs.starfarer.api.campaign.CustomVisualDialogDelegate.DialogCallbacks
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.util.FaderUtil

class C2DialogDelegate(
    private val confirmText: String = "Confirm",
    private val cancelText: String = "Cancel",
    val build: (CustomPanelAPI, DialogCallbackWrapper) -> Unit
) : CustomDialogDelegate, CustomVisualDialogDelegate {
    private var confirmCallback: (() -> Unit)? = null
    private var cancelCallback: (() -> Unit)? = null
    var plugin: CustomUIPanelPlugin? = null
    private var advance: ((Float) -> Unit)? = null
    private var dismissed: ((Int) -> Unit)? = null

    override fun createCustomDialog(panel: CustomPanelAPI, callback: CustomDialogCallback) =
        build(panel, DialogCallbackWrapper.Custom(callback))

    override fun init(panel: CustomPanelAPI, callback: DialogCallbacks) = build(
        panel,
        DialogCallbackWrapper.Visual(callback)
    )

    override fun getConfirmText(): String = confirmText

    override fun getCancelText(): String = cancelText

    override fun hasCancelButton(): Boolean = true

    override fun customDialogConfirm() {
        confirmCallback?.invoke()
    }

    override fun customDialogCancel() {
        cancelCallback?.invoke()
    }

    override fun getCustomPanelPlugin(): CustomUIPanelPlugin? = plugin

    override fun getNoiseAlpha(): Float = 0f

    override fun advance(amount: Float) {
        advance?.invoke(amount)
    }

    override fun reportDismissed(option: Int) {
        dismissed?.invoke(option)
    }

    fun onConfirm(block: () -> Unit): C2DialogDelegate {
        confirmCallback = block
        return this
    }

    fun onCancel(block: () -> Unit): C2DialogDelegate {
        cancelCallback = block
        return this
    }
}

sealed class DialogCallbackWrapper : DialogCallbacks, CustomDialogCallback {
    override fun dismissDialog() {
        when (this) {
            is Visual -> this.inner.dismissDialog()
            else -> Unit
        }
    }

    override fun getPanelFader(): FaderUtil? = when (this) {
        is Visual -> this.inner.panelFader
        else -> null
    }

    override fun dismissCustomDialog(option: Int) {
        when (this) {
            is Custom -> this.inner.dismissCustomDialog(option)
            else -> Unit
        }
    }

    class Visual(val inner: DialogCallbacks) : DialogCallbackWrapper()
    class Custom(val inner: CustomDialogCallback) : DialogCallbackWrapper()
}
