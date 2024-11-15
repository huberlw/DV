import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.*;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.chart.ui.RectangleInsets;

import java.awt.*;

public class ChartsAndPlots
{
    public static JFreeChart createChart(XYSeriesCollection xysc, boolean legend)
    {
        JFreeChart chart = ChartFactory.createXYLineChart(
                "",
                "",
                "",
                xysc,
                PlotOrientation.VERTICAL,
                legend,
                false,
                false);

        // format chart
        chart.setBorderVisible(false);
        chart.setPadding(RectangleInsets.ZERO_INSETS);
        chart.setAntiAlias(true);

        return chart;
    }

    public static XYPlot createPlot(JFreeChart chart, int color)
    {
        // get plot
        XYPlot plot = (XYPlot) chart.getPlot();

        // format plot
        plot.setDrawingSupplier(new DefaultDrawingSupplier(
                new Paint[] { DV.graphColors[color] },
                DefaultDrawingSupplier.DEFAULT_OUTLINE_PAINT_SEQUENCE,
                DefaultDrawingSupplier.DEFAULT_STROKE_SEQUENCE,
                DefaultDrawingSupplier.DEFAULT_OUTLINE_STROKE_SEQUENCE,
                DefaultDrawingSupplier.DEFAULT_SHAPE_SEQUENCE));
        plot.getRangeAxis().setVisible(false);
        plot.getDomainAxis().setVisible(false);
        plot.setOutlinePaint(null);
        plot.setOutlineVisible(false);
        plot.setInsets(RectangleInsets.ZERO_INSETS);
        plot.setDomainPannable(true);
        plot.setRangePannable(true);
        plot.setBackgroundPaint(DV.background);
        plot.setDomainGridlinePaint(Color.GRAY);
        plot.setRangeGridlinePaint(Color.GRAY);
        plot.setSeriesRenderingOrder(SeriesRenderingOrder.REVERSE);

        return plot;
    }


    public static XYPlot createHBPlot(JFreeChart chart, Color color)
    {
        // get plot
        XYPlot plot = (XYPlot) chart.getPlot();

        // format plot
        plot.setDrawingSupplier(new DefaultDrawingSupplier(
                new Paint[] { color },
                DefaultDrawingSupplier.DEFAULT_OUTLINE_PAINT_SEQUENCE,
                DefaultDrawingSupplier.DEFAULT_STROKE_SEQUENCE,
                DefaultDrawingSupplier.DEFAULT_OUTLINE_STROKE_SEQUENCE,
                DefaultDrawingSupplier.DEFAULT_SHAPE_SEQUENCE));
        plot.getRangeAxis().setVisible(true);
        plot.getDomainAxis().setVisible(true);
        plot.setOutlinePaint(null);
        plot.setOutlineVisible(false);
        plot.setInsets(RectangleInsets.ZERO_INSETS);
        plot.setDomainPannable(true);
        plot.setRangePannable(true);
        plot.setBackgroundPaint(DV.background);
        plot.setDomainGridlinePaint(Color.GRAY);
        plot.setSeriesRenderingOrder(SeriesRenderingOrder.REVERSE);

        return plot;
    }


    /**
     * Creates blank graph
     */
    public static ChartPanel blankGraph()
    {
        // create blank graph
        XYSeriesCollection data = new XYSeriesCollection();
        JFreeChart chart = ChartFactory.createXYLineChart("", "", "", data);
        XYPlot plot = (XYPlot) chart.getPlot();
        plot.setDrawingSupplier(new DefaultDrawingSupplier(
                new Paint[] { Color.RED },
                DefaultDrawingSupplier.DEFAULT_OUTLINE_PAINT_SEQUENCE,
                DefaultDrawingSupplier.DEFAULT_STROKE_SEQUENCE,
                DefaultDrawingSupplier.DEFAULT_OUTLINE_STROKE_SEQUENCE,
                DefaultDrawingSupplier.DEFAULT_SHAPE_SEQUENCE));
        plot.getRangeAxis().setVisible(false);
        plot.getDomainAxis().setVisible(false);
        plot.setRangeGridlinesVisible(false);
        plot.setDomainPannable(true);
        plot.setRangePannable(true);
        plot.setBackgroundPaint(DV.background);
        plot.setDomainGridlinePaint(Color.GRAY);
        chart.removeLegend();
        chart.setBorderVisible(false);
        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setMouseWheelEnabled(true);

        return chartPanel;
    }
}
