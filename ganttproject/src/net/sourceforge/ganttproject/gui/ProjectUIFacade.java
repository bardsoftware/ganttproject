/*
 * Created on 13.10.2005
 */
package net.sourceforge.ganttproject.gui;

import java.io.IOException;

import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.document.Document;
import net.sourceforge.ganttproject.gui.options.model.GPOptionGroup;

public interface ProjectUIFacade {
    void saveProject(IGanttProject project);
    void saveProjectAs(IGanttProject project);
    public void saveProjectRemotely(IGanttProject project);
    public boolean ensureProjectSaved(IGanttProject project);
    void openProject(IGanttProject project) throws IOException;
    void openRemoteProject(IGanttProject project) throws IOException;
    void openProject(Document document, IGanttProject project) throws IOException;
    
    void createProject(IGanttProject project);
    GPOptionGroup getOptionGroup();
}
