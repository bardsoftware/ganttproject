package net.sourceforge.ganttproject.gui.options;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import net.sourceforge.ganttproject.gui.options.model.BooleanOption;

class BooleanOptionAction extends AbstractAction {
    private BooleanOption myOption;

    BooleanOptionAction(BooleanOption option) {
        super("");
        myOption = option;
    }

    public void actionPerformed(ActionEvent e) {
        myOption.toggle();
    }

}
