/*
 * Created on Mar 14, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package net.sourceforge.ganttproject.parser;

import java.util.ArrayList;
import java.util.List;

import net.sourceforge.ganttproject.gui.TableHeaderUIFacade;

import org.xml.sax.Attributes;

/**
 * @author bbaranne Mar 14, 2005
 */
public class TaskDisplayColumnsTagHandler implements TagHandler,
        ParsingListener {

	private final TableHeaderUIFacade myVisibleFields;
	private final TableHeaderUIFacade myBuffer = new VisibleFieldsImpl();
	private final String myIDPropertyName;
	private final String myOrderPropertyName;
	private final String myWidthPropertyName;
	private final String myTagName;

    public TaskDisplayColumnsTagHandler(TableHeaderUIFacade visibleFields) {
    	this(visibleFields, "displaycolumn", "property-id", "order", "width");
    }
    public TaskDisplayColumnsTagHandler(
    		TableHeaderUIFacade visibleFields, 
    		String tagName, String idPropertyName, String orderPropertyName, String widthPropertyName) {
        myVisibleFields = visibleFields;
        myTagName = tagName;
        myIDPropertyName = idPropertyName;
        myOrderPropertyName = orderPropertyName;
        myWidthPropertyName = widthPropertyName;
    }

    public void startElement(String namespaceURI, String sName, String qName,
            Attributes attrs) throws FileFormatException {
        if (qName.equals(myTagName))
            loadTaskDisplay(attrs);

    }

    public void endElement(String namespaceURI, String sName, String qName) {
        // TODO Auto-generated method stub
    }

    public void parsingStarted() {
    	myVisibleFields.clear();
    }

    public void parsingFinished() {
    	myVisibleFields.importData(myBuffer);
    }

    private void loadTaskDisplay(Attributes atts) {
        String id = atts.getValue(myIDPropertyName);
        String orderStr = atts.getValue(myOrderPropertyName);
        if (orderStr==null) {
        	orderStr = String.valueOf(myBuffer.getSize());
        }
        String widthStr = atts.getValue(myWidthPropertyName);
        int order = Integer.parseInt(orderStr);
        int width = widthStr==null ? -1 : Integer.parseInt(widthStr); 
        myBuffer.add(id, order, width);
    }    

    private static class TaskFieldImpl implements TableHeaderUIFacade.Column {
    	private final String myID;
		private final int myOrder;
		private final int myWidth;

		TaskFieldImpl(String id, int order, int width) {
    		myID = id;
    		myOrder = order;
    		myWidth = width;
    	}
		public String getID() {
			return myID;
		}

		public int getOrder() {
			return myOrder;
		}

		public int getWidth() {
			return myWidth;
		}
		public boolean isVisible() {
			return true;
		}
		public String getName() {
			return null;
		}    	
    }

    private static class VisibleFieldsImpl implements TableHeaderUIFacade {
		private List<TaskFieldImpl> myFields = new ArrayList<TaskFieldImpl>();
		public void add(String name, int order, int width) {
			myFields.add(new TaskFieldImpl(name, order, width));
		}
		public void clear() {
			myFields.clear();
		}
		public Column getField(int index) {
			return myFields.get(index);
		}
		public int getSize() {
			return myFields.size();
		}
		public void importData(TableHeaderUIFacade source) {
			throw new UnsupportedOperationException();
		}
	}
}
