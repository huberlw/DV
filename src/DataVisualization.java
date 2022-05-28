import org.jfree.chart.*;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.entity.XYItemEntity;
import org.jfree.chart.plot.DefaultDrawingSupplier;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYIntervalSeries;
import org.jfree.data.xy.XYIntervalSeriesCollection;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.RectangleInsets;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

public class DataVisualization
{
    /**
     * Uses Linear Discriminant Analysis (LDA)
     * to find the optimal angles and threshold
     */
    public static void optimizeSetup()
    {
        DV.domainArea = new double[] {-9, 9};

        DV.overlapArea = new double[] {-2, 2};

        DV.threshold = 200;
    }


    /**
     * Linear Discriminant Analysis (LDA)
     * @return optimal angles and threshold
     */
    private double[][] LDA()
    {


        return new double[0][];
    }


    public static void optimizeVisualization()
    {

    }

    public static void undoOptimization()
    {

    }


    public static void getAccuracy()
    {

    }

    public static void getOverlap()
    {

    }


    /**
     * Draws graphs for specified visualization
     * @param active domain, overlap, or threshold line that is actively changing
     */
    public static void drawGraphs(int active)
    {
        DV.graphPanel.removeAll();

        // check for scaling and graph upper class
        boolean upperScaling = addGraph(new ArrayList<>(List.of(DV.data.get(DV.upperClass))), 0, active);
        boolean lowerScaling = false;

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
     * @param upperOrLower draw up when upper (0) and down when lower (1)
     * @param active domain, overlap, or threshold line that is actively changing
     */
    public static boolean addGraph(ArrayList<DataObject> dataObjects, int upperOrLower, int active)
    {
        // get coordinates
        for (DataObject dataObject : dataObjects)
            dataObject.updateCoordinates(DV.angles);

        // create main renderer and dataset
        XYLineAndShapeRenderer lineRenderer = new XYLineAndShapeRenderer(true, false);
        XYSeriesCollection graphLines = new XYSeriesCollection();
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

        // number of lines
        int lineCnt = 0;

        // populate series
        for (DataObject data : dataObjects)
        {
            for (int i = 0; i < data.data.length; i++, lineCnt++)
            {
                // start line at (0, 0)
                lines.add(new XYSeries(lineCnt, false, true));
                lines.get(lineCnt).add(0, 0);
                double endpoint = 0;

                // add points to lines
                for (int j = 0; j < DV.fieldLength; j++)
                {
                    if (upperOrLower == 1)
                    {
                        lines.get(lineCnt).add(data.coordinates[i][j][0], -data.coordinates[i][j][1]);

                        if ((j > 0 && j < DV.fieldLength - 1) && (DV.angles[j] == DV.angles[j+1]))
                            midpointSeries.add(data.coordinates[i][j][0], -data.coordinates[i][j][1]);
                    }
                    else
                    {
                        lines.get(lineCnt).add(data.coordinates[i][j][0], data.coordinates[i][j][1]);

                        if ((j > 0 && j < DV.fieldLength - 1) && (DV.angles[j] == DV.angles[j+1]))
                            midpointSeries.add(data.coordinates[i][j][0], data.coordinates[i][j][1]);
                    }

                    // add endpoint and timeline
                    if (j == DV.fieldLength - 1)
                    {
                        endpoint = data.coordinates[i][j][0];

                        if (!DV.domainActive || endpoint >= DV.domainArea[0] && endpoint <= DV.domainArea[1]) {
                            if (upperOrLower == 1)
                                endpointSeries.add(data.coordinates[i][j][0], -data.coordinates[i][j][1]);
                            else
                                endpointSeries.add(data.coordinates[i][j][0], data.coordinates[i][j][1]);

                            timeLineSeries.add(data.coordinates[i][j][0], 0);
                        }
                    }
                }

                // add to dataset if within domain
                if (!DV.domainActive || endpoint >= DV.domainArea[0] && endpoint <= DV.domainArea[1])
                    graphLines.addSeries(lines.get(lineCnt));
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
                graphLines,
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
            int[] barRanges = new int[400];
            double memberCnt = 0;

            // get bar lengths
            for (DataObject dataObj : dataObjects)
            {
                // get member count
                memberCnt += dataObj.data.length;

                // translate endpoint to slider ticks
                // increment bar which endpoint lands
                for (int i = 0; i < dataObj.data.length; i++)
                    barRanges[(int) Math.round((dataObj.coordinates[i][DV.fieldLength-1][0] / DV.fieldLength * 200) + 200)]++;
            }

            // get interval and bounds
            double interval = DV.fieldLength / 200.0;
            double maxBound = -DV.fieldLength;
            double minBound = -DV.fieldLength;

            // get maximum bar height
            double maxBarHeight = DV.fieldLength / 9.0;

            // add series to collection
            for (int i = 0; i < 400; i++)
            {
                // get max bound
                maxBound += interval;

                XYIntervalSeries bar = new XYIntervalSeries(i, false, true);

                // bar width = interval
                // bar height = (total endpoints on bar) / (total endpoints)
                if (upperOrLower == 1)
                    bar.add(interval, minBound, maxBound, -barRanges[i] / memberCnt, -maxBarHeight, 0);
                else
                    bar.add(interval, minBound, maxBound, barRanges[i] / memberCnt, 0, maxBarHeight);

                bars.addSeries(bar);

                // set min bound to old max
                minBound = maxBound;
            }
        }

        // set strokes for active and inactive lines
        switch (active)
        {
            case 1 -> // threshold line is active
                    {
                        BasicStroke activeStroke = new BasicStroke(3f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{4f}, 0.0f);
                        BasicStroke inactiveOverlapStroke = new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{4f}, 0.0f);
                        BasicStroke inactiveDomainStroke = new BasicStroke(1.5f);

                        thresholdRenderer.setBaseStroke(activeStroke);
                        domainRenderer.setBaseStroke(inactiveDomainStroke);
                        overlapRenderer.setBaseStroke(inactiveOverlapStroke);
                    }
            case 2 -> // domain lines are active
                    {
                        BasicStroke inactiveStroke = new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{4f}, 0.0f);
                        BasicStroke activeStroke = new BasicStroke(3f);

                        thresholdRenderer.setBaseStroke(inactiveStroke);
                        domainRenderer.setBaseStroke(activeStroke);
                        overlapRenderer.setBaseStroke(inactiveStroke);
                    }
            case 3 -> // overlap lines are active
                    {
                        BasicStroke activeStroke = new BasicStroke(3f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{4f}, 0.0f);
                        BasicStroke inactiveThresholdStroke = new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{4f}, 0.0f);
                        BasicStroke inactiveDomainStroke = new BasicStroke(1.5f);

                        thresholdRenderer.setBaseStroke(inactiveThresholdStroke);
                        domainRenderer.setBaseStroke(inactiveDomainStroke);
                        overlapRenderer.setBaseStroke(activeStroke);
                    }
            default -> // nothing is active
                    {
                        BasicStroke thresholdOverlapStroke = new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[] {4f}, 0.0f);
                        BasicStroke domainStroke = new BasicStroke(1.5f);

                        thresholdRenderer.setBaseStroke(thresholdOverlapStroke);
                        domainRenderer.setBaseStroke(domainStroke);
                        overlapRenderer.setBaseStroke(thresholdOverlapStroke);
                    }
        }

        // set threshold renderer and dataset
        thresholdRenderer.setSeriesPaint(0, DV.thresholdLine);
        plot.setRenderer(0, thresholdRenderer);
        plot.setDataset(0, threshold);

        // set overlap renderer and dataset
        overlapRenderer.setSeriesPaint(0, DV.overlapLines);
        overlapRenderer.setSeriesPaint(1, DV.overlapLines);
        plot.setRenderer(1, overlapRenderer);
        plot.setDataset(1, overlap);

        // set domain renderer and dataset
        domainRenderer.setSeriesPaint(0, DV.domainLines);
        domainRenderer.setSeriesPaint(1, DV.domainLines);
        plot.setRenderer(2, domainRenderer);
        plot.setDataset(2, domain);

        // set line renderer and dataset
        lineRenderer.setBaseStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        lineRenderer.setAutoPopulateSeriesStroke(false);
        plot.setRenderer(3, lineRenderer);
        plot.setDataset(3, graphLines);

        // set endpoint renderer and dataset
        endpointRenderer.setSeriesShape(0, new Ellipse2D.Double(-2, -2, 4, 4));
        plot.setRenderer(4, endpointRenderer);
        plot.setDataset(4, endpoints);

        // set bar or timeline renderer and dataset
        if (DV.showBars)
        {
            plot.setRenderer(5, barRenderer);
            plot.setDataset(5, bars);
        }
        else
        {
            if (upperOrLower == 1)
                timeLineRenderer.setSeriesShape(0, new Rectangle2D.Double(-0.25, 0, 0.5, 3));
            else
                timeLineRenderer.setSeriesShape(0, new Rectangle2D.Double(-0.25, -3, 0.5, 3));

            plot.setRenderer(5, timeLineRenderer);
            plot.setDataset(5, timeLine);
        }

        // set midpoint renderer and dataset
        midpointRenderer.setSeriesShape(0, new Ellipse2D.Double(-1.5, -1.5, 3, 3));
        midpointRenderer.setSeriesPaint(0, Color.BLACK);
        plot.setRenderer(6, midpointRenderer);
        plot.setDataset(6, midpoints);

        // create the graph panel and add it to the main panel
        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setMouseWheelEnabled(true);
        chartPanel.setPreferredSize(new Dimension(Resolutions.singleChartPanel[0], Resolutions.singleChartPanel[1]));

        chartPanel.addChartMouseListener(new ChartMouseListener()
        {
            // display point on click
            @Override
            public void chartMouseClicked(ChartMouseEvent e)
            {
                // get clicked entity
                ChartEntity ce = e.getEntity();

                if (ce instanceof XYItemEntity xy)
                {
                    // get class and index
                    int curClass = 0;
                    int index = xy.getSeriesIndex();

                    // if upper class than curClass is upperClass
                    // otherwise search for class
                    if (upperOrLower == 1)
                    {
                        // -1 because index start at 0, not 1
                        int cnt = -1;

                        // loop through classes until the class containing index is found
                        for (int i = 0; i < DV.classNumber; i++)
                        {
                            if (i != DV.upperClass)
                            {
                                cnt += DV.data.get(i).data.length;

                                // if true, index is within the current class
                                if (cnt >= index)
                                {
                                    curClass = i;
                                    index = cnt - index;
                                    break;
                                }
                            }
                        }
                    }
                    else
                        curClass = DV.upperClass;

                    // create points
                    StringBuilder originalPoint = new StringBuilder("<b>Original Point: </b>");
                    StringBuilder normalPoint = new StringBuilder("<b>Normalized Point: </b>");

                    for (int i = 0; i < DV.fieldLength; i++)
                    {
                        // get feature values
                        String tmpOrig = String.format("%.2f", DV.originalData.get(curClass).data[index][i]);
                        String tmpNorm = String.format("%.2f", DV.data.get(curClass).data[index][i]);

                        // add values to points
                        originalPoint.append(tmpOrig);
                        normalPoint.append(tmpNorm);

                        if (i != DV.fieldLength - 1)
                        {
                            originalPoint.append(", ");
                            normalPoint.append(", ");
                        }

                        // add new line
                        if (i % 10 == 9)
                        {
                            originalPoint.append("<br/>&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;");
                            normalPoint.append("<br/>&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;");
                        }
                    }

                    // create message
                    String chosenDataPoint = "<html>" + "<b>Class: </b>" + DV.uniqueClasses.get(curClass) + "<br/>" + originalPoint + "<br/>" + normalPoint + "</html>";

                    // get mouse location
                    Point mouseLoc = MouseInfo.getPointerInfo().getLocation();
                    int x = (int) mouseLoc.getX();
                    int y = (int) mouseLoc.getY();

                    // create popup
                    JOptionPane optionPane = new JOptionPane(chosenDataPoint, JOptionPane.INFORMATION_MESSAGE);
                    JDialog dialog = optionPane.createDialog(null, "Datapoint");
                    dialog.setLocation(x, y);
                    dialog.setVisible(true);
                }
            }

            @Override
            public void chartMouseMoved(ChartMouseEvent chartMouseEvent) {}
        });

        DV.graphPanel.add(chartPanel);

        return bound > DV.fieldLength;
    }
}
