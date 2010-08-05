/*
 * Created on 16.10.2005
 */
package net.sourceforge.ganttproject.util;

import java.io.File;
import java.io.IOException;

public abstract class FileUtil {
    public static String getExtension(File file) {
        int lastDot = file.getName().lastIndexOf(FILE_EXTENSION_SEPARATOR);
        return lastDot>=0 ? file.getName().substring(lastDot+1) : file.getName();
    }
    
    public static File replaceExtension(File f, String newExtension) throws IOException {
        String filename = f.getName();
        int i = filename.lastIndexOf(FILE_EXTENSION_SEPARATOR);

        File containingFolder = f.getParentFile();
        File result;
        if (i > 0 && i < filename.length() - 1) {
            String withoutExtension = filename.substring(0, i);
            result = new File(containingFolder, withoutExtension+FILE_EXTENSION_SEPARATOR+newExtension);
        }
        else {
            result = new File(containingFolder, filename+FILE_EXTENSION_SEPARATOR+newExtension);
        }
        return result;
    }
    
    public static File appendSuffixBeforeExtension(File f, String suffix) throws IOException {
        String filename = f.getName();
        int i = filename.lastIndexOf(FILE_EXTENSION_SEPARATOR);

        File containingFolder = f.getParentFile();
        File result;
        if (i > 0 && i < filename.length() - 1) {
            String withoutExtension = filename.substring(0, i);
            String extension = filename.substring(i);
            result = new File(containingFolder, withoutExtension+suffix+extension);
        }
        else {
            result = new File(containingFolder, filename+suffix);
        }
        if (!result.exists()) {
            result.createNewFile();
        }
        return result;
        
    }
    
    public static String getFilenameWithoutExtension(File f) {
        String filename = f.getName();
        int i = filename.lastIndexOf(FILE_EXTENSION_SEPARATOR);
        return i>=0 ?filename.substring(0, i) : filename; 
    }

    private static final char FILE_EXTENSION_SEPARATOR= '.';    
}
