// Copyright (C) 2016 BarD Software
package biz.ganttproject.storage.webdav;

import com.google.common.base.Preconditions;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import net.sourceforge.ganttproject.document.webdav.MiltonResourceFactory;
import net.sourceforge.ganttproject.document.webdav.WebDavResource;
import net.sourceforge.ganttproject.document.webdav.WebDavServerDescriptor;
import net.sourceforge.ganttproject.document.webdav.WebDavUri;

/**
 * @author dbarashev@bardsoftware.com
 */
public class WebdavLoadService extends Service<ObservableList<WebDavResource>> {
  private final MiltonResourceFactory myResourceFactory = new MiltonResourceFactory();
  private final WebDavServerDescriptor myServer;
  private String myPath;

  WebdavLoadService(WebDavServerDescriptor server) {
    myServer = Preconditions.checkNotNull(server);
  }

  void setPath(String path) {
    if (!path.startsWith("/")) {
      path = "/" + path;
    }
    myPath = path;
  }

  @Override
  protected Task<ObservableList<WebDavResource>> createTask() {
    return new WebdavLoadTask(createRootResource());
  }

  public WebDavResource createResource(WebDavResource parent, String name) {
    myResourceFactory.setCredentials(myServer.getUsername(), myServer.getPassword());
    return myResourceFactory.createResource(parent.getWebDavUri().buildChild(name));
  }

  public WebDavResource createRootResource() {
    myResourceFactory.setCredentials(myServer.getUsername(), myServer.getPassword());
    return myResourceFactory.createResource(buildUrl());
  }

  private WebDavUri buildUrl() {
    String host = myServer.getRootUrl().trim();
    while (host.endsWith("/")) {
      host = host.substring(0, host.length() - 1);
    }
    return new WebDavUri(myServer.name, host, myPath);
  }

}
