package net.sourceforge.ganttproject.document.webdav;

import junit.framework.TestCase;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.document.DocumentManager;
import net.sourceforge.ganttproject.gui.UIFacade;
import org.easymock.EasyMock;

import javax.swing.JComponent;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.plaf.SplitPaneUI;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import java.awt.Component;
import java.awt.Container;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * The WebDAV settings page splits into a server list on the left and the details of the selected
 * server on the right. These tests lay the page out at a given width and measure how much of each
 * half is on screen.
 *
 * <p>The measured quantity is how wide the "server address" text field is after clipping against
 * all of its ancestors, because that is what the user can see and type into. A field of width 300
 * inside a container of width 0 is not on screen.
 */
public class WebDavOptionPageLayoutTest extends TestCase {
  private static final Logger ourLogger = Logger.getLogger(WebDavOptionPageLayoutTest.class.getName());

  private static final String SERVER_URL = "http://webdav.example.com/dav";
  private static final int PAGE_HEIGHT = 500;

  private IGanttProject myProject;
  private UIFacade myUiFacade;
  private WebDavStorageImpl myStorage;

  @Override
  protected void setUp() {
    myProject = EasyMock.createNiceMock(IGanttProject.class);
    myUiFacade = EasyMock.createNiceMock(UIFacade.class);
    DocumentManager documentManager = EasyMock.createNiceMock(DocumentManager.class);
    EasyMock.replay(myProject, myUiFacade, documentManager);

    myStorage = new WebDavStorageImpl(myProject, myUiFacade);

    EasyMock.reset(myProject, documentManager);
    EasyMock.expect(myProject.getDocumentManager()).andReturn(documentManager).anyTimes();
    EasyMock.expect(documentManager.getWebDavStorageUi()).andReturn(myStorage).anyTimes();
    EasyMock.replay(myProject, documentManager);
  }

  /**
   * A page narrower than the server list wants to be. The server details must still be on screen.
   */
  public void testServerDetailsAreVisibleOnANarrowPage() throws Exception {
    onEventDispatchThread(() -> {
      Page page = new Page();
      int pageWidth = page.listPreferredWidth() - 40;
      assertTrue(
          "this test is only about pages that are narrower than the server list wants to be, "
              + "but the list asks for only " + page.listPreferredWidth() + " px",
          pageWidth < page.listPreferredWidth());

      page.layoutAt(pageWidth);
      ourLogger.info("narrow page: " + page.describe(pageWidth));

      assertTrue("the server details got width " + page.detailsWidth()
              + " on a page of width " + pageWidth + "; the address, user name and password fields "
              + "are then not on screen at all",
          page.detailsWidth() > 0);
      assertTrue("the server address field is " + page.visibleUrlFieldWidth()
              + " px wide on a page of width " + pageWidth,
          page.visibleUrlFieldWidth() > 0);
    });
  }

  /**
   * A page wide enough for both halves. Neither half may end up narrower than it asks for.
   */
  public void testWidePageGivesBothHalvesWhatTheyAskFor() throws Exception {
    onEventDispatchThread(() -> {
      Page page = new Page();
      int pageWidth = page.listPreferredWidth() + page.detailsPreferredWidth() + 200;

      page.layoutAt(pageWidth);
      ourLogger.info("wide page: " + page.describe(pageWidth));

      assertTrue("the server list got " + page.listWidth() + " px, less than the "
              + page.listPreferredWidth() + " px it asks for, although the page is "
              + pageWidth + " px wide",
          page.listWidth() >= page.listPreferredWidth());
      assertTrue("the server details got " + page.detailsWidth() + " px, less than the "
              + page.detailsPreferredWidth() + " px they ask for, although the page is "
              + pageWidth + " px wide",
          page.detailsWidth() >= page.detailsPreferredWidth());
      assertTrue("the server address field is only " + page.visibleUrlFieldWidth()
              + " px wide on a page of width " + pageWidth,
          page.visibleUrlFieldWidth() >= page.detailsPreferredWidth() / 2);
    });
  }

