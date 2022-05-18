/*
Copyright 2003-2012 Dmitry Barashev, GanttProject Team

This file is part of GanttProject, an opensource project management tool.

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
package net.sourceforge.ganttproject.task.algorithm;

import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.task.Task;

import java.util.Date;

public class AlgorithmBase {

  public interface Diagnostic {
    void addModifiedTask(Task t, Date newStart, Date newEnd);
    void logError(Exception ex);
  }
  public static class DiagnosticStub implements Diagnostic {
    @Override
    public void addModifiedTask(Task t, Date newStart, Date newEnd) {
    }

    @Override
    public void logError(Exception ex) {
      GPLogger.log(ex);
    }
  }

  private boolean isEnabled = true;
  private Diagnostic myDiagnostic = new DiagnosticStub();

  public void setEnabled(boolean enabled) {
    isEnabled = enabled;
  }

  public boolean isEnabled() {
    return isEnabled;
  }

  public void setDiagnostic(Diagnostic d) {
    myDiagnostic = d;
  }

  protected Diagnostic getDiagnostic() {
    return myDiagnostic;
  }

  public void run() {
  }
}
