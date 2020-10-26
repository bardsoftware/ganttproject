/*
GanttProject is an opensource project management tool.
Copyright (C) 2011 GanttProject Team

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 3
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package net.sourceforge.ganttproject.print;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.JobName;
import javax.print.attribute.standard.MediaSize;
import javax.print.attribute.standard.MediaSizeName;
import javax.print.attribute.standard.OrientationRequested;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.border.MatteBorder;

import biz.ganttproject.core.option.DateOption;
import biz.ganttproject.core.option.DefaultDateOption;

import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.GanttExportSettings;
import net.sourceforge.ganttproject.GanttProject;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.action.zoom.ZoomActionSet;
import net.sourceforge.ganttproject.chart.Chart;
import net.sourceforge.ganttproject.document.Document;
import net.sourceforge.ganttproject.gui.TestGanttRolloverButton;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.options.OptionsPageBuilder;
import net.sourceforge.ganttproject.language.GanttLanguage;

public class PrintPreview extends JDialog {

  private final static MediaSizeName DEFAULT_MEDIA_SIZE_NAME = MediaSizeName.ISO_A4;

  private static GanttLanguage language = GanttLanguage.getInstance();

  private int myPageWidth;

  private int myPageHeight;

  private Printable myPrintable = null;

  private JComboBox myComboScale = null;

  private PreviewContainer myPreviewContainer = null;

  private int myOrientation = PageFormat.LANDSCAPE;

  private PageFormat myPageFormat = null;

  private int myScale;

  private Chart myChart = null;

  // private JButton myStartDateButton = null;
  //
  // private JButton myEndDateButton = null;

  private GanttExportSettings myExportSettings = null;

  // private Date myStartDate = null;
  //
  // private Date myEndDate = null;

  // private GanttDialogDate myEndDialogDate = null;
  //
  // private GanttDialogDate myStartDialogDate = null;

  private JButton myWholeProjectButton = null;

  private MediaSizeName myMediaSizeName;

  private JComboBox myComboMediaSize = null;

  private StatusBar statusBar;

  private DateOption myStart = new DefaultDateOption("generic.startDate") {
    @Override
    public void setValue(Date value) {
      super.setValue(value);
      commit();
      onChangingDates();
      lock();
    }
  };

  private DateOption myFinish = new DefaultDateOption("generic.endDate") {
    @Override
    public void setValue(Date value) {
      super.setValue(value);
      commit();
      onChangingDates();
      lock();
    }
  };

  private final IGanttProject myProject;

  private final UIFacade myUIFacade;

  private void onChangingDates() {
    myExportSettings.setStartDate(myStart.getValue());
    myExportSettings.setEndDate(myFinish.getValue());
    updateSourceImage();
  }

  public PrintPreview(IGanttProject project, UIFacade uifacade, Chart chart, Date start, Date end) {
    super(uifacade.getMainFrame(), language.getText("preview"), false);
    myProject = project;
    myUIFacade = uifacade;
    Dimension screenDim = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
    setSize((int) (screenDim.width * 0.75), (int) (screenDim.height * 0.75));
    setLocationRelativeTo(null);
    myChart = chart;
    if (start == null) {
      start = myChart.getStartDate();
    }
    if (end == null) {
      end = myChart.getEndDate();
    }
    // myStartDate = start;
    // myEndDate = end;
    myExportSettings = new GanttExportSettings();
    // myExportSettings.setOnlySelectedItem(!Mediator
    // .getTaskSelectionManager().getSelectedTasks().isEmpty());
    // myExportSettings.setStartDate(myStartDate);
    // myExportSettings.setEndDate(myEndDate);
    myPrintable = new GanttPrintable(myChart.getRenderedImage(myExportSettings), GanttPrintable.REDUCE_FACTOR_DEFAULT);

    JButton bPrint = new TestGanttRolloverButton(new ImageIcon(getClass().getResource("/icons/print_16.gif")));
    bPrint.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        print();
      }
    });

    JButton bClose = new TestGanttRolloverButton(new ImageIcon(getClass().getResource("/icons/exit_16.gif")));
    bClose.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        setVisible(false);
        dispose();
      }
    });

    final JButton bPortrait = new TestGanttRolloverButton(new ImageIcon(
        getClass().getResource("/icons/portrait_16.gif")));
    final JButton bLandscape = new TestGanttRolloverButton(new ImageIcon(getClass().getResource(
        "/icons/landscape_16.gif")));

    bPortrait.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        run(new Runnable() {
          @Override
          public void run() {
            changePageOrientation(PageFormat.PORTRAIT);
            bLandscape.setEnabled(true);
            bPortrait.setEnabled(false);
          }
        });
      }
    });

    bLandscape.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        run(new Runnable() {
          @Override
          public void run() {
            changePageOrientation(PageFormat.LANDSCAPE);
            bLandscape.setEnabled(false);
            bPortrait.setEnabled(true);
          }
        });
      }
    });
    bLandscape.setEnabled(false);
    String[] scales = { "10 %", "25 %", "50 %", "100 %" };
    myComboScale = new JComboBox(scales);
    myComboScale.setSelectedIndex(2);
    myComboScale.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        run(new Runnable() {
          @Override
          public void run() {
            changeScale();
          }
        });
      }
    });

    myComboScale.addItemListener(new ItemListener() {
      @Override
      public void itemStateChanged(ItemEvent arg0) {
        run(new Runnable() {
          @Override
          public void run() {
            changeScale();
          }
        });
      }
    });

    Dimension dim = myComboScale.getPreferredSize();
    dim.setSize(dim.getWidth() + 20, dim.getHeight());
    myComboScale.setMaximumSize(dim);
    myComboScale.setPreferredSize(dim);
    myComboScale.setEditable(true);

    boolean isDate = start != null && end != null;

    myStart.lock();
    myFinish.lock();
    if (isDate) {
      /*
       * myStartDateButton = new JButton(new GanttCalendar(myStartDate)
       * .toString(), icon);
       * myStartDateButton.setHorizontalTextPosition(SwingConstants.RIGHT);
       * myStartDateButton.addActionListener(new ActionListener() { public void
       * actionPerformed(ActionEvent e) { myStartDialogDate = new
       * GanttDialogDate(new JDialog(), new GanttCalendar(myStartDate), false);
       * myStartDialogDate.setVisible(true); if (myStartDialogDate.getValue() ==
       * GanttDialogDate.OK) { myStartDate =
       * myStartDialogDate.getDate().getTime(); myStartDateButton .setText(new
       * GanttCalendar(myStartDate) .toString());
       * myExportSettings.setStartDate(myStartDate);
       * updateSourceImage(myChart.getChart(myExportSettings)); } } });
       * 
       * myEndDateButton = new JButton(new GanttCalendar(myEndDate) .toString(),
       * icon); myEndDateButton.setHorizontalTextPosition(SwingConstants.RIGHT);
       * myEndDateButton.addActionListener(new ActionListener() { public void
       * actionPerformed(ActionEvent e) { myEndDialogDate = new
       * GanttDialogDate(new JDialog(), new GanttCalendar(myEndDate), false);
       * myEndDialogDate.setVisible(true); if (myEndDialogDate.getValue() ==
       * GanttDialogDate.OK) { myEndDate = myEndDialogDate.getDate().getTime();
       * myEndDateButton.setText(new GanttCalendar(myEndDate) .toString());
       * myExportSettings.setEndDate(myEndDate);
       * updateSourceImage(myChart.getChart(myExportSettings)); } } });
       */
      myWholeProjectButton = new JButton(language.getText("wholeProject"));
      myWholeProjectButton.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          myStart.setValue(myChart.getStartDate());
          myFinish.setValue(myChart.getEndDate());
          /*
           * myStartDate = myChart.getStartDate(); myEndDate =
           * myChart.getEndDate(); myExportSettings.setStartDate(myStartDate);
           * myExportSettings.setEndDate(myEndDate);
           * 
           * myEndDateButton.setText(new GanttCalendar(myExportSettings
           * .getEndDate()).toString()); myStartDateButton.setText(new
           * GanttCalendar( myExportSettings.getStartDate()).toString());
           * myEndDialogDate = new GanttDialogDate(new JDialog(), new
           * GanttCalendar(myEndDate), false); myStartDialogDate = new
           * GanttDialogDate(new JDialog(), new GanttCalendar(myStartDate),
           * false); updateSourceImage(myChart.getChart(myExportSettings));
           */
        }
      });
    }
    List<MediaSizeName> sizes = new ArrayList<MediaSizeName>();
    try {
      Field[] fields = MediaSizeName.class.getDeclaredFields();
      for (int i = 0; i < fields.length; i++) {
        if (fields[i].getType().equals(MediaSizeName.class)) {
          sizes.add((MediaSizeName) fields[i].get(null));
        }
      }
    } catch (IllegalArgumentException e1) {
      e1.printStackTrace();
    } catch (IllegalAccessException e1) {
      e1.printStackTrace();
    }
    myComboMediaSize = new JComboBox(sizes.toArray());
    myComboMediaSize.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent arg0) {
        run(new Runnable() {
          @Override
          public void run() {
            Object selectedItem = myComboMediaSize.getSelectedItem();
            if (selectedItem != null) {
              myMediaSizeName = (MediaSizeName) selectedItem;
              MediaSize ms = MediaSize.getMediaSizeForName(myMediaSizeName);
              Paper p = new Paper();
              float[] size = ms.getSize(MediaSize.INCH);
              p.setSize(size[0] * 72, size[1] * 72);
              p.setImageableArea(72, 72, p.getWidth() - 72 * 2, p.getHeight() - 72 * 2);
              myPageFormat.setPaper(p);
              changePageOrientation(myOrientation);
              statusBar.setText1(ms.getX(MediaSize.MM) + " x " + ms.getY(MediaSize.MM));
              myPreviewContainer.repaint();
            }
          }
        });
      }
    });
    // dim = myComboMediaSize.getPreferredSize();
    // dim.setSize(dim.getWidth() + 20, dim.getHeight());
    // myComboMediaSize.setMaximumSize(dim);
    // myComboMediaSize.setPreferredSize(dim);

    bPrint.setToolTipText(GanttProject.getToolTip(language.correctLabel(language.getText("project.print"))));
    bPortrait.setToolTipText(GanttProject.getToolTip(language.correctLabel(language.getText("portrait"))));
    bLandscape.setToolTipText(GanttProject.getToolTip(language.correctLabel(language.getText("landscape"))));
    bClose.setToolTipText(GanttProject.getToolTip(language.correctLabel(language.getText("close"))));
    final JButton bZoomOut;
    final JButton bZoomIn;
    if (isDate) {
      myWholeProjectButton.setToolTipText(GanttProject.getToolTip(language.getCorrectedLabel("displayWholeProject")));
      /*
       * myStartDateButton.setToolTipText(GanttProject
       * .getToolTip(GanttProject.correctLabel(language
       * .getText("setStartDate"))));
       * myEndDateButton.setToolTipText(GanttProject.getToolTip(GanttProject
       * .correctLabel(language.getText("setEndDate"))));
       */
      // GanttProject gp = Mediator.getGanttProjectSingleton();

      final ZoomActionSet zoomActionSet = myUIFacade.getZoomActionSet();
      final Action zoomOut = zoomActionSet.getZoomOutAction();
      final Action zoomIn = zoomActionSet.getZoomInAction();
      bZoomOut = new JButton((Icon) zoomOut.getValue(Action.SMALL_ICON));
      bZoomIn = new JButton((Icon) zoomIn.getValue(Action.SMALL_ICON));

      bZoomOut.setHorizontalTextPosition(SwingConstants.RIGHT);
      bZoomOut.setText(language.getText("narrowChart"));
      bZoomOut.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent arg0) {
          run(new Runnable() {
            @Override
            public void run() {
              zoomOut.actionPerformed(null);
              updateSourceImage();
              bZoomOut.setEnabled(zoomOut.isEnabled());
              bZoomIn.setEnabled(zoomIn.isEnabled());
            }
          });
        }
      });

      bZoomIn.setHorizontalTextPosition(SwingConstants.RIGHT);
      bZoomIn.setText(language.getText("widenChart"));
      bZoomIn.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent arg0) {
          run(new Runnable() {
            @Override
            public void run() {
              zoomIn.actionPerformed(null);
              updateSourceImage();
              bZoomOut.setEnabled(zoomOut.isEnabled());
              bZoomIn.setEnabled(zoomIn.isEnabled());
            }
          });
        }
      });
    } else {
      bZoomOut = null;
      bZoomIn = null;
    }

    Box tb = Box.createHorizontalBox();
    tb.add(bClose);
    tb.add(Box.createHorizontalStrut(16));
    tb.add(bPrint);
    tb.add(Box.createHorizontalStrut(16));
    tb.add(bPortrait);
    tb.add(bLandscape);
    tb.add(Box.createHorizontalStrut(16));
    tb.add(new JLabel(language.getText("zoom") + " "));
    tb.add(myComboScale);
    tb.add(Box.createHorizontalStrut(16));
    tb.add(new JLabel(language.getText("choosePaperFormat") + " "));
    tb.add(Box.createHorizontalStrut(3));
    tb.add(myComboMediaSize);
    tb.add(Box.createHorizontalGlue());

    Box tb2 = Box.createHorizontalBox();
    if (isDate) {
      tb2.add(bZoomOut);
      tb2.add(Box.createHorizontalStrut(5));
      tb2.add(bZoomIn);
      tb2.add(Box.createHorizontalStrut(5));
      tb2.add(myWholeProjectButton);
      tb2.add(Box.createHorizontalStrut(16));
      OptionsPageBuilder builder = new OptionsPageBuilder();
      builder.setOptionKeyPrefix("");
      tb2.add(builder.createStandaloneOptionPanel(myStart));
      URL iconArrow = this.getClass().getClassLoader().getResource("icons/fromto.gif");
      tb2.add(new JLabel(new ImageIcon(iconArrow)));
      tb2.add(builder.createStandaloneOptionPanel(myFinish));
    }

    JPanel topPanel = new JPanel(new BorderLayout());
    tb.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
    tb2.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
    topPanel.add(tb, BorderLayout.NORTH);
    topPanel.add(tb2, BorderLayout.SOUTH);

    getContentPane().add(topPanel, BorderLayout.NORTH);

    statusBar = new StatusBar();
    statusBar.setLabel0(language.getText("pageNumber"));
    statusBar.setLabel1(language.getText("paperSize"));
    getContentPane().add(statusBar, BorderLayout.SOUTH);

    myPreviewContainer = new PreviewContainer();

    myPageFormat = new PageFormat();
    myPageFormat.setOrientation(myOrientation);

    myMediaSizeName = DEFAULT_MEDIA_SIZE_NAME;
    MediaSize ms = MediaSize.getMediaSizeForName(myMediaSizeName);
    Paper p = new Paper();
    float[] size = ms.getSize(MediaSize.INCH);
    p.setSize(size[0] * 72, size[1] * 72);
    p.setImageableArea(72, 72, p.getWidth() - 72 * 2, p.getHeight() - 72 * 2);
    myPageFormat.setPaper(p);

    statusBar.setText1(ms.getX(MediaSize.MM) + " x " + ms.getY(MediaSize.MM));

    if (myPageFormat.getHeight() == 0 || myPageFormat.getWidth() == 0) {
      myUIFacade.showErrorDialog("Unable to determine default page size");
      return;
    }
    myPageWidth = (int) (myPageFormat.getWidth());
    myPageHeight = (int) (myPageFormat.getHeight());
    myScale = 50;

    createPages();

    JScrollPane ps = new JScrollPane(myPreviewContainer);
    getContentPane().add(ps, BorderLayout.CENTER);

    setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    setVisible(true);
    myComboMediaSize.setSelectedItem(myMediaSizeName);
    myStart.setValue(start);
    myFinish.setValue(end);
  }

  private void createPages() {
    int pageIndex = 0;
    try {
      while (true) {
        BufferedImage img = new BufferedImage(myPageWidth, myPageHeight, BufferedImage.TYPE_INT_RGB);
        Graphics g = img.getGraphics();
        g.setColor(Color.white);
        g.fillRect(0, 0, myPageWidth, myPageHeight);
        if (myPrintable.print(g, myPageFormat, pageIndex) != Printable.PAGE_EXISTS) {
          break;
        }
        PagePreview pp = new PagePreview(pageIndex, myPageFormat, myPrintable, myScale);
        myPreviewContainer.add(pp);
        pageIndex++;
      }
      statusBar.setText0("" + pageIndex);
    } catch (PrinterException e) {
      myUIFacade.showErrorDialog(e);
    }
  }

  private void changeScale() {
    String str = myComboScale.getSelectedItem().toString();
    if (str.endsWith("%")) {
      str = str.substring(0, str.length() - 1);
    }
    str = str.trim();
    myScale = 0;
    try {
      myScale = Integer.parseInt(str);
    } catch (NumberFormatException ex) {
      return;
    }

    Component[] comps = myPreviewContainer.getComponents();
    for (Component c : comps) {
      if (!(c instanceof PagePreview)) {
        continue;
      }
      PagePreview pp = (PagePreview) c;
      pp.setScale(myScale);
    }
    PagePreview.clearCache();
    myPreviewContainer.doLayout();
    myPreviewContainer.getParent().getParent().validate();
  }

  private void run(Runnable runnable) {
    try {
      setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
      runnable.run();
    } catch (Exception e) {
      GPLogger.log(e);
    } finally {
      setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    }
  }

  private void changePageOrientation(int newOrientation) {
    myOrientation = newOrientation;
    myPageFormat.setOrientation(myOrientation);
    myPageWidth = (int) (myPageFormat.getWidth());
    myPageHeight = (int) (myPageFormat.getHeight());

    myPreviewContainer.removeAll();
    myPreviewContainer.repaint();
    createPages();
    myPreviewContainer.doLayout();
    myPreviewContainer.getParent().getParent().validate();
    myPreviewContainer.validate();
    PagePreview.clearCache();
  }

  private void print() {
    PrinterJob prnJob = PrinterJob.getPrinterJob();
    prnJob.setPrintable(myPrintable);
    setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

    PrintRequestAttributeSet attr = new HashPrintRequestAttributeSet();
    attr.add(myMediaSizeName);
    attr.add(myOrientation == PageFormat.LANDSCAPE ? OrientationRequested.LANDSCAPE : OrientationRequested.PORTRAIT);
    Document doc = myProject.getDocument();
    if (doc != null) {
      attr.add(new JobName(doc.getFileName(), language.getLocale()));
    }

    if (prnJob.printDialog(attr)) {
      try {
        prnJob.print(attr);
        setVisible(false);

      } catch (Exception e) {
        e.printStackTrace();
        myUIFacade.showErrorDialog(e);
      }
      dispose();
    }
    setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));

  }

  private void updateSourceImage() {
    try {
      RenderedImage image = myChart.getRenderedImage(myExportSettings);
      myPrintable = new GanttPrintable(image, GanttPrintable.REDUCE_FACTOR_DEFAULT);
      changePageOrientation(myOrientation);
    } catch (OutOfMemoryError e) {
      myUIFacade.showErrorDialog(language.getText("printing.out_of_memory"));
    }
  }

  static class PreviewContainer extends JPanel {
    protected final int H_GAP = 16;

    protected final int V_GAP = 10;

    @Override
    public Dimension getPreferredSize() {
      int n = getComponentCount();
      if (n == 0) {
        return new Dimension(H_GAP, V_GAP);
      }
      Component comp = getComponent(0);
      Dimension dc = comp.getPreferredSize();
      int w = dc.width;
      int h = dc.height;

      Dimension dp = getParent().getSize();
      int nCol = Math.max((dp.width - H_GAP) / (w + H_GAP), 1);
      int nRow = n / nCol;
      if (nRow * nCol < n) {
        nRow++;
      }

      int ww = nCol * (w + H_GAP) + H_GAP;
      int hh = nRow * (h + V_GAP) + V_GAP;
      Insets ins = getInsets();
      return new Dimension(ww + ins.left + ins.right, hh + ins.top + ins.bottom);
    }

    @Override
    public Dimension getMaximumSize() {
      return getPreferredSize();
    }

    @Override
    public Dimension getMinimumSize() {
      return getPreferredSize();
    }

    @Override
    public void doLayout() {
      Insets ins = getInsets();
      int x = ins.left + H_GAP;
      int y = ins.top + V_GAP;

      int n = getComponentCount();
      if (n == 0)
        return;
      Component comp = getComponent(0);
      Dimension dc = comp.getPreferredSize();
      int w = dc.width;
      int h = dc.height;

      Dimension dp = getParent().getSize();
      int nCol = Math.max((dp.width - H_GAP) / (w + H_GAP), 1);
      int nRow = n / nCol;
      if (nRow * nCol < n)
        nRow++;

      int index = 0;
      for (int k = 0; k < nRow; k++) {
        for (int m = 0; m < nCol; m++) {
          if (index >= n) {
            return;
          }
          comp = getComponent(index++);
          comp.setBounds(x, y, w, h);
          x += w + H_GAP;
        }
        y += h + V_GAP;
        x = ins.left + H_GAP;
      }
    }
  }

  static class PagePreview extends JPanel {
    static SortedMap<Integer, Image> ourImageCache = new TreeMap<Integer, Image>();
    private final int myPageIndex;
    private final PageFormat myPageFormat;
    private final Printable myPrintableChart;
    private int myScalePercents;

    public PagePreview(int pageIndex, PageFormat pageFormat, Printable chart, int scalePercents) {
      myScalePercents = scalePercents;
      myPageIndex = pageIndex;
      myPageFormat = pageFormat;
      myPrintableChart = chart;
      setBackground(Color.white);
      setBorder(new MatteBorder(1, 1, 2, 2, Color.black));
    }

    public static void clearCache() {
      ourImageCache.clear();
    }

    void setScale(int scale) {
      myScalePercents = scale;
    }

    private int getScaledWidth() {
      return (int) (myPageFormat.getWidth() * myScalePercents / 100);
    }

    private int getScaledHeight() {
      return (int) (myPageFormat.getHeight() * myScalePercents / 100);
    }

    @Override
    public Dimension getPreferredSize() {
      Insets ins = getInsets();
      return new Dimension(getScaledWidth() + ins.left + ins.right, getScaledHeight() + ins.top + ins.bottom);
    }

    @Override
    public Dimension getMaximumSize() {
      return getPreferredSize();
    }

    @Override
    public Dimension getMinimumSize() {
      return getPreferredSize();
    }

    @Override
    protected void paintComponent(Graphics g) {
      super.paintComponent(g);
      Image scaledImage = ourImageCache.get(new Integer(myPageIndex));
      if (scaledImage == null) {
        BufferedImage bufferImage = new BufferedImage((int) myPageFormat.getWidth(), (int) myPageFormat.getHeight(),
            BufferedImage.TYPE_INT_RGB);
        Graphics bufferGraphics = bufferImage.getGraphics();
        {
          bufferGraphics.setColor(Color.white);
          bufferGraphics.fillRect(0, 0, bufferImage.getWidth(), bufferImage.getHeight());
          try {
            myPrintableChart.print(bufferGraphics, myPageFormat, myPageIndex);
          } catch (PrinterException e) {
            if (!GPLogger.log(e)) {
              e.printStackTrace(System.err);
            }
          }
        }
        scaledImage = bufferImage.getScaledInstance(getScaledWidth(), getScaledHeight(), Image.SCALE_SMOOTH);
        ourImageCache.put(new Integer(myPageIndex), scaledImage);
      }
      g.setColor(getBackground());
      g.fillRect(0, 0, getWidth(), getHeight());
      g.drawImage(scaledImage, 0, 0, null);
    }
  }

  static class StatusBar extends JPanel {
    JLabel label0;

    JLabel message0;

    JLabel label1;

    JLabel message1;

    public StatusBar() {
      super(new BorderLayout());
      label0 = new JLabel();
      message0 = new JLabel();
      JPanel panel0 = new JPanel();
      panel0.add(label0);
      panel0.add(message0);
      add(panel0, BorderLayout.WEST);
      label1 = new JLabel();
      message1 = new JLabel();
      JPanel panel1 = new JPanel();
      panel1.add(label1);
      panel1.add(message1);
      add(panel1, BorderLayout.EAST);

    }

    public void setLabel0(String label) {
      label0.setText(label);
    }

    public void setText0(String text) {
      message0.setText(text);
    }

    public void setLabel1(String label) {
      label1.setText(label);
    }

    public void setText1(String text) {
      message1.setText(text);
    }
  }
}
