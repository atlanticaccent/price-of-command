package data.scripts.ship_picker_listener.windows;

import com.fs.starfarer.campaign.ui.supersuper;
import com.fs.starfarer.ui.n;
import com.fs.starfarer.ui.ooOo;

import java.awt.*;
import java.awt.event.KeyEvent;
import com.price_of_command.janino.interfaces.ShipPickerListenerInterface;

public class Listener implements ooOo, ShipPickerListenerInterface {
    public ooOo listener;

    public Listener() {
    }

    public void attach(n button) {
        n cast_button = (n) button;
        cast_button.setListener(this);
    }

    @Override
    public void actionPerformed(Object o, Object o1) {
        listener.actionPerformed(o, o1);
        try {
            Robot robot = new Robot();
            robot.keyPress(KeyEvent.VK_G);
            robot.keyRelease(KeyEvent.VK_G);
        } catch (AWTException e) {
            throw new RuntimeException(e);
        }
    }
}