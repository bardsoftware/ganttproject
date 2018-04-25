/*
GanttProject is an opensource project management tool. License: GPL3
Copyright (C) 2012 GanttProject Team

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

import biz.ganttproject.core.option.BooleanOption;
import biz.ganttproject.core.option.ChangeValueEvent;
import biz.ganttproject.core.option.ChangeValueListener;
import biz.ganttproject.core.option.DefaultBooleanOption;
import biz.ganttproject.core.option.DefaultEnumerationOption;
import biz.ganttproject.core.option.DefaultIntegerOption;
import biz.ganttproject.core.option.DefaultStringOption;
import biz.ganttproject.core.option.EnumerationOption;
import biz.ganttproject.core.option.GPAbstractOption;
import biz.ganttproject.core.option.IntegerOption;
import biz.ganttproject.core.option.ListOption;
import biz.ganttproject.core.option.StringOption;
import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.ProjectEventListener;
import net.sourceforge.ganttproject.action.CancelAction;
import net.sourceforge.ganttproject.action.OkAction;
import net.sourceforge.ganttproject.document.Document;
import net.sourceforge.ganttproject.document.DocumentStorageUi;
import net.sourceforge.ganttproject.gui.UIFacade;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

/**
 * Implements storage UI for WebDAV storages
 *
 * @author dbarashev (Dmitry Barashev)
 */
public class WebDavStorageImpl implements DocumentStorageUi {
  static class ServerListOption extends GPAbstractOption<WebDavServerDescriptor> implements ListOption<WebDavServerDescriptor> {
    private List<WebDavServerDescriptor> myServers = Lists.newArrayList();

    class EnumerationOptionImpl extends DefaultEnumerationOption<WebDavServerDescriptor> {
      EnumerationOptionImpl(String id) {
        super(id, myServers.toArray(new WebDavServerDescriptor[0]));
      }
      @Override
      protected String objectToString(WebDavServerDescriptor obj) {
        return obj.name;
      }

      @Override
      public void setValue(String value) {
        super.setValue(value);
        ServerListOption.this.setValue(stringToObject(value));
      }

      void reload() {
        String curValue = getValue();
        int idxValue = Arrays.asList(getAvailableValues()).indexOf(curValue);
        super.reloadValues(myServers);
        if (idxValue >= 0) {
          setValueIndex(idxValue);
        }
      }
    }

    private EnumerationOptionImpl myEnumerationOption;

    ServerListOption(String id) {
      super(id);
      myEnumerationOption = new EnumerationOptionImpl(id);
    }

    @Override
    public String getPersistentValue() {
      StringBuilder result = new StringBuilder();
      for (WebDavServerDescriptor server : myServers) {
        result.append("\n").append(server.name).append("\t").append(server.getRootUrl()).append("\t").append(server.username);
        if (server.savePassword) {
          result.append("\t").append(server.password);
        }
      }
      return result.toString();
    }

    @Override
    public void loadPersistentValue(String value) {
      for (String s : value.split("\\n")) {
        if (!Strings.isNullOrEmpty(s)) {
          String[] parts = s.split("\\t");
          WebDavServerDescriptor server = new WebDavServerDescriptor();
          if (parts.length >= 1) {
            server.name = parts[0];
          }
          if (parts.length >= 2) {
            server.setRootUrl(parts[1]);
          }
          if (parts.length >= 3) {
            server.username = parts[2];
          }
          if (parts.length >= 4) {
            server.password = parts[3];
            server.savePassword = true;
          }
          if (server.getRootUrl() != null) {
            myServers.add(server);
          }
        }
      }
      myEnumerationOption.reload();
    }

    @Override
    public void setValues(Iterable<WebDavServerDescriptor> values) {
      myServers = Lists.newArrayList(values);
      myEnumerationOption.reload();
    }

    @Override
    public Iterable<WebDavServerDescriptor> getValues() {
      return myServers;
    }

    @Override
    public void setValueIndex(int idx) {
      super.setValue(myServers.get(idx));
    }

    @Override
    public void setValue(WebDavServerDescriptor value) {
      if (!Objects.equal(value, getValue())) {
        super.setValue(value);
        myEnumerationOption.setSelectedValue(value);
      }
    }

    @Override
    public void addValue(WebDavServerDescriptor value) {
      myServers.add(value);
      myEnumerationOption.reload();
    }

    @Override
    public void updateValue(WebDavServerDescriptor oldValue, WebDavServerDescriptor newValue) {
      int idxOldValue = myServers.indexOf(oldValue);
      assert idxOldValue >= 0 : "Failed to find value=" + oldValue + " in the list=" + myServers;
      myServers.set(idxOldValue, newValue);
      myEnumerationOption.reload();
    }

    @Override
    public void removeValueIndex(int idx) {
      myServers.remove(idx);
      myEnumerationOption.reload();
    }

    @Override
    public EnumerationOption asEnumerationOption() {
      return myEnumerationOption;
    }
  }
  private final ListOption<WebDavServerDescriptor> myServers = new ServerListOption("servers");
  private final StringOption myLegacyLastWebDAVDocument = new DefaultStringOption("last-webdav-document", "");
  private final StringOption myLastWebDavDocumentOption = new DefaultStringOption("lastDocument", null);
  private final IntegerOption myWebDavLockTimeoutOption = new DefaultIntegerOption("webdav.lockTimeout", -1);
  private final BooleanOption myReleaseLockOption = new DefaultBooleanOption("lockRelease", true);
  private final StringOption myUsername = new DefaultStringOption("username", "");
  private final StringOption myPassword = new DefaultStringOption("password", "");
  private final StringOption myProxy = new DefaultStringOption("proxy", "");
  private final MiltonResourceFactory myWebDavFactory = new MiltonResourceFactory();
  private final UIFacade myUiFacade;
  private final IGanttProject myProject;

