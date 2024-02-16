import org.jfree.chart.*;
import org.jfree.data.xy.XYSeries;

import javax.swing.*;
import java.util.*;
import java.util.List;

import static java.lang.Math.max;
import static java.util.Arrays.sort;

public class HyperBlockGeneration
{
    // DO int CURRENT LEVEL
    // DO OBJECTS.GET(CURRENT LEVEL)
    // DO HYPERBLOCKS.GET(CURRENT LEVEL)

    //int HB_LVL = 0;
    //ArrayList<ArrayList<ArrayList<DataObject>>> objects;
    //ArrayList<ArrayList<HyperBlock>> hyperblocks = new ArrayList<>();
    int original_num = 0;

    ArrayList<ArrayList<DataObject>> objects;
    ArrayList<DataObject> lowerObjects;
    ArrayList<DataObject> upperObjects;
    ArrayList<ArrayList<DataObject>> originalObjects;
    ArrayList<HyperBlock> originalHyperBlocks;

    // hyperblock storage
    ArrayList<HyperBlock> hyper_blocks = new ArrayList<>();
    ArrayList<HyperBlock> test_hyper_blocks = new ArrayList<>();
    ArrayList<String> accuracy = new ArrayList<>();

    // refuse
    ArrayList<double[]> refuse_area = new ArrayList<>();

    // artificial datapoints
    ArrayList<ArrayList<double[]>> artificial = new ArrayList<>();
    ArrayList<Double> acc = new ArrayList<>();
    ArrayList<Integer> misclassified = new ArrayList<>();
    ArrayList<Integer> block_size = new ArrayList<>();

    int totalBlockCnt;
    int overlappingBlockCnt;

    int new_case = 0;

    /************************************************************
     * General Line Coordinates Hyperblock Rules Linear (GLC-HBRL)
     ***********************************************************
     */
    // Accuracy threshold for hyperblocks
    double acc_threshold = 100;


    HyperBlockGeneration()
    {
        // put in list and sort
        // get largest interval
        // continue until no more intervals
        // do dustin algorithm
        getData();
        generateHyperblocks();
        hb_test();

        // visualize blocks
        // save blocks
    }

    private void generateHyperblocks()
    {
        // create hyperblocks
        hyper_blocks.clear();

        ArrayList<ArrayList<DataATTR>> attributes = new ArrayList<>();

        for (int k = 0; k < DV.fieldLength; k++)
        {
            ArrayList<DataATTR> tmpField = new ArrayList<>();

            for (int i = 0; i < DV.data.size(); i++)
            {
                for (int j = 0; j < DV.data.get(i).data.length; j++)
                {
                    tmpField.add(new DataATTR(DV.data.get(i).data[j][k], i, j));
                }
            }

            attributes.add(tmpField);
        }

        // Hyperblocks generated with this algorithm
        ArrayList<HyperBlock> gen_hb = new ArrayList<>();

        /***
         * DELETE THIS LATER
         * THIS IS JUST TO CREATE A SET OF DATA THAT IS NOT IN ANY INTERVAL HYPERBLOCK
         */
        ArrayList<ArrayList<DataATTR>> all_intv = new ArrayList<>();

        while (!attributes.get(0).isEmpty())
        {
            // get largest hyperblock for each attribute
            ArrayList<DataATTR> intv = interval_hyper(attributes, acc_threshold, gen_hb);
            all_intv.add(intv);

            // if Hyperblock is unique then add
            if (intv.size() > 1)
            {
                // Create and add new Hyperblock
                ArrayList<ArrayList<double[]>> hb_data = new ArrayList<>();
                ArrayList<double[]> intv_data = new ArrayList<>();

                for (int i = 0; i < intv.size(); i++)
                {
                    int cl = intv.get(i).cl;
                    int cl_index = intv.get(i).cl_index;

                    intv_data.add(DV.data.get(cl).data[cl_index]);
                }

                // Add data and Hyperblock
                hb_data.add(intv_data);
                HyperBlock tmp = new HyperBlock(hb_data);

                /***
                 * CHANGE THIS LATER TO VOTING
                 */
                tmp.classNum = intv.get(0).cl;


                gen_hb.add(tmp);

            }
            // break loop if a unique HB cannot be found
            else
            {
                break;
            }
        }

        /***
         * DELETE THIS LATER
         * THIS IS JUST TO CREATE A SET OF DATA THAT IS NOT IN ANY INTERVAL HYPERBLOCK
         */
        // Create dataset without data from interval Hyperblocks
        ArrayList<ArrayList<double[]>> data = new ArrayList<>();
        ArrayList<ArrayList<double[]>> out_data = new ArrayList<>();
        ArrayList<ArrayList<Integer>> skips = new ArrayList<>();

        // all data
        for (int i = 0; i < DV.data.size(); i++)
        {
            data.add(new ArrayList<>(Arrays.asList(DV.data.get(i).data)));
            out_data.add(new ArrayList<>());
            skips.add(new ArrayList<>());
        }

        // find which data to skip
        for (int i = 0; i < all_intv.size(); i++)
        {
            for (int j = 0; j < all_intv.get(i).size(); j++)
            {
                int cl = all_intv.get(i).get(j).cl;
                int cl_index = all_intv.get(i).get(j).cl_index;

                skips.get(cl).add(cl_index);
            }
        }

        for (int i = 0; i < skips.size(); i++)
            Collections.sort(skips.get(i));

        for (int i = 0; i < DV.data.size(); i++)
        {
            for (int j = 0; j < DV.data.get(i).data.length; j++)
            {
                if (!skips.get(i).isEmpty())
                {
                    if (j != skips.get(i).get(0))
                    {
                        out_data.get(i).add(DV.data.get(i).data[j]);
                    }
                    else
                    {
                        skips.get(i).remove(0);
                    }
                }
                else
                {
                    out_data.get(i).add(DV.data.get(i).data[j]);
                }
            }
        }

        // run dustin algorithm
        hyper_blocks.addAll(gen_hb);
        merger_hyperblock(data, out_data);

        // reorder blocks
        ArrayList<HyperBlock> upper = new ArrayList<>();
        ArrayList<HyperBlock> lower = new ArrayList<>();

        for (HyperBlock hyperBlock : hyper_blocks)
        {
            if (hyperBlock.classNum == DV.upperClass)
                upper.add(hyperBlock);
            else
                lower.add(hyperBlock);
        }

        upper.sort(new BlockComparator());
        lower.sort(new BlockComparator());

        hyper_blocks.clear();
        hyper_blocks.addAll(upper);
        hyper_blocks.addAll(lower);
    }


    /**
     * Data Attribute. Stores one attribute of a datapoint and an identifying key shared with other attributes of the same datapoint.
     * @param value value of one attribute of a datapoint
     * @param cl class of a datapoint
     * @param cl_index index of point within class
     */
    private record DataATTR(double value, int cl, int cl_index) {}


    /**
     * Finds largest interval across all dimensions of a set of data.
     * @param data_by_attr all data split by attribute
     * @param acc_threshold accuracy threshold for interval
     * @param existing_hb existing hyperblocks to check for overlap
     * @return largest interval
     */
    private ArrayList<DataATTR> interval_hyper(ArrayList<ArrayList<DataATTR>> data_by_attr, double acc_threshold, ArrayList<HyperBlock> existing_hb)
    {
        int attr = -1;
        int[] best = {-1, -1, -1};

        // search each attribute
        for (int i = 0; i < data_by_attr.size(); i++)
        {
            // sort data by value
            data_by_attr.get(i).sort(Comparator.comparingDouble(o -> o.value));

            // get longest interval for attribute
            int[] interval = longest_interval(data_by_attr.get(i), acc_threshold, existing_hb, i);

            if (interval[0] > 1 && interval[0] > best[0])
            {
                attr = i;
                best[0] = interval[0];
                best[1] = interval[1];
                best[2] = interval[2];
            }
        }

        // construct ArrayList of data
        ArrayList<DataATTR> longest = new ArrayList<>();

        if (best[0] != -1)
        {
            for (int i = best[1]; i <= best[2]; i++)
            {
                longest.add(data_by_attr.get(attr).get(i));
            }
        }

        return longest;

        /*// Current largest interval for each attribute
        ArrayList<ArrayList<DataATTR>> global_interval = new ArrayList<>();

        // loop through each attribute
        for (int a = 0; a < data_by_attr.size(); a++)
        {
            // create empty interval, establish starting point, and count misclassified points within the interval
            int start = 0;
            int misclassified = 0;
            global_interval.add(new ArrayList<>());

            // create local interval to compare to global
            ArrayList<DataATTR> local_interval = new ArrayList<>();
            boolean local_is_global = false;

            // loop through each datapoint
            for (int i = 0; i < data_by_attr.get(a).size(); i++)
            {
                // add datapoint to interval if classes are the same
                if (data_by_attr.get(a).get(start).cl == data_by_attr.get(a).get(i).cl)
                {
                    local_interval.add(data_by_attr.get(a).get(i));

                    // check if the local interval is greater than the global interval
                    if (local_interval.size() > global_interval.get(a).size())
                    {
                        global_interval.set(a, new ArrayList<>(local_interval));
                        local_is_global = true;
                    }
                    else
                    {
                        local_is_global = false;
                    }
                }
                else if (((double) local_interval.size() / (misclassified + local_interval.size() + 1)) >= acc_threshold)
                {
                    misclassified++;

                    // add datapoint to interval if accuracy is above threshold
                    local_interval.add(data_by_attr.get(a).get(i));

                    // check if the local interval is greater than the global interval
                    if (local_interval.size() > global_interval.get(a).size())
                    {
                        global_interval.set(a, new ArrayList<>(local_interval));
                        local_is_global = true;
                    }
                    else
                    {
                        local_is_global = false;
                    }
                }
                else
                {
                    // classes are different, but the value is still the same
                    // and accuracy is below threshold
                    if (data_by_attr.get(a).get(i-1).value == data_by_attr.get(a).get(i).value)
                    {
                        // value to remove from interval
                        double remove_val = data_by_attr.get(a).get(i).value;

                        remove_interval_value(remove_val, local_interval);

                        // remove value from answer if applicable
                        if (local_is_global)
                        {
                            remove_interval_value(remove_val, global_interval.get(a));
                        }

                        // if all values were removed then set start to after the removed values
                        // else set start to the beginning of removed values
                        if (local_interval.isEmpty())
                        {
                            // increment until value changes
                            while (remove_val == data_by_attr.get(a).get(i).value)
                            {
                                if (i < data_by_attr.get(a).size()-1)
                                    i++;
                                else
                                    break;
                            }
                        }
                        else
                        {
                            // decrement until value changes
                            while (remove_val == data_by_attr.get(a).get(i).value)
                            {
                                if (i > 0)
                                    i--;
                                else
                                    break;
                            }

                            // increment so value remains the same
                            if (remove_val != data_by_attr.get(a).get(i).value)
                                i++;
                        }
                    }

                    // check if interval overlaps with existing hyperblocks
                    if (local_is_global && check_interval_hyperblock_overlap(global_interval.get(a), existing_hb, a))
                    {
                        global_interval.get(a).clear();
                    }

                    // reset variables and continue searching
                    start = i;
                    misclassified = 0;
                    local_interval.clear();
                    local_is_global = false;
                }
            }

            // check if interval overlaps with existing hyperblocks
            if (local_is_global && check_interval_hyperblock_overlap(global_interval.get(a), existing_hb, a))
            {
                global_interval.get(a).clear();
            }
        }

        int biggest = 0;

        for (int i = 1; i < global_interval.size(); i++)
        {
            if (global_interval.get(biggest).size() < global_interval.get(i).size())
                biggest = i;
        }

        // add attribute and class
        global_interval.get(biggest).add(new DataATTR(-1, biggest, global_interval.get(biggest).get(0).cl));
        return global_interval.get(biggest);*/
    }


    private int[] longest_interval(ArrayList<DataATTR> data_by_attr, double acc_threshold, ArrayList<HyperBlock> existing_hb, int attr)
    {
        // intervals and size
        int[] intr = new int[] {1, 0, 0};
        int[] max_intr = new int[] {-1, -1, -1};
        int n = data_by_attr.size();

        for (int i = 0; i < n; i++)
        {
            // If current class matches with next
            if (i < n - 1 &&
                    data_by_attr.get(i + 1).cl == data_by_attr.get(i).cl)
            {
                intr[0]++;
            }
            else
            {
                // Remove value from interval if identical
                if (i < n - 1 && data_by_attr.get(i + 1).value == data_by_attr.get(i).value)
                {
                    intr = remove_value_from_interval(data_by_attr, intr, data_by_attr.get(i).value);
                    i = skip_value_in_interval(data_by_attr, i, data_by_attr.get(i).value);
                }

                // Update longest interval if it doesn't overlap
                if (intr[0] > max_intr[0] &&
                        check_interval_hyperblock_overlap(data_by_attr, intr, attr, existing_hb))
                {
                    max_intr[0] = intr[0];
                    max_intr[1] = intr[1];
                    max_intr[2] = intr[2];
                }

                // Reset current interval
                intr[0] = 1;
                intr[1] = i + 1;
            }

            intr[2] = i + 1;
        }

        // return largest interval
        return max_intr;
    }


    private int[] remove_value_from_interval(ArrayList<DataATTR> data_by_attr, int[] intr, double value)
    {
        while (data_by_attr.get(intr[2]).value == value)
        {
            if (intr[2] > 0)
            {
                intr[0]--;
                intr[2]--;
            }
            else if (intr[1] <= intr[2])
            {
                intr[0] = -1;
                intr[2] = intr[1];
                break;
            }
            else
            {
                intr[0] = -1;
                break;
            }
        }

        return new int[]{intr[0], intr[1], intr[2]};
    }

    private int skip_value_in_interval(ArrayList<DataATTR> data_by_attr, int index, double value)
    {
        while (data_by_attr.get(index).value == value)
        {
            if (index < data_by_attr.size() - 1)
            {
                index++;
            }
            else
            {
                return data_by_attr.size() - 2;
            }
        }

        return index - 1;
    }


