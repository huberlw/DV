import Sliders.RangeSlider;
import Sliders.RangeSliderUI;
import org.jfree.chart.*;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.entity.XYItemEntity;
import org.jfree.chart.labels.StandardXYItemLabelGenerator;
import org.jfree.chart.plot.*;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.*;
import org.jfree.ui.RectangleInsets;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
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
    //final static Map<Integer, JPanel> GRAPHS = new HashMap<>();
    final static Map<Integer, JFreeChart> GRAPHS = new HashMap<>();
    final static Map<Integer, JPanel> REMOTE_GRAPHS = new HashMap<>();

    final static JPanel ldfPanel = new JPanel();
    final static JPanel labelPanel = new JPanel();
    final static JTabbedPane limitSliderPane = new JTabbedPane();

    static JSlider[][] weightSliders;
    static RangeSlider[] sliders;
    static JLabel[] sliderLabels;


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
            // get optimal angles and threshold
            if (DV.glc_or_dsc)
            {
                LDA();
            }
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

            if (DV.glc_or_dsc)
            {
                // optimize angles
                optimizeAngles(false);

                DV.angleSliderPanel.removeAll();

                // bubble sort ascending

                for (int j = 0; j < DV.fieldLength; j++)
                    AngleSliders.createSliderPanel_GLC(DV.fieldNames.get(j), (int) (DV.angles[j] * 100), j);
            }

            // get overlap area
            getOverlap();

        }
        else
        {
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
    public static void createCSVFile()
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
                for (int j = 0; j < DV.trainData.get(i).data.length; j++)
                {
                    for (int k = 0; k < DV.fieldLength; k++)
                    {
                        if (k != DV.fieldLength - 1)
                            out.printf("%f,", DV.trainData.get(i).data[j][k]);
                        else
                            out.printf("%f," + curClass + "\n", DV.trainData.get(i).data[j][k]);
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
                    AngleSliders.createSliderPanel_GLC("tmp " + cnt, (int) (DV.angles[cnt] * 100), cnt);//DV.fieldNames.get(cnt), (int) (DV.angles[cnt] * 100), cnt);
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
        DV.threshold = -DV.fieldLength;

        // search for best threshold
        // search range is 100% of total range
        for (int i = 0; i < 400; i++)
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
             int[] angleRange;

             if (DV.glc_or_dsc)
                 angleRange = new int[]{ 0, 180 };
             else
                 angleRange = new int[]{ -90, 90 };

             // try optimizing 200 times
             while (cnt < 200)
             {
                 // get random angles
                 for (int i = 0; i < DV.data.get(0).coordinates[0].length; i++)
                 {
                     int gradient = (rand.nextInt(101) - 50);
                     double fieldAngle = currentBestAngles[i] + gradient;

                     if (fieldAngle < angleRange[0])
                         fieldAngle = angleRange[0];
                     else if (fieldAngle > angleRange[1])
                         fieldAngle = angleRange[1];

                     DV.angles[i] = fieldAngle;
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
                         AngleSliders.createSliderPanel_GLC("tmp " + 1, (int) (DV.angles[i] * 100), i);//DV.fieldNames.get(i), (int) (DV.angles[i] * 100), i);
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


    public static void normalizeAngles()
    {
        if (DV.data != null)
        {
            /***
             * CONSTRUCTION
             */
            double minRange;
            double maxRange;

            JPanel normPanel = new JPanel();
            normPanel.add(new JLabel("Choose a normalization style or click \"Help\" for more information on normalization styles."));

            int choice = -2;

            while (choice == -2)
            {
                choice = JOptionPane.showOptionDialog(
                        DV.mainFrame, normPanel,
                        "Normalize Angles",
                        JOptionPane.YES_NO_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE,
                        null,
                        new Object[]{"z-Score Min-Max", "Min-Max", "Help"},
                        null);

                if (choice == 2)
                {
                    DV.normalizationInfoPopup();
                    choice = -2;
                }
            }

            double max = Double.MIN_VALUE;
            double min = Double.MAX_VALUE;

            if (choice == 0)
            {
                // mean and standard deviation per column
                double mean = 0;
                double sd = 0;

                // get mean for each column
                for (int i = 0; i < DV.angles.length; i++)
                    mean += DV.angles[i];

                mean /= DV.angles.length;

                // get standard deviation for each column
                for (int i = 0; i < DV.angles.length; i++)
                {
                    sd += Math.pow(DV.angles[i] - mean, 2);

                    if (sd < 0.001)
                    {
                        String message = String.format("""
                            Standard deviation in column %d is less than 0.001.
                            Please manually enter a minimum and maximum. The standard deviation will get(max - min) / 2.
                            Else, the standard deviation will get 0.001.""", i+1);

                        // ask user for manual min max entry
                        double[] manualMinMax = DataSetup.manualMinMaxEntry(message);

                        // use manual min max or default to 0.001 if null
                        if (manualMinMax != null)
                            sd = (manualMinMax[1] / manualMinMax[0]) / 2;
                        else
                            sd = 0.001;
                    }
                }

                sd = Math.sqrt(sd / DV.angles.length);

                // get min and max values
                for (int i = 0; i < DV.angles.length; i++)
                {
                    DV.angles[i] = (DV.angles[i] - mean) / sd;

                    if (DV.angles[i] < min)
                        min = DV.angles[i];
                    else if (DV.angles[i] > max)
                        max = DV.angles[i];
                }
            }
            else
            {
                for (int i = 0; i < DV.angles.length; i++)
                {
                    if (DV.angles[i] < min)
                        min = DV.angles[i];
                    else if (DV.angles[i] > max)
                        max = DV.angles[i];
                }
            }

            DV.angleSliderPanel.removeAll();

            // normalize between [0, 90]
            for (int i = 0; i < DV.angles.length; i++)
            {
                DV.angles[i] = ((DV.angles[i] - min) / (max - min)) * 90;

                if (DV.glc_or_dsc)
                    AngleSliders.createSliderPanel_GLC(DV.fieldNames.get(i), (int) (DV.angles[i] * 100), i);
                else
                    AngleSliders.createSliderPanel_DSC("feature " + i, (int) (DV.angles[i] * 100), i);
            }

            DV.upperIsLower = true;

            // optimize threshold
            findBestThreshold(0);

            // try again with upperIsLower false
            double upperIsLowerAccuracy = DV.accuracy;
            DV.upperIsLower = false;

            findBestThreshold(0);

            // see whether upper is actually lower
            if (DV.accuracy < upperIsLowerAccuracy)
            {
                DV.upperIsLower = true;
                findBestThreshold(0);
            }

            drawGraphs();
        }
        else
            JOptionPane.showMessageDialog(
                    DV.mainFrame,
                    "Please create a project before normalizing angles.\nFor additional information, please view the \"Help\" tab.",
                    "Error: could not normalize angles",
                    JOptionPane.ERROR_MESSAGE);
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
            if (!DV.useOverlapPercent)
            {
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
                ArrayList<Double> allPnts = new ArrayList<>();
                ArrayList<Integer> allInd = new ArrayList<>();

                for (int i = 0, ind = 0; i < DV.data.size(); i++)
                {
                    if (DV.glc_or_dsc)
                        DV.data.get(i).updateCoordinatesGLC(DV.angles);
                    else
                        DV.data.get(i).updateCoordinatesDSC(DV.angles);

                    for (int j = 0; j < DV.data.get(i).data.length; j++)
                    {
                        allPnts.add(DV.data.get(i).coordinates[i][DV.data.get(i).coordinates[i].length - 1][0]);
                        allInd.add(ind++);
                    }
                }

                // bubble sort
                for (int i = 0; i < allPnts.size() - 1; i++)
                {
                    for (int j = 0; j < allPnts.size() - i - 1; j++)
                    {
                        if (allPnts.get(j) > allPnts.get(j+1))
                        {
                            double tmp1 = allPnts.get(j);
                            allPnts.set(j, allPnts.get(j+1));
                            allPnts.set(j+1, tmp1);

                            int tmp2 = allInd.get(j);
                            allInd.set(j, allInd.get(j+1));
                            allInd.set(j+1, tmp2);
                        }
                    }
                }

                // get index of threshold
                int high = 0;

                while (allPnts.get(high) < DV.threshold) high++;
                int low = high - 1;

                int collected = 0;
                int need = (int) Math.round(allPnts.size() * DV.overlapPercent);

                /***
                 * CONSTRUCTION HERE
                 */
                while (collected < need)
                {
                    if (DV.threshold - allPnts.get(low) < allPnts.get(high) - DV.threshold)
                    {
                        DV.overlapArea[0] = allPnts.get(low--);
                        while (allPnts.get(low) == allPnts.get(--low)) collected++;

                    }
                    else
                    {
                        DV.overlapArea[0] = allPnts.get(high++);
                    }
                }
            }
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

        double maxFrequency = getMaxFrequency();

        // get scaler
        double graphScaler = Math.max(upperScaler, lowerScaler);

        // generate analytics
        Analytics.GenerateAnalytics analytics = new Analytics.GenerateAnalytics();
        analytics.execute();

        // add upper graph
        AddGraph upperGraph = new AddGraph(upperObjects, 0, graphScaler, maxFrequency);
        upperGraph.execute();

        // add lower graph
        AddGraph lowerGraph = null;

        if (lowerObjects.size() > 0)
        {
            lowerGraph = new AddGraph(lowerObjects, 1, graphScaler, maxFrequency);
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

        GridBagConstraints gpc = new GridBagConstraints();
        gpc.gridx = 0;
        gpc.gridy = 0;
        gpc.weightx = 1;
        gpc.weighty = 1;
        gpc.fill = GridBagConstraints.BOTH;

        CombinedDomainXYPlot plot = new CombinedDomainXYPlot();
        plot.setOrientation(PlotOrientation.VERTICAL);
        plot.setGap(0);
        plot.getDomainAxis().setVisible(false);
        plot.setOutlinePaint(null);
        plot.setOutlineVisible(false);
        plot.setInsets(RectangleInsets.ZERO_INSETS);
        plot.setDomainPannable(true);
        plot.setRangePannable(true);
        plot.setBackgroundPaint(DV.background);
        plot.setDomainGridlinePaint(Color.GRAY);
        plot.setRangeGridlinePaint(Color.GRAY);

        JFreeChart chart = new JFreeChart("", null, plot, false);

        // add graphs in order
        for (int i = 0; i < GRAPHS.size(); i++)
        {
            if (GRAPHS.containsKey(i))
            {
                plot.add((XYPlot) GRAPHS.get(i).getPlot(), 1);

                if (DV.displayRemoteGraphs)
                    DV.remoteGraphPanel.add(REMOTE_GRAPHS.get(i), gpc);
            }
        }

        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setMouseWheelEnabled(true);
        chartPanel.addChartMouseListener(new ChartMouseListener()
        {
            @Override
            public void chartMouseClicked(ChartMouseEvent e)
            {
                // get clicked entity
                ChartEntity ce = e.getEntity();

                if (ce instanceof XYItemEntity xy)
                {
                    System.out.println(xy.getDataset().getSeriesCount());

                    // ensure entity is endpoint
                    if (xy.getDataset().getSeriesCount() > 1)
                    {
                        // get index
                        int index = xy.getSeriesIndex();

                        // get item
                        int item = xy.getItem();
                        XYDataset dataset = xy.getDataset();
                        double y = dataset.getYValue(index, item);

                        StringBuilder curClassName = new StringBuilder();
                        StringBuilder opClassName = new StringBuilder();

                        int curClass = 0;

                        // if upper class than curClass is upperClass
                        // otherwise search for class
                        if (y < 0)
                        {
                            opClassName.append(DV.data.get(DV.upperClass).className);

                            // loop through classes until the class containing index is found
                            for (int i = 0; i < DV.classNumber; i++)
                            {
                                if (DV.lowerClasses.get(i))
                                {
                                    curClassName.append(DV.data.get(i).className);

                                    if (i != DV.data.size() - 1)
                                        curClassName.append(", ");

                                    // if true, index is within the current class
                                    if (DV.data.get(i).data.length > index)
                                    {
                                        curClass = i;
                                        break;
                                    }
                                    else
                                    {
                                        index -= DV.data.get(i).data.length;
                                    }
                                }
                            }
                        }
                        else
                        {
                            curClass = DV.upperClass;
                            curClassName.append(DV.data.get(curClass).className);

                            for (int i = 0; i < DV.data.size(); i++)
                            {
                                if (DV.lowerClasses.get(i))
                                {
                                    opClassName.append(DV.data.get(i).className);

                                    if (i != DV.data.size() - 1)
                                        opClassName.append(", ");
                                }
                            }
                        }

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
                        String chosenDataPoint = "<html>" + "<b>Class: </b>" + DV.uniqueClasses.get(curClass) + "<br/>" + originalPoint + "<br/>" + normalPoint;

                        // get mouse location
                        Point mouseLoc = MouseInfo.getPointerInfo().getLocation();
                        int mouseX = (int) mouseLoc.getX();
                        int mouseY = (int) mouseLoc.getY();

                        // create popup JOptionPane(Object message, int messageType, int optionType, Icon icon, Object[] options, Object initialValue)
                        JOptionPane optionPane = new JOptionPane(chosenDataPoint + "</html>", JOptionPane.INFORMATION_MESSAGE, JOptionPane.OK_CANCEL_OPTION, null, new Object[]{"Highlight Point", "Create LDF"}, 0);
                        JDialog dialog = optionPane.createDialog(null, "Datapoint");
                        dialog.setLocation(mouseX, mouseY);
                        dialog.setVisible(true);

                        // create LDF function menu
                        if (optionPane.getValue() != null)
                        {
                            if (optionPane.getValue().equals("Highlight Point"))
                            {
                                DV.highlights[curClass][index] = !DV.highlights[curClass][index];
                                DataVisualization.drawGraphs();
                            }
                            else
                            {
                                DV.activeLDF = true;

                                DV.scale = new double[2][DV.fieldLength];

                                for (int i = 0; i < DV.fieldLength; i++)
                                {
                                    DV.scale[0][i] = 1;
                                    DV.scale[1][i] = 1;
                                }

                                // create panels for graph and weight sliders
                                JFrame ldfFrame = new JFrame();
                                ldfFrame.setLocationRelativeTo(DV.mainFrame);
                                ldfFrame.addWindowListener(new WindowAdapter()
                                {
                                    @Override
                                    public void windowClosing(WindowEvent e)
                                    {
                                        DV.activeLDF = false;
                                    }
                                });


                                ldfFrame.setLayout(new GridBagLayout());
                                GridBagConstraints c = new GridBagConstraints();

                                String ldfInfoBase = chosenDataPoint + "<br/><br/>" + "<b>Generalized Rule: </b>";
                                drawLDFRule(ldfInfoBase, curClassName.toString(), opClassName.toString(), curClass, index);

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
                                sliders = new RangeSlider[DV.fieldLength];
                                sliderLabels = new JLabel[DV.fieldLength];
                                DV.limits = new double[DV.fieldLength][];
                                DV.discrete = new boolean[DV.fieldLength];

                                limitSliderPane.removeAll();

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

                                    DV.discrete[i] = false;
                                    DV.limits[i] = new double[]{0, 5 * DV.data.get(curClass).data[index][i]};

                                    sliders[i].setMinimum(0);
                                    sliders[i].setMaximum(500);
                                    sliders[i].setMajorTickSpacing(1);
                                    sliders[i].setValue(0);
                                    sliders[i].setUpperValue(500);
                                    sliders[i].setToolTipText("Sets lower and upper limits for " + DV.fieldNames.get(i));
                                    JLabel sl = new JLabel(limitSliderLabel(curClass, index, i));
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
                                    JCheckBox dBox = new JCheckBox("<html><b>Discrete Attribute:</b> " + DV.discrete[i] + "</html>", DV.discrete[i]);
                                    dBox.setFont(dBox.getFont().deriveFont(16f));
                                    dBox.setToolTipText("Whether the current attribute is discrete. Discrete attributes are always whole numbers.");
                                    final int finalI = i;
                                    final int finalCurClass = curClass;
                                    final int finalIndex = index;
                                    dBox.addChangeListener(de ->
                                    {
                                        DV.discrete[finalI] = dBox.isSelected();
                                        dBox.setText("<html><b>Discrete Attribute:</b> " + DV.discrete[finalI] + "</html>");

                                        drawLDF(finalCurClass, finalIndex, y < 0 ? 1 : 0, ldfInfoBase, curClassName.toString(), opClassName.toString());
                                    });

                                    panelC.gridy = 1;
                                    sliderPanels[i].add(dBox, panelC);

                                    // add slider
                                    panelC.gridy = 2;
                                    sliderPanels[i].add(sliders[i], panelC);

                                    limitSliderPane.add(DV.fieldNames.get(i), sliderPanels[i]);
                                }

                                // add limit sliders
                                c.gridy = 2;
                                c.weighty = 0;
                                ldfFrame.add(limitSliderPane, c);

                                JPanel lowerScalePanel = new JPanel();
                                lowerScalePanel.setLayout(new BoxLayout(lowerScalePanel, BoxLayout.PAGE_AXIS));
                                JScrollPane lowerScaleScroll = new JScrollPane(lowerScalePanel);

                                JPanel upperScalePanel = new JPanel();
                                upperScalePanel.setLayout(new BoxLayout(upperScalePanel, BoxLayout.PAGE_AXIS));
                                JScrollPane upperScaleScroll = new JScrollPane(upperScalePanel);

                                JTabbedPane scaleTabs = new JTabbedPane();
                                scaleTabs.add("Lower Scale", lowerScaleScroll);
                                scaleTabs.add("Upper Scale", upperScaleScroll);

                                // create anglesliders
                                weightSliders = new JSlider[DV.fieldLength][2];

                                for (int i = 0; i < DV.fieldLength; i++)
                                {
                                    if ((DV.data.get(curClass).data[index][i] != 0 && DV.angles[i] <= 90) || DV.angles[i] > 90)
                                    {
                                        // lower
                                        weightSliders[i][0] = new JSlider(0, 500, (int)(DV.scale[0][i] * 100));
                                        lowerScalePanel.add(AngleSliders.createWeightSliderPanel_GLC(
                                                weightSliders[i][0],
                                                DV.fieldNames.get(i),
                                                (int)(DV.scale[0][i] * 100),
                                                i,
                                                ldfInfoBase,
                                                curClassName.toString(),
                                                opClassName.toString(),
                                                curClass,
                                                index,
                                                y < 0 ? 1 : 0,
                                                0));

                                        // upper
                                        weightSliders[i][1] = new JSlider(0, 500, (int)(DV.scale[1][i] * 100));
                                        upperScalePanel.add(AngleSliders.createWeightSliderPanel_GLC(
                                                weightSliders[i][1],
                                                DV.fieldNames.get(i),
                                                (int)(DV.scale[1][i] * 100),
                                                i,
                                                ldfInfoBase,
                                                curClassName.toString(),
                                                opClassName.toString(),
                                                curClass,
                                                index,
                                                y < 0 ? 1 : 0,
                                                1));
                                    }
                                }

                                // add scales
                                c.gridx = 1;
                                c.gridy = 1;
                                c.weightx = 0.2;
                                c.weighty = 1;
                                c.gridheight = 2;
                                ldfFrame.add(scaleTabs, c);

                                // draw graphs
                                drawLDF(curClass, index, y < 0 ? 1 : 0, ldfInfoBase, curClassName.toString(), opClassName.toString());

                                // show
                                ldfFrame.setVisible(true);
                                ldfFrame.revalidate();
                                ldfFrame.pack();
                                ldfFrame.repaint();
                            }
                        }
                    }
                }
            }

            @Override
            public void chartMouseMoved(ChartMouseEvent chartMouseEvent) {}
        });

        DV.graphPanel.add(chartPanel, gpc);

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
        if (!DV.showPopup && (upperScaler > 1 || lowerScaler > 1))
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


    public static void drawReg() throws FileNotFoundException {
        try
        {
            // create csv file
            File csv = new File("D:\\GitHub\\DV\\datasets\\DV_data.csv");
            Files.deleteIfExists(csv.toPath());

            // write to csv file
            PrintWriter out = new PrintWriter(csv);

            // create header for file
            for (int i = 0; i < DV.fieldLength; i++)
            {
                if (i != DV.fieldLength - 1)
                    out.print("feature,");
                else
                    out.print("feature\n");
            }

            // check all classes
            for (int i = 0; i < DV.data.size(); i++)
            {
                // get all data for class
                for (int j = 0; j < DV.data.get(i).data.length; j++)
                {
                    for (int k = 0; k < DV.fieldLength; k++)
                    {
                        if (k != DV.fieldLength - 1)
                            out.printf("%f,", DV.data.get(i).data[j][k]);
                        else
                            out.printf("%f" + "\n", DV.data.get(i).data[j][k]);
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

        System.exit(0);

        String[][] outputData;

        File datafile = new File("C:\\Users\\Administrator\\GitHub\\DV\\datasets\\regression_info.csv");
        try (Scanner fileReader = new Scanner(datafile))
        {
            ArrayList<String> rowData = new ArrayList<>();

            // put rows of data into arraylist
            while(fileReader.hasNextLine())
                rowData.add(fileReader.nextLine());

            ArrayList<String[]> data = new ArrayList<>();

            // split rows by ","
            for (String rowDatum : rowData)
            {
                String[] tmp = rowDatum.split(",");

                // only add if there is data
                if (tmp.length > 0)
                    data.add(tmp);
            }

            // arraylist to array
            outputData = new String[data.size()][];
            outputData = data.toArray(outputData);
        }
        DV.fieldLength = outputData[0].length;
        DV.angles = new double[outputData[0].length];

        for (int i = 0; i < outputData[0].length; i++)
        {
            DV.angles[i] = Double.parseDouble(outputData[0][i]);
        }

        double intercept = Double.parseDouble(outputData[1][0]);

        double div = Double.parseDouble(outputData[2][0]);

        double[] orig = new double[DV.data.get(0).data.length];

        for (int i = 0; i < DV.data.get(0).data.length; i++)
        {
            double[] old = new double[DV.fieldLength];

            for (int j = 0; j < DV.fieldLength; j++)
            {
                old[j] = DV.data.get(0).data[i][j];
            }
            orig[i] = DV.data.get(0).data[i][DV.fieldLength];

            DV.data.get(0).data[i] = old;
        }

        // remove old graphs
        GRAPHS.clear();

        // remove old graphs
        DV.graphPanel.removeAll();

        // holds classes to be graphed
        ArrayList<DataObject> upperObjects = new ArrayList<>(List.of(DV.data.get(0)));

        // calculate coordinates
        double upperScaler = getCoordinates(upperObjects);

        // get max point frequency
        double maxFrequency = 7;//getMaxFrequency();

        // get scaler
        double graphScaler = Math.max(upperScaler, 1);

        // add upper graph
        AddRegGraph upperGraph = new AddRegGraph(upperObjects, orig, 0, graphScaler, maxFrequency, div);
        upperGraph.execute();

        // wait for threads to finish
        try
        {
            // graph threads
            upperGraph.get();
        }
        catch (ExecutionException | InterruptedException e)
        {
            e.printStackTrace();
            return;
        }

        GridBagConstraints gpc = new GridBagConstraints();
        gpc.gridx = 0;
        gpc.gridy = 0;
        gpc.weightx = 1;
        gpc.weighty = 1;
        gpc.fill = GridBagConstraints.BOTH;

        CombinedDomainXYPlot plot = new CombinedDomainXYPlot();
        plot.setOrientation(PlotOrientation.VERTICAL);
        plot.setGap(0);
        plot.getDomainAxis().setVisible(false);
        plot.setOutlinePaint(null);
        plot.setOutlineVisible(false);
        plot.setInsets(RectangleInsets.ZERO_INSETS);
        plot.setDomainPannable(true);
        plot.setRangePannable(true);
        plot.setBackgroundPaint(DV.background);
        plot.setDomainGridlinePaint(Color.GRAY);
        plot.setRangeGridlinePaint(Color.GRAY);

        JFreeChart chart = new JFreeChart("", null, plot, false);

        // add graphs in order
        for (int i = 0; i < GRAPHS.size(); i++)
        {
            if (GRAPHS.containsKey(i))
            {
                plot.add((XYPlot) GRAPHS.get(i).getPlot(), 1);

                if (DV.displayRemoteGraphs)
                    DV.remoteGraphPanel.add(REMOTE_GRAPHS.get(i), gpc);
            }
        }

        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setMouseWheelEnabled(true);

        DV.graphPanel.add(chartPanel, gpc);

        // revalidate graphs and confusion matrices
        DV.graphPanel.repaint();
        DV.graphPanel.revalidate();;
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


    private static double getMaxFrequency()
    {
        int active = 0;

        for (int i = 0; i < DV.activeAttributes.size(); i++)
            if (DV.activeAttributes.get(i)) active++;

        double max = 0;

        if (DV.glc_or_dsc)
        {
            // get bar lengths
            for (int i = 0; i < DV.data.size(); i++)
            {
                int[] barRanges = new int[400];
                // translate endpoint to slider ticks
                // increment bar which endpoint lands
                for (int j = 0; j < DV.data.get(i).coordinates.length; j++)
                {
                    int tmpTick = (int) (Math.round((DV.data.get(i).coordinates[j][active-1][0] / DV.fieldLength * 200) + 200));
                    barRanges[tmpTick]++;

                    if (barRanges[tmpTick] > max)
                        max = barRanges[tmpTick];
                }
            }
        }

        return max;
    }


    public static void drawLDFRule(String ruleBase, String className, String opClassName, int curClass, int index)
    {
        labelPanel.removeAll();

        StringBuilder ldfInfo = new StringBuilder(ruleBase);

        for (int i = 0; i < DV.fieldLength; i++)
        {
            boolean used = false;

            if (!(DV.data.get(curClass).data[index][i] == 0 && DV.angles[i] > 90))
            {
                ldfInfo.append(String.format("%.2f", DV.data.get(curClass).data[index][i] * DV.scale[0][i])).append(" &le; ").append("x").append(i).append(" &le; ").append(String.format("%.2f", DV.data.get(curClass).data[index][i] * DV.scale[1][i]));
                used = true;
            }

            if (used && i != DV.fieldLength - 1) ldfInfo.append(", ");
        }

        if (DV.upperIsLower)
            ldfInfo.append("<br/>&emsp;&emsp;&emsp;&emsp;&emsp;then x belongs to class ").append(className).append("</html>");
        else
            ldfInfo.append("<br/>&emsp;&emsp;&emsp;&emsp;&emsp;then x belongs to class ").append(opClassName).append("</html>");

        JLabel rule = new JLabel(ldfInfo.toString());
        rule.setFont(rule.getFont().deriveFont(14f));

        labelPanel.add(rule);
        labelPanel.revalidate();
        labelPanel.repaint();
    }

    public static void drawLDF(int curClass, int index, int upper_or_lower, String ruleBase, String className, String opClassName)
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
            if ((DV.data.get(curClass).data[index][i] != 0 && DV.angles[i] <= 90) || DV.angles[i] > 90)
            {
                line1.add(DV.data.get(curClass).coordinates[index][i][0], DV.data.get(curClass).coordinates[index][i][1]);
                end1.add(DV.data.get(curClass).coordinates[index][i][0], DV.data.get(curClass).coordinates[index][i][1]);

                double[] xyPoint = DataObject.getXYPointGLC(DV.data.get(curClass).data[index][i], DV.angles[i]);

                x1 += xyPoint[0] * DV.scale[0][i];
                y1 += xyPoint[1] * DV.scale[0][i];

                line2.add(x1, y1);
                end2.add(x1, y1);

                x2 += xyPoint[0] * DV.scale[1][i];
                y2 += xyPoint[1] * DV.scale[1][i];

                line3.add(x2, y2);
                end3.add(x2, y2);

                if (i == DV.fieldLength - 1)
                {
                    time1.add(DV.data.get(curClass).coordinates[index][i][0], 0);
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

        JFreeChart chart = ChartFactory.createXYLineChart(
                "",
                "",
                "",
                originalLine,
                PlotOrientation.VERTICAL,
                true,
                true,
                false);

        // format chart
        chart.setBorderVisible(false);
        chart.setPadding(RectangleInsets.ZERO_INSETS);

        // get plot
        XYPlot plot = (XYPlot) chart.getPlot();

        // format plot
        plot.setDrawingSupplier(new DefaultDrawingSupplier(
                new Paint[] { DV.graphColors[upper_or_lower] },
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

        originalLineRenderer.setBaseStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        originalLineRenderer.setAutoPopulateSeriesStroke(false);
        plot.setRenderer(6, originalLineRenderer);
        plot.setDataset(6, originalLine);

        lowerWeightedLineRenderer.setBaseStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        lowerWeightedLineRenderer.setSeriesPaint(0, Color.RED);
        lowerWeightedLineRenderer.setAutoPopulateSeriesStroke(false);
        plot.setRenderer(7, lowerWeightedLineRenderer);
        plot.setDataset(7, lowerWeightedLine);

        upperWeightedLineRenderer.setBaseStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
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
            if ((DV.data.get(curClass).data[index][i] != 0 && DV.angles[i] <= 90) || DV.angles[i] > 90)
            {
                pcLine1.add(i - invalid,  DV.data.get(curClass).data[index][i]);
                pcEnd1.add(i - invalid,  DV.data.get(curClass).data[index][i]);

                double lowY = DV.data.get(curClass).data[index][i] * DV.scale[0][i];
                double upY = DV.data.get(curClass).data[index][i] * DV.scale[1][i];
                double lowLimitY = (sliders[i].getValue() / 100.0) * DV.data.get(curClass).data[index][i];
                double upLimitY = (sliders[i].getUpperValue() / 100.0) * DV.data.get(curClass).data[index][i];

                if (DV.discrete[i])
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
        pcLowerLimitRenderer.setBaseItemLabelGenerator(new StandardXYItemLabelGenerator()
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

                if (DV.discrete[xVal])
                    return String.format("%.0f", yVal);
                else
                    return String.format("%.2f", yVal);
            }
        });
        pcLowerLimitRenderer.setBaseItemLabelsVisible(true);
        pcPlot.setRenderer(0, pcLowerLimitRenderer);
        pcPlot.setDataset(0, pcLowerLimits);

        pcUpperLimitRenderer.setSeriesShape(0, new Rectangle2D.Double(-12.5, -5, 25, 10));
        pcUpperLimitRenderer.setSeriesPaint(0, Color.BLUE);
        pcUpperLimitRenderer.setBaseItemLabelGenerator(new StandardXYItemLabelGenerator()
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

                if (DV.discrete[xVal])
                    return String.format("%.0f", yVal);
                else
                    return String.format("%.2f", yVal);
            }
        });
        pcUpperLimitRenderer.setBaseItemLabelsVisible(true);
        pcPlot.setRenderer(1, pcUpperLimitRenderer);
        pcPlot.setDataset(1, pcUpperLimits);

        pcOriginalEndpointRenderer.setSeriesShape(0, new Rectangle2D.Double(-2.5, -2.5, 5, 5));
        pcPlot.setRenderer(2, pcOriginalEndpointRenderer);
        pcPlot.setDataset(2, pcOriginalEndpoint);

        pcLowerWeightedEndpointRenderer.setSeriesShape(0, new Rectangle2D.Double(-2.5, -2.5, 5, 5));
        pcLowerWeightedEndpointRenderer.setSeriesPaint(0, Color.RED);
        pcLowerWeightedEndpointRenderer.setBaseItemLabelGenerator(new StandardXYItemLabelGenerator()
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

                if (DV.discrete[xVal])
                    return String.format("%.0f", yVal);
                else
                    return String.format("%.2f", yVal);
            }
        });
        pcLowerWeightedEndpointRenderer.setBaseItemLabelsVisible(true);
        pcPlot.setRenderer(3, pcLowerWeightedEndpointRenderer);
        pcPlot.setDataset(3, pcLowerWeightedEndpoint);

        pcUpperWeightedEndpointRenderer.setSeriesShape(0, new Rectangle2D.Double(-2.5, -2.5, 5, 5));
        pcUpperWeightedEndpointRenderer.setSeriesPaint(0, Color.BLUE);
        pcUpperWeightedEndpointRenderer.setBaseItemLabelGenerator(new StandardXYItemLabelGenerator()
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

                if (DV.discrete[xVal])
                    return String.format("%.0f", yVal);
                else
                    return String.format("%.2f", yVal);
            }
        });
        pcUpperWeightedEndpointRenderer.setBaseItemLabelsVisible(true);
        pcPlot.setRenderer(4, pcUpperWeightedEndpointRenderer);
        pcPlot.setDataset(4, pcUpperWeightedEndpoint);

        pcOriginalLineRenderer.setBaseStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        pcOriginalLineRenderer.setAutoPopulateSeriesStroke(false);
        pcPlot.setRenderer(5, pcOriginalLineRenderer);
        pcPlot.setDataset(5, pcOriginalLine);

        pcLowerWeightedLineRenderer.setBaseStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        pcLowerWeightedLineRenderer.setSeriesPaint(0, Color.RED);
        pcLowerWeightedLineRenderer.setAutoPopulateSeriesStroke(false);
        pcPlot.setRenderer(6, pcLowerWeightedLineRenderer);
        pcPlot.setDataset(6, pcLowerWeightedLine);

        pcUpperWeightedLineRenderer.setBaseStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
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
                        if ((DV.data.get(curClass).data[index][i] != 0 && DV.angles[i] <= 90) || DV.angles[i] > 90)
                        {
                            // update slider label
                            sliderLabels[i].setText(limitSliderLabel(curClass, index, i));

                            // update limit values
                            DV.limits[i][0] = (sliders[i].getValue() / 100.0) * DV.data.get(curClass).data[index][i];
                            DV.limits[i][1] = (sliders[i].getUpperValue() / 100.0) * DV.data.get(curClass).data[index][i];

                            // update limits on graph
                            lowLim.add(i - invalid, DV.limits[i][0]);
                            upLim.add(i - invalid, DV.limits[i][1]);

                            boolean changed = false;

                            if (DV.data.get(curClass).data[index][i] * DV.scale[0][i] < DV.limits[i][0])
                            {
                                DV.scale[0][i] = DV.limits[i][0] / DV.data.get(curClass).data[index][i];
                                weightSliders[i][0].setValue((int)(DV.scale[0][i] * 100));

                                // redraw graphs
                                changed = true;
                            }

                            if (DV.data.get(curClass).data[index][i] * DV.scale[1][i] < DV.limits[i][0])
                            {
                                DV.scale[1][i] = DV.limits[i][0] / DV.data.get(curClass).data[index][i];
                                weightSliders[i][1].setValue((int)(DV.scale[1][i] * 100));

                                // redraw graphs
                                changed = true;
                            }

                            if (DV.data.get(curClass).data[index][i] * DV.scale[0][i] > DV.limits[i][1])
                            {
                                DV.scale[0][i] = DV.limits[i][1] / DV.data.get(curClass).data[index][i];
                                weightSliders[i][0].setValue((int)(DV.scale[0][i] * 100));

                                // redraw graphs
                                changed = true;
                            }

                            if (DV.data.get(curClass).data[index][i] * DV.scale[1][i] > DV.limits[i][1])
                            {
                                DV.scale[1][i] = DV.limits[i][1] / DV.data.get(curClass).data[index][i];
                                weightSliders[i][1].setValue((int)(DV.scale[1][i] * 100));

                                // redraw graphs
                                changed = true;
                            }

                            if (changed)
                            {
                                drawLDFRule(ruleBase, className, opClassName, curClass, index);
                                drawLDF(curClass, index, upper_or_lower, ruleBase, className, opClassName);
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

    private static String limitSliderLabel(int curClass, int index, int attribute)
    {
        double upperVal = (sliders[attribute].getUpperValue() / 100.0) * DV.data.get(curClass).data[index][attribute];
        double lowerVal = (sliders[attribute].getValue() / 100.0) * DV.data.get(curClass).data[index][attribute];

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
        if (DV.discrete[attribute])
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
            sliders[attribute].setUpperValue((int)(upperVal / DV.data.get(curClass).data[index][attribute] * 100.0));
            sliders[attribute].setValue((int)(lowerVal / DV.data.get(curClass).data[index][attribute] * 100.0));
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
     * Draw graph with specified parameters
     */
    private static class AddGraph extends SwingWorker<Boolean, Void>
    {
        final ArrayList<DataObject> DATA_OBJECTS;
        final int UPPER_OR_LOWER;
        final double GRAPH_SCALER;
        final double MAX_FREQUENCY;

        /**
         * Initializes parameters
         * @param dataObjects classes to draw
         * @param upperOrLower draw up when upper (0) and down when lower (1)
         * @param graphScaler how much to zoom out the graphs
         */
        AddGraph(ArrayList<DataObject> dataObjects, int upperOrLower, double graphScaler, double maxFrequency)
        {
            this.DATA_OBJECTS = dataObjects;
            this.UPPER_OR_LOWER = upperOrLower;
            this.GRAPH_SCALER = graphScaler;
            this.MAX_FREQUENCY = maxFrequency;
        }

        @Override
        protected Boolean doInBackground()
        {
            // create main renderer and dataset
            XYLineAndShapeRenderer lineRenderer = new XYLineAndShapeRenderer(true, false);
            XYSeriesCollection graphLines = new XYSeriesCollection();

            // create SVM renderer and dataset
            XYLineAndShapeRenderer svmLineRenderer = new XYLineAndShapeRenderer(true, false);
            XYSeriesCollection svmSeriesCol = new XYSeriesCollection();

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
            XYSeries svmEndpointSeries = new XYSeries(0, false, true);
            XYSeries midpointSeries = new XYSeries(0, false, true);
            XYSeries svmMidpointSeries = new XYSeries(0, false, true);
            XYSeries timeLineSeries = new XYSeries(0, false, true);
            XYSeries svmTimeLineSeries = new XYSeries(0, false, true);

            double buffer = DV.fieldLength / 10.0;

            // populate svm series
            if (DV.drawOnlySVM || DV.drawSVM)
            {
                // update coordinates
                getCoordinates(new ArrayList<>(List.of(DV.supportVectors)));

                for (int i = 0, lineCnt = 0; i < DV.supportVectors.data.length; i++, lineCnt++)
                {
                    int upOrDown = UPPER_OR_LOWER == 1 ? -1 : 1;

                    // start line at (0, 0)
                    XYSeries line = new XYSeries(lineCnt, false, true);

                    if (DV.showFirstSeg)
                        line.add(0, upOrDown * buffer);

                    double endpoint = DV.supportVectors.coordinates[i][DV.supportVectors.coordinates[i].length-1][0];

                    // ensure datapoint is within domain
                    if (!DV.domainActive || (endpoint >= DV.domainArea[0] && endpoint <= DV.domainArea[1]))
                    {
                        for (int j = 0; j < DV.supportVectors.coordinates[i].length; j++)
                        {
                            line.add(DV.supportVectors.coordinates[i][j][0], upOrDown * (DV.supportVectors.coordinates[i][j][1] + buffer));

                            if (j > 0 && j < DV.supportVectors.coordinates[i].length - 1 && DV.angles[j] == DV.angles[j + 1])
                                svmMidpointSeries.add(DV.supportVectors.coordinates[i][j][0], upOrDown * (DV.supportVectors.coordinates[i][j][1]+ buffer));

                            // add endpoint and timeline
                            if (j == DV.supportVectors.coordinates[i].length - 1)
                            {
                                svmEndpointSeries.add(DV.supportVectors.coordinates[i][j][0], upOrDown * (DV.supportVectors.coordinates[i][j][1] + buffer));
                                svmTimeLineSeries.add(DV.supportVectors.coordinates[i][j][0], 0);
                            }
                        }

                        svmSeriesCol.addSeries(line);
                        svmLineRenderer.setSeriesPaint(lineCnt, DV.svmLines);
                    }
                }
            }

            if (!DV.drawOnlySVM)
            {
                // populate main series
                int lineCnt = -1;

                for (DataObject data : DATA_OBJECTS)
                {
                    for (int i = 0; i < data.data.length; i++)
                    {
                        double endpoint = data.coordinates[i][data.coordinates[i].length - 1][0];

                        // ensure datapoint is within domain
                        // if drawing overlap, ensure datapoint is within overlap
                        if ((!DV.domainActive || endpoint >= DV.domainArea[0] && endpoint <= DV.domainArea[1]) &&
                                (!DV.drawOverlap || (DV.overlapArea[0] <= endpoint && endpoint <= DV.overlapArea[1])))
                        {
                            int upOrDown = UPPER_OR_LOWER == 1 ? -1 : 1;

                            // start line at (0, 0)
                            XYSeries line = new XYSeries(++lineCnt, false, true);
                            XYSeries endpointSeries = new XYSeries(lineCnt, false, true);

                            if (DV.showFirstSeg)
                                line.add(0, upOrDown * buffer);

                            // add points to lines
                            for (int j = 0; j < data.coordinates[i].length; j++)
                            {
                                line.add(data.coordinates[i][j][0], upOrDown * (data.coordinates[i][j][1] + buffer));

                                if (j > 0 && j < data.coordinates[i].length - 1 && DV.angles[j] == DV.angles[j + 1])
                                    midpointSeries.add(data.coordinates[i][j][0], upOrDown * (data.coordinates[i][j][1] + buffer));

                                // add endpoint and timeline
                                if (j == data.coordinates[i].length - 1)
                                {
                                    endpointSeries.add(data.coordinates[i][j][0], upOrDown * (data.coordinates[i][j][1] + buffer));
                                    timeLineSeries.add(data.coordinates[i][j][0], 0);

                                    graphLines.addSeries(line);
                                    endpoints.addSeries(endpointSeries);

                                    if (DV.highlights[UPPER_OR_LOWER][i])
                                        lineRenderer.setSeriesPaint(lineCnt, Color.ORANGE);
                                    else
                                        lineRenderer.setSeriesPaint(lineCnt, DV.graphColors[UPPER_OR_LOWER]);

                                    if (UPPER_OR_LOWER == 0 && DV.upperIsLower)
                                    {
                                        // check if endpoint is correctly classified
                                        if (endpoint < DV.threshold)
                                            endpointRenderer.setSeriesPaint(lineCnt, DV.endpoints);
                                        else
                                            endpointRenderer.setSeriesPaint(lineCnt, Color.RED);//DV.graphColors[1])
                                    }
                                    else if (UPPER_OR_LOWER == 0)
                                    {
                                        // check if endpoint is correctly classified
                                        if (endpoint > DV.threshold)
                                            endpointRenderer.setSeriesPaint(lineCnt, DV.endpoints);
                                        else
                                            endpointRenderer.setSeriesPaint(lineCnt, Color.RED);//DV.graphColors[1])
                                    }
                                    else if(UPPER_OR_LOWER == 1 && DV.upperIsLower)
                                    {
                                        // check if endpoint is correctly classified
                                        if (endpoint > DV.threshold)
                                            endpointRenderer.setSeriesPaint(lineCnt, DV.endpoints);
                                        else
                                            endpointRenderer.setSeriesPaint(lineCnt, Color.RED);//DV.graphColors[1]);
                                    }
                                    else
                                    {
                                        // check if endpoint is correctly classified
                                        if (endpoint < DV.threshold)
                                            endpointRenderer.setSeriesPaint(lineCnt, DV.endpoints);
                                        else
                                            endpointRenderer.setSeriesPaint(lineCnt, Color.RED);//DV.graphColors[1])
                                    }

                                    endpointRenderer.setSeriesShape(lineCnt, new Ellipse2D.Double(-1, -1, 2, 2));
                                }
                            }
                        }
                    }
                }

                ArrayList<double[]> endPnts = new ArrayList<>();
                ArrayList<Integer> endCnt = new ArrayList<>();
                double errorDist = 0.01;

                for (int i = 0; i < lineCnt; i++)
                {
                    boolean unique = true;

                    for (int j = 0; j < endPnts.size(); j++)
                    {
                        if (Math.abs(endPnts.get(j)[0] - endpoints.getSeries(i).getMaxX()) <= errorDist && Math.abs(endPnts.get(j)[1] - endpoints.getSeries(i).getMaxY()) <= errorDist)
                        {
                            endCnt.set(j, endCnt.get(j) + 1);
                            unique = false;
                            break;
                        }
                    }

                    if (unique)
                    {
                        endPnts.add(new double[]{endpoints.getSeries(i).getMaxX(), endpoints.getSeries(i).getMaxY()});
                        endCnt.add(1);
                    }
                }

                LinkedHashSet<Integer> endCntUtmp = new LinkedHashSet<>(endCnt);
                ArrayList<Integer> endCntU = new ArrayList<>(endCntUtmp);
                Collections.sort(endCntU);

                int fR = 0;
                int fB = 255;

                int tR = 255;
                int tB = 0;

                ArrayList<Color> heats = new ArrayList<>();

                int range = endCntU.size()-1; // Makes things a bit easier
                for (int i= 0; i < endCntU.size(); i++)
                {
                    // i is the proportion of the "to" colour to use.
                    // j is the proportion of the "from" colour to use.

                    /*int j = range - i;
                    int r = ((fR * j) + (tR * i)) / range;
                    int b = ((fB * j) + (tB * i)) / range;
                    heats.add(new Color(r, 0, b));*/
                }

                for (int i = 0; i < lineCnt; i++)
                {
                    double[] tmpPnt = new double[]{endpoints.getSeries(i).getMaxX(), endpoints.getSeries(i).getMaxY()};
                    int tmpPntCnt = -1;
                    int heat = -1;

                    for (int j = 0; j < endPnts.size(); j++)
                    {
                        if (Math.abs(endPnts.get(j)[0] - tmpPnt[0]) <= errorDist && Math.abs(endPnts.get(j)[1] - tmpPnt[1]) <= errorDist)
                        {
                            tmpPntCnt = endCnt.get(j);
                            break;
                        }
                    }

                    for (int j = 0; j < endCntU.size(); j++)
                    {
                        if (tmpPntCnt == endCntU.get(j))
                        {
                            heat = j;
                        }
                    }

                    //endpointRenderer.setSeriesPaint(i, heats.get(heat));
                }
            }

            // add data to series
            //endpoints.addSeries(endpointSeries);
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
            plot.setSeriesRenderingOrder(SeriesRenderingOrder.REVERSE);
            //System.out.println(plot.getSeriesRenderingOrder());

            // set domain and range of graph
            double bound = GRAPH_SCALER * DV.fieldLength;

            // set domain
            ValueAxis domainView = plot.getDomainAxis();
            domainView.setRange(-bound, bound);
            NumberAxis xAxis = (NumberAxis) plot.getDomainAxis();
            xAxis.setTickUnit(new NumberTickUnit(buffer));

            // set range
            ValueAxis rangeView = plot.getRangeAxis();
            NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();
            yAxis.setTickUnit(new NumberTickUnit(buffer));

            // set range up or down
            if (UPPER_OR_LOWER == 1)
                rangeView.setRange(-bound * (DV.mainPanel.getHeight() * 0.7) / (DV.graphPanel.getWidth() * 0.8), 0);
            else
                rangeView.setRange(0, bound * (DV.mainPanel.getHeight() * 0.7) / (DV.graphPanel.getWidth() * 0.8));

            // renderer for bars
            XYIntervalSeriesCollection bars = new XYIntervalSeriesCollection();
            XYBarRenderer barRenderer = new XYBarRenderer();

            // create bar chart
            if (DV.showBars)
            {
                int[] barRanges = new int[400];

                // get bar lengths
                for (DataObject dataObj : DATA_OBJECTS)
                {
                    // translate endpoint to slider ticks
                    // increment bar which endpoint lands
                    for (int i = 0; i < dataObj.data.length; i++)
                    {
                        int tmpTick = (int) (Math.round((dataObj.coordinates[i][DV.fieldLength-1][0] / DV.fieldLength * 200) + 200));
                        barRanges[tmpTick]++;
                    }
                }

                // get interval and bounds
                double interval = DV.fieldLength / 200.0;
                double maxBound = -DV.fieldLength;
                double minBound = -DV.fieldLength;

                // add series to collection
                for (int i = 0; i < 400; i++)
                {
                    // get max bound
                    maxBound += interval;

                    XYIntervalSeries bar = new XYIntervalSeries(i, false, true);

                    // bar width = interval
                    // bar height = (total endpoints on bar) / (total endpoints)
                    // buffer = maximum bar height
                    if (UPPER_OR_LOWER == 1)
                        bar.add(interval, minBound, maxBound, (-barRanges[i] / MAX_FREQUENCY) * buffer, -buffer, 0);
                    else
                        bar.add(interval, minBound, maxBound, (barRanges[i] / MAX_FREQUENCY) * buffer, 0, buffer);

                    bars.addSeries(bar);
                    barRenderer.setSeriesPaint(i, DV.graphColors[UPPER_OR_LOWER]);

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

            /*System.out.println("THIS IS CLASS " + UPPER_OR_LOWER + ": Endpoints -> " + endpoints.getSeriesCount());
            System.out.println("THIS IS CLASS " + UPPER_OR_LOWER + ": Midpoints -> " + midpoints.getSeriesCount());
            System.out.println("THIS IS CLASS " + UPPER_OR_LOWER + ": Timeline -> " + timeLine.getSeriesCount());
            System.out.println("THIS IS CLASS " + UPPER_OR_LOWER + ": Lines -> " + graphLines.getSeriesCount());*/

            // set endpoint renderer and dataset
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
                barRenderer.setSeriesPaint(0, DV.graphColors[UPPER_OR_LOWER]);
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

                timeLineRenderer.setSeriesPaint(0, DV.graphColors[UPPER_OR_LOWER]);
                plot.setRenderer(10, timeLineRenderer);
                plot.setDataset(10, timeLine);
            }

            // set chart size
            int vertical_res = 1;

            if (DV.classNumber == 1)
                vertical_res *= 2;

            // add domain listeners
            DV.domainSlider.addMouseMotionListener(new MouseMotionListener()
            {
                @Override
                public void mouseDragged(MouseEvent e)
                {
                    RangeSlider slider = (RangeSlider) e.getSource();
                    DV.domainArea[0] = (slider.getValue() - 200) * DV.fieldLength / 200.0;
                    DV.domainArea[1] = (slider.getUpperValue() - 200) * DV.fieldLength / 200.0;

                    // clear old domain lines
                    domainMinLine.clear();
                    domainMaxLine.clear();

                    // clear old lines, midpoints, endpoints, and timeline points
                    graphLines.removeAllSeries();
                    midpointSeries.clear();
                    endpoints.removeAllSeries();
                    timeLineSeries.clear();

                    // turn notify off
                    chart.setNotify(false);

                    // set overlap line
                    domainMinLine.add(DV.domainArea[0], 0);
                    domainMinLine.add(DV.domainArea[0], lineHeight);
                    domainMaxLine.add(DV.domainArea[1], 0);
                    domainMaxLine.add(DV.domainArea[1], lineHeight);

                    // number of lines
                    int lineCnt = -1;

                    for (DataObject data : DATA_OBJECTS)
                    {
                        for (int i = 0; i < data.data.length; i++)
                        {
                            double endpoint = data.coordinates[i][data.coordinates[i].length - 1][0];

                            // ensure datapoint is within domain
                            // if drawing overlap, ensure datapoint is within overlap
                            if ((!DV.domainActive || endpoint >= DV.domainArea[0] && endpoint <= DV.domainArea[1]) &&
                                    (!DV.drawOverlap || (DV.overlapArea[0] <= endpoint && endpoint <= DV.overlapArea[1])))
                            {
                                int upOrDown = UPPER_OR_LOWER == 1 ? -1 : 1;

                                // start line at (0, 0)
                                XYSeries line = new XYSeries(++lineCnt, false, true);
                                XYSeries endpointSeries = new XYSeries(lineCnt, false, true);

                                if (DV.showFirstSeg)
                                    line.add(0, upOrDown * buffer);

                                // add points to lines
                                for (int j = 0; j < data.coordinates[i].length; j++)
                                {
                                    line.add(data.coordinates[i][j][0], upOrDown * (data.coordinates[i][j][1] + buffer));

                                    if (j > 0 && j < data.coordinates[i].length - 1 && DV.angles[j] == DV.angles[j + 1])
                                        midpointSeries.add(data.coordinates[i][j][0], upOrDown * (data.coordinates[i][j][1] + buffer));

                                    // add endpoint and timeline
                                    if (j == data.coordinates[i].length - 1)
                                    {
                                        endpointSeries.add(data.coordinates[i][j][0], upOrDown * (data.coordinates[i][j][1] + buffer));
                                        timeLineSeries.add(data.coordinates[i][j][0], 0);

                                        graphLines.addSeries(line);
                                        endpoints.addSeries(endpointSeries);

                                        if (DV.highlights[UPPER_OR_LOWER][i])
                                            lineRenderer.setSeriesPaint(lineCnt, Color.ORANGE);
                                        else
                                            lineRenderer.setSeriesPaint(lineCnt, DV.graphColors[UPPER_OR_LOWER]);

                                        if (UPPER_OR_LOWER == 0 && DV.upperIsLower)
                                        {
                                            // check if endpoint is correctly classified
                                            if (endpoint < DV.threshold)
                                                endpointRenderer.setSeriesPaint(i, DV.endpoints);
                                            else
                                                endpointRenderer.setSeriesPaint(i, Color.RED);//DV.graphColors[1])
                                        }
                                        else if (UPPER_OR_LOWER == 0)
                                        {
                                            // check if endpoint is correctly classified
                                            if (endpoint > DV.threshold)
                                                endpointRenderer.setSeriesPaint(i, DV.endpoints);
                                            else
                                                endpointRenderer.setSeriesPaint(i, Color.RED);//DV.graphColors[1])
                                        }
                                        else if(UPPER_OR_LOWER == 1 && DV.upperIsLower)
                                        {
                                            // check if endpoint is correctly classified
                                            if (endpoint > DV.threshold)
                                                endpointRenderer.setSeriesPaint(i, DV.endpoints);
                                            else
                                                endpointRenderer.setSeriesPaint(i, Color.RED);//DV.graphColors[1]);
                                        }
                                        else
                                        {
                                            // check if endpoint is correctly classified
                                            if (endpoint < DV.threshold)
                                                endpointRenderer.setSeriesPaint(i, DV.endpoints);
                                            else
                                                endpointRenderer.setSeriesPaint(i, Color.RED);//DV.graphColors[1])
                                        }

                                        endpointRenderer.setSeriesShape(i, new Ellipse2D.Double(-1, -1, 2, 2));
                                    }
                                }
                            }
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
                GRAPHS.put(UPPER_OR_LOWER, chart);
            }

           /* if (DV.displayRemoteGraphs)
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
*/
            return true;
        }
    }

    private static class AddRegGraph extends SwingWorker<Boolean, Void>
    {
        final ArrayList<DataObject> DATA_OBJECTS;
        final int UPPER_OR_LOWER;
        final double GRAPH_SCALER;
        final double MAX_FREQUENCY;
        final double[] PREDS;
        final double INTERCEPT;

        /**
         * Initializes parameters
         * @param dataObjects classes to draw
         * @param upperOrLower draw up when upper (0) and down when lower (1)
         * @param graphScaler how much to zoom out the graphs
         */
        AddRegGraph(ArrayList<DataObject> dataObjects, double[] preds, int upperOrLower, double graphScaler, double maxFrequency, double intercept)
        {
            this.DATA_OBJECTS = dataObjects;
            this.UPPER_OR_LOWER = upperOrLower;
            this.GRAPH_SCALER = graphScaler;
            this.MAX_FREQUENCY = maxFrequency;
            this.PREDS = preds;
            this.INTERCEPT = intercept;
        }
        @Override
        protected Boolean doInBackground()
        {
            // create main renderer and dataset
            XYLineAndShapeRenderer lineRenderer = new XYLineAndShapeRenderer(true, false);
            XYSeriesCollection graphLines = new XYSeriesCollection();

            // renderer for endpoint, midpoint, and timeline
            XYLineAndShapeRenderer endpointRenderer = new XYLineAndShapeRenderer(false, true);
            XYLineAndShapeRenderer midpointRenderer = new XYLineAndShapeRenderer(false, true);
            XYLineAndShapeRenderer timeLineRenderer = new XYLineAndShapeRenderer(false, true);
            XYSeriesCollection endpoints = new XYSeriesCollection();
            XYSeriesCollection midpoints = new XYSeriesCollection();
            XYSeriesCollection timeLine = new XYSeriesCollection();
            XYSeries midpointSeries = new XYSeries(0, false, true);
            XYSeries timeLineSeries = new XYSeries(0, false, true);

            XYLineAndShapeRenderer predRenderer = new XYLineAndShapeRenderer(false, true);
            XYSeriesCollection preds = new XYSeriesCollection();
            XYSeries predSeries = new XYSeries(0, false, true);

            for (int i = 0; i < PREDS.length; i++)
            {
                //predSeries.add(DV.data.get(0).coordinates[i][1][0], 0);
            }

            XYLineAndShapeRenderer realRenderer = new XYLineAndShapeRenderer(false, true);
            XYSeriesCollection reals = new XYSeriesCollection();
            XYSeries realSeries = new XYSeries(0, false, true);

            for (int i = 0; i < DV.data.get(0).data.length; i++)
            {
                //realSeries.add(DV.data.get(0).coordinates[i][1][0], -Math.abs(DV.data.get(0).coordinates[i][1][0] - PREDS[i]));
            }

            double tmpBound = GRAPH_SCALER * DV.fieldLength;
            double fullBound = tmpBound * 2;
            double spacing = fullBound / PREDS.length;
            tmpBound = -tmpBound;

            double buffer = DV.fieldLength / 10.0;

            // populate main series
            int lineCnt = -1;

            for (DataObject data : DATA_OBJECTS)
            {
                for (int i = 0; i < data.data.length; i++)
                {
                    double endpoint = data.coordinates[i][data.coordinates[i].length - 1][0];

                    // ensure datapoint is within domain
                    // if drawing overlap, ensure datapoint is within overlap
                    if (true)
                    {
                        int upOrDown = UPPER_OR_LOWER == 1 ? -1 : 1;

                        // start line at (0, 0)
                        XYSeries line = new XYSeries(++lineCnt, false, true);
                        XYSeries endpointSeries = new XYSeries(lineCnt, false, true);

                        if (DV.showFirstSeg)
                            line.add(0, upOrDown * buffer);

                        // add points to lines
                        for (int j = 0; j < data.coordinates[i].length - 1; j++)
                        {
                            line.add(data.coordinates[i][j][0], upOrDown * (data.coordinates[i][j][1] + buffer));

                            // add endpoint and timeline
                            if (j == data.coordinates[i].length - 2)
                            {
                                endpointSeries.add(data.coordinates[i][j][0], upOrDown * (data.coordinates[i][j][1] + buffer));
                                //timeLineSeries.add(data.coordinates[i][j][0], 0);
                                timeLineSeries.add((PREDS[i] / INTERCEPT), 0);
                                realSeries.add((PREDS[i] / INTERCEPT), 0 - Math.abs((PREDS[i] / INTERCEPT) - data.coordinates[i][j][0]));
                                System.out.println("Original: " + PREDS[i]);
                                System.out.println("Altered: " + (PREDS[i] / INTERCEPT));
                                System.out.println("Prediction: " + data.coordinates[i][j][0]);
                                System.out.println("Difference: " + Math.abs((PREDS[i] / INTERCEPT) - data.coordinates[i][j][0]) + "\n");
                                tmpBound += spacing;

                                graphLines.addSeries(line);
                                endpoints.addSeries(endpointSeries);

                                if (DV.highlights[UPPER_OR_LOWER][i])
                                    lineRenderer.setSeriesPaint(lineCnt, Color.ORANGE);
                                else
                                    lineRenderer.setSeriesPaint(lineCnt, DV.graphColors[UPPER_OR_LOWER]);

                                // check if endpoint is correctly classified
                                endpointRenderer.setSeriesPaint(lineCnt, Color.BLACK);//DV.graphColors[1])
                                endpointRenderer.setSeriesShape(lineCnt, new Ellipse2D.Double(-1, -1, 2, 2));
                            }
                        }
                    }
                }
            }

            // add data to series
            //endpoints.addSeries(endpointSeries);
            midpoints.addSeries(midpointSeries);
            timeLine.addSeries(timeLineSeries);
            preds.addSeries(predSeries);
            reals.addSeries(realSeries);

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
            plot.setSeriesRenderingOrder(SeriesRenderingOrder.REVERSE);
            //System.out.println(plot.getSeriesRenderingOrder());

            // set domain and range of graph
            double bound = GRAPH_SCALER * DV.fieldLength;

            // set domain
            ValueAxis domainView = plot.getDomainAxis();
            domainView.setRange(-bound, bound);
            NumberAxis xAxis = (NumberAxis) plot.getDomainAxis();
            xAxis.setTickUnit(new NumberTickUnit(buffer));

            // set range
            ValueAxis rangeView = plot.getRangeAxis();
            NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();
            yAxis.setTickUnit(new NumberTickUnit(buffer));

            // set range up or down
            if (UPPER_OR_LOWER == 1)
                rangeView.setRange(-bound * (DV.mainPanel.getHeight() * 0.7) / (DV.graphPanel.getWidth() * 0.8), 0);
            else
                rangeView.setRange(0, bound * (DV.mainPanel.getHeight() * 0.7) / (DV.graphPanel.getWidth() * 0.8));

            // renderer for bars
            XYIntervalSeriesCollection bars = new XYIntervalSeriesCollection();
            XYBarRenderer barRenderer = new XYBarRenderer();

            // create bar chart
            if (DV.showBars)
            {
                int[] barRanges = new int[400];

                // get bar lengths
                for (DataObject dataObj : DATA_OBJECTS)
                {
                    // translate endpoint to slider ticks
                    // increment bar which endpoint lands
                    for (int i = 0; i < dataObj.data.length; i++)
                    {
                        int tmpTick = (int) (Math.round((dataObj.coordinates[i][DV.fieldLength-1][0] / DV.fieldLength * 200) + 200));
                        barRanges[tmpTick]++;
                    }
                }

                // get interval and bounds
                double interval = DV.fieldLength / 200.0;
                double maxBound = -DV.fieldLength;
                double minBound = -DV.fieldLength;

                // add series to collection
                for (int i = 0; i < 400; i++)
                {
                    // get max bound
                    maxBound += interval;

                    XYIntervalSeries bar = new XYIntervalSeries(i, false, true);

                    // bar width = interval
                    // bar height = (total endpoints on bar) / (total endpoints)
                    // buffer = maximum bar height
                    if (UPPER_OR_LOWER == 1)
                        bar.add(interval, minBound, maxBound, (-barRanges[i] / MAX_FREQUENCY) * buffer, -buffer, 0);
                    else
                        bar.add(interval, minBound, maxBound, (barRanges[i] / MAX_FREQUENCY) * buffer, 0, buffer);

                    bars.addSeries(bar);
                    barRenderer.setSeriesPaint(i, DV.graphColors[UPPER_OR_LOWER]);

                    // set min bound to old max
                    minBound = maxBound;
                }
            }

            // set endpoint renderer and dataset
            plot.setRenderer(0, endpointRenderer);
            plot.setDataset(0, endpoints);


            // set midpoint renderer and dataset
            midpointRenderer.setSeriesShape(0, new Ellipse2D.Double(-0.5, -0.5, 1, 1));
            midpointRenderer.setSeriesPaint(0, DV.endpoints);
            plot.setRenderer(1, midpointRenderer);
            plot.setDataset(1, midpoints);
            // set line renderer and dataset
            lineRenderer.setBaseStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
            lineRenderer.setAutoPopulateSeriesStroke(false);
            plot.setRenderer(2, lineRenderer);
            plot.setDataset(2, graphLines);

            // set bar or timeline renderer and dataset
            if (DV.showBars)
            {
                barRenderer.setSeriesPaint(0, DV.graphColors[UPPER_OR_LOWER]);
                barRenderer.setShadowVisible(false);
                plot.setRenderer(3, barRenderer);
                plot.setDataset(3, bars);
            }
            else
            {
                if (UPPER_OR_LOWER == 1)
                    timeLineRenderer.setSeriesShape(0, new Rectangle2D.Double(-0.25, 0, 0.5, 3));
                else
                    timeLineRenderer.setSeriesShape(0, new Rectangle2D.Double(-0.25, -3, 0.5, 3));

                timeLineRenderer.setSeriesPaint(0, DV.graphColors[UPPER_OR_LOWER]);
                plot.setRenderer(3, timeLineRenderer);
                plot.setDataset(3, timeLine);
            }

            // set midpoint renderer and dataset
            predRenderer.setSeriesShape(0, new Rectangle2D.Double(-0.25, 0, 0.5, 3));
            predRenderer.setSeriesPaint(0, Color.BLUE);
            plot.setRenderer(4, predRenderer);
            plot.setDataset(4, preds);

            // set midpoint renderer and dataset
            realRenderer.setSeriesShape(0, new Rectangle2D.Double(-0.25, 0, 0.5, 3));
            realRenderer.setSeriesPaint(0, Color.GREEN);
            plot.setRenderer(5, realRenderer);
            plot.setDataset(5, reals);

            // add graph to graph panel
            synchronized (GRAPHS)
            {
                GRAPHS.put(UPPER_OR_LOWER, chart);
            }
            return true;
        }
    }
}
