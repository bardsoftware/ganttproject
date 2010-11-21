/*
 * Created on 20.05.2005
 */
package net.sourceforge.ganttproject;

import javax.swing.Action;
import javax.swing.Icon;

import org.eclipse.core.runtime.IAdaptable;

/**
 * @author bard
 */
public interface GPViewManager {
    GPView createView(IAdaptable adaptable, Icon icon);
    Action getCopyAction();
    Action getCutAction();
    Action getPasteAction();
    
}
