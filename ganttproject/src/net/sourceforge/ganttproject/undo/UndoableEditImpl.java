/*
 * Created on 14.03.2005
 */
package net.sourceforge.ganttproject.undo;

import java.io.File;
import java.io.IOException;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.document.Document;
import net.sourceforge.ganttproject.io.GPSaver;

/**
 * @author bard
 */
class UndoableEditImpl extends AbstractUndoableEdit {
    private String myPresentationName;

    private Document myDocumentBefore;

    private Document myDocumentAfter;

    private UndoManagerImpl myManager;

    UndoableEditImpl(String localizedName, Runnable editImpl,
            UndoManagerImpl manager) throws IOException {
        // System.out.println ("UndoableEditImpl : " + localizedName);
        myManager = manager;
        myPresentationName = localizedName;
        myDocumentBefore = saveFile();
        editImpl.run();
        myDocumentAfter = saveFile();
    }

    private Document saveFile() throws IOException {
        File tempFile = createTemporaryFile();
        tempFile.deleteOnExit();
        Document doc = myManager.getDocumentManager().getDocument(
                tempFile.getAbsolutePath());
        doc.write();
        //GPSaver saver = myManager.getParserFactory().newSaver();
        //saver.save(doc.getOutputStream());
        return doc;
    }

    public boolean canUndo() {
        return myDocumentBefore.canRead();
    }

    public boolean canRedo() {
        return myDocumentAfter.canRead();
    }

    public void redo() throws CannotRedoException {
        try {
            restoreDocument(myDocumentAfter);
        } catch (IOException e) {
        	if (!GPLogger.log(e)) {
        		e.printStackTrace(System.err);
        	}
            throw new CannotRedoException();
        }
    }

    public void undo() throws CannotUndoException {
        try {
            restoreDocument(myDocumentBefore);
        } catch (IOException e) {
        	if (!GPLogger.log(e)) {
        		e.printStackTrace(System.err);
        	}
            throw new CannotRedoException();
        }
    }

    private void restoreDocument(Document document) throws IOException {
        Document projectDocument = myManager.getProject().getDocument(); 
		myManager.getProject().close();
        document.read();
        myManager.getProject().setDocument(projectDocument);
        
    }

    public String getPresentationName() {
        return myPresentationName;
    }

    File createTemporaryFile() throws IOException {
        return File.createTempFile("_GanttProject_qSave", ".gan");
    }

}
