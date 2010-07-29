package net.sourceforge.ganttproject;

import java.awt.Component;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.EventObject;

import javax.swing.JTable;
import javax.swing.event.CellEditorListener;
import javax.swing.table.TableCellEditor;
import javax.swing.text.JTextComponent;

class TreeTableCellEditorImpl  implements TableCellEditor {
	private TableCellEditor myProxiedEditor;

	TreeTableCellEditorImpl(TableCellEditor proxiedEditor){
		myProxiedEditor = proxiedEditor;
	}
	public Component getTableCellEditorComponent(JTable arg0, Object arg1, boolean arg2, int arg3, int arg4) {
		final Component result = myProxiedEditor.getTableCellEditorComponent(arg0, arg1,arg2, arg3, arg4);
		if (result instanceof JTextComponent) {
			((JTextComponent)result).selectAll();
			result.addFocusListener(new FocusAdapter() {
				public void focusGained(FocusEvent arg0) {
					super.focusGained(arg0);
					((JTextComponent)result).selectAll();
					result.removeFocusListener(this);
				}

				public void focusLost(FocusEvent arg0) {
					// TODO Auto-generated method stub
					super.focusLost(arg0);
				}
				
			});
		}
		return result;
	}

	public Object getCellEditorValue() {
		return myProxiedEditor.getCellEditorValue();
	}

	public boolean isCellEditable(EventObject arg0) {
		return myProxiedEditor.isCellEditable(arg0);
	}

	public boolean shouldSelectCell(EventObject arg0) {
		return myProxiedEditor.shouldSelectCell(arg0);
	}

	public boolean stopCellEditing() {
		return myProxiedEditor.stopCellEditing();
	}

	public void cancelCellEditing() {
		myProxiedEditor.cancelCellEditing();
	}

	public void addCellEditorListener(CellEditorListener arg0) {
		myProxiedEditor.addCellEditorListener(arg0);
	}
	public void removeCellEditorListener(CellEditorListener arg0) {
		myProxiedEditor.removeCellEditorListener(arg0);
	}
}
