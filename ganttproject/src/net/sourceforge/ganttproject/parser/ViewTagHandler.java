/*
 * Created on 06.03.2005
 */
package net.sourceforge.ganttproject.parser;

import net.sourceforge.ganttproject.gui.UIFacade;

import org.xml.sax.Attributes;

/**
 * @author bard
 */
public class ViewTagHandler extends AbstractTagHandler {
  private final UIFacade myUIFacade;

  public ViewTagHandler(UIFacade uiFacade) {
    super("view");
    myUIFacade = uiFacade;
  }

  @Override
  protected boolean onStartElement(Attributes attrs) {
    loadViewState(attrs);
    return true;
  }

  private void loadViewState(Attributes attrs) {
    myUIFacade.getZoomManager().setZoomState(attrs.getValue("zooming-state"));
  }
}
