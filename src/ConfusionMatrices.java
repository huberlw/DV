import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class ConfusionMatrices
{
    // holds the percentage of overlap points used
    static String percentageOverlapPointsUsed;

    // holds overlapping datapoints
    static ArrayList<double[]> upper;
    static ArrayList<double[]> lower;

    // holds angles and threshold for LDA
    static ArrayList<Double> LDAFunction = new ArrayList<>();

    // holds current classes
    static ArrayList<String> curClasses;


    /**
     * Generates all data, data without overlap,
     * overlap, and worst case confusion matrices
     */
    public static void generateConfusionMatrices()
    {
        // get current classes being visualized
        getCurClasses();

        // remove old confusion matrices
        DV.confusionMatrixPanel.removeAll();

        // add confusion matrices for previous splits
        addOldConfusionMatrices();

        // create new confusion matrices
        getAllDataConfusionMatrix();
        getDataWithoutOverlapConfusionMatrix();
        getOverlapConfusionMatrix();
        getWorstCaseConfusionMatrix();
        getUserValidationConfusionMatrix();

        // run k-fold cross validation
        if (DV.crossValidationChecked)
        {
            DV.crossValidationPanel.removeAll();
            runKFoldCrossValidation();
        }
    }


    /**
     * Gets current classes being used
     */
    private static void getCurClasses()
    {
        curClasses = new ArrayList<>();

        // add upper class
        curClasses.add(String.format("%d", DV.upperClass));

        StringBuilder lowerClasses = new StringBuilder();

        // add lower classes
        for (int i = 0; i < DV.classNumber; i++)
        {
            if (DV.lowerClasses.get(i))
            {
                if (!lowerClasses.isEmpty())
                    lowerClasses.append(",");

                lowerClasses.append(i);
            }
        }

        curClasses.add(lowerClasses.toString());
    }


    /**
     * Regenerates old confusion matrices from previous data splits
     */
    private static void addOldConfusionMatrices()
    {
        if (DV.prevAllDataChecked)
        {
            // set all previous confusion matrices
            for (int i = 0; i < DV.prevCM.size(); i++)
            {
                JTextArea confusionMatrix = new JTextArea(DV.prevCM.get(i));
                confusionMatrix.setFont(confusionMatrix.getFont().deriveFont(Font.BOLD, 12f));
                confusionMatrix.setEditable(false);
                DV.confusionMatrixPanel.add(confusionMatrix);
            }
        }
    }


    /**
     * Generates confusion matrix with all data
     * Confusion matrix uses its own function
     */
    private static void getAllDataConfusionMatrix()
    {
        if (DV.allDataChecked)
        {
            int totalPoints = 0;
            int totalPointsUsed = 0;
            int correctPoints = 0;

            // get point distribution
            int[][] pntDist = new int[2][2];

            for (int i = 0; i < DV.data.size(); i++)
            {
                totalPoints += DV.data.get(i).data.length;

                if (i == DV.upperClass || DV.lowerClasses.get(i))
                {
                    for (int j = 0; j < DV.data.get(i).coordinates.length; j++)
                    {
                        double endpoint = DV.data.get(i).coordinates[j][DV.fieldLength-1][0];

                        // check if endpoint is within the subset of used data
                        if ((DV.domainArea[0] <= endpoint && endpoint <= DV.domainArea[1]) || !DV.domainActive)
                        {
                            // get classification
                            if (i == DV.upperClass && DV.upperIsLower)
                            {
                                // check if endpoint is correctly classified
                                if (endpoint < DV.threshold)
                                {
                                    pntDist[0][0]++;
                                    totalPointsUsed++;
                                    correctPoints++;
                                }
                                else
                                {
                                    totalPointsUsed++;
                                    pntDist[0][1]++;
                                }
                            }
                            else if (i == DV.upperClass)
                            {
                                // check if endpoint is correctly classified
                                if (endpoint > DV.threshold)
                                {
                                    pntDist[0][0]++;
                                    totalPointsUsed++;
                                    correctPoints++;
                                }
                                else
                                {
                                    totalPointsUsed++;
                                    pntDist[0][1]++;
                                }
                            }
                            else if(DV.lowerClasses.get(i) && DV.upperIsLower)
                            {
                                // check if endpoint is correctly classified
                                if (endpoint > DV.threshold)
                                {
                                    pntDist[1][1]++;
                                    totalPointsUsed++;
                                    correctPoints++;
                                }
                                else
                                {
                                    totalPointsUsed++;
                                    pntDist[1][0]++;
                                }
                            }
                            else
                            {
                                // check if endpoint is correctly classified
                                if (endpoint < DV.threshold)
                                {
                                    pntDist[1][1]++;
                                    totalPointsUsed++;
                                    correctPoints++;
                                }
                                else
                                {
                                    totalPointsUsed++;
                                    pntDist[1][0]++;
                                }
                            }
                        }
                    }
                }
            }

            // create confusion matrix
            StringBuilder cm = new StringBuilder("All Data Analytics\nReal\tPredictions\nClass\t");

            // append predicted classes
            for (int i = 0; i < 2; i++)
                cm.append(curClasses.get(i)).append("\t");

            for (int i = 0; i < 2; i++)
            {
                // append class label
                cm.append("\n").append(curClasses.get(i)).append("\t");

                // append classifications
                cm.append(pntDist[i][0]).append("\t").append(pntDist[i][1]).append("\t");
            }

            // append accuracy
            cm.append(String.format("\nAccuracy: %.2f%%", 100.0 * correctPoints / totalPointsUsed));

            // append percentage of total points used
            cm.append(String.format("\nData Used: %.2f%%", 100.0 * totalPointsUsed / totalPoints));

            // set current all data confusion matrix string
            DV.allDataCM = cm.toString();

            // set all data confusion matrix
            JTextArea confusionMatrix = new JTextArea(DV.allDataCM);
            confusionMatrix.setFont(confusionMatrix.getFont().deriveFont(Font.BOLD, 12f));
            confusionMatrix.setEditable(false);
            DV.confusionMatrixPanel.add(confusionMatrix);
        }
    }


    /**
     * Generates confusion matrix without overlap data
     * Confusion matrix uses its own function
     */
    private static void getDataWithoutOverlapConfusionMatrix()
    {
        if (DV.withoutOverlapChecked || DV.worstCaseChecked)
        {
            // store overlapping datapoints in upper and lower graphs
            ArrayList<double[]> upper = new ArrayList<>();
            ArrayList<double[]> lower = new ArrayList<>();

            // total datapoints within subset of utilized data
            int totalPoints = 0;
            int totalPointsUsed = 0;

            // check all classes
            for (int i = 0; i < DV.data.size(); i++)
            {
                if (i == DV.upperClass)
                {
                    totalPoints += DV.data.get(i).data.length;

                    for (int j = 0; j < DV.data.get(i).coordinates.length; j++)
                    {
                        double endpoint = DV.data.get(i).coordinates[j][DV.fieldLength-1][0];

                        // if endpoint is outside of overlap then store point
                        if ((DV.overlapArea[0] >= endpoint || endpoint >= DV.overlapArea[1]) && ((DV.domainArea[0] <= endpoint && endpoint <= DV.domainArea[1]) || !DV.domainActive))
                        {
                            double[] thisPoint = new double[DV.fieldLength];
                            System.arraycopy(DV.data.get(i).data[j], 0, thisPoint, 0, DV.fieldLength);

                            upper.add(thisPoint);
                            totalPointsUsed++;
                        }
                    }
                }
                else if (DV.lowerClasses.get(i))
                {
                    totalPoints += DV.data.get(i).data.length;

                    for (int j = 0; j < DV.data.get(i).coordinates.length; j++)
                    {
                        double endpoint = DV.data.get(i).coordinates[j][DV.fieldLength-1][0];

                        // if endpoint is outside of overlap then store point
                        if ((DV.overlapArea[0] >= endpoint || endpoint >= DV.overlapArea[1]) && ((DV.domainArea[0] <= endpoint && endpoint <= DV.domainArea[1]) || !DV.domainActive))
                        {
                            double[] thisPoint = new double[DV.fieldLength];
                            System.arraycopy(DV.data.get(i).data[j], 0, thisPoint, 0, DV.fieldLength);

                            lower.add(thisPoint);
                            totalPointsUsed++;
                        }
                    }
                }
            }

            // create file for python process
            createCSVFileForConfusionMatrix(new ArrayList<>(List.of(upper, lower)));

            // get confusion matrix with LDA
            ArrayList<String> cmValues = LDAForConfusionMatrices(true);

            if (DV.withoutOverlapChecked)
            {
                if (cmValues != null)
                {
                    // create confusion matrix
                    StringBuilder cm = new StringBuilder("Data Without Overlap Analytics\nReal\tPredictions\nClass\t");

                    // append predicted classes
                    for (int i = 0; i < 2; i++)
                        cm.append(curClasses.get(i)).append("\t");

                    for (int i = 0, index = -1; i < 2; i++)
                    {
                        // append class label
                        cm.append("\n").append(curClasses.get(i)).append("\t");

                        // append classifications
                        cm.append(cmValues.get(++index)).append("\t").append(cmValues.get(++index)).append("\t");
                    }

                    // append accuracy
                    cm.append("\n").append(cmValues.get(cmValues.size() - 1));

                    // append percentage of total points used
                    cm.append(String.format("\nData Used: %.2f%%", 100.0 * totalPointsUsed / totalPoints));

                    // set overlap confusion matrix
                    JTextArea confusionMatrix = new JTextArea(cm.toString());
                    confusionMatrix.setFont(confusionMatrix.getFont().deriveFont(Font.BOLD, 12f));
                    confusionMatrix.setEditable(false);
                    DV.confusionMatrixPanel.add(confusionMatrix);
                }
            }
        }
    }


    /**
     * Generates confusion matrix with only overlap data
     * Confusion matrix uses its own function
     */
    private static void getOverlapConfusionMatrix()
    {
        if (DV.overlapChecked || DV.worstCaseChecked)
        {
            // store overlapping datapoints in upper and lower graphs
            upper = new ArrayList<>();
            lower = new ArrayList<>();

            // total datapoints within subset of utilized data
            int totalPoints = 0;
            int totalPointsUsed = 0;

            // check all classes
            for (int i = 0; i < DV.data.size(); i++)
            {
                if (i == DV.upperClass)
                {
                    totalPoints += DV.data.get(i).data.length;

                    for (int j = 0; j < DV.data.get(i).coordinates.length; j++)
                    {
                        double endpoint = DV.data.get(i).coordinates[j][DV.fieldLength-1][0];

                        // if endpoint is within overlap then store point
                        if ((DV.overlapArea[0] <= endpoint && endpoint <= DV.overlapArea[1]) && ((DV.domainArea[0] <= endpoint && endpoint <= DV.domainArea[1]) || !DV.domainActive))
                        {
                            double[] thisPoint = new double[DV.fieldLength];
                            System.arraycopy(DV.data.get(i).data[j], 0, thisPoint, 0, DV.fieldLength);

                            upper.add(thisPoint);
                            totalPointsUsed++;
                        }
                    }
                }
                else if (DV.lowerClasses.get(i))
                {
                    totalPoints += DV.data.get(i).data.length;

                    for (int j = 0; j < DV.data.get(i).coordinates.length; j++)
                    {
                        double endpoint = DV.data.get(i).coordinates[j][DV.fieldLength-1][0];

                        // if endpoint is within overlap then store point
                        if ((DV.overlapArea[0] <= endpoint && endpoint <= DV.overlapArea[1]) && ((DV.domainArea[0] <= endpoint && endpoint <= DV.domainArea[1]) || !DV.domainActive))
                        {
                            double[] thisPoint = new double[DV.fieldLength];
                            System.arraycopy(DV.data.get(i).data[j], 0, thisPoint, 0, DV.fieldLength);

                            lower.add(thisPoint);
                            totalPointsUsed++;
                        }
                    }
                }
            }

            // get percentage of overlap points used
            percentageOverlapPointsUsed = String.format("\nData Used: %.2f%%", 100.0 * totalPointsUsed / totalPoints);

            if (DV.overlapChecked)
            {
                // create file for python process
                createCSVFileForConfusionMatrix(new ArrayList<>(List.of(upper, lower)));

                // get confusion matrix with LDA
                ArrayList<String> cmValues = LDAForConfusionMatrices(false);

                if (cmValues != null && cmValues.size() > 0)
                {
                    // create confusion matrix
                    StringBuilder cm = new StringBuilder("Overlap Analytics\nReal\tPredictions\nClass\t");

                    // append predicted classes
                    for (int i = 0; i < 2; i++)
                        cm.append(curClasses.get(i)).append("\t");

                    for (int i = 0, index = -1; i < 2; i++)
                    {
                        // append class label
                        cm.append("\n").append(curClasses.get(i)).append("\t");

                        // append classifications
                        cm.append(cmValues.get(++index)).append("\t").append(cmValues.get(++index)).append("\t");
                    }

                    // append accuracy
                    cm.append("\n").append(cmValues.get(cmValues.size() - 1));

                    // append percentage of total points used
                    cm.append(String.format("\nData Used: %.2f%%", 100.0 * totalPointsUsed / totalPoints));

                    // set overlap confusion matrix
                    JTextArea confusionMatrix = new JTextArea(cm.toString());
                    confusionMatrix.setFont(confusionMatrix.getFont().deriveFont(Font.BOLD, 12f));
                    confusionMatrix.setEditable(false);
                    DV.confusionMatrixPanel.add(confusionMatrix);
                }
                else if (cmValues != null)
                {
                    // create confusion matrix
                    StringBuilder cm = new StringBuilder("Overlap Analytics\nReal\tPredictions\nClass\t");

                    // append predicted classes
                    for (int i = 0; i < 2; i++)
                        cm.append(curClasses.get(i)).append("\t");

                    for (int i = 0; i < 2; i++)
                    {
                        // append class label
                        cm.append("\n").append(curClasses.get(i)).append("\t");

                        // append classifications
                        cm.append(0).append("\t").append(0).append("\t");
                    }

                    // append accuracy
                    cm.append("\nAccuracy: NaN%");

                    // append percentage of total points used
                    cm.append(percentageOverlapPointsUsed);

                    // set overlap confusion matrix
                    JTextArea confusionMatrix = new JTextArea(cm.toString());
                    confusionMatrix.setFont(confusionMatrix.getFont().deriveFont(Font.BOLD, 12f));
                    confusionMatrix.setEditable(false);
                    DV.confusionMatrixPanel.add(confusionMatrix);
                }
            }
        }
    }


    /**
     * Generates confusion matrix with only overlap data
     * Confusion matrix uses function from getDataWithoutOverlapConfusionMatrix
     */
    private static void getWorstCaseConfusionMatrix()
    {
        if (DV.worstCaseChecked)
        {
            // get double[][] arrays for data
            double[][] tmpUpper = new double[upper.size()][];
            double[][] tmpLower = new double[lower.size()][];

            for (int i = 0; i < upper.size(); i++)
                tmpUpper[i] = upper.get(i);

            for (int i = 0; i < lower.size(); i++)
                tmpLower[i] = lower.get(i);

            // create DataObjects of overlapping points
            DataObject upperOverlap = new DataObject("upper", tmpUpper);
            DataObject lowerOverlap = new DataObject("lower", tmpLower);

            // get data without overlap threshold
            double worstCaseThreshold = LDAFunction.get(LDAFunction.size() - 1);

            // get data without overlap angles
            double[] worstCaseAngles = new double[DV.fieldLength];

            for (int i = 0; i < DV.fieldLength; i++)
                worstCaseAngles[i] = LDAFunction.get(i);

            // update points with worst case angles
            upperOverlap.updateCoordinates(worstCaseAngles);
            lowerOverlap.updateCoordinates(worstCaseAngles);

            int totalPointsUsed = lower.size() + upper.size();
            int correctPoints = 0;

            // get point distribution
            int[][] pntDist = new int[2][2];

            // get distribution for upper graph
            for (int i = 0; i < upper.size(); i++)
            {
                if (DV.upperIsLower)
                {
                    if (upperOverlap.coordinates[i][DV.fieldLength- 1][0] < worstCaseThreshold)
                    {
                        pntDist[0][0]++;
                        correctPoints++;
                    }
                    else
                        pntDist[0][1]++;
                }
                else
                {
                    if (upperOverlap.coordinates[i][DV.fieldLength- 1][0] > worstCaseThreshold)
                    {
                        pntDist[1][1]++;
                        correctPoints++;
                    }
                    else
                        pntDist[1][0]++;
                }
            }

            // get point distribution for lower graph
            for (int i = 0; i < lower.size(); i++)
            {
                if (DV.upperIsLower)
                {
                    if (lowerOverlap.coordinates[i][DV.fieldLength- 1][0] > worstCaseThreshold)
                    {
                        pntDist[1][1]++;
                        correctPoints++;
                    }
                    else
                        pntDist[1][0]++;
                }
                else
                {
                    if (lowerOverlap.coordinates[i][DV.fieldLength- 1][0] < worstCaseThreshold)
                    {
                        pntDist[0][0]++;
                        correctPoints++;
                    }
                    else
                        pntDist[0][1]++;
                }
            }

            // create confusion matrix
            StringBuilder cm = new StringBuilder("Worst Case Data Analytics\nReal\tPredictions\nClass\t");

            // append predicted classes
            for (int i = 0; i < 2; i++)
                cm.append(curClasses.get(i)).append("\t");

            for (int i = 0; i < 2; i++)
            {
                // append class label
                cm.append("\n").append(curClasses.get(i)).append("\t");

                // append classifications
                cm.append(pntDist[i][0]).append("\t").append(pntDist[i][1]).append("\t");
            }

            // append accuracy
            cm.append(String.format("\nAccuracy: %.2f%%", 100.0 * correctPoints / totalPointsUsed));

            // append percentage of total points used
            cm.append(percentageOverlapPointsUsed);

            // set all data confusion matrix
            JTextArea confusionMatrix = new JTextArea(cm.toString());
            confusionMatrix.setFont(confusionMatrix.getFont().deriveFont(Font.BOLD, 12f));
            confusionMatrix.setEditable(false);
            DV.confusionMatrixPanel.add(confusionMatrix);
        }
    }


    private static void getUserValidationConfusionMatrix()
    {
        if (DV.userValidationImported && DV.userValidationChecked)
        {
            // get data without overlap threshold
            double worstCaseThreshold = LDAFunction.get(LDAFunction.size() - 1);

            // get data without overlap angles
            double[] worstCaseAngles = new double[DV.fieldLength];

            for (int i = 0; i < DV.fieldLength; i++)
                worstCaseAngles[i] = LDAFunction.get(i);

            // update points with worst case angles
            for (int i = 0; i < DV.validationData.size(); i++)
                DV.validationData.get(i).updateCoordinates(worstCaseAngles);

            int totalPoints = 0;

            for (int i = 0; i < DV.data.size(); i++)
                totalPoints += DV.data.get(i).data.length;

            int totalPointsUsed = 0;
            int correctPoints = 0;

            // get point distribution
            int[][] pntDist = new int[2][2];

            for (int i = 0; i < DV.validationData.size(); i++)
            {
                if (i == DV.upperClass || DV.lowerClasses.get(i))
                {
                    for (int j = 0; j < DV.validationData.get(i).coordinates.length; j++)
                    {
                        double endpoint = DV.validationData.get(i).coordinates[j][DV.fieldLength-1][0];

                        // check if endpoint is within the subset of used data
                        if ((DV.domainArea[0] <= endpoint && endpoint <= DV.domainArea[1]) || !DV.domainActive)
                        {
                            // get classification
                            if (i == DV.upperClass && DV.upperIsLower)
                            {
                                // check if endpoint is correctly classified
                                if (endpoint < worstCaseThreshold)
                                {
                                    pntDist[0][0]++;
                                    totalPointsUsed++;
                                    correctPoints++;
                                }
                                else
                                {
                                    totalPointsUsed++;
                                    pntDist[0][1]++;
                                }
                            }
                            else if (i == DV.upperClass)
                            {
                                // check if endpoint is correctly classified
                                if (endpoint > worstCaseThreshold)
                                {
                                    pntDist[0][0]++;
                                    totalPointsUsed++;
                                    correctPoints++;
                                }
                                else
                                {
                                    totalPointsUsed++;
                                    pntDist[0][1]++;
                                }
                            }
                            else if(DV.lowerClasses.get(i) && DV.upperIsLower)
                            {
                                // check if endpoint is correctly classified
                                if (endpoint > worstCaseThreshold)
                                {
                                    pntDist[1][1]++;
                                    totalPointsUsed++;
                                    correctPoints++;
                                }
                                else
                                {
                                    totalPointsUsed++;
                                    pntDist[1][0]++;
                                }
                            }
                            else
                            {
                                // check if endpoint is correctly classified
                                if (endpoint < worstCaseThreshold)
                                {
                                    pntDist[1][1]++;
                                    totalPointsUsed++;
                                    correctPoints++;
                                }
                                else
                                {
                                    totalPointsUsed++;
                                    pntDist[1][0]++;
                                }
                            }
                        }
                    }
                }
            }

            // create confusion matrix
            StringBuilder cm = new StringBuilder("User Validation Data Analytics\nReal\tPredictions\nClass\t");

            // append predicted classes
            for (int i = 0; i < 2; i++)
                cm.append(curClasses.get(i)).append("\t");

            for (int i = 0; i < 2; i++)
            {
                // append class label
                cm.append("\n").append(curClasses.get(i)).append("\t");

                // append classifications
                cm.append(pntDist[i][0]).append("\t").append(pntDist[i][1]).append("\t");
            }

            // append accuracy
            cm.append(String.format("\nAccuracy: %.2f%%", 100.0 * correctPoints / totalPointsUsed));

            // append percentage of total points used
            cm.append(String.format("\nData Used: %.2f%%", 100.0 * totalPointsUsed / totalPoints));

            // set all data confusion matrix
            JTextArea confusionMatrix = new JTextArea( cm.toString());
            confusionMatrix.setFont(confusionMatrix.getFont().deriveFont(Font.BOLD, 12f));
            confusionMatrix.setEditable(false);
            DV.confusionMatrixPanel.add(confusionMatrix);
        }
    }


    /**
     * Creates CSV file representing specified data from
     * the upper class as class 1 and lower graph as class 2
     */
    private static void createCSVFileForConfusionMatrix(ArrayList<ArrayList<double[]>> data)
    {
        try
        {
            // create csv file
            File csv = new File("src\\LDA\\DV_CM_data.csv");
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
            for (int i = 0; i < data.size(); i++)
            {
                for (int j = 0; j < data.get(i).size(); j++)
                {
                    for (int k = 0; k < DV.fieldLength; k++)
                    {
                        if (k != DV.fieldLength - 1)
                            out.printf("%f,", data.get(i).get(j)[k]);
                        else
                            out.printf("%f," + i + "\n", data.get(i).get(j)[k]);
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
     * Gets functions for confusion matrices
     * @param storeFunction whether to store angles and threshold
     */
    private static ArrayList<String> LDAForConfusionMatrices(boolean storeFunction)
    {
        // get python boolean
        String pyBool;

        if (storeFunction)
            pyBool = "T";
        else
            pyBool = "";

        // create LDA (python) process
        ProcessBuilder lda = new ProcessBuilder(System.getProperty("user.dir") + "\\venv\\Scripts\\python",
                System.getProperty("user.dir") + "\\src\\LDA\\ConfusionMatrixGenerator.py",
                System.getProperty("user.dir") + "\\src\\LDA\\DV_CM_data.csv",
                pyBool);

        try
        {
            // run python (LDA) process
            Process process = lda.start();

            // read python outputs
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String output;

            // confusion matrix classifications
            ArrayList<String> classifications = new ArrayList<>();

            // data without overlap confusion matrix angles and threshold
            if (storeFunction)
                LDAFunction = new ArrayList<>();

            int cnt = 0;

            while ((output = reader.readLine()) != null)
            {
                // get angles and threshold
                if (storeFunction && cnt <= DV.fieldLength)
                {
                    LDAFunction.add(Double.parseDouble(output));
                    cnt++;
                }
                else
                {
                    // get rows of confusion matrix
                    classifications.add(output);
                }
            }

            // delete created file
            //File fileToDelete = new File("src\\LDA\\Overlap_Data.csv");
            //Files.deleteIfExists(fileToDelete.toPath());

            // return confusion matrix
            return classifications;
        }
        catch (IOException e)
        {
            JOptionPane.showMessageDialog(DV.mainFrame, "Error: could not run Linear Discriminant Analysis", "Error", JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }


    /**
     * Runs k-fold cross validation on dataset
     */
    static void runKFoldCrossValidation()
    {
        // store datapoints in upper and lower graphs
        ArrayList<double[]> upper = new ArrayList<>();
        ArrayList<double[]> lower = new ArrayList<>();

        // check all classes
        for (int i = 0; i < DV.data.size(); i++)
        {
            if (i == DV.upperClass)
            {
                for (int j = 0; j < DV.data.get(i).coordinates.length; j++)
                {
                    double endpoint = DV.data.get(i).coordinates[j][DV.fieldLength-1][0];

                    // if endpoint is outside of overlap then store point
                    if ((DV.domainArea[0] <= endpoint && endpoint <= DV.domainArea[1]) || !DV.domainActive)
                    {
                        double[] thisPoint = new double[DV.fieldLength];
                        System.arraycopy(DV.data.get(i).data[j], 0, thisPoint, 0, DV.fieldLength);

                        upper.add(thisPoint);
                    }
                }
            }
            else if (DV.lowerClasses.get(i))
            {
                for (int j = 0; j < DV.data.get(i).coordinates.length; j++)
                {
                    double endpoint = DV.data.get(i).coordinates[j][DV.fieldLength-1][0];

                    // if endpoint is outside of overlap then store point
                    if ((DV.domainArea[0] <= endpoint && endpoint <= DV.domainArea[1]) || !DV.domainActive)
                    {
                        double[] thisPoint = new double[DV.fieldLength];
                        System.arraycopy(DV.data.get(i).data[j], 0, thisPoint, 0, DV.fieldLength);

                        lower.add(thisPoint);
                    }
                }
            }
        }

        // create file for python process
        createCSVFileForConfusionMatrix(new ArrayList<>(List.of(upper, lower)));

        // create k-fold (python) process
        ProcessBuilder cv = new ProcessBuilder(System.getProperty("user.dir") + "\\venv\\Scripts\\python",
                System.getProperty("user.dir") + "\\src\\LDA\\kFoldCrossValidation.py",
                System.getProperty("user.dir") + "\\src\\LDA\\DV_CM_data.csv",
                String.valueOf(DV.kFolds));
        //lda.inheritIO();

        try
        {
            // run python (LDA) process
            Process process = cv.start();

            // read python outputs
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String output;

            // confusion matrix classifications
            ArrayList<String> cvTable = new ArrayList<>();

            while ((output = reader.readLine()) != null)
            {
                // get cross validation table
                cvTable.add(output);
            }

            if (cvTable.size() > 0)
            {
                // create confusion matrix
                StringBuilder table = new StringBuilder("k-Fold Cross Validation");

                for (String row : cvTable) {
                    // append model rows
                    table.append("\n").append(row);
                }

                // set overlap confusion matrix
                JTextArea cross_validate = new JTextArea(table.toString());
                cross_validate.setFont(cross_validate.getFont().deriveFont(Font.BOLD, 12f));
                cross_validate.setEditable(false);
                DV.crossValidationPanel.add(cross_validate);
            }
        }
        catch (IOException e)
        {
            JOptionPane.showMessageDialog(DV.mainFrame, "Error: could not run k-Fold Cross Validation", "Error", JOptionPane.ERROR_MESSAGE);
        }
        // get output
        // display output
    }
}
