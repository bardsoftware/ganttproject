package net.sourceforge.ganttproject.client;

import net.sourceforge.ganttproject.gui.options.model.DateOption;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.*;
import java.io.*;
import java.util.Date;
import java.util.Iterator;

class RssParser {

    private final XPathFactory myXPathFactory = XPathFactory.newInstance();

    private XPathExpression getXPath(String expression) throws XPathExpressionException {
        XPath xpath = myXPathFactory.newXPath();
        xpath.setNamespaceContext(new NamespaceContext() {
            @Override
            public String getNamespaceURI(String s) {
                if ("atom".equals(s)) {
                    return "http://www.w3.org/2005/Atom";
                }
                throw new IllegalArgumentException(s);
            }

            @Override
            public String getPrefix(String s) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Iterator getPrefixes(String s) {
                throw new UnsupportedOperationException();
            }
        });
        return xpath.compile(expression);
    }

    public RssFeed parse(InputStream inputStream, Date lastCheckDate) {
        StringBuilder buf = new StringBuilder();
        RssFeed result = new RssFeed();
        try {
            String xpathExpression = lastCheckDate == null ? "//atom:entry" : "//atom:entry";
            XPathExpression xpath = getXPath(xpathExpression);

            NodeList items = (NodeList) xpath.evaluate(
                new InputSource(new InputStreamReader(inputStream)), XPathConstants.NODESET);
            for (int i = 0; i < items.getLength(); i++) {
                addItem(result, items.item(i));
            }

        } catch (XPathExpressionException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        return result;
    }

    private void addItem(RssFeed result, Node item) throws XPathExpressionException {
        String title = getXPath("atom:title/text()").evaluate(item);
        String body = getXPath("atom:content/text()").evaluate(item);
        result.addItem(title, body);
    }
}
