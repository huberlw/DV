import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class DataSetup
{
    /**
     * Sets up DV with data from datafile
     * @param dataFile .csv file holding data
     * @return whether parseData was successful
     */
    public static boolean setupWithData(File dataFile)
    {
        // data string[][] representation of dataFile (csv)
        String[][] stringData = getStringFromCSV(dataFile);

        if (stringData != null)
        {
            // get classes and class number then removes classes dataset
            if (DV.hasClasses)
            {
                getClasses(stringData);
                stringData = purgeClasses(stringData);
            }
            else
                DV.classNumber = 1;

            // remove ID
            if (DV.hasID)
                stringData = purgeID(stringData);

            // set class visualization -> all classes except the first go on the lower graph
            DV.upperClass = 0;
            DV.lowerClasses = new ArrayList<>(List.of(false));

            for (int i = 1; i < DV.classNumber; i++)
                DV.lowerClasses.add(true);

            // get fieldNames and fieldLength
            DV.fieldNames = getFieldNames(stringData);
            DV.fieldLength = DV.fieldNames.size();

            // initializes angles to 45 degrees
            DV.angles = new double[DV.fieldLength];

            for (int i = 0; i < DV.fieldLength; i++)
                DV.angles[i] = 45;

            // get numerical data from string data
            double[][] numericalData = stringToNumerical(stringData);

            // get normalized numerical data if not null
            if (numericalData != null)
            {
                // save original data
                double[][] originalNumericalData = new double[numericalData.length][];

                for (int i = 0; i < numericalData.length; i++)
                    originalNumericalData[i] = Arrays.copyOf(numericalData[i], DV.fieldLength);

                // normalize data
                double[][] normalizedNumericalData = normalizeData(numericalData);

                // separate by class
                ArrayList<double[][]> splitByClass = separateByClass(normalizedNumericalData);
                ArrayList<double[][]> originalByClass = separateByClass(originalNumericalData);

                // transform classes into data objects
                DV.data = createDataObjects(splitByClass);
                DV.originalData = createDataObjects(originalByClass);

                return true;
            }
            else
                return false;
        }
        else
            return false;
    }


    /**
     * Gets all classes from input data
     * @param stringData String[][] representation of input data
     */
    private static void getClasses(String[][] stringData)
    {
        if (stringData != null)
        {
            // LinkedHashSet preserves order and does not allow duplicates
            LinkedHashSet<String> unique = new LinkedHashSet<>();

            DV.allClasses = new ArrayList<>();

            // add all classes to unique and allClasses
            for (int i = 1; i < stringData.length; i++)
            {
                unique.add(stringData[i][stringData[0].length - 1]);
                DV.allClasses.add(stringData[i][stringData[0].length - 1]);
            }

            // store unique classes and class number
            DV.uniqueClasses = new ArrayList<>(unique);
            DV.classNumber = unique.size();
        }
    }


    /**
     *
     * @param dataFile .csv file holding data
     * @return String[][] representation of dataFile
     */
    private static String[][] getStringFromCSV(File dataFile)
    {
        try (Scanner fileReader = new Scanner(dataFile))
        {
            ArrayList<String> rowData = new ArrayList<>();

            // put rows of data into arraylist
            while(fileReader.hasNextLine())
                rowData.add(fileReader.nextLine());

            String[][] data = new String[rowData.size()][];

            // split rows by ","
            for (int i = 0; i < rowData.size(); i++)
                data[i] = rowData.get(i).split(",");

            return data;
        }
        catch(IOException ioe) { return null; }
    }


    /**
     * Removes ID column from stringData
     * @param stringData String[][] representation of input data
     * @return String[][] representation of input data without ID column
     */
    private static String[][] purgeID(String[][] stringData)
    {
        String[][] noID = new String[stringData.length][stringData[0].length - 1];

        // copies every value except the last column
        for (int i = 0; i < stringData.length; i++)
        {
            if (stringData[0].length - 1 >= 0)
                System.arraycopy(stringData[i], 1, noID[i], 0, stringData[i].length - 1);
        }

        return noID;
    }


    /**
     * Removes class column from stringData
     * @param stringData String[][] representation of input data
     * @return String[][] representation of input data without class column
     */
    private static String[][] purgeClasses(String[][] stringData)
    {
        String[][] noClasses = new String[stringData.length][stringData[0].length - 1];

        // copies every value except the last column
        for (int i = 0; i < stringData.length; i++)
        {
            if (stringData[0].length - 1 >= 0)
                System.arraycopy(stringData[i], 0, noClasses[i], 0, stringData[i].length - 1);
        }

        return noClasses;
    }


    /**
     * Stores first row of dataset as field names
     * @param stringData String[][] representation of input data
     * @return ArrayList with field names
     */
    private static ArrayList<String> getFieldNames(String[][] stringData)
    {
        return new ArrayList<>(List.of(stringData[0]));
    }


    /**
     * Transforms string data into numerical data
     * @param stringData String[][] representation of input data
     * @return normalized input data
     */
    private static double[][] stringToNumerical(String[][] stringData)
    {
        // ArrayList to store numerical data
        ArrayList<ArrayList<Double>> numericalData = new ArrayList<>();

        for (int i = 0; i < stringData.length - 1; i++)
            numericalData.add(new ArrayList<>());

        // true if asked to keep invalid data
        boolean askedToKeep = false;

        // number of invalid data
        int invalids = 0;

        // first row is for field names: skip
        // convert every value in every row to double
        for (int i = 0; i < stringData.length - 1; i++)
        {
            for (int j = 0; j < stringData[i].length; j++)
            {
                // if value cannot become double then quit or remove row
                try
                {
                    numericalData.get(i-invalids).add(Double.parseDouble(stringData[i+1][j]));
                }
                catch (NumberFormatException nfe)
                {
                    if (!askedToKeep)
                    {
                        // inform user about invalid data
                        String message;

                        if(DV.hasID)
                        {
                            message = String.format("Invalid data found in column %d row %d.\n" +
                                    "Would you like to remove all rows with invalid data or cancel visualization?", j+2, i+2);
                        }
                        else
                        {
                            message = String.format("Invalid data found in column %d row %d.\n" +
                                    "Would you like to remove all rows with invalid data or cancel visualization?", j+1, i+2);
                        }

                        Object[] buttons = {"Remove invalids", "Cancel Visualization"};

                        int n = JOptionPane.showOptionDialog(DV.mainFrame,
                                message,
                                "Invalid Data Found",
                                JOptionPane.YES_NO_OPTION,
                                JOptionPane.ERROR_MESSAGE,
                                null,
                                buttons,
                                null);

                        // return if canceled
                        if(n == 1) return null;

                        // don't ask again
                        askedToKeep = true;
                    }

                    // remove invalid from allClasses
                    String invalidData = DV.allClasses.get(i - invalids);
                    DV.allClasses.remove(i - invalids);

                    // check if class still exists
                    for (int k = 0; k < DV.allClasses.size(); k++)
                    {
                        if (invalidData.equals(DV.allClasses.get(k)))
                            break;
                        else if (k == DV.allClasses.size() - 1)
                        {
                            DV.uniqueClasses.remove(invalidData);
                            DV.classNumber--;
                        }
                    }

                    // remove invalid ArrayList from numerical data
                    numericalData.remove(i - invalids);
                    invalids++;
                    break;
                }
            }
        }

        // convert numericalData to double[][]
        double[][] outputData = new double[numericalData.size()][];

        for (int i = 0; i < numericalData.size(); i++)
            outputData[i] = numericalData.get(i).stream().mapToDouble(m -> m).toArray();

        return outputData;
    }


    /**
     * Normalized input data with Min-Max
     * or z-Score Min-Max normalization
     * @param data input data
     * @return normalized input data
     */
    private static double[][] normalizeData(double[][] data)
    {
        // min and max values of each column
        double[] maxValues = new double[data[0].length];
        double[] minValues = new double[data[0].length];

        // do z-Score Min-Max or Min-Max normalization
        if (DV.zScoreMinMax)
        {
            // mean and standard deviation per column
            double[] mean = new double[data[0].length];
            double[] sd = new double[data[0].length];

            // get mean for each column
            for (int i = 0; i < data[0].length; i++)
            {
                for (double[] dataPoint : data)
                    mean[i] += dataPoint[i];

                mean[i] /= data.length;
            }

            // get standard deviation for each column
            for (int i = 0; i < data[0].length; i++)
            {
                for (double[] dataPoint : data)
                    sd[i] += Math.pow(dataPoint[i] - mean[i], 2);

                sd[i] = Math.sqrt(sd[i] / data.length);

                if (sd[i] < 0.001)
                {
                    String message = String.format("""
                            Standard deviation in column %d is less than 0.001.
                            Please manually enter a minimum and maximum. The standard deviation will get(max - min) / 2.
                            Else, the standard deviation will get 0.001.""", i+1);

                    // ask user for manual min max entry
                    double[] manualMinMax = manualMinMaxEntry(message);

                    // use manual min max or default to 0.001 if null
                    if (manualMinMax != null)
                        sd[i] = (manualMinMax[1] / manualMinMax[0]) / 2;
                    else
                        sd[i] = 0.001;
                }
            }

            // get min and max values
            for (int i = 0; i < data[0].length; i++)
            {
                // perform z-Score then set max and min
                maxValues[i] = (data[i][0] - mean[i]) / sd[i];
                minValues[i] = (data[i][0] - mean[i]) / sd[i];

                for (int j = 1; j < data.length; j++)
                {
                    // z-Score of value
                    double tmpValue = (data[j][i] - mean[i]) / sd[i];

                    // check for better max or min
                    if (tmpValue > maxValues[i])
                        maxValues[i] = tmpValue;
                    else if (tmpValue < minValues[i])
                        minValues[i] = tmpValue;
                }
            }
        }
        else
        {
            // get min and max values
            for (int i = 0; i < data[0].length; i++)
            {
                // set max and min
                maxValues[i] = data[i][0];
                minValues[i] = data[i][0];

                for (int j = 1; j < data.length; j++)
                {
                    // check for better max or min
                    if (data[j][i] > maxValues[i])
                        maxValues[i] = data[j][i];
                    else if (data[j][i] < minValues[i])
                        minValues[i] = data[j][i];
                }
            }
        }

        for (int i = 0; i < data[0].length; i++)
        {
            if (maxValues[i] != minValues[i])
            {
                for (int j = 0; j < data.length; j++)
                    data[j][i] = (data[j][i] - minValues[i]) / (maxValues[i] - minValues[i]);
            }
            else
            {
                String message = String.format("Minimum and maximum values in column %d are the same.\n" +
                        "Would you like to manually enter a minimum and maximum or default all values to 0.5?", i+1);

                // ask user for manual min max entry
                double[] manualMinMax = manualMinMaxEntry(message);

                // use manual min max or default to 0.5 if null
                if (manualMinMax != null)
                {
                    for (int j = 0; j < data.length; j++)
                        data[j][i] = (data[j][i] - manualMinMax[0]) / (manualMinMax[1] - manualMinMax[0]);
                }
                else
                {
                    for (int j = 0; j < data.length; j++)
                        data[j][i] = 0.5;
                }
            }
        }

        return data;
    }


    /**
     * Separates data by class
     * @param data data to be separated
     * @return ArrayList of separate class data
     */
    private static ArrayList<double[][]> separateByClass(double[][] data)
    {
        // output ArrayList
        ArrayList<double[][]> separatedClasses = new ArrayList<>();

        // holds arraylists holding data-points for each class
        ArrayList<ArrayList<double[]>> divider = new ArrayList<>();

        for (int i = 0; i < DV.classNumber; i++)
            divider.add(new ArrayList<>());

        // check every class
        for (int i = 0; i < DV.allClasses.size(); i ++)
        {
            // find matching class
            for (String str : DV.uniqueClasses)
            {
                if (str.equals(DV.allClasses.get(i)))
                {
                    divider.get(DV.uniqueClasses.indexOf(str)).add(data[i]);
                    break;
                }
            }
        }

        // transform to double[][]
        for (ArrayList<double[]> separateClass : divider)
            separatedClasses.add(separateClass.toArray(new double[separateClass.size()][]));

        return separatedClasses;
    }


    /**
     * Transforms ArrayList<double[][]> into DataObjects
     * @param separateClasses ArrayList to transform into DataObjects
     * @return ArrayList of DataObjects
     */
    private static ArrayList<DataObject> createDataObjects(ArrayList<double[][]> separateClasses)
    {
        // get classes and output arraylist
        ArrayList<DataObject> dataObjects = new ArrayList<>();

        // creates DataObject with class name and data
        for (int i = 0; i < separateClasses.size(); i++)
            dataObjects.add(new DataObject(DV.uniqueClasses.get(i), separateClasses.get(i)));

        return dataObjects;
    }


    /**
     * Gets user entry for min and max values
     * @param message Message to display to users
     * @return min and max values
     */
    private static double[] manualMinMaxEntry(String message)
    {
        // ask user for manual entry or default column to 0.5
        Object[] options = { "Manual Entry", "Default to 0.5" };

        int choice = JOptionPane.showOptionDialog(
                DV.mainFrame,
                message,
                "Numerical Error",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.ERROR_MESSAGE,
                null,
                options,
                null);

        // choice == manual entry
        if (choice == 0)
        {
            // popup asking for min and max input
            JPanel minMaxPanel = new JPanel(new BorderLayout());
            minMaxPanel.add(new JLabel("Enter a Minimum and Maximum value or cancel to default to 0.5."), BorderLayout.NORTH);

            // text panel
            JPanel textPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

            // maximum text field
            JTextField maxField = new JTextField();
            maxField.setPreferredSize(new Dimension(30, 30));
            textPanel.add(new JLabel("Maximum"));
            textPanel.add(maxField);

            // minimum text field
            JTextField minField = new JTextField();
            minField.setPreferredSize(new Dimension(30, 30));
            textPanel.add(new JLabel("Minimum"));
            textPanel.add(minField);

            // add text panel
            minMaxPanel.add(textPanel, BorderLayout.SOUTH);

            // loop until min and max are valid or user quits
            while (true)
            {
                choice = JOptionPane.showConfirmDialog(DV.mainFrame, minMaxPanel, "Enter Minimum and Maximum", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

                if (choice == 0)
                {
                    try
                    {
                        // get text field values
                        double max = Double.parseDouble(maxField.getText());
                        double min = Double.parseDouble(minField.getText());

                        if (min < max)
                        {
                            // return min and max
                            return new double[] { min, max };
                        }
                        else
                        {
                            // min is greater than or equal to max
                            JOptionPane.showMessageDialog(
                                    DV.mainFrame,
                                    "Error: minimum is greater than or equal to maximum.\n" +
                                            "Please ensure the minimum is less than the maximum.",
                                    "Error",
                                    JOptionPane.ERROR_MESSAGE);
                        }
                    }
                    catch (NumberFormatException nfe)
                    {
                        JOptionPane.showMessageDialog(DV.mainFrame, "Error: please enter numerical values.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
                else
                {
                    return null;
                }
            }
        }
        else
        {
            return null;
        }
    }
}
