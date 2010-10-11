/*
 * Created on 26.09.2005
 */
package org.ganttproject.impex.htmlpdf;

import java.net.URL;

class StylesheetImpl implements Stylesheet {
    private String myLocalizedName;
    private final URL myURL;
    StylesheetImpl(URL stylesheetURL, String localizedName) {
    	assert stylesheetURL!=null;
        myURL = stylesheetURL;
        myLocalizedName = localizedName;
    }
    public String getLocalizedName() {
        return myLocalizedName;
    }

    public String toString() {
        return getLocalizedName();
    }
    
    public URL getUrl() {
        return myURL;
    }
}