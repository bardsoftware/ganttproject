/*
GanttProject is an opensource project management tool. License: GPL3
Copyright (C) 2010 Dmitry Barashev

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
package biz.ganttproject.impex.msproject2;

import biz.ganttproject.core.table.ColumnList;
import com.google.common.collect.Lists;
import net.sf.mpxj.*;
import net.sf.mpxj.mpp.MPPReader;
import net.sf.mpxj.mpx.MPXReader;
import net.sf.mpxj.mspdi.MSPDIReader;
import net.sf.mpxj.reader.ProjectReader;
import net.sourceforge.ganttproject.*;
import net.sourceforge.ganttproject.util.collect.Pair;
import org.w3c.util.DateParser;

import javax.xml.transform.*;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.time.LocalDate;
import java.util.*;
import java.util.logging.Level;

class ProjectFileImporter {
  private final IGanttProject myNativeProject;
  private final ProjectReader myReader;
  private final File myForeignFile;
  private final ColumnList myTaskFields;
  private final List<Pair<Level, String>> myErrors = Lists.newArrayList();
  private final Map<GanttTask, Date> myNativeTask2foreignStart = new HashMap<>();
  private boolean myPatchMspdi = true;
  static Date toJavaDate(LocalDate localDate) {
    return DateParser.toJavaDate(localDate);
  }

  private static ProjectReader createReader(File file) {
    int lastDot = file.getName().lastIndexOf('.');
    if (lastDot == file.getName().length() - 1) {
      return null;
    }
    String fileExt = file.getName().substring(lastDot + 1).toLowerCase();
    return switch (fileExt) {
      case "mpp" -> new MPPReader();
      case "xml" -> new MSPDIReader();
      case "mpx" -> new MPXReader();
      default -> null;
    };
  }

  interface HolidayAdder {
    void addHoliday(Date date, Optional<String> title);
    void addYearlyHoliday(Date date, Optional<String> title);
  }

  public ProjectFileImporter(IGanttProject nativeProject, ColumnList taskFields, File foreignProjectFile) {
    myNativeProject = nativeProject;
    myTaskFields = taskFields;
    myReader = ProjectFileImporter.createReader(foreignProjectFile);
    myForeignFile = foreignProjectFile;
  }

  private static InputStream createPatchedStream(final File inputFile) throws TransformerConfigurationException,
      TransformerFactoryConfigurationError, IOException {
    final Transformer transformer = SAXTransformerFactory.newInstance().newTransformer(
        new StreamSource(ProjectFileImporter.class.getResourceAsStream("/mspdi_fix.xsl")));
    transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

    ByteArrayOutputStream transformationOut = new ByteArrayOutputStream();
    try {
      transformer.transform(new StreamSource(inputFile), new StreamResult(transformationOut));
    } catch (TransformerException e) {
      GPLogger.log(new RuntimeException("Failed to transform file=" + inputFile.getAbsolutePath(), e));
    }

    return new ByteArrayInputStream(transformationOut.toByteArray());
  }

  @SuppressWarnings("unused")
  private List<String> debugTransformation() {
    try {
      BufferedReader is = new BufferedReader(new InputStreamReader(createPatchedStream(myForeignFile)));
      for (String s = is.readLine(); s != null; s = is.readLine()) {
        System.out.println(s);
      }
    } catch (Throwable e) {
      e.printStackTrace();
    }
    return Collections.emptyList();
  }

  void setPatchMspdi(boolean enabled) {
    myPatchMspdi = enabled;
  }

  public void run() throws MPXJException {
    ProjectFile pf;
    try {
      pf = (myReader instanceof MSPDIReader && myPatchMspdi) ? myReader.read(createPatchedStream(myForeignFile))
          : myReader.read(myForeignFile);
    } catch (TransformerConfigurationException e) {
      throw new MPXJException("Failed to read input file=" + myForeignFile.getAbsolutePath() + "<br>" + e.getMessage(),
          e);
    } catch (TransformerFactoryConfigurationError e) {
      throw new MPXJException("Failed to create a transformer factory");
    } catch (IOException | RuntimeException e) {
      throw new MPXJException("Failed to read input file=" + myForeignFile.getAbsolutePath(), e);
    }
    var result = new ProjectFileImporterImpl(pf, myNativeProject).run();
    result.getSingleValueCustomPropertyNames().forEach(this::hideCustomColumn);
    myNativeTask2foreignStart.putAll(result.getOriginalStartDates());
  }

  List<Pair<Level, String>> getErrors() {
    return myErrors;
  }

  Map<GanttTask, Date> getOriginalStartDates() {
    return myNativeTask2foreignStart;
  }

  private void hideCustomColumn(String key) {
    for (int i = 0; i < myTaskFields.getSize(); i++) {
      if (key.equals(myTaskFields.getField(i).getName())) {
        myTaskFields.getField(i).setVisible(false);
      }
    }
  }

  // These two methods are workarounds for the problem with having both name, name() and getName() fields
  // in Java type. Kotlin is having issues compiling such code.
  static MpxjTaskField convert(TaskField tf) {
    return new MpxjTaskField(tf.getName(), tf.getDataType());
  }

  static String getName(FieldType fieldType) {
    return fieldType.getName();
  }
}
