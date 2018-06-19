/*
GanttProject is an opensource project management tool.
Copyright (C) 2003-2011 Alexandre Thomas, GanttProject team

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
package net.sourceforge.ganttproject;

import biz.ganttproject.core.model.task.TaskDefaultColumn;
import biz.ganttproject.core.option.BooleanOption;
import biz.ganttproject.core.option.GPOption;
import biz.ganttproject.core.option.GPOptionGroup;
import biz.ganttproject.core.option.ListOption;
import com.google.common.collect.Maps;
import com.google.common.io.ByteStreams;
import com.google.common.xml.XmlEscapers;
import net.sourceforge.ganttproject.document.DocumentManager;
import net.sourceforge.ganttproject.gui.UIConfiguration;
import net.sourceforge.ganttproject.gui.options.model.GP1XOptionConverter;
import net.sourceforge.ganttproject.io.CSVOptions;
import net.sourceforge.ganttproject.io.GanttXMLOpen;
import net.sourceforge.ganttproject.io.OptionSaver;
import net.sourceforge.ganttproject.io.SaverBase;
import net.sourceforge.ganttproject.parser.RoleTagHandler;
import net.sourceforge.ganttproject.roles.Role;
import net.sourceforge.ganttproject.roles.RoleManager;
import net.sourceforge.ganttproject.roles.RoleSet;
import net.sourceforge.ganttproject.util.ColorConvertion;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.DefaultHandler;

import javax.swing.*;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import java.awt.*;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.security.AccessControlException;
import java.util.HashMap;
import java.util.Map;

/**
 * This class is able to load and save options on the file
 */
public class GanttOptions extends SaverBase {

  private int x = 0, y = 0, width = 800, height = 600;
  private boolean isMaximized;

  private boolean isloaded;

  private boolean automatic;

  private boolean redline;

  private int lockDAVMinutes;

  private int undoNumber;

  private String xslDir;

  private String xslFo;

  private String workingDir = "";

  private final RoleManager myRoleManager;

  private UIConfiguration myUIConfig;

  private int toolBarPosition;

  private boolean bShowStatusBar;

  private String iconSize;

  public static final int ICONS = 0;

  public static final int ICONS_TEXT = 1;

  public static final int TEXT = 2;

  private int buttonsshow;

  /**
   * FTP options
   */
  private String FTPUrl = "";

  private String FTPDirectory = "";

  private String FTPUser = "";

  private String FTPPwd = "";

  /**
   * Export options.
   */
  private boolean bExportName;

  private boolean bExportComplete;

  private boolean bExportRelations;

  private boolean bExport3DBorders;

  /**
   * CSV export options.
   */
  private CSVOptions csvOptions;

  private Map<String, GPOption<?>> myGPOptions = Maps.newLinkedHashMap();
  private Map<String, GP1XOptionConverter> myTagDotAttribute_Converter = new HashMap<String, GP1XOptionConverter>();

  private final DocumentManager myDocumentManager;

  private final PluginPreferencesImpl myPluginPreferencesRootNode;

  private String myVersion;

  public GanttOptions(RoleManager roleManager, DocumentManager documentManager, boolean isOnlyViewer) {
    myDocumentManager = documentManager;
    myRoleManager = roleManager;
    myPluginPreferencesRootNode = new PluginPreferencesImpl(null, "");
    initDefault();
    try {
      this.workingDir = System.getProperty("user.home");
    } catch (AccessControlException e) {
      // This can happen when running in a sandbox (Java WebStart)
      System.err.println(e + ": " + e.getMessage());
    }
  }

  public Preferences getPluginPreferences() {
    return myPluginPreferencesRootNode;
  }

  /**
   * Initialize the options with default values.
   */
  public void initDefault() {
    automatic = false;
    redline = false;
    lockDAVMinutes = 240;
    undoNumber = 50;
    xslDir = GanttOptions.class.getResource("/xslt").toString();
    xslFo = GanttOptions.class.getResource("/xslfo/ganttproject.xsl").toString();
    toolBarPosition = JToolBar.HORIZONTAL;
    bShowStatusBar = true;
    // must be 16 small, 24 for big (32 for extra big not directly include on
    // UI)
    iconSize = "16";
    buttonsshow = GanttOptions.ICONS;

    // Export options
    bExportName = true;
    bExportComplete = true;
    bExportRelations = true;
    bExport3DBorders = false;

    // CSV export options
    csvOptions = new CSVOptions();
  }

