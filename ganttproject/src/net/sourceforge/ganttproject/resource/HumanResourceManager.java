/*
GanttProject is an opensource project management tool.
Copyright (C) 2003-2010 Alexandre Thomas, Michael Barmeier, Dmitry Barashev

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
*/
package net.sourceforge.ganttproject.resource;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.sourceforge.ganttproject.CustomPropertyDefinition;
import net.sourceforge.ganttproject.CustomPropertyManager;
import net.sourceforge.ganttproject.GanttCalendar;
import net.sourceforge.ganttproject.roles.Role;
import net.sourceforge.ganttproject.undo.GPUndoManager;

/**
 * @author barmeier
 */
public class HumanResourceManager implements ResourceManager, CustomPropertyManager {

    private List<ResourceView> myViews = new ArrayList<ResourceView>();

    private List<ProjectResource> resources = new ArrayList<ProjectResource>();

    private int nextFreeId = 0;

    private final Role myDefaultRole;

    /* customFields maintains a list of custom field names
     * and their default values */
    private final Map<String, CustomPropertyDefinition> customFields = new HashMap<String, CustomPropertyDefinition>();

    public HumanResourceManager(Role defaultRole) {
        myDefaultRole = defaultRole;
    }

    public HumanResource newHumanResource() {
        HumanResource result = new HumanResource(this);
        result.setRole(myDefaultRole);
        return result;
    }

    public ProjectResource create(String name, int i) {
        HumanResource hr = new HumanResource(name, i, this);
        hr.setRole(myDefaultRole);
        add(hr);
        return (hr);
    }

    public void add(ProjectResource resource) {
        if (resource.getId() == -1) {
            resource.setId(nextFreeId);
        }
        if (resource.getId() >= nextFreeId) {
            nextFreeId = resource.getId() + 1;
        }
        resources.add(resource);
        fireResourceAdded(resource);
    }

    public void addCustomField(CustomPropertyDefinition definition) {
        customFields.put(definition.getName(), definition);

        /* all the existent resources are added the new property field */
        Iterator<ProjectResource> it = resources.iterator();
        while (it.hasNext()) {
            ((HumanResource) it.next()).addCustomField(definition.getName(), definition.getDefaultValue());
        }
    }

    public boolean checkCustomField(String title){
        return customFields.containsKey(title);
    }

    public void removeCustomField(String title) {
        customFields.remove(title);

        /* the property field is removed from all the existent resources */
        Iterator<ProjectResource> it = resources.iterator();
        while (it.hasNext()) {
            ((HumanResource)it.next()).removeCustomField(title);
        }
    }

    public ProjectResource getById(int id) {
        // Linear search is not really efficient, but we don't have so many
        // resources !?
        ProjectResource pr = null;
        for (int i = 0; i < resources.size(); i++)
            if (resources.get(i).getId() == id) {
                pr = resources.get(i);
                break;
            }
        return pr;
    }

    public List<ProjectResource> getResources() {
        return resources;
    }

    public ProjectResource[] getResourcesArray() {
        return (ProjectResource[]) resources.toArray(new ProjectResource[resources.size()]);
    }

    public void remove(ProjectResource resource) {
        fireResourcesRemoved(new ProjectResource[] { resource });
        resources.remove(resource);
    }

    public void remove(ProjectResource resource, GPUndoManager myUndoManager) {
        final ProjectResource res = resource;
        myUndoManager.undoableEdit("Delete Human OK", new Runnable() {
            public void run() {
                fireResourcesRemoved(new ProjectResource[] { res });
                resources.remove(res);
            }
        });
    }

    public void save(OutputStream target) {
    }

    public void clear() {
        fireCleanup();
        resources.clear();
    }

    public void addView(ResourceView view) {
        myViews.add(view);
    }

    private void fireResourceAdded(ProjectResource resource) {
        ResourceEvent e = new ResourceEvent(this, resource);
        for (Iterator<ResourceView> i = myViews.iterator(); i.hasNext();) {
            ResourceView nextView = i.next();
            nextView.resourceAdded(e);
        }
    }

    void fireResourceChanged(ProjectResource resource) {
        ResourceEvent e = new ResourceEvent(this, resource);
        for (Iterator<ResourceView> i = myViews.iterator(); i.hasNext();) {
            ResourceView nextView = i.next();
            nextView.resourceChanged(e);
        }
    }

