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
package net.sourceforge.ganttproject.action.resource;

import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.resource.HumanResourceManager;

//TODO Add listener for changed resource selection, see TaskActionBase
/**
 * Action base for resource related actions
 */
abstract class ResourceAction extends GPAction {
    private final HumanResourceManager myManager;

    public ResourceAction(String name, HumanResourceManager hrManager) {
        super(name);
        myManager = hrManager;
    }

    protected HumanResourceManager getManager() {
        return myManager;
    }
}
