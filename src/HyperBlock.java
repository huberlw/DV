import java.util.ArrayList;

public class HyperBlock
{
    ArrayList<double[]> hyper_block;
    double radius;
    String className;

    double[] maximums;
    double[] minimums;

    // create hyperblock
    HyperBlock(ArrayList<double[]> hyper_block, double radius)
    {
        this.hyper_block = hyper_block;
        this.radius = radius;

        maximums = new double[DV.fieldLength];
        minimums = new double[DV.fieldLength];

        getBounds();
    }


    public void getBounds()
    {
        for (int i = 0; i < DV.fieldLength; i++)
        {
            maximums[i] = Double.MIN_VALUE;
            minimums[i] = Double.MAX_VALUE;
        }

        for (double[] dbs : hyper_block)
        {
            for (int j = 0; j < DV.fieldLength; j++)
            {
                maximums[j] = Math.max(maximums[j], dbs[j]);
                minimums[j] = Math.min(minimums[j], dbs[j]);
            }
        }
    }
}
