/*
 * Created on 17.07.2003
 *
 */
package net.sourceforge.ganttproject.resource;

import java.util.EventObject;

/**
 * @author bard
 */
public class ResourceEvent extends EventObject {
  /**
   * @param source
   */
  public ResourceEvent(HumanResourceManager mgr, HumanResource resource) {
    super(mgr);
    myManager = mgr;
    myResource = resource;
    myResources = new HumanResource[] { myResource };
  }

  public ResourceEvent(HumanResourceManager mgr, HumanResource[] resources) {
    super(mgr);
    myManager = mgr;
    myResources = resources;
    myResource = resources.length > 0 ? resources[0] : null;
  }

  public HumanResourceManager getManager() {
    return myManager;
  }

  public HumanResource getResource() {
    return myResource;
  }

  public HumanResource[] getResources() {
    return myResources;
  }

  private HumanResource[] myResources;

  private HumanResourceManager myManager;

  private HumanResource myResource;

}