  public static File getOptionsFile() {
    return new File(System.getProperty("user.home") + System.getProperty("file.separator") + ".ganttproject");
  }


  /**
   * Save the options file
   */
  public void save() {
    try {
      File file = getOptionsFile();
      ByteArrayOutputStream outBuffer = new ByteArrayOutputStream();
      doSave(outBuffer);
      BufferedOutputStream outFile = new BufferedOutputStream(new FileOutputStream(file));
      ByteStreams.copy(new ByteArrayInputStream(outBuffer.toByteArray()), outFile);
      outFile.flush();
      outFile.close();
    } catch (Throwable e) {
      GPLogger.log(e);
    }
  }

  private void doSave(OutputStream out) throws Exception {
    final TransformerHandler handler = ((SAXTransformerFactory) SAXTransformerFactory.newInstance()).newTransformerHandler();
    Transformer serializer = handler.getTransformer();
    serializer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
    serializer.setOutputProperty(OutputKeys.INDENT, "yes");
    serializer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
    handler.setResult(new StreamResult(out));
    handler.startDocument();
    // handler.startDTD("ganttproject.sourceforge.net",
    // "-//GanttProject.org//DTD GanttProject-1.x//EN",
    // "http://ganttproject.sourceforge.net/dtd/ganttproject.dtd");
    // handler.endDTD();

    final AttributesImpl attrs = new AttributesImpl();
    addAttribute("version", GPVersion.getCurrentVersionNumber(), attrs);
    handler.startElement("", "ganttproject-options", "ganttproject-options", attrs);

    attrs.clear();
    // write the task Color

    // Color color = getUIConfiguration().getTaskColor();
    // attrs.addAttribute("", "red", "red", "CDATA", "" + color.getRed());
    // attrs.addAttribute("", "green", "green", "CDATA", ""
    // + color.getGreen());
    // attrs.addAttribute("", "blue", "blue", "CDATA", ""
    // + color.getBlue());
    // handler.startElement("", "task-color", "task-color", attrs);
    // handler.endElement("", "task-color", "task-color"); attrs.clear();

    Color resourceColor = myUIConfig.getResourceColor();
    if (resourceColor != null) {
      attrs.addAttribute("", "resources", "resources", "CDATA", ColorConvertion.getColor(resourceColor));
    }
    Color resourceOverloadColor = myUIConfig.getResourceOverloadColor();
    if (resourceOverloadColor != null) {
      attrs.addAttribute("", "resourcesOverload", "resourcesOverload", "CDATA",
          ColorConvertion.getColor(resourceOverloadColor));
    }
    Color resourceUnderloadColor = myUIConfig.getResourceUnderloadColor();
    if (resourceUnderloadColor != null) {
      attrs.addAttribute("", "resourcesUnderload", "resourcesUnderload", "CDATA",
          ColorConvertion.getColor(resourceUnderloadColor));
    }
    Color weekEndColor = myUIConfig.getWeekEndColor();
    if (weekEndColor != null) {
      attrs.addAttribute("", "weekEnd", "weekEnd", "CDATA", ColorConvertion.getColor(weekEndColor));
    }
    Color daysOffColor = myUIConfig.getDayOffColor();
    if (daysOffColor != null) {
      attrs.addAttribute("", "daysOff", "daysOff", "CDATA", ColorConvertion.getColor(daysOffColor));
    }
    handler.startElement("", "colors", "colors", attrs);
    handler.endElement("", "colors", "colors");
    attrs.clear();

    // Geometry of the window
    addAttribute("x", "" + x, attrs);
    addAttribute("y", "" + y, attrs);
    addAttribute("width", "" + width, attrs);
    addAttribute("height", "" + height, attrs);
    addAttribute("maximized", String.valueOf(this.isMaximized), attrs);
    emptyElement("geometry", attrs, handler);

    // ToolBar position
    addAttribute("position", "" + toolBarPosition, attrs);
    addAttribute("icon-size", "" + iconSize, attrs);
    addAttribute("show", "" + buttonsshow, attrs);
    emptyElement("toolBar", attrs, handler);
    addAttribute("show", "" + bShowStatusBar, attrs);
    emptyElement("statusBar", attrs, handler);

    // Export options
    addAttribute("name", "" + bExportName, attrs);
    addAttribute("complete", "" + bExportComplete, attrs);
    addAttribute("border3d", "" + bExport3DBorders, attrs);
    addAttribute("relations", "" + bExportRelations, attrs);
    emptyElement("export", attrs, handler);

    // csv export options
    startElement("csv-export", attrs, handler);
    addAttribute("fixed", "" + csvOptions.bFixedSize, attrs);
    addAttribute("separatedChar", "" + csvOptions.sSeparatedChar, attrs);
    addAttribute("separatedTextChar", "" + csvOptions.sSeparatedTextChar, attrs);
    emptyElement("csv-general", attrs, handler);

    endElement("csv-export", handler);

    addAttribute("id", "csv-export", attrs);
    startElement("view", attrs, handler);
    for (Map.Entry<String, BooleanOption> entry : csvOptions.getTaskOptions().entrySet()) {
      if (entry.getValue().isChecked()) {
        addAttribute("id", XmlEscapers.xmlAttributeEscaper().escape(entry.getKey()), attrs);
        emptyElement("field", attrs, handler);
      }
    }
    startElement("resources", attrs, handler);
    for (Map.Entry<String, BooleanOption> entry : csvOptions.getResourceOptions().entrySet()) {
      if (entry.getValue().isChecked()) {
        addAttribute("id", XmlEscapers.xmlAttributeEscaper().escape(entry.getKey()), attrs);
        emptyElement("field", attrs, handler);
      }
    }
    endElement("resources", handler);
    if (csvOptions.getBomOption().getValue()) {
      new OptionSaver().saveOption(
          "write-bom", csvOptions.getBomOption(), handler);
    }
    endElement("view", handler);

    // automatic popup launch
    addAttribute("value", "" + automatic, attrs);
    emptyElement("automatic-launch", attrs, handler);
    // automatic tips of the day launch
    // Should WebDAV resources be locked, when opening them?
    addAttribute("value", "" + lockDAVMinutes, attrs);
    emptyElement("lockdavminutes", attrs, handler);
    // write the xsl directory
    addAttribute("dir", xslDir, attrs);
    emptyElement("xsl-dir", attrs, handler);
    // write the xslfo directory
    addAttribute("file", xslFo, attrs);
    emptyElement("xsl-fo", attrs, handler);
    // write the working directory directory
    addAttribute("dir", workingDir, attrs);
    emptyElement("working-dir", attrs, handler);
    // The last opened files
    {
      startElement("files", attrs, handler);

      for (String recent : myDocumentManager.getRecentDocuments()) {
        addAttribute("path", recent, attrs);
        emptyElement("file", attrs, handler);
      }
      endElement("files", handler);
    }

    //addAttribute("category", "chart-main", attrs);
    //addAttribute("spec", getFontSpec(getUIConfiguration().getChartMainFont()), attrs);
    //emptyElement("font", attrs, handler);

    saveRoleSets(handler);
    new OptionSaver().saveOptionMap(myGPOptions.entrySet(), handler);
    savePreferences(myPluginPreferencesRootNode.node("/configuration"), handler);
    savePreferences(myPluginPreferencesRootNode.node("/instance"), handler);
    endElement("ganttproject-options", handler);

    GPLogger.log("[GanttOptions] save(): finished!!");
    handler.endDocument();
  }

