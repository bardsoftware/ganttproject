/*
 * Created on 03.11.2004
 */
package net.sourceforge.ganttproject.action;

import java.awt.event.ActionEvent;
import java.text.MessageFormat;

import net.sourceforge.ganttproject.gui.zoom.ZoomEvent;
import net.sourceforge.ganttproject.gui.zoom.ZoomListener;
import net.sourceforge.ganttproject.gui.zoom.ZoomManager;

/**
 * @author bard
 */
public class ZoomOutAction extends GPAction implements ZoomListener {
    private final ZoomManager myZoomManager;

    public ZoomOutAction(ZoomManager zoomManager, String iconSize) {
        super("zoomOut", iconSize);
        myZoomManager = zoomManager;
        myZoomManager.addZoomListener(this);

    }

    public void actionPerformed(ActionEvent arg0) {
        if (myZoomManager.canZoomOut()) {
            myZoomManager.zoomOut();
        }
        setEnabled(myZoomManager.canZoomOut());
    }

    public void zoomChanged(ZoomEvent e) {
        setEnabled(myZoomManager.canZoomOut());
    }

    protected String getIconFilePrefix() {
        return "zoomm_";
    }

    public void isIconVisible() {
        // TODO Auto-generated method stub
    }

    @Override
    protected String getLocalizedName() {
        return MessageFormat.format("<html><b>&nbsp;{0}&nbsp;</b></html>", getI18n("zoomOut"));
    }

    public void isIconVisible(boolean isNull) {
        setIconVisible(isNull);
    }

}
