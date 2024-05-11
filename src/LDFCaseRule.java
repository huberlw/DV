import Sliders.RangeSlider;
import Sliders.RangeSliderUI;
import org.jfree.chart.*;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.labels.StandardXYItemLabelGenerator;
import org.jfree.chart.plot.DefaultDrawingSupplier;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.chart.ui.RectangleInsets;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;

public class LDFCaseRule
{
    // LDF panels
    JPanel ldfPanel = new JPanel();
    JPanel labelPanel = new JPanel();
    JTabbedPane limitSliderPane = new JTabbedPane();

    // sliders to adjust LDF
    JSlider[][] weightSliders;
    RangeSlider[] sliders;
    JLabel[] sliderLabels;

    // attributes of LDF
    double[][] scale;
    double[][] limits;
    boolean[] discrete;

    // case info
    final String chosenDataPoint;
    final String ruleBase;
    final String curClassName;
    final String opClassName;
    final int curClass;
    final int index;
    final int upper_or_lower;


    /**
     * Constructor for LDFRule for a single case. Visualizes LDF.
     */
    public LDFCaseRule(String chosenDataPoint, String curClassName, String opClassName, int curClass, int index)
    {
        this.chosenDataPoint = chosenDataPoint;
        this.ruleBase = chosenDataPoint + "<br/><br/>" + "<b>Generalized Rule: </b>";
        this.curClassName = curClassName;
        this.opClassName = opClassName;
        this.curClass = curClass;
        this.index = index;
        this.upper_or_lower = curClass == DV.upperClass ? 0 : 1;

        // create LDF function menu
        scale = new double[2][DV.fieldLength];

        for (int i = 0; i < DV.fieldLength; i++)
        {
            scale[0][i] = 1;
            scale[1][i] = 1;
        }

        // create panels for graph and weight sliders
        createLDFCaseWindow();
    }


    /**
     * Creates window for LDFCaseRule
     */
    private void createLDFCaseWindow()
    {
        // create panels for graph and weight sliders
        JFrame ldfFrame = new JFrame();
        ldfFrame.setLocationRelativeTo(DV.mainFrame);
        ldfFrame.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        ldfPanel = new JPanel();
        getLDFRule();

        // add labels
        c.gridx = 0;
        c.gridy = 0;
        c.ipady = 10;
        c.fill = GridBagConstraints.BOTH;
        ldfFrame.add(labelPanel, c);

        // add graphs
        c.gridy = 1;
        c.weightx = 0.8;
        c.weighty = 1;
        ldfFrame.add(ldfPanel, c);

        JPanel[] sliderPanels = new JPanel[DV.fieldLength];
        limitSliderPane.removeAll();
        createSliders(sliderPanels);

        // add limit sliders
        c.gridy = 2;
        c.weighty = 0;
        ldfFrame.add(limitSliderPane, c);

        // add scales
        c.gridx = 1;
        c.gridy = 1;
        c.weightx = 0.2;
        c.weighty = 1;
        c.gridheight = 2;
        ldfFrame.add(createWeightSliders(), c);

        // draw graphs
        drawLDF();

        // show
        ldfFrame.setVisible(true);
        ldfFrame.revalidate();
        ldfFrame.pack();
        ldfFrame.repaint();
    }


    /**
     * Creates sliders for each slider panel
     * @param sliderPanels panels to create sliders on
     */
    private void createSliders(JPanel[] sliderPanels)
    {
        sliders = new RangeSlider[DV.fieldLength];
        sliderLabels = new JLabel[DV.fieldLength];
        limits = new double[DV.fieldLength][];
        discrete = new boolean[DV.fieldLength];

        for (int i = 0; i < DV.fieldLength; i++)
        {
            sliderPanels[i] = new JPanel();
            sliderPanels[i].setLayout(new GridBagLayout());

            sliders[i] = new RangeSlider()
            {
                @Override
                public void updateUI()
                {
                    setUI(new RangeSliderUI(this, Color.DARK_GRAY, Color.RED, Color.BLUE));
                    updateLabelUIs();
                }
            };

            discrete[i] = false;
            limits[i] = new double[]{0, 5 * DV.trainData.get(curClass).data[index][i]};

            sliders[i].setMinimum(0);
            sliders[i].setMaximum(500);
            sliders[i].setMajorTickSpacing(1);
            sliders[i].setValue(0);
            sliders[i].setUpperValue(500);
            sliders[i].setToolTipText("Sets lower and upper limits for " + DV.fieldNames.get(i));
            JLabel sl = new JLabel(limitSliderLabel(i));
            sl.setFont(sl.getFont().deriveFont(16f));
            sliderLabels[i] = sl;

            // add label
            GridBagConstraints panelC = new GridBagConstraints();
            panelC.gridx = 0;
            panelC.gridy = 0;
            panelC.weightx = 1;
            panelC.ipady = 20;
            panelC.fill = GridBagConstraints.HORIZONTAL;
            sliderPanels[i].add(sliderLabels[i], panelC);

            // add discrete point option
            JCheckBox dBox = getjCheckBox(i);

            panelC.gridy = 1;
            sliderPanels[i].add(dBox, panelC);

            // add slider
            panelC.gridy = 2;
            sliderPanels[i].add(sliders[i], panelC);

            limitSliderPane.add(DV.fieldNames.get(i), sliderPanels[i]);
        }
    }


