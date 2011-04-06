package net.sourceforge.ganttproject.action;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.Action;
import javax.swing.KeyStroke;

import net.sourceforge.ganttproject.gui.UIFacade;

class RefreshViewAction extends GPAction {

    private UIFacade myUIFacade;

    public RefreshViewAction(UIFacade uiFacade) {
        super("refresh");
        myUIFacade = uiFacade;
        this.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_R, GPAction.MENU_MASK));
    }

    public void actionPerformed(ActionEvent ae) {
        myUIFacade.refresh();
    }
}
