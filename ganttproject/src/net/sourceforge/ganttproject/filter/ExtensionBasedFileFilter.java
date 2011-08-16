/*
 * Created on 05.12.2004
 */
package net.sourceforge.ganttproject.filter;

import java.io.File;
import java.util.regex.Pattern;

import javax.swing.filechooser.FileFilter;

/**
 * @author bard
 */
public class ExtensionBasedFileFilter extends FileFilter {
    private final String myDescription;

    private final Pattern myPattern;

    public ExtensionBasedFileFilter(String fileExtension, String description) {
        myDescription = description;
        myPattern = Pattern.compile(fileExtension);
    }

    @Override
    public boolean accept(File f) {
        if (f.isDirectory()) {
            return true;
        }
        return matches(getExtension(f));
    }

    @Override
    public String getDescription() {
        return myDescription;
    }

    /** @return extension of File f */
    private static String getExtension(File f) {
        String ext = null;
        String s = f.getName();
        int i = s.lastIndexOf('.');

        if (i > 0 && i < s.length() - 1) {
            ext = s.substring(i + 1).toLowerCase();
        }
        return ext;
    }

    private boolean matches(String fileExtension) {
        boolean result = fileExtension != null
                && myPattern.matcher(fileExtension).matches();
        return result;
    }
}
