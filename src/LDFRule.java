import org.jfree.chart.*;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.labels.StandardXYItemLabelGenerator;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.RectangleEdge;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

public class LDFRule
{
    // classification rule for LDF
    JLabel rules;

    // combined graph for LDF
    JPanel singleGraphPanel;

    // separated graphs for LDF
    JPanel doubleGraphPanel;

    // purity of LDF
    JLabel purity;

    // container for graphs
    JTabbedPane graphTabs = new JTabbedPane();

    // whether the user has clicked mouse 2 to begin drawing strip
    boolean clicked = false;

    // number of rules
    int rIndex = 0;

    // whether the drawing has started or not
    boolean start = false;

    // rectangular area of LDF rules
    ArrayList<double[][]> ruleStrips = new ArrayList<>();

    // LDF rules rendering tools
    XYLineAndShapeRenderer stripRenderer = new XYLineAndShapeRenderer(true, false);
    final XYSeriesCollection strips = new XYSeriesCollection();

    // indexes of the lower and upper bound of a strip
    ArrayList<int[]> ruleStripRanges = new ArrayList<>();

    // datapoints counter for upper and lower DV graphs
    ArrayList<double[]> ruleStripClassification = new ArrayList<>();


    /**
     * Constructor for LDFRule. Visualizes LDF.
     */
    public LDFRule()
    {
        JFrame ldfFrame = new JFrame();
        ldfFrame.setLocationRelativeTo(DV.mainFrame);
        ldfFrame.setLayout(new GridBagLayout());
        GridBagConstraints ldfc = new GridBagConstraints();

        rules = new JLabel();
        rules.setText("<html><b>Generalized Rule:</b><br/></html>");
        rules.setFont(rules.getFont().deriveFont(16f));

        ldfc.gridx = 0;
        ldfc.gridy = 0;
        ldfc.weightx = 0.95;
        ldfc.weighty = 0;
        ldfc.fill = GridBagConstraints.BOTH;
        ldfFrame.add(rules, ldfc);

        JButton removeRules = new JButton("Remove Rules");
        removeRules.addActionListener(rr ->
        {
            rIndex = 0;
            start = false;
            clicked = false;
            ruleStrips.clear();
            strips.removeAllSeries();

            rules.setText("<html><b>Generalized Rule:</b><br/></html>");
            purity.setText("<html><b>Strip Accuracy:</b><br/></html>");
        });

        ldfc.gridx = 1;
        ldfc.weightx = 0.05;
        ldfFrame.add(removeRules, ldfc);

        singleGraphPanel = new JPanel();
        singleGraphPanel.setLayout(new BoxLayout(singleGraphPanel, BoxLayout.PAGE_AXIS));
        doubleGraphPanel = new JPanel();
        doubleGraphPanel.setLayout(new BoxLayout(doubleGraphPanel, BoxLayout.PAGE_AXIS));

        graphTabs.add("Combined Graph", singleGraphPanel);
        graphTabs.add("Separate Graphs", doubleGraphPanel);

        ldfc.gridx = 0;
        ldfc.gridy = 1;
        ldfc.weightx = 1;
        ldfc.weighty = 1;
        ldfc.gridwidth = 2;
        ldfFrame.add(graphTabs, ldfc);

        purity = new JLabel();
        purity.setText("<html><b>Strip Accuracy:</b><br/></html>");
        purity.setFont(purity.getFont().deriveFont(16f));

        ldfc.gridy = 2;
        ldfc.weighty = 0;
        ldfFrame.add(purity, ldfc);

        // draw graphs
        drawGraphs();

        // show
        ldfFrame.setVisible(true);
        ldfFrame.revalidate();
        ldfFrame.pack();
        ldfFrame.repaint();
    }