  /**
   * A page a little too narrow for both halves. Both must give way; neither may collapse to its
   * bare minimum while the other keeps a surplus. Translations with longer labels put the page in
   * this range without the user resizing anything.
   */
  public void testSlightlyTooNarrowPageShrinksBothHalves() throws Exception {
    onEventDispatchThread(() -> {
      Page page = new Page();
      int bothHalves = page.listPreferredWidth() + page.detailsPreferredWidth();
      int pageWidth = bothHalves - 30;
      assertTrue("this test is about a page that is too narrow for both halves, but "
              + pageWidth + " px is enough for " + bothHalves + " px", pageWidth < bothHalves);

      page.layoutAt(pageWidth);
      ourLogger.info("slightly too narrow page: " + page.describe(pageWidth));

      assertTrue("the page is " + pageWidth + " px wide, only " + (bothHalves - pageWidth)
              + " px short of both halves, and the server list collapsed to " + page.listWidth()
              + " px of the " + page.listPreferredWidth() + " px it asks for",
          page.listWidth() >= page.listPreferredWidth() / 2);
      assertTrue("the page is " + pageWidth + " px wide, only " + (bothHalves - pageWidth)
              + " px short of both halves, and the server details collapsed to "
              + page.detailsWidth() + " px of the " + page.detailsPreferredWidth()
              + " px they ask for",
          page.detailsWidth() >= page.detailsPreferredWidth() / 2);
    });
  }

  /**
   * On a narrow page the user must be able to shift the boundary and reach either half. A split
   * pane whose halves keep their built-in minimum widths cannot be dragged at all when the page is
   * too small for both minimums, which would leave the same problem in a new shape.
   */
  public void testDividerCanBeMovedOnANarrowPage() throws Exception {
    onEventDispatchThread(() -> {
      Page page = new Page();
      int pageWidth = page.listPreferredWidth() - 40;
      page.layoutAt(pageWidth);

      JSplitPane split = page.splitPane();
      assertNotNull("the two halves of the page are not separated by a movable divider", split);
      SplitPaneUI ui = split.getUI();
      assertTrue("unexpected split pane UI " + ui, ui instanceof BasicSplitPaneUI);
      int lowest = ((BasicSplitPaneUI) ui).getMinimumDividerLocation(split);
      int highest = ((BasicSplitPaneUI) ui).getMaximumDividerLocation(split);
      ourLogger.info("narrow page: divider can be dragged between " + lowest + " and " + highest);
      assertTrue("the divider cannot be moved on a page of width " + pageWidth
              + ": it can only sit between " + lowest + " and " + highest,
          lowest < highest);

      split.setDividerLocation(highest);
      page.relayout();
      ourLogger.info("divider at its rightmost: " + page.describe(pageWidth));
      assertTrue("with the divider dragged as far right as it goes the server details are "
              + page.detailsWidth() + " px wide", page.detailsWidth() > 0);
      assertTrue("with the divider dragged as far right as it goes the server address field is "
              + page.visibleUrlFieldWidth() + " px wide", page.visibleUrlFieldWidth() > 0);

      split.setDividerLocation(lowest);
      page.relayout();
      ourLogger.info("divider at its leftmost: " + page.describe(pageWidth));
      assertTrue("with the divider dragged as far left as it goes the server list is "
              + page.listWidth() + " px wide", page.listWidth() > 0);
      assertTrue("with the divider dragged as far left as it goes the server address field is "
              + page.visibleUrlFieldWidth() + " px wide", page.visibleUrlFieldWidth() > 0);
    });
  }

  /** The built settings page, plus the measurements taken on it. */
  private class Page {
    private final JComponent myPage;
    private final Component myListArea;
    private final Component myDetailsArea;
    private final JTextField myUrlField;

    Page() {
      WebDavServerDescriptor server = new WebDavServerDescriptor();
      server.setName("test server");
      server.setRootUrl(SERVER_URL);
      myStorage.getServersOption().addValue(server);
      myStorage.getServersOption().setValueIndex(0);

      WebDavOptionPageProvider provider = new WebDavOptionPageProvider();
      provider.init(myProject, myUiFacade);
      myPage = (JComponent) provider.buildPageComponent();

      JTable table = findFirst(myPage, JTable.class);
      assertNotNull("no server list on the settings page", table);
      myUrlField = findTextFieldWithText(myPage, SERVER_URL);
      assertNotNull("no server address field on the settings page", myUrlField);
      myListArea = halfOfThePageHolding(table, myUrlField);
      myDetailsArea = halfOfThePageHolding(myUrlField, table);
      assertNotSame("the server list and the server details are not in separate halves of the page",
          myListArea, myDetailsArea);
    }

