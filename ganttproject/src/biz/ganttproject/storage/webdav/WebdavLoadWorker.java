// Copyright (C) 2016 BarD Software
package biz.ganttproject.storage.webdav;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.document.webdav.WebDavResource;
import net.sourceforge.ganttproject.util.collect.Pair;

import java.util.List;

/**
 * @author dbarashev@bardsoftware.com
 */
public class WebdavLoadWorker {
  private final WebDavResource myRoot;

  public WebdavLoadWorker(WebDavResource root) {
    myRoot = Preconditions.checkNotNull(root);
  }

  public Pair<WebDavResource, List<WebDavResource>> load() throws Exception {
    WebDavResource resource = myRoot;
    if (resource.exists() && resource.isCollection()) {
      return Pair.create(resource, readChildren(resource));
    }
    WebDavResource parent = resource.getParent();
    if (parent.exists() && parent.isCollection()) {
      return Pair.create(parent, readChildren(parent));
    }
    return null;
  }

  private List<WebDavResource> readChildren(WebDavResource parent) throws WebDavResource.WebDavException {
    List<WebDavResource> children = Lists.newArrayList();
    for (WebDavResource child : parent.getChildResources()) {
      try {
        if (child.exists()) {
          children.add(child);
        }
      } catch (WebDavResource.WebDavException e) {
        GPLogger.logToLogger(e);
      }
    }
    return children;
  }
}
