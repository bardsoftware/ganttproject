/*
GanttProject is an opensource project management tool. License: GPL3
Copyright (C) 2005-2011 GanttProject Team

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

import biz.ganttproject.app.InternationalizationCoreKt;
import biz.ganttproject.app.Localizer;
import biz.ganttproject.core.io.XmlProject;
import biz.ganttproject.core.option.GPOption;
import biz.ganttproject.core.table.ColumnList;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import io.milton.http.exceptions.NotAuthorizedException;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.gui.GPColorChooser;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.io.GPSaver;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.parser.*;
import net.sourceforge.ganttproject.resource.HumanResourceManager;
import net.sourceforge.ganttproject.roles.RoleManager;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.task.TaskManagerImpl;
import org.eclipse.core.runtime.IStatus;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import javax.net.ssl.SSLException;

/**
 * @author bard
 */
public class ProxyDocument implements Document {
  private Document myPhysicalDocument;

  private final IGanttProject myProject;

  private final UIFacade myUIFacade;

  private final ParserFactory myParserFactory;

  private final DocumentCreator myCreator;

  private final ColumnList myTaskVisibleFields;

  private final ColumnList myResourceVisibleFields;
  private byte[] myContents;

  ProxyDocument(DocumentCreator creator, Document physicalDocument, IGanttProject project, UIFacade uiFacade,
      ColumnList taskVisibleFields, ColumnList resourceVisibleFields, ParserFactory parserFactory) {
    myPhysicalDocument = physicalDocument;
    myProject = project;
    myUIFacade = uiFacade;
    myParserFactory = parserFactory;
    myCreator = creator;
    myTaskVisibleFields = taskVisibleFields;
    myResourceVisibleFields = resourceVisibleFields;
  }

  public Document getRealDocument() {
    return myPhysicalDocument;
  }
  @Override
  public void setMirror(Document mirrorDocument) {
    myPhysicalDocument = Preconditions.checkNotNull(mirrorDocument);
  }

  @Override
  public String getFileName() {
    return myPhysicalDocument.getFileName();
  }

  @Override
  public boolean canRead() {
    return myPhysicalDocument.canRead();
  }

  @Override
  public IStatus canWrite() {
    return myPhysicalDocument.canWrite();
  }

  @Override
  public boolean isValidForMRU() {
    return myPhysicalDocument.isValidForMRU();
  }

  @Override
  public boolean acquireLock() {
    return myPhysicalDocument.acquireLock();
  }

  @Override
  public void releaseLock() {
    myPhysicalDocument.releaseLock();
  }

  @Override
  public InputStream getInputStream() throws IOException {
    return myPhysicalDocument.getInputStream();
  }

  @Override
  public OutputStream getOutputStream() throws IOException {
    return myPhysicalDocument.getOutputStream();
  }

  @Override
  public String getPath() {
    return myPhysicalDocument.getPath();
  }

  @Override
  public String getFilePath() {
    String result = myPhysicalDocument.getFilePath();
    if (result == null) {
      try {
        result = myCreator.createTemporaryFile();
      } catch (IOException e) {
        myUIFacade.showErrorDialog(e);
      }
    }
    return result;
  }

  @Override
  public String getUsername() {
    return myPhysicalDocument.getUsername();
  }

  @Override
  public String getPassword() {
    return myPhysicalDocument.getPassword();
  }

  @Override
  public String getLastError() {
    return myPhysicalDocument.getLastError();
  }

  @Override
  public void read() throws IOException, DocumentException {
    try {
      getTaskManager().setEventsEnabled(false);
      getHumanResourceManager().setEventsEnabled(false);
      doParse();
    } catch (Exception e) {
      throw new DocumentException(getReadFailureMessage(e), e);
    } finally {
      getTaskManager().setEventsEnabled(true);
      getHumanResourceManager().setEventsEnabled(true);
    }
  }

  /**
   * Picks the message for a failure which happened while reading a document. The failures which
   * send the user to a different place than a broken file get their own text: a rejected
   * authentication, a server which cannot be reached, a server which does not answer in time and
   * a secure connection which cannot be established. Anything else keeps the generic message.
   */
  static String getReadFailureMessage(Throwable failure) {
    return getReadFailureMessage(failure, InternationalizationCoreKt.getRootLocalizer());
  }

