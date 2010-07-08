package org.ganttproject.impex.htmlpdf.fonts;

import java.awt.Font;
import java.awt.FontFormatException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;

import net.sourceforge.ganttproject.GPLogger;

import org.apache.fop.fonts.FontFileReader;
import org.apache.fop.fonts.TTFFile;

/**
 * Created by IntelliJ IDEA.
 * 
 * @author bard Date: 07.01.2004
 */
public class JDKFontLocator {
    private FontMetricsStorage myFontMetricsStorage = new FontMetricsStorage();

    public FontRecord[] getFontRecords() {
        String javaHome = System.getProperty("java.home");
        File fontDirectory = new File(javaHome + "/lib/fonts");
        // TTFReader ttfReader = new TTFReader();
        File[] children = fontDirectory.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".ttf");
            }
        });
        if (children == null) {
            children = new File[0];
        }
        ArrayList result = new ArrayList(children.length);
        for (int i = 0; i < children.length; i++) {
            try {
                FontRecord record = new FontRecord(children[i],
                        myFontMetricsStorage);
                if (record.getMetricsLocation() != null) {
                    populateWithTriplets(record);
                    result.add(record);
                }
            } catch (IOException e) {
            	if (!GPLogger.log(e)) {
            		e.printStackTrace(System.err);
            	}
            } catch (IndexOutOfBoundsException e) {
            	if (!GPLogger.log(e)) {
            		e.printStackTrace(System.err);
            	}
            }

        }
        return (FontRecord[]) result.toArray(new FontRecord[0]);
    }

    private void populateWithTriplets(FontRecord record) {
        TTFFileExt ttfFile = record.getTTFFile();
        boolean isItalic = ttfFile.isItalic();
        boolean isBold = ttfFile.isBold();
        String name = ttfFile.getFamilyName();
        FontTriplet triplet = new FontTriplet(name, isItalic, isBold);
        record.addTriplet(triplet);
        if (name.toLowerCase().indexOf("typewriter") >= 0) {
            FontTriplet monospaceTriplet = new FontTriplet("monospace",
                    isItalic, isBold);
            record.addTriplet(monospaceTriplet);
        } else if (name.toLowerCase().indexOf("sans") >= 0) {
            FontTriplet sansTriplet = new FontTriplet("sans-serif", isItalic,
                    isBold);
            record.addTriplet(sansTriplet);
        } else {
            FontTriplet serifTriplet = new FontTriplet("serif", isItalic,
                    isBold);
            record.addTriplet(serifTriplet);
        }
    }
}

class TTFFileExt extends TTFFile {
    private final File myFile;

    private Font myAwtFont;

    TTFFileExt(File file) throws IOException {
        if (!file.exists()) {
            throw new RuntimeException("File=" + file + " does not exist");
        }
        System.err.println("[TTFileExt] <ctor> file=" + file.getAbsolutePath());
        myFile = file;
        FontFileReader reader = new FontFileReader(file.getCanonicalPath());
        readFont(reader);
    }

    public boolean isItalic() {
        return Integer.parseInt(getItalicAngle()) >> 16 != 0;
    }

    public boolean isBold() {
        return getAwtFont().isBold();
    }

    public File getFile() {
        return myFile;
    }

    private Font getAwtFont() {
        if (myAwtFont == null) {
            try {
                myAwtFont = Font.createFont(Font.TRUETYPE_FONT,
                        new FileInputStream(getFile()));
            } catch (FontFormatException e) {
            	if (!GPLogger.log(e)) {
            		e.printStackTrace(System.err);
            	}
            } catch (IOException e) {
            	if (!GPLogger.log(e)) {
            		e.printStackTrace(System.err);
            	}
            }
        }
        return myAwtFont;
    }
}
