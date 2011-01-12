/*
GanttProject is an opensource project management tool.
Copyright (C) 2002-2010 Dmitry Barashev

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
package net.sourceforge.ganttproject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Enumeration;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import net.sourceforge.ganttproject.parser.PreviousStateTasksTagHandler;
import net.sourceforge.ganttproject.task.Task;

import org.xml.sax.SAXException;

/**
 * @author nbohn
 */
public class GanttPreviousState {
    private final String myName;

    private final File myFile;

    private final OutputStreamWriter os;

    private final GanttTree2 myTree;

    /** String used to indent */
    private final String s = "    ";

    /** constructor for a new previous state */
    public GanttPreviousState(String name, GanttProject project)
            throws IOException {
        myName = name;
        myTree = project.getTree();
        myFile = createTemporaryFile();
        myFile.deleteOnExit();
        os = new OutputStreamWriter(new FileOutputStream(myFile), "UTF-8");

        saveFile();
    }

    /** constructor for a loaded previous state */
    public GanttPreviousState(String name) throws IOException {
        myName = name;
        myTree = null;
        myFile = createTemporaryFile();
        myFile.deleteOnExit();
        os = new OutputStreamWriter(new FileOutputStream(myFile), "UTF-8");
    }

    public void saveFile() {
        try {
            StreamResult result = new StreamResult(os);
            SAXTransformerFactory factory = (SAXTransformerFactory) SAXTransformerFactory
                    .newInstance();
            TransformerHandler handler = factory.newTransformerHandler();
            Transformer serializer = handler.getTransformer();
            serializer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            serializer.setOutputProperty(OutputKeys.INDENT, "yes");
            serializer.setOutputProperty(
                    "{http://xml.apache.org/xslt}indent-amount", "4");
            handler.setResult(result);
            handler.startDocument();

            handler.endDocument();
            writeTasks();
            os.close();
        } catch (Throwable e) {
        	if (!GPLogger.log(e)) {
        		e.printStackTrace(System.err);
        	}
        }
    }

    private void writeTasks() {
        assert myTree != null : "myTree is not initialised, used wrong constructor to create this object!";

        Enumeration children = ((DefaultMutableTreeNode) myTree.getJTree()
                .getModel().getRoot()).children();
        write(s + "<previous-tasks name=\"" + myName + "\">\n");

        while (children.hasMoreElements()) {
            DefaultMutableTreeNode element = (DefaultMutableTreeNode) children
                    .nextElement();
            writeTask(os, /* lot.indexOf(element) */element);
        }
        write(s + "</previous-tasks>");
    }

    public void saveFilesFromLoaded(ArrayList<GanttPreviousStateTask> tasks) {
        try {
            StreamResult result = new StreamResult(os);
            SAXTransformerFactory factory = (SAXTransformerFactory) SAXTransformerFactory
                    .newInstance();
            TransformerHandler handler = factory.newTransformerHandler();
            Transformer serializer = handler.getTransformer();
            serializer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            serializer.setOutputProperty(OutputKeys.INDENT, "yes");
            serializer.setOutputProperty(
                    "{http://xml.apache.org/xslt}indent-amount", "4");
            handler.setResult(result);
            handler.startDocument();

            handler.endDocument();
            writeTasksFromLoaded(tasks);
            os.close();
        } catch (Throwable e) {
        	if (!GPLogger.log(e)) {
        		e.printStackTrace(System.err);
        	}
        }
    }

    public void writeTasksFromLoaded(ArrayList<GanttPreviousStateTask> tasks) throws IOException {
        write(s + "<previous-tasks name=\"" + myName + "\">\n");
        for (int i = 0; i < tasks.size(); i++) {
            os.write(s + s + "<previous-task id=\""
                    + tasks.get(i).getId() + "\"");
            os.write(" start=\""
                    + tasks.get(i).getStart()
                            .toXMLString() + "\"");
            os.write(" duration=\""
                    + tasks.get(i).getDuration()
                    + "\"");
            os.write(" meeting=\""
                    + tasks.get(i).isMilestone()
                    + "\"");
            os.write(" super=\""
                    + tasks.get(i).hasNested()
                    + "\"");
            os.write("/>\n");
        }
        write(s + "</previous-tasks>");
    }