    /**
     * Creates sliders to adjust the scale for the upper and lower bound of a case
     * @return Tabbed pane of sliders
     */
    private JTabbedPane createWeightSliders()
    {
        JPanel lowerScalePanel = new JPanel();
        lowerScalePanel.setLayout(new BoxLayout(lowerScalePanel, BoxLayout.PAGE_AXIS));
        JScrollPane lowerScaleScroll = new JScrollPane(lowerScalePanel);

        JPanel upperScalePanel = new JPanel();
        upperScalePanel.setLayout(new BoxLayout(upperScalePanel, BoxLayout.PAGE_AXIS));
        JScrollPane upperScaleScroll = new JScrollPane(upperScalePanel);

        weightSliders = new JSlider[DV.fieldLength][2];
        for (int i = 0; i < DV.fieldLength; i++)
        {
            if ((DV.trainData.get(curClass).data[index][i] != 0 && DV.angles[i] <= 90) || DV.angles[i] > 90)
            {
                // lower
                weightSliders[i][0] = new JSlider(0, 500, (int)(scale[0][i] * 100));
                lowerScalePanel.add(AngleSliders.createLDFSliderPanel_GLC(
                        this,
                        weightSliders[i][0],
                        (int)(scale[0][i] * 100),
                        i,
                        0));

                // upper
                weightSliders[i][1] = new JSlider(0, 500, (int)(scale[1][i] * 100));
                upperScalePanel.add(AngleSliders.createLDFSliderPanel_GLC(
                        this,
                        weightSliders[i][1],
                        (int)(scale[1][i] * 100),
                        i,
                        1));
            }
        }

        JTabbedPane scaleTabs = new JTabbedPane();
        scaleTabs.add("Lower Scale", lowerScaleScroll);
        scaleTabs.add("Upper Scale", upperScaleScroll);

        return scaleTabs;
    }


    /**
     * Slider option to make attribute discrete
     * @param i attribute
     * @return Slider option
     */
    private JCheckBox getjCheckBox(int i)
    {
        JCheckBox dBox = new JCheckBox("<html><b>Discrete Attribute:</b> " + discrete[i] + "</html>", discrete[i]);
        dBox.setFont(dBox.getFont().deriveFont(16f));
        dBox.setToolTipText("Whether the current attribute is discrete. Discrete attributes are always whole numbers.");
        final int finalI = i;

        dBox.addChangeListener(de ->
        {
            discrete[finalI] = dBox.isSelected();
            dBox.setText("<html><b>Discrete Attribute:</b> " + discrete[finalI] + "</html>");

            drawLDF();
        });

        return dBox;
    }


    /**
     * Creates new LDF rule with given scales
     */
    public void getLDFRule()
    {
        labelPanel.removeAll();

        StringBuilder ldfInfo = new StringBuilder(ruleBase);

        for (int i = 0; i < DV.fieldLength; i++)
        {
            boolean used = false;

            if (!(DV.trainData.get(curClass).data[index][i] == 0 && DV.angles[i] > 90))
            {
                ldfInfo.append(
                        String.format("%.2f", DV.trainData.get(curClass).data[index][i] * scale[0][i]))
                        .append(" &le; ").append("x").append(i).append(" &le; ")
                        .append(String.format("%.2f", DV.trainData.get(curClass).data[index][i] * scale[1][i]));
                used = true;
            }

            if (used && i != DV.fieldLength - 1) ldfInfo.append(", ");
        }

        if (DV.upperIsLower)
            ldfInfo.append("<br/>&emsp;&emsp;&emsp;&emsp;&emsp;then x belongs to class ").append(curClassName).append("</html>");
        else
            ldfInfo.append("<br/>&emsp;&emsp;&emsp;&emsp;&emsp;then x belongs to class ").append(opClassName).append("</html>");

        JLabel rule = new JLabel(ldfInfo.toString());
        rule.setFont(rule.getFont().deriveFont(14f));

        labelPanel.add(rule);
        labelPanel.revalidate();
        labelPanel.repaint();
    }


