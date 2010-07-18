package net.sourceforge.ganttproject.io;

import javax.xml.transform.sax.TransformerHandler;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

class SaverBase {
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

    protected void addAttribute(String name, boolean value, AttributesImpl attrs) {
        addAttribute(name, Boolean.toString(value), attrs);
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
