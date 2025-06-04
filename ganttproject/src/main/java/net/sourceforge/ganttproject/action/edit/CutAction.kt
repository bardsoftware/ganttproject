/*
GanttProject is an opensource project management tool.
Copyright (C) 2005-2011 GanttProject Team

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 3
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package net.sourceforge.ganttproject.action.edit

import net.sourceforge.ganttproject.action.GPAction
import net.sourceforge.ganttproject.gui.UIFacade
import net.sourceforge.ganttproject.gui.UIUtil
import net.sourceforge.ganttproject.gui.view.GPViewManager
import net.sourceforge.ganttproject.undo.GPUndoManager
import java.awt.event.ActionEvent

//TODO Enable/Disable action on selection changes
class CutAction(
    private val myViewmanager: GPViewManager,
    private val myUndoManager: GPUndoManager,
    private val myUiFacade: UIFacade
) : GPAction("cut") {
    override fun actionPerformed(e: ActionEvent) {
        if (calledFromAppleScreenMenu(e)) {
            return
        }
        //    myUndoManager.undoableEdit(getLocalizedName(), new Runnable() {
//      @Override
//      public void run() {
        myViewmanager.selectedArtefacts.startMoveClipboardTransaction()
        //      }
        //});
        myUiFacade.activeChart.focus()
    }

    override fun asToolbarAction(): CutAction {
        val result = CutAction(myViewmanager, myUndoManager, myUiFacade)
        result.setFontAwesomeLabel(UIUtil.getFontawesomeLabel(result))
        this.addPropertyChangeListener { evt ->
            if ("enabled" == evt.propertyName) {
                result.isEnabled = (evt.newValue as Boolean)
            }
        }
        result.isEnabled = this.isEnabled
        return result
    }
}