    public void write(String s) {
        try {
            os.write(s);
        } catch (IOException e) {
        	if (!GPLogger.log(e)) {
        		e.printStackTrace(System.err);
        	}
        }
    }

    private File createTemporaryFile() throws IOException {
        String fileName = "_GanttProject_ps_" + (int) (10000. * Math.random());
        return File.createTempFile(fileName, ".gan");
    }

    public String getName() {
        return myName;
    }

    public File getFile() {
        return myFile;
    }

    public void remove() {
        myFile.delete();
    }

    /** Simple write information of tasks */
    public void writeTask(Writer fout, DefaultMutableTreeNode node) {
        assert myTree != null : "myTree is not initialised, used wrong constructor to create this object!";

        /** List of tasks */
        ArrayList<Task> lot = new ArrayList<Task>();
        try {
            GanttTask task = (GanttTask) node.getUserObject();

            if (task.getTaskID() == -1)
                throw new RuntimeException(
                        "A task can not has a number equal to -1");

            /*
             * int id = task.getTaskID();
             * if (id >= lot.size()) { return; }
             */

            boolean haschild = false;

            ArrayList<Object> child = myTree.getAllChildTask(node);
            if (child.size() != 0) {
                haschild = true;
            }

            // Writes data of task
            fout.write(s + s + "<previous-task id=\"" + task.getTaskID() + // lots.indexOf(task.toString())
                    // +
                    // //By
                    // CL
                    "\"");
            fout.write(" start=\"" + task.getStart().toXMLString() + "\"");
            fout.write(" duration=\"" + task.getLength() + "\"");
            fout.write(" meeting=\"" + task.isMilestone() + "\"");
            fout.write(" super=\"" + haschild + "\"");

            fout.write("/>\n");

            // TODO What is up with newid? It is never read, what is its purpose?
            // Write the child of the task
            if (haschild) {
                for (int i = 0; i < child.size(); i++) {
                    Task task2 = (Task) ((DefaultMutableTreeNode) child.get(i))
                            .getUserObject();
                    int newid = -1; // lot.lastIndexOf(task2);

                    for (int j = 0; j < lot.size(); j++) {
                        String a = task2.toString();
                        if (a == null)
                            System.out.println("nul");
                        String b = lot.get(j).toString();

                        if (a.equals(b)) {
                            newid = j;
                        }
                    }
                    writeTask(fout, (DefaultMutableTreeNode) child.get(i));
                }
            }

            // end of task section

        } catch (Exception e) {
        	if (!GPLogger.log(e)) {
        		e.printStackTrace(System.err);
        	}
        }
    }

    /** Correct the characters to be compatible with xml format */
    public String correct(String s) {
        String res;
        if (s != null) {
            res = s.replaceAll("&", "&#38;");
            res = res.replaceAll("<", "&#60;");
            res = res.replaceAll(">", "&#62;");
            res = res.replaceAll("/", "&#47;");
            res = res.replaceAll("\"", "&#34;");
        } else
            res = s;
        return res;
    }

    public String replaceAll(String notes, String s1, String s2) {
        return notes.replaceAll(s1, s2);
    }

    public ArrayList<GanttPreviousStateTask> load() throws ParserConfigurationException, SAXException, IOException {
        ArrayList<GanttPreviousStateTask> tasks = null;
        PreviousStateTasksTagHandler handler = new PreviousStateTasksTagHandler(
                null);
        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser saxParser = factory.newSAXParser();
        saxParser.parse(myFile, handler);
        tasks = handler.getTasks();
        return tasks;
    }
}
