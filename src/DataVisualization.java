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
    final static Map<Integer, JPanel> REMOTE_GRAPHS = new HashMap<>();

    // vertical scale of graphs
    static double verticalScale;


    /**
     * Gets optimal angles and threshold
     * then gets overlap and domain
     * areas
     */
    public static void optimizeSetup()
    {
        // set domain are to max length
        DV.domainArea = new double[] { -DV.fieldLength, DV.fieldLength };

        if (DV.classNumber > 1)
        {
            // setup vertical scaling
            verticalScale = 0.4;

            // get optimal angles and threshold
            if (DV.glc_or_dsc)
                LDA();
            else
            {
                // set angles to 0
                Arrays.fill(DV.angles, 0);

                // get current points
                for (int i = 0; i < DV.data.size(); i++)
                {
                    if (i == DV.upperClass || DV.lowerClasses.get(i))
                        DV.data.get(i).updateCoordinatesDSC(DV.angles);
                }

                // setup angle sliders
                for (int i = 0; i < DV.data.get(0).coordinates[0].length; i++)
                    AngleSliders.createSliderPanel_DSC("feature " + i, 0, i);

                DV.angleSliderPanel.repaint();
                DV.angleSliderPanel.revalidate();
            }

            // optimize threshold
            if (DV.glc_or_dsc)
                optimizeThreshold(0);
            else
                findBestThreshold(0);

            // try again with upperIsLower false
            double upperIsLowerAccuracy = DV.accuracy;
            DV.upperIsLower = false;

            if (DV.glc_or_dsc)
                optimizeThreshold(0);
            else
                findBestThreshold(0);

            // see whether upper is actually lower
            if (DV.accuracy < upperIsLowerAccuracy)
            {
                DV.upperIsLower = true;

                if (DV.glc_or_dsc)
                    optimizeThreshold(0);
                else
                    findBestThreshold(0);
            }

            // optimize angles
            optimizeAngles(false);

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

            // setup angle sliders
            for (int i = 0; i < DV.fieldLength; i++)
                AngleSliders.createSliderPanel_GLC(DV.fieldNames.get(i), (int) (DV.angles[i] * 100), i);

            // repaint DV
            DV.mainFrame.repaint();
            DV.mainFrame.revalidate();
        }
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

            int cnt = 0;

            while ((output = reader.readLine()) != null)
            {
                // get angles then threshold
                if (cnt < DV.fieldLength)
                {
                    // update angles and create angle slider
                    DV.angles[cnt] = Double.parseDouble(output);
                    AngleSliders.createSliderPanel_GLC(DV.fieldNames.get(cnt), (int) (DV.angles[cnt] * 100), cnt);
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

            // repaint and revalidate angle sliders
            DV.angleSliderPanel.repaint();
            DV.angleSliderPanel.revalidate();
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
            File fileToDelete = new File("source\\Python\\svm_data.csv");
            Files.deleteIfExists(fileToDelete.toPath());
            fileToDelete = new File("source\\Python\\DV_data.csv");
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

    public static void findBestThreshold(double bestAccuracy)
    {
        // store current best threshold
        double bestThreshold = DV.threshold;

        // increment equals 2 slider ticks
        double increment = DV.fieldLength / 200.0;
        DV.threshold = 0;

        // search for best threshold
        // search range is 100% of total range
        for (int i = 0; i < 200; i++)
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
    public static void optimizeAngles(boolean informUser)
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
             int angleRange;

             if (DV.glc_or_dsc)
                 angleRange = 18000;
             else
                 angleRange = 9000;

             // try optimizing 200 times
             while (cnt < 200)
             {
                 // get random angles
                 for (int i = 0; i < DV.data.get(0).coordinates[0].length; i++)
                 {
                     int gradient = (rand.nextInt(11) - 5) * 100;
                     //int gradient = (rand.nextInt(91)) * 100;
                     int fieldAngle = (int) (currentBestAngles[i] * 100) + gradient;

                     if (fieldAngle < 0)
                         fieldAngle = 0;
                     else if (fieldAngle > angleRange)
                         fieldAngle = angleRange;

                     DV.angles[i] = fieldAngle / 100.0;
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

             // set accuracy and threshold to the best
             DV.threshold = currentBestThreshold;
             DV.accuracy = currentBestAccuracy;

             // remove angle sliders and set layout
             DV.angleSliderPanel.removeAll();

             // inform user if optimization was successful or now
             if (foundBetter)
             {
                 // update threshold
                 DV.threshold = currentBestThreshold;
                 DV.thresholdSlider.setValue((int) (DV.threshold / DV.fieldLength * 200) + 200);

                 // update angles
                 for (int i = 0; i < DV.data.get(0).coordinates[0].length; i++)
                 {
                     DV.angles[i] = currentBestAngles[i];

                     if (DV.glc_or_dsc)
                         AngleSliders.createSliderPanel_GLC(DV.fieldNames.get(i), (int) (DV.angles[i] * 100), i);
                     else
                         AngleSliders.createSliderPanel_DSC("feature " + i, (int) (DV.angles[i] * 100), i);
                 }



                 // inform user
                 if (informUser)
                 {
                     // redraw graphs
                     drawGraphs();
                     DV.angleSliderPanel.repaint();
                     DV.angleSliderPanel.revalidate();

                     JOptionPane.showMessageDialog(
                             DV.mainFrame,
                             "Visualization has been optimized!",
                             "Optimization Complete",
                             JOptionPane.INFORMATION_MESSAGE);
                 }
             }
             else
             {
                 // update threshold
                 DV.threshold = DV.prevThreshold;
                 DV.thresholdSlider.setValue((int) (DV.threshold / DV.fieldLength * 200) + 200);

                 // update angles
                 for (int i = 0; i < DV.data.get(0).coordinates[0].length; i++)
                 {
                     DV.angles[i] = DV.prevAngles[i];

                     if (DV.glc_or_dsc)
                         AngleSliders.createSliderPanel_GLC(DV.fieldNames.get(i), (int) (DV.angles[i] * 100), i);
                     else
                         AngleSliders.createSliderPanel_DSC("feature " + i, (int) (DV.angles[i] * 100), i);
                 }

                 // inform user
                 if (informUser)
                 {
                     // redraw graphs
                     drawGraphs();
                     DV.angleSliderPanel.repaint();
                     DV.angleSliderPanel.revalidate();

                     JOptionPane.showMessageDialog(
                             DV.mainFrame,
                             "Was unable to optimize visualization.\nVisualization is already optimal or near optimal.",
                             "Unable to Optimize",
                             JOptionPane.INFORMATION_MESSAGE);
                 }
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
            AngleSliders.createSliderPanel_GLC(DV.fieldNames.get(i), (int) (DV.angles[i] * 100), i);
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
        // update points
        for (int i = 0; i < DV.data.size(); i++)
        {
            // check if class is visualized
            if (i == DV.upperClass || DV.lowerClasses.get(i))
            {
                if (DV.glc_or_dsc)
                    DV.data.get(i).updateCoordinatesGLC(DV.angles);
                else
                    DV.data.get(i).updateCoordinatesDSC(DV.angles);
            }
        }

        // total correct points and total points
        double correctPoints = 0;
        double totalPoints = 0;

        for (int i = 0; i < DV.data.size(); i++)
        {
            totalPoints += DV.data.get(i).data.length;

            if (i == DV.upperClass || DV.lowerClasses.get(i))
            {
                for (int j = 0; j < DV.data.get(i).coordinates.length; j++)
                {
                    double endpoint = DV.data.get(i).coordinates[j][DV.data.get(i).coordinates[j].length-1][0];

                    // check if endpoint is within the subset of used data
                    if ((DV.domainArea[0] <= endpoint && endpoint <= DV.domainArea[1]) || !DV.domainActive)
                    {
                        // get classification
                        if (i == DV.upperClass && DV.upperIsLower)
                        {
                            // check if endpoint is correctly classified
                            if (endpoint < DV.threshold)
                                correctPoints++;
                        }
                        else if (i == DV.upperClass)
                        {
                            // check if endpoint is correctly classified
                            if (endpoint > DV.threshold)
                                correctPoints++;
                        }
                        else if(DV.lowerClasses.get(i) && DV.upperIsLower)
                        {
                            // check if endpoint is correctly classified
                            if (endpoint > DV.threshold)
                                correctPoints++;
                        }
                        else
                        {
                            // check if endpoint is correctly classified
                            if (endpoint < DV.threshold)
                                correctPoints++;
                        }
                    }
                }
            }
        }

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
            {
                if (DV.glc_or_dsc)
                    DV.data.get(i).updateCoordinatesGLC(DV.angles);
                else
                    DV.data.get(i).updateCoordinatesDSC(DV.angles);
            }
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
                        double endpoint = DV.data.get(i).coordinates[j][DV.data.get(i).coordinates[j].length-1][0];

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

        if (DV.displayRemoteGraphs)
            REMOTE_GRAPHS.clear();

        // remove old graphs
        DV.graphPanel.removeAll();

        if (DV.displayRemoteGraphs)
            DV.remoteGraphPanel.removeAll();

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
            {
                DV.graphPanel.add(GRAPHS.get(i));

                if (DV.displayRemoteGraphs)
                    DV.remoteGraphPanel.add(REMOTE_GRAPHS.get(i));
            }
        }

        // revalidate graphs and confusion matrices
        DV.graphPanel.repaint();
        DV.analyticsPanel.repaint();
        DV.graphPanel.revalidate();
        DV.analyticsPanel.revalidate();

        if (DV.displayRemoteGraphs)
        {
            DV.remoteGraphPanel.repaint();
            DV.remoteGraphPanel.revalidate();
        }

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
            double tmpScale;

            if (DV.glc_or_dsc)
                tmpScale = dataObject.updateCoordinatesGLC(DV.angles);
            else
                tmpScale = dataObject.updateCoordinatesDSC(DV.angles);

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

            // create SVM renderer and dataset
            XYLineAndShapeRenderer svmLineRenderer = new XYLineAndShapeRenderer(true, false);
            XYSeriesCollection svmSeriesCol = new XYSeriesCollection();
            ArrayList<XYSeries> svmLines = new ArrayList<>();

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
            XYLineAndShapeRenderer svmEndpointRenderer = new XYLineAndShapeRenderer(false, true);
            XYLineAndShapeRenderer midpointRenderer = new XYLineAndShapeRenderer(false, true);
            XYLineAndShapeRenderer svmMidpointRenderer = new XYLineAndShapeRenderer(false, true);
            XYLineAndShapeRenderer timeLineRenderer = new XYLineAndShapeRenderer(false, true);
            XYLineAndShapeRenderer svmTimeLineRenderer = new XYLineAndShapeRenderer(false, true);
            XYSeriesCollection endpoints = new XYSeriesCollection();
            XYSeriesCollection svmEndpoints = new XYSeriesCollection();
            XYSeriesCollection midpoints = new XYSeriesCollection();
            XYSeriesCollection svmMidpoints = new XYSeriesCollection();
            XYSeriesCollection timeLine = new XYSeriesCollection();
            XYSeriesCollection svmTimeLine = new XYSeriesCollection();
            XYSeries endpointSeries = new XYSeries(0, false, true);
            XYSeries svmEndpointSeries = new XYSeries(0, false, true);
            XYSeries midpointSeries = new XYSeries(1, false, true);
            XYSeries svmMidpointSeries = new XYSeries(1, false, true);
            XYSeries timeLineSeries = new XYSeries(2, false, true);
            XYSeries svmTimeLineSeries = new XYSeries(3, false, true);

            // populate svm series
            if (DV.drawOnlySVM || DV.drawSVM)
            {
                // update coordinates
                getCoordinates(new ArrayList<>(List.of(DV.supportVectors)));

                for (int i = 0, lineCnt = 0, seriesCnt = 0; i < DV.supportVectors.data.length; i++, lineCnt++)
                {
                    // start line at (0, 0)
                    svmLines.add(new XYSeries(lineCnt, false, true));
                    if (DV.showFirstSeg)
                        svmLines.get(lineCnt).add(0, 0);
                    double endpoint = DV.supportVectors.coordinates[i][DV.supportVectors.coordinates[i].length-1][0];

                    // ensure datapoint is within domain
                    if (!DV.domainActive || (endpoint >= DV.domainArea[0] && endpoint <= DV.domainArea[1]))
                    {
                        for (int j = 0; j < DV.supportVectors.coordinates[i].length; j++)
                        {
                            int upOrDown = UPPER_OR_LOWER == 1 ? -1 : 1;

                            svmLines.get(lineCnt).add(DV.supportVectors.coordinates[i][j][0], upOrDown * DV.supportVectors.coordinates[i][j][1]);

                            if (j > 0 && j < DV.supportVectors.coordinates[i].length - 1 && DV.angles[j] == DV.angles[j + 1])
                                svmMidpointSeries.add(DV.supportVectors.coordinates[i][j][0], upOrDown * DV.supportVectors.coordinates[i][j][1]);

                            // add endpoint and timeline
                            if (j == DV.supportVectors.coordinates[i].length - 1)
                            {
                                svmEndpointSeries.add(DV.supportVectors.coordinates[i][j][0], upOrDown * DV.supportVectors.coordinates[i][j][1]);
                                svmTimeLineSeries.add(DV.supportVectors.coordinates[i][j][0], 0);
                            }
                            //else
                                //svmMidpointSeries.add(DV.supportVectors.coordinates[i][j][0], upOrDown * DV.supportVectors.coordinates[i][j][1]);
                        }
                    }

                    // add to dataset if within domain
                    if (!DV.domainActive || endpoint >= DV.domainArea[0] && endpoint <= DV.domainArea[1])
                    {
                        svmSeriesCol.addSeries(svmLines.get(lineCnt));
                        svmLineRenderer.setSeriesPaint(seriesCnt++, DV.svmLines);
                    }
                }
            }

            if (!DV.drawOnlySVM)
            {
                // populate main series
                for (DataObject data : DATA_OBJECTS)
                {
                    for (int i = 0, lineCnt = 0; i < data.data.length; i++, lineCnt++)
                    {
                        // start line at (0, 0)
                        lines.add(new XYSeries(lineCnt, false, true));
                        if (DV.showFirstSeg)
                            lines.get(lineCnt).add(0, 0);
                        double endpoint = data.coordinates[i][data.coordinates[i].length - 1][0];

                        // ensure datapoint is within domain
                        // if drawing overlap, ensure datapoint is within overlap
                        if ((!DV.domainActive || endpoint >= DV.domainArea[0] && endpoint <= DV.domainArea[1]) &&
                                (!DV.drawOverlap || (DV.overlapArea[0] <= endpoint && endpoint <= DV.overlapArea[1])))
                        {
                            // add points to lines
                            for (int j = 0; j < data.coordinates[i].length; j++)
                            {
                                int upOrDown = UPPER_OR_LOWER == 1 ? -1 : 1;

                                lines.get(lineCnt).add(data.coordinates[i][j][0], upOrDown * data.coordinates[i][j][1]);

                                if (j > 0 && j < data.coordinates[i].length - 1 && DV.angles[j] == DV.angles[j + 1])
                                    midpointSeries.add(data.coordinates[i][j][0], upOrDown * data.coordinates[i][j][1]);

                                // add endpoint and timeline
                                if (j == data.coordinates[i].length - 1)
                                {
                                    endpointSeries.add(data.coordinates[i][j][0], upOrDown * data.coordinates[i][j][1]);
                                    timeLineSeries.add(data.coordinates[i][j][0], 0);
                                }
                                //else
                                    //midpointSeries.add(data.coordinates[i][j][0], upOrDown * data.coordinates[i][j][1]);
                            }
                        }

                        // add to dataset if within domain
                        if (!DV.domainActive || endpoint >= DV.domainArea[0] && endpoint <= DV.domainArea[1])
                            graphLines.addSeries(lines.get(lineCnt));
                    }
                }
            }

            // add data to series
            endpoints.addSeries(endpointSeries);
            svmEndpoints.addSeries(svmEndpointSeries);
            midpoints.addSeries(midpointSeries);
            svmMidpoints.addSeries(svmMidpointSeries);
            timeLine.addSeries(timeLineSeries);
            svmTimeLine.addSeries(svmTimeLineSeries);

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
                double max = 0;

                // get bar lengths
                for (DataObject dataObj : DATA_OBJECTS)
                {
                    // translate endpoint to slider ticks
                    // increment bar which endpoint lands
                    for (int i = 0; i < dataObj.data.length; i++)
                    {
                        int tmpTick = (int) (Math.round((dataObj.coordinates[i][DV.fieldLength-1][0] / DV.fieldLength * 200) + 200));
                        barRanges[tmpTick]++;

                        if (barRanges[tmpTick] > max)
                            max = barRanges[tmpTick];
                    }
                }

                // get interval and bounds
                double interval = DV.fieldLength / 200.0;
                double maxBound = -DV.fieldLength;
                double minBound = -DV.fieldLength;

                // get maximum bar height
                double maxBarHeight = DV.fieldLength / 10.0;

                // add series to collection
                for (int i = 0; i < 400; i++)
                {
                    // get max bound
                    maxBound += interval;

                    XYIntervalSeries bar = new XYIntervalSeries(i, false, true);

                    // bar width = interval
                    // bar height = (total endpoints on bar) / (total endpoints)
                    if (UPPER_OR_LOWER == 1)
                        bar.add(interval, minBound, maxBound, (-barRanges[i] / max) * maxBarHeight, -maxBarHeight, 0);
                    else
                        bar.add(interval, minBound, maxBound, (barRanges[i] / max) * maxBarHeight, 0, maxBarHeight);

                    bars.addSeries(bar);

                    // set min bound to old max
                    minBound = maxBound;
                }
            }

            // create basic strokes
            BasicStroke thresholdOverlapStroke = new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[] {12f, 6f}, 0.0f);
            BasicStroke domainStroke = new BasicStroke(1f);

            // set svm endpoint renderer and dataset
            svmEndpointRenderer.setSeriesShape(0, new Ellipse2D.Double(-1, -1, 2, 2));
            svmEndpointRenderer.setSeriesPaint(0, DV.svmEndpoints);
            plot.setRenderer(0, svmEndpointRenderer);
            plot.setDataset(0, svmEndpoints);

            // set endpoint renderer and dataset
            endpointRenderer.setSeriesShape(0, new Ellipse2D.Double(-1, -1, 2, 2));
            endpointRenderer.setSeriesPaint(0, DV.endpoints);
            plot.setRenderer(1, endpointRenderer);
            plot.setDataset(1, endpoints);

            // set mvm midpoint renderer and dataset
            svmMidpointRenderer.setSeriesShape(0, new Ellipse2D.Double(-0.5, -0.5, 1, 1));
            svmMidpointRenderer.setSeriesPaint(0, DV.svmEndpoints);
            plot.setRenderer(2, svmMidpointRenderer);
            plot.setDataset(2, svmMidpoints);

            // set midpoint renderer and dataset
            midpointRenderer.setSeriesShape(0, new Ellipse2D.Double(-0.5, -0.5, 1, 1));
            midpointRenderer.setSeriesPaint(0, DV.endpoints);
            plot.setRenderer(3, midpointRenderer);
            plot.setDataset(3, midpoints);

            // set threshold renderer and dataset
            thresholdRenderer.setSeriesStroke(0, thresholdOverlapStroke);
            thresholdRenderer.setSeriesPaint(0, DV.thresholdLine);
            plot.setRenderer(4, thresholdRenderer);
            plot.setDataset(4, threshold);

            // set overlap renderer and dataset
            overlapRenderer.setSeriesStroke(0, thresholdOverlapStroke);
            overlapRenderer.setSeriesStroke(1, thresholdOverlapStroke);
            overlapRenderer.setSeriesPaint(0, DV.overlapLines);
            overlapRenderer.setSeriesPaint(1, DV.overlapLines);
            plot.setRenderer(5, overlapRenderer);
            plot.setDataset(5, overlap);

            if (DV.domainActive)
            {
                // set domain renderer and dataset
                domainRenderer.setSeriesStroke(0, domainStroke);
                domainRenderer.setSeriesStroke(1, domainStroke);
                domainRenderer.setSeriesPaint(0, DV.domainLines);
                domainRenderer.setSeriesPaint(1, DV.domainLines);
                plot.setRenderer(6, domainRenderer);
                plot.setDataset(6, domain);
            }

            // set svm line renderer and dataset
            svmLineRenderer.setBaseStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
            svmLineRenderer.setAutoPopulateSeriesStroke(false);
            plot.setRenderer(7, svmLineRenderer);
            plot.setDataset(7, svmSeriesCol);

            // set line renderer and dataset
            lineRenderer.setBaseStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
            lineRenderer.setAutoPopulateSeriesStroke(false);
            plot.setRenderer(8, lineRenderer);
            plot.setDataset(8, graphLines);

            // set bar or timeline renderer and dataset
            if (DV.showBars)
            {
                barRenderer.setShadowVisible(false);
                plot.setRenderer(9, barRenderer);
                plot.setDataset(9, bars);
            }
            else
            {
                svmTimeLineRenderer.setSeriesPaint(0, DV.svmLines);
                plot.setRenderer(9, svmTimeLineRenderer);
                plot.setDataset(9, svmTimeLine);

                if (UPPER_OR_LOWER == 1)
                    timeLineRenderer.setSeriesShape(0, new Rectangle2D.Double(-0.25, 0, 0.5, 3));
                else
                    timeLineRenderer.setSeriesShape(0, new Rectangle2D.Double(-0.25, -3, 0.5, 3));

                plot.setRenderer(10, timeLineRenderer);
                plot.setDataset(10, timeLine);
            }

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
                            if (DV.glc_or_dsc)
                                lines.get(lineCnt).add(0, 0);
                            double endpoint = data.coordinates[i][data.coordinates[i].length - 1][0];

                            // ensure datapoint is within domain
                            // if drawing overlap, ensure datapoint is within overlap
                            if ((!DV.domainActive || endpoint >= DV.domainArea[0] && endpoint <= DV.domainArea[1]) &&
                                    (!DV.drawOverlap || (DV.overlapArea[0] <= endpoint && endpoint <= DV.overlapArea[1])))
                            {
                                // add points to lines
                                for (int j = 0; j < data.coordinates[i].length; j++)
                                {
                                    int upOrDown = UPPER_OR_LOWER == 1 ? -1 : 1;

                                    lines.get(lineCnt).add(data.coordinates[i][j][0], upOrDown * data.coordinates[i][j][1]);

                                    if (j > 0 && j < data.coordinates[i].length - 1 && DV.angles[j] == DV.angles[j + 1])
                                        midpointSeries.add(data.coordinates[i][j][0], upOrDown * data.coordinates[i][j][1]);

                                    // add endpoint and timeline
                                    if (j == data.coordinates[i].length - 1)
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

            if (DV.displayRemoteGraphs)
            {
                try
                {
                    // create the graph panel and add it to the main panel
                    ChartPanel chartPanel2 = new ChartPanel((JFreeChart) chart.clone());
                    chartPanel2.setMouseWheelEnabled(true);
                    chartPanel2.setPreferredSize(new Dimension(Resolutions.singleChartPanel[0], Resolutions.singleChartPanel[1] * vertical_res));

                    // show datapoint when clicked
                    chartPanel2.addChartMouseListener(new ChartMouseListener()
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

                    // add graph to graph panel
                    synchronized (REMOTE_GRAPHS)
                    {
                        REMOTE_GRAPHS.put(UPPER_OR_LOWER, chartPanel2);
                    }
                }
                catch(CloneNotSupportedException e)
                {
                    e.printStackTrace();
                }
            }

            return true;
        }
    }
}