    /**
     * Gets classifications, ranges, rules, and purity of each strip.
     */
    private void stripAnalysis()
    {
        ruleStripClassification.clear();
        ruleStripRanges.clear();

        for (int i = 0; i < ruleStrips.size(); i++)
        {
            ruleStripRanges.add(new int[]{ -1, -1, -1, -1 });
            ruleStripClassification.add(new double[]{ 0, 0 });
        }

        // get strip bounds
        // check if a point is in strip 1, 2, 3... etc.
        // save smallest and largest point in every strip
        // check classification of strip
        // if strip classification is both = then class ERROR
        // establish strip rules based on smallest and largest point
        // calculate strip purity

        for (int i = 0; i < ruleStrips.size(); i++)
        {
            for (int j = 0; j < DV.trainData.size(); j++)
            {
                for (int k = 0; k < DV.trainData.get(j).coordinates.length; k++)
                {
                    // check if within strip
                    boolean inDomain = false;
                    boolean inRange = false;

                    double x = DV.trainData.get(j).coordinates[k][DV.fieldLength-1][0];
                    double y = DV.trainData.get(j).coordinates[k][DV.fieldLength-1][1];

                    if (ruleStrips.get(i)[0][0] < ruleStrips.get(i)[1][0])
                    {
                        // check if within domain
                        if (x >= ruleStrips.get(i)[0][0] && x < ruleStrips.get(i)[1][0])
                        {
                            inDomain = true;
                        }
                    }
                    else
                    {
                        // check if within domain
                        if (x < ruleStrips.get(i)[0][0] && x >= ruleStrips.get(i)[1][0])
                        {
                            inDomain = true;
                        }
                    }

                    if (ruleStrips.get(i)[0][1] < ruleStrips.get(i)[1][1])
                    {
                        // check if within range
                        if (y >= ruleStrips.get(i)[0][1] && y < ruleStrips.get(i)[1][1])
                        {
                            inRange = true;
                        }
                    }
                    else
                    {
                        // check if within range
                        if (y < ruleStrips.get(i)[0][1] && y >= ruleStrips.get(i)[1][1])
                        {
                            inRange = true;
                        }
                    }

                    // if within strip
                    if (inDomain && inRange)
                    {
                        if (j == DV.upperClass)
                        {
                            ruleStripClassification.get(i)[1]++;
                        }
                        else if (DV.lowerClasses.get(j))
                        {
                            ruleStripClassification.get(i)[0]++;
                        }

                        if (ruleStripRanges.get(i)[0] == -1)
                        {
                            ruleStripRanges.get(i)[0] = j;
                            ruleStripRanges.get(i)[1] = k;
                            ruleStripRanges.get(i)[2] = j;
                            ruleStripRanges.get(i)[3] = k;
                        }
                        else
                        {
                            double lowB = DV.trainData.get(ruleStripRanges.get(i)[0]).coordinates[ruleStripRanges.get(i)[1]][DV.fieldLength-1][0];
                            double highB = DV.trainData.get(ruleStripRanges.get(i)[2]).coordinates[ruleStripRanges.get(i)[3]][DV.fieldLength-1][0];

                            if (x < lowB)
                            {
                                ruleStripRanges.get(i)[0] = j;
                                ruleStripRanges.get(i)[1] = k;
                            }
                            else if (x > highB)
                            {
                                ruleStripRanges.get(i)[2] = j;
                                ruleStripRanges.get(i)[3] = k;
                            }
                        }
                    }
                }
            }
        }

        getStripRules();
        getPurity();
    }


    /**
     * Gets rules of each strip.
     */
    private void getStripRules()
    {
        String upperClass = DV.trainData.get(DV.upperClass).className;
        StringBuilder lowerClasses = new StringBuilder();

        for (int i = 0; i < DV.trainData.size(); i++)
        {
            if (DV.lowerClasses.get(i))
            {
                lowerClasses.append(DV.trainData.get(i).className);

                if (i != DV.trainData.size() - 1)
                    lowerClasses.append(", ");
            }
        }

        StringBuilder sb = new StringBuilder("<html><b>Generalized Rule:</b><br/> ");

        for (int i = 0; i < ruleStrips.size(); i++)
        {
            sb.append("<b>Strip ").append(i).append(":</b> ");

            for (int j = 0; j < DV.fieldLength; j++)
            {
                sb.append(String.format("%.2f", DV.trainData.get(ruleStripRanges.get(i)[0]).data[ruleStripRanges.get(i)[1]][j])).append(" &le; x");
                sb.append(j).append(" &le; ").append(String.format("%.2f", DV.trainData.get(ruleStripRanges.get(i)[2]).data[ruleStripRanges.get(i)[3]][j]));

                if (j != DV.fieldLength - 1) sb.append(", ");
            }

            sb.append(" then x belongs to class ");

            double rangeLow, rangeHigh;

            if (ruleStrips.get(i)[0][0] < ruleStrips.get(i)[1][0])
            {
                rangeLow = ruleStrips.get(i)[0][0];
                rangeHigh = ruleStrips.get(i)[1][0];
            }
            else
            {
                rangeHigh = ruleStrips.get(i)[0][0];
                rangeLow = ruleStrips.get(i)[1][0];
            }

            if (DV.upperIsLower)
            {
                if (rangeLow <= DV.threshold && rangeHigh <= DV.threshold)
                    sb.append(upperClass);
                else if (rangeLow > DV.threshold && rangeHigh > DV.threshold)
                    sb.append(lowerClasses);
                else
                    sb.append("ERROR: strip overlaps threshold");
            }
            else
            {
                if (rangeLow <= DV.threshold && rangeHigh <= DV.threshold)
                    sb.append(lowerClasses);
                else if (rangeLow > DV.threshold && rangeHigh > DV.threshold)
                    sb.append(upperClass);
                else
                    sb.append("ERROR: strip overlaps threshold");
            }

            if (i != ruleStrips.size() - 1)
                sb.append("<br/>");
            else
                sb.append("</html>");
        }

        rules.setText(sb.toString());
    }


