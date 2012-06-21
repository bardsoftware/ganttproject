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

import javax.swing.JComponent;

import net.sourceforge.ganttproject.document.Document;
import net.sourceforge.ganttproject.document.DocumentStorageUi;
import net.sourceforge.ganttproject.gui.options.model.ChangeValueEvent;
import net.sourceforge.ganttproject.gui.options.model.ChangeValueListener;
import net.sourceforge.ganttproject.gui.options.model.DefaultIntegerOption;
import net.sourceforge.ganttproject.gui.options.model.DefaultStringOption;
import net.sourceforge.ganttproject.gui.options.model.IntegerOption;
import net.sourceforge.ganttproject.gui.options.model.StringOption;

/**
 * Implements storage UI for WebDAV storages
 *
 * @author dbarashev (Dmitry Barashev)
 */
public class WebDavStorageImpl implements DocumentStorageUi {

  private final StringOption myLastWebDAVDocument = new DefaultStringOption("last-webdav-document", "");
  private final IntegerOption myWebDavLockTimeoutOption = new DefaultIntegerOption("webdav.lockTimeout", -1);

  public WebDavStorageImpl() {
    myWebDavLockTimeoutOption.addChangeValueListener(new ChangeValueListener() {
      @Override
      public void changeValue(ChangeValueEvent event) {
        HttpDocument.setLockDAVMinutes(myWebDavLockTimeoutOption.getValue());
      }
    });
  }

  @Override
  public JComponent open(Document currentDocument, DocumentReceiver receiver) {
    GanttURLChooser chooser = createChooser(currentDocument, receiver);
    return chooser.createOpenDocumentUi();

  }
  @Override
  public JComponent save(Document currentDocument, DocumentReceiver receiver) {
    GanttURLChooser chooser = createChooser(currentDocument, receiver);
    return chooser.createSaveDocumentUi();
  }

  private GanttURLChooser createChooser(Document currentDocument, DocumentReceiver receiver) {
    String lastDocUrl = (currentDocument == null) ? getLastWebDAVDocumentOption().getValue() : currentDocument.getURI().toString();
    String username = currentDocument == null ? null : currentDocument.getUsername();
    String password = currentDocument == null ? null : currentDocument.getPassword();
    return new GanttURLChooser(lastDocUrl, username, password, getWebDavLockTimeoutOption(), receiver);
  }

  public StringOption getLastWebDAVDocumentOption() {
    return myLastWebDAVDocument;
  }

  public IntegerOption getWebDavLockTimeoutOption() {
    return myWebDavLockTimeoutOption;
  }
}
