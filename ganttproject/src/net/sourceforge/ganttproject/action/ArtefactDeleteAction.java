/*
GanttProject is an opensource project management tool.
Copyright (C) 2011 GanttProject Team

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

import javax.swing.AbstractAction;

public class ArtefactDeleteAction extends GPAction {
    private final ActiveActionProvider myProvider;

    public ArtefactDeleteAction(ActiveActionProvider provider) {
        myProvider = provider;
    }

    public void actionPerformed(ActionEvent e) {
        AbstractAction activeAction = myProvider.getActiveAction();
        activeAction.actionPerformed(e);
    }

    @Override
    protected String getIconFilePrefix() {
        return "delete_";
    }

    @Override
    public void isIconVisible(boolean isNull) {
        setIconVisible(isNull);
    }
}
