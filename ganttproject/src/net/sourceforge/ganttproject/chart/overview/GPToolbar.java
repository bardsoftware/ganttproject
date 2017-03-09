// Copyright (C) 2017 BarD Software
package net.sourceforge.ganttproject.chart.overview;

import biz.ganttproject.core.option.ChangeValueEvent;
import biz.ganttproject.core.option.ChangeValueListener;
import biz.ganttproject.core.option.IntegerOption;
import com.google.common.base.Preconditions;
import net.sourceforge.ganttproject.gui.TestGanttRolloverButton;
import net.sourceforge.ganttproject.gui.UIFacade;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * @author dbarashev@bardsoftware.com
 */
public class GPToolbar {
  private final JPanel myToolbar;
  private final List<TestGanttRolloverButton> myButtons;
  private final IntegerOption myDpiOption;
  private Box.Filler myFiller;

  GPToolbar(JPanel toolbar, java.util.List<TestGanttRolloverButton> buttons, IntegerOption dpiOption) {
    myToolbar = Preconditions.checkNotNull(toolbar);
    myButtons = Preconditions.checkNotNull(buttons);
    myDpiOption = dpiOption;
    if (myDpiOption != null) {
      myDpiOption.addChangeValueListener(new ChangeValueListener() {
        @Override
        public void changeValue(ChangeValueEvent event) {
          SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
              updateButtons();
            }
          });
        }
      });
    }
  }

  public void updateButtons() {
    if (myDpiOption != null && myDpiOption.getValue() >= UIFacade.DEFAULT_DPI) {
      for (TestGanttRolloverButton btn : myButtons) {
        btn.setScale(myDpiOption.getValue().floatValue() / UIFacade.DEFAULT_DPI);
      }
      resize();
    }
  }

  public JPanel getToolbar() {
    return myToolbar;
  }

  public void resize() {
    resizeToolbar(myButtons);
  }

  private void resizeToolbar(List<? extends JComponent> buttons) {
    int width = (int)(48 * myDpiOption.getValue().floatValue() / UIFacade.DEFAULT_DPI);
    Dimension d = new Dimension(width, width);
    for (JComponent b : buttons) {
      if (b == null) {
        continue;
      }
      b.setMinimumSize(d);
      b.setMaximumSize(d);
      b.setPreferredSize(d);
      b.updateUI();
    }
    Dimension toolbarSize = myToolbar.getSize();
    if (d.height != toolbarSize.height) {
      if (myFiller != null) {
        myToolbar.remove(myFiller);
      }

      Dimension newSize = new Dimension(1, d.height);
      myFiller = new Box.Filler(newSize, newSize, newSize);
      myToolbar.add(myFiller, 0);
    }
  }
}
