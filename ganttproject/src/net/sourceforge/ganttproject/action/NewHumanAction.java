/*
GanttProject is an opensource project management tool.
Copyright (C) 2011 GanttProject team

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

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.net.URL;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.KeyStroke;

import net.sourceforge.ganttproject.GanttProject;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.resource.HumanResourceManager;

/**
 * Action connected to the menu item for insert a new resource
 */
public class NewHumanAction extends ResourceAction {
    private final GanttProject myProject;

    private final int MENU_MASK = Toolkit.getDefaultToolkit()
            .getMenuShortcutKeyMask();

    public NewHumanAction(HumanResourceManager hrManager, GanttProject project) {
        super(hrManager);
        myProject = project;

        putValue(AbstractAction.NAME, GanttLanguage.getInstance()
                .correctLabel(getLanguage().getText("newHuman")));
        putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(
                KeyEvent.VK_H, MENU_MASK));
        URL iconUrl = this.getClass().getClassLoader().getResource(
                "icons/insert_16.gif");
        if (iconUrl != null) {
            putValue(Action.SMALL_ICON, new ImageIcon(iconUrl));
        }
    }

    public void actionPerformed(ActionEvent event) {
        myProject.newHumanResource();
    }

    public void languageChanged() {
        putValue(AbstractAction.NAME, GanttLanguage.getInstance()
                .correctLabel(getLanguage().getText("newHuman")));
    }
}
