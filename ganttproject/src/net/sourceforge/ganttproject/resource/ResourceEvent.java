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
    public ResourceEvent(ResourceManager mgr, ProjectResource resource) {
        super(mgr);
        myManager = mgr;
        myResource = resource;
        myResources = new ProjectResource[] { myResource };
    }

    public ResourceEvent(ResourceManager mgr, ProjectResource[] resources) {
        super(mgr);
        myManager = mgr;
        myResources = resources;
        myResource = resources.length > 0 ? resources[0] : null;
    }

    public ResourceManager getManager() {
        return myManager;
    }

    public ProjectResource getResource() {
        return myResource;
    }

    public ProjectResource[] getResources() {
        return myResources;
    }

    private ProjectResource[] myResources;

    private ResourceManager myManager;

    private ProjectResource myResource;

}
