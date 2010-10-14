/***************************************************************************
 OpenFileDialog.java  -  description
 -------------------
 begin                : may 2003

 ***************************************************************************/

/***************************************************************************
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *                                                                         *
 ***************************************************************************/

package net.sourceforge.ganttproject.gui;

import java.io.File;

import javax.swing.JFileChooser;

import net.sourceforge.ganttproject.GanttProject;
import net.sourceforge.ganttproject.filter.GanttXMLFileFilter;

/**
 * Open A dialog box to select an xml file for import resources
 */
public class OpenFileDialog {
    /** Construtor */
    public OpenFileDialog(GanttProject project) {
        myStartDirectory = new File(System.getProperty("user.home"));
        myproject = project;
    }

    /** Open the file chooser */
    public OpenFileDialog(String startDirectory) {
        myStartDirectory = new File(startDirectory);
        if (!myStartDirectory.isDirectory()) {
            myStartDirectory = myStartDirectory.getParentFile();
        }
    }

    /** Show the dialog box */
    public File show() {
        File result = null;
        JFileChooser fc = new JFileChooser(myStartDirectory);
        fc.addChoosableFileFilter(new GanttXMLFileFilter());

        int returnVal = fc.showOpenDialog(null);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            result = fc.getSelectedFile();
        }
        return result;
    }

    private File myStartDirectory;

    private GanttProject myproject;
}
