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

/**
 * This class implements the interface Document for file access on local file
 * systems.
 * 
 * @author Michael Haeusler (michael at akatose.de)
 */
public class FileDocument extends AbstractDocument {

    private File file;

    public FileDocument(File file) {
        this.file = file;
    }

    public String getDescription() {
        return file.getName();
    }

    public boolean canRead() {
        return file.canRead();
    }

    public boolean canWrite() {
        boolean writable = file.canWrite();
        if (!writable && !file.exists()) {
            try {
                if (file.createNewFile()) {
                    file.delete();
                    writable = true;
                }
            } catch (IOException e) {
            }
        }
        return (writable);
    }

    public boolean isValidForMRU() {
        return file.exists();
    }

    public InputStream getInputStream() throws FileNotFoundException {
        return new FileInputStream(file);
    }

    public OutputStream getOutputStream() throws FileNotFoundException {
        return new FileOutputStream(file);
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
