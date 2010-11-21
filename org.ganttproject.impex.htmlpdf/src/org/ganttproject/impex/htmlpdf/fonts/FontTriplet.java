package org.ganttproject.impex.htmlpdf.fonts;

/**
 * Created by IntelliJ IDEA. User: bard Date: 07.01.2004 Time: 18:26:37
 */
public class FontTriplet {
    private String myName;

    private boolean isItalic;

    private boolean isBold;

    public FontTriplet(String name, boolean italic, boolean bold) {
        myName = name;
        isItalic = italic;
        isBold = bold;
    }

    public String getName() {
        return myName;
    }

    public boolean isItalic() {
        return isItalic;
    }

    public boolean isBold() {
        return isBold;
    }
}
