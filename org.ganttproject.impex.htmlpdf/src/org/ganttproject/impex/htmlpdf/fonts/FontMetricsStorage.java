package org.ganttproject.impex.htmlpdf.fonts;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;

import net.sourceforge.ganttproject.GPLogger;

/**
 * Created by IntelliJ IDEA.
 * 
 * @author bard Date: 07.01.2004
 */
public class FontMetricsStorage {
    public URI getFontMetricsURI(TTFFileExt ttfFile) {
        URI result = null;
        String fontName = ttfFile.getFile().getName();
        String resourceName = "font-metrics/" + fontName + ".xml";
        URL resourceUrl = getClass().getClassLoader().getResource(resourceName);

        try {
            result = resourceUrl == null ? 
            		null : 
            		new URI(URLEncoder.encode(resourceUrl.toString(), "utf-8"));
        } catch (URISyntaxException e) {
        	if (!GPLogger.log(e)) {
        		e.printStackTrace(System.err);
        	}
        } catch (UnsupportedEncodingException e) {
        	if (!GPLogger.log(e)) {
        		e.printStackTrace(System.err);
        	}
		}
        return result;
    }
}