    int listPreferredWidth() {
      return myListArea.getPreferredSize().width;
    }

    int detailsPreferredWidth() {
      return myDetailsArea.getPreferredSize().width;
    }

    void layoutAt(int width) {
      myPage.setSize(width, PAGE_HEIGHT);
      relayout();
    }

    void relayout() {
      layoutTree(myPage);
    }

    int listWidth() {
      return myListArea.getWidth();
    }

    int detailsWidth() {
      return myDetailsArea.getWidth();
    }

    int visibleUrlFieldWidth() {
      return visibleWidth(myUrlField, myPage);
    }

    JSplitPane splitPane() {
      return findFirst(myPage, JSplitPane.class);
    }

    String describe(int pageWidth) {
      return String.format(
          "page width=%d | server list x=%d width=%d | server details x=%d width=%d "
              + "| address field visible width=%d",
          pageWidth, xIn(myListArea, myPage), listWidth(),
          xIn(myDetailsArea, myPage), detailsWidth(), visibleUrlFieldWidth());
    }

    /**
     * The topmost container below the page that holds {@code inside} but not {@code outside}. That
     * is one half of the page, whichever way the page happens to be split.
     */
    private Component halfOfThePageHolding(Component inside, Component outside) {
      Component result = inside;
      for (Component c = inside; c != null && c != myPage; c = c.getParent()) {
        if (!isAncestorOf(c, outside)) {
          result = c;
        }
      }
      return result;
    }
  }

  private static void onEventDispatchThread(Runnable body) throws Exception {
    final Throwable[] thrown = new Throwable[1];
    SwingUtilities.invokeAndWait(() -> {
      try {
        body.run();
      } catch (Throwable e) {
        thrown[0] = e;
      }
    });
    if (thrown[0] instanceof Error) {
      throw (Error) thrown[0];
    }
    if (thrown[0] != null) {
      throw new AssertionError(thrown[0]);
    }
  }

  /** Lays out the whole subtree; the page is not in a window, so validate() does nothing here. */
  private static void layoutTree(Component component) {
    if (component instanceof Container) {
      Container container = (Container) component;
      container.doLayout();
      for (Component child : container.getComponents()) {
        layoutTree(child);
      }
    }
  }

  /**
   * How much of {@code component} is on screen: its width after clipping against every ancestor up
   * to {@code root}. Swing paints children clipped to their parents.
   */
  private static int visibleWidth(Component component, Component root) {
    Rectangle clip = new Rectangle(xIn(component, root), 0, component.getWidth(), 1);
    for (Component c = component.getParent(); c != null; c = c.getParent()) {
      clip = clip.intersection(new Rectangle(xIn(c, root), 0, c.getWidth(), 1));
      if (c == root) {
        break;
      }
    }
    return Math.max(0, clip.width);
  }

  /** x of {@code component} in the coordinates of {@code ancestor}. */
  private static int xIn(Component component, Component ancestor) {
    int x = 0;
    for (Component c = component; c != null && c != ancestor; c = c.getParent()) {
      x += c.getX();
    }
    return x;
  }

  private static boolean isAncestorOf(Component ancestor, Component descendant) {
    for (Component c = descendant; c != null; c = c.getParent()) {
      if (c == ancestor) {
        return true;
      }
    }
    return false;
  }

  private static <T> T findFirst(Component component, Class<T> type) {
    if (type.isInstance(component)) {
      return type.cast(component);
    }
    if (component instanceof Container) {
      for (Component child : ((Container) component).getComponents()) {
        T found = findFirst(child, type);
        if (found != null) {
          return found;
        }
      }
    }
    return null;
  }

  private static JTextField findTextFieldWithText(Component component, String text) {
    List<JTextField> fields = new ArrayList<>();
    collectTextFields(component, fields);
    for (JTextField field : fields) {
      if (text.equals(field.getText())) {
        return field;
      }
    }
    return null;
  }

  private static void collectTextFields(Component component, List<JTextField> result) {
    if (component instanceof JTextField) {
      result.add((JTextField) component);
    }
    if (component instanceof Container) {
      for (Component child : ((Container) component).getComponents()) {
        collectTextFields(child, result);
      }
    }
  }
}
