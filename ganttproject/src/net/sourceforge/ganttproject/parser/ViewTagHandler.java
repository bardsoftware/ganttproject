/*
 * Created on 06.03.2005
 */
package net.sourceforge.ganttproject.parser;

import net.sourceforge.ganttproject.gui.UIFacade;

import org.xml.sax.Attributes;

/**
 * @author bard
 */
public class ViewTagHandler implements TagHandler {
  private final UIFacade myUIFacade;

  public ViewTagHandler(UIFacade uiFacade) {
    myUIFacade = uiFacade;
  }

  @Override
  public void startElement(String namespaceURI, String sName, String qName, Attributes attrs)
      throws FileFormatException {
    if ("view".equals(qName)) {
      loadViewState(attrs);
    }
  }

  private void loadViewState(Attributes attrs) {
    myUIFacade.getZoomManager().setZoomState(attrs.getValue("zooming-state"));
  }

  @Override
  public void endElement(String namespaceURI, String sName, String qName) {
  }

}
