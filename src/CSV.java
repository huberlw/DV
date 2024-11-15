import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CSV
{
    // exception logger
    private final static Logger LOGGER = Logger.getLogger(Analytics.class.getName());


    /**
     * Creates CSV file representing specified data
     * classes are labeled from 0 to n-1
     * @param data data to be used in csv file
     * @param filePath path of file to be created
     */
    public static void createCSV(ArrayList<ArrayList<double[]>> data, String filePath)
    {
        try
        {
            // write to csv file
            Writer out = new FileWriter(filePath, false);

            // create header for file
            for (int i = 0; i < DV.fieldLength; i++)
            {
                if (i != DV.fieldLength - 1)
                    out.write(DV.fieldNames.get(i) + ",");
                else
                    out.write(DV.fieldNames.get(i) + ",class\n");
            }

            // check all classes
            for (int i = 0; i < data.size(); i++)
            {
                for (int j = 0; j < data.get(i).size(); j++)
                {
                    for (int k = 0; k < data.get(i).get(0).length; k++)
                    {
                        if (k != data.get(i).get(0).length - 1)
                            out.write(String.format("%f,", data.get(i).get(j)[k]));
                        else
                            out.write(String.format("%f," + i + "\n", data.get(i).get(j)[k]));
                    }
                }
            }

            // close file
            out.close();
        }
        catch (IOException ioe)
        {
            LOGGER.log(Level.SEVERE, ioe.toString(), ioe);
        }
    }


    /**
     * Creates CSV file representing specified data from DataObject
     * @param data data to be used in csv file
     * @param filePath path of file to be created
     */
    public static void createCSVDataObject(ArrayList<DataObject> data, String filePath)
    {
        try
        {
            // write to csv file
            Writer out = new FileWriter(filePath, false);

            // create header for file
            for (int i = 0; i < DV.fieldLength; i++)
            {
                if (i != DV.fieldLength - 1)
                    out.write(DV.fieldNames.get(i) + ",");
                else
                    out.write(DV.fieldNames.get(i) + ",class\n");
            }

            // check all classes
            for (DataObject datum : data)
            {
                for (int j = 0; j < datum.data.length; j++)
                {
                    for (int k = 0; k < DV.fieldLength; k++)
                    {
                        if (k != DV.fieldLength - 1)
                            out.write(String.format("%f,", datum.data[j][k]));
                        else
                            out.write(String.format("%f," + datum.className + "\n", datum.data[j][k]));
                    }
                }
            }

            // close file
            out.close();
        }
        catch (IOException ioe)
        {
            LOGGER.log(Level.SEVERE, ioe.toString(), ioe);
        }
    }


    /**
     * Creates CSV file representing specified regression data
     * @param data data to be used in csv file
     * @param filePath path of file to be created
     */
    public static void createRegCSVDataObject(ArrayList<DataObject> data, String filePath)
    {
        try
        {
            // write to csv file
            Writer out = new FileWriter(filePath, false);

            // create header for file
            for (int i = 0; i < DV.fieldLength; i++)
            {
                if (i != DV.fieldLength - 1)
                    out.write(DV.fieldNames.get(i) + ",");
                else
                    out.write(DV.fieldNames.get(i) + ",class\n");
            }

            // check all classes
            for (int i = 0; i < data.get(0).data.length; i++)
            {
                for (int j = 0; j < DV.fieldLength; j++)
                {
                    if (j != DV.fieldLength - 1)
                        out.write(String.format("%f,", data.get(0).data[i][j]));
                    else
                        out.write(String.format("%f," + "%f" + "\n", data.get(0).data[i][j], DV.reg_true_values.get(i)));
                }
            }

            // close file
            out.close();
        }
        catch (IOException ioe)
        {
            LOGGER.log(Level.SEVERE, ioe.toString(), ioe);
        }
    }


    /**
     * Creates k folds from the data and saves them to CSV files
     * @param path The path to save the folds
     * @param data The data to be split into folds
     * @param k The number of folds
     */
    public static void createKFoldsAndSaveToCSV(String path, String[][] data, int k)
    {
        List<String[]> dataList = new ArrayList<>(Arrays.asList(data));
        String[] header = dataList.get(0);
        dataList.remove(0); // Remove the header

        // Shuffle the data
        Collections.shuffle(dataList);

        // Calculate the size of each fold
        int foldSize = dataList.size() / k;
        for (int i = 0; i < k; i++)
        {
            List<String[]> testFold = new ArrayList<>();
            List<String[]> trainFold = new ArrayList<>(dataList);
            for (int j = 0; j < foldSize; j++)
            {
                int index = i * foldSize + j;
                if (index < dataList.size())
                    testFold.add(dataList.get(index));
            }

            // Remove test fold data from train fold
            trainFold.removeAll(testFold);

            // Write train and test folds to CSV
            writeFoldToCSV(trainFold, path + "\\test_fold_" + (i + 1) + ".csv", header);
            writeFoldToCSV(testFold, path + "\\train_fold_" + (i + 1) + ".csv", header);
        }
    }


    /**
     * Writes the fold data to a CSV file
     * @param fold The fold data
     * @param fileName The name of the file
     * @param header The header of the data
     */
    private static void writeFoldToCSV(List<String[]> fold, String fileName, String[] header)
    {
        try (FileWriter writer = new FileWriter(fileName))
        {
            // Write the header
            for (int i = 0; i < header.length; i++)
            {
                writer.append(header[i]);
                if (i < header.length - 1)
                    writer.append(",");
            }
            writer.append("\n");

            for (String[] dataPoint : fold)
            {
                for (int i = 0; i < dataPoint.length; i++)
                {
                    writer.append(dataPoint[i]);
                    if (i < dataPoint.length - 1)
                        writer.append(",");
                }
                writer.append("\n");
            }
        }
        catch (IOException ioe)
        {
            LOGGER.log(Level.SEVERE, ioe.toString(), ioe);
        }
    }
}
