/*
Copyright 2021 Dmitry Barashev, BarD Software s.r.o

This file is part of GanttProject, an open-source project management tool.

GanttProject is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

GanttProject is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with GanttProject.  If not, see <http://www.gnu.org/licenses/>.
*/
package net.sourceforge.ganttproject.task.algorithm

import biz.ganttproject.core.option.BooleanOption

/**
 * This scheduler can be switched on and off using a boolean option. When switched on,
 * it delegates the real work to the delegate scheduler.
 */
class SchedulerOptional(private val isDisabled: BooleanOption, private val delegate: SchedulerImpl) : AlgorithmBase() {
  override fun isEnabled() = if (isDisabled.isChecked) false else delegate.isEnabled
  override fun setEnabled(enabled: Boolean) {
    delegate.isEnabled = enabled
    run()
  }

  override fun setDiagnostic(d: Diagnostic?) {
    delegate.diagnostic = d
  }

  override fun getDiagnostic() = delegate.diagnostic

  override fun run() {
    if (isEnabled) {
      delegate.run()
    }
  }
}
