import java.util.ArrayList;

public class HyperBlock
{
    // all hyperblocks
    ArrayList<ArrayList<double[]>> hyper_block;

    // class number of hyperblock
    int classNum;

    // number of datapoints in hyperblock
    int size;

    // class name of hyperblock
    String className;

    // seed attribute of hyperblocks created with IHyper algorithm
    String attribute;

    // minimums and maximums for each feature in hyperblock
    ArrayList<double[]> maximums;
    ArrayList<double[]> minimums;


    /**
     * Constructor for HyperBlock
     * @param hyper_block datapoints to go in hyperblock
     */
    HyperBlock(ArrayList<ArrayList<double[]>> hyper_block, int classNum)
    {
        this.hyper_block = hyper_block;

        this.maximums = new ArrayList<>();
        this.minimums = new ArrayList<>();
        this.classNum = classNum;

        findBounds();
    }


    /**
     * Constructor for HyperBlock
     * @param hyper_block datapoints to go in hyperblock
     */
    HyperBlock(ArrayList<ArrayList<double[]>> hyper_block)
    {
        this.hyper_block = hyper_block;

        this.maximums = new ArrayList<>();
        this.minimums = new ArrayList<>();

        findBounds();
    }


    /**
     * Constructor for HyperBlock
     * @param max maximum bound for hyperblock
     * @param min minimum bound for hyperblock
     */
    HyperBlock(double[] max, double[] min, int classNum)
    {
        setBounds(max, min);
        findData();
        this.classNum = classNum;
    }


    /**
     * Constructor for HyperBlock
     * @param max maximum bound for hyperblock
     * @param min minimum bound for hyperblock
     */
    HyperBlock(double[] max, double[] min)
    {
        setBounds(max, min);
        findData();
    }


    /**
     * Sets minimum and maximum bound for a hyperblock
     * @param max maximum bound
     * @param min minimum bound
     */
    private void setBounds(double[] max, double[] min)
    {
        maximums = new ArrayList<>();
        minimums = new ArrayList<>();

        maximums.add(max);
        minimums.add(min);
    }


    /**
     * Finds all data within a hyperblock
     */
    private void findData()
    {
        ArrayList<ArrayList<double[]>> dps = new ArrayList<>();
        ArrayList<double[]> classPnts = new ArrayList<>();
        for (int i = 0; i < DV.trainData.size(); i++)
        {
            for (int j = 0; j < DV.trainData.get(i).data.length; j++)
            {
                for (int q = 0; q < maximums.size(); q++)
                {
                    boolean inside = true;
                    for (int k = 0; k < DV.fieldLength; k++)
                    {
                        if (DV.trainData.get(i).data[j][k] > maximums.get(q)[k] || DV.trainData.get(i).data[j][k] < minimums.get(q)[k])
                        {
                            inside = false;
                            break;
                        }
                    }

                    if (inside)
                        classPnts.add(DV.trainData.get(i).data[j]);
                }
            }
        }

        dps.add(classPnts);
        hyper_block = dps;
    }


    /**
     * Gets minimums and maximums of a hyperblock
     */
    public void findBounds()
    {
        size = 0;
        maximums.clear();
        minimums.clear();

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


            size += hyper_block.get(h).size();
        }
    }
}