  private void savePreferences(Preferences node, TransformerHandler handler) throws BackingStoreException, SAXException {
    AttributesImpl attrs = new AttributesImpl();
    startElement(node.name(), attrs, handler);
    String[] keys = node.keys();
    for (int i = 0; i < keys.length; i++) {
      addAttribute("name", keys[i], attrs);
      addAttribute("value", node.get(keys[i], ""), attrs);
      emptyElement("option", attrs, handler);
    }
    String[] children = node.childrenNames();
    for (int i = 0; i < children.length; i++) {
      savePreferences(node.node(children[i]), handler);
    }
    endElement(node.name(), handler);
  }

  private String getFontSpec(Font font) {
    return font.getFamily() + "-" + getFontStyle(font) + "-" + font.getSize();
  }

  private String getFontStyle(Font font) {
    String result;
    final int BOLDITALIC = Font.BOLD + Font.ITALIC;
    switch (font.getStyle()) {
      case Font.PLAIN: {
        result = "plain";
        break;
      }
      case Font.BOLD: {
        result = "bold";
        break;
      }
      case Font.ITALIC: {
        result = "italic";
        break;
      }
      case BOLDITALIC: {
        result = "bolditalic";
        break;
      }
      default: {
        throw new RuntimeException("Illegal value of font style. style=" + font.getStyle() + " font=" + font);
      }
    }
    return result;
  }

