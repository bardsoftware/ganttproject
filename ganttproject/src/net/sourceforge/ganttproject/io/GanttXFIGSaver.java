/***************************************************************************
 GanttXFIGSaver.java
 -------------------
 begin                : 15 juin 2004
 copyright            : (C) 2004 by Thomas Alexandre
 email                : alexthomas(at)ganttproject.org
 ***************************************************************************/

/***************************************************************************
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *                                                                         *
 ***************************************************************************/

package net.sourceforge.ganttproject.io;

import java.awt.Color;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.swing.tree.DefaultMutableTreeNode;

import net.sourceforge.ganttproject.GanttCalendar;
import net.sourceforge.ganttproject.GanttTask;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskManager;

/**
 * @author athomas This class is based on the octave program of A S Hodel
 *         (hodelas(at)ieee.org) from Auburn University check the URL at
 *         http://www.eng.auburn.edu/users/hodelas/LaTeX/ganttProject_mn.html It
 *         allow to export a project into an xfig scalable vector image.
 */
public class GanttXFIGSaver {

    // fields
    // private GanttTree tree;
    // private GanttGraphicArea area;
    // private PrjInfos prjInfos;

    // TODO lot is used as a list of DefaultMutableTreeNodes and as a list of Tasks, seems inconsistent in this class...
    List lot = new ArrayList(); // list of tasks

    // ArrayList lots = new ArrayList();
    ArrayList<Color> loc = new ArrayList<Color>(); // list of colors

    ArrayList atl = new ArrayList(); // list of text object

    ArrayList abl = new ArrayList(); // list of box object

    // store the start of the project
    GanttCalendar dateShift = new GanttCalendar();

    float scale = 0, chwidth = 0;

    // width of the text
    float fTtextwidth = 0.0f;

    // debug value to print some text on standrad output..
    boolean debug = true;

    private TaskManager myTaskManager;

    /** Constructor. */
    public GanttXFIGSaver(IGanttProject project) {
        // this.area = area;
        // this.prjInfos = prjInfos;
        myTaskManager = project.getTaskManager();
    }

    /** Save the project as XFIG on a stream */
    public void save(OutputStream stream) {
        try {
            OutputStreamWriter fout = new OutputStreamWriter(stream);
            beginToSave(fout);
            fout.close(); // close the stream
        } catch (IOException e) {
            System.out.println(e);
            System.out.println("Error in saving the xfig file");
        }
    }

    /** Start saving the xfig image. */
    public void beginToSave(OutputStreamWriter fout) throws IOException {
        if (debug)
            System.out.println("beginToSave begin");

        float depthval = 50; // depth level - 999 is the lowest level.

        // targetWidthPoints is in pixels, 1200 pixels per inch,
        // or about 472 pixels/cm
        float targetWidthPoints = 1200;

        // get the list of tasks on an array list
        // lot = tree.getAllTasks();
        lot = Arrays.asList(myTaskManager.getTasks());
        fTtextwidth = getProjectTextWidth();
        if (debug)
            System.out.println("Text Size : " + fTtextwidth);

        // write the xfig header
        xfigheader(fout);

        // Search for the user colors
        searchUserColor();

        // Need to give user control of date range created in plot
        setProjectPlotTimes(targetWidthPoints, 50);

        // Write the list of tasks
        drawTasks(fout);

        // Write the axes
        labelAxes(fout);

        if (debug)
            System.out.println("beginToSave end");
    }

    /**
     * @return the with of the text. Determine width of text labels (task names)
     *         in this project assume fixed width, 14 letters to the inch, 1200
     * ppi each level is indented 1/4 inch (300 points @ 1200ppi)
     */
    public float getProjectTextWidth() {
        if (debug)
            System.out.println("getProjectTextWidth begin");

        float res = 0.0f;
        for (Iterator<Task> it = lot.iterator(); it.hasNext();) {
            // get the name of the task
            Task task = it.next();
            float taskTextWidth = getTaskTextWidth(task);
            if (taskTextWidth > res) {
                res = taskTextWidth;
            }
        }
        if (debug)
            System.out.println("getProjectTextWidth end");
        return res;
    }

