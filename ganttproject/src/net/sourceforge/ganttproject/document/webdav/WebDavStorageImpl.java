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

import java.awt.event.ActionEvent;
import java.io.IOException;

import javax.swing.Action;
import javax.swing.JComponent;

import net.sourceforge.ganttproject.action.CancelAction;
import net.sourceforge.ganttproject.action.OkAction;
import net.sourceforge.ganttproject.document.Document;
import net.sourceforge.ganttproject.document.DocumentStorageUi;
import net.sourceforge.ganttproject.document.webdav.WebDavResource.WebDavException;
import net.sourceforge.ganttproject.gui.options.model.ChangeValueEvent;
import net.sourceforge.ganttproject.gui.options.model.ChangeValueListener;
import net.sourceforge.ganttproject.gui.options.model.DefaultIntegerOption;
import net.sourceforge.ganttproject.gui.options.model.DefaultStringListOption;
import net.sourceforge.ganttproject.gui.options.model.DefaultStringOption;
import net.sourceforge.ganttproject.gui.options.model.IntegerOption;
import net.sourceforge.ganttproject.gui.options.model.ListOption;
import net.sourceforge.ganttproject.gui.options.model.StringOption;

/**
 * Implements storage UI for WebDAV storages
 *
 * @author dbarashev (Dmitry Barashev)
 */
public class WebDavStorageImpl implements DocumentStorageUi {
  private final ListOption myServers = new DefaultStringListOption("servers");
  private final StringOption myLastWebDAVDocument = new DefaultStringOption("last-webdav-document", "");
  private final IntegerOption myWebDavLockTimeoutOption = new DefaultIntegerOption("webdav.lockTimeout", -1);
  private final StringOption myUsername = new DefaultStringOption("username", "");
  private final MiltonResourceFactory myWebDavFactory = new MiltonResourceFactory();

  public WebDavStorageImpl() {
  }

  @Override
  public Components open(Document currentDocument, final DocumentReceiver receiver) {
    final GanttURLChooser chooser = createChooser(currentDocument);
    final OkAction openAction = createNoLockAction("storage.action.open", chooser, receiver);
    final OkAction openAndLockAction = createLockAction("storage.action.openAndLock", chooser, receiver);
    chooser.setSelectionListener(new GanttURLChooser.SelectionListener() {
      @Override
      public void setSelection(WebDavResource resource) {
        if (resource == null) {
          return;
        }
        try {
          openAndLockAction.setEnabled(resource.canLock());
        } catch (WebDavException e) {
          chooser.showError(e);
        }
      }
    });
    JComponent contentPane = chooser.createOpenDocumentUi();
    chooser.getPathOption().addChangeValueListener(new ChangeValueListener() {
      @Override
      public void changeValue(ChangeValueEvent event) {
        boolean empty = "".equals(event.getNewValue());
        openAction.setEnabled(!empty);
        openAndLockAction.setEnabled(!empty);
      }
    });
    return new Components(contentPane, new Action[] {openAction, openAndLockAction, new CancelAction() {
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
    final OkAction saveAndLockAction = createLockAction("storage.action.saveAndLock", chooser, receiver);
    chooser.setSelectionListener(new GanttURLChooser.SelectionListener() {
      @Override
      public void setSelection(WebDavResource resource) {
        try {
          saveAndLockAction.setEnabled(resource.canLock());
        } catch (WebDavException e) {
          chooser.showError(e);
        }
      }
    });
    JComponent contentPane = chooser.createSaveDocumentUi();
    return new Components(contentPane, new Action[] {saveAction, saveAndLockAction, new CancelAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        receiver.setDocument(null);
      }
    }});
  }

  private GanttURLChooser createChooser(Document currentDocument) {
    String lastDocUrl = (currentDocument == null || !currentDocument.getURI().getScheme().toLowerCase().startsWith("http"))
        ? getLastWebDAVDocumentOption().getValue() : currentDocument.getURI().toString();
    if (currentDocument != null) {
      myUsername.setValue(currentDocument.getUsername());
    }
    String password = currentDocument == null ? null : currentDocument.getPassword();
    myWebDavFactory.clearCache();
    return new GanttURLChooser(myServers, lastDocUrl, myUsername, password, getWebDavLockTimeoutOption(), myWebDavFactory);
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
          receiver.setDocument(new HttpDocument(myWebDavFactory.createResource(chooser.getUrl()), chooser.getUsername(), chooser.getPassword(), HttpDocument.NO_LOCK));
        } catch (IOException e) {
          chooser.showError(e);
        }
      }
    };
  }

  private OkAction createLockAction(String key, final GanttURLChooser chooser, final DocumentReceiver receiver) {
    return new OkAction(key) {
      {
        setDefault(false);
      }
      @Override
      public void actionPerformed(ActionEvent event) {
        try {
          myWebDavFactory.setCredentials(chooser.getUsername(), chooser.getPassword());
          receiver.setDocument(new HttpDocument(myWebDavFactory.createResource(chooser.getUrl()), chooser.getUsername(), chooser.getPassword(), chooser.getLockTimeout()));
        } catch (IOException e) {
          chooser.showError(e);
        }
      }
    };
  }

  public ListOption getServersOption() {
    return myServers;
  }

  public StringOption getWebDavUsernameOption() {
    return myUsername;
  }

  public StringOption getLastWebDAVDocumentOption() {
    return myLastWebDAVDocument;
  }

  public IntegerOption getWebDavLockTimeoutOption() {
    return myWebDavLockTimeoutOption;
  }
}