  public String correct(String s) {
    String res;
    res = s.replaceAll("&", "&#38;");
    res = res.replaceAll("<", "&#60;");
    res = res.replaceAll(">", "&#62;");
    res = res.replaceAll("/", "&#47;");
    res = res.replaceAll("\"", "&#34;");
    return res;
  }

  /**
   * Load the options file
   */
  public boolean load() {
    // Use an instance of ourselves as the SAX event handler
    DefaultHandler handler = new GanttXMLOptionsParser();

    // Use the default (non-validating) parser
    SAXParserFactory factory = SAXParserFactory.newInstance();
    try {
      String sFileName = ".ganttproject";
      /*
       * if(System.getProperty("os.name").startsWith("Windows") ||
       * System.getProperty("os.name").startsWith("Mac")) sFileName =
       * "ganttproject.ini";
       */

      File file = new File(System.getProperty("user.home") + System.getProperty("file.separator") + sFileName);
      if (!file.exists()) {
        return false;
      }

      myDocumentManager.clearRecentDocuments();

      // Parse the input
      SAXParser saxParser = factory.newSAXParser();
      saxParser.parse(file, handler);

      loadRoleSets(file);

    } catch (Exception e) {
      if (!GPLogger.log(e)) {
        e.printStackTrace(System.err);
      }
      return false;
    }

    isloaded = true;
    return true;
  }

  private void loadRoleSets(File optionsFile) {
    GanttXMLOpen loader = new GanttXMLOpen(null);

    loader.addTagHandler(new RoleTagHandler(getRoleManager()));
    loader.load(optionsFile);
  }

  private void saveRoleSets(TransformerHandler handler) throws TransformerFactoryConfigurationError, SAXException {
    RoleSet[] roleSets = getRoleManager().getRoleSets();
    for (int i = 0; i < roleSets.length; i++) {
      AttributesImpl attrs = new AttributesImpl();
      attrs.addAttribute("", "roleset-name", "roleset-name", "CDATA", roleSets[i].getName());
      handler.startElement("", "roles", "roles", attrs);
      saveRoles(roleSets[i], handler);
      handler.endElement("", "roles", "roles");
    }
  }

  private void saveRoles(RoleSet roleSet, TransformerHandler handler) throws SAXException {
    Role[] roles = roleSet.getRoles();
    AttributesImpl attrs = new AttributesImpl();
    for (int i = 0; i < roles.length; i++) {
      Role next = roles[i];
      addAttribute("id", "" + next.getID(), attrs);
      addAttribute("name", next.getName(), attrs);
      emptyElement("role", attrs, handler);
    }

  }

  public UIConfiguration getUIConfiguration() {
    if (myUIConfig == null) {
      myUIConfig = new UIConfiguration(new Color(140, 182, 206), redline);
    }
    return myUIConfig;
  }

  private RoleManager getRoleManager() {
    return myRoleManager;
  }

  /**
   * Class to parse the xml option file
   */
  class GanttXMLOptionsParser extends DefaultHandler {

    private PluginOptionsHandler myPluginOptionsHandler;
    private ListOption myContextOption;
    private StringBuilder myCdataBuffer = new StringBuilder();
    private Map<String, BooleanOption> myContextViewOptions = null;

