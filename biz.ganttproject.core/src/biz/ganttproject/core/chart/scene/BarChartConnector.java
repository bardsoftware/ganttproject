package biz.ganttproject.core.chart.scene;

public interface BarChartConnector<T, D> {
  BarChartActivity<T> getStart();
  BarChartActivity<T> getEnd();
  D getImpl();
}
