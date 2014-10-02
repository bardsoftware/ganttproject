/*
GanttProject is an opensource project management tool. License: GPL3
Copyright (C) 2003-2012 GanttProject Team

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 3
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
*/

package net.sourceforge.ganttproject.document.webdav;

import io.milton.http.exceptions.NotAuthorizedException;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SpringLayout;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.action.CancelAction;
import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.action.OkAction;
import net.sourceforge.ganttproject.document.webdav.WebDavResource.WebDavException;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.UIUtil;
import net.sourceforge.ganttproject.gui.options.OptionsPageBuilder;
import net.sourceforge.ganttproject.gui.options.SpringUtilities;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.util.collect.Pair;

import org.divxdede.swing.busy.JBusyComponent;
import org.jdesktop.swingx.JXHyperlink;
import org.jdesktop.swingx.JXList;

import biz.ganttproject.core.option.BooleanOption;
import biz.ganttproject.core.option.ChangeValueEvent;
import biz.ganttproject.core.option.ChangeValueListener;
import biz.ganttproject.core.option.DefaultStringOption;
import biz.ganttproject.core.option.EnumerationOption;
import biz.ganttproject.core.option.IntegerOption;
import biz.ganttproject.core.option.ListOption;
import biz.ganttproject.core.option.StringOption;

import com.google.common.base.Objects;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

/**
 * UI component for WebDAV operarions.
 *
 * @author dbarashev (Dmitry Barashev)
 */
class GanttURLChooser {
  private static final GanttLanguage language = GanttLanguage.getInstance();

  private final StringOption myUsername;

  private final StringOption myPassword;

  private final StringOption myPath;

  private final IntegerOption myTimeout;

  private SelectionListener mySelectionListener;

  private final ListOption<WebDavServerDescriptor> myServers;

  private final MiltonResourceFactory myWebDavFactory;

  private final GPAction myReloadAction = new GPAction("fileChooser.reload") {
    @Override
    public void actionPerformed(ActionEvent event) {
      if (!isEnabled()) {
        return;
      }
      reloadFilesTable();
    }
  };

  private final GPAction myUpAction = new GPAction("fileChooser.up") {
    @Override
    public void actionPerformed(ActionEvent event) {
      try {
        WebDavResource collection = tableModel.getCollection();
        WebDavResource parent = collection.getParent();
        if (parent != null && parent.exists() && parent.isCollection()) {
          new ReloadWorker(parent).execute();
          myPath.setValue(new URL(parent.getUrl()).getPath());
        }
      } catch (WebDavException e) {
        showError(e);
      } catch (MalformedURLException e) {
        showError(e);
      }
    }
  };

  private final GPAction myLockAction = new GPAction("fileChooser.lock") {
    @Override
    public void actionPerformed(ActionEvent evt) {
      WebDavResource resource = getSelectedResource();
      if (resource == null) {
        return;
      }
      try {
        if (!resource.isLocked()) {
          resource.lock(myTimeout.getValue() * 60);
          reloadFilesTable();
        }
      } catch (WebDavException e) {
        showError(e);
      }
    }
  };

  private final GPAction myUnlockAction = new GPAction("fileChooser.unlock") {
    @Override
    public void actionPerformed(ActionEvent evt) {
      WebDavResource resource = getSelectedResource();
      if (resource == null) {
        return;
      }
      try {
        if (resource.isLocked()) {
          resource.unlock();
          reloadFilesTable();
        }
      } catch (WebDavException e) {
        showError(e);
      }
    }
  };

  private final GPAction myDeleteAction = new GPAction("fileChooser.delete") {
    @Override
    public void actionPerformed(ActionEvent evt) {
      final WebDavResource resource = getSelectedResource();
      if (resource == null) {
        return;
      }
      myUiFacade.showOptionDialog(JOptionPane.QUESTION_MESSAGE, GanttLanguage.getInstance().getText("fileChooser.delete.question"), new Action[] {
        new OkAction() {
          @Override
          public void actionPerformed(ActionEvent evt) {
            try {
              resource.delete();
            } catch (WebDavException e) {
              showError(e);
            }
          }
        }, CancelAction.EMPTY
      });
    }
  };

  private final FilesTableModel tableModel = new FilesTableModel();

  private JXList table;

  private JButton myLockButton;

  private final UIFacade myUiFacade;

  private final IGanttProject myProject;

  private JScrollPane myFilesComponent;

  private WebDavUri myInitialUri;

  private JLabel myPasswordLabel;

  private OkAction myOkAction;

  private final List<Runnable> myListenerRemovers = Lists.newArrayList();

