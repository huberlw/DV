import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class ConfusionMatrices
{
    // data without overlap confusion matrix angles and threshold
    static ArrayList<Double> noOverlapAngles = new ArrayList<>();
    static double noOverlapThreshold;

    /**
     * Generates all data, data without overlap,
     * overlap, and worst case confusion matrices
     */
    public static void generateConfusionMatrices()
    {
        //getAllDataConfusionMatrix();
        //getDataWithoutOverlapConfusionMatrix();
        getOverlapConfusionMatrix();
        //getWorstCaseConfusionMatrix();
    }


    /**
     * Generates confusion matrix with all data
     * Confusion matrix uses its own function
     */
    private static void getAllDataConfusionMatrix()
    {
        //LDAForConfusionMatrices(false);
    }


    /**
     * Generates confusion matrix without overlap data
     * Confusion matrix uses its own function
     */
    private static void getDataWithoutOverlapConfusionMatrix()
    {
        //LDAForConfusionMatrices(true);
    }


    /**
     * Generates confusion matrix with only overlap data
     * Confusion matrix uses its own function
     */
    private static void getOverlapConfusionMatrix()
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
                    if (DV.overlapArea[0] >= endpoint && endpoint <= DV.overlapArea[1])
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
                    if (DV.overlapArea[0] >= endpoint && endpoint <= DV.overlapArea[1])
                    {
                        double[] thisPoint = new double[DV.fieldLength];
                        System.arraycopy(DV.data.get(i).data[j], 0, thisPoint, 0, DV.fieldLength);

                        lower.add(thisPoint);
                    }
                }
            }
        }

        // get confusion matrix with LDA
        LDAForConfusionMatrices(new ArrayList<>(List.of(upper, lower)), false);
    }


    /**
     * Generates confusion matrix with only overlap data
     * Confusion matrix uses function from getDataWithoutOverlapConfusionMatrix
     */
    private static void getWorstCaseConfusionMatrix()
    {
        //LDAForConfusionMatrices(false);
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
    private static void LDAForConfusionMatrices(ArrayList<ArrayList<double[]>> data, boolean storeFunction)
    {
        // get python boolean
        String bool;
        if (storeFunction)
            bool = "T";
        else
            bool = "";

        // create LDA (python) process
        ProcessBuilder ldaCM = new ProcessBuilder("venv\\Scripts\\python",
                System.getProperty("user.dir") + "\\src\\LDA\\ConfusionMatrixGenerator.py",
                System.getProperty("user.dir") + "\\src\\LDA\\DV_CM_data.csv",
                String.valueOf(DV.fieldLength),
                bool);

        ldaCM.inheritIO();

        try
        {
            // create file for python process
            createCSVFileForConfusionMatrix(data);

            // run python (LDA) process
            Process processCM = ldaCM.start();

            // read python outputs
            BufferedReader reader = new BufferedReader(new InputStreamReader(processCM.getInputStream()));
            String output;

            int cnt = 0;

            while ((output = reader.readLine()) != null)
            {
                /*if (storeFunction && cnt < DV.fieldLength)
                {
                    noOverlapAngles.add(Double.parseDouble(output));
                    cnt++;
                    continue;
                }
                else if(storeFunction && cnt == DV.fieldLength)
                {
                    noOverlapThreshold = Double.parseDouble(output);
                    cnt++;
                    continue;
                }*/

                System.out.println(output);
            }

            // delete created file
            File fileToDelete = new File("src\\LDA\\DV_CM_data.csv");
            Files.deleteIfExists(fileToDelete.toPath());
        }
        catch (IOException ioe)
        {
            ioe.printStackTrace();
        }
    }
}