    /**
     * Creates label for limit slider
     * @param attribute current attribute
     * @return label
     */
    private String limitSliderLabel(int attribute)
    {
        double upperVal = (sliders[attribute].getUpperValue() / 100.0) * DV.trainData.get(curClass).data[index][attribute];
        double lowerVal = (sliders[attribute].getValue() / 100.0) * DV.trainData.get(curClass).data[index][attribute];

        // undo min-max normalization
        upperVal *= (DV.max[attribute] - DV.min[attribute]);
        upperVal += DV.min[attribute];

        lowerVal *= (DV.max[attribute] - DV.min[attribute]);
        lowerVal += DV.min[attribute];

        // undo z-score
        if (DV.zScoreMinMax)
        {
            upperVal *= DV.sd[attribute];
            upperVal += DV.mean[attribute];

            lowerVal *= DV.sd[attribute];
            lowerVal += DV.mean[attribute];
        }

        String label;

        // if discrete round to whole number
        if (discrete[attribute])
        {
            upperVal = Math.round(upperVal);
            lowerVal = Math.round(lowerVal);

            label = "<html>" + "<b>Limits for " + DV.fieldNames.get(attribute) +
                    "<br>Upper Limit:</b> " + upperVal + "\t<b>Lower Limit:</b> " + lowerVal + "</html>";

            // transform back to normalized value
            // z-score
            if (DV.zScoreMinMax)
            {
                upperVal -= DV.mean[attribute];
                upperVal /= DV.sd[attribute];

                lowerVal -= DV.mean[attribute];
                lowerVal /= DV.sd[attribute];
            }

            // min-max normalization
            upperVal -= DV.min[attribute];
            upperVal /= (DV.max[attribute] - DV.min[attribute]);

            lowerVal -= DV.min[attribute];
            lowerVal /= (DV.max[attribute] - DV.min[attribute]);

            // change sliders to match rounded whole number
            sliders[attribute].setUpperValue((int)(upperVal / DV.trainData.get(curClass).data[index][attribute] * 100.0));
            sliders[attribute].setValue((int)(lowerVal / DV.trainData.get(curClass).data[index][attribute] * 100.0));
        }
        else
        {
            // round to two decimals
            upperVal = Math.round(upperVal * 100) / 100.0;
            lowerVal = Math.round(lowerVal * 100) / 100.0;

            label = "<html>" + "<b>Limits for " + DV.fieldNames.get(attribute) +
                    "<br>Upper Limit:</b> " + upperVal + "\t<b>Lower Limit:</b> " + lowerVal + "</html>";
        }

        return label;
    }