    /**
     * Gets purity of each strip.
     */
    private void getPurity()
    {
        StringBuilder sb = new StringBuilder("<html><b>Strip Accuracy:</b><br/> ");

        for (int i = 0; i < ruleStrips.size(); i++)
        {
            int classification = getClassification(i);

            String accuracy = getAccuracyLabel(classification, i);

            sb.append("<b>Strip ").append(i).append(":</b> ").append(accuracy).append("%");

            if (i != ruleStrips.size() - 1)
                sb.append("<br/>");
            else
                sb.append("</html>");
        }

        purity.setText(sb.toString());
    }

    /**
     * Label for accuracy
     * @param cls class of strip
     * @param i rule strip index
     * @return accuracy label
     */
    private String getAccuracyLabel(int cls, int i)
    {
        String accuracy;
        if (cls == 0)
            accuracy = String.format("%.2f", (ruleStripClassification.get(i)[0] / (ruleStripClassification.get(i)[0] + ruleStripClassification.get(i)[1])) * 100);
        else if (cls == 1)
            accuracy = String.format("%.2f", (ruleStripClassification.get(i)[1] / (ruleStripClassification.get(i)[0] + ruleStripClassification.get(i)[1])) * 100);
        else
            accuracy = "ERROR: strip overlaps threshold";

        return accuracy;
    }


    /**
     * Get class of rule strip
     * @param i rule strip index
     * @return class
     */
    private int getClassification(int i)
    {
        double rangeLow, rangeHigh;
        if (ruleStrips.get(i)[0][0] < ruleStrips.get(i)[1][0])
        {
            rangeLow = ruleStrips.get(i)[0][0];
            rangeHigh = ruleStrips.get(i)[1][0];
        }
        else
        {
            rangeHigh = ruleStrips.get(i)[0][0];
            rangeLow = ruleStrips.get(i)[1][0];
        }

        int classification;

        if (DV.upperIsLower)
        {
            if (rangeLow <= DV.threshold && rangeHigh <= DV.threshold)
                classification = 1;
            else if (rangeLow > DV.threshold && rangeHigh > DV.threshold)
                classification = 0;
            else
                classification = -1;
        }
        else
        {
            if (rangeLow <= DV.threshold && rangeHigh <= DV.threshold)
                classification = 0;
            else if (rangeLow > DV.threshold && rangeHigh > DV.threshold)
                classification = 1;
            else
                classification = -1;
        }

        return classification;
    }


    /**
     * Draws Combined and Separate graphs.
     */
    private void drawGraphs()
    {
        // holds classes to be graphed
        ArrayList<ArrayList<DataObject>> objects = new ArrayList<>();
        ArrayList<DataObject> upperObjects = new ArrayList<>(List.of(DV.trainData.get(DV.upperClass)));
        ArrayList<DataObject> lowerObjects = new ArrayList<>();

        // get classes to be graphed
        if (DV.hasClasses)
        {
            for (int j = 0; j < DV.classNumber; j++)
            {
                if (DV.lowerClasses.get(j))
                    lowerObjects.add(DV.trainData.get(j));
            }
        }

        objects.add(upperObjects);
        objects.add(lowerObjects);

        singleGraphPanel.removeAll();
        singleGraphPanel.add(addCombinedGraph(objects));

        doubleGraphPanel.removeAll();
        doubleGraphPanel.add(addSingleGraph(upperObjects, 0));
        doubleGraphPanel.add(addSingleGraph(lowerObjects, 1));

        getStripRules();
        getPurity();
    }


