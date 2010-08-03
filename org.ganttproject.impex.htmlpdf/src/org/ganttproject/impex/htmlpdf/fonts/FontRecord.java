package org.ganttproject.impex.htmlpdf.fonts;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * 
 * @author bard Date: 07.01.2004
 */
public class FontRecord {
    private URI myLocation;

    private URI myMetricsLocation;

    private ArrayList myTriplets = new ArrayList();

    private TTFFileExt myTTFFile;

    public FontRecord(File fontFile, FontMetricsStorage metricsStorage)
            throws IOException {
        myTTFFile = new TTFFileExt(fontFile);
        myLocation = fontFile.toURI();
        myMetricsLocation = metricsStorage.getFontMetricsURI(myTTFFile);
    }

    public FontRecord(URI fontLocation, URI metricsLocation) {
        myLocation = fontLocation;
        myMetricsLocation = metricsLocation;
    }

    public void addTriplet(FontTriplet triplet) {
        myTriplets.add(triplet);
    }

    public URI getFontLocation() {
        return myLocation;
    }

    public URI getMetricsLocation() {
        return myMetricsLocation;
    }

    public FontTriplet[] getFontTriplets() {
        return (FontTriplet[]) myTriplets.toArray(new FontTriplet[0]);
    }

    public TTFFileExt getTTFFile() {
        return myTTFFile;
    }

    public String toString() {
        return "font file=" + myLocation + " metrics file=" + myMetricsLocation;
    }
}
