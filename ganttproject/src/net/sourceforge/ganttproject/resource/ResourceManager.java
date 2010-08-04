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
import java.util.List;
import net.sourceforge.ganttproject.CustomPropertyManager;
import net.sourceforge.ganttproject.undo.GPUndoManager;

/**
 * This interface is used to isolate the implementation of a resource manager
 * from the application. The interface is defined against an abstract class the
 * ProjectResource class. Normally only one instance of the Resourcemanager
 * should be instantiated.
 *
 * @author barmeier
 */
public interface ResourceManager {
    public ProjectResource create(String name, int i);

    /**
     * Adds the resource to the internal list of available resources.
     *
     * @param resource
     *            The resource that should be added to the list.
     */
    public void add(ProjectResource resource);

    /**
     * Retrieves an ancestor of ProjectResource identified by an identity value.
     *
     * @param id
     *            The id is an integer value that is unique for every resource.
     * @return Ancestor of ProjectResource containing the requested resource.
     * @see ProjectResource
     */
    public ProjectResource getById(int id);

    /**
     * Removes the resource.
     *
     * @param resource
     *            The resource to remove.
     */
    public void remove(ProjectResource resource);

    /**
     * Removes the resource by its id.
     *
     * @param Id
     *            Id of the resource to remove.
     */

    public void remove(ProjectResource resource, GPUndoManager myUndoManager);

    //public void removeById(int Id);

    /**
     * Retrieves a list of all resources available.
     *
     * @return ArrayList filled with ProjectResource ancestors.
     * @see ProjectResource
     */
    public List getResources();

    public ProjectResource[] getResourcesArray();

    /**
     * Loads resources from the InputStreamReader. All resources already stored
     * in the Resourcemanager are lost and will be replace with the resources
     * loaded from the stream.
     *
     * @return The ArrayLisr returned contains all ProjectResource ancestor that
     *         were read from the InputStreamReader.
     * @param source
     *            The InputStreamReader from which the data will be read. The
     *            format and kind of data read is subject of the class
     *            implementing this interface.
     */
    // public ArrayList load (InputStream source);
    /**
     * Writes all resources stored in the OutputStreamWriter. The format and
     * kind of data written in the stream are subject of the class that
     * implements this interface.
     *
     * @param target
     *            Stream to write the data to.
     */
    public void save(OutputStream target);

    /** Removes all resources from the manager. */
    public void clear();

    /**
     * Adds a new view of this manager
     *
     * @param view
     */
    public void addView(ResourceView view);

    public CustomPropertyManager getCustomPropertyManager();

}
