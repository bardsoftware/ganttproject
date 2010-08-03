/*
 * Created on 26.09.2005
 */
package org.ganttproject.impex.htmlpdf;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamSource;

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