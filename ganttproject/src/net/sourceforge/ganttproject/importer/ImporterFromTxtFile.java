package net.sourceforge.ganttproject.importer;

import java.io.File;

import net.sourceforge.ganttproject.io.GanttTXTOpen;
import net.sourceforge.ganttproject.language.GanttLanguage;

public class ImporterFromTxtFile extends ImporterBase implements Importer {

    public String getFileNamePattern() {
        return "txt";
    }

    public String getFileTypeDescription() {
        return GanttLanguage.getInstance().getText("textFiles");
    }

    public void run(File selectedFile) {
        GanttTXTOpen opener = new GanttTXTOpen(getProject().getTaskManager());
        opener.load(selectedFile);
    }

}