    /**
     * @return the textwith of this task for and its children tasks. Determine
     *         width of text labels (task names) in this task assume fixed
     *         width, 14 letters to the inch, 1200 ppi each level is indented
     * 1/4 inch (300 points @ 1200ppi)
     */
    private float getTaskTextWidth(Task task) {
        if (debug)
            System.out.println("getTaskTextWidth begin");
        Task t = task;
        float res = (float) t.getName().length() * (1.0f / 14.0f) * (1200.0f);

        Task[] children = task.getManager().getTaskHierarchy().getNestedTasks(
                task);

        for (int i = 0; i < children.length; i++) {
            float subTaskTextWidth = 0.25f + getTaskTextWidth((children[i]));
            if (subTaskTextWidth > res)
                res = subTaskTextWidth;
        }
        if (debug)
            System.out.println("getTaskTextWidth end");
        return res;
    }

    /**
     * Write the header of the XFIG FILE Based on the xfig file specification.
     */
    public void xfigheader(OutputStreamWriter fout) throws IOException {
        if (debug)
            System.out.println("xfigheader begin");
        fout.write("#FIG 3.2\n"); // version
        fout.write("Landscape\n"); // orientation
        fout.write("Center\n"); // justification
        fout.write("Inches\n"); // units
        fout.write("Letter\n"); // papersize
        fout.write("100.0\n"); // magnification
        fout.write("Single\n"); // multiplePage
        fout.write("-2\n"); // transparentColor
        fout.write("1200 2\n"); // resolutionPpi origin
        if (debug)
            System.out.println("xfigheader end");
    }

