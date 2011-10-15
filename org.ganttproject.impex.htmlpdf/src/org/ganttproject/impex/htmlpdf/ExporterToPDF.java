package org.ganttproject.impex.htmlpdf;

import java.awt.Component;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.ganttproject.impex.htmlpdf.itext.ITextEngine;
import org.ganttproject.impex.htmlpdf.itext.ITextStylesheet;

import net.sourceforge.ganttproject.gui.options.model.GPOptionGroup;
import net.sourceforge.ganttproject.language.GanttLanguage;

public class ExporterToPDF extends ExporterBase {

    private final FOPEngine myFopEngine = new FOPEngine(this);
    private final ITextEngine myITextEngine = new ITextEngine(this);
    private Stylesheet mySelectedStylesheet;

    @Override
    protected ExporterJob[] createJobs(File outputFile, List<File> resultFiles) {
        if (mySelectedStylesheet instanceof PDFStylesheet) {
            return myFopEngine.createJobs(outputFile, resultFiles);
        }
        if (mySelectedStylesheet instanceof ITextStylesheet) {
            return myITextEngine.createJobs(outputFile, resultFiles);
        }
        assert false : "Unknown stylesheet is selected: " + mySelectedStylesheet;
        return null;
    }

    @Override
    protected String getStylesheetOptionID() {
        return "impex.pdf.stylesheet";
    }

    @Override
    protected List<Stylesheet> getStylesheets() {
        List<Stylesheet> result = new ArrayList<Stylesheet>();
        result.addAll(myITextEngine.getStylesheets());
        result.addAll(myFopEngine.getStylesheets());
        return result;
    }

    @Override
    protected void setSelectedStylesheet(Stylesheet stylesheet) {
        mySelectedStylesheet = stylesheet;
        initEngine();
    }

    private void initEngine() {
        if (mySelectedStylesheet instanceof PDFStylesheet) {
            myFopEngine.setContext(getProject(), getUIFacade(), getPreferences(), mySelectedStylesheet);
        }
        if (mySelectedStylesheet instanceof ITextStylesheet) {
            myITextEngine.setContext(getProject(), getUIFacade(), getPreferences(), mySelectedStylesheet);
        }
        assert false : "Unknown stylesheet is selected: " + mySelectedStylesheet;
    }

    @Override
    public String getFileTypeDescription() {
        return GanttLanguage.getInstance().getText("impex.pdf.description");
    }

    @Override
    public Component getCustomOptionsUI() {
        if (mySelectedStylesheet instanceof ITextStylesheet) {
            return myITextEngine.getCustomOptionsUI();
        }
        return null;
    }

    @Override
    public List<GPOptionGroup> getSecondaryOptions() {
        List<GPOptionGroup> result = new ArrayList<GPOptionGroup>();
        result.add(createExportRangeOptionGroup());
        if (mySelectedStylesheet instanceof PDFStylesheet) {
            List<GPOptionGroup> secondaryOptions = myFopEngine.getSecondaryOptions();
            if (secondaryOptions != null) {
                result.addAll(secondaryOptions);
            }
        }
        if (mySelectedStylesheet instanceof ITextStylesheet) {
            result.addAll(myITextEngine.getSecondaryOptions());
        }
        return result;
    }

    @Override
    public String getFileNamePattern() {
        return "pdf";
    }

    @Override
    public String proposeFileExtension() {
        return "pdf";
    }

    @Override
    public String[] getFileExtensions() {
        return new String[]{"pdf"};
    }

}