    @Override
    public void startElement(String namespaceURI, String sName, // simple name
                             String qName, // qualified name
                             Attributes attrs) throws SAXException {

      if ("ganttproject-options".equals(qName)) {
        myVersion = attrs.getValue("version");
        return;
      }
      if ("configuration".equals(qName) || "instance".equals(qName)) {
        myPluginOptionsHandler = new PluginOptionsHandler(myPluginPreferencesRootNode);
      }
      if (myPluginOptionsHandler != null) {
        myPluginOptionsHandler.startElement(namespaceURI, sName, qName, attrs);
        return;
      }
      int r = 0, g = 0, b = 0;

      if ("option".equals(qName)) {
        String id = attrs.getValue("id");
        GPOption option;
        if (id.equals(csvOptions.getBomOption().getID())) {
          option = csvOptions.getBomOption();
        } else {
          option = myGPOptions.get(id);
        }
        if (option instanceof ListOption) {
          myContextOption = (ListOption) option;
        } else {
          if (option != null) {
            option.loadPersistentValue(attrs.getValue("value"));
          }
        }
        return;
      }

      if ("view".equals(qName)) {
        String viewId = attrs.getValue("id");
        if ("csv-export".equals(viewId)) {
          myContextViewOptions = csvOptions.getTaskOptions();
          for (BooleanOption o : myContextViewOptions.values()) {
            o.setValue(false);
          }
        }
      }
      if ("resources".equals(qName) && myContextViewOptions != null) {
        myContextViewOptions = csvOptions.getResourceOptions();
        for (BooleanOption o : myContextViewOptions.values()) {
          o.setValue(false);
        }
      }
      if ("field".equals(qName) && myContextViewOptions != null) {
        BooleanOption option = myContextViewOptions.get(attrs.getValue("id"));
        if (option != null) {
          option.setValue(true);
        }
      }
      if (attrs != null) {
        for (int i = 0; i < attrs.getLength(); i++) {
          /** Attribute name */
          String aName = attrs.getQName(i);
          /** Value of attribute */
          String value = attrs.getValue(i);

          String tagDotAttribute = qName + "." + aName;
          GP1XOptionConverter converter = myTagDotAttribute_Converter.get(tagDotAttribute);
          if (converter != null) {
            converter.loadValue(value);
            continue;
          }
          if (qName.equals("task-color")) {
            if (aName.equals("red")) {
              r = new Integer(value).hashCode();
            } else if (aName.equals("green")) {
              g = new Integer(value).hashCode();
            } else if (aName.equals("blue")) {
              b = new Integer(value).hashCode();
            }
          } else if (qName.equals("geometry")) {
            if (aName.equals("x")) {
              x = new Integer(value).hashCode();
            } else if (aName.equals("y")) {
              y = new Integer(value).hashCode();
            } else if (aName.equals("width")) {
              width = new Integer(value).hashCode();
            } else if (aName.equals("height")) {
              height = new Integer(value).hashCode();
            } else if ("maximized".equals(aName)) {
              isMaximized = Boolean.parseBoolean(value);
            }
          } else if (qName.equals("file")) {
            if (aName.equals("path")) {
              myDocumentManager.addToRecentDocuments(value);
            }
          } else if (qName.equals("automatic-launch")) {
            if (aName.equals("value")) {
              automatic = (new Boolean(value)).booleanValue();
            }
          } else if (qName.equals("lockdavminutes")) {
            if (aName.equals("value")) {
              lockDAVMinutes = (new Integer(value)).intValue();
            }
          } else if (qName.equals("xsl-dir")) {
            if (aName.equals("dir")) {
              if (new File(value).exists())
                xslDir = value;
            }
          } else if (qName.equals("xsl-fo")) {
            if (aName.equals("file")) {
              if (new File(value).exists())
                xslFo = value;
            }
          } else if (qName.equals("working-dir")) {
            if (aName.equals("dir")) {
              if (new File(value).exists())
                workingDir = value;
            }
          } else if (qName.equals("toolBar")) {
            if (aName.equals("position"))
              toolBarPosition = (new Integer(value)).intValue();
            else if (aName.equals("icon-size"))
              iconSize = value;
            else if (aName.equals("show"))
              buttonsshow = (new Integer(value)).intValue();
          } else if (qName.equals("statusBar")) {
            if (aName.equals("show"))
              bShowStatusBar = (new Boolean(value)).booleanValue();
          } else if (qName.equals("export")) {
            if (aName.equals("name")) {
              bExportName = (new Boolean(value)).booleanValue();
            } else if (aName.equals("complete")) {
              bExportComplete = (new Boolean(value)).booleanValue();
            } else if (aName.equals("relations")) {
              bExportRelations = (new Boolean(value)).booleanValue();
            } else if (aName.equals("border3d")) {
              bExport3DBorders = (new Boolean(value)).booleanValue();
            }
          } else if (qName.equals("colors")) {
            if (aName.equals("resources")) {
              Color colorR = ColorConvertion.determineColor(value);
              if (colorR != null)
                setResourceColor(colorR);
            } else if (aName.equals("resourcesOverload")) {
              Color colorR = ColorConvertion.determineColor(value);
              if (colorR != null)
                setResourceOverloadColor(colorR);
            } else if (aName.equals("resourcesUnderload")) {
              Color colorR = ColorConvertion.determineColor(value);
              if (colorR != null)
                setResourceUnderloadColor(colorR);
            } else if (aName.equals("weekEnd")) {
              Color colorR = ColorConvertion.determineColor(value);
              if (colorR != null)
                setWeekEndColor(colorR);
            } else if (aName.equals("daysOff")) {
              Color colorD = ColorConvertion.determineColor(value);
              if (colorD != null)
                setDaysOffColor(colorD);
            }
          } else if (qName.equals("csv-general")) {
            if (aName.equals("fixed"))
              csvOptions.bFixedSize = (new Boolean(value)).booleanValue();
            if (aName.equals("separatedChar"))
              csvOptions.sSeparatedChar = value;
            if (aName.equals("separatedTextChar"))
              csvOptions.sSeparatedTextChar = value;
          } else if (qName.equals("csv-tasks")) {
            if (aName.equals("id")) {
              csvOptions.getTaskOptions().get(TaskDefaultColumn.ID.getStub().getID()).setValue(Boolean.valueOf(value));
            } else if (aName.equals("name")) {
              csvOptions.getTaskOptions().get(TaskDefaultColumn.NAME.getStub().getID()).setValue(Boolean.valueOf(value));
            } else if (aName.equals("start-date")) {
              csvOptions.getTaskOptions().get(TaskDefaultColumn.BEGIN_DATE.getStub().getID()).setValue(Boolean.valueOf(value));
            } else if (aName.equals("end-date")) {
              csvOptions.getTaskOptions().get(TaskDefaultColumn.END_DATE.getStub().getID()).setValue(Boolean.valueOf(value));
            } else if (aName.equals("percent")) {
              csvOptions.getTaskOptions().get(TaskDefaultColumn.COMPLETION.getStub().getID()).setValue(Boolean.valueOf(value));
            } else if (aName.equals("duration")) {
              csvOptions.getTaskOptions().get(TaskDefaultColumn.DURATION.getStub().getID()).setValue(Boolean.valueOf(value));
            } else if (aName.equals("color")) {
              csvOptions.getTaskOptions().get(TaskDefaultColumn.COLOR.getStub().getID()).setValue(Boolean.valueOf(value));
            } else if (aName.equals("webLink")) {
              csvOptions.getTaskOptions().get("webLink").setValue(Boolean.valueOf(value));
            } else if (aName.equals("resources")) {
              csvOptions.getTaskOptions().get("resources").setValue(Boolean.valueOf(value));
            } else if (aName.equals("notes")) {
              csvOptions.getTaskOptions().get("notes").setValue(Boolean.valueOf(value));
            }
          } else if (qName.equals("csv-resources")) {
            Boolean bValue = Boolean.valueOf(value);
            switch (aName) {
              case "id":
                csvOptions.getResourceOptions().get("id").setValue(bValue);
                break;
              case "name":
                csvOptions.getResourceOptions().get(ResourceDefaultColumn.NAME.getStub().getID()).setValue(bValue);
                break;
              case "mail":
                csvOptions.getResourceOptions().get(ResourceDefaultColumn.EMAIL.getStub().getID()).setValue(bValue);
                break;
              case "phone":
                csvOptions.getResourceOptions().get(ResourceDefaultColumn.PHONE.getStub().getID()).setValue(bValue);
                break;
              case "role":
                csvOptions.getResourceOptions().get(ResourceDefaultColumn.ROLE.getStub().getID()).setValue(bValue);
                break;
            }
          }
        }
      }
    }

