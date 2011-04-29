/*
 * Created on 18.08.2003
 *
 */
package net.sourceforge.ganttproject.document;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

/**
 * This class implements the interface Document for file access on local file
 * systems.
 *
 * @author Michael Haeusler (michael at akatose.de)
 */
public class FileDocument extends AbstractDocument {

    private File file;
    private long myLastAccessTimestamp;

    public FileDocument(File file) {
        this.file = file;
    }

    public String getDescription() {
        return file.getName();
    }

    public boolean canRead() {
        return file.canRead();
    }

    public IStatus canWrite() {
        return (file.exists()) ? canOverwrite() : canCreate(file);
    }

    private IStatus canOverwrite() {
        if (file.isDirectory()) {
            return new Status(
                IStatus.ERROR, PLUGIN_ID, Document.ErrorCode.IS_DIRECTORY.ordinal(),
                "",  null);
        }
        if (!file.canWrite()) {
            return new Status(
                IStatus.ERROR, PLUGIN_ID, Document.ErrorCode.NOT_WRITABLE.ordinal(),
                "",  null);
        }
        if (file.lastModified() > getLastAccessTimestamp()) {
            return new Status(
                IStatus.ERROR, PLUGIN_ID, Document.ErrorCode.LOST_UPDATE.ordinal(),
                "",  null);
        }
        return Status.OK_STATUS;
    }

    private long getLastAccessTimestamp() {
        return myLastAccessTimestamp;
    }

    private static IStatus canCreate(File f) {
        File parentFile = f.getParentFile();
        if (parentFile.exists()) {
            if (!parentFile.isDirectory()) {
                return new Status(
                    IStatus.ERROR, PLUGIN_ID, Document.ErrorCode.PARENT_IS_NOT_DIRECTORY.ordinal(),
                    parentFile.getAbsolutePath(),  null);
            }
            if (!parentFile.canWrite()) {
                return new Status(
                    IStatus.ERROR, PLUGIN_ID, Document.ErrorCode.PARENT_IS_NOT_WRITABLE.ordinal(),
                    parentFile.getAbsolutePath(),  null);
            }
            return Status.OK_STATUS;
        }
        return canCreate(parentFile);
    }

    public boolean isValidForMRU() {
        return file.exists();
    }

    public InputStream getInputStream() throws FileNotFoundException {
        myLastAccessTimestamp = System.currentTimeMillis();
        return new FileInputStream(file);
    }

    public OutputStream getOutputStream() throws FileNotFoundException {
        return new FileOutputStream(file) {
            @Override
            public void close() throws IOException {
                super.close();
                myLastAccessTimestamp = System.currentTimeMillis();
            }
        };
    }

    public String getPath() {
        return file.getPath();
    }

    public String getFilePath() {
        return getPath();
    }

    public void open() throws IOException {

    }

    public void write() throws IOException {
        // TODO Auto-generated method stub

    }

    public URI getURI() {
        return file.toURI();
    }

    public boolean isLocal() {
        return true;
    }


}
