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

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import javax.swing.JComponent;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpURL;
import org.apache.commons.httpclient.HttpsURL;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.webdav.lib.WebdavResource;

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
  private final StringOption myUsername = new DefaultStringOption("username", "");

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
    if (currentDocument != null) {
      myUsername.setValue(currentDocument.getUsername());
    }
    String password = currentDocument == null ? null : currentDocument.getPassword();
    return new GanttURLChooser(lastDocUrl, myUsername, password, getWebDavLockTimeoutOption(), receiver);
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

  static WebdavResource createResource(String url, String username, String password) throws IOException {
    HttpURL httpUrl;
    try {
      if (url.startsWith("https")) {
        httpUrl = new HttpsURL(url);
      } else {
        httpUrl = new HttpURL(url);
      }
      httpUrl.setUserinfo(username, password);
    } catch (URIException e) {
      throw new IOException("Failed to parse url=" + url, e);
    }
    try {
      Credentials credentials = new UsernamePasswordCredentials(username, password);
      WebdavResource result = new WebdavResource(httpUrl, credentials, WebdavResource.NOACTION, 0);
      result.setFollowRedirects(true);
      return result;
    } catch (HttpException e) {
      throw new IOException(MessageFormat.format("Failed to access resource {0} \nError code: {1} ({2})", url, e.getReasonCode(), e.getReason()), e);
    } catch (IOException e) {
      throw new IOException(MessageFormat.format("Failed to access resource {0} \n{1}", url, e.getMessage()), e);
    }
  }

  static List<String> getLockOwners(WebdavResource resource) {
    Enumeration ownersEnum = resource.getActiveLockOwners();
    return ownersEnum == null ? Collections.emptyList() : Collections.list(resource.getActiveLockOwners());
  }
}
