/*
GanttProject is an opensource project management tool.
Copyright (C) 2005-2011 GanttProject team

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
package net.sourceforge.ganttproject.document;

import java.io.File;
import java.io.IOException;

import net.sourceforge.ganttproject.gui.options.model.GPOption;
import net.sourceforge.ganttproject.gui.options.model.GPOptionGroup;
import net.sourceforge.ganttproject.gui.options.model.StringOption;

/**
 * @author bard
 */
public interface DocumentManager {
  Document newAutosaveDocument() throws IOException;

  Document getLastAutosaveDocument(Document priorTo) throws IOException;

  Document getDocument(String path);

  void addToRecentDocuments(Document document);

  Document getDocument(String path, String userName, String password);

  void changeWorkingDirectory(File parentFile);

  String getWorkingDirectory();

  GPOptionGroup getOptionGroup();

  FTPOptions getFTPOptions();

  GPOptionGroup[] getNetworkOptionGroups();

  DocumentStorageUi getWebDavStorageUi();

  abstract class FTPOptions extends GPOptionGroup {
    public FTPOptions(String id, GPOption<?>[] options) {
      super(id, options);
    }

    public abstract StringOption getServerName();

    public abstract StringOption getUserName();

    public abstract StringOption getDirectoryName();

    public abstract StringOption getPassword();
  }
}
