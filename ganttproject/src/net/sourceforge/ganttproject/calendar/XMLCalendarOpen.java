/**
 * 
 */
package net.sourceforge.ganttproject.calendar;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.filechooser.FileFilter;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import net.sourceforge.ganttproject.parser.FileFormatException;
import net.sourceforge.ganttproject.parser.ParsingListener;
import net.sourceforge.ganttproject.parser.TagHandler;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * @author nbohn
 */
public class XMLCalendarOpen {

    //private File myCalendarFiles[];

    private List<URL> myCalendarResources = new ArrayList<URL>();
    private String myCalendarLabels[];

    /** The main frame */
    private ArrayList<TagHandler> myTagHandlers = new ArrayList<TagHandler>();

    private ArrayList<ParsingListener> myListeners = new ArrayList<ParsingListener>();

    boolean load(InputStream inputStream) throws ParserConfigurationException,
            SAXException, IOException {
        // Use an instance of ourselves as the SAX event handler
        DefaultHandler handler = new GanttXMLParser();

        // Use the default (non-validating) parser
        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser saxParser = factory.newSAXParser();
        saxParser.parse(inputStream, handler);
        return true;
    }

    void addTagHandler(TagHandler handler) {
        myTagHandlers.add(handler);
    }

    void addParsingListener(ParsingListener listener) {
        myListeners.add(listener);
    }

    private TagHandler getDefaultTagHandler() {
        return new DefaultTagHandler();
    }

    private class DefaultTagHandler implements TagHandler {
        private String name;

        public void startElement(String namespaceURI, String sName,
                String qName, Attributes attrs) {
            String eName = qName; // element name
            if (attrs != null) {
                for (int i = 0; i < attrs.getLength(); i++) {
                    String aName = attrs.getLocalName(i); // Attr name
                    if ("".equals(aName)) {
                        aName = attrs.getQName(i);

                        // The project part
                    }
                    if (eName.equals("calendar")) {
                        if (aName.equals("name")) {
                            name = attrs.getValue(i);
                        } else if (aName.equals("type")) {
                        }
                    }
                }
            }
        }

        public void endElement(String namespaceURI, String sName, String qName) {
        }

        public String getName() {
            return name;
        }

    }

    private class GanttXMLParser extends DefaultHandler {

        // ===========================================================
        // SAX DocumentHandler methods
        // ===========================================================

        public void endDocument() throws SAXException {
            for (int i = 0; i < myListeners.size(); i++) {
                ParsingListener l = myListeners.get(i);
                l.parsingFinished();
            }
        }

        public void startElement(String namespaceURI, String sName, // simple name
                String qName, // qualified name
                Attributes attrs) throws SAXException {
            for (Iterator<TagHandler> handlers = myTagHandlers.iterator(); handlers
                    .hasNext();) {
                TagHandler next = handlers.next();
                try {
                    next.startElement(namespaceURI, sName, qName, attrs);
                } catch (FileFormatException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        public void endElement(String namespaceURI, String sName, String qName)
                throws SAXException {
            for (Iterator<TagHandler> handlers = myTagHandlers.iterator(); handlers
                    .hasNext();) {
                TagHandler next = handlers.next();
                next.endElement(namespaceURI, sName, qName);
            }
        }
    }

    public void setCalendars() throws Exception {
        // TODO The loading of the calendar is not clear. There are 2 cases here
        // :
        // - using eclipse where test is a directory
        // - using the built jar archive where test is not a directory
    
//        URL url = getClass().getResource("/calendar");
//        URL resolvedUrl = Platform.resolve(url);
//        File test = new File(resolvedUrl.getPath());
//        File path = new File(URLDecoder.decode(test.getAbsolutePath()));

        // if(test.isDirectory()) {
        //     path = test;
        // } else {
        //     path = new File("calendar");
        // }
        myCalendarResources.clear();
        DefaultTagHandler th = (DefaultTagHandler) getDefaultTagHandler();
        addTagHandler(th);
        IConfigurationElement[] calendarExtensions = Platform.getExtensionRegistry().getConfigurationElementsFor(GPCalendar.EXTENSION_POINT_ID);
//        myCalendarFiles = path.listFiles(new Filter(".calendar"));
        myCalendarLabels = new String[calendarExtensions.length];
        for (int i = 0; i < calendarExtensions.length; i++) {
            Bundle nextBundle = Platform.getBundle(calendarExtensions[i].getDeclaringExtension().getNamespace());
            URL calendarUrl = nextBundle.getResource(calendarExtensions[i].getAttribute("resource-url"));
            if (calendarUrl != null) {
                load(calendarUrl.openStream());
                myCalendarLabels[i] = th.getName();
                myCalendarResources.add(calendarUrl);
            }
        }
    }

    public URL[] getCalendarResources() {
        return (URL[]) myCalendarResources.toArray(new URL[0]);
    }

    public String[] getLabels() {
        return myCalendarLabels;
    }

    // Class is never used... delete?
    private static class Filter extends FileFilter implements FilenameFilter {
        private String extension;

        public Filter(String extension) {
            if (extension == null) {
                throw new NullPointerException(
                        "The description (or extension) can not be null.");
            }
            this.extension = extension;
        }

        public String getDescription() {
            // TODO Auto-generated method stub
            return null;
        }

        public boolean accept(File file, String arg1) {
            return arg1.endsWith(extension);
        }

        public boolean accept(File arg0) {
            // TODO Auto-generated method stub
            return false;
        }
    }

}