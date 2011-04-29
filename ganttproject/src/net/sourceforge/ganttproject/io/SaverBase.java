package net.sourceforge.ganttproject.io;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

public class SaverBase {

    protected TransformerHandler createHandler(Result result) throws TransformerConfigurationException {
        SAXTransformerFactory factory = (SAXTransformerFactory) SAXTransformerFactory
                .newInstance();
        TransformerHandler handler = factory.newTransformerHandler();
        Transformer serializer = handler.getTransformer();
        serializer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        serializer.setOutputProperty(OutputKeys.INDENT, "yes");
        serializer.setOutputProperty(OutputKeys.METHOD, "xml");
        serializer.setOutputProperty(
                "{http://xml.apache.org/xslt}indent-amount", "4");
        handler.setResult(result);
        return handler;
    }

    protected void startElement(String name, TransformerHandler handler)
            throws SAXException {
        startElement(name, ourEmptyAttributes, handler);
    }

    protected void startElement(String name, AttributesImpl attrs,
            TransformerHandler handler) throws SAXException {
        handler.startElement("", name, name, attrs);
        attrs.clear();
    }

    protected void endElement(String name, TransformerHandler handler)
            throws SAXException {
        handler.endElement("", name, name);
    }

    protected void addAttribute(String name, String value, AttributesImpl attrs) {
    	if (value!=null) { 
    		attrs.addAttribute("", name, name, "CDATA", value);
    	}
    }

	protected void addAttribute(String name, int value, AttributesImpl attrs) {
		addAttribute(name, String.valueOf(value), attrs);
	}

    protected void addAttribute(String name, Boolean value, AttributesImpl attrs) {
        addAttribute(name, value.toString(), attrs);
    }

    protected void emptyElement(String name, AttributesImpl attrs,
            TransformerHandler handler) throws SAXException {
        startElement(name, attrs, handler);
        endElement(name, handler);
        attrs.clear();
    }

    protected void cdataElement(String name, String cdata, AttributesImpl attrs, TransformerHandler handler) throws SAXException {
        startElement(name, handler);
        handler.startCDATA();
        handler.characters(cdata.toCharArray(), 0, cdata.length());
        handler.endCDATA();
        endElement(name, handler);
    }
    protected void emptyComment(TransformerHandler handler) throws SAXException {
        handler.comment(new char[] { ' ' }, 0, 1);

    }

    private static AttributesImpl ourEmptyAttributes = new AttributesImpl();

}
