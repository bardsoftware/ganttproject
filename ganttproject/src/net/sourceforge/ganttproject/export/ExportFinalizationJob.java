/*
 * Created on 04.11.2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package net.sourceforge.ganttproject.export;

import java.io.File;

public interface ExportFinalizationJob {
	void run(File[] exportedFiles);
}
