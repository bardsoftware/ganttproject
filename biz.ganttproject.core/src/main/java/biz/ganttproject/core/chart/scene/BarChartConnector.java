package biz.ganttproject.core.chart.scene;

import java.awt.*;

public interface BarChartConnector<T, D> {
  BarChartActivity<T> getStart();
  BarChartActivity<T> getEnd();
  D getImpl();
  Dimension getStartVector();
  Dimension getEndVector();
}