    /**
     * Finds largest interval across one dimension of a set of data while mimicking a given Linear Discriminant Function (LDF).
     * Will ensure that datapoints classified correctly by a given LDF will also be classified correctly within the interval.
     * @param data_by_attr all data split by attribute
     * @param data_map map of all data
     * @param acc_threshold accuracy threshold for interval
     * @param existing_hb existing hyperblocks to check for overlap
     * @param LDF_misclassified datapoints misclassified by given LDF
     * @return largest interval
     */
    /*private ArrayList<DataATTR> interval_hyper_rules_linear(ArrayList<ArrayList<DataATTR>> data_by_attr, HashMap<Integer, double[]> data_map, double acc_threshold, ArrayList<ArrayList<ArrayList<DataATTR>>> existing_hb,  ArrayList<double[]> LDF_misclassified)
    {
        // Current largest interval for each attribute
        ArrayList<ArrayList<DataATTR>> global_largest = new ArrayList<>();

        // loop through each attribute
        for (int a = 0; a < data_by_attr.size(); a++)
        {
            // create empty interval and establish starting point
            global_largest.add(new ArrayList<>());
            int start = 0;
            double misclassified = 0;

            // create local interval to compare to global
            ArrayList<DataATTR> local_interval = new ArrayList<>();

            // loop through each datapoint
            for (int i = 0; i < data_by_attr.get(a).size(); i++)
            {
                // check if case is misclassified by the LDF
                boolean LDF_mc = false;

                // check full datapoint against all datapoints misclassified by the given LDF
                double[] full_pnt = data_map.get(data_by_attr.get(a).get(i).key);

                for (double[] LDF_check : LDF_misclassified)
                {
                    // check if datapoint is a misclassified case
                    if (Arrays.deepEquals(new Object[]{ LDF_check }, new Object[]{ full_pnt }))
                    {
                        LDF_mc = true;
                        break;
                    }
                }

                // add datapoint to local interval if classes are the same
                if (data_by_attr.get(a).get(start).cl == data_by_attr.get(a).get(i).cl)
                {
                    local_interval.add(data_by_attr.get(a).get(i));

                    // replace the global interval with the local interval if it is larger
                    if (local_interval.size() > global_largest.get(a).size())
                        global_largest.set(a, new ArrayList<>(local_interval));
                }
                else
                {
                    // add datapoint to local interval if accuracy is above a given threshold and the datapoint is also misclassified by the given LDF
                    if (LDF_mc && (local_interval.size() - misclassified - 1) / (local_interval.size()) >= acc_threshold)
                    {
                        // increase misclassified cases within interval
                        misclassified++;

                        local_interval.add(data_by_attr.get(a).get(i));

                        if (local_interval.size() > global_largest.get(a).size())
                            global_largest.set(a, new ArrayList<>(local_interval));
                    }
                    else if (i >= 1)
                    {
                        int cnt = 0;
                        // classes are different, but the value is still the same
                        if (data_by_attr.get(a).get(i-1).value == data_by_attr.get(a).get(i).value)
                        {
                            double remove_val = data_by_attr.get(a).get(i).value;

                            // increment until value changes
                            while (remove_val == data_by_attr.get(a).get(i).value)
                            {
                                if (i < data_by_attr.get(a).size()-1)
                                {
                                    i++;
                                    cnt++;
                                }
                                else
                                    break;
                            }

                            // remove value from answer
                            if (global_largest.get(a).size() > 0)
                            {
                                int offset = 0;
                                int size = global_largest.get(a).size();

                                // remove overlapped attributes
                                for (int k = 0; k < size; k++)
                                {
                                    if (global_largest.get(a).get(k - offset).value == remove_val)
                                    {
                                        global_largest.get(a).remove(k - offset);
                                        offset++;
                                    }
                                }
                            }
                            else
                            {
                                atri.set(a, -1);
                                cls.set(a, -1);
                            }
                        }

                        if (global_largest.get(a).size() > 0)
                        {
                            ArrayList<ArrayList<DataATTR>> block = new ArrayList<>();

                            for (int k = 0; k < DV.fieldLength; k++)
                                block.add(new ArrayList<>());

                            for (DataATTR pnt : global_largest.get(a))
                            {
                                for (int k = 0; k < data_by_attr.size(); k++)
                                {
                                    int index = pnt.key;

                                    for (int h = 0; h < data_by_attr.get(k).size(); h++)
                                    {
                                        if (index == data_by_attr.get(k).get(h).key)
                                        {
                                            // add point
                                            block.get(k).add(new DataATTR(index, data_by_attr.get(k).get(h).value));
                                            break;
                                        }
                                    }
                                }
                            }

                            for (ArrayList<DataATTR> blk : block)
                                blk.sort(Comparator.comparingDouble(o -> o.value));

                            for (ArrayList<ArrayList<DataATTR>> tmp_pnts : hb)
                            {
                                // check if interval overlaps with other hyperblocks
                                int non_unique = 0;

                                for (int k = 0; k < tmp_pnts.size(); k++)
                                {
                                    tmp_pnts.get(k).sort(Comparator.comparingDouble(o -> o.value));

                                    if (block.get(k).get(0).value >= tmp_pnts.get(k).get(0).value && block.get(k).get(0).value <= tmp_pnts.get(k).get(tmp_pnts.get(k).size() - 1).value)
                                    {
                                        non_unique++;
                                    }
                                    else if (block.get(k).get(block.get(k).size() - 1).value <= tmp_pnts.get(k).get(tmp_pnts.get(k).size() - 1).value && block.get(k).get(block.get(k).size() - 1).value >= tmp_pnts.get(k).get(0).value)
                                    {
                                        non_unique++;
                                    }
                                    else if (tmp_pnts.get(k).get(0).value >= block.get(k).get(0).value && tmp_pnts.get(k).get(0).value <= block.get(k).get(block.get(k).size() - 1).value)
                                    {
                                        non_unique++;
                                    }
                                    else if (tmp_pnts.get(k).get(tmp_pnts.get(k).size()-1).value <= block.get(k).get(block.get(k).size()-1).value && tmp_pnts.get(k).get(tmp_pnts.get(k).size()-1).value >= block.get(k).get(0).value)
                                    {
                                        non_unique++;
                                    }
                                    else
                                        break;
                                }

                                if (non_unique == tmp_pnts.size())
                                {
                                    global_largest.get(a).clear();
                                    break;
                                }
                            }
                        }

                        if (data_by_attr.get(a).get(i-1-cnt).value == data_by_attr.get(a).get(i-cnt).value)
                            start = i;
                        else
                        {
                            double remove_val = data_by_attr.get(a).get(i).value;

                            // increment until value changes
                            while (remove_val == data_by_attr.get(a).get(i).value)
                            {
                                if (i < data_by_attr.get(a).size()-1)
                                    i++;
                                else
                                    break;
                            }

                            start = i;
                        }
                        local_interval.clear();
                        misclassified = 0;
                    }
                    else
                    {
                        double remove_val = data_by_attr.get(a).get(i).value;

                        // increment until value changes
                        while (remove_val == data_by_attr.get(a).get(i).value)
                        {
                            if (i < data_by_attr.get(a).size()-1)
                                i++;
                            else
                                break;
                        }

                        start = i;
                        local_interval.clear();
                        misclassified = 0;
                    }
                }
            }

            if (global_largest.get(a).size() > 0)
            {
                ArrayList<ArrayList<DataATTR>> block = new ArrayList<>();

                for (int k = 0; k < DV.fieldLength; k++)
                    block.add(new ArrayList<>());

                for (DataATTR pnt : global_largest.get(a))
                {
                    for (int k = 0; k < data_by_attr.size(); k++)
                    {
                        int index = pnt.key;

                        for (int h = 0; h < data_by_attr.get(k).size(); h++)
                        {
                            if (index == data_by_attr.get(k).get(h).key)
                            {
                                // add point
                                block.get(k).add(new DataATTR(index, data_by_attr.get(k).get(h).value));
                                break;
                            }
                        }
                    }
                }

                for (ArrayList<DataATTR> blk : block)
                    blk.sort(Comparator.comparingDouble(o -> o.value));

                for (ArrayList<ArrayList<DataATTR>> tmp_pnts : hb)
                {
                    // check if interval overlaps with other hyperblocks
                    int non_unique = 0;

                    for (int k = 0; k < tmp_pnts.size(); k++)
                    {
                        tmp_pnts.get(k).sort(Comparator.comparingDouble(o -> o.value));

                        // search for overlap
                        if (block.get(k).get(0).value >= tmp_pnts.get(k).get(0).value && block.get(k).get(0).value <= tmp_pnts.get(k).get(tmp_pnts.get(k).size() - 1).value)
                        {
                            non_unique++;
                        }
                        else if (block.get(k).get(block.get(k).size() - 1).value <= tmp_pnts.get(k).get(tmp_pnts.get(k).size() - 1).value && block.get(k).get(block.get(k).size() - 1).value >= tmp_pnts.get(k).get(0).value)
                        {
                            non_unique++;
                        }
                        else if (tmp_pnts.get(k).get(0).value >= block.get(k).get(0).value && tmp_pnts.get(k).get(0).value <= block.get(k).get(block.get(k).size() - 1).value)
                        {
                            non_unique++;
                        }
                        else if (tmp_pnts.get(k).get(tmp_pnts.get(k).size()-1).value <= block.get(k).get(block.get(k).size()-1).value && tmp_pnts.get(k).get(tmp_pnts.get(k).size()-1).value >= block.get(k).get(0).value)
                        {
                            non_unique++;
                        }
                        else
                            break;
                    }

                    if (non_unique == tmp_pnts.size())
                    {
                        global_largest.get(a).clear();
                        break;
                    }
                }
            }
        }

        int big = 0;

        for (int i = 0; i < global_largest.size(); i++)
        {
            if (global_largest.get(big).size() < global_largest.get(i).size())
                big = i;
        }

        // add attribute and class
        global_largest.get(big).add(new DataATTR(atri.get(big), cls.get(big)));
        return global_largest.get(big);
    }*/


    /**
     * Checks if a given interval overlaps with any existing hyperblock.
     * @param data_by_attr data interval exists on
     * @param intv interval to check
     * @param attr attribute interval exists on
     * @param existing_hb all existing hyperblocks
     * @return whether the interval is unique or not
     */
    private boolean check_interval_hyperblock_overlap(ArrayList<DataATTR> data_by_attr, int[] intv,  int attr, ArrayList<HyperBlock> existing_hb)
    {
        // get interval range
        double intv_min = data_by_attr.get(intv[1]).value;
        double intv_max = data_by_attr.get(intv[2]).value;

        // check if interval range overlaps with any existing hyperblocks
        // to not overlap the interval maximum must be below all existing hyperblock minimums
        // or the interval minimum must be above all existing hyperblock maximums
        for (HyperBlock hb : existing_hb)
        {
            for (int j = 0; j < hb.hyper_block.size(); j++)
            {
                // if not unique, then return false
                if (!(intv_max < hb.minimums.get(j)[attr] || intv_min > hb.maximums.get(j)[attr]))
                {
                    return false;
                }
            }
        }

        // if unique, then return true
        return true;
    }


    /**
     * Removes the given value from the given interval.
     * @param val value to be removed
     * @param interval interval to remove value from
     */
    private void remove_interval_value(double val, ArrayList<DataATTR> interval)
    {
        int offset = 0;
        int size = interval.size();

        // remove overlapped values
        for (int k = 0; k < size; k++)
        {
            if (interval.get(k - offset).value == val)
            {
                interval.remove(k - offset);
                offset++;
            }
        }
    }

    private void merger_hyperblock(ArrayList<ArrayList<double[]>> data, ArrayList<ArrayList<double[]>> out_data)
    {
        ArrayList<HyperBlock> blocks = new ArrayList<>(hyper_blocks);
        hyper_blocks.clear();

        for (int i = 0, cnt = blocks.size(); i < out_data.size(); i++)
        {
            // create hyperblock from each datapoint
            for (int j = 0; j < out_data.get(i).size(); j++)
            {
                blocks.add(new HyperBlock(new ArrayList<>()));
                blocks.get(cnt).hyper_block.add(new ArrayList<>(List.of(out_data.get(i).get(j))));
                blocks.get(cnt).findBounds();
                blocks.get(cnt).classNum = i;
                cnt++;
            }
        }

        boolean actionTaken;
        ArrayList<Integer> toBeDeleted = new ArrayList<>();
        int cnt = blocks.size();

        do
        {
            if (cnt <= 0)
            {
                cnt = blocks.size();
            }

            toBeDeleted.clear();
            actionTaken = false;

            if (blocks.size() <= 0)
            {
                break;
            }

            HyperBlock tmp = blocks.get(0);
            blocks.remove(0);

            int tmpClass = tmp.classNum;

            for (int i = 0; i < blocks.size(); i++)
            {
                int curClass = blocks.get(i).classNum;

                if (tmpClass != curClass)
                    continue;

                ArrayList<Double> maxPoint = new ArrayList<>();
                ArrayList<Double> minPoint = new ArrayList<>();

                // define combined space
                for (int j = 0; j < DV.fieldLength; j++)
                {
                    double newLocalMax = Math.max(tmp.maximums.get(0)[j], blocks.get(i).maximums.get(0)[j]);
                    double newLocalMin = Math.min(tmp.minimums.get(0)[j], blocks.get(i).minimums.get(0)[j]);

                    maxPoint.add(newLocalMax);
                    minPoint.add(newLocalMin);
                }

                ArrayList<double[]> pointsInSpace = new ArrayList<>();
                ArrayList<Integer> classInSpace = new ArrayList<>();

                for (int j = 0; j < data.size(); j++)
                {
                    for (int k = 0; k < data.get(j).size(); k++)
                    {
                        boolean withinSpace = true;
                        double[] tmp_pnt = new double[DV.fieldLength];

                        for (int w = 0; w < DV.fieldLength; w++)
                        {
                            tmp_pnt[w] = data.get(j).get(k)[w];

                            if (!(tmp_pnt[w] <= maxPoint.get(w) && tmp_pnt[w] >= minPoint.get(w)))
                            {
                                withinSpace = false;
                                break;
                            }
                        }

                        if (withinSpace)
                        {
                            pointsInSpace.add(tmp_pnt);
                            classInSpace.add(j);
                        }
                    }
                }

                // check if new space is pure
                HashSet<Integer> classCnt = new HashSet<>(classInSpace);

                if (classCnt.size() <= 1)
                {
                    actionTaken = true;
                    tmp.hyper_block.get(0).clear();
                    tmp.hyper_block.get(0).addAll(pointsInSpace);
                    tmp.findBounds();
                    toBeDeleted.add(i);
                }
            }

            int offset = 0;

            for (int i : toBeDeleted)
            {
                blocks.remove(i-offset);
                offset++;
            }

            blocks.add(tmp);
            cnt--;

        } while (actionTaken || cnt > 0);

        // impure
        cnt = blocks.size();

        do
        {
            if (cnt <= 0)
            {
                cnt = blocks.size();
            }

            toBeDeleted.clear();
            actionTaken = false;

            if (blocks.size() <= 1)
            {
                break;
            }

            HyperBlock tmp = blocks.get(0);
            blocks.remove(0);

            ArrayList<Double> acc = new ArrayList<>();

            for (int i = 0; i < blocks.size(); i++)
            {
                // get majority class
                int majorityClass = 0;

                HashMap<Integer, Integer> classCnt = new HashMap<>();

                for (int j = 0; j < blocks.get(i).hyper_block.size(); j++)
                {
                    int curClass = blocks.get(i).classNum;

                    if (classCnt.containsKey(curClass))
                    {
                        classCnt.replace(curClass, classCnt.get(curClass) + 1);
                    }
                    else
                    {
                        classCnt.put(curClass, 1);
                    }
                }

                int majorityCnt = Integer.MIN_VALUE;

                for (int key : classCnt.keySet())
                {
                    if (classCnt.get(key) > majorityCnt)
                    {
                        majorityCnt = classCnt.get(key);
                        majorityClass = key;
                    }
                }

                ArrayList<Double> maxPoint = new ArrayList<>();
                ArrayList<Double> minPoint = new ArrayList<>();

                // define combined space
                for (int j = 0; j < DV.fieldLength; j++)
                {
                    double newLocalMax = Math.max(tmp.maximums.get(0)[j], blocks.get(i).maximums.get(0)[j]);
                    double newLocalMin = Math.min(tmp.minimums.get(0)[j], blocks.get(i).minimums.get(0)[j]);

                    maxPoint.add(newLocalMax);
                    minPoint.add(newLocalMin);
                }

                ArrayList<double[]> pointsInSpace = new ArrayList<>();
                ArrayList<Integer> classInSpace = new ArrayList<>();

                for (int j = 0; j < data.size(); j++)
                {
                    for (int k = 0; k < data.get(j).size(); k++)
                    {
                        boolean withinSpace = true;
                        double[] tmp_pnt = new double[DV.fieldLength];

                        for (int w = 0; w < DV.fieldLength; w++)
                        {
                            tmp_pnt[w] = data.get(j).get(k)[w];

                            if (!(tmp_pnt[w] <= maxPoint.get(w) && tmp_pnt[w] >= minPoint.get(w)))
                            {
                                withinSpace = false;
                                break;
                            }
                        }

                        if (withinSpace)
                        {
                            pointsInSpace.add(tmp_pnt);
                            classInSpace.add(j);
                        }
                    }
                }

                classCnt.clear();

                // check if new space is pure enough
                for (int ints : classInSpace)
                {
                    if (classCnt.containsKey(ints))
                    {
                        classCnt.replace(ints, classCnt.get(ints) + 1);
                    }
                    else
                    {
                        classCnt.put(ints, 1);
                    }
                }

                double curClassTotal = 0;
                double classTotal = 0;

                for (int key : classCnt.keySet())
                {
                    if (key == majorityClass)
                    {
                        curClassTotal = classCnt.get(key);
                    }

                    classTotal += classCnt.get(key);
                }

                acc.add(curClassTotal / classTotal);
            }

            int highestAccIndex = 0;

            for (int j = 0; j < acc.size(); j++)
            {
                if (acc.get(j) > acc.get(highestAccIndex))
                {
                    highestAccIndex = j;
                }
            }

            // if acc meets threshold
            if (acc.get(highestAccIndex) >= acc_threshold)
            {
                actionTaken = true;

                ArrayList<Double> maxPoint = new ArrayList<>();
                ArrayList<Double> minPoint = new ArrayList<>();

                // define combined space
                for (int j = 0; j < DV.fieldLength; j++)
                {
                    double newLocalMax = Math.max(tmp.maximums.get(0)[j], blocks.get(highestAccIndex).maximums.get(0)[j]);
                    double newLocalMin = Math.min(tmp.minimums.get(0)[j], blocks.get(highestAccIndex).minimums.get(0)[j]);

                    maxPoint.add(newLocalMax);
                    minPoint.add(newLocalMin);
                }

                ArrayList<double[]> pointsInSpace = new ArrayList<>();
                ArrayList<Integer> classInSpace = new ArrayList<>();

                for (int j = 0; j < data.size(); j++)
                {
                    for (int k = 0; k < data.get(j).size(); k++)
                    {
                        boolean withinSpace = true;
                        double[] tmp_pnt = new double[DV.fieldLength];

                        for (int w = 0; w < DV.fieldLength; w++)
                        {
                            tmp_pnt[w] = data.get(j).get(k)[w];

                            if (!(tmp_pnt[w] <= maxPoint.get(w) && tmp_pnt[w] >= minPoint.get(w)))
                            {
                                withinSpace = false;
                                break;
                            }
                        }

                        if (withinSpace)
                        {
                            pointsInSpace.add(tmp_pnt);
                            classInSpace.add(j);
                        }
                    }
                }

                if (tmp.hyper_block.get(0).size() < blocks.get(highestAccIndex).hyper_block.get(0).size())
                {
                    tmp.classNum = blocks.get(highestAccIndex).classNum;
                }

                tmp.hyper_block.get(0).clear();
                tmp.hyper_block.get(0).addAll(pointsInSpace);
                tmp.findBounds();

                // store this index to delete the cube that was combined
                toBeDeleted.add(highestAccIndex);
            }

            int offset = 0;

            for (int i : toBeDeleted)
            {
                blocks.remove(i-offset);
                offset++;
            }

            blocks.add(tmp);
            cnt--;

        } while (actionTaken || cnt > 0);

        hyper_blocks.addAll(blocks);
    }

    private void interval_merger_hyperblock()
    {

    }

    private void hyperblock_rules_linear()
    {

    }


