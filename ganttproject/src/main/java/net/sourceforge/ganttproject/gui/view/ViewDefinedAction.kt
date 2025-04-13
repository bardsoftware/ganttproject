/*
 * Copyright 2025 BarD Software s.r.o., Dmitry Barashev.
 *
 * This file is part of GanttProject, an opensource project management tool.
 *
 * GanttProject is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 * GanttProject is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with GanttProject.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.sourceforge.ganttproject.gui.view

import net.sourceforge.ganttproject.GPLogger
import net.sourceforge.ganttproject.action.GPAction
import java.awt.event.ActionEvent
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener

/**
 * This is an action that is common for all views, however, its state and behavior are defined by the
 * view implementations. Such actions are object properties, clipboard actions, etc.
 */
class ViewDefinedAction(id: String): GPAction(id), PropertyChangeListener {
  // This action delegates all calls to the delegate action that changes as user switches between the views.
  var delegateAction: GPAction? = null
    set(value) {
      LOG.debug("View-defined action ${id}. Delegate changed: $field => $value")
      field?.let(::unbind)
      field = value
      field?.let(::bind)
    }

  init {
    actionStateChanged()
  }

  override fun actionPerformed(e: ActionEvent?) {
    delegateAction?.actionPerformed(e)
  }

  override fun propertyChange(evt: PropertyChangeEvent?) {
    evt?.let {
      if ("enabled" == it.propertyName) {
        actionStateChanged();
        LOG.debug("View-defined action $id enabled property change: now isEnabled=$isEnabled")
      }
    }
  }

  override fun getLocalizedName(): String? = delegateAction?.localizedName ?: super.getLocalizedName()
  override fun getLocalizedDescription(): String? = delegateAction?.localizedDescription ?: super.getLocalizedDescription()

  private fun bind(action: GPAction) {
    action.addPropertyChangeListener(this)
    actionStateChanged()
  }

  private fun unbind(action: GPAction) {
    action.removePropertyChangeListener(this)
  }

  private fun actionStateChanged() {
    // State of a delegate action has been changed, so update out state as well
    delegateAction?.let {
      setEnabled(it.isEnabled)
      updateTooltip();
    } ?: run {
      isEnabled = false
    }
  }
}

private val LOG = GPLogger.create("View.Action")