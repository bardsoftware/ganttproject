/*
 * Created on 22.10.2005
 */
package net.sourceforge.ganttproject.gui;

import java.awt.Component;

import javax.swing.Action;

public interface ResourceTreeUIFacade {
    Action getMoveUpAction();
    Action getMoveDownAction();
    Component getUIComponent();    
    TableHeaderUIFacade getVisibleFields();
}
