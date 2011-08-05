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
import net.sourceforge.ganttproject.util.BrowserControl;

/**
 * The About panel.
 *
 * @author athomas 
 */
public class AboutPanel extends GeneralOptionPanel {

    public AboutPanel() {
        super(language.getCorrectedLabel("about"), language
                .getText("settingsAbout") + " " + GanttProject.version);
        GanttSplash splash = new GanttSplash();
        JLabel jLabelImage = splash.getSplashComponent();
        vb.add(new JPanel());
        vb.add(jLabelImage);
        vb.add(new JPanel());
        JButton bHomePage = new JButton(language.getCorrectedLabel("webPage"),
                new ImageIcon(getClass().getResource("/icons/home_16.gif")));
        bHomePage.setToolTipText(GanttProject.getToolTip(language
                .getText("goTo") + " " + "http://ganttproject.biz"));
        bHomePage.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                BrowserControl.displayURL("http://ganttproject.biz/");
            }
        });

        vb.add(bHomePage);
        applyComponentOrientation(language.getComponentOrientation());
    }

    public boolean applyChanges(boolean askForApply) {
        // There are never changes for the about panel
        return false;
    }

    public void initialize() {
    }
}
