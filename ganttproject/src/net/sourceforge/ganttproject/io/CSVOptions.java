/***************************************************************************
 CSVOptions.java
 ------------------------------------------
 begin                : 7 juil. 2004
 copyright            : (C) 2004 by Thomas Alexandre
 email                : alexthomas@ganttproject.org
 ***************************************************************************/

/***************************************************************************
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 3 of the License, or     *
 *   (at your option) any later version.                                   *
 *                                                                         *
 ***************************************************************************/
package net.sourceforge.ganttproject.io;

/**
 * @author athomas Settings for exporting in csv format
 */
public class CSVOptions {
    public boolean bExportTaskID = true;

    public boolean bExportTaskName = true;

    public boolean bExportTaskStartDate = true;

    public boolean bExportTaskEndDate = true;

    public boolean bExportTaskPercent = true;

    public boolean bExportTaskDuration = true;

    public boolean bExportTaskWebLink = true;

    public boolean bExportTaskResources = true;

    public boolean bExportTaskNotes = true;

    public boolean bExportResourceID = true;

    public boolean bExportResourceName = true;

    public boolean bExportResourceMail = true;

    public boolean bExportResourcePhone = true;

    public boolean bExportResourceRole = true;

    public boolean bFixedSize = false;

    public String sSeparatedChar = ",";

    public String sSeparatedTextChar = "\"";

    /** @return the csv settings as an xml schema. */
    public String getXml() {
        String res = "    <csv-export>\n";
        // general options
        res += "      <csv-general \n";
        res += "        fixed=\"" + bFixedSize + "\"\n";
        res += "        separatedChar=\"" + correct(sSeparatedChar) + "\"\n";
        res += "        separatedTextChar=\"" + correct(sSeparatedTextChar)
                + "\"/>\n";

        // tasks export options
        res += "      <csv-tasks\n";
        res += "        id=\"" + bExportTaskID + "\"\n";
        res += "        name=\"" + bExportTaskName + "\"\n";
        res += "        start-date=\"" + bExportTaskStartDate + "\"\n";
        res += "        end-date=\"" + bExportTaskEndDate + "\"\n";
        res += "        percent=\"" + bExportTaskPercent + "\"\n";
        res += "        duration=\"" + bExportTaskDuration + "\"\n";
        res += "        webLink=\"" + bExportTaskWebLink + "\"\n";
        res += "        resources=\"" + bExportTaskResources + "\"\n";
        res += "        notes=\"" + bExportTaskNotes + "\"/>\n";

        // resources export options
        res += "      <csv-resources\n";
        res += "        id=\"" + bExportResourceID + "\"\n";
        res += "        name=\"" + bExportResourceName + "\"\n";
        res += "        mail=\"" + bExportResourceMail + "\"\n";
        res += "        phone=\"" + bExportResourcePhone + "\"\n";
        res += "        role=\"" + bExportResourceRole + "\"/>\n";

        return res += "    </csv-export>\n";
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

    /** @return a list of the possible separated char. */
    public String[] getSeparatedTextChars() {
        String[] charText = { "   \'   ", "   \"   " };
        return charText;
    }
}
