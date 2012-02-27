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

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import net.sourceforge.ganttproject.GanttOptions;
import net.sourceforge.ganttproject.filter.ExtensionBasedFileFilter;

public abstract class ImporterFactory {
    public static Importer createImporter(FileFilter fileFilter) {
        if (fileFilter == ImporterFactory.txtFilter) {
            return new ImporterFromTxtFile();
        }
        if (fileFilter == ImporterFactory.ganFilter) {
            return new ImporterFromGanttFile();
        }
        // else if (fileFilter==plannerFilter) {
        // return new ImporterFromPlannerFile();
        // }
        return null;
    }

    public static JFileChooser createFileChooser(GanttOptions options) {
        JFileChooser fc = new JFileChooser(options.getWorkingDir());
        FileFilter[] filefilters = fc.getChoosableFileFilters();
        for (int i = 0; i < filefilters.length; i++) {
            fc.removeChoosableFileFilter(filefilters[i]);
        }
        fc.addChoosableFileFilter(ImporterFactory.ganFilter);
        fc.addChoosableFileFilter(ImporterFactory.mppFilter);
        fc.addChoosableFileFilter(ImporterFactory.txtFilter);
        // fc.addChoosableFileFilter(plannerFilter);

        return fc;

    }

    private static FileFilter txtFilter = new ExtensionBasedFileFilter("txt",
            "Text files (.txt)");

    // private static FileFilter mppFilter = new
    // ExtensionBasedFileFilter("mpp|mpx|xml", "MsProject files (.mpp, .mpx,
    // .xml)");
    private static FileFilter mppFilter = new ExtensionBasedFileFilter(
            "mpp|mpx|xml", "MsProject files (.mpp, .mpx, .xml)");

    private static FileFilter ganFilter = new ExtensionBasedFileFilter(
            "xml|gan", "GanttProject files (.gan, .xml)");
    // private static FileFilter plannerFilter = new
    // ExtensionBasedFileFilter("mrproject|planner", "Planner (MrProject) files
    // (.mrproject)");

}