    /*private void generateHyperblocks3()
    {
        // create hyperblocks
        hyper_blocks.clear();

        // hyperblocks and hyperblock info
        ArrayList<ArrayList<ArrayList<HyperBlockVisualization.Pnt>>> hb = new ArrayList<>();
        ArrayList<Integer> hb_a = new ArrayList<>();
        ArrayList<Integer> hb_c = new ArrayList<>();

        // indexes of combined hyperblocks
        ArrayList<ArrayList<ArrayList<HyperBlockVisualization.Pnt>>> combined_hb = new ArrayList<>();
        ArrayList<Integer> combined_hb_index = new ArrayList<>();

        // get each element
        ArrayList<ArrayList<HyperBlockVisualization.Pnt>> attributes = new ArrayList<>();

        for (int k = 0; k < DV.fieldLength; k++)
        {
            ArrayList<HyperBlockVisualization.Pnt> tmpField = new ArrayList<>();
            int count = 0;

            for (int i = 0; i < DV.data.size(); i++)
            {
                for (int j = 0; j < DV.data.get(i).data.length; j++)
                {
                    tmpField.add(new HyperBlockVisualization.Pnt(count, DV.data.get(i).data[j][k]));
                    count++;
                }
            }

            attributes.add(tmpField);
        }

        // order attributes by sizes
        for (ArrayList<HyperBlockVisualization.Pnt> attribute : attributes)
        {
            attribute.sort(Comparator.comparingDouble(o -> o.value));
        }

        while (attributes.get(0).size() > 0)
        {
            // get largest hyperblock for each attribute
            ArrayList<HyperBlockVisualization.Pnt> area = findLargestArea(attributes, DV.data.get(DV.upperClass).data.length, hb);

            // if hyperblock is unique then add
            if (area.size() > 1)
            {
                // add hyperblock info
                hb_a.add(area.get(area.size() - 1).index);
                hb_c.add((int)area.get(area.size() - 1).value);
                area.remove(area.size() - 1);

                // add hyperblock
                ArrayList<ArrayList<HyperBlockVisualization.Pnt>> block = new ArrayList<>();

                for (int i = 0; i < DV.fieldLength; i++)
                    block.add(new ArrayList<>());

                for (HyperBlockVisualization.Pnt pnt : area)
                {
                    for (int i = 0; i < attributes.size(); i++)
                    {
                        int index = pnt.index;

                        for (int k = 0; k < attributes.get(i).size(); k++)
                        {
                            if (index == attributes.get(i).get(k).index)
                            {
                                // add point
                                block.get(i).add(new HyperBlockVisualization.Pnt(index, attributes.get(i).get(k).value));

                                // remove point
                                attributes.get(i).remove(k);
                                break;
                            }
                        }
                    }
                }

                // add new hyperblock
                hb.add(block);
            }
            else
            {
                break;
            }
            else
            {
                // get longest overlapping interval
                // combine if same class or refuse to classify interval
                ArrayList<Pnt> oa = findLargestOverlappedArea(attributes, DV.data.get(DV.upperClass).data.length);

                if (oa.size() > 1)
                {
                    int atr = oa.get(oa.size() - 1).key;
                    int cls = (int)oa.get(oa.size() - 1).value;
                    oa.remove(oa.size() - 1);

                    // create temp data
                    ArrayList<ArrayList<Pnt>> block = new ArrayList<>();

                    for (int i = 0; i < DV.fieldLength; i++)
                        block.add(new ArrayList<>());

                    for (Pnt pnt : oa)
                    {
                        for (int i = 0; i < attributes.size(); i++)
                        {
                            int key = pnt.key;

                            for (int k = 0; k < attributes.get(i).size(); k++)
                            {
                                if (key == attributes.get(i).get(k).key)
                                {
                                    // add point
                                    block.get(i).add(new Pnt(key, attributes.get(i).get(k).value));
                                    break;
                                }
                            }
                        }
                    }

                    for (ArrayList<Pnt> blk : block)
                        blk.sort(Comparator.comparingDouble(o -> o.value));

                    // find overlapping hyperblocks
                    boolean[] non_unique = new boolean[hb.size()];

                    for (int i = 0; i < hb.size(); i++)
                    {
                        // get attribute
                        for (int j = 0; j < hb.get(i).size(); j++)
                        {
                            double smallest = Double.MAX_VALUE;
                            double largest = Double.MIN_VALUE;

                            for (int k = 0; k < hb.get(i).get(j).size(); k++)
                            {
                                if (hb.get(i).get(j).get(k).value < smallest)
                                    smallest = hb.get(i).get(j).get(k).value;
                                else if (hb.get(i).get(j).get(k).value > largest)
                                    largest = hb.get(i).get(j).get(k).value;
                            }

                            // search for overlap
                            if (block.get(j).get(0).value >= smallest && block.get(j).get(0).value <= largest)
                            {
                                non_unique[i] = true;
                                break;
                            }
                            // interval overlaps below
                            else if (block.get(j).get(block.get(j).size()-1).value <= largest && block.get(j).get(block.get(j).size() - 1).value >= smallest)
                            {
                                non_unique[i] = true;
                                break;
                            }
                            else if (smallest >= block.get(j).get(0).value && smallest <= block.get(j).get(block.get(j).size()-1).value)
                            {
                                non_unique[i] = true;
                                break;
                            }
                            else if (largest <= block.get(j).get(block.get(j).size()-1).value && largest >= block.get(j).get(0).value)
                            {
                                non_unique[i] = true;
                                break;
                            }
                        }
                    }

                    // get smallest combined hyperblock
                    int sml = -1;

                    for (int i = 0; i < hb.size(); i++)
                    {
                        if (non_unique[i] && sml == -1)
                        {
                            sml = i;
                        }
                        else if (non_unique[i])
                        {
                            int size1 = hb.get(sml).size();
                            int size2 = hb.get(i).size();

                            for (int j = 0; j < combined_hb_index.size(); j++)
                            {
                                if (combined_hb_index.get(j) == sml)
                                {
                                    size1 += combined_hb.get(j).size();
                                }
                                else if (combined_hb_index.get(j) == i)
                                {
                                    size2 += combined_hb.get(j).size();
                                }
                            }

                            if (size2 < size1)
                                sml = i;
                        }
                    }

                    // combine blocks
                    if (sml != -1)
                    {
                        for (ArrayList<Pnt> pnts : block)
                            pnts.clear();

                        for (Pnt pnt : oa)
                        {
                            for (int i = 0; i < attributes.size(); i++)
                            {
                                int key = pnt.key;

                                for (int k = 0; k < attributes.get(i).size(); k++)
                                {
                                    if (key == attributes.get(i).get(k).key)
                                    {
                                        // add point
                                        block.get(i).add(new Pnt(key, attributes.get(i).get(k).value));

                                        // remove point
                                        attributes.get(i).remove(k);
                                        break;
                                    }
                                }
                            }
                        }

                        combined_hb.add(block);
                        combined_hb_index.add(sml);
                    }
                    else {
                        // refuse to classify
                        refuse_area.add(new double[]{atr, block.get(atr).get(0).value, block.get(atr).get(block.get(atr).size() - 1).value});
                    }
                }
                else
                {
                    break;
                }
            }
        }

        // remove overlap
        for (int i = 0; i < refuse_area.size(); i++)
        {
            int atr = (int) refuse_area.get(i)[0];
            double low = refuse_area.get(i)[1];
            double high = refuse_area.get(i)[2];


            for (int j = 0; j < hb.size(); j++)
            {
                int offset = 0;

                for (int k = 0; k < hb.get(j).get(atr).size(); k++)
                {
                    if (low <= hb.get(j).get(atr).get(k - offset).value && hb.get(j).get(atr).get(k - offset).value<= high)
                    {
                        for (int w = 0; w < hb.get(j).size(); w++)
                        {
                            hb.get(j).get(w).remove(k - offset);
                        }

                        offset++;
                    }
                }
            }
        }

        for (int i = 0; i < hb.size(); i++)
        {
            // sort hb by key
            for (int j = 0; j <  hb.get(i).size(); j++)
            {
                hb.get(i).get(j).sort(Comparator.comparingInt(o -> o.index));
            }

            ArrayList<double[]> temp = new ArrayList<>();

            for (int j = 0; j < hb.get(i).get(0).size(); j++)
            {
                double[] tmp = new double[hb.get(i).size()];

                for (int k = 0; k < hb.get(i).size(); k++)
                {
                    tmp[k] = hb.get(i).get(k).get(j).value;
                }

                temp.add(tmp);
            }

            ArrayList<ArrayList<double[]>> tmp = new ArrayList<>();
            tmp.add(temp);

            hyper_blocks.add(new HyperBlock(tmp));
            hyper_blocks.get(i).className = DV.data.get(hb_c.get(i)).className;
            hyper_blocks.get(i).classNum = hb_c.get(i);
            hyper_blocks.get(i).attribute = Integer.toString(hb_a.get(i)+1);
        }

        // add combined blocks
        for (int i = 0; i < combined_hb.size(); i++)
        {
            int index = combined_hb_index.get(i);

            ArrayList<double[]> temp = new ArrayList<>();

            for (int j = 0; j < combined_hb.get(i).get(0).size(); j++)
            {
                double[] tmp = new double[combined_hb.get(i).size()];

                for (int k = 0; k < combined_hb.get(i).size(); k++)
                {
                    tmp[k] = combined_hb.get(i).get(k).get(j).value;
                }

                temp.add(tmp);
            }

            hyper_blocks.get(index).hyper_block.add(temp);
            hyper_blocks.get(index).findBounds();
        }

        for (int i = 0; i < combined_hb_index.size(); i++)
        {
            System.out.println("Block " + (combined_hb_index.get(i) + 1) + " is combined with another block of size " + combined_hb.get(i).size());
        }

        // add dustin algo
        // create dataset
        ArrayList<ArrayList<double[]>> data = new ArrayList<>();

        for (int i = 0; i < DV.data.size(); i++)
            data.add(new ArrayList<>());

        for (int i = 0; i < attributes.get(0).size(); i++)
        {
            if (attributes.get(0).get(i).index < DV.data.get(DV.upperClass).data.length)
            {
                double[] tmp_pnt = new double[DV.fieldLength];

                int index = attributes.get(0).get(i).index;

                for (int k = 0; k < attributes.size(); k++)
                {
                    for (int h = 0; h < attributes.get(k).size(); h++)
                    {
                        if (index == attributes.get(k).get(h).index)
                        {
                            // add point
                            tmp_pnt[k] = attributes.get(k).get(h).value;
                            break;
                        }
                    }
                }

                data.get(0).add(tmp_pnt);
            }
            else
            {
                double[] tmp_pnt = new double[DV.fieldLength];

                int index = attributes.get(0).get(i).index;

                for (int k = 0; k < attributes.size(); k++)
                {
                    for (int h = 0; h < attributes.get(k).size(); h++)
                    {
                        if (index == attributes.get(k).get(h).index)
                        {
                            // add point
                            tmp_pnt[k] = attributes.get(k).get(h).value;
                            break;
                        }
                    }
                }

                data.get(1).add(tmp_pnt);
            }
        }

        ArrayList<ArrayList<double[]>> test_data = new ArrayList<>();

        for (int i = 0; i < DV.data.size(); i++)
        {
            test_data.add(new ArrayList<>());

            for (int j = 0; j < DV.data.get(i).data.length; j++)
            {
                test_data.get(i).add(DV.data.get(i).data[j]);
            }
        }

        if (hb.size() > 1)
        {
            int maxind = 0;
            for (int j = 0; j < hb.get(1).get(0).size(); j++)
            {
                if (hb.get(1).get(0).get(j).index > maxind)
                {
                    maxind = hb.get(1).get(0).get(j).index;
                }
            }

            double[] tmpstuff = new double[DV.fieldLength];
            for (int i = 0; i < DV.fieldLength; i++)
            {
                for (int j = 0; j < hb.get(1).get(i).size(); j++)
                {
                    if (hb.get(1).get(i).get(j).index == maxind)
                    {
                        tmpstuff[i] = hb.get(1).get(i).get(j).value;
                    }
                }
            }
        }

        int cnt = 0;
        for (int i = 0; i < DV.misclassifiedData.size(); i++)
        {
            for (int j = 0; j < hyper_blocks.size(); j++)
            {
                for (int k = 0; k < hyper_blocks.get(j).hyper_block.get(0).size(); k++)
                {
                    if (Arrays.deepEquals(new Object[]{DV.misclassifiedData.get(i)}, new Object[]{hyper_blocks.get(j).hyper_block.get(0).get(k)}))
                    {
                        cnt++;
                    }
                }
            }
        }

        System.out.println("Misclassified: " + DV.misclassifiedData.size());
        System.out.println("Interval: " + cnt);

        cnt = 0;
        for (int i = 0; i < DV.misclassifiedData.size(); i++)
        {
            for (int j = 0; j < data.size(); j++)
            {
                for (int k = 0; k < data.get(j).size(); k++)
                {
                    boolean  comp = true;
                    for (int q = 0; q < DV.fieldLength; q++)
                    {
                        if (DV.misclassifiedData.get(i)[q] != data.get(j).get(k)[q])
                            comp = false;
                    }

                    if (comp)
                        cnt++;
                    if (Arrays.deepEquals(new Object[]{DV.misclassifiedData.get(i)}, new Object[]{data.get(j).get(k)}))
                    {
                        cnt++;
                    }
                }
            }
        }

        System.out.println("Not Interval: " + cnt);

        // dustin alg
        generateHyperblocks(test_data, data);

        // reorder blocks
        ArrayList<HyperBlock> upper = new ArrayList<>();
        ArrayList<HyperBlock> lower = new ArrayList<>();

        for (HyperBlock hyperBlock : hyper_blocks)
        {
            if (hyperBlock.classNum == DV.upperClass)
                upper.add(hyperBlock);
            else
                lower.add(hyperBlock);
        }

        upper.sort(new HyperBlockVisualization.BlockComparator());
        lower.sort(new HyperBlockVisualization.BlockComparator());

        hyper_blocks.clear();
        hyper_blocks.addAll(upper);
        hyper_blocks.addAll(lower);

        for (int i = 0; i < hyper_blocks.size(); i++)
        {
            double[] avg = new double[DV.fieldLength];

            Arrays.fill(avg, 0);

            for (int j = 0; j < hyper_blocks.get(i).hyper_block.get(0).size(); j++)
            {
                for (int k = 0; k < hyper_blocks.get(i).hyper_block.get(0).get(j).length; k++)
                {
                    avg[k] += hyper_blocks.get(i).hyper_block.get(0).get(j)[k];
                }
            }

            for (int j = 0; j < hyper_blocks.get(i).hyper_block.get(0).get(0).length; j++)
                avg[j] /= hyper_blocks.get(i).hyper_block.get(0).size();

            ArrayList<double[]> avg_tmp = new ArrayList<>();
            avg_tmp.add(avg);

            artificial.add(avg_tmp);
        }
        blockCheck();
//        for (int w = 0; w < hyper_blocks.size(); w++)
//        {
//            try{
//                File csv = new File("D:\\Downloads\\Hyperblocks\\HB" + (w+1) + ".csv");
//                Files.deleteIfExists(csv.toPath());
//
//                // write to csv file
//                PrintWriter out = new PrintWriter(csv);
//
//                out.print("Class:," + hyper_blocks.get(w).className + "\n");
//                out.print("Size:," + hyper_blocks.get(w).size + "\n");
//                out.print("Accuracy:," + (Math.round(acc.get(w) * 10000) / 100.0) + "\n");
//
//                // create header for file
//                for (int i = 0; i < DV.fieldLength; i++)
//                {
//                    if (i != DV.fieldLength - 1)
//                        out.print(DV.fieldNames.get(i) + ",");
//                    else
//                        out.print(DV.fieldNames.get(i) + "\n");
//                }
//
//                // get all data for class
//                for (int j = 0; j < hyper_blocks.get(w).hyper_block.get(0).size(); j++)
//                {
//                    double[] tmp = new double[DV.fieldLength];
//                    for (int k = 0; k < DV.fieldLength; k++)
//                    {
//                        tmp[k] = hyper_blocks.get(w).hyper_block.get(0).get(j)[k];
//                        // transform to real value
//                        // undo min-max normalization
//                        tmp[k] *= (DV.max[k] - DV.min[k]);
//                        tmp[k] += DV.min[k];
//
//                        // undo z-score
//                        if (DV.zScoreMinMax)
//                        {
//                            tmp[k] *= DV.sd[k];
//                            tmp[k] += DV.mean[k];
//                        }
//
//                        // round real value to whole number
//                        //tmp[k] = Math.round(tmp[k]);;
//                    }
//
//                    for (int k = 0; k < DV.fieldLength; k++)
//                    {
//                        if (k != DV.fieldLength - 1)
//                            out.printf("%f,", tmp[k]);
//                        else
//                            out.printf("%f" + "\n", tmp[k]);
//                    }
//                }
//
//                // close file
//                out.close();
//            }
//            catch (IOException ioe)
//            {
//                ioe.printStackTrace();
//            }
//        }
    }*/


    /*private ArrayList<DataATTR> findLargestOverlappedArea(ArrayList<ArrayList<DataATTR>> attributes, int upper)
    {
        ArrayList<ArrayList<HyperBlockVisualization.Pnt>> ans = new ArrayList<>();
        ArrayList<Integer> atri = new ArrayList<>();
        ArrayList<Integer> cls = new ArrayList<>();

        for (int a = 0; a < attributes.size(); a++)
        {
            ans.add(new ArrayList<>());
            atri.add(-1);
            cls.add(-1);
            int start = 0;

            ArrayList<HyperBlockVisualization.Pnt> tmp = new ArrayList<>();

            for (int i = 0; i < attributes.get(a).size(); i++)
            {
                if (attributes.get(a).get(start).key < upper && attributes.get(a).get(i).key < upper)
                {
                    tmp.add(attributes.get(a).get(i));

                    if (tmp.size() > ans.get(a).size())
                    {
                        ans.set(a, new ArrayList<>(tmp));
                        atri.set(a, a);
                        cls.set(a, 0);
                    }
                }
                else if (attributes.get(a).get(start).key >= upper && attributes.get(a).get(i).key >= upper)
                {
                    tmp.add(attributes.get(a).get(i));

                    if (tmp.size() > ans.get(a).size())
                    {
                        ans.set(a, new ArrayList<>(tmp));
                        atri.set(a, a);
                        cls.set(a, 1);
                    }
                }
                else
                {
                    // classes are different, but the value is still the same
                    if (attributes.get(a).get(i-1).value == attributes.get(a).get(i).value)
                    {
                        double remove_val = attributes.get(a).get(i).value;

                        // increment until value changes
                        while (remove_val == attributes.get(a).get(i).value)
                        {
                            if (i < attributes.get(a).size()-1)
                                i++;
                            else
                                break;
                        }

                        // remove value from answer
                        if (ans.get(a).size() > 0)
                        {
                            int offset = 0;
                            int size = ans.get(a).size();

                            // remove overlapped attributes
                            for (int k = 0; k < size; k++)
                            {
                                if (ans.get(a).get(k - offset).value == remove_val)
                                {
                                    ans.get(a).remove(k - offset);
                                    offset++;
                                }
                            }
                        }
                        else
                        {
                            atri.set(a, -1);
                            cls.set(a, -1);
                        }
                    }

                    start = i;
                    tmp.clear();
                    tmp.add(attributes.get(a).get(i));
                }
            }
        }

        int big = 0;

        for (int i = 0; i < ans.size(); i++)
        {
            if (ans.get(big).size() < ans.get(i).size())
                big = i;
        }

        // add attribute and class
        ans.get(big).add(new HyperBlockVisualization.Pnt(atri.get(big), cls.get(big)));
        return ans.get(big);
    }*/


