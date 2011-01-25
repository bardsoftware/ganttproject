package net.sourceforge.ganttproject.action;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.Action;
import javax.swing.KeyStroke;

import net.sourceforge.ganttproject.GanttOptions;
import net.sourceforge.ganttproject.gui.UIFacade;

public class RefreshViewAction extends GPAction implements RolloverAction {

    private UIFacade myUIFacade;

    public RefreshViewAction(UIFacade uiFacade, GanttOptions options) {
        super(null, options.getIconSize());
        myUIFacade = uiFacade;
        this.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(
                KeyEvent.VK_R, GPAction.MENU_MASK));
    }

    public String getIconFilePrefix() {
        return "refresh_";
    }

    public void actionPerformed(ActionEvent ae) {
        myUIFacade.refresh();
    }
    public void isIconVisible(boolean isNull) {
        setIconVisible(true);

    }

    public void setIconSize(String iconSize) {
        putValue(Action.SMALL_ICON, createIcon("16"));
        ;
    }
}
