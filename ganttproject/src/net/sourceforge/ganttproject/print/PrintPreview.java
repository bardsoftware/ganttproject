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
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.net.URL;
import java.util.Date;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Vector;

import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.JobName;
import javax.print.attribute.standard.MediaSize;
import javax.print.attribute.standard.MediaSizeName;
import javax.print.attribute.standard.OrientationRequested;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.border.MatteBorder;

import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.GanttExportSettings;
import net.sourceforge.ganttproject.GanttProject;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.action.ZoomInAction;
import net.sourceforge.ganttproject.action.ZoomOutAction;
import net.sourceforge.ganttproject.chart.Chart;
import net.sourceforge.ganttproject.document.Document;
import net.sourceforge.ganttproject.gui.TestGanttRolloverButton;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.options.OptionsPageBuilder;
import net.sourceforge.ganttproject.gui.options.model.DateOption;
import net.sourceforge.ganttproject.gui.options.model.DefaultDateOption;
import net.sourceforge.ganttproject.gui.zoom.ZoomManager;
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

//    private JButton myStartDateButton = null;
//
//    private JButton myEndDateButton = null;

    private GanttExportSettings myExportSettings = null;

//    private Date myStartDate = null;
//
//    private Date myEndDate = null;

//    private GanttDialogDate myEndDialogDate = null;
//
//    private GanttDialogDate myStartDialogDate = null;

    private JButton myWholeProjectButton = null;

    private MediaSizeName myMediaSizeName;

    private JComboBox myComboMediaSize = null;

    private StatusBar statusBar;

    private DateOption myStart = new DefaultDateOption("generic.startDate") {
        public void setValue(Date value) {
            super.setValue(value);
            commit();
            onChangingDates();
            lock();
        }
    };

    private DateOption myFinish = new DefaultDateOption("generic.endDate") {
        public void setValue(Date value) {
            super.setValue(value);
            commit();
            onChangingDates();
            lock();
        }
    };

	private final IGanttProject myProject;

	private final UIFacade myUIfacade;

    private void onChangingDates() {
        myExportSettings.setStartDate(myStart.getValue());
        myExportSettings.setEndDate(myFinish.getValue());
        updateSourceImage();            
    }

    public PrintPreview(IGanttProject project, UIFacade uifacade, Chart chart, Date start,
            Date end) {
        super(uifacade.getMainFrame(), GanttLanguage.getInstance().getText("preview"), false);
        myProject = project;
        myUIfacade = uifacade;
        Dimension screenDim = java.awt.Toolkit.getDefaultToolkit()
                .getScreenSize();
        setSize((int) (screenDim.width * 0.75), (int) (screenDim.height * 0.75));
        setLocationRelativeTo(null);
        myChart = chart;
        if (start==null) {
            start = myChart.getStartDate();
        }
        if (end==null) {
            end = myChart.getEndDate();
        }
//        myStartDate = start;
//        myEndDate = end;
        myExportSettings = new GanttExportSettings();
//        myExportSettings.setOnlySelectedItem(!Mediator
//                .getTaskSelectionManager().getSelectedTasks().isEmpty());
//        myExportSettings.setStartDate(myStartDate);
//        myExportSettings.setEndDate(myEndDate);
        myPrintable = new GanttPrintable(myChart.getChart(myExportSettings),
                GanttPrintable.REDUCE_FACTOR_DEFAULT);
        JToolBar tb = new JToolBar();
        JToolBar tb2 = new JToolBar();

        JButton bPrint = new TestGanttRolloverButton(new ImageIcon(getClass()
                .getResource("/icons/print_16.gif")));
        bPrint.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                print();
            }
        });

        JButton bClose = new TestGanttRolloverButton(new ImageIcon(getClass()
                .getResource("/icons/exit_16.gif")));
        bClose.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
                dispose();
            }
        });

        final JButton bPortrait = new TestGanttRolloverButton(new ImageIcon(
                getClass().getResource("/icons/portrait_16.gif")));
        final JButton bLandscape = new TestGanttRolloverButton(new ImageIcon(
                getClass().getResource("/icons/landscape_16.gif")));

        bPortrait.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                run(new Runnable() {
                    public void run() {
                        changePageOrientation(PageFormat.PORTRAIT);
                        bLandscape.setEnabled(true);
                        bPortrait.setEnabled(false);
                    }
                });
            }
        });

        bLandscape.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                run(new Runnable() {
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
            public void actionPerformed(ActionEvent e) {
                run(new Runnable() {
                    public void run() {
                        changeScale();
                    }
                });
            }
        });

        myComboScale.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent arg0) {
                run(new Runnable() {
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
            myStartDateButton = new JButton(new GanttCalendar(myStartDate)
                    .toString(), icon);
            myStartDateButton.setHorizontalTextPosition(SwingConstants.RIGHT);
            myStartDateButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    myStartDialogDate = new GanttDialogDate(new JDialog(),
                            new GanttCalendar(myStartDate), false);
                    myStartDialogDate.setVisible(true);
                    if (myStartDialogDate.getValue() == GanttDialogDate.OK) {
                        myStartDate = myStartDialogDate.getDate().getTime();
                        myStartDateButton
                                .setText(new GanttCalendar(myStartDate)
                                        .toString());
                        myExportSettings.setStartDate(myStartDate);
                        updateSourceImage(myChart.getChart(myExportSettings));
                    }
                }
            });

            myEndDateButton = new JButton(new GanttCalendar(myEndDate)
                    .toString(), icon);
            myEndDateButton.setHorizontalTextPosition(SwingConstants.RIGHT);
            myEndDateButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    myEndDialogDate = new GanttDialogDate(new JDialog(),
                            new GanttCalendar(myEndDate), false);
                    myEndDialogDate.setVisible(true);
                    if (myEndDialogDate.getValue() == GanttDialogDate.OK) {
                        myEndDate = myEndDialogDate.getDate().getTime();
                        myEndDateButton.setText(new GanttCalendar(myEndDate)
                                .toString());
                        myExportSettings.setEndDate(myEndDate);
                        updateSourceImage(myChart.getChart(myExportSettings));
                    }
                }
            });
            */
            myWholeProjectButton = new JButton(language.getText("wholeProject"));
            myWholeProjectButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    myStart.setValue(myChart.getStartDate());
                    myFinish.setValue(myChart.getEndDate());
                    /*
                    myStartDate = myChart.getStartDate();
                    myEndDate = myChart.getEndDate();
                    myExportSettings.setStartDate(myStartDate);
                    myExportSettings.setEndDate(myEndDate);

                    myEndDateButton.setText(new GanttCalendar(myExportSettings
                            .getEndDate()).toString());
                    myStartDateButton.setText(new GanttCalendar(
                            myExportSettings.getStartDate()).toString());
                    myEndDialogDate = new GanttDialogDate(new JDialog(),
                            new GanttCalendar(myEndDate), false);
                    myStartDialogDate = new GanttDialogDate(new JDialog(),
                            new GanttCalendar(myStartDate), false);
                    updateSourceImage(myChart.getChart(myExportSettings));
                    */
                }
            });
        }
        Vector<MediaSizeName> vMedia = new Vector<MediaSizeName>();
        ;
        // try {
        // vMedia = getAllMediaSizeNameAvailable();
        vMedia.add(MediaSizeName.ISO_A0);
        vMedia.add(MediaSizeName.ISO_A1);
        vMedia.add(MediaSizeName.ISO_A2);
        vMedia.add(MediaSizeName.ISO_A3);
        vMedia.add(MediaSizeName.ISO_A4);
        vMedia.add(MediaSizeName.ISO_A5);
        vMedia.add(MediaSizeName.ISO_A6);
        // } catch (ClassNotFoundException e1) {
        // // TODO Auto-generated catch block
        // e1.printStackTrace();
        // }

        if (vMedia != null) {
            myComboMediaSize = new JComboBox(vMedia);
            dim = myComboMediaSize.getPreferredSize();
            dim.setSize(dim.getWidth() + 20, dim.getHeight());
            myComboMediaSize.setMaximumSize(dim);
            myComboMediaSize.setPreferredSize(dim);
            myComboMediaSize.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent arg0) {
                    run(new Runnable() {
                        public void run() {
                            Object selectedItem = myComboMediaSize
                                    .getSelectedItem();
                            if (selectedItem != null) {
                                myMediaSizeName = (MediaSizeName) selectedItem;
                                MediaSize ms = MediaSize
                                        .getMediaSizeForName(myMediaSizeName);
                                Paper p = new Paper();
                                float[] size = ms.getSize(MediaSize.INCH);
                                p.setSize(size[0] * 72, size[1] * 72);
                                p.setImageableArea(72, 72,
                                        p.getWidth() - 72 * 2,
                                        p.getHeight() - 72 * 2);
                                myPageFormat.setPaper(p);
                                changePageOrientation(myOrientation);
                                statusBar.setText1(ms.getX(MediaSize.MM)
                                        + " x " + ms.getY(MediaSize.MM));
                                myPreviewContainer.repaint();
                            }
                        }
                    });
                }
            });
        }

        bPrint.setToolTipText(GanttProject.getToolTip(GanttProject
                .correctLabel(language.getText("printProject"))));
        bPortrait.setToolTipText(GanttProject.getToolTip(GanttProject
                .correctLabel(language.getText("portrait"))));
        bLandscape.setToolTipText(GanttProject.getToolTip(GanttProject
                .correctLabel(language.getText("landscape"))));
        bClose.setToolTipText(GanttProject.getToolTip(GanttProject
                .correctLabel(language.getText("close"))));
        final JButton bZoomOut;
        final JButton bZoomIn;
        if (isDate) {
            myWholeProjectButton.setToolTipText(GanttProject
                    .getToolTip(GanttProject.correctLabel(language
                            .getText("displayWholeProject"))));
            /*
            myStartDateButton.setToolTipText(GanttProject
                    .getToolTip(GanttProject.correctLabel(language
                            .getText("setStartDate"))));
            myEndDateButton.setToolTipText(GanttProject.getToolTip(GanttProject
                    .correctLabel(language.getText("setEndDate"))));
                    */
            //GanttProject gp = Mediator.getGanttProjectSingleton();
            final ZoomManager zoomManager = myUIfacade.getZoomManager();
            final Action zoomOut = new ZoomOutAction(zoomManager, "16");
            final Action zoomIn = new ZoomInAction(zoomManager, "16");
            bZoomOut = new JButton((Icon) zoomOut.getValue(Action.SMALL_ICON));
            bZoomIn = new JButton((Icon) zoomIn.getValue(Action.SMALL_ICON));

            bZoomOut.setHorizontalTextPosition(SwingConstants.RIGHT);
            bZoomOut.setText(language.getText("narrowChart"));
            bZoomOut.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent arg0) {
                    run(new Runnable() {
                        public void run() {
                            zoomOut.actionPerformed(null);
                            updateSourceImage();
                            bZoomOut.setEnabled(zoomManager.canZoomOut());
                            bZoomIn.setEnabled(zoomManager.canZoomIn());
                        }
                    });
                }
            });

            bZoomIn.setHorizontalTextPosition(SwingConstants.RIGHT);
            bZoomIn.setText(language.getText("widenChart"));
            bZoomIn.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent arg0) {
                    run(new Runnable() {
                        public void run() {
                            zoomIn.actionPerformed(null);
                            updateSourceImage();
                            bZoomOut.setEnabled(zoomManager.canZoomOut());
                            bZoomIn.setEnabled(zoomManager.canZoomIn());
                        }
                    });
                }
            });

        } else {
            bZoomOut = null;
            bZoomIn = null;
        }

        tb2.setFloatable(false);
        tb.setFloatable(false);
        tb.add(bClose);
        tb.addSeparator(new Dimension(16, 16));
        tb.add(bPrint);
        tb.addSeparator(new Dimension(16, 16));
        tb.add(bPortrait);
        tb.add(bLandscape);
        tb.addSeparator(new Dimension(16, 16));
        tb.add(new JLabel(language.getText("zoom") + " "));
        tb.add(myComboScale);
        if (vMedia != null && !vMedia.isEmpty()) {
            tb.addSeparator(new Dimension(16, 16));
            tb.add(new JLabel(language.getText("choosePaperFormat") + " "));
            tb.addSeparator(new Dimension(0, 10));
            tb.add(myComboMediaSize);
        }

        if (isDate) {
            tb2.add(bZoomOut);
            tb2.addSeparator(new Dimension(5, 0));
            tb2.add(bZoomIn);
            tb2.addSeparator(new Dimension(20, 0));
            tb2.add(myWholeProjectButton);
            tb2.addSeparator(new Dimension(16, 16));
            OptionsPageBuilder builder = new OptionsPageBuilder();
            builder.setOptionKeyPrefix("");
            tb2.add(builder.createStandaloneOptionPanel(myStart));
            URL iconArrow = this.getClass().getClassLoader().getResource(
                    "icons/fromto.gif");
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

        // --
        myPageFormat = new PageFormat();
        myPageFormat.setOrientation(myOrientation);

        myMediaSizeName = DEFAULT_MEDIA_SIZE_NAME;
        MediaSize ms = MediaSize.getMediaSizeForName(myMediaSizeName);
        Paper p = new Paper();
        float[] size = ms.getSize(MediaSize.INCH);
        p.setSize(size[0] * 72, size[1] * 72);
        p.setImageableArea(72, 72, p.getWidth() - 72 * 2,
                p.getHeight() - 72 * 2);
        myPageFormat.setPaper(p);
        // --
        statusBar.setText1(ms.getX(MediaSize.MM) + " x "
                + ms.getY(MediaSize.MM));

        if (myPageFormat.getHeight() == 0 || myPageFormat.getWidth() == 0) {
            myUIfacade.showErrorDialog("Unable to determine default page size");
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
                BufferedImage img = new BufferedImage(myPageWidth,
                        myPageHeight, BufferedImage.TYPE_INT_RGB);
                Graphics g = img.getGraphics();
                g.setColor(Color.white);
                g.fillRect(0, 0, myPageWidth, myPageHeight);
                if (myPrintable.print(g, myPageFormat, pageIndex) != Printable.PAGE_EXISTS)
                    break;
                PagePreview pp = new PagePreview(pageIndex, myPageFormat, myPrintable, myScale);
                myPreviewContainer.add(pp);
                pageIndex++;
            }
            statusBar.setText0("" + pageIndex);
        } catch (PrinterException e) {
            myUIfacade.showErrorDialog(e);
        }
	}

    private void changeScale() {
        String str = myComboScale.getSelectedItem().toString();
        if (str.endsWith("%"))
            str = str.substring(0, str.length() - 1);
        str = str.trim();
        myScale = 0;
        try {
            myScale = Integer.parseInt(str);
        } catch (NumberFormatException ex) {
            return;
        }

        Component[] comps = myPreviewContainer.getComponents();
        for (int k = 0; k < comps.length; k++) {
            if (!(comps[k] instanceof PagePreview))
                continue;
            PagePreview pp = (PagePreview) comps[k];
            pp.setScale(myScale);
        }
        myPreviewContainer.doLayout();
        myPreviewContainer.getParent().getParent().validate();
    }

    private void run(Runnable runnable) {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        runnable.run();
        setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
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

    }

    private void print() {
        PrinterJob prnJob = PrinterJob.getPrinterJob();
        prnJob.setPrintable(myPrintable);
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        PrintRequestAttributeSet attr = new HashPrintRequestAttributeSet();
        attr.add(myMediaSizeName);
        attr.add(myOrientation == PageFormat.LANDSCAPE ? 
        		   OrientationRequested.LANDSCAPE :
                   OrientationRequested.PORTRAIT);
        Document doc = myProject.getDocument();
        if (doc != null)
            attr.add(new JobName(doc.getDescription(), language.getLocale()));

        if (prnJob.printDialog(attr)) {
            try {
                prnJob.print(attr);
                setVisible(false);

            } catch (Exception e) {
            	e.printStackTrace();
            	myUIfacade.showErrorDialog(e);
            }
            dispose();
        }
        setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));

    }

    private void updateSourceImage() {
    	try { 
	    	BufferedImage image = myChart.getChart(myExportSettings);
	        myPrintable = new GanttPrintable(image, GanttPrintable.REDUCE_FACTOR_DEFAULT);
	        changePageOrientation(myOrientation);
    	}
    	catch (OutOfMemoryError e) {
    		myUIfacade.showErrorDialog(GanttLanguage.getInstance().getText("printing.out_of_memory"));
    	}
    }

    static class PreviewContainer extends JPanel {
        protected final int H_GAP = 16;

        protected final int V_GAP = 10;

        public Dimension getPreferredSize() {
            int n = getComponentCount();
            if (n == 0)
                return new Dimension(H_GAP, V_GAP);
            Component comp = getComponent(0);
            Dimension dc = comp.getPreferredSize();
            int w = dc.width;
            int h = dc.height;

            Dimension dp = getParent().getSize();
            int nCol = Math.max((dp.width - H_GAP) / (w + H_GAP), 1);
            int nRow = n / nCol;
            if (nRow * nCol < n)
                nRow++;

            int ww = nCol * (w + H_GAP) + H_GAP;
            int hh = nRow * (h + V_GAP) + V_GAP;
            Insets ins = getInsets();
            return new Dimension(ww + ins.left + ins.right, hh + ins.top
                    + ins.bottom);
        }

        public Dimension getMaximumSize() {
            return getPreferredSize();
        }

        public Dimension getMinimumSize() {
            return getPreferredSize();
        }

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
                    if (index >= n)
                        return;
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
    	static SortedMap<Integer, BufferedImage> ourImageCache = new TreeMap<Integer, BufferedImage>();
		private final int myPageIndex;
		private final PageFormat myPageFormat;
		private final Printable myChart;
		private int myScalePercents;
		
        public PagePreview(int pageIndex, PageFormat pageFormat, Printable chart, int scalePercents) {
        	myScalePercents = scalePercents;
            myPageIndex = pageIndex;
            myPageFormat = pageFormat;
            myChart = chart;
            setBackground(Color.white);
            setBorder(new MatteBorder(1, 1, 2, 2, Color.black));
        }

        void setScale(int scale) {
        	myScalePercents = scale; 
            repaint();
        }

        private int getScaledWidth() {
        	return (int)(myPageFormat.getWidth() * myScalePercents / 100);
        }

        private int getScaledHeight() {
        	return (int) (myPageFormat.getHeight() * myScalePercents / 100);
        }

        public Dimension getPreferredSize() {
            Insets ins = getInsets();
            return new Dimension(
            		getScaledWidth() + ins.left + ins.right, 
            		getScaledHeight() + ins.top + ins.bottom);
        }

        public Dimension getMaximumSize() {
            return getPreferredSize();
        }

        public Dimension getMinimumSize() {
            return getPreferredSize();
        }

        protected void paintComponent(Graphics g) {
        	super.paintComponent(g);
        	BufferedImage bufferImage = ourImageCache.get(new Integer (myPageIndex));
        	if (bufferImage==null) {
	        	bufferImage = new BufferedImage(
	        			(int)myPageFormat.getWidth(), 
	        			(int)myPageFormat.getHeight(), 
	        			BufferedImage.TYPE_INT_RGB);
	        	if (ourImageCache.size() >= 4) {
	        		ourImageCache.remove(ourImageCache.firstKey());
	        	}
	        	ourImageCache.put(new Integer(myPageIndex), bufferImage);
        	}
            Graphics bufferGraphics = bufferImage.getGraphics();
            {
	            bufferGraphics.setColor(Color.white);
	            bufferGraphics.fillRect(0, 0, bufferImage.getWidth(), bufferImage.getHeight());
	            try {
					myChart.print(bufferGraphics, myPageFormat, myPageIndex);
				} catch (PrinterException e) {
		        	if (!GPLogger.log(e)) {
		        		e.printStackTrace(System.err);
		        	}
				}
            }
            Image scaledImage = bufferImage.getScaledInstance(
            		getScaledWidth(), getScaledHeight(), Image.SCALE_SMOOTH);
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
