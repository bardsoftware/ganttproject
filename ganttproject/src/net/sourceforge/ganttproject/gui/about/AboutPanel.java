/***************************************************************************
 AboutPanel.java
 -----------------
 begin                : 28 juin 2004
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
package net.sourceforge.ganttproject.gui.about;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.sourceforge.ganttproject.GanttProject;
import net.sourceforge.ganttproject.GanttSplash;
import net.sourceforge.ganttproject.gui.options.GeneralOptionPanel;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.util.BrowserControl;

/**
 * @author athomas The About panel.
 */
public class AboutPanel extends GeneralOptionPanel {

    public AboutPanel() {
        super(GanttProject.correctLabel(GanttLanguage.getInstance().getText(
                "about")), GanttLanguage.getInstance().getText("settingsAbout")
                + " " + GanttProject.version);
        GanttSplash splash = new GanttSplash();
        JLabel jLabelImage = splash.getSplashComponent();
        vb.add(new JPanel());
        vb.add(jLabelImage);
        vb.add(new JPanel());
        JButton bHomePage = new JButton(GanttProject.correctLabel(language
                .getText("webPage")), new ImageIcon(getClass().getResource(
                "/icons/home_16.gif")));
        bHomePage.setToolTipText(GanttProject.getToolTip(language
                .getText("goTo")
                + " " + "http://ganttproject.biz")); // add a simple tool tip text
        bHomePage.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                BrowserControl.displayURL("http://ganttproject.biz/");
            }
        });

        vb.add(bHomePage);
        applyComponentOrientation(language.getComponentOrientation());

    }

    /** This method checks if the value has changed, and asks for commit changes. */
    public boolean applyChanges(boolean askForApply) {
        return bHasChange = false;
    }

    /** Initialize the component. */
    public void initialize() {
    }
}
