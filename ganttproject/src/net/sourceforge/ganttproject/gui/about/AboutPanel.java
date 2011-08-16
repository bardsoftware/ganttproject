/*
GanttProject is an opensource project management tool.
Copyright (C) 2004-2011 Alexandre Thomas, GanttProject Team

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
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
