/*
 * Created on 06.03.2005
 */
package net.sourceforge.ganttproject.io;

import javax.xml.transform.sax.TransformerHandler;

import net.sourceforge.ganttproject.gui.TableHeaderUIFacade;
import net.sourceforge.ganttproject.gui.UIFacade;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * @author bard
 */
class ViewSaver extends SaverBase {
    public void save(UIFacade facade, TransformerHandler handler)
            throws SAXException {
        AttributesImpl attrs = new AttributesImpl();
        addAttribute("zooming-state", facade.getZoomManager().getZoomState()
                .getPersistentName(), attrs);
        addAttribute("id", "gantt-chart", attrs);
        emptyElement("view", attrs, handler);
        
    	addAttribute("id", "resource-table", attrs);
    	startElement("view", attrs, handler);
    	writeColumns(facade.getResourceTree().getVisibleFields(), handler);
    	
    	endElement("view", handler);
        
    }

    protected void writeColumns(TableHeaderUIFacade visibleFields, TransformerHandler handler) throws SAXException {
    	AttributesImpl attrs = new AttributesImpl();
    	int totalWidth = 0;
    	for (int i=0; i<visibleFields.getSize(); i++) {
    		if (visibleFields.getField(i).isVisible()) {
    			totalWidth += visibleFields.getField(i).getWidth();
    		}
    	}    	
    	for (int i=0; i<visibleFields.getSize(); i++) {
    		TableHeaderUIFacade.Column field = visibleFields.getField(i);
    		if (field.isVisible()) {
	    		addAttribute("id", field.getID(), attrs);
	    		addAttribute("name", field.getName(), attrs);
	    		addAttribute("width", field.getWidth()*100/totalWidth, attrs);
	    		addAttribute("order", field.getOrder(), attrs);
	    		emptyElement("field", attrs, handler);
    		}
    	}    	
    }
    
}
