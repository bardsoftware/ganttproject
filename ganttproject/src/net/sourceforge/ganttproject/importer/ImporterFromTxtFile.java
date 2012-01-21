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
package net.sourceforge.ganttproject.importer;

import java.io.File;

import net.sourceforge.ganttproject.io.GanttTXTOpen;
import net.sourceforge.ganttproject.language.GanttLanguage;

public class ImporterFromTxtFile extends ImporterBase implements Importer {

    @Override
    public String getFileNamePattern() {
        return "txt";
    }

    @Override
    public String getFileTypeDescription() {
        return GanttLanguage.getInstance().getText("textFiles");
    }

    @Override
    public void run(File selectedFile) {
        GanttTXTOpen opener = new GanttTXTOpen(getProject().getTaskManager());
        opener.load(selectedFile);
    }

}