    @Override
    public void endElement(String uri, String localName, String name) throws SAXException {
      if (myPluginOptionsHandler != null) {
        myPluginOptionsHandler.endElement(uri, localName, name);
      }
      if ("configuration".equals(name) || "instance".equals(name)) {
        myPluginOptionsHandler = null;
      }
      if (myContextOption != null) {
        myContextOption.loadPersistentValue(myCdataBuffer.toString());
        myContextOption = null;
        myCdataBuffer = new StringBuilder();
      }
      if ("view".equals(name)) {
        myContextViewOptions = null;
      }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
      if (myContextOption != null) {
        myCdataBuffer.append(ch, start, length);
      }
    }
  }

  /**
   * @return the color for resources.
   */
  public Color getResourceColor() {
    return getUIConfiguration().getResourceColor();
  }

  /**
   * @return the lock DAV Minutes.
   */
  public int getLockDAVMinutes() {
    return lockDAVMinutes;
  }

  /**
   * @return the undo number
   */
  public int getUndoNumber() {
    return undoNumber;
  }

  /**
   * @return the working directory.
   */
  public String getWorkingDir() {
    return workingDir;
  }

  /**
   * @return the xsl directory.
   */
  public String getXslDir() {
    return xslDir;
  }

  /**
   * @return the xsl-fo file.
   */
  public String getXslFo() {
    return (new File(xslFo).exists()) ? xslFo : getClass().getResource("/xslfo/ganttproject.xsl").toString();
  }