    // code taken from VisCanvas2.0 autoCluster function
    // CREDIT LATER
    /*private void generateHyperblocks(ArrayList<ArrayList<double[]>> data, ArrayList<ArrayList<double[]>> stuff)
    {
        ArrayList<HyperBlock> blocks = new ArrayList<>(hyper_blocks);
        hyper_blocks.clear();

        for (int i = 0, cnt = blocks.size(); i < stuff.size(); i++)
        {
            // create hyperblock from each datapoint
            for (int j = 0; j < stuff.get(i).size(); j++)
            {
                blocks.add(new HyperBlock(new ArrayList<>()));
                blocks.get(cnt).hyper_block.add(new ArrayList<>(java.util.List.of(stuff.get(i).get(j))));
                blocks.get(cnt).findBounds();
                blocks.get(cnt).className = DV.data.get(i).className;
                blocks.get(cnt).classNum = i;
                cnt++;
            }
        }

        boolean actionTaken;
        ArrayList<Integer> toBeDeleted = new ArrayList<>();
        int cnt = blocks.size();

        do
        {
            if (cnt <= 0)
            {
                cnt = blocks.size();
            }

            toBeDeleted.clear();
            actionTaken = false;

            if (blocks.size() <= 0)
            {
                break;
            }

            HyperBlock tmp = blocks.get(0);
            blocks.remove(0);

            int tmpClass = tmp.classNum;

            for (int i = 0; i < blocks.size(); i++)
            {
                int curClass = blocks.get(i).classNum;

                if (tmpClass != curClass)
                    continue;

                ArrayList<Double> maxPoint = new ArrayList<>();
                ArrayList<Double> minPoint = new ArrayList<>();

                // define combined space
                for (int j = 0; j < DV.fieldLength; j++)
                {
                    double newLocalMax = Math.max(tmp.maximums.get(0)[j], blocks.get(i).maximums.get(0)[j]);
                    double newLocalMin = Math.min(tmp.minimums.get(0)[j], blocks.get(i).minimums.get(0)[j]);

                    maxPoint.add(newLocalMax);
                    minPoint.add(newLocalMin);
                }

                // check if misclassified point lies in space
                boolean cont = true;

                for (int m = 0; m < DV.misclassifiedData.size(); m++)
                {
                    boolean inside = true;

                    for (int f = 0; f < DV.fieldLength; f++)
                    {
                        if (DV.misclassifiedData.get(m)[f] > maxPoint.get(f) || DV.misclassifiedData.get(m)[f] < minPoint.get(f))
                        {
                            inside = false;
                            break;
                        }
                    }

                    if (inside)
                    {
                        cont = false;
                        break;
                    }
                }

                if (cont)
                {
                    ArrayList<double[]> pointsInSpace = new ArrayList<>();
                    ArrayList<Integer> classInSpace = new ArrayList<>();

                    for (int j = 0; j < data.size(); j++)
                    {
                        for (int k = 0; k < data.get(j).size(); k++)
                        {
                            boolean withinSpace = true;
                            double[] tmp_pnt = new double[DV.fieldLength];

                            for (int w = 0; w < DV.fieldLength; w++)
                            {
                                tmp_pnt[w] = data.get(j).get(k)[w];

                                if (!(tmp_pnt[w] <= maxPoint.get(w) && tmp_pnt[w] >= minPoint.get(w)))
                                {
                                    withinSpace = false;
                                    break;
                                }
                            }

                            if (withinSpace)
                            {
                                pointsInSpace.add(tmp_pnt);
                                classInSpace.add(j);
                            }
                        }
                    }

                    // check if new space is pure
                    HashSet<Integer> classCnt = new HashSet<>(classInSpace);

                    if (classCnt.size() <= 1)
                    {
                        actionTaken = true;
                        tmp.hyper_block.get(0).clear();
                        tmp.hyper_block.get(0).addAll(pointsInSpace);
                        tmp.findBounds();
                        toBeDeleted.add(i);
                    }
                }
            }

            int offset = 0;

            for (int i : toBeDeleted)
            {
                blocks.remove(i-offset);
                offset++;
            }

            blocks.add(tmp);
            cnt--;

        } while (actionTaken || cnt > 0);

        // impure
        cnt = blocks.size();

        do
        {
            if (cnt <= 0)
            {
                cnt = blocks.size();
            }

            toBeDeleted.clear();
            actionTaken = false;

            if (blocks.size() <= 1)
            {
                break;
            }

            HyperBlock tmp = blocks.get(0);
            blocks.remove(0);

            int tmpClass = tmp.classNum;

            ArrayList<Double> acc = new ArrayList<>();

            for (HyperBlock block : blocks)
            {
                // get majority class
                int majorityClass = 0;

                HashMap<Integer, Integer> classCnt = new HashMap<>();

                for (int j = 0; j < block.hyper_block.size(); j++)
                {
                    int curClass = block.classNum;

                    if (classCnt.containsKey(curClass))
                    {
                        classCnt.replace(curClass, classCnt.get(curClass) + 1);
                    }
                    else
                    {
                        classCnt.put(curClass, 1);
                    }
                }

                int majorityCnt = Integer.MIN_VALUE;

                for (int key : classCnt.keySet())
                {
                    if (classCnt.get(key) > majorityCnt)
                    {
                        majorityCnt = classCnt.get(key);
                        majorityClass = key;
                        block.className = DV.data.get(key).className;
                    }
                }

                ArrayList<Double> maxPoint = new ArrayList<>();
                ArrayList<Double> minPoint = new ArrayList<>();

                // define combined space
                for (int j = 0; j < DV.fieldLength; j++)
                {
                    double newLocalMax = Math.max(tmp.maximums.get(0)[j], block.maximums.get(0)[j]);
                    double newLocalMin = Math.min(tmp.minimums.get(0)[j], block.minimums.get(0)[j]);

                    maxPoint.add(newLocalMax);
                    minPoint.add(newLocalMin);
                }

                // check if misclassified point lies in space
                boolean cont = true;

                for (int m = 0; m < DV.misclassifiedData.size(); m++)
                {
                    boolean inside = true;

                    for (int f = 0; f < DV.fieldLength; f++)
                    {
                        if (DV.misclassifiedData.get(m)[f] > maxPoint.get(f) || DV.misclassifiedData.get(m)[f] < minPoint.get(f))
                        {
                            inside = false;
                            break;
                        }
                    }

                    if (inside)
                    {
                        cont = false;
                        break;
                    }
                }

                if (cont)
                {
                    ArrayList<double[]> pointsInSpace = new ArrayList<>();
                    ArrayList<Integer> classInSpace = new ArrayList<>();

                    for (int j = 0; j < data.size(); j++)
                    {
                        for (int k = 0; k < data.get(j).size(); k++)
                        {
                            boolean withinSpace = true;
                            double[] tmp_pnt = new double[DV.fieldLength];

                            for (int w = 0; w < DV.fieldLength; w++)
                            {
                                tmp_pnt[w] = data.get(j).get(k)[w];

                                if (!(tmp_pnt[w] <= maxPoint.get(w) && tmp_pnt[w] >= minPoint.get(w)))
                                {
                                    withinSpace = false;
                                    break;
                                }
                            }

                            if (withinSpace)
                            {
                                pointsInSpace.add(tmp_pnt);
                                classInSpace.add(j);
                            }
                        }
                    }

                    classCnt.clear();

                    // check if new space is pure enough
                    for (int ints : classInSpace)
                    {
                        if (classCnt.containsKey(ints))
                        {
                            classCnt.replace(ints, classCnt.get(ints) + 1);
                        }
                        else
                        {
                            classCnt.put(ints, 1);
                        }
                    }

                    double curClassTotal = 0;
                    double classTotal = 0;

                    for (int key : classCnt.keySet())
                    {
                        if (key == majorityClass)
                        {
                            curClassTotal = classCnt.get(key);
                        }

                        classTotal += classCnt.get(key);
                    }

                    acc.add(curClassTotal / classTotal);

                    if (curClassTotal == classTotal)
                    {
                        pure_blocks.add(block);
                    }
                }
                else
                {
                    acc.add(0.0);
                }
            }

            int highestAccIndex = 0;

            for (int j = 0; j < acc.size(); j++)
            {
                if (acc.get(j) > acc.get(highestAccIndex))
                {
                    highestAccIndex = j;
                }
            }

            // if acc meets threshold
            if (acc.get(highestAccIndex) >= acc_threshold)
            {
                actionTaken = true;

                ArrayList<Double> maxPoint = new ArrayList<>();
                ArrayList<Double> minPoint = new ArrayList<>();

                // define combined space
                for (int j = 0; j < DV.fieldLength; j++)
                {
                    double newLocalMax = Math.max(tmp.maximums.get(0)[j], blocks.get(highestAccIndex).maximums.get(0)[j]);
                    double newLocalMin = Math.min(tmp.minimums.get(0)[j], blocks.get(highestAccIndex).minimums.get(0)[j]);

                    maxPoint.add(newLocalMax);
                    minPoint.add(newLocalMin);
                }

                // check if misclassified point lies in space
                boolean cont = true;

                for (int m = 0; m < DV.misclassifiedData.size(); m++)
                {
                    boolean inside = true;

                    for (int f = 0; f < DV.fieldLength; f++)
                    {
                        if (DV.misclassifiedData.get(m)[f] > maxPoint.get(f) || DV.misclassifiedData.get(m)[f] < minPoint.get(f))
                        {
                            inside = false;
                            break;
                        }
                    }

                    if (inside)
                    {
                        cont = false;
                        break;
                    }
                }

                if (cont)
                {
                    ArrayList<double[]> pointsInSpace = new ArrayList<>();
                    ArrayList<Integer> classInSpace = new ArrayList<>();

                    for (int j = 0; j < data.size(); j++)
                    {
                        for (int k = 0; k < data.get(j).size(); k++)
                        {
                            boolean withinSpace = true;
                            double[] tmp_pnt = new double[DV.fieldLength];

                            for (int w = 0; w < DV.fieldLength; w++)
                            {
                                tmp_pnt[w] = data.get(j).get(k)[w];

                                if (!(tmp_pnt[w] <= maxPoint.get(w) && tmp_pnt[w] >= minPoint.get(w)))
                                {
                                    withinSpace = false;
                                    break;
                                }
                            }

                            if (withinSpace)
                            {
                                pointsInSpace.add(tmp_pnt);
                                classInSpace.add(j);
                            }
                        }
                    }

                    if (tmp.hyper_block.get(0).size() < blocks.get(highestAccIndex).hyper_block.get(0).size())
                    {
                        tmp.classNum = blocks.get(highestAccIndex).classNum;
                    }
                    tmp.hyper_block.get(0).clear();
                    tmp.hyper_block.get(0).addAll(pointsInSpace);
                    tmp.findBounds();

                    // store this key to delete the cube that was combined
                    toBeDeleted.add(highestAccIndex);
                }
            }

            int offset = 0;

            for (int i : toBeDeleted)
            {
                blocks.remove(i-offset);
                offset++;
            }

            blocks.add(tmp);
            cnt--;

        } while (actionTaken || cnt > 0);

        hyper_blocks.addAll(blocks);

        // count blocks that share instance of data
        overlappingBlockCnt = 0;

        for (int i = 0; i < data.size(); i++)
        {
            for (int j = 0; j < data.get(i).size(); j++)
            {
                int presentIn = 0;

                for (int k = 0; k < pure_blocks.size(); k++)
                {
                    for (int w = 0; w < pure_blocks.get(k).hyper_block.get(0).size(); w++)
                    {
                        if (Arrays.equals(pure_blocks.get(k).hyper_block.get(0).get(w), data.get(i).get(j)))
                        {
                            presentIn++;
                            break;
                        }
                    }
                }

                if (presentIn > 1)
                {
                    overlappingBlockCnt++;
                }
            }
        }

        totalBlockCnt = hyper_blocks.size();

        accuracy.clear();

        for (HyperBlock block : blocks)
        {
            // get majority class
            int majorityClass = 0;

            HashMap<Integer, Integer> classCnt = new HashMap<>();

            for (int j = 0; j < block.hyper_block.size(); j++)
            {
                int curClass = block.classNum;

                if (classCnt.containsKey(curClass))
                {
                    classCnt.replace(curClass, classCnt.get(curClass) + 1);
                }
                else
                {
                    classCnt.put(curClass, 1);
                }
            }

            int majorityCnt = Integer.MIN_VALUE;

            for (int key : classCnt.keySet())
            {
                if (classCnt.get(key) > majorityCnt)
                {
                    majorityCnt = classCnt.get(key);
                    majorityClass = key;
                    block.className = DV.data.get(key).className;
                    block.classNum = key;
                }
            }

            double curClassTotal = 0;
            double classTotal = 0;

            for (int key : classCnt.keySet())
            {
                if (key == majorityClass)
                {
                    curClassTotal = classCnt.get(key);
                }

                classTotal += classCnt.get(key);
            }

            accuracy.add(String.format("%.2f%%", 100 * curClassTotal / classTotal));

            if (curClassTotal == classTotal)
            {
                pure_blocks.add(block);
            }
        }
    }*/



    static class BlockComparator implements Comparator<HyperBlock>
    {

        // Gives largest Hyperblock
        public int compare(HyperBlock b1, HyperBlock b2)
        {
            int b1_size = 0;
            int b2_size = 0;

            for (int i = 0; i < b1.hyper_block.size(); i++)
                b1_size += b1.hyper_block.get(i).size();

            for (int i = 0; i < b2.hyper_block.size(); i++)
                b2_size += b2.hyper_block.get(i).size();

            return Integer.compare(b2_size, b1_size);
        }
    }

    public void increase_level()
    {
        // get all artificial datapoints
        originalObjects = new ArrayList<>();
        originalObjects.addAll(objects);

        originalHyperBlocks = new ArrayList<>();
        originalHyperBlocks.addAll(hyper_blocks);

        ArrayList<double[]> tmpData = new ArrayList<>();
        ArrayList<double[]> tmpData2 = new ArrayList<>();

        // starting
        for (int i = 0; i < hyper_blocks.size(); i++)
        {
            ArrayList<double[]> stuff = find_evelope_cases(i);

            for (int j = 0; j < stuff.size(); j++)
            {
                if (hyper_blocks.get(i).classNum == 0)
                    tmpData.add(stuff.get(j));
                else
                    tmpData2.add(stuff.get(j));
            }

        }

        //DV.misclassifiedData.clear();

        //misclassified.clear();
        //acc.clear();

        objects = new ArrayList<>();
        upperObjects = new ArrayList<>();
        lowerObjects = new ArrayList<>();

        double[][] newData = new double[tmpData.size()][DV.fieldLength];
        tmpData.toArray(newData);
        DataObject newObj = new DataObject("upper", newData);

        double[][] newData2 = new double[tmpData2.size()][DV.fieldLength];
        tmpData2.toArray(newData2);
        DataObject newObj2 = new DataObject("lower", newData2);

        newObj.updateCoordinatesGLC(DV.angles);
        upperObjects.add(newObj);

        newObj2.updateCoordinatesGLC(DV.angles);
        lowerObjects.add(newObj2);

        objects.add(upperObjects);
        objects.add(lowerObjects);

        DV.data.clear();
        DV.data.add(newObj);
        DV.data.add(newObj2);

        generateHyperblocks();
        blockCheck();

        average_hb_test();
    }

    private void average_hb_test()
    {
        // we need to keep track of if each point is in each HB
        // if this point is good
        // if this point is bad
        // or if this point is not in any hb
        int[] good = new int[hyper_blocks.size()];
        int[] bad = new int[hyper_blocks.size()];

        for (int i = 0; i < hyper_blocks.size(); i++)
        {
            good[i] = 0;
            bad[i] = 0;
        }
        int not_in = 0;

        // populate main series
        for (int d = 0; d < originalObjects.size(); d++)
        {
            for (DataObject data : originalObjects.get(d))
            {
                for (int i = 0; i < data.data.length; i++)
                {
                    boolean in_one = false;

                    for (int hb = 0; hb < hyper_blocks.size(); hb++)
                    {
                        // start line at (0, 0)
                        boolean within = false;

                        for (int k = 0; k < hyper_blocks.get(hb).hyper_block.size(); k++)
                        {
                            boolean within_cur = true;

                            for (int j = 0; j < DV.fieldLength; j++)
                            {
                                if (data.data[i][j] < hyper_blocks.get(hb).minimums.get(k)[j] || data.data[i][j] > hyper_blocks.get(hb).maximums.get(k)[j])
                                {
                                    within_cur = false;
                                }

                                if (j == DV.fieldLength - 1)
                                {
                                    if (within_cur)
                                    {
                                        within = true;
                                    }
                                }
                            }
                        }

                        // add points to lines
                        for (int j = 0; j < DV.fieldLength; j++)
                        {
                            // add endpoint and timeline
                            if (j == DV.fieldLength - 1)
                            {
                                // add series
                                if (within)
                                {
                                    in_one = true;

                                    if (d == hyper_blocks.get(hb).classNum)
                                    {
                                        good[hb]++;
                                    }
                                    else
                                    {
                                        bad[hb]++;
                                    }
                                }
                            }
                        }
                    }

                    if(!in_one) not_in++;
                }
            }
        }


        // we now know the correctly and incorrectly classified points in each HB
        // we also know the number of points not in any HB

        System.out.println("\n\n\n");

        for (int i = 0; i < hyper_blocks.size(); i++)
        {
            if (good[i] + bad[i] > 0)
                System.out.println("HB-" + (i+1) + ":\n" + "\tSize: " + (good[i] + bad[i]) + "\n\tGood: " + good[i] + "\n\tBad: " + bad[i] + "\n\tAcc: " + (good[i] / (double)(good[i] + bad[i])));
            else
                System.out.println("HB-" + (i+1) + ":\n" + "\tSize: " + (good[i] + bad[i]) + "\n\tGood: " + good[i] + "\n\tBad: " + bad[i] + "\n\tAcc: NAN");
        }
        System.out.println("\nData not in any HB: " + not_in);
        System.out.println("\n\n\n");
    }

