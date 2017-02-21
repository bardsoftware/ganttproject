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

import biz.ganttproject.core.calendar.GPCalendarCalc;
import biz.ganttproject.core.option.ListOption;
import biz.ganttproject.core.table.ColumnList;
import com.google.common.collect.ImmutableSet;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.gui.GPColorChooser;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.io.GPSaver;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.parser.AbstractTagHandler;
import net.sourceforge.ganttproject.parser.AllocationTagHandler;
import net.sourceforge.ganttproject.parser.CustomPropertiesTagHandler;
import net.sourceforge.ganttproject.parser.DefaultWeekTagHandler;
import net.sourceforge.ganttproject.parser.DependencyTagHandler;
import net.sourceforge.ganttproject.parser.FileFormatException;
import net.sourceforge.ganttproject.parser.GPParser;
import net.sourceforge.ganttproject.parser.HolidayTagHandler;
import net.sourceforge.ganttproject.parser.OptionTagHandler;
import net.sourceforge.ganttproject.parser.ParserFactory;
import net.sourceforge.ganttproject.parser.ParsingListener;
import net.sourceforge.ganttproject.parser.PreviousStateTasksTagHandler;
import net.sourceforge.ganttproject.parser.ResourceTagHandler;
import net.sourceforge.ganttproject.parser.RoleTagHandler;
import net.sourceforge.ganttproject.parser.TaskDisplayColumnsTagHandler;
import net.sourceforge.ganttproject.parser.TaskPropertiesTagHandler;
import net.sourceforge.ganttproject.parser.TaskTagHandler;
import net.sourceforge.ganttproject.parser.VacationTagHandler;
import net.sourceforge.ganttproject.parser.ViewTagHandler;
import net.sourceforge.ganttproject.resource.HumanResourceManager;
import net.sourceforge.ganttproject.roles.RoleManager;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.task.TaskManagerImpl;
import org.eclipse.core.runtime.IStatus;
import org.xml.sax.Attributes;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Set;

/**
 * @author bard
 */
class ProxyDocument implements Document {
  private final Document myPhysicalDocument;

  private IGanttProject myProject;

  private UIFacade myUIFacade;

  private final ParserFactory myParserFactory;

  private final DocumentCreator myCreator;

  private PortfolioImpl myPortfolio;

  private final ColumnList myTaskVisibleFields;

