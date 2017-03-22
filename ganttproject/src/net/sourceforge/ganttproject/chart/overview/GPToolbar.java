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
  private final int myBaseHeight;
  private final boolean myButtonsSquared;
  private Box.Filler myFiller;
  private float myButtonSizeScaling = 1f;

  GPToolbar(JPanel toolbar, List<TestGanttRolloverButton> buttons, int baseHeight, boolean buttonsSquared, IntegerOption dpiOption) {
    myToolbar = Preconditions.checkNotNull(toolbar);
    myButtons = Preconditions.checkNotNull(buttons);
    myButtonsSquared = buttonsSquared;
    myBaseHeight = baseHeight;
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
    final int height = (int)(myBaseHeight * myDpiOption.getValue().floatValue() / UIFacade.DEFAULT_DPI);
    if (myButtonsSquared) {
      Dimension d = new Dimension(height, height);
      for (JComponent b : buttons) {
        if (b == null) {
          continue;
        }
//        if (myButtonSizeScaling != 1f) {
//          ((JButton) b).setMargin(new Insets(0, 0, 0, 0));
//        }
        b.setMinimumSize(d);
        b.setMaximumSize(d);
        b.setPreferredSize(d);
        b.updateUI();
      }
    }
    Dimension toolbarSize = myToolbar.getSize();
    if (height != toolbarSize.height) {
      if (myFiller != null) {
        myToolbar.remove(myFiller);
      }

      Dimension newSize = new Dimension(1, height);
      myFiller = new Box.Filler(newSize, newSize, newSize);
      myToolbar.add(myFiller, 0);
    }
  }

  public void setButtonSizeScaling(float buttonSizeScaling) {
    this.myButtonSizeScaling = buttonSizeScaling;
  }
}
