import Sliders.RangeSlider;
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
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class DataVisualization
{
    // holds upper and lower graphs
    final static Map<Integer, JPanel> GRAPHS = new HashMap<>();

    // vertical scale of graphs
    static double verticalScale;


    /**
     * Gets optimal angles and threshold
     * then gets overlap and domain
     * areas
     */
    public static void optimizeSetup()
    {
        if (DV.classNumber > 1)
        {
            // setup vertical scaling
            verticalScale = 0.4;

            // get optimal angles and threshold
            LDA();

            // optimize threshold
            optimizeThreshold(0);

            // try again with upperIsLower false
            double upperIsLowerAccuracy = DV.accuracy;
            DV.upperIsLower = false;

            optimizeThreshold(0);

            // see whether upper is actually lower
            if (DV.accuracy < upperIsLowerAccuracy)
            {
                DV.upperIsLower = true;
                optimizeThreshold(0);
            }

            // get overlap area
            getOverlap();
        }
        else
        {
            // setup vertical scaling
            verticalScale = 0.8;

            // set overlap to right end of graph
            getOverlap();

            // set threshold to left end of graph
            DV.threshold = -DV.fieldLength;
            DV.thresholdSlider.setValue(0);

            // setup angle slider panel
            DV.angleSliderPanel.removeAll();
            DV.angleSliderPanel.setLayout(new GridLayout(DV.fieldLength, 0));

            // setup angle sliders
            for (int i = 0; i < DV.fieldLength; i++)
                AngleSliders.createSliderPanel(DV.fieldNames.get(i), (int) (DV.angles[i] * 100), i);

            // repaint DV
            DV.mainFrame.repaint();
            DV.mainFrame.revalidate();
        }

        // set domain are to max length
        DV.domainArea = new double[] { -DV.fieldLength, DV.fieldLength };
    }


    /**
     * Create CSV file representing the upper graph
     * as class 1 and the lower graph as class 2
     */
    private static void createCSVFile()
    {
        try
        {
            // create csv file
            File csv = new File("source\\Python\\DV_data.csv");
            Files.deleteIfExists(csv.toPath());

            // write to csv file
            PrintWriter out = new PrintWriter(csv);

            // create header for file
            for (int i = 0; i < DV.fieldLength; i++)
            {
                if (i != DV.fieldLength - 1)
                    out.print("feature,");
                else
                    out.print("feature,class\n");
            }

            // check all classes
            for (int i = 0; i < DV.data.size(); i++)
            {
                // get class or skip
                String curClass;
                if (i == DV.upperClass)
                    curClass = "1";
                else if (DV.lowerClasses.get(i))
                    curClass = "2";
                else
                    continue;

                // get all data for class
                for (int j = 0; j < DV.data.get(i).data.length; j++)
                {
                    for (int k = 0; k < DV.fieldLength; k++)
                    {
                        if (k != DV.fieldLength - 1)
                            out.printf("%f,", DV.data.get(i).data[j][k]);
                        else
                            out.printf("%f," + curClass + "\n", DV.data.get(i).data[j][k]);
                    }
                }
            }

            // close file
            out.close();
        }
        catch (IOException ioe)
        {
            ioe.printStackTrace();
        }
    }


    /**
     * Linear Discriminant Analysis (LDA)
     * Gets optimal angles and threshold
     */
    private static void LDA()
    {
        // create LDA (python) process
        ProcessBuilder lda = new ProcessBuilder("cmd", "/c",
                "source\\Python\\LinearDiscriminantAnalysis\\LinearDiscriminantAnalysis.exe",
                "source\\Python\\DV_data.csv");

        try
        {
            // create file for python process
            createCSVFile();

            // run python (LDA) process
            Process process = lda.start();

            // read python outputs
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String output;

            // if process is running continue
            DV.angleSliderPanel.removeAll();
            DV.angleSliderPanel.setLayout(new GridLayout(DV.fieldLength, 0));

            int cnt = 0;

            while ((output = reader.readLine()) != null)
            {
                // get angles then threshold
                if (cnt < DV.fieldLength)
                {
                    // update angles and create angle slider
                    DV.angles[cnt] = Double.parseDouble(output);
                    AngleSliders.createSliderPanel(DV.fieldNames.get(cnt), (int) (DV.angles[cnt] * 100), cnt);
                    cnt++;
                }
                else
                {
                    // get threshold
                    DV.threshold = Double.parseDouble(output);
                    DV.thresholdSlider.setValue((int) (Double.parseDouble(output) / DV.fieldLength * 200) + 200);
                }
            }

            // delete created file
            File fileToDelete = new File("source\\Python\\DV_data.csv");
            Files.deleteIfExists(fileToDelete.toPath());
        }
        catch (IOException e)
        {
            JOptionPane.showMessageDialog(DV.mainFrame, "Error: could not run Linear Discriminant Analysis", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }


    /**
     * Support Vector Machine (SVM)
     * Gets support vectors
     */
    public static void SVM()
    {
        // create LDA (python) process
        ProcessBuilder svm = new ProcessBuilder("cmd", "/c",
                "source\\Python\\SupportVectorMachine\\SupportVectorMachine.exe",
                "source\\Python\\DV_data.csv");

        try
        {
            // create file for python process
            createCSVFile();

            // run python (LDA) process
            Process process = svm.start();
            process.waitFor();

            // setup support vectors
            File svFile = new File("source\\Python\\svm_data.csv");

            if (!DataSetup.setupSupportVectors(svFile))
                JOptionPane.showMessageDialog(DV.mainFrame, "Error: could not run Support Vector Machines", "Error", JOptionPane.ERROR_MESSAGE);

            // delete created file
            File fileToDelete = new File("source\\Python\\DV_data.csv");
            Files.deleteIfExists(fileToDelete.toPath());
        }
        catch (IOException | InterruptedException e)
        {
            JOptionPane.showMessageDialog(DV.mainFrame, "Error: could not run Support Vector Machines", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }


    /**
     * Searches for the best accuracy by
     * adjusting the threshold location
     * @param bestAccuracy current best accuracy
     */
    public static void optimizeThreshold(double bestAccuracy)
    {
        // store current best threshold
        double bestThreshold = DV.threshold;

        // increment equals 2 slider ticks
        double increment = 2 * DV.fieldLength / 200.0;
        DV.threshold -= 15 * increment;

        // search for best threshold
        // search range is 15% of total range
        for (int i = 0; i < 30; i++)
        {
            // calculate accuracy with trial threshold
            getAccuracy();

            // update the best threshold and accuracy
            if (DV.accuracy >= bestAccuracy)
            {
                bestAccuracy = DV.accuracy;
                bestThreshold = DV.threshold;
            }

            // new trial threshold
            DV.threshold += increment;
        }

        // set best accuracy
        DV.accuracy = bestAccuracy;

        // use best threshold
        DV.threshold = bestThreshold;

        // set slider to best
        DV.thresholdSlider.setValue((int) (bestThreshold / DV.fieldLength * 200) + 200);
    }


    /**
     * Uses random gradient search
     * to attempt optimizing angles.
     * Uses optimizeThreshold() to
     * optimize threshold.
     */
    public static void optimizeVisualization()
     {
         if (DV.classNumber > 1)
         {
             // store previous angles and threshold
             DV.prevAngles = Arrays.copyOf(DV.angles, DV.fieldLength);
             DV.prevThreshold = DV.threshold;

             // store current best angles, threshold, and accuracy
             double[] currentBestAngles = Arrays.copyOf(DV.angles, DV.fieldLength);
             double currentBestThreshold = DV.threshold;
             double currentBestAccuracy = DV.accuracy;

             // get random
             Random rand = new Random(System.currentTimeMillis());

             // get count and foundBetter
             int cnt = 0;
             boolean foundBetter = false;

             // try optimizing 200 times
             while (cnt < 200)
             {
                 // remove angle sliders and set layout
                 DV.angleSliderPanel.removeAll();

                 // get random angles
                 for (int i = 0; i < DV.fieldLength; i++)
                 {
                     int gradient = (rand.nextInt(11) - 5) * 100;
                     int fieldAngle = (int) (currentBestAngles[i] * 100) + gradient;

                     if (fieldAngle < 0)
                         fieldAngle = 0;
                     else if (fieldAngle > 18000)
                         fieldAngle = 18000;

                     DV.angles[i] = fieldAngle / 100.0;
                     AngleSliders.createSliderPanel(DV.fieldNames.get(i), fieldAngle, i);
                 }

                 // optimize threshold for new angles
                 optimizeThreshold(currentBestAccuracy);

                 // get accuracy for new setup
                 getAccuracy();

                 // update current bests if accuracy improved
                 if (DV.accuracy > currentBestAccuracy)
                 {
                     foundBetter = true;

                     currentBestAngles= Arrays.copyOf(DV.angles, DV.fieldLength);
                     currentBestThreshold = DV.threshold;
                     currentBestAccuracy = DV.accuracy;
                 }

                 cnt++;
             }

             // remove angle sliders and set layout
             DV.angleSliderPanel.removeAll();

             // inform user if optimization was successful or now
             if (foundBetter)
             {
                 // update threshold
                 DV.threshold = currentBestThreshold;
                 DV.thresholdSlider.setValue((int) (DV.threshold / DV.fieldLength * 200) + 200);

                 // update angles
                 for (int i = 0; i < DV.fieldLength; i++)
                 {
                     DV.angles[i] = currentBestAngles[i];
                     AngleSliders.createSliderPanel(DV.fieldNames.get(i), (int) (DV.angles[i] * 100), i);
                 }

                 // redraw graphs
                 drawGraphs();
                 DV.mainFrame.repaint();
                 DV.mainFrame.revalidate();

                 // inform user
                 JOptionPane.showMessageDialog(
                         DV.mainFrame,
                         "Visualization has been optimized!",
                         "Optimization Complete",
                         JOptionPane.INFORMATION_MESSAGE);
             }
             else
             {
                 // update threshold
                 DV.threshold = DV.prevThreshold;
                 DV.thresholdSlider.setValue((int) (DV.threshold / DV.fieldLength * 200) + 200);

                 // update angles
                 for (int i = 0; i < DV.fieldLength; i++)
                 {
                     DV.angles[i] = DV.prevAngles[i];
                     AngleSliders.createSliderPanel(DV.fieldNames.get(i), (int) (DV.angles[i] * 100), i);
                 }

                 // redraw graphs
                 drawGraphs();
                 DV.mainFrame.repaint();
                 DV.mainFrame.revalidate();

                 // inform user
                 JOptionPane.showMessageDialog(
                         DV.mainFrame,
                         "Was unable to optimize visualization.\nVisualization is already optimal or near optimal.",
                         "Unable to Optimize",
                         JOptionPane.INFORMATION_MESSAGE);
             }
         }
        else
        {
            // inform user
            JOptionPane.showMessageDialog(
                    DV.mainFrame,
                    "The current data has no classes: unable to optimize.",
                    "Unable to Optimize",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }


    /**
     * Undoes last optimizations
     */
    public static void undoOptimization()
    {
        // threshold
        DV.angles = Arrays.copyOf(DV.prevAngles, DV.fieldLength);

        // update sliders
        DV.thresholdSlider.setValue((int) (DV.threshold / DV.fieldLength * 200) + 200);
        DV.angleSliderPanel.removeAll();

        // restore angles
        for (int i = 0; i < DV.fieldLength; i++)
        {
            DV.angles[i] = DV.prevAngles[i];
            AngleSliders.createSliderPanel(DV.fieldNames.get(i), (int) (DV.angles[i] * 100), i);
        }

        // redraw graphs
        drawGraphs();
        DV.mainFrame.repaint();
        DV.mainFrame.revalidate();
    }


    /**
     * Gets current accuracy of visualization
     */
    public static void getAccuracy()
    {
        // total number of points and correctly classified points
        double totalPoints = 0;
        double correctPoints = 0;

        // update points and add to total
        for (int i = 0; i < DV.data.size(); i++)
        {
            // check if class is visualized
            if (i == DV.upperClass || DV.lowerClasses.get(i))
            {
                DV.data.get(i).updateCoordinates(DV.angles);
                totalPoints += DV.data.get(i).coordinates.length;
            }
        }

        // stores how points were classified
        double[][] pointClassification = new double[2][2];

        // check every class
        for (int i = 0; i < DV.data.size(); i++)
        {
            // if class is visualized check points
            if (i == DV.upperClass)
            {
                for (int j = 0; j < DV.data.get(i).coordinates.length; j++)
                {
                    // get endpoint
                    double endpoint = DV.data.get(i).coordinates[j][DV.fieldLength - 1][0];

                    // get classification
                    if (DV.upperIsLower)
                    {
                        if (endpoint <= DV.threshold)
                            pointClassification[0][0]++;
                        else
                            pointClassification[0][1]++;
                    }
                    else
                    {
                        if (endpoint >= DV.threshold)
                            pointClassification[0][0]++;
                        else
                            pointClassification[0][1]++;
                    }
                }
            }
            else if (DV.lowerClasses.get(i))
            {
                for (int j = 0; j < DV.data.get(i).coordinates.length; j++)
                {
                    // get endpoint
                    double endpoint = DV.data.get(i).coordinates[j][DV.fieldLength - 1][0];

                    // get classification
                    if (DV.upperIsLower)
                    {
                        if (endpoint >= DV.threshold)
                            pointClassification[1][1]++;
                        else
                            pointClassification[1][0]++;
                    }
                    else
                    {
                        if (endpoint <= DV.threshold)
                            pointClassification[1][1]++;
                        else
                            pointClassification[1][0]++;
                    }
                }
            }
        }

        // get diagonals
        correctPoints += pointClassification[0][0];
        correctPoints += pointClassification[1][1];

        // get accuracy
        DV.accuracy = (correctPoints / totalPoints) * 100;
    }


    /**
     * Gets overlap area of visualization
     */
    public static void getOverlap()
    {
        // get current points
        for (int i = 0; i < DV.data.size(); i++)
        {
            if (i == DV.upperClass || DV.lowerClasses.get(i))
                DV.data.get(i).updateCoordinates(DV.angles);
        }

        if (DV.classNumber > 1 && DV.accuracy != 100)
        {
            // overlap area, previous overlap are, and total area
            DV.overlapArea = new double[] { DV.fieldLength, -DV.fieldLength };
            double[] prevOverlap = { DV.fieldLength, -DV.fieldLength };
            double[] totalArea = { DV.fieldLength, -DV.fieldLength };

            // check if graph has misclassified a point
            boolean[] misclassified = { false, false };

            // check if previous overlap exists
            boolean[] isPrevious = { false, false };

            // find overlap for all classes
            for (int i = 0; i < DV.data.size(); i++)
            {
                if (i == DV.upperClass || DV.lowerClasses.get(i))
                {
                    for (int j = 0; j < DV.data.get(i).coordinates.length; j++)
                    {
                        double endpoint = DV.data.get(i).coordinates[j][DV.fieldLength-1][0];

                        // get classification
                        if ((i == DV.upperClass && DV.upperIsLower) || (DV.lowerClasses.get(i) && !DV.upperIsLower))
                        {
                            // check if endpoint expands total area
                            if (endpoint < totalArea[0])
                                totalArea[0] = endpoint;

                            // check if endpoint is misclassified
                            if (endpoint > DV.threshold)
                            {
                                // check if endpoint expands overlap area
                                if (endpoint > DV.overlapArea[1])
                                {
                                    prevOverlap[1] = DV.overlapArea[1];
                                    DV.overlapArea[1] = endpoint;

                                    if (misclassified[1])
                                        isPrevious[1] = true;

                                    misclassified[1] = true;
                                }
                                else if (endpoint > prevOverlap[1]) // get largest previous overlap point
                                {
                                    prevOverlap[1] = endpoint;
                                    isPrevious[1] = true;
                                }
                            }
                        }
                        else
                        {
                            // check if endpoint expands total area
                            if (endpoint > totalArea[1])
                                totalArea[1] = endpoint;

                            // check if endpoint is misclassified
                            if (endpoint < DV.threshold)
                            {
                                // check if endpoint expands overlap area
                                if (endpoint < DV.overlapArea[0])
                                {
                                    prevOverlap[0] = DV.overlapArea[0];
                                    DV.overlapArea[0] = endpoint;

                                    if (misclassified[0])
                                        isPrevious[0] = true;

                                    misclassified[0] = true;
                                }
                                else if (endpoint < prevOverlap[0]) // get largest previous overlap point
                                {
                                    prevOverlap[0] = endpoint;
                                    isPrevious[0] = true;
                                }
                            }
                        }
                    }
                }
            }

            // if overlap area exceeds 90% or total area, set overlap point to previous overlap point or threshold
            if ((DV.overlapArea[0] + DV.overlapArea[1]) / (totalArea[0] + totalArea[1]) > 0.9)
            {
                double high = Math.abs(totalArea[1]) - Math.abs(DV.overlapArea[1]);

                double low = Math.abs(DV.overlapArea[0]) - Math.abs(totalArea[0]);

                if (high >= low)
                {
                    if (isPrevious[1])
                        DV.overlapArea[1] = prevOverlap[1];
                    else
                        DV.overlapArea[1] = DV.threshold;
                }
                else
                {
                    if (isPrevious[0])
                        DV.overlapArea[0] = prevOverlap[0];
                    else
                        DV.overlapArea[0] = DV.threshold;
                }
            }

            // if a class was never misclassified set overlap point to the threshold
            if (misclassified[0] && !misclassified[1])
                DV.overlapArea[1] = DV.threshold;
            else if (!misclassified[0] && misclassified[1])
                DV.overlapArea[0] = DV.threshold;

            // set slider
            DV.overlapSlider.setValue((int) (DV.overlapArea[0] / DV.fieldLength * 200) + 200);
            DV.overlapSlider.setUpperValue((int) (DV.overlapArea[1] / DV.fieldLength * 200) + 200);
        }
        else
        {
            // set overlap area
            DV.overlapArea = new double[] { DV.fieldLength, DV.fieldLength };

            // set slider
            DV.overlapSlider.setValue(400);
            DV.overlapSlider.setUpperValue(400);
        }
    }


    /**
     * Draws graphs for specified visualization*
     */
    public static void drawGraphs()
    {
        // remove old graphs
        GRAPHS.clear();

        // remove old graphs
        DV.graphPanel.removeAll();

        // holds classes to be graphed
        ArrayList<DataObject> upperObjects = new ArrayList<>(List.of(DV.data.get(DV.upperClass)));
        ArrayList<DataObject> lowerObjects = new ArrayList<>();

        // get classes to be graphed
        if (DV.hasClasses)
        {
            for (int j = 0; j < DV.classNumber; j++)
            {
                if (DV.lowerClasses.get(j))
                    lowerObjects.add(DV.data.get(j));
            }
        }

        // calculate coordinates
        double upperScaler = getCoordinates(upperObjects);
        double lowerScaler = getCoordinates(lowerObjects);

        // get scaler
        double graphScaler = Math.max(upperScaler, lowerScaler);

        // generate analytics
        Analytics.GenerateAnalytics analytics = new Analytics.GenerateAnalytics();
        analytics.execute();

        // add upper graph
        AddGraph upperGraph = new AddGraph(upperObjects, 0, graphScaler);
        upperGraph.execute();

        // add lower graph
        AddGraph lowerGraph = null;

        if (lowerObjects.size() > 0)
        {
            lowerGraph = new AddGraph(lowerObjects, 1, graphScaler);
            lowerGraph.execute();
        }

        // wait for threads to finish
        try
        {
            // analytics thread
            analytics.get();

            // graph threads
            upperGraph.get();
            if (DV.hasClasses && lowerGraph != null)
                lowerGraph.get();
        }
        catch (ExecutionException | InterruptedException e)
        {
            e.printStackTrace();
            return;
        }

        // add graphs in order
        for (int i = 0; i < GRAPHS.size(); i++)
        {
            if (GRAPHS.containsKey(i))
                DV.graphPanel.add(GRAPHS.get(i));
        }

        // revalidate graphs and confusion matrices
        DV.graphPanel.repaint();
        DV.analyticsPanel.repaint();
        DV.graphPanel.revalidate();
        DV.analyticsPanel.revalidate();

        // warn user if graphs are scaled
        if (DV.showPopup && (upperScaler > 1 || lowerScaler > 1))
        {
            JOptionPane.showMessageDialog(DV.mainFrame,
                    """
                            Because of the size, the graphs have been zoomed out.
                            All functionality remains, but there will be empty space on each side of the graph.
                            Zoom in or scale the graphs to remove the white space.
                            """,
                    "Zoom Warning", JOptionPane.INFORMATION_MESSAGE);
            DV.showPopup = false;
        }
    }


    /**
     * Generates coordinates for dataObjects that will be graphed
     * @param dataObjects dataObjects to be graphed
     * @return whether the graphs need to be scaled or not
     */
    private static double getCoordinates(ArrayList<DataObject> dataObjects)
    {
        double graphScaler = 1;

        // get coordinates
        for (DataObject dataObject : dataObjects)
        {
            double tmpScale = dataObject.updateCoordinates(DV.angles);

            // check for greater scaler
            if (tmpScale > graphScaler)
                graphScaler = tmpScale;
        }

        return graphScaler;
    }


    /**
     * Draw graph with specified parameters
     */
    private static class AddGraph extends SwingWorker<Boolean, Void>
    {
        final ArrayList<DataObject> DATA_OBJECTS;
        final int UPPER_OR_LOWER;
        final double GRAPH_SCALER;

        /**
         * Initializes parameters
         * @param dataObjects classes to draw
         * @param upperOrLower draw up when upper (0) and down when lower (1)
         * @param graphScaler how much to zoom out the graphs
         */
        AddGraph(ArrayList<DataObject> dataObjects, int upperOrLower, double graphScaler)
        {
            this.DATA_OBJECTS = dataObjects;
            this.UPPER_OR_LOWER = upperOrLower;
            this.GRAPH_SCALER = graphScaler;
        }

        @Override
        protected Boolean doInBackground()
        {
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

            // get line height
            final double lineHeight = UPPER_OR_LOWER == 1 ? GRAPH_SCALER * -DV.fieldLength : GRAPH_SCALER * DV.fieldLength;

            // set domain lines
            domainMinLine.add(DV.domainArea[0], 0);
            domainMinLine.add(DV.domainArea[0], lineHeight);
            domainMaxLine.add(DV.domainArea[1], 0);
            domainMaxLine.add(DV.domainArea[1], lineHeight);

            // add domain series to collection
            domain.addSeries(domainMaxLine);
            domain.addSeries(domainMinLine);

            // set overlap lines
            overlapMinLine.add(DV.overlapArea[0], 0);
            overlapMinLine.add(DV.overlapArea[0], lineHeight);
            overlapMaxLine.add(DV.overlapArea[1], 0);
            overlapMaxLine.add(DV.overlapArea[1], lineHeight);

            // add overlap series to collection
            overlap.addSeries(overlapMaxLine);
            overlap.addSeries(overlapMinLine);

            // get threshold line
            thresholdLine.add(DV.threshold, 0);
            thresholdLine.add(DV.threshold, lineHeight);

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
            for (DataObject data : DATA_OBJECTS)
            {
                for (int i = 0; i < data.data.length; i++, lineCnt++)
                {
                    // start line at (0, 0)
                    lines.add(new XYSeries(lineCnt, false, true));
                    lines.get(lineCnt).add(0, 0);
                    double endpoint = data.coordinates[i][DV.fieldLength - 1][0];

                    // ensure datapoint is within domain
                    // if drawing overlap, ensure datapoint is within overlap
                    if ((!DV.domainActive || endpoint >= DV.domainArea[0] && endpoint <= DV.domainArea[1]) &&
                            (!DV.drawOverlap || (DV.overlapArea[0] <= endpoint && endpoint <= DV.overlapArea[1])))
                    {
                        // add points to lines
                        for (int j = 0; j < DV.fieldLength; j++)
                        {
                            int upOrDown = UPPER_OR_LOWER == 1 ? -1 : 1;

                            lines.get(lineCnt).add(data.coordinates[i][j][0], upOrDown * data.coordinates[i][j][1]);

                            if (j > 0 && j < DV.fieldLength - 1 && DV.angles[j] == DV.angles[j + 1])
                                midpointSeries.add(data.coordinates[i][j][0], upOrDown * data.coordinates[i][j][1]);

                            // add endpoint and timeline
                            if (j == DV.fieldLength - 1)
                            {
                                if (UPPER_OR_LOWER == 1)
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
                    new Paint[] { DV.graphColors[UPPER_OR_LOWER] },
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

            // set domain and range of graph
            double bound = GRAPH_SCALER * DV.fieldLength;
            double tick = DV.fieldLength / 10.0;

            // set domain
            ValueAxis domainView = plot.getDomainAxis();
            domainView.setRange(-bound, bound);
            NumberAxis xAxis = (NumberAxis) plot.getDomainAxis();
            xAxis.setTickUnit(new NumberTickUnit(tick));

            // set range
            ValueAxis rangeView = plot.getRangeAxis();
            NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();
            yAxis.setTickUnit(new NumberTickUnit(tick));

            // set range up or down
            if (UPPER_OR_LOWER == 1)
                rangeView.setRange(-bound * verticalScale, 0);
            else
                rangeView.setRange(0, bound * verticalScale);

            // renderer for bars
            XYIntervalSeriesCollection bars = new XYIntervalSeriesCollection();
            XYBarRenderer barRenderer = new XYBarRenderer();

            // create bar chart
            if (DV.showBars)
            {
                int[] barRanges = new int[400];
                double memberCnt = 0;

                // get bar lengths
                for (DataObject dataObj : DATA_OBJECTS)
                {
                    // get member count
                    memberCnt += dataObj.data.length;

                    // translate endpoint to slider ticks
                    // increment bar which endpoint lands
                    for (int i = 0; i < dataObj.data.length; i++)
                        barRanges[(int) (Math.round((dataObj.coordinates[i][DV.fieldLength-1][0] / DV.fieldLength * 200) + 200))]++;
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
                    if (UPPER_OR_LOWER == 1)
                        bar.add(interval, minBound, maxBound, -barRanges[i] / memberCnt, -maxBarHeight, 0);
                    else
                        bar.add(interval, minBound, maxBound, barRanges[i] / memberCnt, 0, maxBarHeight);

                    bars.addSeries(bar);

                    // set min bound to old max
                    minBound = maxBound;
                }
            }

            // create basic strokes
            BasicStroke thresholdOverlapStroke = new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[] {12f, 6f}, 0.0f);
            BasicStroke domainStroke = new BasicStroke(2f);

            // set endpoint renderer and dataset
            endpointRenderer.setSeriesShape(0, new Ellipse2D.Double(-2, -2, 4, 4));
            endpointRenderer.setSeriesPaint(0, DV.endpoints);
            plot.setRenderer(0, endpointRenderer);
            plot.setDataset(0, endpoints);

            // set threshold renderer and dataset
            thresholdRenderer.setSeriesStroke(0, thresholdOverlapStroke);
            thresholdRenderer.setSeriesPaint(0, DV.thresholdLine);
            plot.setRenderer(1, thresholdRenderer);
            plot.setDataset(1, threshold);

            // set overlap renderer and dataset
            overlapRenderer.setSeriesStroke(0, thresholdOverlapStroke);
            overlapRenderer.setSeriesStroke(1, thresholdOverlapStroke);
            overlapRenderer.setSeriesPaint(0, DV.overlapLines);
            overlapRenderer.setSeriesPaint(1, DV.overlapLines);
            plot.setRenderer(2, overlapRenderer);
            plot.setDataset(2, overlap);

            // set domain renderer and dataset
            domainRenderer.setSeriesStroke(0, domainStroke);
            domainRenderer.setSeriesStroke(1, domainStroke);
            domainRenderer.setSeriesPaint(0, DV.domainLines);
            domainRenderer.setSeriesPaint(1, DV.domainLines);
            plot.setRenderer(3, domainRenderer);
            plot.setDataset(3, domain);

            // set line renderer and dataset
            lineRenderer.setBaseStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
            lineRenderer.setAutoPopulateSeriesStroke(false);
            plot.setRenderer(4, lineRenderer);
            plot.setDataset(4, graphLines);

            // set bar or timeline renderer and dataset
            if (DV.showBars)
            {
                plot.setRenderer(5, barRenderer);
                plot.setDataset(5, bars);
            }
            else
            {
                if (UPPER_OR_LOWER == 1)
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

            // set chart size
            int vertical_res = 1;

            if (DV.classNumber == 1)
                vertical_res *= 2;

            chartPanel.setPreferredSize(new Dimension(Resolutions.singleChartPanel[0], Resolutions.singleChartPanel[1] * vertical_res));

            // show datapoint when clicked
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
                        if (UPPER_OR_LOWER == 1)
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

                                // add new line
                                if (i % 10 == 9)
                                {
                                    originalPoint.append("<br/>&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;");
                                    normalPoint.append("<br/>&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;");
                                }
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

            // add domain listeners
            DV.domainSlider.addMouseMotionListener(new MouseMotionListener()
            {
                @Override
                public void mouseDragged(MouseEvent e)
                {
                    RangeSlider slider = (RangeSlider) e.getSource();
                    DV.domainArea[0] = (slider.getValue() - 200) * DV.fieldLength / 200.0;
                    DV.domainArea[1] = (slider.getUpperValue() - 200) * DV.fieldLength / 200.0;

                    // draw lines as active (thicker)
                    BasicStroke activeStroke = new BasicStroke(4f);
                    domainRenderer.setSeriesStroke(0, activeStroke);
                    domainRenderer.setSeriesStroke(1, activeStroke);

                    // clear old domain lines
                    domainMinLine.clear();
                    domainMaxLine.clear();

                    // clear old lines, midpoints, endpoints, and timeline points
                    lines.clear();
                    graphLines.removeAllSeries();
                    midpointSeries.clear();
                    endpointSeries.clear();
                    timeLineSeries.clear();

                    // turn notify off
                    chart.setNotify(false);

                    // set overlap line
                    domainMinLine.add(DV.domainArea[0], 0);
                    domainMinLine.add(DV.domainArea[0], lineHeight);
                    domainMaxLine.add(DV.domainArea[1], 0);
                    domainMaxLine.add(DV.domainArea[1], lineHeight);

                    // number of lines
                    int lineCnt = 0;

                    // populate series
                    for (DataObject data : DATA_OBJECTS)
                    {
                        for (int i = 0; i < data.data.length; i++, lineCnt++)
                        {
                            // start line at (0, 0)
                            lines.add(new XYSeries(lineCnt, false, true));
                            lines.get(lineCnt).add(0, 0);
                            double endpoint = data.coordinates[i][DV.fieldLength - 1][0];

                            // ensure datapoint is within domain
                            // if drawing overlap, ensure datapoint is within overlap
                            if ((!DV.domainActive || endpoint >= DV.domainArea[0] && endpoint <= DV.domainArea[1]) &&
                                    (!DV.drawOverlap || (DV.overlapArea[0] <= endpoint && endpoint <= DV.overlapArea[1])))
                            {
                                // add points to lines
                                for (int j = 0; j < DV.fieldLength; j++)
                                {
                                    int upOrDown = UPPER_OR_LOWER == 1 ? -1 : 1;

                                    lines.get(lineCnt).add(data.coordinates[i][j][0], upOrDown * data.coordinates[i][j][1]);

                                    if (j > 0 && j < DV.fieldLength - 1 && DV.angles[j] == DV.angles[j + 1])
                                        midpointSeries.add(data.coordinates[i][j][0], upOrDown * data.coordinates[i][j][1]);

                                    // add endpoint and timeline
                                    if (j == DV.fieldLength - 1)
                                    {
                                        if (UPPER_OR_LOWER == 1)
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

                    // update graph
                    chart.setNotify(true);

                    // generate analytics
                    Analytics.GenerateAnalytics analytics = new Analytics.GenerateAnalytics();
                    analytics.execute();

                    // wait for generation
                    try
                    {
                        analytics.get();
                    }
                    catch (InterruptedException | ExecutionException ex)
                    {
                        ex.printStackTrace();
                    }

                    // revalidate graphs and confusion matrices
                    DV.analyticsPanel.repaint();
                    DV.analyticsPanel.revalidate();
                }

                @Override
                public void mouseMoved(MouseEvent e) {}
            });

            DV.domainSlider.addMouseListener(new MouseListener()
            {
                @Override
                public void mouseClicked(MouseEvent e) {}

                @Override
                public void mousePressed(MouseEvent e)
                {
                    // draw lines as active (thicker)
                    BasicStroke activeStroke = new BasicStroke(4f);
                    domainRenderer.setSeriesStroke(0, activeStroke);
                    domainRenderer.setSeriesStroke(1, activeStroke);
                }

                @Override
                public void mouseReleased(MouseEvent e)
                {
                    // draw lines as inactive (normal)
                    BasicStroke inactiveStroke = new BasicStroke(2f);
                    domainRenderer.setSeriesStroke(0, inactiveStroke);
                    domainRenderer.setSeriesStroke(1, inactiveStroke);
                }

                @Override
                public void mouseEntered(MouseEvent e) {}

                @Override
                public void mouseExited(MouseEvent e) {}
            });

            // add overlap listeners
            DV.overlapSlider.addMouseMotionListener(new MouseMotionListener()
            {
                @Override
                public void mouseDragged(MouseEvent e)
                {
                    RangeSlider slider = (RangeSlider) e.getSource();
                    DV.overlapArea[0] = (slider.getValue() - 200) * DV.fieldLength / 200.0;
                    DV.overlapArea[1] = (slider.getUpperValue() - 200) * DV.fieldLength / 200.0;

                    // draw lines as active (thicker)
                    BasicStroke activeStroke = new BasicStroke(4f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{12f, 6f}, 0.0f);
                    overlapRenderer.setSeriesStroke(0, activeStroke);
                    overlapRenderer.setSeriesStroke(1, activeStroke);

                    // clear old lines
                    overlapMinLine.clear();
                    overlapMaxLine.clear();

                    // turn notify off
                    chart.setNotify(false);

                    // set overlap line
                    overlapMinLine.add(DV.overlapArea[0], 0);
                    overlapMinLine.add(DV.overlapArea[0], lineHeight);
                    overlapMaxLine.add(DV.overlapArea[1], 0);
                    overlapMaxLine.add(DV.overlapArea[1], lineHeight);

                    // update graph
                    chart.setNotify(true);

                    // generate analytics
                    Analytics.GenerateAnalytics analytics = new Analytics.GenerateAnalytics();
                    analytics.execute();

                    // wait for generation
                    try
                    {
                        analytics.get();
                    }
                    catch (InterruptedException | ExecutionException ex)
                    {
                        ex.printStackTrace();
                    }

                    // revalidate graphs and confusion matrices
                    DV.analyticsPanel.repaint();
                    DV.analyticsPanel.revalidate();
                }

                @Override
                public void mouseMoved(MouseEvent e) {}
            });

            DV.overlapSlider.addMouseListener(new MouseListener()
            {
                @Override
                public void mouseClicked(MouseEvent e) {}

                @Override
                public void mousePressed(MouseEvent e)
                {
                    // draw lines as active (thicker)
                    BasicStroke activeStroke = new BasicStroke(4f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{12f, 6f}, 0.0f);
                    overlapRenderer.setSeriesStroke(0, activeStroke);
                    overlapRenderer.setSeriesStroke(1, activeStroke);
                }

                @Override
                public void mouseReleased(MouseEvent e)
                {
                    // draw lines as inactive (normal)
                    BasicStroke inactiveStroke = new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[] {12f, 6f}, 0.0f);
                    overlapRenderer.setSeriesStroke(0, inactiveStroke);
                    overlapRenderer.setSeriesStroke(1, inactiveStroke);
                }

                @Override
                public void mouseEntered(MouseEvent e) {}

                @Override
                public void mouseExited(MouseEvent e) {}
            });

            // add threshold listeners
            DV.thresholdSlider.addMouseMotionListener(new MouseMotionListener()
            {
                @Override
                public void mouseDragged(MouseEvent e)
                {
                    // get position
                    JSlider slider = (JSlider) e.getSource();
                    DV.threshold = (slider.getValue() - 200) * DV.fieldLength / 200.0;

                    // draw line as active (thicker)
                    BasicStroke activeStroke = new BasicStroke(4f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{12f, 6f}, 0.0f);
                    thresholdRenderer.setSeriesStroke(0, activeStroke);

                    // clear old line
                    thresholdLine.clear();

                    // turn notify off
                    chart.setNotify(false);

                    // set threshold line
                    thresholdLine.add(DV.threshold, 0);
                    thresholdLine.add(DV.threshold, lineHeight);

                    // update graph
                    chart.setNotify(true);

                    // generate analytics
                    Analytics.GenerateAnalytics analytics = new Analytics.GenerateAnalytics();
                    analytics.execute();

                    // wait for generation
                    try
                    {
                        analytics.get();
                    }
                    catch (InterruptedException | ExecutionException ex)
                    {
                        ex.printStackTrace();
                    }

                    // revalidate graphs and confusion matrices
                    DV.analyticsPanel.repaint();
                    DV.analyticsPanel.revalidate();
                }

                @Override
                public void mouseMoved(MouseEvent e) {}
            });

            DV.thresholdSlider.addMouseListener(new MouseListener()
            {
                @Override
                public void mouseClicked(MouseEvent e) {}

                @Override
                public void mousePressed(MouseEvent e)
                {
                    // draw lines as active (thicker)
                    BasicStroke activeStroke = new BasicStroke(4f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{12f, 6f}, 0.0f);
                    thresholdRenderer.setSeriesStroke(0, activeStroke);
                }

                @Override
                public void mouseReleased(MouseEvent e)
                {
                    // draw lines as inactive (normal)
                    BasicStroke inactiveStroke = new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[] {12f, 6f}, 0.0f);
                    thresholdRenderer.setSeriesStroke(0, inactiveStroke);
                }

                @Override
                public void mouseEntered(MouseEvent e) {}

                @Override
                public void mouseExited(MouseEvent e) {}
            });

            // add graph to graph panel
            synchronized (GRAPHS)
            {
                GRAPHS.put(UPPER_OR_LOWER, chartPanel);
            }

            return true;
        }
    }
}
