/*
GanttProject is an opensource project management tool. License: GPL2
Copyright (C) 2005-2011 GanttProject Team

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
package net.sourceforge.ganttproject.action.help;

import java.awt.event.ActionEvent;

import javax.swing.JMenu;

import net.sourceforge.ganttproject.GanttProject;
import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.ViewLogDialog;
import net.sourceforge.ganttproject.gui.about.AboutDialog;

/**
 * Collection of actions from Help menu.
 *
 * @author dbarashev (Dmitry Barashev)
 */
public class HelpMenu {

    private final AboutAction myAboutAction;
    private final ViewLogAction myViewLogAction;

    public HelpMenu(GanttProject mainFrame) {
        myAboutAction = new AboutAction(mainFrame);
        myViewLogAction = new ViewLogAction(mainFrame.getUIFacade());
    }
    public JMenu createMenu() {
        JMenu result = new JMenu(GPAction.createVoidAction("help"));
        result.add(myAboutAction);
        result.add(myViewLogAction);
        return result;
    }

    private static class AboutAction extends GPAction {
        private final GanttProject myMainFrame;

        AboutAction(GanttProject mainFrame) {
            super("about");
            myMainFrame = mainFrame;
        }

        public void actionPerformed(ActionEvent e) {
            AboutDialog agp = new AboutDialog(myMainFrame);
            agp.setVisible(true);
        }
    }

    private static class ViewLogAction extends GPAction {
        private final UIFacade myUiFacade;

        ViewLogAction(UIFacade uiFacade) {
            super("viewLog");
            myUiFacade = uiFacade;
        }

        public void actionPerformed(ActionEvent e) {
            ViewLogDialog.show(myUiFacade);
        }
    }
}
