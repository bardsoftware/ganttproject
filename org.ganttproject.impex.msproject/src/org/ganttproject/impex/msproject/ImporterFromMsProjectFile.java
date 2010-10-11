package org.ganttproject.impex.msproject;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import com.tapsterrock.mpx.MPXException;

import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.document.Document;
import net.sourceforge.ganttproject.document.FileDocument;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.options.model.GPOption;
import net.sourceforge.ganttproject.gui.options.model.GPOptionGroup;
import net.sourceforge.ganttproject.importer.Importer;
import net.sourceforge.ganttproject.importer.ImporterBase;
import net.sourceforge.ganttproject.language.GanttLanguage;

public class ImporterFromMsProjectFile extends ImporterBase implements Importer {
    private LocaleOption myLanguageOption = new LocaleOption();

    private GPOptionGroup myMPXOptions = new GPOptionGroup("importer.msproject.mpx", new GPOption[] {myLanguageOption});

    public String getFileNamePattern() {
        return "mpp|mpx|xml";
    }

    public GPOptionGroup[] getSecondaryOptions() {
        return new GPOptionGroup[] {myMPXOptions};
    }

    public String getFileTypeDescription() {
        return GanttLanguage.getInstance().getText("impex.msproject.description");
    }

    public void run(File selectedFile) {
        Document document = getDocument(selectedFile);
        openDocument(getProject(), getUiFacade(), document);
    }

    protected Document getDocument(File selectedFile) {
        return new FileDocument(selectedFile);
    }

    GanttMPXJOpen open;

    protected void openDocument(final IGanttProject project, final UIFacade uiFacade, Document document) {
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(
                    getClass().getClassLoader());
            if (document.getPath().toLowerCase().endsWith(".mpp")) {
                open = new GanttMPPOpen(project);
            }
            else if (document.getPath().toLowerCase().endsWith(".mpx")) {
                open = null;
                Locale importlocale = myLanguageOption.getSelectedLocale();
                open = new GanttMPXOpen(project, importlocale);
            } else if (document.getPath().toLowerCase().endsWith(".xml"))
                open = new GanttMSPDIOpen(project);
            else
                open = null;

            open.load(document.getInputStream());

        } catch (IOException e) {
            uiFacade.showErrorDialog(e);
        } catch (MPXException e) {
            uiFacade.showErrorDialog(e);
        }
        finally {
            Thread.currentThread().setContextClassLoader(contextClassLoader);
        }
    }

}