    private void fireResourcesRemoved(ProjectResource[] resources) {
        ResourceEvent e = new ResourceEvent(this, resources);
        for (int i = 0; i < myViews.size(); i++) {
            ResourceView nextView = myViews.get(i);
            nextView.resourcesRemoved(e);
        }
    }

    public void fireAssignmentsChanged(ProjectResource resource) {
        ResourceEvent e = new ResourceEvent(this, resource);
        for (Iterator<ResourceView> i = myViews.iterator(); i.hasNext();) {
            ResourceView nextView = i.next();
            nextView.resourceAssignmentsChanged(e);
        }
    }

    private void fireCleanup() {
        fireResourcesRemoved((ProjectResource[]) resources
                .toArray(new ProjectResource[resources.size()]));
    }

    /** Move up the resource number index */
    public void up(HumanResource hr) {
        int index =  resources.indexOf(hr);
        assert index>=0;
        resources.remove(index);
        resources.add(index - 1, hr);
        fireResourceChanged(hr);
    }

    /** Move down the resource number index */
    public void down(HumanResource hr) {
        int index =  resources.indexOf(hr);
        assert index>=0;
        resources.remove(index);
        resources.add(index + 1, hr);
        fireResourceChanged(hr);

    }

    public Map<HumanResource, HumanResource> importData(HumanResourceManager hrManager, HumanResourceMerger merger) {
        Map<HumanResource, HumanResource> foreign2native = new HashMap<HumanResource, HumanResource>();
        List<ProjectResource> foreignResources = hrManager.getResources();
        for (int i = 0; i < foreignResources.size(); i++) {
            HumanResource foreignHR = (HumanResource) foreignResources.get(i);
            HumanResource nativeHR = (HumanResource) getById(foreignHR.getId());
            if (nativeHR == null) {
                nativeHR = (HumanResource) create(foreignHR.getName(), nextFreeId);
            }
            foreign2native.put(foreignHR, nativeHR);
        }
        merger.merge(foreign2native);
        return foreign2native;
    }

    public CustomPropertyManager getCustomPropertyManager() {
        return this;
    }

    public List<CustomPropertyDefinition> getDefinitions() {
        List<CustomPropertyDefinition> result = new ArrayList<CustomPropertyDefinition>(customFields.values());
        return result;
    }

    CustomPropertyDefinition getCustomPropertyDefinition(String nextName) {
        return (CustomPropertyDefinition) customFields.get(nextName);
    }

    static String getValueAsString(Object value) {
        final String result;
        if (value!=null) {
            if (value instanceof GanttCalendar) {
                result = ((GanttCalendar)value).toXMLString();
            }
            else {
                result = String.valueOf(value);
            }
        }
        else {
            result = null;
        }
        return result;
    }

    public CustomPropertyDefinition createDefinition(String id, String typeAsString, String name, String defaultValueAsString) {
        final CustomPropertyDefinition stubDefinition = CustomPropertyManager.PropertyTypeEncoder.decodeTypeAndDefaultValue(typeAsString, defaultValueAsString);
        CustomPropertyDefinition result = new CustomPropertyDefinitionImpl(name, id, stubDefinition);
        addCustomField(result);
        return result;
    }

    public void importData(CustomPropertyManager source) {
        // TODO Auto-generated method stub

    }

    static class CustomPropertyDefinitionImpl implements CustomPropertyDefinition {
        private final String myName;
        private final String myID;
        private final Object myDefaultValue;
        private final String myDefaultValueAsString;
        private final Class myType;
        private final String myTypeAsString;

        CustomPropertyDefinitionImpl(String name, String id, CustomPropertyDefinition stub) {
            myName = name;
            myID = id;
            myDefaultValue = stub.getDefaultValue();
            myDefaultValueAsString = stub.getDefaultValueAsString();
            myType = stub.getType();
            myTypeAsString = stub.getTypeAsString();
        }
        public Object getDefaultValue() {
            return myDefaultValue;
        }

        public String getDefaultValueAsString() {
            return myDefaultValueAsString;
        }

        public String getID() {
            return myID;
        }

        public String getName() {
            return myName;
        }

        public Class getType() {
            return myType;
        }

        public String getTypeAsString() {
            return myTypeAsString;
        }

    }
}