    /**
     * Creates Combined graph.
     * @param obj data to be visualized
     * @return visualization panel
     */
    private ChartPanel addCombinedGraph(ArrayList<ArrayList<DataObject>> obj)
    {
        // create main renderer and dataset
        XYLineAndShapeRenderer lineRenderer = new XYLineAndShapeRenderer(true, false);
        XYSeriesCollection graphLines = new XYSeriesCollection();

        // create renderer for threshold line
        XYLineAndShapeRenderer thresholdRenderer = new XYLineAndShapeRenderer(true, false);
        XYSeriesCollection threshold = new XYSeriesCollection();
        XYSeries thresholdLine = new XYSeries(0, false, true);

        // get threshold line
        thresholdLine.add(DV.threshold, 0);
        thresholdLine.add(DV.threshold, DV.fieldLength);

        // add threshold series to collection
        threshold.addSeries(thresholdLine);

        // renderer for endpoint, midpoint, and timeline
        XYLineAndShapeRenderer endpointRenderer = new XYLineAndShapeRenderer(false, true);
        XYLineAndShapeRenderer midpointRenderer = new XYLineAndShapeRenderer(false, true);
        XYLineAndShapeRenderer timeLineRenderer = new XYLineAndShapeRenderer(false, true);
        XYSeriesCollection endpoints = new XYSeriesCollection();
        XYSeriesCollection midpoints = new XYSeriesCollection();
        XYSeriesCollection timeLine = new XYSeriesCollection();
        XYSeries midpointSeries = new XYSeries(0, false, true);

        double buffer = DV.fieldLength / 10.0;

        // populate main series
        for (int d = 0, lineCnt = -1; d < obj.size(); d++)
        {
            for (DataObject data : obj.get(d))
            {
                for (int i = 0; i < data.data.length; i++)
                {
                    double endpoint = data.coordinates[i][data.coordinates[i].length - 1][0];

                    // ensure datapoint is within domain
                    // if drawing overlap, ensure datapoint is within overlap
                    if ((!DV.domainActive || endpoint >= DV.domainArea[0] && endpoint <= DV.domainArea[1]) &&
                            (!DV.drawOverlap || (DV.overlapArea[0] <= endpoint && endpoint <= DV.overlapArea[1])))
                    {
                        // start line at (0, 0)
                        XYSeries line = new XYSeries(++lineCnt, false, true);
                        XYSeries endpointSeries = new XYSeries(lineCnt, false, true);
                        XYSeries timeLineSeries = new XYSeries(lineCnt, false, true);

                        if (DV.showFirstSeg)
                            line.add(0, buffer);

                        // add points to lines
                        for (int j = 0; j < data.coordinates[i].length; j++)
                        {
                            line.add(data.coordinates[i][j][0], data.coordinates[i][j][1] + buffer);

                            if (j > 0 && j < data.coordinates[i].length - 1 && DV.angles[j] == DV.angles[j + 1])
                                midpointSeries.add(data.coordinates[i][j][0], data.coordinates[i][j][1] + buffer);

                            // add endpoint and timeline
                            if (j == data.coordinates[i].length - 1)
                            {
                                endpointSeries.add(data.coordinates[i][j][0], data.coordinates[i][j][1] + buffer);
                                timeLineSeries.add(data.coordinates[i][j][0], 0);

                                // add series
                                graphLines.addSeries(line);
                                endpoints.addSeries(endpointSeries);
                                timeLine.addSeries(timeLineSeries);


                                // set series paint
                                endpointRenderer.setSeriesPaint(lineCnt, DV.endpoints);
                                lineRenderer.setSeriesPaint(lineCnt, DV.graphColors[d]);
                                timeLineRenderer.setSeriesPaint(lineCnt, DV.graphColors[d]);

                                endpointRenderer.setSeriesShape(lineCnt, new Ellipse2D.Double(-1, -1, 2, 2));
                                timeLineRenderer.setSeriesShape(lineCnt, new Rectangle2D.Double(-0.25, -3, 0.5, 3));
                            }
                        }
                    }
                }
            }
        }

        // add data to series
        midpoints.addSeries(midpointSeries);

        // get chart and plot
        JFreeChart chart = ChartsAndPlots.createChart(graphLines, false);
        XYPlot plot = ChartsAndPlots.createPlot(chart, 0);

        // set domain and range of graph
        double bound = DV.fieldLength;

        // set domain
        ValueAxis domainView = plot.getDomainAxis();
        domainView.setRange(-bound, bound);
        NumberAxis xAxis = (NumberAxis) plot.getDomainAxis();
        xAxis.setTickUnit(new NumberTickUnit(buffer));

        // set range
        ValueAxis rangeView = plot.getRangeAxis();
        NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();
        yAxis.setTickUnit(new NumberTickUnit(buffer));
        rangeView.setRange(0, bound * (DV.mainPanel.getHeight() * 0.7) / (DV.graphPanel.getWidth() * 0.8));

        // create basic strokes
        BasicStroke thresholdOverlapStroke = new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[] {12f, 6f}, 0.0f);

