/*
 * Created on 22.10.2005
 */
package net.sourceforge.ganttproject.gui;

import java.awt.Component;

import javax.swing.Action;

public interface TaskTreeUIFacade {
    Action getIndentAction();
    Action getUnindentAction();
    Action getMoveUpAction();
    Action getMoveDownAction();
    Component getTreeComponent();
    void setLinkTasksAction(Action action);
    void setUnlinkTasksAction(Action unlinkAction);
    TableHeaderUIFacade getVisibleFields();
}
