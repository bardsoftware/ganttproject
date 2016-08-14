// Copyright (C) 2016 BarD Software
package biz.ganttproject.storage.webdav;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import net.sourceforge.ganttproject.document.webdav.WebDavResource;

import java.util.List;

/**
 * @author dbarashev@bardsoftware.com
 */
public class WebdavLoadTask extends Task<ObservableList<WebDavResource>> {
  private final WebdavLoadWorker myWorker;

  WebdavLoadTask(WebDavResource root) {
    myWorker = new WebdavLoadWorker(root);
  }

  @Override
  protected ObservableList<WebDavResource> call() throws Exception {
    ObservableList<WebDavResource> result = FXCollections.observableArrayList();
    updateMessage("Connecting to the server");
    List<WebDavResource> resources = myWorker.load().second();
    if (isCancelled()) {
      updateMessage("Cancelled");
      return null;
    }
    result.setAll(resources);
    return result;
  }
}