        // set strip renderer and dataset
        plot.setRenderer(0, stripRenderer);
        plot.setDataset(0, strips);
        stripRenderer.setBaseItemLabelsVisible(true);
        stripRenderer.setBaseItemLabelGenerator(stripLabelGenerator());

        // set endpoint renderer and dataset
        plot.setRenderer(1, endpointRenderer);
        plot.setDataset(1, endpoints);

        // set midpoint renderer and dataset
        midpointRenderer.setSeriesShape(0, new Ellipse2D.Double(-0.5, -0.5, 1, 1));
        midpointRenderer.setSeriesPaint(0, DV.endpoints);
        plot.setRenderer(2, midpointRenderer);
        plot.setDataset(2, midpoints);

        // set threshold renderer and dataset
        thresholdRenderer.setSeriesStroke(0, thresholdOverlapStroke);
        thresholdRenderer.setSeriesPaint(0, DV.thresholdLine);
        plot.setRenderer(3, thresholdRenderer);
        plot.setDataset(3, threshold);

        // set line renderer and dataset
        lineRenderer.setBaseStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        lineRenderer.setAutoPopulateSeriesStroke(false);
        plot.setRenderer(4, lineRenderer);
        plot.setDataset(4, graphLines);

        plot.setRenderer(5, timeLineRenderer);
        plot.setDataset(5, timeLine);

