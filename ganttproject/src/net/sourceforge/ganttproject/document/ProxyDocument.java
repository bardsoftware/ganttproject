/*
 * Created on 12.03.2005
 */
package net.sourceforge.ganttproject.document;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;

import org.eclipse.core.runtime.IStatus;
import org.xml.sax.Attributes;

import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.calendar.GPCalendar;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.TableHeaderUIFacade;
import net.sourceforge.ganttproject.io.GPSaver;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.parser.AllocationTagHandler;
import net.sourceforge.ganttproject.parser.CustomPropertiesTagHandler;
import net.sourceforge.ganttproject.parser.DefaultWeekTagHandler;
import net.sourceforge.ganttproject.parser.DependencyTagHandler;
import net.sourceforge.ganttproject.parser.FileFormatException;
import net.sourceforge.ganttproject.parser.GPParser;
import net.sourceforge.ganttproject.parser.HolidayTagHandler;
import net.sourceforge.ganttproject.parser.ParserFactory;
import net.sourceforge.ganttproject.parser.PreviousStateTasksTagHandler;
import net.sourceforge.ganttproject.parser.ResourceTagHandler;
import net.sourceforge.ganttproject.parser.RoleTagHandler;
import net.sourceforge.ganttproject.parser.TagHandler;
import net.sourceforge.ganttproject.parser.TaskDisplayColumnsTagHandler;
import net.sourceforge.ganttproject.parser.TaskPropertiesTagHandler;
import net.sourceforge.ganttproject.parser.TaskTagHandler;
import net.sourceforge.ganttproject.parser.VacationTagHandler;
import net.sourceforge.ganttproject.parser.ViewTagHandler;
import net.sourceforge.ganttproject.resource.HumanResourceManager;
import net.sourceforge.ganttproject.roles.RoleManager;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.task.TaskManagerImpl;

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

    private final TableHeaderUIFacade myVisibleFields;

    ProxyDocument(DocumentCreator creator, Document physicalDocument, IGanttProject project,
            UIFacade uiFacade, TableHeaderUIFacade visibleFields, ParserFactory parserFactory) {
        myPhysicalDocument = physicalDocument;
        myProject = project;
        myUIFacade = uiFacade;
        myParserFactory = parserFactory;
        myCreator = creator;
        myVisibleFields = visibleFields;
    }

    public String getDescription() {
        return myPhysicalDocument.getDescription();
    }

    public boolean canRead() {
        return myPhysicalDocument.canRead();
    }

    public IStatus canWrite() {
        return myPhysicalDocument.canWrite();
    }

    public boolean isValidForMRU() {
        return myPhysicalDocument.isValidForMRU();
    }

    public boolean acquireLock() {
        return myPhysicalDocument.acquireLock();
    }

    public void releaseLock() {
        myPhysicalDocument.releaseLock();
    }

    public InputStream getInputStream() throws IOException {
        return myPhysicalDocument.getInputStream();
    }

    public OutputStream getOutputStream() throws IOException {
        return myPhysicalDocument.getOutputStream();
    }

    public String getPath() {
        return myPhysicalDocument.getPath();
    }

    public String getFilePath() {
        String result = myPhysicalDocument.getFilePath();
        if (result==null) {
            try {
                result = myCreator.createTemporaryFile();
            } catch (IOException e) {
                myUIFacade.showErrorDialog(e);
            }
        }
        return result;
    }

    public String getURLPath() {
        return myPhysicalDocument.getURLPath();
    }

    public String getUsername() {
        return myPhysicalDocument.getUsername();
    }

    public String getPassword() {
        return myPhysicalDocument.getPassword();
    }

    public String getLastError() {
        return myPhysicalDocument.getLastError();
    }

    public void read() throws IOException, DocumentException {
        FailureState failure = new FailureState();
        SuccessState success = new SuccessState();
        ParsingState parsing = new ParsingState(success, failure);
//        OpenCopyConfirmationState confirmation = new OpenCopyConfirmationState(
//                parsing, failure);
//        AcquireLockState lock = new AcquireLockState(parsing, confirmation);
        try {
            getTaskManager().setEventsEnabled(false);
            parsing.enter();
        }
        finally {
            getTaskManager().setEventsEnabled(true);
        }
        //lock.enter();
    }

    public void write() throws IOException {
        GPSaver saver = myParserFactory.newSaver();
        byte[] buffer;
        try {
            ByteArrayOutputStream bufferStream = new ByteArrayOutputStream();
            saver.save(bufferStream);
            bufferStream.flush();
            buffer = bufferStream.toByteArray();
        }
        catch (IOException e) {
            getUIFacade().showErrorDialog(e);
            return;
        }
        OutputStream output = getOutputStream();
        try {
            output.write(buffer);
            output.flush();
        }
        finally {
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
        return (HumanResourceManager) myProject.getHumanResourceManager();
    }

    private GPCalendar getActiveCalendar() {
        return myProject.getActiveCalendar();
    }

    private UIFacade getUIFacade() {
        return myUIFacade;
    }

    class AcquireLockState {
        OpenCopyConfirmationState myConfirmationState;

        ParsingState myParsingState;

        /**
         * @param parsing
         * @param confirmation
         */
        public AcquireLockState(ParsingState parsing,
                OpenCopyConfirmationState confirmation) {
            myParsingState = parsing;
            myConfirmationState = confirmation;
        }

        void enter() throws IOException, DocumentException {
            boolean locked = acquireLock();
            if (!locked) {
                myConfirmationState.enter();
            } else {
                myParsingState.enter();
            }
        }
    }


    class OpenCopyConfirmationState {
        ParsingState myParsingState;

        FailureState myExitState;

        public OpenCopyConfirmationState(ParsingState parsing,
                FailureState failure) {
            myParsingState = parsing;
            myExitState = failure;
        }

        void enter() throws IOException, DocumentException {
            String message = GanttLanguage.getInstance().getText("msg13");
            String title = GanttLanguage.getInstance().getText("warning");
            if (UIFacade.Choice.YES==getUIFacade().showConfirmationDialog(message, title)) {
                myParsingState.enter();
            } else {
                myExitState.enter();
            }
        }
    }

    class ParsingState {
        FailureState myFailureState;

        SuccessState mySuccessState;

        public ParsingState(SuccessState success, FailureState failure) {
            mySuccessState = success;
            myFailureState = failure;
        }

        void enter() throws IOException, DocumentException {
            GPParser opener = myParserFactory.newParser();
            HumanResourceManager hrManager = getHumanResourceManager();
            RoleManager roleManager = getRoleManager();
            TaskManager taskManager = getTaskManager();
            ResourceTagHandler resourceHandler = new ResourceTagHandler(
                    hrManager, roleManager, myProject.getResourceCustomPropertyManager());
            DependencyTagHandler dependencyHandler = new DependencyTagHandler(
                    opener.getContext(), taskManager, getUIFacade());
            AllocationTagHandler allocationHandler = new AllocationTagHandler(
                    hrManager, getTaskManager(), getRoleManager());
            VacationTagHandler vacationHandler = new VacationTagHandler(
                    hrManager);
            PreviousStateTasksTagHandler previousStateHandler =
                new PreviousStateTasksTagHandler(myProject.getBaselines());
            RoleTagHandler rolesHandler = new RoleTagHandler(roleManager);
            TaskTagHandler taskHandler = new TaskTagHandler(taskManager, opener
                    .getContext());
            DefaultWeekTagHandler weekHandler = new DefaultWeekTagHandler(
                    getActiveCalendar());
            OnlyShowWeekendsTagHandler onlyShowWeekendsHandler = new OnlyShowWeekendsTagHandler(
                    getActiveCalendar());
            ViewTagHandler viewHandler = new ViewTagHandler(getUIFacade());

            TaskPropertiesTagHandler taskPropHandler = new TaskPropertiesTagHandler(myProject.getTaskCustomColumnManager());
            opener.addTagHandler(taskPropHandler);
            CustomPropertiesTagHandler customPropHandler = new CustomPropertiesTagHandler(
                    opener.getContext(), getTaskManager(), myProject.getCustomColumnsStorage());
            opener.addTagHandler(customPropHandler);
            TaskDisplayColumnsTagHandler taskDisplayHandler =
                new TaskDisplayColumnsTagHandler(myVisibleFields);
            opener.addTagHandler(taskDisplayHandler);

            TaskDisplayColumnsTagHandler resourceViewHandler = new TaskDisplayColumnsTagHandler(
                    getUIFacade().getResourceTree().getVisibleFields(), "field", "id", "order", "width");
            opener.addTagHandler(resourceViewHandler);
            opener.addParsingListener(resourceViewHandler);

            opener.addTagHandler(taskHandler);

            opener.addParsingListener(taskPropHandler);
            opener.addParsingListener(taskDisplayHandler);
            opener.addParsingListener(customPropHandler);

            opener.addTagHandler(opener.getDefaultTagHandler());
            opener.addTagHandler(resourceHandler);
            opener.addTagHandler(dependencyHandler);
            opener.addTagHandler(allocationHandler);
            opener.addParsingListener(allocationHandler);
            opener.addTagHandler(vacationHandler);
            opener.addTagHandler(previousStateHandler);
            opener.addTagHandler(rolesHandler);
            opener.addTagHandler(weekHandler);
            opener.addTagHandler(onlyShowWeekendsHandler);
            opener.addTagHandler(viewHandler);
            opener.addParsingListener(dependencyHandler);
            opener.addParsingListener(resourceHandler);

            HolidayTagHandler holidayHandler = new HolidayTagHandler(myProject);
            opener.addTagHandler(holidayHandler);
            opener.addParsingListener(holidayHandler);

            PortfolioTagHandler portfolioHandler = new PortfolioTagHandler();
            opener.addTagHandler(portfolioHandler);
            InputStream is;
			try {
				is = getInputStream();
			} catch (IOException e) {
				myFailureState.enter();
				throw new DocumentException(GanttLanguage.getInstance().getText("msg8")
						+ ": " + e.getLocalizedMessage(), e);
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

    public URI getURI() {
        return myPhysicalDocument.getURI();
    }

    public boolean isLocal() {
        return myPhysicalDocument.isLocal();
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     * @author arun_ram
     * Added on Feb 26, 2006
     */
    public boolean equals(Object doc) {
        if (false == doc instanceof ProxyDocument) {
            return false;
        }
        return getPath().equals(((Document)doc).getPath());
    }

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
    private class PortfolioTagHandler implements TagHandler {
        private static final String PORTFOLIO_TAG = "portfolio";
        private static final String PROJECT_TAG = "project";
        private static final String LOCATION_ATTR = "location";
        private boolean isReadingPortfolio = false;
        public void startElement(String namespaceURI, String sName, String qName,
                Attributes attrs) throws FileFormatException {
            if (PORTFOLIO_TAG.equals(qName)) {
                isReadingPortfolio = true;
                return;
            }
            if (PROJECT_TAG.equals(qName) && isReadingPortfolio) {
                String locationAsString = attrs.getValue(LOCATION_ATTR);
                if (locationAsString!=null) {
                    Document document = myCreator.getDocument(locationAsString);
                    getPortfolioImpl().setDefaultDocument(document);
                }

            }
        }

        public void endElement(String namespaceURI, String sName, String qName) {
            if (PORTFOLIO_TAG.equals(qName)) {
                isReadingPortfolio = false;
            }
        }
    }

    private static class OnlyShowWeekendsTagHandler implements TagHandler {

        private final GPCalendar calendar;

        public OnlyShowWeekendsTagHandler(GPCalendar calendar) {
            this.calendar = calendar;
        }

        public void startElement(String namespaceURI, String sName,
                String qName, Attributes attrs) {
            if ("only-show-weekends".equals(qName))
                calendar.setOnlyShowWeekends(Boolean.parseBoolean(attrs.getValue("value")));
        }

        public void endElement(String namespaceURI, String sName, String qName) {
        }
    }
}
