/**
 * 
 */
package net.sourceforge.ganttproject.gui;


public interface TableHeaderUIFacade {
	int getSize();
	Column getField(int index);
	void clear();
	void add(String name, int order, int width);
	void importData(TableHeaderUIFacade source);
	
	public interface Column {
		String getID();
		String getName();
		int getOrder();
		int getWidth();
		boolean isVisible();
	}
}