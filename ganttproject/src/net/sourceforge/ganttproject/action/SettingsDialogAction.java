/*
GanttProject is an opensource project management tool. License: GPL2
Copyright (C) 2011 Dmitry Barashev

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
package net.sourceforge.ganttproject.action;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.Action;
import javax.swing.KeyStroke;

import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.options.SettingsDialog2;

public class SettingsDialogAction extends GPAction {
    private final UIFacade myUiFacade;
    private final IGanttProject myProject;
    
    public SettingsDialogAction(IGanttProject project, UIFacade uiFacade) {
        super("settings");
        putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_G, MENU_MASK));
        myUiFacade = uiFacade;
        myProject = project;
    }
    @Override
    public void actionPerformed(ActionEvent e) {
        SettingsDialog2 dialog = new SettingsDialog2(myProject, myUiFacade);
        dialog.show();
    }
}
