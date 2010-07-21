/*
 * Created on 04.12.2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.ganttproject.impex.htmlpdf;

import org.eclipse.core.runtime.jobs.Job;

abstract class ExportJob extends Job {
	ExportJob(String name) {
		super(name);
	}
	public boolean belongsTo(Object family) {
		return ExporterBase.EXPORT_JOB_FAMILY.equals(family);
	}
	
}
