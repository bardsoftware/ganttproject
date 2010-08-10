package biz.ganttproject.impex.msproject2;

import java.io.File;
import java.io.IOException;

import net.sf.mpxj.MPXJException;
import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.importer.Importer;
import net.sourceforge.ganttproject.importer.ImporterBase;

public class ImporterFromMsProjectFile extends ImporterBase implements Importer {
    public ImporterFromMsProjectFile() {
        super("msproject2");
    }

    public String getFileNamePattern() {
        return "mpp|mpx|xml";
    }

    public void run(File selectedFile) {
        try {
            new ProjectFileImporter(getProject(), selectedFile).run();
        } catch (MPXJException e) {
            getUiFacade().showErrorDialog(e);
        }
    }
}
