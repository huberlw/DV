import java.util.ArrayList;

public class HyperBlock
{
    ArrayList<ArrayList<double[]>> hyper_block;
    int classNum;
    String className;
    String attribute;

    ArrayList<double[]> maximums;
    ArrayList<double[]> minimums;

    // create hyperblock
    HyperBlock(ArrayList<ArrayList<double[]>> hyper_block)
    {
        this.hyper_block = hyper_block;

        maximums = new ArrayList<>();
        minimums = new ArrayList<>();

        getBounds();
    }


    public void getBounds()
    {
        for (int h = 0; h < hyper_block.size(); h++)
        {
            maximums.add(new double[DV.fieldLength]);
            minimums.add(new double[DV.fieldLength]);

            for (int i = 0; i < DV.fieldLength; i++)
            {
                maximums.get(h)[i] = -Double.MIN_VALUE;
                minimums.get(h)[i] = Double.MAX_VALUE;
            }

            for (double[] dbs : hyper_block.get(h))
            {
                for (int j = 0; j < DV.fieldLength; j++)
                {
                    maximums.get(h)[j] = Math.max(maximums.get(h)[j], dbs[j]);
                    minimums.get(h)[j] = Math.min(minimums.get(h)[j], dbs[j]);
                }
            }
        }
    }
}