        return getChartPanel(chart);
    }


    /**
     * Creates Separate graph.
     * @param obj data to be visualized
     * @param curClass current class being visualized
     * @return visualization panel
     */
    private ChartPanel addSingleGraph(ArrayList<DataObject> obj, int curClass)
    {
        // create main renderer and dataset
        XYLineAndShapeRenderer lineRenderer = new XYLineAndShapeRenderer(true, false);
        XYSeriesCollection graphLines = new XYSeriesCollection();

        // create renderer for threshold line
        XYLineAndShapeRenderer thresholdRenderer = new XYLineAndShapeRenderer(true, false);
        XYSeriesCollection threshold = new XYSeriesCollection();
        XYSeries thresholdLine = new XYSeries(0, false, true);

        // get threshold line
        thresholdLine.add(DV.threshold, 0);
        thresholdLine.add(DV.threshold, DV.fieldLength);

        // add threshold series to collection
        threshold.addSeries(thresholdLine);

        // renderer for endpoint, midpoint, and timeline
        XYLineAndShapeRenderer endpointRenderer = new XYLineAndShapeRenderer(false, true);
        XYLineAndShapeRenderer midpointRenderer = new XYLineAndShapeRenderer(false, true);
        XYLineAndShapeRenderer timeLineRenderer = new XYLineAndShapeRenderer(false, true);
        XYSeriesCollection endpoints = new XYSeriesCollection();
        XYSeriesCollection midpoints = new XYSeriesCollection();
        XYSeriesCollection timeLine = new XYSeriesCollection();
        XYSeries midpointSeries = new XYSeries(0, false, true);

        double buffer = DV.fieldLength / 10.0;

        // populate main series
        for (int q = 0, lineCnt = -1; q < obj.size(); q++)
        {
            DataObject data = obj.get(q);

            for (int i = 0; i < data.data.length; i++)
            {
                double endpoint = data.coordinates[i][data.coordinates[i].length - 1][0];

                // ensure datapoint is within domain
                // if drawing overlap, ensure datapoint is within overlap
                if ((!DV.domainActive || endpoint >= DV.domainArea[0] && endpoint <= DV.domainArea[1]) &&
                        (!DV.drawOverlap || (DV.overlapArea[0] <= endpoint && endpoint <= DV.overlapArea[1])))
                {
                    // start line at (0, 0)
                    XYSeries line = new XYSeries(++lineCnt, false, true);
                    XYSeries endpointSeries = new XYSeries(lineCnt, false, true);
                    XYSeries timeLineSeries = new XYSeries(lineCnt, false, true);

                    if (DV.showFirstSeg)
                        line.add(0, buffer);

                    // add points to lines
                    for (int j = 0; j < data.coordinates[i].length; j++)
                    {
                        line.add(data.coordinates[i][j][0], data.coordinates[i][j][1] + buffer);

                        if (j > 0 && j < data.coordinates[i].length - 1 && DV.angles[j] == DV.angles[j + 1])
                            midpointSeries.add(data.coordinates[i][j][0], data.coordinates[i][j][1] + buffer);

                        // add endpoint and timeline
                        if (j == data.coordinates[i].length - 1)
                        {
                            endpointSeries.add(data.coordinates[i][j][0], data.coordinates[i][j][1] + buffer);
                            timeLineSeries.add(data.coordinates[i][j][0], 0);

                            // add series
                            graphLines.addSeries(line);
                            endpoints.addSeries(endpointSeries);
                            timeLine.addSeries(timeLineSeries);


                            // set series paint
                            endpointRenderer.setSeriesPaint(lineCnt, DV.endpoints);
                            lineRenderer.setSeriesPaint(lineCnt, DV.graphColors[curClass]);
                            timeLineRenderer.setSeriesPaint(lineCnt, DV.graphColors[curClass]);

                            endpointRenderer.setSeriesShape(lineCnt, new Ellipse2D.Double(-1, -1, 2, 2));
                            timeLineRenderer.setSeriesShape(lineCnt, new Rectangle2D.Double(-0.25, -3, 0.5, 3));
                        }
                    }
                }
            }
        }

        // add data to series
        midpoints.addSeries(midpointSeries);

        // get chart and plot
        JFreeChart chart = ChartsAndPlots.createChart(graphLines, false);
        XYPlot plot = ChartsAndPlots.createPlot(chart, 0);

        // set domain and range of graph
        double bound = DV.fieldLength;

        // set domain
        ValueAxis domainView = plot.getDomainAxis();
        domainView.setRange(-bound, bound);
        NumberAxis xAxis = (NumberAxis) plot.getDomainAxis();
        xAxis.setTickUnit(new NumberTickUnit(buffer));

        // set range
        ValueAxis rangeView = plot.getRangeAxis();
        NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();
        yAxis.setTickUnit(new NumberTickUnit(buffer));
        rangeView.setRange(0, bound * (DV.mainPanel.getHeight() * 0.7) / (DV.graphPanel.getWidth() * 0.8));

        // create basic strokes
        BasicStroke thresholdOverlapStroke = new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[] {12f, 6f}, 0.0f);

        // set strip renderer and dataset
        plot.setRenderer(0, stripRenderer);
        plot.setDataset(0, strips);
        stripRenderer.setBaseItemLabelsVisible(true);
        stripRenderer.setBaseItemLabelGenerator(stripLabelGenerator());

        // set endpoint renderer and dataset
        plot.setRenderer(1, endpointRenderer);
        plot.setDataset(1, endpoints);

        // set midpoint renderer and dataset
        midpointRenderer.setSeriesShape(0, new Ellipse2D.Double(-0.5, -0.5, 1, 1));
        midpointRenderer.setSeriesPaint(0, DV.endpoints);
        plot.setRenderer(2, midpointRenderer);
        plot.setDataset(2, midpoints);

        // set threshold renderer and dataset
        thresholdRenderer.setSeriesStroke(0, thresholdOverlapStroke);
        thresholdRenderer.setSeriesPaint(0, DV.thresholdLine);
        plot.setRenderer(3, thresholdRenderer);
        plot.setDataset(3, threshold);

        // set line renderer and dataset
        lineRenderer.setBaseStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        lineRenderer.setAutoPopulateSeriesStroke(false);
        plot.setRenderer(4, lineRenderer);
        plot.setDataset(4, graphLines);

        plot.setRenderer(5, timeLineRenderer);
        plot.setDataset(5, timeLine);

        return getChartPanel(chart);
    }


    /**
     * Creates ChartPanel with mouse listeners from chart
     * @param chart visualization chart
     * @return ChartPanel with mouse listeners
     */
    private ChartPanel getChartPanel(JFreeChart chart)
    {
        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPopupMenu(null);
        chartPanel.setMouseZoomable(false);
        chartPanel.restoreAutoBounds();

        chartPanel.addMouseWheelListener(e ->
        {
            if (e.getWheelRotation() > 0)
                chartPanel.zoomOutDomain(0.5, 0.5);
            else if (e.getWheelRotation() < 0)
                chartPanel.zoomInDomain(1.5, 1.5);
        });

        chartPanel.addChartMouseListener(new ChartMouseListener()
        {
            @Override
            public void chartMouseClicked(ChartMouseEvent chartMouseEvent) {}

            @Override
            public void chartMouseMoved(ChartMouseEvent cme)
            {
                if(clicked)
                {
                    Rectangle2D dataArea = chartPanel.getScreenDataArea();
                    JFreeChart chart = cme.getChart();
                    XYPlot plot = (XYPlot) chart.getPlot();
                    ValueAxis xAxis = plot.getDomainAxis();
                    ValueAxis yAxis = plot.getRangeAxis();

                    double curX = cme.getTrigger().getX();
                    double curY = cme.getTrigger().getY();

                    double newX = xAxis.java2DToValue(curX, dataArea, RectangleEdge.BOTTOM);
                    double newY = yAxis.java2DToValue(curY, dataArea, RectangleEdge.LEFT);

                    System.out.println("Coordinates: " + newX + " " + newY);

                    if (!start)
                    {
                        start = true;

                        double[][] tmpPnt = new double[2][2];
                        tmpPnt[0] = new double[]{ newX, newY };
                        tmpPnt[1] = new double[] { 0, 0 };

                        ruleStrips.add(tmpPnt);

                        XYSeries strip = new XYSeries(rIndex, false, true);
                        strip.add(tmpPnt[0][0], tmpPnt[0][1]);

                        strips.addSeries(strip);
                        stripRenderer.setSeriesPaint(rIndex, new Color(255, 0, 0, 100));
                        stripRenderer.setSeriesStroke(rIndex, new BasicStroke(4f));
                    }
                    else
                    {
                        ruleStrips.get(rIndex)[1][0] = newX;
                        ruleStrips.get(rIndex)[1][1] = newY;

                        strips.removeSeries(rIndex);

                        XYSeries strip = getXySeries();

                        strips.addSeries(strip);
                    }
                }
            }

            private XYSeries getXySeries()
            {
                XYSeries strip = new XYSeries(rIndex, false, true);
                strip.add(ruleStrips.get(rIndex)[0][0], ruleStrips.get(rIndex)[0][1]);
                strip.add(ruleStrips.get(rIndex)[1][0], ruleStrips.get(rIndex)[0][1]);
                strip.add(ruleStrips.get(rIndex)[1][0], ruleStrips.get(rIndex)[1][1]);
                strip.add(ruleStrips.get(rIndex)[0][0], ruleStrips.get(rIndex)[1][1]);
                strip.add(ruleStrips.get(rIndex)[0][0], ruleStrips.get(rIndex)[0][1]);
                return strip;
            }
        });

        chartPanel.addMouseListener(new MouseListener()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                if (SwingUtilities.isRightMouseButton(e))
                {
                    if (clicked)
                    {
                        rIndex++;
                        start = false;
                        clicked = false;

                        stripAnalysis();
                    }
                    else
                    {
                        clicked = true;
                    }
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {}

            @Override
            public void mouseReleased(MouseEvent e) {}

            @Override
            public void mouseEntered(MouseEvent e) {}

            @Override
            public void mouseExited(MouseEvent e) {}
        });
        return chartPanel;
    }


    /**
     * Creates label for a strip
     * @return item label generator
     */
    private StandardXYItemLabelGenerator stripLabelGenerator()
    {
        return new StandardXYItemLabelGenerator()
        {
            @Override
            public String generateLabel(XYDataset dataset, int series, int item)
            {
                if (item == 0)
                    return "Strip " + series;
                else
                    return null;
            }
        };
    }
}