public class DataObject
{
    // class name and data
    String className;
    double[][] data;

    // x and y points of data for graphing in GLC-L
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
     * @param angles weights for each value in a datapoint
     * @return highest point in DataObject
     */
    public double updateCoordinatesGLC(double[] angles)
    {
        // generate coordinates for every datapoint
        // save the highest x/y value
        double highest = Double.MIN_VALUE;
        for (int i = 0; i < data.length; i++)
        {
            coordinates[i] = generateCoordinatesGLC(data[i], angles);

            // check for new highest x/y value
            if (coordinates[i][coordinates[i].length-1][0] > highest)
                highest = coordinates[i][coordinates[i].length-1][0];
            else if (coordinates[i][coordinates[i].length-1][1] > highest)
                highest = coordinates[i][coordinates[i].length-1][1];
        }

        // get vertical scaling
        highest = getScaling(highest);

        return highest;
    }


    /**
     * Generates coordinates for a datapoint
     * @param dataPoint datapoint in DataObject
     * @param angles weights for each value
     * @return coordinates for datapoint
     */
    private double[][] generateCoordinatesGLC(double[] dataPoint, double[] angles)
    {
        // get xyPoints
        double[][] xyPoints = new double[dataPoint.length][2];
        for (int i = 0; i < dataPoint.length; i++)
        {
            if (DV.activeAttributes.get(i))
                xyPoints[i] = getXYPointGLC(dataPoint[i], angles[i]);
            else
                xyPoints[i] = new double[]{0, 0};
        }

        // append points one after the other
        for (int i = 1; i < xyPoints.length; i++)
        {
            xyPoints[i][0] += xyPoints[i-1][0];
            xyPoints[i][1] += xyPoints[i-1][1];
        }

        return xyPoints;
    }


    /**
     * Gets coordinates for a value
     * @param value value to find coordinates for
     * @param angle weight of value
     * @return coordinates for value
     */
    public static double[] getXYPointGLC(double value, double angle)
    {
        double[] xyPoint = new double[2];

        // reverse direction if angle is greater than 90 degrees
        // calculate x
        if (angle > 90)
            xyPoint[0] = -(Math.cos(Math.toRadians(180 - angle)) * value);
        else
            xyPoint[0] = Math.cos(Math.toRadians(angle)) * value;

        xyPoint[1] = Math.sin(Math.toRadians(angle)) * value;

        return xyPoint;
    }


    /**
     * Gets the vertical scaling for the graph
     * @param highest highest x/y value in DataObject
     * @return vertical scaling
     */
    private double getScaling(double highest)
    {
        // add 10% buffer to visualization
        highest += DV.fieldLength / 10.0;

        // if there is only 1 class (and therefore 1 graph) then double the vertical size
        double vertical_scale = (DV.mainPanel.getHeight() * 0.7) / (DV.graphPanel.getWidth() * 0.8);
        if (DV.classNumber == 1)
            vertical_scale *= 2;

        // only save the highest if greater than field length * vertical_scale
        if (highest < DV.fieldLength * vertical_scale)
            highest = 1;
        else
            highest = (highest + 0.1) / (DV.fieldLength * vertical_scale);

        // set domain area
        if (highest > DV.fieldLength)
            DV.domainArea = new double[]{ -highest, highest };

        return highest;
    }
}