    private void hb_test()
    {
        // we need to keep track of if each point is in each HB
        // if this point is good
        // if this point is bad
        // or if this point is not in any hb
        int[] good = new int[hyper_blocks.size()];
        int[] bad = new int[hyper_blocks.size()];

        for (int i = 0; i < hyper_blocks.size(); i++)
        {
            good[i] = 0;
            bad[i] = 0;
        }
        int not_in = 0;

        // populate main series
        for (int d = 0; d < objects.size(); d++)
        {
            for (DataObject data : objects.get(d))
            {
                for (int i = 0; i < data.data.length; i++)
                {
                    for (int hb = 0; hb < hyper_blocks.size(); hb++)
                    {
                        for (int k = 0; k < hyper_blocks.get(hb).hyper_block.size(); k++)
                        {
                            boolean within_cur = true;

                            for (int j = 0; j < DV.fieldLength; j++)
                            {
                                if (data.data[i][j] < hyper_blocks.get(hb).minimums.get(k)[j] || data.data[i][j] > hyper_blocks.get(hb).maximums.get(k)[j])
                                {
                                    within_cur = false;
                                }

                                if (j == DV.fieldLength - 1)
                                {
                                    if (within_cur)
                                    {
                                        if (d == hyper_blocks.get(hb).classNum)
                                        {
                                            good[hb]++;
                                        }
                                        else
                                        {
                                            bad[hb]++;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }


        // we now know the correctly and incorrectly classified points in each HB
        // we also know the number of points not in any HB

        System.out.println("\n\n\n");

        for (int i = 0; i < hyper_blocks.size(); i++)
        {
            if (good[i] + bad[i] > 0)
                System.out.println("HB-" + (i+1) + ":\n" + "\tSize: " + (good[i] + bad[i]) + "\n\tGood: " + good[i] + "\n\tBad: " + bad[i] + "\n\tAcc: " + (good[i] / (double)(good[i] + bad[i])));
            else
                System.out.println("HB-" + (i+1) + ":\n" + "\tSize: " + (good[i] + bad[i]) + "\n\tGood: " + good[i] + "\n\tBad: " + bad[i] + "\n\tAcc: NAN");
        }
        System.out.println("\n\n\n");
    }

    /*private void increase_level_REAL()
    {
        ArrayList<HyperBlock> storage = new ArrayList<>(hyper_blocks);
        hyper_blocks.addAll(test_hyper_blocks);

        JTabbedPane testTabs = new JTabbedPane();
        JPanel testtmp = new JPanel();
        testtmp.add(drawPCBlockTiles(objects, -1, -1));
        JScrollPane tmpteststuff = new JScrollPane(testtmp);
        testTabs.add("Original", tmpteststuff);

        hyper_blocks = new ArrayList<>(storage);

        originalObjects = new ArrayList<>();
        originalObjects.addAll(objects);

        originalHyperBlocks = new ArrayList<>();
        originalHyperBlocks.addAll(hyper_blocks);

        for (int i = 0; i < hyper_blocks.size(); i++)
        {
            //drawStuff(i);
        }

        ArrayList<double[]> ntmpData = new ArrayList<>();
        ArrayList<double[]> ntmpData2 = new ArrayList<>();

        // starting
        for (int i = 0; i < hyper_blocks.size(); i++)
        {
            for (int j = 0; j < hyper_blocks.get(i).hyper_block.size(); j++)
            {
                double[] tmp1 = new double[DV.fieldLength * 2];
                int cnt = 0;

                for (int k = 0; k < DV.fieldLength; k++)
                {
                    tmp1[cnt] = hyper_blocks.get(i).minimums.get(j)[k];
                    cnt++;

                    tmp1[cnt] = hyper_blocks.get(i).maximums.get(j)[k];
                    cnt++;
                }

                if (hyper_blocks.get(i).classNum == 0)
                    ntmpData.add(tmp1);
                else
                    ntmpData2.add(tmp1);
            }
        }

        int mult = (Integer) hb_lvl.getValue();
        int val = DV.angles.length;

        for (int i = 0; i < mult - 1; i++)
            val *= 2;

        double[] newAngles = new double[val];

        int cnt = 0;

        for (int i = 0; i < DV.angles.length; i++)
        {
            for (int j = 0; j < (val / DV.angles.length); j++)
            {
                newAngles[cnt] = DV.angles[i];
                cnt++;
            }
        }

        DV.fieldLength = DV.fieldLength * 2;
        DV.misclassifiedData.clear();

        misclassified.clear();
        acc.clear();

        DV.angles = newAngles;

        for (int w = 0; w < test_hyper_blocks.size(); w++)
        {
            for (int q = 0; q < 2; q++)
            {
                ArrayList<double[]> tmpData = new ArrayList<>(ntmpData);
                ArrayList<double[]> tmpData2 = new ArrayList<>(ntmpData2);

                double[] tmp1 = new double[DV.fieldLength];
                cnt = 0;

                for (int k = 0; k < test_hyper_blocks.get(w).minimums.get(0).length; k++)
                {
                    tmp1[cnt] = test_hyper_blocks.get(w).minimums.get(0)[k];
                    cnt++;

                    tmp1[cnt] = test_hyper_blocks.get(w).maximums.get(0)[k];
                    cnt++;
                }

                if (q == 0)
                    tmpData.add(tmp1);
                else
                    tmpData2.add(tmp1);

                test_hyper_blocks.get(w).classNum = q;

                objects = new ArrayList<>();
                upperObjects = new ArrayList<>();
                lowerObjects = new ArrayList<>();

                double[][] newData = new double[tmpData.size()][DV.fieldLength * 2];
                tmpData.toArray(newData);
                DataObject newObj = new DataObject("upper", newData);

                double[][] newData2 = new double[tmpData2.size()][DV.fieldLength * 2];
                tmpData2.toArray(newData2);
                DataObject newObj2 = new DataObject("lower", newData2);

                newObj.updateCoordinatesGLC(newAngles);
                upperObjects.add(newObj);

                newObj2.updateCoordinatesGLC(newAngles);
                lowerObjects.add(newObj2);

                objects.add(upperObjects);
                objects.add(lowerObjects);

                DV.data.clear();
                DV.data.add(newObj);
                DV.data.add(newObj2);

                generateHyperblocks3();
                blockCheck();

                JPanel tmptest = new JPanel();
                if (q == 0)
                    tmptest.add(drawPCBlockTiles(objects, upperObjects.size() - 1, 0));
                else
                    tmptest.add(drawPCBlockTiles(objects, lowerObjects.size() - 1, 1));
                testTabs.add("HB" + w + "-" + q, tmptest);
            }
        }

        JFrame tmpframe2 = new JFrame();
        tmpframe2.setExtendedState( tmpframe2.getExtendedState()|JFrame.MAXIMIZED_BOTH );
        tmpframe2.add(testTabs);
        tmpframe2.pack();
        tmpframe2.revalidate();
        tmpframe2.setVisible(true);

        //combineAll();

        //for (int i = 0; i < hyper_blocks.size(); i++)
        //pc_lvl_2_Test2(objects, i);
    }*/

    private void blockCheck()
    {
        int bcnt = 0;
        for (int h = 0; h < hyper_blocks.size(); h++)
        {
            for (int q = 0; q < hyper_blocks.get(h).hyper_block.size(); q++)
            {
                bcnt += hyper_blocks.get(h).hyper_block.get(q).size();
            }
        }

        int[] counter = new int[hyper_blocks.size()];
        ArrayList<ArrayList<double[]>> hello = new ArrayList<>();

        for (int h = 0; h < hyper_blocks.size(); h++)
        {
            ArrayList<double[]> inside = new ArrayList<>();
            ArrayList<double[]> good = new ArrayList<>();
            ArrayList<double[]> bad = new ArrayList<>();

            double maj_cnt = 0;

            for (int i = 0; i < DV.data.size(); i++)
            {
                for (int j = 0; j < DV.data.get(i).data.length; j++)
                {
                    for (int q = 0; q < hyper_blocks.get(h).hyper_block.size(); q++)
                    {
                        boolean tmp = true;

                        for (int k = 0; k < DV.fieldLength; k++)
                        {
                            if (DV.data.get(i).data[j][k] > hyper_blocks.get(h).maximums.get(q)[k] || DV.data.get(i).data[j][k] < hyper_blocks.get(h).minimums.get(q)[k])
                            {
                                tmp = false;
                                break;
                            }
                        }

                        if (tmp)
                        {
                            if (i == hyper_blocks.get(h).classNum)
                            {
                                maj_cnt++;
                            }

                            counter[h]++;

                            double[] hi = new double[DV.fieldLength];
                            System.arraycopy(DV.data.get(i).data[j], 0, hi, 0, DV.fieldLength);
                            inside.add(hi);

                            for (int k = 0; k < hyper_blocks.get(h).hyper_block.size(); k++)
                            {
                                for (int w = 0; w < hyper_blocks.get(h).hyper_block.get(k).size(); w++)
                                {
                                    if (Arrays.deepEquals(new Object[]{hi}, new Object[]{hyper_blocks.get(h).hyper_block.get(k).get(w)}))
                                    {
                                        good.add(hi);
                                        break;
                                    }
                                    else if (k == hyper_blocks.get(h).hyper_block.size()-1 && w == hyper_blocks.get(h).hyper_block.get(k).size() - 1)
                                    {
                                        bad.add(hi);
                                    }
                                }
                            }
                        }
                    }
                }
            }

            //System.out.println("\nBlock " + (h+1) + " Size: " + counter[h]);
            //System.out.println("Block " + (h+1) + " Accuracy: " + (maj_cnt / counter[h]));

            acc.add(maj_cnt / counter[h]);
            misclassified.add(counter[h] - (int)maj_cnt);
            block_size.add(counter[h]);

            int cnt = 0;

            for (int j = 0; j < hello.size(); j++)
            {
                for (int k = 0; k < hello.get(j).size(); k++)
                {
                    for (int w = 0; w < inside.size(); w++)
                    {
                        if (Arrays.deepEquals(new Object[]{inside.get(w)}, new Object[]{hello.get(j).get(k)}))
                            cnt++;//System.out.println("DUPLICATE POINT: hb = " + (j+1) + "   point = " + k);
                    }
                }
            }

            //System.out.println("Block " + (h+1) + " Duplicates: " + cnt);

            ArrayList<double[]> tmptmp = new ArrayList<>(inside);
            hello.add(tmptmp);
        }
    }


    // create new HB around test data
    private void classify_new_case()
    {
        original_num = hyper_blocks.size();
        double[] test_mah = new double[]{1.975812333099429, 1.284909284046077, 0.6306117275483252, 2.9777008547494446, 3.0285801070325156, 0.6306117275483252, 3.208965647593643, 3.088327068745582, 1.7661883479067624, 0.8742481279541545, 1.1603281359622302, 0.6306117275483252, 1.6013262293532615, 16.433274822372734, 1.975812333099429, 1.5936640297164983, 1.371620583840687, 4.5054896968870235, 4.5054896968870235, 1.6006900723548672, 1.975812333099429, 2.548922139765465, 1.1603281359622302, 1.6323024061944285, 1.4324773890770508, 3.4220349318247343, 1.5936640297164983, 6.840199795474814, 2.320521986616137, 2.1128503428456833, 1.975812333099429, 1.284909284046077, 2.099028244516945, 1.975812333099429, 1.975812333099429, 1.975812333099429, 1.975812333099429, 1.5503053661096706, 1.3534282889192037, 25.329843078565062, 3.0285801070325156, 1.0774479359860019, 1.117869755403515, 1.826687955756608, 1.284909284046077, 13.054773756312496, 13.701330449842725, 19.205033769263736, 27.67490587248777, 21.266069360353328, 21.517660028312264, 36.72848151971808, 25.40681063598222, 30.428878426956903, 32.383219823574336, 6.523765004338447, 17.95536613435098, 31.713285879651643, 49.05903825896904, 14.245378584683916, 23.935501211330717, 19.282746073973502, 18.963044564996725, 19.92497087604741, 20.01301730021541, 20.703514020332495, 22.81345242007682, 22.068232758567163, 20.256938759467623};

        int cnt = 0;

        for (int i = 0; i < DV.testData.size(); i++)
        {
            for (int j = 0; j < DV.testData.get(i).data.length; j++)
            {
                if (cnt == new_case)
                {
                // add to existing hyperblock or generate new hyperblock
                if (!in_HB(DV.testData.get(i).data[j]))
                {
                    System.out.println("\nCREATING NEW HB!");
                    System.out.println("Test Case Class: " + i);
                    create_kNN_HB(DV.testData.get(i).data[j], test_mah[cnt], 5);
                }

                else
                    System.out.println("NEW CASE " + new_case + " IS ALREADY WITHIN AN HB");

                new_case++;
                    return;
                }
                else
                    cnt++;

                cnt++;
            }
        }

        add_test_data();
    }


    private boolean in_HB(double[] c)
    {
        for (int i = 0; i < hyper_blocks.size(); i++)
        {
            for (int j = 0; j < hyper_blocks.get(i).hyper_block.size(); j++)
            {
                boolean inside = true;

                for (int k = 0; k < DV.fieldLength; k++)
                {
                    // check if outside bounds
                    if (hyper_blocks.get(i).maximums.get(j)[k] < c[k] ||
                            hyper_blocks.get(i).minimums.get(j)[k] > c[k])
                    {
                        inside = false;
                        break;
                    }
                }

                // new case is within bounds
                if (inside)
                {
                    // add case to hyperblock
                    hyper_blocks.get(i).hyper_block.get(j).add(c);

                    return true;
                }
            }
        }

        return false;
    }

    // create pure hb around a new case
    private void create_kNN_HB(double[] c, double val, int k)
    {
        // create HB
        HyperBlock hb_c = kNN_mahalanobis(c, val, k);
        hyper_blocks.add(hb_c);
        System.out.println("HB Class: " + hb_c.classNum + "\n");

        double[] avg = new double[DV.fieldLength];
        Arrays.fill(avg, 0);

        for (int i = 0; i < hb_c.hyper_block.get(0).size(); i++)
        {
            for (int j = 0; j < hb_c.hyper_block.get(0).get(i).length; j++)
            {
                avg[j] += hb_c.hyper_block.get(0).get(i)[j];
            }
        }

        for (int j = 0; j < hb_c.hyper_block.get(0).get(0).length; j++)
            avg[j] /= hb_c.hyper_block.get(0).size();

        ArrayList<double[]> avg_tmp = new ArrayList<>();
        avg_tmp.add(avg);

        artificial.add(avg_tmp);
    }

    private double euclidean_distance(double[] x, double[] y)
    {
        double dist = 0;

        for (int i = 0; i < x.length; i++)
        {
            double coef = Math.cos(Math.toRadians(DV.angles[i]));
            dist += Math.pow((Math.pow(x[i], 2) - Math.pow(y[i], 2)) * coef, 2);
        }

        return Math.sqrt(dist);
    }

    private HyperBlock kNN(double[] x, int k)
    {
        ArrayList<double[]> cluster = new ArrayList<>();
        double[] dists = new double[k];
        int[] classes = new int[k];

        for (int i = 0; i < objects.size(); i++)
        {
            for (int j = 0; j < objects.get(i).size(); j++)
            {
                for (int q = 0; q < objects.get(i).get(j).data.length; q++)
                {
                    double dist = euclidean_distance(x, objects.get(i).get(j).data[q]);

                    if (cluster.size() < k)
                    {
                        dists[cluster.size()] = dist;
                        classes[cluster.size()] = i;
                        cluster.add(objects.get(i).get(j).data[k]);
                    }
                    else
                    {
                        double num = 0;
                        int index = 0;

                        for (int w = 0; w < k; w++)
                        {
                            if (dist < dists[w] && (dist - dists[w]) < num)
                            {
                                num = dist - dists[w];
                                index = w;
                            }
                        }

                        if (num < 0)
                        {
                            cluster.set(index, objects.get(i).get(j).data[k]);
                            classes[index] = i;
                            dists[index] = dist;
                        }
                    }
                }
            }
        }

        cluster.add(x);
        ArrayList<ArrayList<double[]>> data_c = new ArrayList<>(java.util.List.of(cluster));

        // create HB
        HyperBlock hb_c = new HyperBlock(data_c);
        hb_c.classNum = classify_hb(classes, hb_c);

        return hb_c;
    }

    private HyperBlock kNN_mahalanobis(double[] x, double val, int k)
    {
        ArrayList<double[]> cluster = new ArrayList<>();
        double[] dists = new double[k];
        int[] classes = new int[k];

        double[] train_mah = new double[]{2.066354732712421, 13.98735041009519, 1.046333559705122, 17.24407121832487, 2.186915224235358, 17.071965771796844, 1.972975465178567, 8.747465762548657, 1.5033382401784798, 2.674087134154137, 0.8704170281357733, 2.6687547802767435, 0.8742481279541546, 1.371620583840687, 3.244543361680094, 0.6306117275483252, 2.1880683431851486, 2.1093191344052484, 1.6013262293532615, 0.8704170281357733, 4.989155884090962, 1.06977227882828, 1.4324773890770508, 1.4753270477221958, 1.0862678066727285, 0.870417028135773, 15.078190428616542, 2.1685517914466885, 1.5936640297164983, 2.186915224235358, 1.7779307547736631, 1.5936640297164983, 1.371620583840687, 2.2860155184648834, 3.428344385422842, 12.803379133987075, 3.9635531263719344, 8.148567160529156, 4.1859928508293045, 1.33617318098772, 1.4335505882041621, 13.713965204066742, 1.1026310517332243, 2.390108209626137, 12.347405946097439, 1.371620583840687, 1.5546795050134716, 2.1880683431851486, 1.3832227119710072, 1.371620583840687, 1.5936640297164983, 1.4324773890770506, 2.1880683431851486, 1.554636922388419, 2.066354732712421, 1.6409167821669677, 2.578631331753054, 7.84638959731352, 2.2844943760173817, 5.719246839691202, 4.7491857090891365, 5.104777374767509, 1.5854803247758606, 2.852523481625524, 1.6215975631453132, 1.5936640297164983, 1.1603281359622302, 34.934486620895896, 3.428344385422842, 1.4324773890770508, 0.8281106778394318, 1.1973531578597063, 2.3397454889491027, 0.8742481279541545, 1.0774479359860019, 1.1943032681909391, 1.0774479359860019, 1.284909284046077, 5.719246839691202, 0.8704170281357733, 2.302625900549903, 28.845047589797502, 2.674087134154137, 1.9602150590365206, 1.975812333099429, 2.3384218356378788, 1.4324773890770508, 1.4206125115140142, 1.1603281359622302, 21.692297815590738, 1.20528922070971, 1.1603281359622302, 2.65138635804643, 1.0774479359860019, 2.1880683431851486, 1.5936640297164983, 1.4324773890770508, 1.371620583840687, 2.674087134154137, 1.975812333099429, 3.244543361680094, 1.914888309803047, 3.9018193540582615, 1.601326229353261, 2.1880683431851486, 1.1603281359622302, 1.371620583840687, 8.319977443489272, 4.275548265033009, 1.975812333099429, 0.6306117275483252, 2.1880683431851486, 2.066354732712421, 2.1880683431851486, 2.674087134154137, 2.674087134154137, 2.5379420427094415, 2.1880683431851486, 1.5936640297164983, 2.1880683431851486, 4.698839315334091, 2.19487086418763, 1.5936640297164983, 2.674087134154137, 8.982755966339218, 6.0522440466039065, 3.937324830663116, 2.4526510149234477, 1.4324773890770508, 7.7165196427694465, 2.1880683431851486, 1.8224131567970592, 6.863559293146329, 2.9777008547494446, 10.080769602170571, 1.0774479359860019, 0.6306117275483252, 1.1603281359622302, 12.178930186112348, 6.518566099539499, 2.1880683431851486, 2.066354732712421, 1.22563847950436, 0.6306117275483252, 1.5936640297164983, 2.1880683431851486, 1.1603281359622302, 1.4753270477221958, 1.1973531578597063, 1.975812333099429, 2.1880683431851486, 4.74809906739866, 11.687890562020673, 2.1880683431851486, 2.1880683431851486, 2.1880683431851486, 2.1880683431851486, 5.949720841304825, 1.4335505882041621, 1.975812333099429, 1.975812333099429, 2.0400403214824365, 6.765423859826195, 7.742587369627828, 1.1603281359622302, 2.1880683431851486, 1.4457666315216737, 1.5936640297164983, 2.558244351790042, 1.4467915814662338, 1.975812333099429, 2.1880683431851486, 2.0400403214824365, 2.1880683431851486, 1.284909284046077, 1.975812333099429, 1.975812333099429, 3.081170413448166, 3.7665531212134065, 3.4539952179200757, 1.4324773890770508, 9.733612990726316, 1.5936640297164983, 1.5584467229225405, 2.333613153530106, 3.6124475478547455, 1.4324773890770508, 0.8704170281357733, 4.989155884090962, 5.133198638252578, 2.0102105198129716, 4.989155884090962, 1.1943032681909391, 1.8657562907749943, 1.0862678066727285, 1.975812333099429, 1.5936640297164983, 2.0400403214824365, 3.395708832450196, 3.8425309979291247, 1.975812333099429, 0.9327093329614633, 1.284909284046077, 1.284909284046077, 3.6935149484385223, 1.8759454297195663, 1.0842343942956825, 2.0922907011070695, 1.7139055405675787, 0.6306117275483252, 2.382546125662317, 3.61530469822875, 0.6306117275483252, 1.1603281359622302, 1.3534282889192037, 1.4847089094840342, 4.305342659250819, 1.0774479359860019, 2.3734530066656547, 2.8220104315211145, 4.088219094583042, 1.5936640297164983, 0.9153085327352488, 1.5936640297164983, 2.4157045302860736, 1.0862678066727285, 1.5936640297164983, 2.200049992151084, 6.2668398575831725, 1.5936640297164983, 1.1472009450031182, 7.825972844027412, 1.0537980002603713, 1.9609672442834158, 3.428344385422842, 1.0774479359860019, 11.779401500595418, 1.5936640297164983, 0.8704170281357733, 5.050053231503503, 4.679273400456304, 1.7661883479067624, 2.513056934368519, 13.513988175889526, 1.3534282889192037, 4.74843121616913, 2.1128503428456833, 4.401944863324574, 3.5785594644556507, 2.3059196634378143, 9.175728546631143, 1.284909284046077, 1.975812333099429, 2.1128503428456833, 2.382546125662317, 2.422292892451853, 2.1128503428456833, 2.274404260489741, 4.848682308151187, 2.3480634861053535, 4.041386382736273, 3.4539952179200757, 4.2111996584455635, 4.768967258268675, 1.6667974300904842, 1.3534282889192037, 1.3534282889192037, 2.320521986616137, 1.06977227882828, 4.768967258268675, 3.744421867936893, 1.3534282889192037, 2.1128503428456833, 1.0774479359860019, 1.7242423710664923, 1.3534282889192037, 2.8493473673956613, 2.5051526544366034, 5.584939273377225, 2.3480634861053535, 4.12527380848572, 1.06977227882828, 1.975812333099429, 1.3098008089911166, 22.907715280104735, 1.0697722788282797, 2.382546125662317,
                2.1888901749616596, 0.8742481279541545, 0.8742481279541545, 3.244543361680094, 0.8742481279541545, 1.1026310517332243, 1.371620583840687, 1.975812333099429, 4.463498941555049, 4.177636916733701, 2.1128503428456833, 1.284909284046077, 1.975812333099429, 1.6013262293532615, 2.1128503428456833, 1.06977227882828, 2.382546125662317, 2.0400403214824365, 1.8711913354546725, 2.5749805221278774, 1.9634200886766269, 0.6306117275483252, 1.3832227119710072, 1.3534282889192037, 1.3716205838406865, 5.390082898590918, 1.3098008089911166, 0.9153085327352488, 2.674087134154137, 0.6306117275483252, 0.8704170281357733, 4.968285852434665, 2.066354732712421, 2.200049992151084, 0.8742481279541545, 2.811859101944171, 1.4807515623443204, 1.0774479359860019, 5.1030419024448195, 0.8742481279541545, 3.6304366709223235, 1.6013262293532615, 1.6880352063659727, 1.476965988062906, 0.6306117275483252, 2.1880683431851486, 1.9362200459007395, 7.161227486822898, 1.0774479359860019, 14.231304311034648, 2.3944615208088478, 3.4539952179200757, 0.8704170281357733, 1.6013262293532615, 2.066354732712421, 2.066354732712421, 2.1880683431851486, 0.6306117275483252, 1.4206125115140142, 1.5652252731761296, 1.1893536875090311, 0.6306117275483252, 1.5936640297164983, 2.200049992151084, 1.6013262293532615, 1.5936640297164983, 1.5936640297164983, 2.1880683431851486, 1.7857804307024283, 1.0774479359860019, 10.29409112952154, 1.975812333099429, 1.7661883479067624, 2.1128503428456833, 2.3480634861053535, 1.6013262293532615, 1.194303268190939, 3.7918550049609947, 0.6306117275483252, 6.180854682098881, 0.6306117275483252, 2.0400403214824365, 0.8742481279541545, 1.695250202546872, 1.975812333099429, 2.1128503428456833, 4.0416542090535135, 1.3131854496587192, 2.9724681617989726, 0.6306117275483253, 0.8742481279541545, 1.6013262293532615, 0.6306117275483252, 6.574973289316958, 5.640639359168244, 1.975812333099429, 2.332860833253399, 4.801036621302534, 4.723502398571744, 1.284909284046077, 1.3534282889192037, 3.8344207573686675, 1.6013262293532615, 1.975812333099429, 1.0774479359860019, 6.971022259204466, 4.097689029907875, 1.3534282889192037, 3.4539952179200757, 2.6224788887082693, 0.6306117275483252, 0.6306117275483252, 9.696921415393732, 1.5825164533292089, 12.677950480840533, 11.119996982053454, 13.504661611319824, 20.546162790256993, 15.7489864660282, 6.953137271892834, 12.104061126479598, 28.06431311468222, 13.820575742854729, 12.6724486225516, 12.362110028376687, 22.096107070824047, 28.67625111559413, 28.47699023916822, 13.597759504856908, 14.700556363123333, 25.113804792711193, 2.025875487803124, 20.629402164731314, 28.435065638458, 19.763546179666953, 8.701600479014214, 30.730117674898082, 14.117360351452616, 17.408673152115476, 10.639713776408566, 15.714601608255782, 33.21798248431422, 14.274641068931906, 44.63472764234434, 27.59098127985281, 42.84292335207326, 68.0635633365362, 21.15481734173104, 12.628808316609717, 24.72079414802643, 51.8022357361955, 9.913959922563363, 8.945165628227384, 48.277891914094674, 12.730803571659074, 39.732742781491986, 6.824609263281786, 19.2518647982011, 37.610129487997796, 16.252892069013825, 33.17678286897131, 26.013991769305875, 10.49919484271381, 12.855412713285416, 25.577207277627792, 31.113729067467645, 28.14741209597795, 25.379060304976974, 21.42301765207297, 25.270413453801957, 14.645562380298289, 17.542961251551713, 22.090206470541442, 3.970790540832069, 10.956231447005173, 19.62958185300255, 16.666665207579754, 18.934986210047438, 11.433912404572029, 16.997758582318276, 7.046367330586009, 19.361472412445398, 60.69908082064885, 17.459309342248545, 11.146002933106915, 18.232909557289545, 26.030323605253813, 13.747581910831114, 8.661382891194714, 11.178490376908643, 22.312560361538946, 36.07331218620473, 25.230131044603727, 12.048926496909713, 38.388114696947056, 5.294723501123709, 27.267998041867678, 13.54782313972014, 19.238794774079015, 14.717924003692982, 12.408093503885837, 8.847988659886415, 21.720977451582222, 11.051955294751425, 17.252818207574535, 35.62042764675301, 3.7790239763303757, 6.292482457739575, 12.974232030110677, 13.870443858482474, 13.671072948293709, 21.289426751809508, 9.62855483400433, 7.2921141625737285, 13.177745890748902, 36.702129614626905, 15.826915152557028, 36.49811774417682, 13.849616283868569, 10.486758338016282, 10.531611220440865, 21.256936286174895, 22.096107070824047, 33.21798248431422, 12.440644495629224, 25.474431832830184, 21.03253593486808, 8.615328062094688, 20.22663663626464, 36.677063315297275, 28.147003371634714, 17.118834495288592, 34.46528584049656, 21.936433948821858, 17.118834495288592, 4.296489003306805, 27.698025364370903, 20.46535486122924, 15.153450103078185, 22.367290151773553, 11.05170576136085, 32.04598906105828, 20.780568073763273, 13.579507378806026, 17.360655207059406, 10.988381669153231, 22.561963791627036, 34.02327485427141, 25.15085403825553, 17.29246616946618, 30.872331519170405, 13.31946473074035, 40.93311378103352, 8.97841276465745, 8.706817304895534, 24.311342518115453, 21.692202016673445, 22.369672153484583, 47.49995060474981, 15.657310080719803, 17.248407876451157, 8.55864610167089, 10.997067174041954, 10.943936954810807, 9.173871281476305, 20.290570513748012, 24.579451955388233, 20.691311758164893, 10.20702911166505, 27.49574207227197, 2.016879969558297, 26.441298972188704, 28.75134207497063, 10.039794086253165, 28.673572934028304, 47.358727675150234, 15.14833109354556, 11.890883508235756, 18.698314875123458, 14.673796778429384, 14.039344467512562, 35.83524216032261, 18.829179206812018, 9.310049627795825, 21.965757215267647, 23.9252209612987, 17.341880497696508, 18.295793624886624, 26.953263865006086, 33.58709244869957, 34.48917477838923, 13.237198103070613, 17.02495284236582, 13.882253720568379, 11.27275187676009, 22.56300127721484, 12.736021724619768, 15.713924687604797, 4.985412931293462, 14.24427322862943, 36.53256939196098, 11.481080599807369, 24.714338061147608, 19.78595726168441, 3.463759090404905, 20.9867668131546, 15.82668303971548, 22.1014492898862, 16.3030583564118, 40.15406221533512, 23.481197622533475, 10.393619221359039, 13.496795844187492, 13.063005316322439, 18.64163574841981, 21.303844600943645, 22.594657094006767, 16.51604117602026, 28.231691249419796, 36.12891279728376, 23.0215843474471, 18.598034906645555, 14.028938042068631, 29.168021968442705, 13.751197383158448, 24.091783757321025, 40.43227286177044};

        int cnt=0;
        for (int i = 0; i < objects.size(); i++)
        {
            for (int j = 0; j < objects.get(i).size(); j++)
            {
                for (int q = 0; q < objects.get(i).get(j).data.length; q++)
                {
                    double dist = Math.abs(val - train_mah[cnt]);
                    cnt++;

                    if (cluster.size() < k)
                    {
                        dists[cluster.size()] = dist;
                        classes[cluster.size()] = i;
                        cluster.add(objects.get(i).get(j).data[k]);
                    }
                    else
                    {
                        double num = 0;
                        int index = 0;

                        for (int w = 0; w < k; w++)
                        {
                            if (dist < dists[w] && (dist - dists[w]) < num)
                            {
                                num = dist - dists[w];
                                index = w;
                            }
                        }

                        if (num < 0)
                        {
                            cluster.set(index, objects.get(i).get(j).data[k]);
                            classes[index] = i;
                            dists[index] = dist;
                        }
                    }
                }
            }
        }

        cluster.add(x);
        ArrayList<ArrayList<double[]>> data_c = new ArrayList<>(java.util.List.of(cluster));

        // create HB
        HyperBlock hb_c = new HyperBlock(data_c);
        hb_c.classNum = classify_hb(classes, hb_c);

        return hb_c;
    }

    private int classify_hb(int[] classes, HyperBlock hb)
    {
        int x = 0, y = 0;

        for (int i = 0; i < classes.length; i++)
        {
            if (classes[i] == 0) x++;
            else y++;
        }

        int[] inside = new int[]{ 0, 0};

        // populate main series
        for (int d = 0; d < objects.size(); d++)
        {
            int lineCnt = 0;

            for (DataObject data : objects.get(d))
            {
                for (int i = 0; i < data.data.length; i++)
                {
                    // start line at (0, 0)
                    XYSeries line = new XYSeries(lineCnt, false, true);
                    boolean within = false;

                    for (int k = 0; k < hb.hyper_block.size(); k++)
                    {
                        boolean within_cur = true;

                        for (int j = 0; j < DV.fieldLength; j++)
                        {
                            if (data.data[i][j] < hb.minimums.get(k)[j] || data.data[i][j] > hb.maximums.get(k)[j])
                            {
                                within_cur = false;
                            }

                            if (j == DV.fieldLength - 1)
                            {
                                if (within_cur)
                                {
                                    within = true;
                                }
                            }
                        }
                    }

                    // add points to lines
                    for (int j = 0; j < DV.fieldLength; j++)
                    {
                        line.add(j, data.data[i][j]);

                        // add endpoint and timeline
                        if (j == DV.fieldLength - 1)
                        {
                            if (within)
                            {
                                inside[d]++;
                                lineCnt++;
                            }
                        }
                    }
                }
            }
        }

        x = inside[0];
        y = inside[1];

        System.out.println("HB Purity: " + (x > y ? (x / (double)(x+y)) : (y / (double)(x+y))));
        return x > y ? 0 : 1;
    }

    private void getOverlapData()
    {
        objects = new ArrayList<>();
        upperObjects = new ArrayList<>();
        lowerObjects = new ArrayList<>();

        ArrayList<double[]> upper = new ArrayList<>();
        ArrayList<double[]> lower = new ArrayList<>();

        // check all classes
        for (int i = 0; i < DV.data.size(); i++)
        {
            if (i == DV.upperClass)
            {
                for (int j = 0; j < DV.data.get(i).coordinates.length; j++)
                {
                    double endpoint = DV.data.get(i).coordinates[j][DV.data.get(i).coordinates[j].length-1][0];

                    // if endpoint is within overlap then store point
                    if ((DV.overlapArea[0] <= endpoint && endpoint <= DV.overlapArea[1]) && ((DV.domainArea[0] <= endpoint && endpoint <= DV.domainArea[1]) || !DV.domainActive))
                    {
                        double[] thisPoint = new double[DV.data.get(i).coordinates[j].length];
                        System.arraycopy(DV.data.get(i).data[j], 0, thisPoint, 0, DV.data.get(i).coordinates[j].length);

                        upper.add(thisPoint);
                    }
                }
            }
            else if (DV.lowerClasses.get(i))
            {
                for (int j = 0; j < DV.data.get(i).coordinates.length; j++)
                {
                    double endpoint = DV.data.get(i).coordinates[j][DV.data.get(i).coordinates[j].length-1][0];

                    // if endpoint is within overlap then store point
                    if ((DV.overlapArea[0] <= endpoint && endpoint <= DV.overlapArea[1]) && ((DV.domainArea[0] <= endpoint && endpoint <= DV.domainArea[1]) || !DV.domainActive))
                    {
                        double[] thisPoint = new double[DV.data.get(i).coordinates[j].length];
                        System.arraycopy(DV.data.get(i).data[j], 0, thisPoint, 0, DV.data.get(i).coordinates[j].length);

                        lower.add(thisPoint);
                    }
                }
            }
        }

        double[][] upperData = new double[upper.size()][DV.fieldLength];
        upper.toArray(upperData);
        DataObject upperObj = new DataObject("upper", upperData);
        upperObj.updateCoordinatesGLC(DV.angles);
        upperObjects.add(upperObj);

        double[][] lowerData = new double[lower.size()][DV.fieldLength];
        lower.toArray(lowerData);
        DataObject lowerObj = new DataObject("lower", lowerData);
        lowerObj.updateCoordinatesGLC(DV.angles);
        lowerObjects.add(lowerObj);

        objects.add(upperObjects);
        objects.add(lowerObjects);

        DV.data.clear();
        DV.data.add(upperObj);
        DV.data.add(lowerObj);
    }

    private void getNonOverlapData()
    {
        objects = new ArrayList<>();
        upperObjects = new ArrayList<>();
        lowerObjects = new ArrayList<>();

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
                    double endpoint = DV.data.get(i).coordinates[j][DV.data.get(i).coordinates[j].length-1][0];

                    // if endpoint is outside of overlap then store point
                    if ((DV.overlapArea[0] > endpoint || endpoint > DV.overlapArea[1]) && ((DV.domainArea[0] <= endpoint && endpoint <= DV.domainArea[1]) || !DV.domainActive))
                    {
                        double[] thisPoint = new double[DV.data.get(i).coordinates[j].length];
                        System.arraycopy(DV.data.get(i).data[j], 0, thisPoint, 0, DV.data.get(i).coordinates[j].length);

                        upper.add(thisPoint);
                    }
                }
            }
            else if (DV.lowerClasses.get(i))
            {
                for (int j = 0; j < DV.data.get(i).coordinates.length; j++)
                {
                    double endpoint = DV.data.get(i).coordinates[j][DV.data.get(i).coordinates[j].length-1][0];

                    // if endpoint is outside of overlap then store point
                    if ((DV.overlapArea[0] > endpoint || endpoint > DV.overlapArea[1]) && ((DV.domainArea[0] <= endpoint && endpoint <= DV.domainArea[1]) || !DV.domainActive))
                    {
                        double[] thisPoint = new double[DV.data.get(i).coordinates[j].length];
                        System.arraycopy(DV.data.get(i).data[j], 0, thisPoint, 0, DV.data.get(i).coordinates[j].length);

                        lower.add(thisPoint);
                    }
                }
            }
        }

        double[][] upperData = new double[upper.size()][DV.fieldLength];
        upper.toArray(upperData);
        DataObject upperObj = new DataObject("upper", upperData);
        upperObj.updateCoordinatesGLC(DV.angles);
        upperObjects.add(upperObj);

        double[][] lowerData = new double[lower.size()][DV.fieldLength];
        lower.toArray(lowerData);
        DataObject lowerObj = new DataObject("lower", lowerData);
        lowerObj.updateCoordinatesGLC(DV.angles);
        lowerObjects.add(lowerObj);

        objects.add(upperObjects);
        objects.add(lowerObjects);

        DV.data.clear();
        DV.data.add(upperObj);
        DV.data.add(lowerObj);
    }

    private void getData()
    {
        objects = new ArrayList<>();
        upperObjects = new ArrayList<>(List.of(DV.data.get(DV.upperClass)));
        lowerObjects = new ArrayList<>();

        // get classes to be graphed
        if (DV.hasClasses)
        {
            for (int j = 0; j < DV.classNumber; j++)
            {
                if (DV.lowerClasses.get(j))
                    lowerObjects.add(DV.data.get(j));
            }
        }

        objects.add(upperObjects);
        objects.add(lowerObjects);
    }

    private void add_test_data()
    {
        objects = new ArrayList<>();

        upperObjects.add(DV.testData.get(DV.upperClass));

        if (DV.hasClasses)
        {
            for (int j = 0; j < DV.classNumber; j++)
            {
                if (DV.lowerClasses.get(j))
                    lowerObjects.add(DV.testData.get(j));
            }
        }

        objects.add(upperObjects);
        objects.add(lowerObjects);
    }


    /**
     *                          *
     * ------------------------ *
     * UNSTABLE FUNCTIONS BELOW *
     * ------------------------ *
     *                          *
     */



    //
    ////
    ////// DATA GENERALIZATION TESTING
    ////
    //
    private ArrayList<double[]> create_averages(int num)
    {
        ArrayList<double[]> stuff = new ArrayList<>();

        double avgs = hyper_blocks.get(num).hyper_block.get(0).size() / 30.0;
        boolean more = false;

        if (avgs >= 1)
            more = true;

        if (more)
        {
            double[] avg1 = new double[DV.fieldLength];
            double[] avg2 = new double[DV.fieldLength];
            double[] avg3 = new double[DV.fieldLength];

            Arrays.fill(avg1, 0);
            Arrays.fill(avg2, 0);
            Arrays.fill(avg3, 0);

            int upper = 0;
            int lower = 0;
            int middle = 0;

            double[] low = new double[DV.fieldLength];
            double[] up = new double[DV.fieldLength];
            boolean[] same = new boolean[DV.fieldLength];

            Arrays.fill(low, 0);
            Arrays.fill(up, 0);
            Arrays.fill(same, false);

            for (int k = 0; k < DV.fieldLength; k++)
            {
                double range = hyper_blocks.get(num).maximums.get(0)[k] - hyper_blocks.get(num).minimums.get(0)[k];
                double split = range * (1.0/3.0);

                up[k] = split * 2;
                low[k] = split;

                if (range < 0.1)
                    same[k] = true;
            }

            for (int j = 0; j < hyper_blocks.get(num).hyper_block.get(0).size(); j++)
            {
                for (int k = 0; k < hyper_blocks.get(num).hyper_block.get(0).get(j).length; k++)
                {
                    if (same[k])
                    {
                        avg1[k] += hyper_blocks.get(num).hyper_block.get(0).get(j)[k];
                        lower++;
                        avg2[k] += hyper_blocks.get(num).hyper_block.get(0).get(j)[k];
                        middle++;
                        avg3[k] += hyper_blocks.get(num).hyper_block.get(0).get(j)[k];
                        upper++;
                    }
                    else if (hyper_blocks.get(num).hyper_block.get(0).get(j)[k] > up[k]) // upper
                    {
                        avg3[k] += hyper_blocks.get(num).hyper_block.get(0).get(j)[k];
                        upper++;
                    }
                    else if (hyper_blocks.get(num).hyper_block.get(0).get(j)[k] < low[k]) // lower
                    {
                        avg1[k] += hyper_blocks.get(num).hyper_block.get(0).get(j)[k];
                        lower++;
                    }
                    else // middle
                    {
                        avg2[k] += hyper_blocks.get(num).hyper_block.get(0).get(j)[k];
                        middle++;
                    }
                }
            }

            for (int j = 0; j < hyper_blocks.get(num).hyper_block.get(0).get(0).length; j++)
            {
                avg1[j] /= lower;
                avg2[j] /= middle;
                avg3[j] /= upper;
            }

            // find data closest to each average
            // euclidean distance

            double[] real1 = new double[DV.fieldLength];
            double[] real2 = new double[DV.fieldLength];
            double[] real3 = new double[DV.fieldLength];

            double dist1 = Double.MAX_VALUE;
            double dist2 = Double.MAX_VALUE;
            double dist3 = Double.MAX_VALUE;

            for (int j = 0; j < hyper_blocks.get(num).hyper_block.get(0).size(); j++)
            {
                if (dist1 > euclidean_distance(avg1, hyper_blocks.get(num).hyper_block.get(0).get(j)))
                    real1 = hyper_blocks.get(num).hyper_block.get(0).get(j);

                if (dist2 > euclidean_distance(avg2, hyper_blocks.get(num).hyper_block.get(0).get(j)))
                    real2 = hyper_blocks.get(num).hyper_block.get(0).get(j);

                if (dist3 > euclidean_distance(avg3, hyper_blocks.get(num).hyper_block.get(0).get(j)))
                    real3 = hyper_blocks.get(num).hyper_block.get(0).get(j);
            }

            stuff.add(real1);
            stuff.add(real2);
            stuff.add(real3);
        }
        else
        {
            double[] avg = new double[DV.fieldLength];

            Arrays.fill(avg, 0);

            for (int j = 0; j < hyper_blocks.get(num).hyper_block.get(0).size(); j++)
            {
                for (int k = 0; k < hyper_blocks.get(num).hyper_block.get(0).get(j).length; k++)
                {
                    avg[k] += hyper_blocks.get(num).hyper_block.get(0).get(j)[k];
                }
            }

            for (int j = 0; j < hyper_blocks.get(num).hyper_block.get(0).get(0).length; j++)
                avg[j] /= hyper_blocks.get(num).hyper_block.get(0).size();

            double[] real = new double[DV.fieldLength];

            double dist = Double.MAX_VALUE;

            for (int j = 0; j < hyper_blocks.get(num).hyper_block.get(0).size(); j++)
            {
                if (dist > euclidean_distance(avg, hyper_blocks.get(num).hyper_block.get(0).get(j)))
                    real = hyper_blocks.get(num).hyper_block.get(0).get(j);
            }

            stuff.add(real);
        }

        return stuff;
    }

    private ArrayList<double[]> create_averages_more_detailed(int num)
    {
        ArrayList<double[]> stuff = new ArrayList<>();

        double avgs = hyper_blocks.get(num).hyper_block.get(0).size() / 20.0;

        if (avgs >= 1)
        {
            double[] avg1 = new double[DV.fieldLength];
            double[] avg2 = new double[DV.fieldLength];
            double[] avg3 = new double[DV.fieldLength];
            double[] avg4 = new double[DV.fieldLength];
            double[] avg5 = new double[DV.fieldLength];

            Arrays.fill(avg1, 0);
            Arrays.fill(avg2, 0);
            Arrays.fill(avg3, 0);
            Arrays.fill(avg4, 0);
            Arrays.fill(avg5, 0);

            int upperupper = 0;
            int upper = 0;
            int lower = 0;
            int lowerlower = 0;
            int bottom = 0;

            double[] low = new double[DV.fieldLength];
            double[] lowlow = new double[DV.fieldLength];
            double[] up = new double[DV.fieldLength];
            double[] upup = new double[DV.fieldLength];
            boolean[] same = new boolean[DV.fieldLength];

            Arrays.fill(low, 0);
            Arrays.fill(lowlow, 0);
            Arrays.fill(up, 0);
            Arrays.fill(upup, 0);
            Arrays.fill(same, false);

            for (int k = 0; k < DV.fieldLength; k++)
            {
                double range = hyper_blocks.get(num).maximums.get(0)[k] - hyper_blocks.get(num).minimums.get(0)[k];
                double split = range * (1.0/5.0);
                System.out.println("HB" + num + " SPLIT: " + split + " K -> " + k);

                upup[k] = split * 4;
                up[k] = split * 3;
                low[k] = split * 2;
                lowlow[k] = split;

                if (range < 0.1)
                    same[k] = true;
            }

            for (int j = 0; j < hyper_blocks.get(num).hyper_block.get(0).size(); j++)
            {
                for (int k = 0; k < hyper_blocks.get(num).hyper_block.get(0).get(j).length; k++)
                {
                    if (same[k])
                    {
                        avg1[k] += hyper_blocks.get(num).hyper_block.get(0).get(j)[k];
                        avg2[k] += hyper_blocks.get(num).hyper_block.get(0).get(j)[k];
                        avg3[k] += hyper_blocks.get(num).hyper_block.get(0).get(j)[k];
                        avg4[k] += hyper_blocks.get(num).hyper_block.get(0).get(j)[k];
                        avg5[k] += hyper_blocks.get(num).hyper_block.get(0).get(j)[k];
                        lowerlower++;
                        lower++;
                        bottom++;
                        upper++;
                        upperupper++;
                    }
                    else if (hyper_blocks.get(num).hyper_block.get(0).get(j)[k] > upup[k]) // upper
                    {
                        avg5[k] += hyper_blocks.get(num).hyper_block.get(0).get(j)[k];
                        upperupper++;
                    }
                    else if (hyper_blocks.get(num).hyper_block.get(0).get(j)[k] > up[k]) // upper
                    {
                        avg4[k] += hyper_blocks.get(num).hyper_block.get(0).get(j)[k];
                        upper++;
                    }
                    else if (hyper_blocks.get(num).hyper_block.get(0).get(j)[k] > low[k]) // lower
                    {
                        avg3[k] += hyper_blocks.get(num).hyper_block.get(0).get(j)[k];
                        lower++;
                    }
                    else if (hyper_blocks.get(num).hyper_block.get(0).get(j)[k] > lowlow[k]) // lower
                    {
                        avg2[k] += hyper_blocks.get(num).hyper_block.get(0).get(j)[k];
                        lowerlower++;
                    }
                    else // bottom
                    {
                        avg1[k] += hyper_blocks.get(num).hyper_block.get(0).get(j)[k];
                        bottom++;
                    }
                }
            }

            for (int j = 0; j < hyper_blocks.get(num).hyper_block.get(0).get(0).length; j++)
            {
                avg1[j] /= bottom;
                avg2[j] /= lowerlower;
                avg3[j] /= lower;
                avg4[j] /= upper;
                avg5[j] /= upperupper;
            }

            // find data closest to each average
            // euclidean distance

            double[] real1 = new double[DV.fieldLength];
            double[] real2 = new double[DV.fieldLength];
            double[] real3 = new double[DV.fieldLength];
            double[] real4 = new double[DV.fieldLength];
            double[] real5 = new double[DV.fieldLength];

            double dist1 = Double.MAX_VALUE;
            double dist2 = Double.MAX_VALUE;
            double dist3 = Double.MAX_VALUE;
            double dist4 = Double.MAX_VALUE;
            double dist5 = Double.MAX_VALUE;

            for (int j = 0; j < hyper_blocks.get(num).hyper_block.get(0).size(); j++)
            {
                if (dist1 > euclidean_distance(avg1, hyper_blocks.get(num).hyper_block.get(0).get(j)))
                    real1 = hyper_blocks.get(num).hyper_block.get(0).get(j);

                if (dist2 > euclidean_distance(avg2, hyper_blocks.get(num).hyper_block.get(0).get(j)))
                    real2 = hyper_blocks.get(num).hyper_block.get(0).get(j);

                if (dist3 > euclidean_distance(avg3, hyper_blocks.get(num).hyper_block.get(0).get(j)))
                    real3 = hyper_blocks.get(num).hyper_block.get(0).get(j);

                if (dist4 > euclidean_distance(avg4, hyper_blocks.get(num).hyper_block.get(0).get(j)))
                    real4 = hyper_blocks.get(num).hyper_block.get(0).get(j);

                if (dist5 > euclidean_distance(avg5, hyper_blocks.get(num).hyper_block.get(0).get(j)))
                    real5 = hyper_blocks.get(num).hyper_block.get(0).get(j);
            }

            stuff.add(real1);
            stuff.add(real2);
            stuff.add(real3);
            stuff.add(real4);
            stuff.add(real5);
        }
        else
        {
            double[] avg = new double[DV.fieldLength];

            Arrays.fill(avg, 0);

            for (int j = 0; j < hyper_blocks.get(num).hyper_block.get(0).size(); j++)
            {
                for (int k = 0; k < hyper_blocks.get(num).hyper_block.get(0).get(j).length; k++)
                {
                    avg[k] += hyper_blocks.get(num).hyper_block.get(0).get(j)[k];
                }
            }

            for (int j = 0; j < hyper_blocks.get(num).hyper_block.get(0).get(0).length; j++)
                avg[j] /= hyper_blocks.get(num).hyper_block.get(0).size();

            double[] real = new double[DV.fieldLength];

            double dist = Double.MAX_VALUE;

            for (int j = 0; j < hyper_blocks.get(num).hyper_block.get(0).size(); j++)
            {
                if (dist > euclidean_distance(avg, hyper_blocks.get(num).hyper_block.get(0).get(j)))
                    real = hyper_blocks.get(num).hyper_block.get(0).get(j);
            }

            stuff.add(real);
        }

        return stuff;
    }

    /*private ArrayList<double[]> create_averages_more_detailed_and_lossless(int num)
    {
        ArrayList<double[]> stuff = new ArrayList<>();

        double avgs = hyper_blocks.get(num).hyper_block.get(0).size() / 20.0;

        if (avgs >= 1)
        {
            double[] avg1 = new double[DV.fieldLength];
            double[] avg2 = new double[DV.fieldLength];
            double[] avg3 = new double[DV.fieldLength];
            double[] avg4 = new double[DV.fieldLength];
            double[] avg5 = new double[DV.fieldLength];

            Arrays.fill(avg1, 0);
            Arrays.fill(avg2, 0);
            Arrays.fill(avg3, 0);
            Arrays.fill(avg4, 0);
            Arrays.fill(avg5, 0);

            int upperupper = 0;
            int upper = 0;
            int lower = 0;
            int lowerlower = 0;
            int bottom = 0;

            double[] low = new double[DV.fieldLength];
            double[] lowlow = new double[DV.fieldLength];
            double[] up = new double[DV.fieldLength];
            double[] upup = new double[DV.fieldLength];
            boolean[] same = new boolean[DV.fieldLength];

            Arrays.fill(low, 0);
            Arrays.fill(lowlow, 0);
            Arrays.fill(up, 0);
            Arrays.fill(upup, 0);
            Arrays.fill(same, false);

            for (int k = 0; k < DV.fieldLength; k++)
            {
                double range = hyper_blocks.get(num).maximums.get(0)[k] - hyper_blocks.get(num).minimums.get(0)[k];
                double split = range * (1.0/5.0);
                System.out.println("HB" + num + " SPLIT: " + split + " K -> " + k);

                upup[k] = split * 4;
                up[k] = split * 3;
                low[k] = split * 2;
                lowlow[k] = split;

                if (range < 0.1)
                    same[k] = true;
            }

            for (int j = 0; j < hyper_blocks.get(num).hyper_block.get(0).size(); j++)
            {
                for (int k = 0; k < hyper_blocks.get(num).hyper_block.get(0).get(j).length; k++)
                {
                    if (same[k])
                    {
                        avg1[k] += hyper_blocks.get(num).hyper_block.get(0).get(j)[k];
                        avg2[k] += hyper_blocks.get(num).hyper_block.get(0).get(j)[k];
                        avg3[k] += hyper_blocks.get(num).hyper_block.get(0).get(j)[k];
                        avg4[k] += hyper_blocks.get(num).hyper_block.get(0).get(j)[k];
                        avg5[k] += hyper_blocks.get(num).hyper_block.get(0).get(j)[k];
                        lowerlower++;
                        lower++;
                        bottom++;
                        upper++;
                        upperupper++;
                    }
                    else if (hyper_blocks.get(num).hyper_block.get(0).get(j)[k] > upup[k]) // upper
                    {
                        avg5[k] += hyper_blocks.get(num).hyper_block.get(0).get(j)[k];
                        upperupper++;
                    }
                    else if (hyper_blocks.get(num).hyper_block.get(0).get(j)[k] > up[k]) // upper
                    {
                        avg4[k] += hyper_blocks.get(num).hyper_block.get(0).get(j)[k];
                        upper++;
                    }
                    else if (hyper_blocks.get(num).hyper_block.get(0).get(j)[k] > low[k]) // lower
                    {
                        avg3[k] += hyper_blocks.get(num).hyper_block.get(0).get(j)[k];
                        lower++;
                    }
                    else if (hyper_blocks.get(num).hyper_block.get(0).get(j)[k] > lowlow[k]) // lower
                    {
                        avg2[k] += hyper_blocks.get(num).hyper_block.get(0).get(j)[k];
                        lowerlower++;
                    }
                    else // bottom
                    {
                        avg1[k] += hyper_blocks.get(num).hyper_block.get(0).get(j)[k];
                        bottom++;
                    }
                }
            }

            for (int j = 0; j < hyper_blocks.get(num).hyper_block.get(0).get(0).length; j++)
            {
                avg1[j] /= bottom;
                avg2[j] /= lowerlower;
                avg3[j] /= lower;
                avg4[j] /= upper;
                avg5[j] /= upperupper;
            }

            // find data closest to each average
            // euclidean distance

            double[] real1 = new double[DV.fieldLength];
            double[] real2 = new double[DV.fieldLength];
            double[] real3 = new double[DV.fieldLength];
            double[] real4 = new double[DV.fieldLength];
            double[] real5 = new double[DV.fieldLength];

            double[] dist1 = new double[DV.fieldLength];
            double[] dist2 = new double[DV.fieldLength];
            double[] dist3 = new double[DV.fieldLength];
            double[] dist4 = new double[DV.fieldLength];
            double[] dist5 = new double[DV.fieldLength];

            for (int i = 0; i < DV.fieldLength; i++)
            {
                dist1[i] = Double.MAX_VALUE;
                dist2[i] = Double.MAX_VALUE;
                dist3[i] = Double.MAX_VALUE;
                dist4[i] = Double.MAX_VALUE;
                dist5[i] = Double.MAX_VALUE;
            }

            for (int j = 0; j < hyper_blocks.get(num).hyper_block.get(0).size(); j++)
            {
                boolean test1 = true;
                double[] result1 = lossless_distance(avg1, hyper_blocks.get(num).hyper_block.get(0).get(j));

                for (int k = 0; k < DV.fieldLength; k++)
                {
                    if (result1[k] > dist1[k])
                        test1 = false;
                }

                if (test1)
                    real1 = hyper_blocks.get(num).hyper_block.get(0).get(j);

                boolean test2 = true;
                double[] result2 = lossless_distance(avg2, hyper_blocks.get(num).hyper_block.get(0).get(j));

                for (int k = 0; k < DV.fieldLength; k++)
                {
                    if (result2[k] > dist2[k])
                        test2 = false;
                }

                if (test2)
                    real2 = hyper_blocks.get(num).hyper_block.get(0).get(j);

                boolean test3 = true;
                double[] result3 = lossless_distance(avg3, hyper_blocks.get(num).hyper_block.get(0).get(j));

                for (int k = 0; k < DV.fieldLength; k++)
                {
                    if (result3[k] > dist3[k])
                        test3 = false;
                }

                if (test3)
                    real3 = hyper_blocks.get(num).hyper_block.get(0).get(j);

                boolean test4 = true;
                double[] result4 = lossless_distance(avg4, hyper_blocks.get(num).hyper_block.get(0).get(j));

                for (int k = 0; k < DV.fieldLength; k++)
                {
                    if (result4[k] > dist4[k])
                        test4 = false;
                }

                if (test4)
                    real4 = hyper_blocks.get(num).hyper_block.get(0).get(j);

                boolean test5 = true;
                double[] result5 = lossless_distance(avg5, hyper_blocks.get(num).hyper_block.get(0).get(j));

                for (int k = 0; k < DV.fieldLength; k++)
                {
                    if (result5[k] > dist5[k])
                        test5 = false;
                }

                if (test5)
                    real5 = hyper_blocks.get(num).hyper_block.get(0).get(j);
            }

            stuff.add(real1);
            stuff.add(real2);
            stuff.add(real3);
            stuff.add(real4);
            stuff.add(real5);
        }
        else
        {
            double[] avg = new double[DV.fieldLength];

            Arrays.fill(avg, 0);

            for (int j = 0; j < hyper_blocks.get(num).hyper_block.get(0).size(); j++)
            {
                for (int k = 0; k < hyper_blocks.get(num).hyper_block.get(0).get(j).length; k++)
                {
                    avg[k] += hyper_blocks.get(num).hyper_block.get(0).get(j)[k];
                }
            }

            for (int j = 0; j < hyper_blocks.get(num).hyper_block.get(0).get(0).length; j++)
                avg[j] /= hyper_blocks.get(num).hyper_block.get(0).size();

            double[] real = new double[DV.fieldLength];

            double[] dist = new double[DV.fieldLength];

            for (int i = 0; i < DV.fieldLength; i++)
            {
                dist[i] = Double.MAX_VALUE;
            }

            for (int j = 0; j < hyper_blocks.get(num).hyper_block.get(0).size(); j++)
            {
                boolean test1 = true;
                double[] result1 = lossless_distance(avg, hyper_blocks.get(num).hyper_block.get(0).get(j));

                for (int k = 0; k < DV.fieldLength; k++)
                {
                    if (result1[k] > dist[k])
                        test1 = false;
                }

                if (test1)
                    real = hyper_blocks.get(num).hyper_block.get(0).get(j);
            }

            stuff.add(real);
        }

        return stuff;
    }*/

    /*private ArrayList<double[]> create_averages_lossless_distance(int num)
    {
        ArrayList<double[]> stuff = new ArrayList<>();

        double avgs = hyper_blocks.get(num).hyper_block.get(0).size() / 30.0;
        boolean more = false;

        if (avgs >= 1)
            more = true;

        if (more)
        {
            double[] avg1 = new double[DV.fieldLength];
            double[] avg2 = new double[DV.fieldLength];
            double[] avg3 = new double[DV.fieldLength];

            Arrays.fill(avg1, 0);
            Arrays.fill(avg2, 0);
            Arrays.fill(avg3, 0);

            int upper = 0;
            int lower = 0;
            int middle = 0;

            double[] low = new double[DV.fieldLength];
            double[] up = new double[DV.fieldLength];
            boolean[] same = new boolean[DV.fieldLength];

            Arrays.fill(low, 0);
            Arrays.fill(up, 0);
            Arrays.fill(same, false);

            for (int k = 0; k < DV.fieldLength; k++)
            {
                double range = hyper_blocks.get(num).maximums.get(0)[k] - hyper_blocks.get(num).minimums.get(0)[k];
                double split = range * (1.0/3.0);

                up[k] = split * 2;
                low[k] = split;

                if (range < 0.1)
                    same[k] = true;
            }

            for (int j = 0; j < hyper_blocks.get(num).hyper_block.get(0).size(); j++)
            {
                for (int k = 0; k < hyper_blocks.get(num).hyper_block.get(0).get(j).length; k++)
                {
                    if (same[k])
                    {
                        avg1[k] += hyper_blocks.get(num).hyper_block.get(0).get(j)[k];
                        lower++;
                        avg2[k] += hyper_blocks.get(num).hyper_block.get(0).get(j)[k];
                        middle++;
                        avg3[k] += hyper_blocks.get(num).hyper_block.get(0).get(j)[k];
                        upper++;
                    }
                    else if (hyper_blocks.get(num).hyper_block.get(0).get(j)[k] > up[k]) // upper
                    {
                        avg3[k] += hyper_blocks.get(num).hyper_block.get(0).get(j)[k];
                        upper++;
                    }
                    else if (hyper_blocks.get(num).hyper_block.get(0).get(j)[k] < low[k]) // lower
                    {
                        avg1[k] += hyper_blocks.get(num).hyper_block.get(0).get(j)[k];
                        lower++;
                    }
                    else // middle
                    {
                        avg2[k] += hyper_blocks.get(num).hyper_block.get(0).get(j)[k];
                        middle++;
                    }
                }
            }

            for (int j = 0; j < hyper_blocks.get(num).hyper_block.get(0).get(0).length; j++)
            {
                avg1[j] /= lower;
                avg2[j] /= middle;
                avg3[j] /= upper;
            }

            // find data closest to each average
            // euclidean distance

            double[] real1 = new double[DV.fieldLength];
            double[] real2 = new double[DV.fieldLength];
            double[] real3 = new double[DV.fieldLength];

            double[] dist1 = new double[DV.fieldLength];
            double[] dist2 = new double[DV.fieldLength];
            double[] dist3 = new double[DV.fieldLength];

            for (int i = 0; i < DV.fieldLength; i++)
            {
                dist1[i] = Double.MAX_VALUE;
                dist2[i] = Double.MAX_VALUE;
                dist3[i] = Double.MAX_VALUE;
            }

            for (int j = 0; j < hyper_blocks.get(num).hyper_block.get(0).size(); j++)
            {
                boolean test1 = true;
                double[] result1 = lossless_distance(avg1, hyper_blocks.get(num).hyper_block.get(0).get(j));

                for (int k = 0; k < DV.fieldLength; k++)
                {
                    if (result1[k] > dist1[k])
                        test1 = false;
                }

                if (test1)
                    real1 = hyper_blocks.get(num).hyper_block.get(0).get(j);

                boolean test2 = true;
                double[] result2 = lossless_distance(avg1, hyper_blocks.get(num).hyper_block.get(0).get(j));

                for (int k = 0; k < DV.fieldLength; k++)
                {
                    if (result2[k] > dist2[k])
                        test2 = false;
                }

                if (test2)
                    real2 = hyper_blocks.get(num).hyper_block.get(0).get(j);

                boolean test3 = true;
                double[] result3 = lossless_distance(avg1, hyper_blocks.get(num).hyper_block.get(0).get(j));

                for (int k = 0; k < DV.fieldLength; k++)
                {
                    if (result3[k] > dist3[k])
                        test3 = false;
                }

                if (test3)
                    real3 = hyper_blocks.get(num).hyper_block.get(0).get(j);
            }

            stuff.add(real1);
            stuff.add(real2);
            stuff.add(real3);
        }
        else
        {
            double[] avg = new double[DV.fieldLength];

            Arrays.fill(avg, 0);

            for (int j = 0; j < hyper_blocks.get(num).hyper_block.get(0).size(); j++)
            {
                for (int k = 0; k < hyper_blocks.get(num).hyper_block.get(0).get(j).length; k++)
                {
                    avg[k] += hyper_blocks.get(num).hyper_block.get(0).get(j)[k];
                }
            }

            for (int j = 0; j < hyper_blocks.get(num).hyper_block.get(0).get(0).length; j++)
                avg[j] /= hyper_blocks.get(num).hyper_block.get(0).size();

            double[] real = new double[DV.fieldLength];

            double[] dist = new double[DV.fieldLength];

            for (int i = 0; i < DV.fieldLength; i++)
            {
                dist[i] = Double.MAX_VALUE;
            }

            for (int j = 0; j < hyper_blocks.get(num).hyper_block.get(0).size(); j++)
            {
                boolean test1 = true;
                double[] result1 = lossless_distance(avg, hyper_blocks.get(num).hyper_block.get(0).get(j));

                for (int k = 0; k < DV.fieldLength; k++)
                {
                    if (result1[k] > dist[k])
                        test1 = false;
                }

                if (test1)
                    real = hyper_blocks.get(num).hyper_block.get(0).get(j);
            }

            stuff.add(real);
        }

        return stuff;
    }*/

    private ArrayList<double[]> find_evelope_cases(int num)
    {
        ArrayList<double[]> stuff = new ArrayList<>();

        boolean[] max = new boolean[DV.fieldLength];
        boolean[] min = new boolean[DV.fieldLength];

        for (int i = 0; i < DV.fieldLength; i++)
        {
            max[i] = true;
            min[i] = true;
        }

        for (int i = 0; i < hyper_blocks.get(num).hyper_block.get(0).size(); i++)
        {
            boolean part = false;

            for (int j = 0; j < DV.fieldLength; j++)
            {
                if (hyper_blocks.get(num).hyper_block.get(0).get(i)[j] == hyper_blocks.get(num).maximums.get(0)[j] && max[j])
                {
                    part = true;
                    max[j] = false;
                }
                else if (hyper_blocks.get(num).hyper_block.get(0).get(i)[j] == hyper_blocks.get(num).minimums.get(0)[j] && min[j])
                {
                    part = true;
                    min[j] = false;
                }
            }

            if (part)
                stuff.add(hyper_blocks.get(num).hyper_block.get(0).get(i));
        }

        return stuff;
    }


    //
    ////
    ////// HB COMBINING TESTING
    ////
    //

    // find HBs that can be combined while maintaining the purity threshold
    /*private void increase_level_combine_check()
    {
        ArrayList<ChartPanel> panels = new ArrayList<>();
        ArrayList<String> names = new ArrayList<>();
        ArrayList<ChartPanel> bad_panels = new ArrayList<>();
        ArrayList<String> bad_names = new ArrayList<>();
        int good = 0;
        int bad = 0;

        // go through every HB combination
        for (int i = 0; i < hyper_blocks.size(); i++)
        {
            for (int j = 0; j < hyper_blocks.size(); j++)
            {
                if (j > i && hyper_blocks.get(i).classNum == hyper_blocks.get(j).classNum)
                {
                    double[] max = new double[DV.fieldLength];
                    double[] min = new double[DV.fieldLength];

                    for (int k = 0; k < DV.fieldLength; k++)
                    {
                        max[k] = Math.max(hyper_blocks.get(i).maximums.get(0)[k], hyper_blocks.get(j).maximums.get(0)[k]);
                        min[k] = Math.min(hyper_blocks.get(i).minimums.get(0)[k], hyper_blocks.get(j).minimums.get(0)[k]);
                    }

                    HyperBlock test = new HyperBlock(max, min);
                    System.out.println("Combined HB" + (i+1) + " + HB" + (j+1));
                    test.classNum = hyper_blocks.get(i).classNum;

                    if (test.hyper_block.size() == 2)
                    {
                        double purity = (double) Math.max(test.hyper_block.get(0).size(), test.hyper_block.get(1).size()) / (test.hyper_block.get(0).size() + test.hyper_block.get(1).size());
                        System.out.println("Purity = " + purity);

                        if (purity >= acc_threshold)
                        {
                            System.out.println("GOOD\n");
                            good++;

                            panels.add(individual_HB_vis(test, "Combined HB" + (i+1) + " + HB" + (j+1) + " -> Purity = " + purity));
                            names.add("<html><b>GOOD</b> -> Combined HB" + (i+1) + " + HB" + (j+1) + " -> Purity = " + purity);
                        }
                        else
                        {
                            System.out.println("BAD\n");
                            bad++;

                            bad_panels.add(individual_HB_vis(test, "Combined HB" + (i+1) + " + HB" + (j+1) + " -> Purity = " + purity));
                            bad_names.add("<html><b>BAD</b> -> Combined HB" + (i+1) + " + HB" + (j+1) + " -> Purity = " + purity);
                        }
                    }
                    else if (test.hyper_block.size() == 1)
                    {
                        System.out.println("Purity = 1");
                        System.out.println("GOOD\n");
                        good++;

                        panels.add(individual_HB_vis(test, "Combined HB" + (i+1) + " + HB" + (j+1) + " -> Purity = 1"));
                        names.add("<html><b>GOOD</b> -> Combined HB" + (i+1) + " + HB" + (j+1) + " -> Purity = 1");
                    }
                    else
                        System.out.println("ERROR\n");
                }
            }
        }

        System.out.println("Combination Results:\nGood = " + good + "\nBad = " + bad);

        test_hb_vis(panels, names, "Combination Results: Good = " + good);
        test_hb_vis(bad_panels, bad_names, "Combination Results: Bad = " + bad);
    }*/
}