    /** Search for the corresponding colors. */
    public void searchUserColor() // TODO continue to write this method
    {
        if (debug)
            System.out.println("searchUserColor begin");
        loc.clear(); // clear the list

        for (Iterator it = lot.iterator(); it.hasNext();) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) it.next();
            if (!node.isRoot()) {
                GanttTask task = (GanttTask) (node.getUserObject());
                Color color = task.getColor();
                String hexaColor = getHexaColor(color);
            }
        }
        if (debug)
            System.out.println("searchUserColor end");
    }

    /**
     * get project time range, compute box scaling values and shifts so that the
     * total chart fits in targetWidthPoints units: targetWidthPoints is in
     * pixels, 1200 pixels per inch, or about 472 pixels/cm this also adds box
     * and text objects to the project tasks for plotting later
     */
    public void setProjectPlotTimes(float targetWidthPoints, float depthval) {
        if (debug)
            System.out.println("setProjectPlotTimes begin");
        float targetwidth = targetWidthPoints - fTtextwidth;

        GanttCalendar startDate = null, endDate = null;

        // Get project start, end times
        for (Iterator it = lot.iterator(); it.hasNext();) {
            // get the task
            Task task = (Task) (((DefaultMutableTreeNode) it.next())
                    .getUserObject());
            if (startDate == null && endDate == null) {
                startDate = task.getStart();
                endDate = task.getEnd();
            } else {
                if (task.getStart().compareTo(startDate) == -1) // before
                    startDate = task.getStart();
                if (task.getEnd().compareTo(startDate) == 1) // after
                    endDate = task.getEnd();
            }
        }
        // shift all dates by this amount to plot
        dateShift = startDate;

        scale = targetwidth / Math.max(1.0f, (float) (endDate.diff(startDate)));
        chwidth = scale * (float) (endDate.diff(startDate));

        System.out.println("Chart width =" + chwidth + " points = "
                + (chwidth / 1200) + " inches\n");

        int index = 0;

        // now add text and box objects to the tasks
        for (Iterator it = lot.iterator(); it.hasNext();) {
            DefaultMutableTreeNode node = ((DefaultMutableTreeNode) it.next());
            if (!node.isRoot()) {
                // get the task
                GanttTask task = (GanttTask) node.getUserObject();

                // get the text infos of the task
                TextObject textLabel = task2text(task, index, node.getLevel(),
                        (int) depthval);
                if (debug)
                    System.out.println("    add TEXT");
                atl.add(textLabel);

                // get the box object of the task
                BoxObject boxObject = task2box(task, (int) depthval, index,
                        node.isLeaf());
                if (debug)
                    System.out.println("    add BOX");
                abl.add(boxObject);

                index++;
            }
        }
        if (debug)
            System.out.println("setProjectPlotTimes end + index=" + index);
    }

    /** convert task structureto an xfig text data structure */
    public TextObject task2text(GanttTask task, int number, int level,
            int depthval) {
        if (debug)
            System.out.println("task2text begin");

        /*
         * System.out.println("task : "+task+ " number : "+number+ " level :
         * "+level+ " depthval : "+depthval);
         */

        TextObject taskText = new TextObject();
        taskText.sub_type = 0; // left justified
        taskText.color = 0; // black
        taskText.depth = depthval;
        taskText.pen_style = 0; // unused
        taskText.font = 0; // Times Roman
        taskText.font_size = 10; // fonts size in points
        taskText.angle = 0; // text angle in radians
        taskText.font_flags = 4; // not rigid, not special, postscript, not
        // hidden.
        taskText.height = 0.25f * 1200f;
        taskText.length = 0.125f * (float) (task.getName().length()) * 1200.0f;
        taskText.y = (int) (1200f * 0.25f * (float) number + 5.0f - 1200.0f / 16.0f);
        taskText.x = (int) (1200.0f * 0.25f * ((float) level + 1.0f));
        taskText.str = task.getName();
        if (debug)
            System.out.println("task2text end");
        return taskText;
    }

    /** get the task box. */
    public BoxObject task2box(GanttTask task, int depthval, int number,
            boolean isLeaf) {
        // boxObject: total box
        // boxObjectPct: black line in center of box object
        // min box width: 1.

        // object to return
        BoxObject boxObject = new BoxObject();

        if (isLeaf) {
            boxObject.sub_type = 2; // box
            boxObject.npoints = 5;
        } else {
            boxObject.sub_type = 3; // polygon
            boxObject.npoints = 7;
        }

        boxObject.depth = depthval;

        if (!isLeaf)
            boxObject.depth -= 2; // ?????
        // See BoxObject class for other default values

        // Construct box corners
        float boxLen = Math.max(1.0f, (task.getLength()) * scale);
        float boxLenPct = Math.max(1.0f, boxLen
                * task.getCompletionPercentage() / 100.0f);
        float boxHeight = 1200.0f / 8.0f;
        float pLo = boxHeight * 0.25f;
        float pHi = boxHeight * 0.75f;
        float[] pointsPct = { 0, pLo, boxLenPct, pLo, boxLenPct, pHi, 0, pHi,
                0, pLo };

        if (boxObject.sub_type == 2) {
            boxObject.points = new float[10];
            boxObject.points[0] = 0;
            boxObject.points[1] = 0;
            boxObject.points[2] = boxLen;
            boxObject.points[3] = 0;
            boxObject.points[4] = boxLen;
            boxObject.points[5] = boxHeight;
            boxObject.points[6] = 0;
            boxObject.points[7] = boxHeight;
            boxObject.points[8] = 0;
            boxObject.points[9] = 0;
        } else {
            float boxLo = boxHeight / 4;
            boxObject.points = new float[14];
            boxObject.points[0] = 0;
            boxObject.points[1] = boxHeight;
            boxObject.points[2] = 0;
            boxObject.points[3] = 0;
            boxObject.points[4] = boxLen;
            boxObject.points[5] = 0;
            boxObject.points[6] = boxLen;
            boxObject.points[7] = boxHeight; // top bow
            boxObject.points[8] = boxLen;
            boxObject.points[9] = boxLo;
            boxObject.points[10] = 0;
            boxObject.points[11] = boxLo;
            boxObject.points[10] = 0;
            boxObject.points[13] = boxHeight;
        }
        int blen = boxObject.points.length;

        // shift to this row
        float xShift = 0;
        float yShift = 1200.0f * 0.25f * ((float) number + 4) + 1200.0f / 16.0f;

        /*
         * boxObject.points(2:2:blen) += yShift; boxObject.points(1:2:blen) +=
         * xShift;
         */
        for (int i = 1; i < blen; i += 2)
            boxObject.points[i] += yShift;
        for (int i = 0; i < blen; i += 2)
            boxObject.points[i] += xShift;
        // pointsPct(2:2:10) += yShift;
        // pointsPct(1:2:9) += xShift;

        // boxObjectPct = boxObject; // percent completed
        // boxObjectPct.points = pointsPct;
        // boxObjectPct.npoints = 5;
        // boxObjectPct.area_fill = 20;
        // boxObjectPct.fill_color = 0;

        // shift boxes by start date
        /* boxObject.points(1:2:blen) += task.start*boxScale; */
        for (int i = 0; i < blen; i += 2)
            boxObject.points[i] += task.getStart().diff(dateShift) * scale;
        // boxObjectPct.points(1:2:9) += task.start*boxScale;

        // now shift again by text width (must be calculated and set in
        // task.boxindent)
        /* boxObject.points(1:2:blen) += task.boxindent; */
        for (int i = 0; i < blen; i += 2)
            boxObject.points[i] += fTtextwidth + 120;
        // boxObjectPct.points(1:2:9) += task.boxindent;
        // boxObjectPct.depth--;

        // fix angle brackets in polygon object
        /*
         * if(boxObject.sub_type == 3) { angwid = min(60,boxLen/2);
         * boxObject.points([9,11]) += [-angwid,angwid]; }
         */
        if (boxObject.sub_type == 3) {
            float angwid = Math.min(60, boxLen / 2);
            boxObject.points[9] += -angwid;
            boxObject.points[11] += angwid;
        }

        return boxObject;
    }

    /** Draw the list of tasks. */
    public void drawTasks(OutputStreamWriter fout) throws IOException {
        try {
            if (debug)
                System.out.println("drawTasks begin");

            // loop on tasks
            int i = 0;
            for (Iterator it = lot.iterator(); it.hasNext();) {
                DefaultMutableTreeNode node = ((DefaultMutableTreeNode) it
                        .next());
                // get the task
                if (!node.isRoot()) {
                    GanttTask task = (GanttTask) (node.getUserObject());

                    TextObject txtObj = (TextObject) (atl.get(i));
                    BoxObject boxObject = (BoxObject) (abl.get(i));

                    // print the text of the task
                    drawtext(fout, txtObj);

                    // print the box of the task
                    drawbox(fout, boxObject);

                    // print the percent complete

                    // print relations

                    // update index
                    i++;
                }
            }
            if (debug)
                System.out.println("drawTasks end");
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    /** Draw text for the taskObject. */
    public void drawtext(OutputStreamWriter fout, TextObject txtObj)
            throws IOException {
        if (debug)
            System.out.println("drawtext begin");
        fout.write("4 ");
        fout.write(txtObj.sub_type + " ");
        fout.write(txtObj.color + " ");
        fout.write(txtObj.depth + " ");
        fout.write(txtObj.pen_style + " ");
        fout.write(txtObj.font + " ");
        fout.write(txtObj.font_size + " ");
        fout.write(txtObj.angle + " ");
        fout.write(txtObj.font_flags + " ");
        fout.write(txtObj.height + " " + txtObj.length + " ");
        fout.write(txtObj.x + " " + txtObj.y + " ");
        fout.write(txtObj.str + "\\001\n");
        if (debug)
            System.out.println("drawtext end");
    }

    /** draw the box of the task. */
    public void drawbox(OutputStreamWriter fout, BoxObject boxObject)
            throws IOException {
        if (debug)
            System.out.println("drawbox begin");
        // draw an xfig box object;
        fout.write("2 ");
        fout.write(boxObject.sub_type + " ");
        fout.write(boxObject.line_style + " ");
        fout.write(boxObject.thickness + " ");
        fout.write(boxObject.pen_color + " ");
        fout.write(boxObject.fill_color + " ");
        fout.write(boxObject.depth + " ");
        fout.write(boxObject.pen_style + " ");
        fout.write(boxObject.area_fill + " ");
        fout.write(" " + boxObject.style_val + "  ");
        fout.write(boxObject.join_style + " ");
        fout.write(boxObject.cap_style + " ");
        fout.write(boxObject.radius + " ");
        fout.write(boxObject.forward_arrow + " ");
        fout.write(boxObject.backward_arrow + " ");
        fout.write(boxObject.npoints + "\n\t");

        int arrow_type = 1;
        int arrow_style = 1;
        float arrow_thickness = 1.00f;
        float arrow_width = 60.0f;
        float arrow_height = 60.0f;

        if (boxObject.sub_type == 3) {
            if (boxObject.forward_arrow != 0) {
                System.out.println("forward");
                fout.write("      " + (int) arrow_type + " "
                        + (int) arrow_style + " " + (int) arrow_thickness
                        + (int) arrow_width + " " + (int) arrow_height + "\n");
            }
            if (boxObject.backward_arrow != 0) {
                fout.write("      " + (int) arrow_type + " "
                        + (int) arrow_style + " " + (int) arrow_thickness
                        + (int) arrow_width + " " + (int) arrow_height + "\n");
                System.out.println("back");
            }
        }

        for (int i = 0; i < boxObject.points.length; i++)
            fout.write((int) (boxObject.points[i]) + " ");

        fout.write("\n");
        if (debug)
            System.out.println("drawbox end");
    }

    /** Draw the axes. */
    public void labelAxes(OutputStreamWriter fout) {
        // TODO write this method
        if (debug)
            System.out.println("labelAxes begin");
        if (debug)
            System.out.println("labelAxes end");
    }

    /** @return a color as a string like #00FF00 for green color. */
    private String getHexaColor(Color color) {
        if (debug)
            System.out.println("getHexaColor begin");
        String sColor = "#"; // result string

        if (color.getRed() <= 15)
            sColor += "0";
        sColor += Integer.toHexString(color.getRed());
        if (color.getGreen() <= 15)
            sColor += "0";
        sColor += Integer.toHexString(color.getGreen());
        if (color.getBlue() <= 15)
            sColor += "0";
        sColor += Integer.toHexString(color.getBlue());

        if (debug)
            System.out.println("getHexaColor end");
        return sColor;
    }

    /** Class to store text informations. */
    private class TextObject {
        int sub_type;

        int color;

        int depth;

        int pen_style;

        int font;

        float font_size;

        float angle;

        int font_flags;

        float height;

        float length;

        int x;

        int y;

        String str = "";
    }

    /** Class to store box informations. */
    private class BoxObject {
        int code = 2; // polyline always

        int npoints;

        int sub_type;

        int fill_color = 1; // blue color by default

        /*
         * case ("Black"), boxObject.fill_color = 0; case ("Blue"),
         * boxObject.fill_color = 1; case ("Green"), boxObject.fill_color = 2;
         * case ("Cyan"), boxObject.fill_color = 3; case ("Red"),
         * boxObject.fill_color = 4; case ("Magenta"), boxObject.fill_color = 5;
         * case ("Yellow"), boxObject.fill_color = 6; case ("White"),
         * boxObject.fill_color = 7;
         */

        int line_style = 0;

        int thickness = 1;

        int pen_color = 0;

        int depth;

        int pen_style = 0;

        int area_fill = 20;

        int style_val = 0;

        int join_style = 0;

        int cap_style = 0;

        int radius = 0;

        int forward_arrow = 0;

        int backward_arrow = 0;

        float[] points;
    }
}
