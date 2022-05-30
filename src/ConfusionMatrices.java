import javax.swing.*;
import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class ConfusionMatrices
{
    // holds angles and threshold for LDA
    static ArrayList<Double> LDAFunction = new ArrayList<>();

    /**
     * Generates all data, data without overlap,
     * overlap, and worst case confusion matrices
     */
    public static void generateConfusionMatrices()
    {
        getAllDataConfusionMatrix();
        getDataWithoutOverlapConfusionMatrix();
        getOverlapConfusionMatrix();
        getWorstCaseConfusionMatrix();
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
            int correctPoints = 0;

            // get point distribution
            int[][] pntDist = new int[2][2];

            for (int i = 0; i < DV.data.size(); i++)
            {
                if (i == DV.upperClass || DV.lowerClasses.get(i))
                {
                    totalPoints += DV.data.get(i).data.length;

                    for (int j = 0; j < DV.data.get(i).coordinates.length; j++)
                    {
                        double endpoint = DV.data.get(i).coordinates[j][DV.fieldLength-1][0];

                        // get classification
                        if (i == DV.upperClass && DV.upperIsLower)
                        {
                            // check if endpoint is correctly classified
                            if (endpoint < DV.threshold && ((DV.domainArea[0] <= endpoint && endpoint <= DV.domainArea[1]) || !DV.domainActive))
                            {
                                pntDist[0][0]++;
                                correctPoints++;
                            }
                            else
                                pntDist[0][1]++;
                        }
                        else if (i == DV.upperClass)
                        {
                            // check if endpoint is correctly classified
                            if (endpoint > DV.threshold && ((DV.domainArea[0] <= endpoint && endpoint <= DV.domainArea[1]) || !DV.domainActive))
                            {
                                pntDist[0][0]++;
                                correctPoints++;
                            }
                            else
                                pntDist[0][1]++;
                        }
                        else if(DV.lowerClasses.get(i) && DV.upperIsLower)
                        {
                            // check if endpoint is correctly classified
                            if (endpoint > DV.threshold && ((DV.domainArea[0] <= endpoint && endpoint <= DV.domainArea[1]) || !DV.domainActive))
                            {
                                pntDist[1][1]++;
                                correctPoints++;
                            }
                            else
                                pntDist[1][0]++;
                        }
                        else
                        {
                            // check if endpoint is correctly classified
                            if (endpoint < DV.threshold && ((DV.domainArea[0] <= endpoint && endpoint <= DV.domainArea[1]) || !DV.domainActive))
                            {
                                pntDist[1][1]++;
                                correctPoints++;
                            }
                            else
                                pntDist[1][0]++;
                        }
                    }
                }
            }

            // create confusion matrix
            StringBuilder cm = new StringBuilder("All Data Analytics\nReal\tPredictions\nClass\t");

            // append predicted classes
            for (int i = 1; i <= 2 + DV.prevAccuracies.size(); i++)
                cm.append(i).append("\t");

            for (int i = 0; i < 2 + DV.prevAccuracies.size(); i++)
            {
                // append class label
                cm.append("\n").append(i+1).append("\t");

                // append classifications
                cm.append(pntDist[i][0]).append("\t").append(pntDist[i][1]).append("\t");
            }

            // append accuracy
            cm.append(String.format("\nAccuracy: %.2f%%", 100.0 * correctPoints / totalPoints));

            // set all data confusion matrix
            DV.allDataCM.setText(cm.toString());
        }
        else
        {
            DV.allDataCM.setText("");
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

            // check all classes
            for (int i = 0; i < DV.data.size(); i++)
            {
                if (i == DV.upperClass)
                {
                    for (int j = 0; j < DV.data.get(i).coordinates.length; j++)
                    {
                        double endpoint = DV.data.get(i).coordinates[j][DV.fieldLength-1][0];

                        // if endpoint is outside of overlap then store point
                        if ((DV.overlapArea[0] >= endpoint || endpoint >= DV.overlapArea[1]) && ((DV.domainArea[0] <= endpoint && endpoint <= DV.domainArea[1]) || !DV.domainActive))
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
                        if ((DV.overlapArea[0] >= endpoint || endpoint >= DV.overlapArea[1]) && ((DV.domainArea[0] <= endpoint && endpoint <= DV.domainArea[1]) || !DV.domainActive))
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

            // get confusion matrix with LDA
            ArrayList<String> cmValues = LDAForConfusionMatrices(true);

            if (DV.withoutOverlapChecked)
            {
                if (cmValues != null)
                {
                    // create confusion matrix
                    StringBuilder cm = new StringBuilder("Data Without Overlap Analytics\nReal\tPredictions\nClass\t");

                    // append predicted classes
                    for (int i = 1; i <= 2 + DV.prevAccuracies.size(); i++)
                        cm.append(i).append("\t");

                    int cnt = 0;
                    int index = -1;

                    while (cnt++ < 2 + DV.prevAccuracies.size())
                    {
                        // append class label
                        cm.append("\n").append(cnt).append("\t");

                        // append classifications
                        cm.append(cmValues.get(++index)).append("\t").append(cmValues.get(++index)).append("\t");
                    }

                    // append accuracy
                    cm.append("\n").append(cmValues.get(cmValues.size() - 1));

                    // set overlap confusion matrix
                    DV.dataWithoutOverlapCM.setText(cm.toString());
                }
            }
            else
                DV.dataWithoutOverlapCM.setText("");
        }
        else
            DV.dataWithoutOverlapCM.setText("");
    }


    /**
     * Generates confusion matrix with only overlap data
     * Confusion matrix uses its own function
     */
    private static void getOverlapConfusionMatrix()
    {
        if (DV.overlapChecked)
        {
            // store overlapping datapoints in upper and lower graphs
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

                        // if endpoint is within overlap then store point
                        if ((DV.overlapArea[0] <= endpoint && endpoint <= DV.overlapArea[1]) && ((DV.domainArea[0] <= endpoint && endpoint <= DV.domainArea[1]) || !DV.domainActive))
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

                        // if endpoint is within overlap then store point
                        if ((DV.overlapArea[0] <= endpoint && endpoint <= DV.overlapArea[1]) && ((DV.domainArea[0] <= endpoint && endpoint <= DV.domainArea[1]) || !DV.domainActive))
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

            // get confusion matrix with LDA
            ArrayList<String> cmValues = LDAForConfusionMatrices(false);

            if (cmValues != null)
            {
                // create confusion matrix
                StringBuilder cm = new StringBuilder("Overlap Analytics\nReal\tPredictions\nClass\t");

                // append predicted classes
                for (int i = 1; i <= 2 + DV.prevAccuracies.size(); i++)
                    cm.append(i).append("\t");

                int cnt = 0;
                int index = -1;

                while (cnt++ < 2 + DV.prevAccuracies.size())
                {
                    // append class label
                    cm.append("\n").append(cnt).append("\t");

                    // append classifications
                    cm.append(cmValues.get(++index)).append("\t").append(cmValues.get(++index)).append("\t");
                }

                // append accuracy
                cm.append("\n").append(cmValues.get(cmValues.size() - 1));

                // set overlap confusion matrix
                DV.overlapCM.setText(cm.toString());
            }
        }
        else
            DV.overlapCM.setText("");
    }


    /**
     * Generates confusion matrix with only overlap data
     * Confusion matrix uses function from getDataWithoutOverlapConfusionMatrix
     */
    private static void getWorstCaseConfusionMatrix()
    {
        if (DV.worstCaseChecked)
        {
            // get data without overlap threshold
            double worstCaseThreshold = LDAFunction.get(LDAFunction.size() - 1);

            // get data without overlap angles
            double[] worstCaseAngles = new double[DV.fieldLength];

            for (int i = 0; i < DV.fieldLength; i++)
                worstCaseAngles[i] = LDAFunction.get(i);

            // update points with worst case angles
            for (int i = 0; i < DV.data.size(); i++)
            {
                if (i == DV.upperClass || DV.lowerClasses.get(i))
                    DV.data.get(i).updateCoordinates(worstCaseAngles);
            }

            int totalPoints = 0;
            int correctPoints = 0;

            // get point distribution
            int[][] pntDist = new int[2][2];

            for (int i = 0; i < DV.data.size(); i++)
            {
                if (i == DV.upperClass || DV.lowerClasses.get(i))
                {
                    totalPoints += DV.data.get(i).data.length;

                    for (int j = 0; j < DV.data.get(i).coordinates.length; j++)
                    {
                        double endpoint = DV.data.get(i).coordinates[j][DV.fieldLength-1][0];

                        // get classification
                        if (i == DV.upperClass && DV.upperIsLower)
                        {
                            // check if endpoint is correctly classified
                            if (endpoint < worstCaseThreshold && ((DV.domainArea[0] <= endpoint && endpoint <= DV.domainArea[1]) || !DV.domainActive))
                            {
                                pntDist[0][0]++;
                                correctPoints++;
                            }
                            else
                                pntDist[0][1]++;
                        }
                        else if (i == DV.upperClass)
                        {
                            // check if endpoint is correctly classified
                            if (endpoint > worstCaseThreshold && ((DV.domainArea[0] <= endpoint && endpoint <= DV.domainArea[1]) || !DV.domainActive))
                            {
                                pntDist[0][0]++;
                                correctPoints++;
                            }
                            else
                                pntDist[0][1]++;
                        }
                        else if(DV.lowerClasses.get(i) && DV.upperIsLower)
                        {
                            // check if endpoint is correctly classified
                            if (endpoint > worstCaseThreshold && ((DV.domainArea[0] <= endpoint && endpoint <= DV.domainArea[1]) || !DV.domainActive))
                            {
                                pntDist[1][1]++;
                                correctPoints++;
                            }
                            else
                                pntDist[1][0]++;
                        }
                        else
                        {
                            // check if endpoint is correctly classified
                            if (endpoint < worstCaseThreshold && ((DV.domainArea[0] <= endpoint && endpoint <= DV.domainArea[1]) || !DV.domainActive))
                            {
                                pntDist[1][1]++;
                                correctPoints++;
                            }
                            else
                                pntDist[1][0]++;
                        }
                    }
                }
            }

            // create confusion matrix
            StringBuilder cm = new StringBuilder("Worst Case Analytics\nReal\tPredictions\nClass\t");

            // append predicted classes
            for (int i = 1; i <= 2 + DV.prevAccuracies.size(); i++)
                cm.append(i).append("\t");

            for (int i = 0; i < 2 + DV.prevAccuracies.size(); i++)
            {
                // append class label
                cm.append("\n").append(i+1).append("\t");

                // append classifications
                cm.append(pntDist[i][0]).append("\t").append(pntDist[i][1]).append("\t");
            }

            // append accuracy
            cm.append(String.format("\nAccuracy: %.2f%%", 100.0 * correctPoints / totalPoints));

            // set all data confusion matrix
            DV.worstCaseCM.setText(cm.toString());
        }
        else
            DV.worstCaseCM.setText("");
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
        ProcessBuilder lda = new ProcessBuilder("venv\\Scripts\\python",
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
            File fileToDelete = new File("src\\LDA\\DV_CM_data.csv");
            Files.deleteIfExists(fileToDelete.toPath());

            // return confusion matrix
            return classifications;
        }
        catch (IOException e)
        {
            JOptionPane.showMessageDialog(DV.mainFrame, "Error: could not run Linear Discriminant Analysis", "Error", JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }
}
