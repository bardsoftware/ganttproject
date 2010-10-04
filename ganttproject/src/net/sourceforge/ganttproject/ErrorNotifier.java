/*
 * Created on 13.11.2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package net.sourceforge.ganttproject;

import java.util.ArrayList;
import java.util.List;

class ErrorNotifier implements Runnable {
	private final List<String> myErrors = new ArrayList<String>();
	private final UIFacadeImpl myUIFacade;

	ErrorNotifier(UIFacadeImpl uiFacade) {
		myUIFacade = uiFacade;
	}
	void add(Throwable e) {
		myErrors.add(e.getMessage());
	}

	public void run() {
		StringBuffer buf = new StringBuffer();
		for (int i=0; i<myErrors.size(); i++) {
			buf.append(String.valueOf(myErrors.get(i)));
			buf.append("\n\n");
		}
		myUIFacade.showErrorDialog(buf.toString());
		myErrors.clear();
		myUIFacade.resetErrorLog();
	}

}