  public WebDavStorageImpl(final IGanttProject project, UIFacade uiFacade) {
    myProject = project;
    myUiFacade = uiFacade;
    project.addProjectEventListener(new ProjectEventListener.Stub() {
      @Override
      public void projectClosed() {
        if (myReleaseLockOption.isChecked() && project.getDocument() != null) {
          project.getDocument().releaseLock();
        }
      }
    });
    myPassword.setScreened(true);
  }

  @Override
  public Components open(Document currentDocument, final DocumentReceiver receiver) {
    final GanttURLChooser chooser = createChooser(currentDocument);
    final OkAction openAction = createNoLockAction("storage.action.open", chooser, receiver);
    //final OkAction openAndLockAction = createLockAction("storage.action.openAndLock", chooser, receiver);
//    chooser.setSelectionListener(new GanttURLChooser.SelectionListener() {
//      @Override
//      public void setSelection(WebDavResource resource) {
//        if (resource == null) {
//          return;
//        }
//        try {
//          openAndLockAction.setEnabled(resource.canLock());
//        } catch (WebDavException e) {
//          chooser.showError(e);
//        }
//      }
//    });
    JComponent contentPane = chooser.createOpenDocumentUi(openAction);
    chooser.getPathOption().addChangeValueListener(new ChangeValueListener() {
      @Override
      public void changeValue(ChangeValueEvent event) {
        boolean empty = "".equals(event.getNewValue());
        openAction.setEnabled(!empty);
//        openAndLockAction.setEnabled(!empty);
      }
    });
    return new Components(contentPane, new Action[] {openAction, /*openAndLockAction,*/ new CancelAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        receiver.setDocument(null);
      }
    }});
  }

  @Override
  public Components save(Document currentDocument, final DocumentReceiver receiver) {
    final GanttURLChooser chooser = createChooser(currentDocument);
    OkAction saveAction = createNoLockAction("storage.action.save", chooser, receiver);
    JComponent contentPane = chooser.createSaveDocumentUi(saveAction);
    return new Components(contentPane, new Action[] {saveAction, new CancelAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        receiver.setDocument(null);
      }
    }});
  }

  private GanttURLChooser createChooser(Document currentDocument) {
    WebDavUri currentUri;
    if (currentDocument instanceof HttpDocument) {
      currentUri = ((HttpDocument)currentDocument).getWebdavResource().getWebDavUri();
      myUsername.setValue(currentDocument.getUsername());
      myPassword.setValue(currentDocument.getPassword());
    } else {
      String lastDocument = Objects.firstNonNull(
          getLastWebDavDocumentOption().getValue(), getLegacyLastWebDAVDocumentOption().getValue());
      if (lastDocument == null) {
        currentUri = null;
      } else {
        String[] savedComponents = lastDocument.split("\\t");
        if (savedComponents.length == 1) {
          currentUri = new WebDavUri(savedComponents[0]);
        } else {
          try {
            URL rootUrl = new URL(savedComponents[0]);
            currentUri = new WebDavUri(rootUrl.getHost(), savedComponents[0], savedComponents[1]);
          } catch (MalformedURLException e) {
            GPLogger.logToLogger(e);
            currentUri = null;
          }
        }
      }
    }
    myWebDavFactory.clearCache();
    return new GanttURLChooser(myProject, myUiFacade, myServers, currentUri, myUsername, myPassword, getWebDavLockTimeoutOption(), getWebDavReleaseLockOption(), myWebDavFactory);
  }

  private OkAction createNoLockAction(String key, final GanttURLChooser chooser, final DocumentReceiver receiver) {
    return new OkAction(key) {
      {
        setDefault(false);
      }
      @Override
      public void actionPerformed(ActionEvent event) {
        try {
          myWebDavFactory.setCredentials(chooser.getUsername(), chooser.getPassword());
          WebDavUri webDavUri = chooser.getUrl();
          if (webDavUri != null) {
            receiver.setDocument(new HttpDocument(
                myWebDavFactory.createResource(webDavUri), chooser.getUsername(), chooser.getPassword(), HttpDocument.NO_LOCK));
            myLastWebDavDocumentOption.setValue(webDavUri.buildRootUrl() + "\t" + webDavUri.path);
            myLegacyLastWebDAVDocument.setValue(webDavUri.buildUrl());
          }
          chooser.dispose();
        } catch (IOException e) {
          chooser.showError(e);
        }
      }
    };
  }

  public ListOption<WebDavServerDescriptor> getServersOption() {
    return myServers;
  }

  public StringOption getLegacyLastWebDAVDocumentOption() {
    return myLegacyLastWebDAVDocument;
  }

  public StringOption getLastWebDavDocumentOption() {
    return myLastWebDavDocumentOption;
  }

  public IntegerOption getWebDavLockTimeoutOption() {
    return myWebDavLockTimeoutOption;
  }

  public BooleanOption getWebDavReleaseLockOption() {
    return myReleaseLockOption;
  }

  public WebDavServerDescriptor findServer(String path) {
    WebDavUri uri = new WebDavUri(path);
    for (WebDavServerDescriptor server : myServers.getValues()) {
      if (server.getRootUrl().equals(uri.buildRootUrl())) {
        return server;
      }
    }
    return null;
  }

  public StringOption getProxyOption () {
    return myProxy;
  }
}