  private final ColumnList myResourceVisibleFields;

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
    FailureState failure = new FailureState();
    SuccessState success = new SuccessState();
    ParsingState parsing = new ParsingState(success, failure);
    // OpenCopyConfirmationState confirmation = new OpenCopyConfirmationState(
    // parsing, failure);
    // AcquireLockState lock = new AcquireLockState(parsing, confirmation);
    try {
      getTaskManager().setEventsEnabled(false);
      parsing.enter();
    } finally {
      getTaskManager().setEventsEnabled(true);
    }
    // lock.enter();
  }

  @Override
  public void write() throws IOException {
    GPSaver saver = myParserFactory.newSaver();
    byte[] buffer;
    try {
      ByteArrayOutputStream bufferStream = new ByteArrayOutputStream();
      saver.save(bufferStream);
      bufferStream.flush();
      buffer = bufferStream.toByteArray();
    } catch (IOException e) {
      getUIFacade().showErrorDialog(e);
      return;
    }
    OutputStream output = getOutputStream();
    try {
      output.write(buffer);
      output.flush();
    } finally {
      output.close();
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

  private GPCalendarCalc getActiveCalendar() {
    return myProject.getActiveCalendar();
  }

  private UIFacade getUIFacade() {
    return myUIFacade;
  }

  // class AcquireLockState {
  // OpenCopyConfirmationState myConfirmationState;
  //
  // ParsingState myParsingState;
  //
  // public AcquireLockState(ParsingState parsing,
  // OpenCopyConfirmationState confirmation) {
  // myParsingState = parsing;
  // myConfirmationState = confirmation;
  // }
  //
  // void enter() throws IOException, DocumentException {
  // boolean locked = acquireLock();
  // if (!locked) {
  // myConfirmationState.enter();
  // } else {
  // myParsingState.enter();
  // }
  // }
  // }
  //
  // class OpenCopyConfirmationState {
  // private final ParsingState myParsingState;
  //
  // private final FailureState myExitState;
  //
  // public OpenCopyConfirmationState(ParsingState parsing,
  // FailureState failure) {
  // myParsingState = parsing;
  // myExitState = failure;
  // }
  //
  // void enter() throws IOException, DocumentException {
  // String message = GanttLanguage.getInstance().getText("msg13");
  // String title = GanttLanguage.getInstance().getText("warning");
  // if (UIFacade.Choice.YES==getUIFacade().showConfirmationDialog(message,
  // title)) {
  // myParsingState.enter();
  // } else {
  // myExitState.enter();
  // }
  // }
  // }

  class ParsingState {
    private final FailureState myFailureState;

    private final SuccessState mySuccessState;

    public ParsingState(SuccessState success, FailureState failure) {
      mySuccessState = success;
      myFailureState = failure;
    }

    void enter() throws IOException, DocumentException {
      GPParser opener = myParserFactory.newParser();
      HumanResourceManager hrManager = getHumanResourceManager();
      RoleManager roleManager = getRoleManager();
      TaskManager taskManager = getTaskManager();
      ResourceTagHandler resourceHandler = new ResourceTagHandler(hrManager, roleManager,
          myProject.getResourceCustomPropertyManager());
      DependencyTagHandler dependencyHandler = new DependencyTagHandler(opener.getContext(), taskManager, getUIFacade());
      AllocationTagHandler allocationHandler = new AllocationTagHandler(hrManager, getTaskManager(), getRoleManager());
      VacationTagHandler vacationHandler = new VacationTagHandler(hrManager);
      PreviousStateTasksTagHandler previousStateHandler = new PreviousStateTasksTagHandler(myProject.getBaselines());
      RoleTagHandler rolesHandler = new RoleTagHandler(roleManager);
      TaskTagHandler taskHandler = new TaskTagHandler(taskManager, opener.getContext(), myUIFacade.getTaskTree());
      DefaultWeekTagHandler weekHandler = new DefaultWeekTagHandler(getActiveCalendar());
      OnlyShowWeekendsTagHandler onlyShowWeekendsHandler = new OnlyShowWeekendsTagHandler(getActiveCalendar());

      TaskPropertiesTagHandler taskPropHandler = new TaskPropertiesTagHandler(myProject.getTaskCustomColumnManager());
      opener.addTagHandler(taskPropHandler);
      CustomPropertiesTagHandler customPropHandler = new CustomPropertiesTagHandler(opener.getContext(),
          getTaskManager());
      opener.addTagHandler(customPropHandler);


      TaskDisplayColumnsTagHandler pilsenTaskDisplayHandler = TaskDisplayColumnsTagHandler.createPilsenHandler();
      TaskDisplayColumnsTagHandler legacyTaskDisplayHandler = TaskDisplayColumnsTagHandler.createLegacyHandler();

      opener.addTagHandler(pilsenTaskDisplayHandler);
      opener.addTagHandler(legacyTaskDisplayHandler);
      opener.addParsingListener(TaskDisplayColumnsTagHandler.createTaskDisplayColumnsWrapper(myTaskVisibleFields, pilsenTaskDisplayHandler, legacyTaskDisplayHandler));
      opener.addTagHandler(new ViewTagHandler("gantt-chart", getUIFacade(), pilsenTaskDisplayHandler));


      TaskDisplayColumnsTagHandler resourceFieldsHandler = new TaskDisplayColumnsTagHandler(
          "field", "id", "order", "width", "visible");
      opener.addTagHandler(resourceFieldsHandler);
      opener.addParsingListener(TaskDisplayColumnsTagHandler.createTaskDisplayColumnsWrapper(myResourceVisibleFields, resourceFieldsHandler));
      opener.addTagHandler(new ViewTagHandler("resource-table", getUIFacade(), resourceFieldsHandler));

      opener.addTagHandler(taskHandler);
      opener.addParsingListener(taskHandler);

      opener.addParsingListener(customPropHandler);

      opener.addTagHandler(opener.getDefaultTagHandler());
      opener.addTagHandler(opener.getTimelineTagHandler());
      opener.addParsingListener((ParsingListener)opener.getTimelineTagHandler());
      opener.addTagHandler(resourceHandler);
      opener.addTagHandler(dependencyHandler);
      opener.addTagHandler(allocationHandler);
      opener.addParsingListener(allocationHandler);
      opener.addTagHandler(vacationHandler);
      opener.addTagHandler(previousStateHandler);
      opener.addTagHandler(rolesHandler);
      opener.addTagHandler(weekHandler);
      opener.addTagHandler(onlyShowWeekendsHandler);
      opener.addTagHandler(new OptionTagHandler<ListOption<Color>>(GPColorChooser.getRecentColorsOption()));
      opener.addParsingListener(dependencyHandler);
      opener.addParsingListener(resourceHandler);


      HolidayTagHandler holidayHandler = new HolidayTagHandler(myProject.getActiveCalendar());
      opener.addTagHandler(new AbstractTagHandler("calendars") {
        @Override
        protected boolean onStartElement(Attributes attrs) {
          myProject.getActiveCalendar().setBaseCalendarID(attrs.getValue("base-id"));
          return true;
        }
      });
      opener.addTagHandler(holidayHandler);

      PortfolioTagHandler portfolioHandler = new PortfolioTagHandler();
      opener.addTagHandler(portfolioHandler);
      InputStream is;
      try {
        is = getInputStream();
      } catch (IOException e) {
        myFailureState.enter();
        throw new DocumentException(GanttLanguage.getInstance().getText("msg8") + ": " + e.getLocalizedMessage(), e);
      }
      if (opener.load(is)) {
        mySuccessState.enter();
      } else {
        myFailureState.enter();
      }
    }
  }

  class SuccessState {
    void enter() {
    }
  }

  class FailureState {
    void enter() {
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
    if (false == doc instanceof ProxyDocument) {
      return false;
    }
    return getPath().equals(((Document) doc).getPath());
  }

  @Override
  public Portfolio getPortfolio() {
    return myPortfolio;
  }

  private PortfolioImpl getPortfolioImpl() {
    if (myPortfolio == null) {
      myPortfolio = new PortfolioImpl();
    }
    return myPortfolio;
  }

  private class PortfolioImpl implements Portfolio {
    private Document myDefaultDocument;

    @Override
    public Document getDefaultDocument() {
      return myDefaultDocument;
    }

    void setDefaultDocument(Document document) {
      if (myDefaultDocument != null) {
        throw new IllegalStateException("Don't set default document twice");
      }
      myDefaultDocument = document;
    }
  }

  private class PortfolioTagHandler extends AbstractTagHandler {
    private static final String PORTFOLIO_TAG = "portfolio";
    private static final String PROJECT_TAG = "project";
    private final Set<String> TAGS = ImmutableSet.of(PORTFOLIO_TAG, PROJECT_TAG);
    private static final String LOCATION_ATTR = "location";
    private boolean isReadingPortfolio = false;

    public PortfolioTagHandler() {
      super(null, false);
    }
    @Override
    public void startElement(String namespaceURI, String sName, String qName, Attributes attrs)
        throws FileFormatException {
      if (!TAGS.contains(qName)) {
        return;
      }
      setTagStarted(true);
      if (PORTFOLIO_TAG.equals(qName)) {
        isReadingPortfolio = true;
        return;
      }
      if (PROJECT_TAG.equals(qName) && isReadingPortfolio) {
        String locationAsString = attrs.getValue(LOCATION_ATTR);
        if (locationAsString != null) {
          Document document = myCreator.getDocument(locationAsString);
          getPortfolioImpl().setDefaultDocument(document);
        }

      }
    }

    @Override
    public void endElement(String namespaceURI, String sName, String qName) {
      if (!TAGS.contains(qName)) {
        return;
      }
      if (PORTFOLIO_TAG.equals(qName)) {
        isReadingPortfolio = false;
      }
      setTagStarted(false);
    }
  }

  private static class OnlyShowWeekendsTagHandler extends AbstractTagHandler {

    private final GPCalendarCalc calendar;

    public OnlyShowWeekendsTagHandler(GPCalendarCalc calendar) {
      super("only-show-weekends");
      this.calendar = calendar;
    }

    @Override
    protected boolean onStartElement(Attributes attrs) {
      calendar.setOnlyShowWeekends(Boolean.parseBoolean(attrs.getValue("value")));
      return true;
    }
  }
}