  private JBusyComponent<JComponent> myBusyComponent;

  static interface SelectionListener {
    public void setSelection(WebDavResource resource);
  }

  GanttURLChooser(IGanttProject project, UIFacade uiFacade, ListOption<WebDavServerDescriptor> servers, WebDavUri currentUri, StringOption username,
      StringOption password, IntegerOption lockTimeoutOption, BooleanOption releaseLockOption,
      MiltonResourceFactory webDavFactory) {
    myProject = project;
    myUiFacade = uiFacade;
    myWebDavFactory = webDavFactory;
    myPath = new DefaultStringOption("path");
    myServers = servers;
    myUsername = username;
    myPassword = password;
    myTimeout = lockTimeoutOption;
    myInitialUri = currentUri;

    myListenerRemovers.add(myServers.addChangeValueListener(new ChangeValueListener() {
      @Override
      public void changeValue(ChangeValueEvent event) {
        myPath.setValue("");
        updateUsernameAndPassword();
        myReloadAction.actionPerformed(null);
      }
    }));

    myListenerRemovers.add(myUsername.addChangeValueListener(new ChangeValueListener() {
      @Override
      public void changeValue(ChangeValueEvent event) {
        if (myServers.getValue() != null) {
          myServers.getValue().username = myUsername.getValue();
        }
      }
    }));

    myListenerRemovers.add(myPassword.addChangeValueListener(new ChangeValueListener() {
      @Override
      public void changeValue(ChangeValueEvent event) {
        if (myServers.getValue() != null) {
          myServers.getValue().password = myPassword.getValue();
        }
      }
    }));
  }

  class ReloadWorker extends SwingWorker<Pair<WebDavResource, List<WebDavResource>>, Object> {
    private WebDavResource myResource;

    public ReloadWorker(WebDavResource resource) {
      myResource = resource;
      setProgressBar(true);
    }
    @Override
    protected Pair<WebDavResource, List<WebDavResource>> doInBackground() throws Exception {
      try {
        WebDavResource resource = myResource;
        if (resource.exists() && resource.isCollection()) {
          return Pair.create(resource, readChildren(resource));
        }
        WebDavResource parent = resource.getParent();
        if (parent.exists() && parent.isCollection()) {
          return Pair.create(parent, readChildren(parent));
        }
        return null;
      } catch (WebDavException e) {
        showError(e);
        return null;
      }
    }

    private List<WebDavResource> readChildren(WebDavResource parent) throws WebDavException {
      List<WebDavResource> children = Lists.newArrayList();
      for (WebDavResource child : parent.getChildResources()) {
        try {
          if (child.exists()) {
            children.add(child);
          }
        }
        catch (WebDavException e) {
          GPLogger.logToLogger(e);
        }
      }
      return children;
    }