  /**
   * @return automatic launch properties box when create a new task.
   */
  public boolean getAutomatic() {
    return automatic;
  }

  /**
   * @return true is options are loaded from the options file.
   */
  public boolean isLoaded() {
    return isloaded;
  }

  /**
   * @return true if show the status bar.
   */
  public boolean getShowStatusBar() {
    return bShowStatusBar;
  }

  /**
   * set show the status bar.
   */
  public void setShowStatusBar(boolean showStatusBar) {
    bShowStatusBar = showStatusBar;
  }

  /**
   * @return the top left x position of the window.
   */
  public int getX() {
    return x;
  }

  /**
   * @return the top left y position of the window.
   */
  public int getY() {
    return y;
  }

  /**
   * @return the width of the window.
   */
  public int getWidth() {
    return width;
  }

  /**
   * @return the height of the window.
   */
  public int getHeight() {
    return height;
  }

  public boolean isMaximized() {
    return this.isMaximized;
  }

  /**
   * @return the csvOptions.
   */
  public CSVOptions getCSVOptions() {
    return csvOptions;
  }

  /**
   * @return the toolbar position.
   */
  public int getToolBarPosition() {
    return toolBarPosition;
  }

  /**
   * @return the size of the icons on the toolbar.
   */
  public String getIconSize() {
    return iconSize;
  }

  /**
   * @return true if you want to export the name of the task on the exported
   * chart.
   */
  public boolean getExportName() {
    return bExportName;
  }

  /**
   * @return true if you want to export the complete percent of the task on the
   * exported chart.
   */
  public boolean getExportComplete() {
    return bExportComplete;
  }

  /**
   * @return true if you want to export the relations of the task on the
   * exported chart.
   */
  public boolean getExportRelations() {
    return bExportRelations;
  }

  /**
   * @return the 3d borders export.
   */
  public boolean getExport3dBorders() {
    return bExport3DBorders;
  }

  public GanttExportSettings getExportSettings() {
    return new GanttExportSettings(bExportName, bExportComplete, bExportRelations, bExport3DBorders);
  }

  public void setExportName(boolean exportName) {
    bExportName = exportName;
  }

  public void setExportComplete(boolean exportComplete) {
    bExportComplete = exportComplete;
  }

  public void setExportRelations(boolean eportRelations) {
    bExportRelations = eportRelations;
  }

  public void setExport3dBorders(boolean borders3d) {
    bExport3DBorders = borders3d;
  }

  /**
   * @return the button show attribute must be GanttOptions.ICONS or
   * GanttOptions.TEXT ir GanttOptions.ICONS_TEXT
   */
  public int getButtonShow() {
    return buttonsshow;
  }

  /**
   * set a new button show value must be GanttOptions.ICONS or GanttOptions.TEXT
   * ir GanttOptions.ICONS_TEXT
   */
  public void setButtonShow(int buttonShow) {
    if (buttonShow != GanttOptions.ICONS && buttonShow != GanttOptions.TEXT && buttonShow != GanttOptions.ICONS_TEXT)
      buttonShow = GanttOptions.ICONS;
    buttonsshow = buttonShow;
  }

  /**
   * Set a new icon size. Must be 16, 24 (or 32 exceptionnally)
   */
  public void setIconSize(String sIconSize) {
    iconSize = sIconSize;
  }

  /**
   * set the toolbar position.
   */
  public void setToolBarPosition(int iToolBarPosition) {
    toolBarPosition = iToolBarPosition;
  }

  /**
   * Set new window position (top left corner)
   */
  public void setWindowPosition(int x, int y) {
    this.x = x;
    this.y = y;
  }

