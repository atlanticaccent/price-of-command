package data.scripts.ship_picker_listener.windows

import com.fs.starfarer.campaign.ui.supersuper
import com.fs.starfarer.ui.n
import com.fs.starfarer.ui.ooOo
import java.awt.Robot
import java.awt.event.KeyEvent

class Listener(private val listener: ooOo) : ooOo {
    constructor(listener: supersuper) : this(listener as ooOo)

    override fun actionPerformed(p0: Any?, p1: Any?) {
        listener.actionPerformed(p0, p1)
        val robot = Robot()
        robot.keyPress(KeyEvent.VK_G)
        robot.keyRelease(KeyEvent.VK_G)
    }

    fun attach(button: n) {
        button.listener = this
    }
}