    /**
     * Draws LDF for a given case
     */
    public void drawLDF()
    {
        // clear panel
        ldfPanel.removeAll();
        ldfPanel.setLayout(new BoxLayout(ldfPanel, BoxLayout.PAGE_AXIS));

        // lines, endpoints, and timeline points
        XYLineAndShapeRenderer originalLineRenderer = new XYLineAndShapeRenderer(true, false);
        XYLineAndShapeRenderer originalEndpointRenderer = new XYLineAndShapeRenderer(false, true);
        XYLineAndShapeRenderer originalTimelineRenderer = new XYLineAndShapeRenderer(false, true);
        XYSeriesCollection originalLine = new XYSeriesCollection();
        XYSeriesCollection originalEndpoint = new XYSeriesCollection();
        XYSeriesCollection originalTimeline = new XYSeriesCollection();
        XYLineAndShapeRenderer lowerWeightedLineRenderer = new XYLineAndShapeRenderer(true, false);
        XYLineAndShapeRenderer lowerWeightedTimelineRenderer = new XYLineAndShapeRenderer(false, true);
        XYLineAndShapeRenderer lowerWeightedEndpointRenderer = new XYLineAndShapeRenderer(false, true);
        XYSeriesCollection lowerWeightedLine = new XYSeriesCollection();
        XYSeriesCollection lowerWeightedEndpoint = new XYSeriesCollection();
        XYSeriesCollection lowerWeightedTimeline = new XYSeriesCollection();
        XYLineAndShapeRenderer upperWeightedLineRenderer = new XYLineAndShapeRenderer(true, false);
        XYLineAndShapeRenderer upperWeightedTimelineRenderer = new XYLineAndShapeRenderer(false, true);
        XYLineAndShapeRenderer upperWeightedEndpointRenderer = new XYLineAndShapeRenderer(false, true);
        XYSeriesCollection upperWeightedLine = new XYSeriesCollection();
        XYSeriesCollection upperWeightedEndpoint = new XYSeriesCollection();
        XYSeriesCollection upperWeightedTimeline = new XYSeriesCollection();
        XYLineAndShapeRenderer thresholdRenderer = new XYLineAndShapeRenderer(true, false);
        XYSeriesCollection threshold = new XYSeriesCollection();
        XYSeries thresholdLine = new XYSeries(0, false, true);

        // get threshold line
        thresholdLine.add(DV.threshold, 0);
        thresholdLine.add(DV.threshold, DV.fieldLength);

        // add threshold series to collection
        threshold.addSeries(thresholdLine);

        XYSeries line1 = new XYSeries(0, false, true);
        line1.add(0, 0);
        XYSeries line2 = new XYSeries(0, false, true);
        line2.add(0, 0);
        XYSeries line3 = new XYSeries(0, false, true);
        line3.add(0, 0);

        XYSeries end1 = new XYSeries(0, false, true);
        XYSeries end2 = new XYSeries(0, false, true);
        XYSeries end3 = new XYSeries(0, false, true);

        XYSeries time1 = new XYSeries(0, false, true);
        XYSeries time2 = new XYSeries(0, false, true);
        XYSeries time3 = new XYSeries(0, false, true);

        double x1 = 0, y1 = 0;
        double x2 = 0, y2 = 0;

        for (int i = 0; i < DV.fieldLength; i++)
        {
            if ((DV.trainData.get(curClass).data[index][i] != 0 && DV.angles[i] <= 90) || DV.angles[i] > 90)
            {
                line1.add(DV.trainData.get(curClass).coordinates[index][i][0], DV.trainData.get(curClass).coordinates[index][i][1]);
                end1.add(DV.trainData.get(curClass).coordinates[index][i][0], DV.trainData.get(curClass).coordinates[index][i][1]);

                double[] xyPoint = DataObject.getXYPointGLC(DV.trainData.get(curClass).data[index][i], DV.angles[i]);

                x1 += xyPoint[0] * scale[0][i];
                y1 += xyPoint[1] * scale[0][i];

                line2.add(x1, y1);
                end2.add(x1, y1);

                x2 += xyPoint[0] * scale[1][i];
                y2 += xyPoint[1] * scale[1][i];

                line3.add(x2, y2);
                end3.add(x2, y2);

                if (i == DV.fieldLength - 1)
                {
                    time1.add(DV.trainData.get(curClass).coordinates[index][i][0], 0);
                    time2.add(x1, 0);
                    time3.add(x2, 0);
                }
            }
        }

        originalLine.addSeries(line1);
        lowerWeightedLine.addSeries(line2);
        upperWeightedLine.addSeries(line3);
        originalEndpoint.addSeries(end1);
        lowerWeightedEndpoint.addSeries(end2);
        upperWeightedEndpoint.addSeries(end3);
        originalTimeline.addSeries(time1);
        lowerWeightedTimeline.addSeries(time2);
        upperWeightedTimeline.addSeries(time3);

        // get chart and plot
        JFreeChart chart = ChartsAndPlots.createChart(originalLine, true);
        XYPlot plot = ChartsAndPlots.createPlot(chart, upper_or_lower);

        // add legend
        LegendItemCollection lc = new LegendItemCollection();
        lc.add(new LegendItem("Original n-D Point", DV.graphColors[upper_or_lower]));
        lc.add(new LegendItem("Lower Scaled n-D Point", Color.RED));
        lc.add(new LegendItem("Upper Scaled n-D Point", Color.BLUE));
        plot.setFixedLegendItems(lc);

        lowerWeightedEndpointRenderer.setSeriesShape(0, new Ellipse2D.Double(-1, -1, 2, 2));
        lowerWeightedEndpointRenderer.setSeriesPaint(0, DV.endpoints);
        plot.setRenderer(0, lowerWeightedEndpointRenderer);
        plot.setDataset(0, lowerWeightedEndpoint);

        upperWeightedEndpointRenderer.setSeriesShape(0, new Ellipse2D.Double(-1, -1, 2, 2));
        upperWeightedEndpointRenderer.setSeriesPaint(0, DV.endpoints);
        plot.setRenderer(1, upperWeightedEndpointRenderer);
        plot.setDataset(1, upperWeightedEndpoint);

        originalEndpointRenderer.setSeriesShape(0, new Ellipse2D.Double(-1, -1, 2, 2));
        originalEndpointRenderer.setSeriesPaint(0, DV.endpoints);
        plot.setRenderer(2, originalEndpointRenderer);
        plot.setDataset(2, originalEndpoint);

        lowerWeightedTimelineRenderer.setSeriesShape(0, new Rectangle2D.Double(-0.25, 0, 0.5, 3));
        lowerWeightedTimelineRenderer.setSeriesPaint(0, Color.RED);
        plot.setRenderer(3, lowerWeightedTimelineRenderer);
        plot.setDataset(3, lowerWeightedTimeline);

        upperWeightedTimelineRenderer.setSeriesShape(0, new Rectangle2D.Double(-0.25, 0, 0.5, 3));
        upperWeightedTimelineRenderer.setSeriesPaint(0, Color.BLUE);
        plot.setRenderer(4, upperWeightedTimelineRenderer);
        plot.setDataset(4, upperWeightedTimeline);

        originalTimelineRenderer.setSeriesShape(0, new Rectangle2D.Double(-0.25, 0, 0.5, 3));
        originalTimelineRenderer.setSeriesPaint(0, DV.endpoints);
        plot.setRenderer(5, originalTimelineRenderer);
        plot.setDataset(5, originalTimeline);

        originalLineRenderer.setSeriesStroke(0, new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        originalLineRenderer.setAutoPopulateSeriesStroke(false);
        plot.setRenderer(6, originalLineRenderer);
        plot.setDataset(6, originalLine);

        lowerWeightedLineRenderer.setSeriesStroke(0, new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        lowerWeightedLineRenderer.setSeriesPaint(0, Color.RED);
        lowerWeightedLineRenderer.setAutoPopulateSeriesStroke(false);
        plot.setRenderer(7, lowerWeightedLineRenderer);
        plot.setDataset(7, lowerWeightedLine);

        upperWeightedLineRenderer.setSeriesStroke(0, new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        upperWeightedLineRenderer.setSeriesPaint(0, Color.BLUE);
        upperWeightedLineRenderer.setAutoPopulateSeriesStroke(false);
        plot.setRenderer(8, upperWeightedLineRenderer);
        plot.setDataset(8, upperWeightedLine);

        // set threshold renderer and dataset
        thresholdRenderer.setSeriesStroke(0, new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[] {12f, 6f}, 0.0f));
        thresholdRenderer.setSeriesPaint(0, DV.thresholdLine);
        plot.setRenderer(9, thresholdRenderer);
        plot.setDataset(9, threshold);

        // add chart
        ldfPanel.add(new ChartPanel(chart));

        // create parallel coordinates chart
        XYLineAndShapeRenderer pcOriginalLineRenderer = new XYLineAndShapeRenderer(true, false);
        XYLineAndShapeRenderer pcOriginalEndpointRenderer = new XYLineAndShapeRenderer(false, true);
        XYSeriesCollection pcOriginalLine = new XYSeriesCollection();
        XYSeriesCollection pcOriginalEndpoint = new XYSeriesCollection();
        XYLineAndShapeRenderer pcLowerWeightedLineRenderer = new XYLineAndShapeRenderer(true, false);
        XYLineAndShapeRenderer pcLowerWeightedEndpointRenderer = new XYLineAndShapeRenderer(false, true);
        XYSeriesCollection pcLowerWeightedLine = new XYSeriesCollection();
        XYSeriesCollection pcLowerWeightedEndpoint = new XYSeriesCollection();
        XYLineAndShapeRenderer pcUpperWeightedLineRenderer = new XYLineAndShapeRenderer(true, false);
        XYLineAndShapeRenderer pcUpperWeightedEndpointRenderer = new XYLineAndShapeRenderer(false, true);
        XYSeriesCollection pcUpperWeightedLine = new XYSeriesCollection();
        XYSeriesCollection pcUpperWeightedEndpoint = new XYSeriesCollection();

        // limit lines
        XYLineAndShapeRenderer pcLowerLimitRenderer = new XYLineAndShapeRenderer(false, true);
        XYLineAndShapeRenderer pcUpperLimitRenderer = new XYLineAndShapeRenderer(false, true);
        XYSeriesCollection pcLowerLimits = new XYSeriesCollection();
        XYSeriesCollection pcUpperLimits = new XYSeriesCollection();

        XYSeries pcLine1 = new XYSeries(0, false, true);
        XYSeries pcLine2 = new XYSeries(0, false, true);
        XYSeries pcLine3 = new XYSeries(0, false, true);

        XYSeries pcEnd1 = new XYSeries(0, false, true);
        XYSeries pcEnd2 = new XYSeries(0, false, true);
        XYSeries pcEnd3 = new XYSeries(0, false, true);

        XYSeries upLim = new XYSeries(0, false, true);
        XYSeries lowLim = new XYSeries(0, false, true);

        for (int i = 0, invalid = 0; i < DV.fieldLength; i++)
        {
            if ((DV.trainData.get(curClass).data[index][i] != 0 && DV.angles[i] <= 90) || DV.angles[i] > 90)
            {
                pcLine1.add(i - invalid,  DV.trainData.get(curClass).data[index][i]);
                pcEnd1.add(i - invalid,  DV.trainData.get(curClass).data[index][i]);

                double lowY = DV.trainData.get(curClass).data[index][i] * scale[0][i];
                double upY = DV.trainData.get(curClass).data[index][i] * scale[1][i];
                double lowLimitY = (sliders[i].getValue() / 100.0) * DV.trainData.get(curClass).data[index][i];
                double upLimitY = (sliders[i].getUpperValue() / 100.0) * DV.trainData.get(curClass).data[index][i];

                if (discrete[i])
                {
                    // transform to real value
                    // undo min-max normalization
                    lowY *= (DV.max[i] - DV.min[i]);
                    lowY += DV.min[i];

                    upY *= (DV.max[i] - DV.min[i]);
                    upY += DV.min[i];

                    lowLimitY *= (DV.max[i] - DV.min[i]);
                    lowLimitY += DV.min[i];

                    upLimitY *= (DV.max[i] - DV.min[i]);
                    upLimitY += DV.min[i];

                    // undo z-score
                    if (DV.zScoreMinMax)
                    {
                        lowY *= DV.sd[i];
                        lowY += DV.mean[i];

                        upY *= DV.sd[i];
                        upY += DV.mean[i];

                        lowLimitY *= DV.sd[i];
                        lowLimitY += DV.mean[i];

                        upLimitY *= DV.sd[i];
                        upLimitY += DV.mean[i];
                    }

                    // round real value to whole number
                    lowY = Math.round(lowY);
                    upY = Math.round(upY);
                    lowLimitY = Math.round(lowLimitY);
                    upLimitY = Math.round(upLimitY);

                    // transform back to normalized value
                    // z-score
                    if (DV.zScoreMinMax)
                    {
                        lowY -= DV.mean[i];
                        lowY /= DV.sd[i];

                        upY -= DV.mean[i];
                        upY /= DV.sd[i];

                        lowLimitY -= DV.mean[i];
                        lowLimitY /= DV.sd[i];

                        upLimitY -= DV.mean[i];
                        upLimitY /= DV.sd[i];
                    }

                    // min-max normalization
                    lowY -= DV.min[i];
                    lowY /= (DV.max[i] - DV.min[i]);

                    upY -= DV.min[i];
                    upY /= (DV.max[i] - DV.min[i]);

                    lowLimitY -= DV.min[i];
                    lowLimitY /= (DV.max[i] - DV.min[i]);

                    upLimitY -= DV.min[i];
                    upLimitY /= (DV.max[i] - DV.min[i]);
                }

                pcLine2.add(i - invalid,  lowY);
                pcEnd2.add(i - invalid,  lowY);

                pcLine3.add(i - invalid,  upY);
                pcEnd3.add(i - invalid,  upY);

                lowLim.add(i - invalid, lowLimitY);
                upLim.add(i - invalid, upLimitY);
            }
            else invalid++;
        }

        pcOriginalLine.addSeries(pcLine1);
        pcLowerWeightedLine.addSeries(pcLine2);
        pcUpperWeightedLine.addSeries(pcLine3);
        pcOriginalEndpoint.addSeries(pcEnd1);
        pcLowerWeightedEndpoint.addSeries(pcEnd2);
        pcUpperWeightedEndpoint.addSeries(pcEnd3);
        pcLowerLimits.addSeries(lowLim);
        pcUpperLimits.addSeries(upLim);

        JFreeChart pcChart = ChartFactory.createXYLineChart(
                "",
                "",
                "",
                pcOriginalLine,
                PlotOrientation.VERTICAL,
                false,
                true,
                false);

        // format chart
        pcChart.setBorderVisible(false);
        pcChart.setPadding(RectangleInsets.ZERO_INSETS);

        // get plot
        XYPlot pcPlot = (XYPlot) pcChart.getPlot();

        // format plot
        pcPlot.setDrawingSupplier(new DefaultDrawingSupplier(
                new Paint[] { DV.graphColors[upper_or_lower] },
                DefaultDrawingSupplier.DEFAULT_OUTLINE_PAINT_SEQUENCE,
                DefaultDrawingSupplier.DEFAULT_STROKE_SEQUENCE,
                DefaultDrawingSupplier.DEFAULT_OUTLINE_STROKE_SEQUENCE,
                DefaultDrawingSupplier.DEFAULT_SHAPE_SEQUENCE));
        pcPlot.getRangeAxis().setVisible(true);
        pcPlot.getDomainAxis().setVisible(false);
        pcPlot.setOutlinePaint(null);
        pcPlot.setOutlineVisible(false);
        pcPlot.setInsets(RectangleInsets.ZERO_INSETS);
        pcPlot.setDomainPannable(true);
        pcPlot.setRangePannable(true);
        pcPlot.setBackgroundPaint(DV.background);
        pcPlot.setDomainGridlinePaint(Color.GRAY);

        // set domain
        ValueAxis domainView = pcPlot.getDomainAxis();
        domainView.setRange(-0.1, DV.fieldLength);

        NumberAxis xAxis = (NumberAxis) pcPlot.getDomainAxis();
        xAxis.setTickUnit(new NumberTickUnit(1));

        // set range
        NumberAxis yAxis = (NumberAxis) pcPlot.getRangeAxis();
        yAxis.setTickUnit(new NumberTickUnit(0.25));

        pcLowerLimitRenderer.setSeriesShape(0, new Rectangle2D.Double(-12.5, -5, 25, 10));
        pcLowerLimitRenderer.setSeriesPaint(0, Color.RED);
        pcLowerLimitRenderer.setSeriesItemLabelGenerator(0, ScaleLabelGenerator());
        pcLowerLimitRenderer.setSeriesItemLabelGenerator(0, ScaleLabelGenerator());
        pcLowerLimitRenderer.setSeriesItemLabelsVisible(0, true);
        pcPlot.setRenderer(0, pcLowerLimitRenderer);
        pcPlot.setDataset(0, pcLowerLimits);

        pcUpperLimitRenderer.setSeriesShape(0, new Rectangle2D.Double(-12.5, -5, 25, 10));
        pcUpperLimitRenderer.setSeriesPaint(0, Color.BLUE);
        pcUpperLimitRenderer.setSeriesItemLabelGenerator(0, ScaleLabelGenerator());
        pcUpperLimitRenderer.setSeriesItemLabelsVisible(0, true);
        pcPlot.setRenderer(1, pcUpperLimitRenderer);
        pcPlot.setDataset(1, pcUpperLimits);

        pcOriginalEndpointRenderer.setSeriesShape(0, new Rectangle2D.Double(-2.5, -2.5, 5, 5));
        pcPlot.setRenderer(2, pcOriginalEndpointRenderer);
        pcPlot.setDataset(2, pcOriginalEndpoint);

        pcLowerWeightedEndpointRenderer.setSeriesShape(0, new Rectangle2D.Double(-2.5, -2.5, 5, 5));
        pcLowerWeightedEndpointRenderer.setSeriesPaint(0, Color.RED);
        pcLowerWeightedEndpointRenderer.setSeriesItemLabelGenerator(0, ScaleLabelGenerator());
        pcLowerWeightedEndpointRenderer.setSeriesItemLabelsVisible(0, true);
        pcPlot.setRenderer(3, pcLowerWeightedEndpointRenderer);
        pcPlot.setDataset(3, pcLowerWeightedEndpoint);

        pcUpperWeightedEndpointRenderer.setSeriesShape(0, new Rectangle2D.Double(-2.5, -2.5, 5, 5));
        pcUpperWeightedEndpointRenderer.setSeriesPaint(0, Color.BLUE);
        pcUpperWeightedEndpointRenderer.setSeriesItemLabelGenerator(0, ScaleLabelGenerator());
        pcUpperWeightedEndpointRenderer.setSeriesItemLabelsVisible(0, true);
        pcPlot.setRenderer(4, pcUpperWeightedEndpointRenderer);
        pcPlot.setDataset(4, pcUpperWeightedEndpoint);

        pcOriginalLineRenderer.setSeriesStroke(0, new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        pcOriginalLineRenderer.setAutoPopulateSeriesStroke(false);
        pcPlot.setRenderer(5, pcOriginalLineRenderer);
        pcPlot.setDataset(5, pcOriginalLine);

        pcLowerWeightedLineRenderer.setSeriesStroke(0, new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        pcLowerWeightedLineRenderer.setSeriesPaint(0, Color.RED);
        pcLowerWeightedLineRenderer.setAutoPopulateSeriesStroke(false);
        pcPlot.setRenderer(6, pcLowerWeightedLineRenderer);
        pcPlot.setDataset(6, pcLowerWeightedLine);

        pcUpperWeightedLineRenderer.setSeriesStroke(0, new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        pcUpperWeightedLineRenderer.setSeriesPaint(0, Color.BLUE);
        pcUpperWeightedLineRenderer.setAutoPopulateSeriesStroke(false);
        pcPlot.setRenderer(7, pcUpperWeightedLineRenderer);
        pcPlot.setDataset(7, pcUpperWeightedLine);

        // add mouse listeners to limits
        for (int i = 0; i < DV.fieldLength; i++)
        {
            sliders[i].addMouseMotionListener(new MouseMotionListener()
            {
                @Override
                public void mouseDragged(MouseEvent e)
                {
                    upLim.clear();
                    lowLim.clear();

                    for (int i = 0, invalid = 0; i < DV.fieldLength; i++)
                    {
                        if ((DV.trainData.get(curClass).data[index][i] != 0 && DV.angles[i] <= 90) || DV.angles[i] > 90)
                        {
                            // update slider label
                            sliderLabels[i].setText(limitSliderLabel(i));

                            // update limit values
                            limits[i][0] = (sliders[i].getValue() / 100.0) * DV.trainData.get(curClass).data[index][i];
                            limits[i][1] = (sliders[i].getUpperValue() / 100.0) * DV.trainData.get(curClass).data[index][i];

                            // update limits on graph
                            lowLim.add(i - invalid, limits[i][0]);
                            upLim.add(i - invalid, limits[i][1]);

                            boolean changed = false;

                            if (DV.trainData.get(curClass).data[index][i] * scale[0][i] < limits[i][0])
                            {
                                scale[0][i] = limits[i][0] / DV.trainData.get(curClass).data[index][i];
                                weightSliders[i][0].setValue((int)(scale[0][i] * 100));

                                // redraw graphs
                                changed = true;
                            }

                            if (DV.trainData.get(curClass).data[index][i] * scale[1][i] < limits[i][0])
                            {
                                scale[1][i] = limits[i][0] / DV.trainData.get(curClass).data[index][i];
                                weightSliders[i][1].setValue((int)(scale[1][i] * 100));

                                // redraw graphs
                                changed = true;
                            }

                            if (DV.trainData.get(curClass).data[index][i] * scale[0][i] > limits[i][1])
                            {
                                scale[0][i] = limits[i][1] / DV.trainData.get(curClass).data[index][i];
                                weightSliders[i][0].setValue((int)(scale[0][i] * 100));

                                // redraw graphs
                                changed = true;
                            }

                            if (DV.trainData.get(curClass).data[index][i] * scale[1][i] > limits[i][1])
                            {
                                scale[1][i] = limits[i][1] / DV.trainData.get(curClass).data[index][i];
                                weightSliders[i][1].setValue((int)(scale[1][i] * 100));

                                // redraw graphs
                                changed = true;
                            }

                            if (changed)
                            {
                                getLDFRule();
                                drawLDF();
                            }
                        }
                        else invalid++;
                    }

                    limitSliderPane.revalidate();
                    limitSliderPane.repaint();
                }

                @Override
                public void mouseMoved(MouseEvent e) {}
            });
        }

        ldfPanel.add(new ChartPanel(pcChart));
        ldfPanel.revalidate();
        ldfPanel.repaint();
    }


    /**
     * Generates Labels for a given XY Item
     * @return label generator
     */
    private StandardXYItemLabelGenerator ScaleLabelGenerator()
    {
        return new StandardXYItemLabelGenerator()
        {
            @Override
            public String generateLabel(XYDataset dataset, int series, int item)
            {
                // x-value is always in integer for this PC plot (0, 1, 2,...)
                int xVal = (int) dataset.getXValue(series, item);
                double yVal = dataset.getYValue(series, item);

                // undo min-max normalization
                yVal *= (DV.max[xVal] - DV.min[xVal]);
                yVal += DV.min[xVal];

                // undo z-score
                if (DV.zScoreMinMax)
                {
                    yVal *= DV.sd[xVal];
                    yVal += DV.mean[xVal];
                }

                if (discrete[xVal])
                    return String.format("%.0f", yVal);
                else
                    return String.format("%.2f", yVal);
            }
        };
    }
}