  /**
   * Set new window position (top left corner)
   */
  public void setWindowSize(int width, int height, boolean isMaximized) {
    this.width = width;
    this.height = height;
    this.isMaximized = isMaximized;
  }

  /**
   * Set new working directory value.
   */
  public void setWorkingDirectory(String workingDirectory) {
    workingDir = workingDirectory;
  }

  /**
   * set a new value for web dav locking.
   */
  public void setLockDAVMinutes(int minuteslock) {
    lockDAVMinutes = minuteslock;
  }

  /**
   * set a new value for undo number.
   */
  public void setUndoNumber(int number) {
    undoNumber = number;
  }

  /**
   * set a new default resources color.
   */
  public void setResourceColor(Color color) {
    getUIConfiguration().setResourceColor(color);
  }

  /**
   * set a new resources overload tasks color.
   */
  public void setResourceOverloadColor(Color color) {
    getUIConfiguration().setResourceOverloadColor(color);
  }

  /**
   * set a new resources underload tasks color.
   */
  public void setResourceUnderloadColor(Color color) {
    getUIConfiguration().setResourceUnderloadColor(color);
  }

  /**
   * set a new previous tasks color.
   */
  public void setPreviousTaskColor(Color color) {
    getUIConfiguration().setPreviousTaskColor(color);
  }

  /**
   * set a new earlier previous tasks color.
   */
  public void setEarlierPreviousTaskColor(Color color) {
    getUIConfiguration().setEarlierPreviousTaskColor(color);
  }

  /**
   * set a new later previous tasks color.
   */
  public void setLaterPreviousTaskColor(Color color) {
    getUIConfiguration().setLaterPreviousTaskColor(color);
  }

  /**
   * Set a new week end color.
   */
  public void setWeekEndColor(Color color) {
    getUIConfiguration().setWeekEndColor(color);
  }

  /**
   * Set a new week end color.
   */
  public void setDaysOffColor(Color color) {
    getUIConfiguration().setDayOffColor(color);
  }

  /**
   * Set a new xsl directory for html export.
   */
  public void setXslDir(String xslDir) {
    this.xslDir = xslDir;
  }

  /**
   * Set a new xsl-fo file for pdf export.
   */
  public void setXslFo(String xslFo) {
    this.xslFo = xslFo;
  }

  public void setUIConfiguration(UIConfiguration uiConfiguration) {
    this.myUIConfig = uiConfiguration;
  }

  /**
   * set new automatic launch value.
   */
  public void setAutomatic(boolean automatic) {
    this.automatic = automatic;
  }

  public void setRedline(boolean isOn) {
    this.redline = isOn;
    getUIConfiguration().setRedlineOn(isOn);
  }

  public String getFTPDirectory() {
    return FTPDirectory;
  }

  public String getFTPPwd() {
    return FTPPwd;
  }

  public String getFTPUrl() {
    return FTPUrl;
  }

  public String getFTPUser() {
    return FTPUser;
  }

  public void setFTPDirectory(String pvString) {
    FTPDirectory = pvString;
  }

  public void setFTPPwd(String pvString) {
    FTPPwd = pvString;
  }

  public void setFTPUrl(String pvString) {
    FTPUrl = pvString;
  }

  public void setFTPUser(String pvString) {
    FTPUser = pvString;
  }

  public void addOptionGroups(GPOptionGroup... optionGroups) {
    for (GPOptionGroup group : optionGroups) {
      addOptions(group);
    }
  }

  public void addOptionGroups(Iterable<GPOptionGroup> optionGroups) {
    for (GPOptionGroup group : optionGroups) {
      addOptions(group);
    }
  }

  public void addOptions(GPOptionGroup optionGroup) {
    GPOption[] options = optionGroup.getOptions();
    for (int i = 0; i < options.length; i++) {
      GPOption nextOption = options[i];
      myGPOptions.put(optionGroup.getID() + "." + nextOption.getID(), nextOption);
      if (nextOption instanceof GP1XOptionConverter) {
        GP1XOptionConverter nextConverter = (GP1XOptionConverter) nextOption;
        myTagDotAttribute_Converter.put(nextConverter.getTagName() + "." + nextConverter.getAttributeName(),
            nextConverter);
      }
    }
  }

  public String getVersion() {
    return myVersion;
  }
}
