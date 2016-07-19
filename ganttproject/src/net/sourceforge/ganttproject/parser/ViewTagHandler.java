/*
 * Created on 06.03.2005
 */
package net.sourceforge.ganttproject.parser;

import com.google.common.base.Objects;
import net.sourceforge.ganttproject.gui.UIFacade;
import org.xml.sax.Attributes;

/**
 * @author bard
 */
public class ViewTagHandler extends AbstractTagHandler {
  private final UIFacade myUIFacade;
  private final String myViewId;
  private final TaskDisplayColumnsTagHandler myFieldsHandler;

  public ViewTagHandler(String viewId, UIFacade uiFacade, TaskDisplayColumnsTagHandler fieldsHandler) {
    super("view");
    myViewId = viewId;
    myFieldsHandler = fieldsHandler;
    myUIFacade = uiFacade;
  }

  @Override
  protected boolean onStartElement(Attributes attrs) {
    if (Objects.equal(myViewId, attrs.getValue("id"))) {
      loadViewState(attrs);
      myFieldsHandler.setEnabled(true);
      return true;
    }
    return false;
  }

  @Override
  protected void onEndElement() {
    myFieldsHandler.setEnabled(false);
  }

  private void loadViewState(Attributes attrs) {
    myUIFacade.getZoomManager().setZoomState(attrs.getValue("zooming-state"));
  }
}
