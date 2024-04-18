import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CSV
{
    // exception logger
    private final static Logger LOGGER = Logger.getLogger(Analytics.class.getName());

    /**
     * Creates CSV file representing specified data from
     * the upper class as class 1 and lower graph as class 2
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
                    out.write("feature,");
                else
                    out.write("feature,class\n");
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
     * Creates CSV file representing specified data from
     * the upper class as class 1 and lower graph as class 2
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
                    out.write("feature,");
                else
                    out.write("feature,class\n");
            }

            // check all classes
            for (int i = 0; i < data.size(); i++)
            {
                for (int j = 0; j < data.get(i).data.length; j++)
                {
                    for (int k = 0; k < DV.fieldLength; k++)
                    {
                        if (k != DV.fieldLength - 1)
                            out.write(String.format("%f,", data.get(i).data[j][k]));
                        else
                            out.write(String.format("%f," + i + "\n", data.get(i).data[j][k]));
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
}
