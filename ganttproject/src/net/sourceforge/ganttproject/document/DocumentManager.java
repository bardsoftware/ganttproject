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
import java.util.List;

import net.sourceforge.ganttproject.gui.ProjectMRUMenu;
import biz.ganttproject.core.option.GPOption;
import biz.ganttproject.core.option.GPOptionGroup;
import biz.ganttproject.core.option.StringOption;


/**
 * @author bard
 */
public interface DocumentManager {
  Document newAutosaveDocument() throws IOException;

  Document getLastAutosaveDocument(Document priorTo) throws IOException;

  Document getDocument(String path);

  Document getProxyDocument(Document physicalDocument);

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

  List<String> getRecentDocuments();

  void addListener(DocumentMRUListener listener);

  void addToRecentDocuments(Document document);

  void addToRecentDocuments(String value);

  void clearRecentDocuments();
}