  /**
   * The same, with the localizer passed in, so that a test can see which key is asked for.
   *
   * <p>The chain is walked with the exceptions already visited kept aside, because a cause chain
   * may contain a loop of any length: an exception which reports itself as its own cause, but also
   * two exceptions which report each other, or a loop which the chain only enters further down.
   * Guava's {@code Throwables.getCausalChain} finds such a loop as well, but it answers with an
   * {@code IllegalArgumentException} instead of a shortened chain, and this method runs while a
   * read failure is being reported: an exception thrown here would replace the failure the user is
   * supposed to read about with a failure of the reporting itself.
   */
  static String getReadFailureMessage(Throwable failure, Localizer i18n) {
    Set<Throwable> visited = Collections.newSetFromMap(new IdentityHashMap<>());
    for (Throwable cause = failure; cause != null && visited.add(cause); cause = cause.getCause()) {
      if (cause instanceof NotAuthorizedException) {
        return i18n.formatText("document.error.read.authenticationRejected");
      }
      if (cause instanceof SSLException) {
        // The server did answer, so this is not a reachability problem: the user has to look at the
        // certificate or at the protocol settings, not at the address.
        return i18n.formatText("document.error.read.insecureConnection");
      }
      if (cause instanceof SocketTimeoutException) {
        // The server may be reachable and merely slow, which is different advice than an
        // unreachable one: waiting and trying again can help here.
        return i18n.formatText("document.error.read.timedOut");
      }
      if (cause instanceof UnknownHostException || cause instanceof ConnectException
          || cause instanceof NoRouteToHostException) {
        return i18n.formatText("document.error.read.serverUnreachable");
      }
    }
    return i18n.formatText("document.error.read.parseFailure");
  }

  public void createContents() throws IOException {
    if (myContents == null) {
      GPSaver saver = myParserFactory.newSaver();
      ByteArrayOutputStream bufferStream = new ByteArrayOutputStream();
      saver.save(bufferStream);
      bufferStream.flush();
      myContents = bufferStream.toByteArray();
    }
  }
  @Override
  public void write() throws IOException {
    if (myContents == null) {
      createContents();
    }
    try (OutputStream output = getOutputStream()) {
      output.write(myContents);
      output.flush();
    } finally {
      myContents = null;
    }
  }

  private TaskManagerImpl getTaskManager() {
    return (TaskManagerImpl) myProject.getTaskManager();
  }

  private RoleManager getRoleManager() {
    return myProject.getRoleManager();
  }

  private HumanResourceManager getHumanResourceManager() {
    return myProject.getHumanResourceManager();
  }

  private void doParse() throws DocumentException {
    GPParser opener = myParserFactory.newParser();
    HumanResourceManager hrManager = getHumanResourceManager();
    RoleManager roleManager = getRoleManager();
    TaskManager taskManager = getTaskManager();
    ResourceTagHandler resourceHandler = new ResourceTagHandler(hrManager, roleManager, myProject.getResourceCustomPropertyManager(), myUIFacade.getZoomManager(), myResourceVisibleFields);
    AllocationTagHandler allocationHandler = new AllocationTagHandler(hrManager, getTaskManager(), getRoleManager());
    TaskTagHandler taskHandler = new TaskTagHandler(taskManager, myUIFacade.getTaskCollapseView());

    opener.addTagHandler(new AbstractTagHandler("qqq") {
      @Override
      public void process(XmlProject xmlProject) {
        new RoleSerializer(roleManager).loadRoles(xmlProject);
        new CalendarSerializer(myProject.getActiveCalendar()).loadCalendar(xmlProject);
        new BaselineSerializer().loadBaselines(xmlProject, myProject.getBaselines());

        resourceHandler.process(xmlProject);
        taskHandler.process(xmlProject);

        List<GPOption<?>> optionsToRead = new ArrayList<>(myUIFacade.getGanttViewProvider().getOptions());
        optionsToRead.add(GPColorChooser.getRecentColorsOption());
        var ganttChart = myUIFacade.getGanttChart();
        if (ganttChart != null) {
          for (var option : ganttChart.getTaskLabelOptions().getOptions()) {
            optionsToRead.add(option);
          }
        }
        TaskSerializerKt.loadGanttView(xmlProject,
          taskManager,
          myProject.getTaskFilterManager(),
          myUIFacade.getCurrentTaskView(), myUIFacade.getZoomManager(), myTaskVisibleFields, optionsToRead);

        opener.getDefaultTagHandler().process(xmlProject);
        allocationHandler.process(xmlProject);
      }
    });

    try {
      var is = getInputStream();
      if (!opener.load(is)) {
        throw new DocumentException("Can't open document");
      }
    } catch (IOException e) {
      throw new DocumentException(GanttLanguage.getInstance().getText("msg8") + ": " + e.getLocalizedMessage(), e);
    }
  }

  @Override
  public URI getURI() {
    return myPhysicalDocument.getURI();
  }

  @Override
  public boolean isLocal() {
    return myPhysicalDocument.isLocal();
  }

  @Override
  public boolean equals(Object doc) {
    if (!(doc instanceof ProxyDocument)) {
      return false;
    }
    return getPath().equals(((Document) doc).getPath());
  }
}
