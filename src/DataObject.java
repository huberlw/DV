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
     * @param angles weights for each value in a datapoint
     * @return highest point in DataObject
     */
    public double updateCoordinatesGLC(double[] angles)
    {
        // save the highest point
        double highest = Double.MIN_VALUE;

        // generate coordinates for every datapoint
        for (int i = 0; i < data.length; i++)
        {
            coordinates[i] = generateCoordinatesGLC(data[i], angles);

            // check for new highest
            if (coordinates[i][coordinates[i].length-1][0] > highest)
                highest = coordinates[i][coordinates[i].length-1][1];
            else if (coordinates[i][coordinates[i].length-1][1] > highest)
                highest = coordinates[i][coordinates[i].length-1][1];
        }

        // get vertical scaling
        double vertical_scale = 0.4;

        if (DV.classNumber == 1)
            vertical_scale *= 2;

        // only save the highest if greater than field length * vertical_scale
        if (highest < DV.fieldLength * vertical_scale)
            highest = 1;
        else
            highest = (highest + 0.1) / (DV.fieldLength * vertical_scale);

        if (highest > DV.fieldLength)
            DV.domainArea = new double[]{ -highest, highest };

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
        // output points
        double[][] xyPoints = new double[dataPoint.length][2];

        // get xyPoints
        xyPoints[0] = getXYPointGLC(dataPoint[0], angles[0]);

        for (int i = 1; i < dataPoint.length; i++)
        {
            xyPoints[i] = getXYPointGLC(dataPoint[i], angles[i]);

            // add previous points to current points
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
    private double[] getXYPointGLC(double value, double angle)
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


    private int[] generateDSCAngles(double[] datapoint, double[] cur_angles)
    {
        int[] angles = new int[(int) Math.ceil(datapoint.length / 2.0)];

        // get angles
        for (int i = 0, cnt = 0; i < datapoint.length; i++, cnt++)
        {
            double value1 = datapoint[i];
            double value2;

            if (++i != datapoint.length)
                value2 = datapoint[i];
            else
                value2 = datapoint[--i];

            if (value2 == 0)
                angles[cnt] = 90;
            else if (value1 == 0)
                angles[cnt] = 0;
            else
                angles[cnt] = (int) Math.round(Math.toDegrees(Math.atan(value1 / value2)));

            angles[cnt] += cur_angles[cnt];
        }

        return angles;
    }


    public double updateCoordinatesDSC(double[] angles)
    {
        // save the highest point
        double highest = Double.MIN_VALUE;

        // generate coordinates for every datapoint
        for (int i = 0; i < data.length; i++)
        {
            double[] datapoint;

            if (i != data.length - 2 || data[i].length % 2 == 0)
                datapoint = data[i];
            else
            {
                datapoint = new double[data[i].length+1];
                System.arraycopy(data[i], 0, datapoint, 0, data[i].length);
                datapoint[data[i].length] = data[i][data[i].length-1];
            }

            coordinates[i] = generateCoordinatesDSC(datapoint, generateDSCAngles(datapoint, angles));

            // check for new highest
            if (coordinates[i][coordinates[i].length-1][0] > highest)
                highest = coordinates[i][coordinates[i].length-1][1];
            else if (coordinates[i][coordinates[i].length-1][1] > highest)
                highest = coordinates[i][coordinates[i].length-1][1];
        }

        // get vertical scaling
        double vertical_scale = 0.4;

        if (DV.classNumber == 1)
            vertical_scale *= 2;

        // only save the highest if greater than field length * vertical_scale
        if (highest < DV.fieldLength * vertical_scale)
            highest = 1;
        else
            highest = (highest + 0.1) / (DV.fieldLength * vertical_scale);

        if (highest > DV.fieldLength)
            DV.domainArea = new double[]{ -highest, highest };

        return highest;
    }

    private double[][] generateCoordinatesDSC(double[] dataPoint, int[] angles)
    {
        // output points
        double[][] xyPoints = new double[(int)Math.ceil(dataPoint.length / 2.0)][2];

        // get xyPoints
        xyPoints[0] = getXYPointDSC(dataPoint[0], dataPoint[1], angles[0]);

        for (int i = 2, cnt = 1; i < dataPoint.length; i++, cnt++)
        {
            if (i + 1 != dataPoint.length)
                xyPoints[cnt] = getXYPointDSC(dataPoint[i], dataPoint[++i], angles[cnt]);
            else
                xyPoints[cnt] = getXYPointDSC(dataPoint[i], dataPoint[i], angles[cnt]);

            // add previous points to current points
            xyPoints[cnt][0] += xyPoints[cnt-1][0];
            xyPoints[cnt][1] += xyPoints[cnt-1][1];
        }

        return xyPoints;
    }

    private double[] getXYPointDSC(double value1, double value2, int angle)
    {
        double[] xyPoint = new double[2];

        double value = Math.sqrt(Math.pow(value1, 2) + Math.pow(value2, 2));

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
