import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.DefaultDrawingSupplier;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYIntervalSeriesCollection;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.RectangleInsets;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class DataVisualization
{
    public static void updateAngles()
    {

    }


    /**
     * Draws graphs for specified visualization
     * @param active domain, overlap, or threshold line that is actively changing
     */
    public static void drawGraphs(int active)
    {
        DV.graphPanel.removeAll();

        // check for scaling
        boolean upperScaling = false;
        boolean lowerScaling = false;

        // graph upper class
        upperScaling = addGraph(new ArrayList<>(List.of(DV.data.get(DV.upperClass))), 0, active);

        // graph lower classes
        if (DV.classNumber > 1)
        {
            // store all lower classes
            ArrayList<DataObject> dataObjects = new ArrayList<>();

            for (int j = 0; j < DV.classNumber; j++)
            {
                if (DV.lowerClasses.get(j))
                    dataObjects.add(DV.data.get(j));
            }

            lowerScaling = addGraph(dataObjects, 1, active);
        }

        // regenerate confusion matrices
        DV.allDataCM.removeAll();
        DV.dataWithoutOverlapCM.removeAll();
        DV.overlapCM.removeAll();
        DV.worstCaseCM.removeAll();
        ConfusionMatrices.generateConfusionMatrices();

        // revalidate graphs and confusion matrices
        DV.graphPanel.revalidate();
        DV.confusionMatrixPanel.revalidate();

        // warn user if graphs are scaled
        if (DV.showPopup && (upperScaling || lowerScaling))
        {
            JOptionPane.showMessageDialog(DV.mainFrame,
                    """
                            Because of the size, the graphs have been scaled.
                            All functionality remains, but differences in threshold, overlap, or domain line positions may be noticeable.
                            These positions are analytically the same, just not visually.
                            """,
                    "Zoom Warning", JOptionPane.INFORMATION_MESSAGE);
            DV.showPopup = false;
        }
    }


    /**
     * Draw graph with specified parameters
     * @param dataObjects classes to draw
     * @param upperOrLower draw up, else draw down
     * @param activate domain, overlap, or threshold line that is actively changing
     */
    public static boolean addGraph(ArrayList<DataObject> dataObjects, int upperOrLower, int activate)
    {
        for (DataObject dataObject : dataObjects)
            dataObject.updateCoordinates(DV.angles);

        // create main renderer and dataset
        XYLineAndShapeRenderer lineRenderer = new XYLineAndShapeRenderer(true, false);
        XYSeriesCollection dataset = new XYSeriesCollection();
        ArrayList<XYSeries> lines = new ArrayList<>();

        // create renderer for domain, overlap, and threshold lines
        XYLineAndShapeRenderer domainRenderer = new XYLineAndShapeRenderer(true, false);
        XYLineAndShapeRenderer overlapRenderer = new XYLineAndShapeRenderer(true, false);
        XYLineAndShapeRenderer thresholdRenderer = new XYLineAndShapeRenderer(true, false);
        XYSeriesCollection domain = new XYSeriesCollection();
        XYSeriesCollection overlap = new XYSeriesCollection();
        XYSeriesCollection threshold = new XYSeriesCollection();
        XYSeries domainMaxLine = new XYSeries(-1, false, true);
        XYSeries domainMinLine = new XYSeries(-2, false, true);
        XYSeries overlapMaxLine = new XYSeries(-3, false, true);
        XYSeries overlapMinLine = new XYSeries(-4, false, true);
        XYSeries thresholdLine = new XYSeries(0, false, true);

        // get domain line height
        double domainOverlapLineHeight = DV.fieldLength / 10.0;

        // set domain lines
        if (upperOrLower == 1)
        {
            domainMaxLine.add(DV.domainArea[0], 0);
            domainMaxLine.add(DV.domainArea[0], -domainOverlapLineHeight);
            domainMinLine.add(DV.domainArea[1], 0);
            domainMinLine.add(DV.domainArea[1], -domainOverlapLineHeight);
        }
        else
        {
            domainMaxLine.add(DV.domainArea[0], 0);
            domainMaxLine.add(DV.domainArea[0], domainOverlapLineHeight);
            domainMinLine.add(DV.domainArea[1], 0);
            domainMinLine.add(DV.domainArea[1], domainOverlapLineHeight);
        }

        // add domain series to collection
        domain.addSeries(domainMaxLine);
        domain.addSeries(domainMinLine);

        // set overlap lines
        if (upperOrLower == 1)
        {
            overlapMaxLine.add(DV.overlapArea[0], 0);
            overlapMaxLine.add(DV.overlapArea[0], -domainOverlapLineHeight);
            overlapMinLine.add(DV.overlapArea[1], 0);
            overlapMinLine.add(DV.overlapArea[1], -domainOverlapLineHeight);
        }
        else
        {
            overlapMaxLine.add(DV.overlapArea[0], 0);
            overlapMaxLine.add(DV.overlapArea[0], domainOverlapLineHeight);
            overlapMinLine.add(DV.overlapArea[1], 0);
            overlapMinLine.add(DV.overlapArea[1], domainOverlapLineHeight);
        }

        // add overlap series to collection
        overlap.addSeries(overlapMaxLine);
        overlap.addSeries(overlapMinLine);

        // get threshold line height
        double thresholdLineHeight = DV.fieldLength / 12.0;

        // get threshold line
        if (upperOrLower == 1)
        {
            if (DV.lowerRange == 0)
            {
                thresholdLine.add(DV.overlapArea[0], 0);
                thresholdLine.add(DV.overlapArea[0], -thresholdLineHeight);
            }
            else
            {
                thresholdLine.add(DV.overlapArea[1], 0);
                thresholdLine.add(DV.overlapArea[1], -thresholdLineHeight);
            }
        }
        else
        {
            if (DV.lowerRange == 0)
            {
                thresholdLine.add(DV.overlapArea[0], 0);
                thresholdLine.add(DV.overlapArea[0], thresholdLineHeight);
            }
            else
            {
                thresholdLine.add(DV.overlapArea[1], 0);
                thresholdLine.add(DV.overlapArea[1], thresholdLineHeight);
            }
        }

        // add threshold series to collection
        threshold.addSeries(thresholdLine);

        // renderer for endpoint, midpoint, and timeline
        XYLineAndShapeRenderer endpointRenderer = new XYLineAndShapeRenderer(false, true);
        XYLineAndShapeRenderer midpointRenderer = new XYLineAndShapeRenderer(false, true);
        XYLineAndShapeRenderer timeLineRenderer = new XYLineAndShapeRenderer(false, true);
        XYSeriesCollection endpoints = new XYSeriesCollection();
        XYSeriesCollection midpoints = new XYSeriesCollection();
        XYSeriesCollection timeLine = new XYSeriesCollection();
        XYSeries endpointSeries = new XYSeries(0, false, true);
        XYSeries midpointSeries = new XYSeries(1, false, true);
        XYSeries timeLineSeries = new XYSeries(2, false, true);
        int lineCnt = 0;

        // populate series
        for (DataObject data : dataObjects)
        {
            for (int j = 0; j < data.data.length; j++)
            {
                // start line at (0, 0)
                lines.add(new XYSeries(lineCnt, false, true));
                lines.get(lineCnt).add(0, 0);
                double endpoint = 0;

                // add points to lines
                for (int k = 0; k < DV.fieldLength; k++)
                {
                    if (upperOrLower == 1)
                    {
                        lines.get(lineCnt).add(data.coordinates[j][k][0], -data.coordinates[j][k][1]);

                        if (k > 0 && k < DV.fieldLength - 1 && DV.angles[j] == DV.angles[j + 1])
                            midpointSeries.add(data.coordinates[j][k][0], -data.coordinates[j][k][1]);
                    }
                    else
                    {
                        lines.get(lineCnt).add(data.coordinates[j][k][0], data.coordinates[j][k][1]);

                        if (k > 0 && k < DV.fieldLength - 1 && DV.angles[j] == DV.angles[j + 1])
                            midpointSeries.add(data.coordinates[j][k][0], data.coordinates[j][k][1]);
                    }

                    // add endpoint and timeline
                    if (k == DV.fieldLength - 1)
                    {
                        endpoint = data.coordinates[j][k][0];

                        if (!DV.domainActive || endpoint >= DV.domainArea[0] && endpoint <= DV.domainArea[1]) {
                            if (upperOrLower == 1)
                                endpointSeries.add(data.coordinates[j][k][0], -data.coordinates[j][k][1]);
                            else
                                endpointSeries.add(data.coordinates[j][k][0], data.coordinates[j][k][1]);

                            timeLineSeries.add(data.coordinates[j][k][0], 0);
                        }
                    }
                }

                // add to dataset if within domain
                if (!DV.domainActive || endpoint >= DV.domainArea[0] && endpoint <= DV.domainArea[1])
                    dataset.addSeries(lines.get(lineCnt));
            }
        }

        // add data to series
        endpoints.addSeries(endpointSeries);
        timeLine.addSeries(timeLineSeries);
        midpoints.addSeries(midpointSeries);

        JFreeChart chart = ChartFactory.createXYLineChart(
                "",
                "",
                "",
                dataset,
                PlotOrientation.VERTICAL,
                false,
                true,
                false);

        // format chart
        chart.setBorderVisible(false);
        chart.setPadding(RectangleInsets.ZERO_INSETS);

        // get plot
        XYPlot plot = (XYPlot) chart.getPlot();

        // format plot
        plot.setDrawingSupplier(new DefaultDrawingSupplier(
                new Paint[] { DV.graphColors[upperOrLower] },
                DefaultDrawingSupplier.DEFAULT_OUTLINE_PAINT_SEQUENCE,
                DefaultDrawingSupplier.DEFAULT_STROKE_SEQUENCE,
                DefaultDrawingSupplier.DEFAULT_OUTLINE_STROKE_SEQUENCE,
                DefaultDrawingSupplier.DEFAULT_SHAPE_SEQUENCE));
        plot.getRangeAxis().setVisible(false);
        plot.getDomainAxis().setVisible(false);
        plot.setRangeGridlinesVisible(true);
        plot.setOutlinePaint(null);
        plot.setOutlineVisible(false);
        plot.setInsets(RectangleInsets.ZERO_INSETS);

        // set domain and range of graph
        double bound = 1.2 * DV.fieldLength;

        if (upperOrLower == 1)
        {
            double tick = DV.fieldLength / 10.0;

            ValueAxis domainView = plot.getDomainAxis();
            domainView.setRange(-bound, bound);
            NumberAxis xAxis = (NumberAxis) plot.getDomainAxis();
            xAxis.setTickUnit(new NumberTickUnit(tick));

            ValueAxis rangeView = plot.getRangeAxis();
            rangeView.setRange(-bound * 0.4, 0);
            NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();
            yAxis.setTickUnit(new NumberTickUnit(tick));
        }
        else
        {
            double tick = DV.fieldLength / 10.0;

            ValueAxis domainView = plot.getDomainAxis();
            domainView.setRange(-bound, bound);
            NumberAxis xAxis = (NumberAxis) plot.getDomainAxis();
            xAxis.setTickUnit(new NumberTickUnit(tick));

            ValueAxis rangeView = plot.getRangeAxis();
            rangeView.setRange(0, bound * 0.4);
            NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();
            yAxis.setTickUnit(new NumberTickUnit(tick));
        }

        // renderer for bars
        XYIntervalSeriesCollection bars = new XYIntervalSeriesCollection();
        XYBarRenderer barRenderer = new XYBarRenderer();

        // create bar chart
        if (DV.showBars)
        {
            int[] barRanges = new int[100];
            double memberCnt = 0;

            // get member count
            for (DataObject dataObject : dataObjects)
                memberCnt += dataObject.data.length;

            for (DataObject data : dataObjects)
            {
                for (int j = 0; j < data.data.length; j++)
                {
                    double endpoint = data.coordinates[j][DV.fieldLength-1][0];
                    int rangeCnt = -1;
                    double sizeCnt = -DV.fieldLength;
                    while (endpoint > sizeCnt)
                    {
                        rangeCnt++;
                        sizeCnt+= DV.fieldLength / 50.0;
                    }

                     barRanges[rangeCnt]++;
                }
            }

            double interval = DV.fieldLength / 50.0;
            double tmpMaxInterval = -DV.fieldLength;
            double oldInterval = -DV.fieldLength;

            // add series to collection
        }

        return bound > DV.fieldLength;
    }


    public static void optimizeVisualization()
    {

    }

    public static void undoOptimization()
    {

    }

    public static void optimizeSetup()
    {

    }

    public static void getAccuracy()
    {

    }

    public static void getOverlap()
    {

    }
}
