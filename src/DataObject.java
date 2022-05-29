public class DataObject
{
    // class name and data
    String className;
    double[][] data;

    // x and y points of data for graphing
    double[][][] coordinates;


    /**
     * Constructor for DataObject
     * @param name class name for data
     * @param dataValues data for class
     */
    public DataObject(String name, double[][] dataValues)
    {
        className = name;
        data = dataValues;
        coordinates = new double[data.length][][];
    }


    /**
     * Updates the coordinates for each datapoint in a DataObject
     * @return highest point in DataObject
     */
    public double updateCoordinates()
    {
        // save the highest point
        double highest = Double.MIN_VALUE;

        // generate coordinates for every datapoint
        for (int i = 0; i < data.length; i++)
        {
            coordinates[i] = generateCoordinates(data[i]);

            // check for new highest
            if (coordinates[i][DV.fieldLength-1][1] > highest)
                highest = coordinates[i][DV.fieldLength-1][1];
        }

        // only save the highest if greater than field length * 0.4
        if (highest < DV.fieldLength * 0.4)
            highest = 1;
        else
            highest = (highest + 0.1) / (DV.fieldLength * 0.4);

        return highest;
    }


    /**
     * Generates coordinates for a datapoint
     * @param dataPoint datapoint in DataObject
     * @return coordinates for datapoint
     */
    private double[][] generateCoordinates(double[] dataPoint)
    {
        // output points
        double[][] xyPoints = new double[dataPoint.length][2];

        // get xyPoints
        xyPoints[0] = getXYPoint(dataPoint[0], DV.angles[0]);

        for (int i = 1; i < dataPoint.length; i++)
        {
            xyPoints[i] = getXYPoint(dataPoint[i], DV.angles[i]);

            // add previous points to current points
            xyPoints[i][0] += xyPoints[i-1][0];
            xyPoints[i][1] += xyPoints[i-1][1];
        }

        return xyPoints;
    }


    /**
     * Gets coordinates for a value
     * @param value value to find coordinates for
     * @param angle direction of value
     * @return coordinates for value
     */
    private double[] getXYPoint(double value, double angle)
    {
        double[] xyPoint = new double[2];

        // reverse direction if angle is greater than 90 degrees
        // calculate x
        if (angle > 90)
        {
            angle = 180 - angle;
            xyPoint[0] = -(Math.cos(Math.toRadians(angle)) * value);
        }
        else
            xyPoint[0] = (Math.cos(Math.toRadians(angle))) * value;

        // calculate y
        xyPoint[1] = Math.sin(Math.toRadians(angle)) * value;

        return xyPoint;
    }
}
