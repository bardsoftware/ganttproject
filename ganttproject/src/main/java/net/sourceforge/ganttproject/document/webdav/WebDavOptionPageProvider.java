package net.sourceforge.ganttproject.document.webdav;

import biz.ganttproject.core.option.ChangeValueEvent;
import biz.ganttproject.core.option.ChangeValueListener;
import biz.ganttproject.core.option.DefaultBooleanOption;
import biz.ganttproject.core.option.DefaultStringOption;
import biz.ganttproject.core.option.GPOptionGroup;
import biz.ganttproject.core.option.ListOption;
import com.google.common.collect.Lists;
import net.sourceforge.ganttproject.gui.AbstractTableAndActionsComponent.SelectionListener;
import net.sourceforge.ganttproject.gui.EditableList;
import net.sourceforge.ganttproject.gui.options.OptionPageProviderBase;
import net.sourceforge.ganttproject.gui.options.OptionsPageBuilder;
import net.sourceforge.ganttproject.language.GanttLanguage;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;
import java.util.List;

public class WebDavOptionPageProvider extends OptionPageProviderBase {
  /** Neither half of the page is squeezed below this width, so that the divider stays movable. */
  private static final int MIN_HALF_WIDTH = 100;

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
  public JComponent buildPageComponent() {
    WebDavStorageImpl webdavStorage = (WebDavStorageImpl) getProject().getDocumentManager().getWebDavStorageUi();
    final ListOption<WebDavServerDescriptor> serversOption = webdavStorage.getServersOption();
    final EditableList<WebDavServerDescriptor> serverList = new EditableList<WebDavServerDescriptor>(
        Lists.newArrayList(serversOption.getValues()), Collections.EMPTY_LIST) {

          @Override
          protected WebDavServerDescriptor updateValue(WebDavServerDescriptor newValue, WebDavServerDescriptor curValue) {
            newValue.setUsername(curValue.getUsername());
            newValue.setPassword(curValue.getPassword());
            newValue.setRootUrl(curValue.getRootUrl());
            serversOption.updateValue(curValue, newValue);
            return newValue;
          }

          @Override
          protected WebDavServerDescriptor createValue(WebDavServerDescriptor prototype) {
            serversOption.addValue(prototype);
            return prototype;
          }

          @Override
          protected void deleteValue(WebDavServerDescriptor value) {
            serversOption.removeValueIndex(findIndex(value));
          }

          private int findIndex(WebDavServerDescriptor value) {
            return Lists.newArrayList(serversOption.getValues()).indexOf(value);
          }

          @Override
          protected WebDavServerDescriptor createPrototype(Object editValue) {
            return new WebDavServerDescriptor(String.valueOf(editValue), "", "");
          }

          @Override
          protected String getStringValue(WebDavServerDescriptor t) {
            return t.getName();
          }
    };
    serverList.getTableComponent().setPreferredSize(new Dimension(150, 300));
    serverList.setUndefinedValueLabel(GanttLanguage.getInstance().getText("webdav.serverNamePrompt"));

    final DefaultStringOption urlOption = new DefaultStringOption("webdav.server.url");
    urlOption.addChangeValueListener(new ChangeValueListener() {
      @Override
      public void changeValue(ChangeValueEvent event) {
        if (serverList.getSelectedObject() != null) {
          serverList.getSelectedObject().setRootUrl(urlOption.getValue());
        }
      }
    });

    final DefaultStringOption usernameOption = new DefaultStringOption("webdav.server.username");
    usernameOption.addChangeValueListener(new ChangeValueListener() {
      @Override
      public void changeValue(ChangeValueEvent event) {
        if (serverList.getSelectedObject() != null) {
          serverList.getSelectedObject().setUsername(usernameOption.getValue());
        }
      }
    });

    final DefaultStringOption passwordOption = new DefaultStringOption("webdav.server.password");
    passwordOption.addChangeValueListener(new ChangeValueListener() {
      @Override
      public void changeValue(ChangeValueEvent event) {
        if (serverList.getSelectedObject() != null) {
          serverList.getSelectedObject().setPassword(passwordOption.getValue());
        }
      }
    });
    passwordOption.setScreened(true);

    final DefaultBooleanOption savePasswordOption = new DefaultBooleanOption("webdav.server.savePassword", false);
    savePasswordOption.addChangeValueListener(new ChangeValueListener() {
      @Override
      public void changeValue(ChangeValueEvent event) {
        if (serverList.getSelectedObject() != null) {
          serverList.getSelectedObject().setSavePassword(savePasswordOption.getValue());
        }
      }
    });

    GPOptionGroup optionGroup = new GPOptionGroup("webdav.server", urlOption, usernameOption, passwordOption, savePasswordOption);

    serverList.getTableAndActions().addSelectionListener(new SelectionListener<WebDavServerDescriptor>() {
      @Override
      public void selectionChanged(List<WebDavServerDescriptor> selection) {
        if (selection.size() == 1) {
          WebDavServerDescriptor selected = selection.get(0);
          urlOption.setValue(selected.getRootUrl());
          usernameOption.setValue(selected.getUsername());
          passwordOption.setValue(selected.getPassword());
          savePasswordOption.setValue(selected.getSavePassword());
        }
      }
    });
    int selected = Lists.newArrayList(serversOption.getValues()).indexOf(serversOption.getValue());
    if (selected >= 0) {
      serverList.getTableAndActions().setSelection(selected);
    }
    //Box result = Box.createHorizontalBox();
    JPanel serversPanel = new JPanel(new BorderLayout());
    serversPanel.add(serverList.createDefaultComponent(), BorderLayout.CENTER);

    OptionsPageBuilder builder = new OptionsPageBuilder();
    GPOptionGroup lockingGroup = new GPOptionGroup("webdav.lock", webdavStorage.getWebDavLockTimeoutOption(), webdavStorage.getWebDavReleaseLockOption());
    lockingGroup.setI18Nkey(builder.getI18N().getCanonicalOptionLabelKey(webdavStorage.getWebDavLockTimeoutOption()), "webdav.lockTimeout.label");
    lockingGroup.setI18Nkey(builder.getI18N().getCanonicalOptionLabelKey(webdavStorage.getWebDavReleaseLockOption()), "option.webdav.lock.releaseOnProjectClose.label");
    serversPanel.add(builder.buildPlanePage(new GPOptionGroup[] {lockingGroup}), BorderLayout.SOUTH);

    builder = new OptionsPageBuilder(null, OptionsPageBuilder.ONE_COLUMN_LAYOUT);
    JComponent serverDetails = builder.buildPlanePage(new GPOptionGroup[] {optionGroup});
    serverDetails.setPreferredSize(new Dimension(300, 300));

    // BorderLayout gave the server list its full preferred width and the server details only what
    // was left, even when that was negative. On a page narrower than the list, the fields for
    // address, user name and password were not on screen at all. A split pane leaves both halves
    // visible and lets the user move the boundary.
    //
    // The resize weight of 0.5 makes both halves grow and shrink together. With the default weight
    // of 0 the list keeps its preferred width and the details take the rest, which matches the old
    // BorderLayout while there is room, but as soon as the page is a little too narrow the list
    // drops to its minimum width in one step. Longer translations of the locking labels are enough
    // to reach that point.
    //
    // The minimum widths are what keeps the divider movable: without them it is pinned to the
    // list's own minimum width and cannot be dragged at all on a narrow page.
    serversPanel.setMinimumSize(new Dimension(MIN_HALF_WIDTH, 0));
    serverDetails.setMinimumSize(new Dimension(MIN_HALF_WIDTH, 0));
    JSplitPane result = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, serversPanel, serverDetails);
    result.setResizeWeight(0.5);
    result.setBorder(BorderFactory.createEmptyBorder());
    return OptionPageProviderBase.wrapContentComponent(result, getCanonicalPageTitle(), null);
  }
}
