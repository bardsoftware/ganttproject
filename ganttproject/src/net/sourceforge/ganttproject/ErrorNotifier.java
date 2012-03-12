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
package net.sourceforge.ganttproject;

import java.util.ArrayList;
import java.util.List;

class ErrorNotifier implements Runnable {
  private final List<String> myErrors = new ArrayList<String>();
  private final UIFacadeImpl myUIFacade;

  ErrorNotifier(UIFacadeImpl uiFacade) {
    myUIFacade = uiFacade;
  }

  void add(Throwable e) {
    myErrors.add(e.getMessage());
  }

  @Override
  public void run() {
    StringBuffer buf = new StringBuffer();
    for (int i = 0; i < myErrors.size(); i++) {
      buf.append(String.valueOf(myErrors.get(i)));
      buf.append("\n\n");
    }
    myUIFacade.showErrorDialog(buf.toString());
    myErrors.clear();
    myUIFacade.resetErrorLog();
  }
}
