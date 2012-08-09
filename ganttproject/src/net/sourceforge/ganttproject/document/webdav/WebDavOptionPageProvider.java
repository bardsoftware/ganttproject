package net.sourceforge.ganttproject.document.webdav;

import java.awt.Component;

import javax.swing.JPanel;

import org.jdesktop.swingx.JXTable;

import net.sourceforge.ganttproject.gui.AbstractTableAndActionsComponent;
import net.sourceforge.ganttproject.gui.options.OptionPageProviderBase;
import net.sourceforge.ganttproject.gui.options.model.GPOptionGroup;

public class WebDavOptionPageProvider extends OptionPageProviderBase {


  public WebDavOptionPageProvider() {
    super("storage.webdav");
    // TODO Auto-generated constructor stub
  }

  @Override
  public GPOptionGroup[] getOptionGroups() {
    // TODO Auto-generated method stub
    return new GPOptionGroup[0];
  }

  @Override
  public boolean hasCustomComponent() {
    return true;
  }

  @Override
  public Component buildPageComponent() {
    WebDavStorageImpl webdavStorage = (WebDavStorageImpl) getProject().getDocumentManager().getWebDavStorageUi();
    JXTable table = ServerListEditor.createTable(ServerListEditor.createTableModel(webdavStorage.getServersOption()));
    AbstractTableAndActionsComponent<WebDavServerDescriptor> tableAndAction = new AbstractTableAndActionsComponent<WebDavServerDescriptor>(table) {
      @Override
      protected void onAddEvent() {
      }

      @Override
      protected void onDeleteEvent() {
      }

      @Override
      protected void onSelectionChanged() {
      }
    };
    JPanel panel = AbstractTableAndActionsComponent.createDefaultTableAndActions(table, tableAndAction.getActionsComponent());
    return OptionPageProviderBase.wrapContentComponent(panel, "WebDAV servers", "foo");
  }
}
