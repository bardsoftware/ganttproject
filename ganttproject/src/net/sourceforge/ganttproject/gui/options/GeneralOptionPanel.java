/***************************************************************************
 GeneralOptionPanel.java 
 ------------------------------------------
 begin                : 24 juin 2004
 copyright            : (C) 2004 by Thomas Alexandre
 email                : alexthomas(at)ganttproject.org
 ***************************************************************************/

/***************************************************************************
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *                                                                         *
 ***************************************************************************/
package net.sourceforge.ganttproject.gui.options;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.Box;
import javax.swing.JPanel;

import net.sourceforge.ganttproject.Mediator;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.language.GanttLanguage;

/**
 * @author athomas Abstract class for the Options panels
 */
public abstract class GeneralOptionPanel extends JPanel {

    protected GanttLanguage language = GanttLanguage.getInstance();

    /** General vertical box. */
    protected Box vb = Box.createVerticalBox();

    /** Tell if the parameters of the panel have change. */
    protected boolean bHasChange = false;

    private String myTitle;

    private String myComment;

    public GeneralOptionPanel(String title, String comment) {
        super();
        setLayout(new BorderLayout());
        add(vb, BorderLayout.CENTER);
        myTitle = title;
        myComment = comment;
    }

    public Component getComponent() {
        return this;
    }

    /** This method check if the value has changed, and assk for commit changes. */
    public abstract boolean applyChanges(boolean askForApply);

    /** Initialize the component. */
    public abstract void initialize();

    /** This method ask for saving the changes. */
    public boolean askForApplyChanges() {
        return (UIFacade.Choice.YES==getUIFacade().showConfirmationDialog(language.getText("msg20"),
                language.getText("question")));
    }

    /**
     * @return
     */
    public String getTitle() {
        return myTitle;
    }

    /**
     * @return
     */
    public String getComment() {
        return myComment;
    }

    private UIFacade getUIFacade() {
        return Mediator.getGanttProjectSingleton().getUIFacade();
    }

    public void rollback() {
        initialize();
    }
}