    @Override
    protected void done() {
      try {
        Pair<WebDavResource, List<WebDavResource>> result = get();
        if (result != null) {
          tableModel.setCollection(result.first(), result.second());
        }
      } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      } catch (ExecutionException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      } finally {
        setProgressBar(false);
      }
    }
  };

  protected void reloadFilesTable() {
    myWebDavFactory.clearCache();
    myWebDavFactory.setCredentials(myUsername.getValue(), myPassword.getValue());
    WebDavUri webDavUri = buildUrl();
    if (webDavUri != null) {
      new ReloadWorker(myWebDavFactory.createResource(webDavUri)).execute();
    }
  }

  private boolean tryApplyUrl(WebDavUri currentUri) {
    WebDavServerDescriptor savedServer = findSavedServer(currentUri.buildRootUrl());
    if (savedServer == null) {
      WebDavServerDescriptor server = new WebDavServerDescriptor(
          Strings.isNullOrEmpty(currentUri.hostName) ? currentUri.hostUrl : currentUri.hostName, currentUri.buildRootUrl(), "");
      myServers.addValue(server);
      myServers.setValue(server);
    } else if (!savedServer.equals(myServers.getValue())) {
      myServers.setValue(savedServer);
    }
    myPath.setValue(currentUri.path);
    return true;
  }

  private WebDavServerDescriptor findSavedServer(String domainUrl) {
    for (WebDavServerDescriptor server : myServers.getValues()) {
      if (server.getRootUrl().equals(domainUrl)) {
        return server;
      }
    }
    return null;
  }

  public JComponent createOpenDocumentUi(OkAction openAction) {
    myOkAction = openAction;
    return createComponent();
  }

  public JComponent createSaveDocumentUi(OkAction saveAction) {
    myOkAction = saveAction;
    return createComponent();
  }

  private WebDavUri buildUrl() {
    WebDavServerDescriptor server= myServers.getValue();
    if (server == null) {
      return null;
    }
    String host = server.getRootUrl().trim();
    while (host.endsWith("/")) {
      host = host.substring(0, host.length() - 1);
    }
    String path = Objects.firstNonNull(myPath.getValue(), "");
    if (!path.startsWith("/")) {
      path = "/" + path;
    }
    return new WebDavUri(server.name, host, path);
  }

  private WebDavResource getSelectedResource() {
    return (WebDavResource) table.getSelectedValue();
  }

  private JComponent createComponent() {
    OptionsPageBuilder builder = new OptionsPageBuilder();

    JPanel panel = new JPanel(new SpringLayout());
    panel.add(new JLabel(language.getCorrectedLabel("webServer")));
    EnumerationOption serverChoiceOption = myServers.asEnumerationOption();
    panel.add(builder.createOptionComponent(null, serverChoiceOption));
    panel.add(new JLabel());
    panel.add(createUsernamePasswordPanel());

    panel.add(new JLabel(language.getCorrectedLabel("fileFromServer")));
    panel.add(builder.createOptionComponent(null, myPath));
    myPath.addChangeValueListener(new ChangeValueListener() {
      @Override
      public void changeValue(ChangeValueEvent event) {
        String value = (String) event.getNewValue();
        if (value == null) {
          return;
        }
        String lcValue = value.toLowerCase();
        if (lcValue.startsWith("http://") || lcValue.startsWith("https://")) {
          if (!tryApplyUrl(new WebDavUri(value))) {
            myUpAction.setEnabled(value.split("/").length > 1);
          }
        }
      }
    });

    addEmptyRow(panel);

    {
      JPanel filesTablePanel = new JPanel(new BorderLayout());
      JButton refreshButton = new JButton(myReloadAction);
      JButton upButton = new JButton(myUpAction);
      myLockButton = new JButton(myLockAction);

      panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(myUpAction.getKeyStroke(), myUpAction);
      panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(myReloadAction.getKeyStroke(), myReloadAction);
      panel.getActionMap().put(myUpAction, myUpAction);
      panel.getActionMap().put(myReloadAction, myReloadAction);
      upButton.setText("");
      refreshButton.setText("");
      Box filesHeaderBox = Box.createVerticalBox();
      filesHeaderBox.add(upButton);
      filesHeaderBox.add(Box.createVerticalStrut(5));
      filesHeaderBox.add(refreshButton);
      filesHeaderBox.add(Box.createVerticalGlue());
      filesHeaderBox.add(new JButton(myDeleteAction));
      filesHeaderBox.add(Box.createVerticalStrut(5));
      filesHeaderBox.add(myLockButton);

      filesTablePanel.add(filesHeaderBox, BorderLayout.EAST);
      filesHeaderBox.setBorder(BorderFactory.createEmptyBorder(0, 3, 0, 0));
      table = new JXList(tableModel);
      table.setHighlighters(UIUtil.ZEBRA_HIGHLIGHTER);
      table.setCellRenderer(new FilesCellRenderer());
      table.addListSelectionListener(new ListSelectionListener() {
        @Override
        public void valueChanged(ListSelectionEvent e) {
          WebDavResource resource = getSelectedResource();
          if (resource == null) {
            return;
          }
          onSelectionChanged(resource);
        }
      });
      table.addMouseListener(new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent e) {
          if (e.getClickCount() == 2) {
            tryEnterCollection(table, tableModel);
          }
        }
      });
      table.addKeyListener(new KeyAdapter() {
        @Override
        public void keyReleased(KeyEvent e) {
          if (e.getKeyCode() == KeyEvent.VK_ENTER) {
            tryEnterCollection(table, tableModel);
          }
        }
      });

      myFilesComponent = new JScrollPane(table);
      myFilesComponent.setMaximumSize(table.getPreferredScrollableViewportSize());

      filesTablePanel.add(myFilesComponent, BorderLayout.CENTER);

      panel.add(new JLabel(language.getText("fileChooser.fileList")));
      panel.add(filesTablePanel);
    }

    addEmptyRow(panel);
    SpringUtilities.makeCompactGrid(panel, 6, 2, 0, 0, 10, 5);
    panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, panel.getPreferredSize().height));

    Box properties = Box.createVerticalBox();
    properties.add(panel);
    properties.add(Box.createVerticalGlue());

    myBusyComponent = new JBusyComponent<JComponent>(properties);
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        if (myInitialUri != null) {
          tryApplyUrl(myInitialUri);
        }
        reloadFilesTable();
      }
    });
    return myBusyComponent;
  }

  private void setProgressBar(boolean b) {
    myBusyComponent.setBusy(b);
  }

  private Component createUsernamePasswordPanel() {
    JPanel grid = new JPanel(new GridLayout(3, 2));
    grid.add(new JLabel(language.getText("userName")));
    final JLabel username = new JLabel(myUsername.getValue());
    myUsername.addChangeValueListener(new ChangeValueListener() {
      @Override
      public void changeValue(ChangeValueEvent event) {
        username.setText(myUsername.getValue() == null ? "" : myUsername.getValue());
      }
    });
    grid.add(username);
    grid.add(new JLabel(language.getText("password")));
    myPasswordLabel = new JLabel(myPassword.getValue() == null ? "" : Strings.repeat("*", myPassword.getValue().length()));
    myPassword.addChangeValueListener(new ChangeValueListener() {
      @Override
      public void changeValue(ChangeValueEvent event) {
        UIUtil.clearErrorLabel(myPasswordLabel);
        myPasswordLabel.setText(
            myPassword.getValue() == null ? "" : Strings.repeat("*", myPassword.getValue().length()));
      }
    });
    grid.add(myPasswordLabel);
    grid.add(new JXHyperlink(new GPAction("webdav.configure") {
      @Override
      public void actionPerformed(ActionEvent arg0) {
        WebDavOptionPageProvider optionPage = new WebDavOptionPageProvider();
        optionPage.init(myProject, myUiFacade);
        myUiFacade.createDialog(optionPage.buildPageComponent(), new Action[] {CancelAction.CLOSE}, "").show();
        updateUsernameAndPassword();
        myReloadAction.actionPerformed(null);
      }
    }));
    UIUtil.walkComponentTree(grid, new Predicate<JComponent>() {
      @Override
      public boolean apply(JComponent input) {
        input.setFont(input.getFont().deriveFont(input.getFont().getSize()*0.82f));
        return true;
      }
    });

    Box result = Box.createHorizontalBox();
    result.add(grid);
    result.add(Box.createHorizontalGlue());
    return grid;
  }

  private void updateUsernameAndPassword() {
    WebDavServerDescriptor server = myServers.getValue();
    myUsername.setValue(server == null ? "" : server.username);
    myPassword.setValue(server == null ? "" : server.password);
    setWebDavActionsEnabled(
        !Strings.isNullOrEmpty(myUsername.getValue()) && !Strings.isNullOrEmpty(myPassword.getValue()));
  }

 private void setWebDavActionsEnabled(boolean value) {
   myDeleteAction.setEnabled(value);
   myLockAction.setEnabled(value);
   myReloadAction.setEnabled(value);
   myUpAction.setEnabled(value);
   myOkAction.setEnabled(value);
 }

  protected void onSelectionChanged(WebDavResource resource) {
    if (mySelectionListener != null) {
      mySelectionListener.setSelection(resource);
    }
    myPath.setValue(resource.getWebDavUri().path);
    try {
      if (resource.isLocked()) {
        myLockButton.setAction(myUnlockAction);
      } else {
        myLockButton.setAction(myLockAction);
      }
    } catch (WebDavException e) {
      GPLogger.logToLogger(e);
    }
  }

  protected void tryEnterCollection(JXList table, FilesTableModel tableModel) {
    WebDavResource resource = (WebDavResource) table.getSelectedValue();
    try {
      if (resource.isCollection()) {
        new ReloadWorker(resource).execute();
      }
    } catch (WebDavException e) {
      showError(e);
    }
  }

  private static void addEmptyRow(JPanel form) {
    form.add(Box.createRigidArea(new Dimension(1, 10)));
    form.add(Box.createRigidArea(new Dimension(1, 10)));
  }

  WebDavUri getUrl() {
    return buildUrl();
  }

  String getUsername() {
    return myUsername.getValue();
  }

  String getPassword() {
    return myPassword.getValue();
  }

  int getLockTimeout() {
    return myTimeout.getValue();
  }

  void setSelectionListener(SelectionListener selectionListener) {
    mySelectionListener = selectionListener;
  }

  StringOption getPathOption() {
    return myPath;
  }

  void showError(Exception e) {
    if (e.getCause() instanceof NotAuthorizedException) {
      UIUtil.setupErrorLabel(myPasswordLabel, "Access denied");
      setWebDavActionsEnabled(false);
    }
    GPLogger.log(e);
  }

  public void dispose() {
    for (Runnable remover : myListenerRemovers) {
      remover.run();
    }
  }
}
