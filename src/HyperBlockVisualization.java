import org.jfree.chart.*;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.DefaultDrawingSupplier;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYAreaRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.RectangleInsets;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.util.*;
import java.util.List;

public class HyperBlockVisualization
{
    JPanel pcGraphPanel;
    JPanel glcGraphPanel;
    JTabbedPane graphTabs;

    JLabel graphLabel;
    JButton right;
    JButton left;
    int visualized_block = 0;

    // hyperblock storage
    ArrayList<HyperBlock> hyper_blocks = new ArrayList<>();
    ArrayList<HyperBlock> pure_blocks = new ArrayList<>();
    ArrayList<String> accuracy = new ArrayList<>();

    // refuse
    ArrayList<double[]> refuse_area = new ArrayList<>();

    // artificial datapoints
    ArrayList<double[]> artificial = new ArrayList<>();

    int totalBlockCnt;
    int overlappingBlockCnt;

    // accuracy threshold for hyperblocks
    double acc_threshold = 0.9;

    double glcBuffer = DV.fieldLength / 10.0;

    JFreeChart pcChart;
    JFreeChart[] glcChart = new JFreeChart[2];

    XYLineAndShapeRenderer[] glcBlockRenderer = new XYLineAndShapeRenderer[2];
    XYSeriesCollection[] glcBlocks = new XYSeriesCollection[2];
    XYAreaRenderer[] glcBlockAreaRenderer = new XYAreaRenderer[2];
    XYSeriesCollection[] glcBlocksArea = new XYSeriesCollection[2];

    XYLineAndShapeRenderer pcBlockRenderer;
    XYSeriesCollection pcBlocks;
    XYAreaRenderer pcBlockAreaRenderer;
    XYSeriesCollection pcBlocksArea;

    XYLineAndShapeRenderer artRenderer;
    XYSeriesCollection artLines;


    public HyperBlockVisualization()
    {
        /*// popup asking for number of folds
        JPanel thresholdAccPanel = new JPanel(new BorderLayout());

        // text panel
        JPanel textPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

        // maximum text field
        JTextField thresholdField = new JTextField();
        thresholdField.setPreferredSize(new Dimension(30, 30));
        textPanel.add(new JLabel("Hyperblock Accuracy Threshold: "));
        thresholdField.setText(Double.toString(acc_threshold));
        textPanel.add(thresholdField);

        // add text panel
        thresholdAccPanel.add(textPanel, BorderLayout.SOUTH);

        int choice = -2;
        // loop until folds are valid or user quits
        while (choice == -2)
        {
            choice = JOptionPane.showConfirmDialog(DV.mainFrame, thresholdAccPanel, "Hyperblock Threshold", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

            if (choice == 0)
            {
                try
                {
                    // get text field values
                    double threshold = Double.parseDouble(thresholdField.getText());

                    if (threshold >= 0 && threshold <= 1)
                    {
                        acc_threshold = threshold;
                    }
                    else
                    {
                        // invalid fold input
                        JOptionPane.showMessageDialog(
                                DV.mainFrame,
                                "Error: input is invalid.\n" +
                                        "Please enter a whole decimal value between 0 and 1.",
                                "Error",
                                JOptionPane.ERROR_MESSAGE);

                        choice = -2;
                    }
                }
                catch (NumberFormatException nfe)
                {
                    JOptionPane.showMessageDialog(DV.mainFrame, "Error: please enter a decimal value between 0 and 1.", "Error", JOptionPane.ERROR_MESSAGE);
                    choice = -2;
                }
            }
            else if (choice == 2 || choice == -1)
            {
                return;
            }
        }*/

        generateHyperblocks3();

        JFrame tmpFrame = new JFrame();
        tmpFrame.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        graphTabs = new JTabbedPane();

        pcGraphPanel = new JPanel();
        pcGraphPanel.setLayout(new BoxLayout(pcGraphPanel, BoxLayout.PAGE_AXIS));
        glcGraphPanel = new JPanel();
        glcGraphPanel.setLayout(new BoxLayout(glcGraphPanel, BoxLayout.PAGE_AXIS));

        graphTabs.add("PC Graph", pcGraphPanel);
        graphTabs.add("GLC-L Graph", glcGraphPanel);

        c.weightx = 1;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        c.ipady = 10;
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        tmpFrame.add(graphTabs, c);

        graphLabel = new JLabel("");
        graphLabel.setFont(graphLabel.getFont().deriveFont(20f));

        String tmp = "<html><b>Block:</b> " + (visualized_block + 1) + "/" + hyper_blocks.size() + "<br/>";
        tmp += "<b>Class:</b> " + hyper_blocks.get(visualized_block).className + "<br/>";
        tmp += "<b>Datapoints:</b> " + hyper_blocks.get(visualized_block).hyper_block.size() + "<br/>";
        //tmp += "<b>Accuracy:</b> " + accuracy.get(visualized_block) + "</html>";

        graphLabel.setText(tmp);

        c.gridy = 1;
        c.weighty = 0;
        tmpFrame.add(graphLabel, c);

        left = new JButton("Previous Block");

        c.weightx = 0.5;
        c.gridy = 2;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        tmpFrame.add(left, c);

        right = new JButton("Next Block");

        c.gridx = 1;
        tmpFrame.add(right, c);

        // holds classes to be graphed
        ArrayList<ArrayList<DataObject>> objects = new ArrayList<>();
        ArrayList<DataObject> upperObjects = new ArrayList<>(List.of(DV.data.get(DV.upperClass)));
        ArrayList<DataObject> lowerObjects = new ArrayList<>();

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

        pcGraphPanel.removeAll();
        pcGraphPanel.add(drawPCBlocks(objects));

        glcGraphPanel.removeAll();
        glcGraphPanel.add(drawGLCBlocks(upperObjects, 0));
        glcGraphPanel.add(drawGLCBlocks(lowerObjects, 1));

        right.addActionListener(e ->
        {
            visualized_block++;

            if (visualized_block > hyper_blocks.size() - 1)
                visualized_block = 0;

            updateBlocks();
        });

        left.addActionListener(e ->
        {
            visualized_block--;

            if (visualized_block < 0)
                visualized_block = hyper_blocks.size() - 1;

            updateBlocks();
        });

        // show
        tmpFrame.setVisible(true);
        tmpFrame.revalidate();
        tmpFrame.pack();
        tmpFrame.repaint();

        blockCheck();
    }

    private void blockCheck()
    {
        int[] counter = new int[hyper_blocks.size()];

        for (int h = 0; h < hyper_blocks.size(); h++)
        {
            for (int i = 0; i < DV.data.size(); i++)
            {
                for (int j = 0; j < DV.data.get(i).data.length; j++)
                {
                    boolean tmp = true;

                    for (int k = 0; k < DV.fieldLength; k++)
                    {
                        if (DV.data.get(i).data[j][k] > hyper_blocks.get(h).maximums[k] || DV.data.get(i).data[j][k] < hyper_blocks.get(h).minimums[k])
                        {
                            tmp = false;
                            break;
                        }
                    }

                    if (tmp)
                        counter[h]++;
                }
            }
        }

        for (int i = 0; i < hyper_blocks.size(); i++)
        {
            System.out.println("Block " + (i + 1) + " Size: " + counter[i]);
        }
    }

    // code taken from VisCanvas2.0 autoCluster function
    // CREDIT LATER
    /*private void generateHyperblocks()
    {
        hyper_blocks.clear();
        ArrayList<HyperBlock> blocks = new ArrayList<>();

        for (int i = 0; i < DV.data.size(); i++)
        {
            // create hyperblock from each datapoint
            for (int j = 0; j < DV.data.get(i).data.length; j++)
            {
                blocks.add(new HyperBlock(new ArrayList<>(List.of(new int[]{i, j})), 0));
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

            int tmpClass = tmp.hyper_block.get(0)[0];

            for (int i = 0; i < blocks.size(); i++)
            {
                int curClass = blocks.get(i).hyper_block.get(0)[0];

                if (tmpClass != curClass)
                    continue;

                ArrayList<Double> maxPoint = new ArrayList<>();
                ArrayList<Double> minPoint = new ArrayList<>();

                // define combined space
                for (int j = 0; j < DV.fieldLength; j++)
                {
                    double newLocalMax = Math.max(tmp.maximums[j], blocks.get(i).maximums[j]);
                    double newLocalMin = Math.min(tmp.minimums[j], blocks.get(i).minimums[j]);

                    maxPoint.add(newLocalMax);
                    minPoint.add(newLocalMin);
                }

                ArrayList<int[]> pointsInSpace = new ArrayList<>();

                for (int j = 0; j < DV.data.size(); j++)
                {
                    for (int k = 0; k < DV.data.get(j).data.length; k++)
                    {
                        boolean withinSpace = true;

                        for (int w = 0; w < DV.fieldLength; w++)
                        {
                            double feature = DV.data.get(j).data[k][w];

                            if (!(feature <= maxPoint.get(w) && feature >= minPoint.get(w)))
                            {
                                withinSpace = false;
                                break;
                            }
                        }

                        if (withinSpace)
                        {
                            pointsInSpace.add(new int[]{j, k});
                        }
                    }
                }

                HashSet<Integer> classCnt = new HashSet<>();

                // check if new space is pure
                for (int[] ints : pointsInSpace)
                {
                    classCnt.add(ints[0]);
                }

                if (classCnt.size() <= 1)
                {
                    actionTaken = true;
                    tmp.hyper_block.clear();
                    tmp.hyper_block.addAll(pointsInSpace);
                    tmp.getBounds();
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

            for (HyperBlock block : blocks)
            {
                // get majority class
                int majorityClass = 0;

                HashMap<Integer, Integer> classCnt = new HashMap<>();

                for (int j = 0; j < block.hyper_block.size(); j++)
                {
                    int curClass = block.hyper_block.get(j)[0];

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
                    double newLocalMax = Math.max(tmp.maximums[j], block.maximums[j]);
                    double newLocalMin = Math.min(tmp.minimums[j], block.minimums[j]);

                    maxPoint.add(newLocalMax);
                    minPoint.add(newLocalMin);
                }

                ArrayList<int[]> pointsInSpace = new ArrayList<>();

                // define all points in combined space
                for (int j = 0; j < DV.data.size(); j++)
                {
                    for (int k = 0; k < DV.data.get(j).data.length; k++)
                    {
                        boolean withinSpace = true;

                        for (int w = 0; w < DV.fieldLength; w++)
                        {
                            double feature = DV.data.get(j).data[k][w];

                            if (!(feature <= maxPoint.get(w) && feature >= minPoint.get(w)))
                            {
                                withinSpace = false;
                                break;
                            }
                        }

                        if (withinSpace)
                        {
                            pointsInSpace.add(new int[]{j, k});
                        }
                    }
                }

                classCnt.clear();

                // check if new space is pure enough
                for (int[] ints : pointsInSpace)
                {
                    int thisClass = ints[0];

                    if (classCnt.containsKey(thisClass))
                    {
                        classCnt.replace(thisClass, classCnt.get(thisClass) + 1);
                    }
                    else
                    {
                        classCnt.put(thisClass, 1);
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
                    double newLocalMax = Math.max(tmp.maximums[j], blocks.get(highestAccIndex).maximums[j]);
                    double newLocalMin = Math.min(tmp.minimums[j], blocks.get(highestAccIndex).minimums[j]);

                    maxPoint.add(newLocalMax);
                    minPoint.add(newLocalMin);
                }

                ArrayList<int[]> pointsInSpace = new ArrayList<>();

                // define all points in combined space
                for (int j = 0; j < DV.data.size(); j++)
                {
                    for (int k = 0; k < DV.data.get(j).data.length; k++)
                    {
                        boolean withinSpace = true;

                        for (int w = 0; w < DV.fieldLength; w++)
                        {
                            double feature = DV.data.get(j).data[k][w];

                            if (!(feature <= maxPoint.get(w) && feature >= minPoint.get(w)))
                            {
                                withinSpace = false;
                                break;
                            }
                        }

                        if (withinSpace)
                        {
                            pointsInSpace.add(new int[]{j, k});
                        }
                    }
                }

                tmp.hyper_block.clear();
                tmp.hyper_block.addAll(pointsInSpace);
                tmp.getBounds();

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

        // count blocks that share instance of data
        overlappingBlockCnt = 0;

        for (int i = 0; i < DV.data.size(); i++)
        {
            for (int j = 0; j < DV.data.get(i).data.length; j++)
            {
                int presentIn = 0;

                for (int k = 0; k < pure_blocks.size(); k++)
                {
                    for (int w = 0; w < pure_blocks.get(k).hyper_block.size(); k++)
                    {
                        if (pure_blocks.get(k).hyper_block.get(w)[0] == i && pure_blocks.get(k).hyper_block.get(w)[1] == j)
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
                int curClass = block.hyper_block.get(j)[0];

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

    private class Pnt
    {
        final int index;
        final double value;

        Pnt(int index, double value)
        {
            this.index = index;
            this.value = value;
        }
    }

    private ArrayList<Pnt> findLargestArea(ArrayList<ArrayList<Pnt>> attributes, int upper, ArrayList<ArrayList<ArrayList<Pnt>>> hb)
    {
        ArrayList<Pnt> global = new ArrayList<>();
        ArrayList<Pnt> ans = new ArrayList<>();
        int global_atr = -1;
        int global_cls = -1;
        int attribute = -1;
        int cls = -1;

        for (int a = 0; a < attributes.size(); a++)
        {
            int start = 0;
            ArrayList<Pnt> tmp = new ArrayList<>();

            for (int i = 0; i < attributes.get(a).size(); i++)
            {
                if (attributes.get(a).get(start).index < upper && attributes.get(a).get(i).index < upper)
                {
                    tmp.add(attributes.get(a).get(i));

                    if (tmp.size() > ans.size())
                    {
                        ans = new ArrayList<>(tmp);
                        attribute = a;
                        cls = 0;
                    }
                }
                else if (attributes.get(a).get(start).index >= upper && attributes.get(a).get(i).index >= upper)
                {
                    tmp.add(attributes.get(a).get(i));

                    if (tmp.size() > ans.size())
                    {
                        ans = new ArrayList<>(tmp);
                        attribute = a;
                        cls = 1;
                    }
                }
                else
                {
                    if (ans.size() > 0)
                    {
                        // interval is not unique
                        if (ans.get(ans.size()-1).value == attributes.get(a).get(i).value)
                        {
                            int offset = 0;
                            int size = ans.size();

                            // remove overlapped attributes
                            for (int k = 0; k < size; k++)
                            {
                                if (ans.get(k - offset).value == attributes.get(a).get(i).value)
                                {
                                    ans.remove(k - offset);
                                    offset++;
                                }
                            }
                        }
                    }
                    else
                    {
                        attribute = -1;
                        cls = -1;
                    }

                    if (ans.size() > 0)
                    {
                        // check if interval overlaps with other hyperblocks
                        for (ArrayList<ArrayList<Pnt>> tmp_pnts : hb)
                        {
                            // get pnt array for attribute
                            ArrayList<Pnt> pnts = tmp_pnts.get(attribute);

                            // count non-unique attributes
                            int non_unique = 0;

                            // search for overlap
                            if (ans.get(0).value >= pnts.get(0).value && ans.get(0).value < pnts.get(pnts.size() - 1).value)
                            {
                                non_unique++;
                            }
                            // interval overlaps below
                            else if (ans.get(ans.size() - 1).value <= pnts.get(pnts.size() - 1).value && ans.get(ans.size() - 1).value > pnts.get(0).value)
                            {
                                non_unique++;
                            }

                            if (non_unique > 0)
                            {
                                ans.clear();
                                attribute = -1;
                                cls = -1;
                                break;
                            }
                        }
                    }

                    if (ans.size() > global.size())
                    {
                        global = new ArrayList<>(ans);
                        global_atr = attribute;
                        global_cls = cls;
                    }

                    start = i;
                    tmp.clear();
                }
            }
        }

        if (ans.size() > global.size())
        {
            global = new ArrayList<>(ans);
            global_atr = attribute;
            global_cls = cls;
        }

        // add attribute and class
        global.add(new Pnt(global_atr, global_cls));
        return global;
    }


    private ArrayList<Pnt> findLargestOverlappedArea(ArrayList<ArrayList<Pnt>> attributes, int upper)
    {
        ArrayList<Pnt> global = new ArrayList<>();
        ArrayList<Pnt> ans = new ArrayList<>();
        int global_atr = -1;
        int global_cls = -1;
        int attribute = -1;
        int cls = -1;

        for (int a = 0; a < attributes.size(); a++)
        {
            int start = 0;
            ArrayList<Pnt> tmp = new ArrayList<>();

            for (int i = 0; i < attributes.get(a).size(); i++)
            {
                if (attributes.get(a).get(start).index < upper && attributes.get(a).get(i).index < upper)
                {
                    tmp.add(attributes.get(a).get(i));

                    if (tmp.size() > ans.size())
                    {
                        ans = new ArrayList<>(tmp);
                        attribute = a;
                        cls = 0;
                    }
                }
                else if (attributes.get(a).get(start).index >= upper && attributes.get(a).get(i).index >= upper)
                {
                    tmp.add(attributes.get(a).get(i));

                    if (tmp.size() > ans.size())
                    {
                        ans = new ArrayList<>(tmp);
                        attribute = a;
                        cls = 1;
                    }
                }
                else
                {
                    if (ans.size() > 0)
                    {
                        // interval is not unique
                        if (ans.get(ans.size()-1).value == attributes.get(a).get(i).value)
                        {
                            // remove overlapped attributes
                            int size = ans.size();
                            int offset = 0;

                            for (int w = 0; w < size; w++)
                            {
                                if (ans.get(w - offset).value == attributes.get(a).get(i).value)
                                {
                                    ans.remove(w - offset);
                                    offset++;
                                }
                            }
                        }
                    }

                    if (ans.size() > global.size())
                    {
                        global = new ArrayList<>(ans);
                        global_atr = attribute;
                        global_cls = cls;
                    }

                    start = i;
                    tmp.clear();
                }
            }
        }

        if (ans.size() > global.size())
        {
            global = new ArrayList<>(ans);
            global_atr = attribute;
            global_cls = cls;
        }

        // add attribute and class
        global.add(new Pnt(global_atr, global_cls));
        return global;
    }


    private void generateHyperblocks3()
    {
        // hyperblocks and hyperblock info
        ArrayList<ArrayList<ArrayList<Pnt>>> hb = new ArrayList<>();
        ArrayList<Integer> hb_a = new ArrayList<>();
        ArrayList<Integer> hb_c = new ArrayList<>();

        // indexes of combined hyperblocks
        ArrayList<Integer> combined_hb = new ArrayList<>();


        // get each element
        ArrayList<ArrayList<Pnt>> attributes = new ArrayList<>();

        for (int k = 0; k < DV.fieldLength; k++)
        {
            ArrayList<Pnt> tmpField = new ArrayList<>();
            int count = 0;

            for (int i = 0; i < DV.data.size(); i++)
            {
                for (int j = 0; j < DV.data.get(i).data.length; j++)
                {
                    tmpField.add(new Pnt(count, DV.data.get(i).data[j][k]));
                    count++;
                }
            }

            attributes.add(tmpField);
        }

        // order attributes by sizes
        for (ArrayList<Pnt> attribute : attributes)
        {
            attribute.sort(Comparator.comparingDouble(o -> o.value));
        }

        while (attributes.get(0).size() > 0)
        {
            // get largest hyperblock for each attribute
            ArrayList<Pnt> area = findLargestArea(attributes, DV.data.get(DV.upperClass).data.length, hb);

            // if hyperblock is unique then add
            if (area.size() > 1)
            {
                // add hyperblock info
                hb_a.add(area.get(area.size() - 1).index);
                hb_c.add((int)area.get(area.size() - 1).value);
                area.remove(area.size() - 1);

                // add hyperblock
                ArrayList<ArrayList<Pnt>> block = new ArrayList<>();

                for (int i = 0; i < DV.fieldLength; i++)
                    block.add(new ArrayList<>());

                for (Pnt pnt : area)
                {
                    for (int i = 0; i < attributes.size(); i++)
                    {
                        int index = pnt.index;

                        for (int k = 0; k < attributes.get(i).size(); k++)
                        {
                            if (index == attributes.get(i).get(k).index)
                            {
                                // add point
                                block.get(i).add(new Pnt(index, attributes.get(i).get(k).value));

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
                boolean combined = false;

                // get longest overlapping interval
                // combine if same class or refuse to classify interval
                ArrayList<Pnt> oa = findLargestOverlappedArea(attributes, DV.data.get(DV.upperClass).data.length);
                int atr = oa.get(oa.size() - 1).index;
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
                        int index = pnt.index;

                        for (int k = 0; k < attributes.get(i).size(); k++)
                        {
                            if (index == attributes.get(i).get(k).index)
                            {
                                // add point
                                block.get(i).add(new Pnt(index, attributes.get(i).get(k).value));

                                // remove point
                                attributes.get(i).remove(k);
                                break;
                            }
                        }
                    }
                }

                // find overlapping hyperblocks
                for (int i = 0; i < hb.size(); i++)
                {
                    // non-unique attributes
                    boolean non_unique = false;

                    // get attribute
                    for (int j = 0; j < hb.get(i).size(); j++)
                    {
                        // search for overlap
                        if (block.get(j).get(0).value >= hb.get(i).get(j).get(0).value && block.get(j).get(0).value < hb.get(i).get(j).get(hb.get(i).get(j).size() - 1).value)
                        {
                            if (j != hb_a.get(i))
                            {
                                non_unique = true;
                                break;
                            }
                        }
                        // interval overlaps below
                        else if (block.get(j).get(block.get(j).size() - 1).value <= hb.get(i).get(j).get(hb.get(i).get(j).size() - 1).value && block.get(j).get(block.get(j).size() - 1).value > hb.get(i).get(j).get(0).value)
                        {
                            if (j != hb_a.get(i))
                            {
                                non_unique = true;
                                break;
                            }
                        }
                    }

                    if (non_unique && cls == hb_c.get(i) && i != hb_a.get(i))
                    {
                        combined = true;

                        // combine
                        combined_hb.add(i);

                        for (int k = 0; k < oa.size(); k++)
                        {
                            for (int w = 0; w < DV.fieldLength; w++)
                            {
                                hb.get(i).get(w).add(block.get(w).get(k));
                            }
                        }

                        break;
                    }
                }

                if (!combined)
                {
                    // refuse to classify
                    refuse_area.add(new double[]{atr, block.get(atr).get(0).value, block.get(atr).get(block.get(atr).size() - 1).value});
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

        // create hyperblocks
        hyper_blocks.clear();

        for (int i = 0; i < hb.size(); i++)
        {
            ArrayList<double[]> temp = new ArrayList<>();
            double[] avg = new double[hb.get(i).size()];

            for (int j = 0; j < hb.get(i).size(); j++)
                avg[j] = 0;

            for (int j = 0; j < hb.get(i).get(0).size(); j++)
            {
                double[] tmp = new double[hb.get(i).size()];

                for (int k = 0; k < hb.get(i).size(); k++)
                {
                    tmp[k] = hb.get(i).get(k).get(j).value;
                    avg[k] += hb.get(i).get(k).get(j).value;
                }

                temp.add(tmp);
            }

            for (int j = 0; j < hb.get(i).size(); j++)
                avg[j] /= hb.get(i).get(0).size();

            artificial.add(avg);

            hyper_blocks.add(new HyperBlock(temp, 0));
            hyper_blocks.get(i).className = DV.data.get(hb_c.get(i)).className;
        }
    }


    private void updateBlocks()
    {
        for (int i = 0; i < hyper_blocks.size(); i++)
        {
            if (i == visualized_block)
            {
                // pc graph
                // turn notify off
                pcChart.setNotify(false);

                pcBlocks.removeAllSeries();
                pcBlocksArea.removeAllSeries();
                artLines.removeAllSeries();

                XYSeries pcOutline = new XYSeries(0, false, true);
                XYSeries pcArea = new XYSeries(0, false, true);
                XYSeries line = new XYSeries(0, false, true);

                for (int j = 0; j < DV.fieldLength; j++)
                {
                    pcOutline.add(j, hyper_blocks.get(i).minimums[j]);
                    pcArea.add(j, hyper_blocks.get(i).minimums[j]);
                }

                for (int j = DV.fieldLength - 1; j > -1; j--)
                {
                    pcOutline.add(j, hyper_blocks.get(i).maximums[j]);
                    pcArea.add(j, hyper_blocks.get(i).maximums[j]);
                }

                for (int j = 0; j < artificial.get(i).length; j++)
                {
                    line.add(j, artificial.get(i)[j]);
                }

                pcOutline.add(0, hyper_blocks.get(i).minimums[0]);
                pcArea.add(0, hyper_blocks.get(i).minimums[0]);
                artLines.addSeries(line);

                pcBlocks.addSeries(pcOutline);
                pcBlocksArea.addSeries(pcArea);
                artRenderer.setSeriesPaint(0, Color.BLACK);

                pcChart.setNotify(true);

                // glc graph
                // turn notify off
                glcChart[0].setNotify(false);
                glcChart[1].setNotify(false);

                glcBlocks[0].removeAllSeries();
                glcBlocks[1].removeAllSeries();
                glcBlocksArea[0].removeAllSeries();
                glcBlocksArea[1].removeAllSeries();

                XYSeries glcOutline = new XYSeries(0, false, true);
                XYSeries glcArea = new XYSeries(0, false, true);

                double[] xyOriginPointMin = DataObject.getXYPointGLC(hyper_blocks.get(i).minimums[0], DV.angles[0]);
                double[] xyOriginPointMax = DataObject.getXYPointGLC(hyper_blocks.get(i).maximums[0], DV.angles[0]);

                double[] xyCurPointMin = Arrays.copyOf(xyOriginPointMin, xyOriginPointMin.length);
                double[] xyCurPointMax = Arrays.copyOf(xyOriginPointMax, xyOriginPointMax.length);

                glcOutline.add(xyOriginPointMin[0], xyOriginPointMin[1] + glcBuffer);
                glcArea.add(xyOriginPointMin[0], xyOriginPointMin[1] + glcBuffer);

                for (int j = 1; j < DV.fieldLength; j++)
                {
                    double[] xyPoint = DataObject.getXYPointGLC(hyper_blocks.get(i).minimums[j], DV.angles[j]);

                    xyCurPointMin[0] = xyCurPointMin[0] + xyPoint[0];
                    xyCurPointMin[1] = xyCurPointMin[1] + xyPoint[1];

                    glcOutline.add(xyCurPointMin[0], xyCurPointMin[1] + glcBuffer);
                    glcArea.add(xyCurPointMin[0], xyCurPointMin[1] + glcBuffer);

                    // get maximums
                    xyPoint = DataObject.getXYPointGLC(hyper_blocks.get(i).maximums[j], DV.angles[j]);

                    xyCurPointMax[0] = xyCurPointMax[0] + xyPoint[0];
                    xyCurPointMax[1] = xyCurPointMax[1] + xyPoint[1];
                }

                glcOutline.add(xyCurPointMax[0], xyCurPointMax[1] + glcBuffer);
                glcArea.add(xyCurPointMax[0], xyCurPointMax[1] + glcBuffer);

                for (int j = DV.fieldLength - 2; j > -1; j--)
                {
                    double[] xyPoint = DataObject.getXYPointGLC(hyper_blocks.get(i).maximums[j], DV.angles[j]);

                    xyCurPointMax[0] = xyCurPointMax[0] - xyPoint[0];
                    xyCurPointMax[1] = xyCurPointMin[1] - xyPoint[1];

                    glcOutline.add(xyCurPointMax[0], xyCurPointMax[1] + glcBuffer);
                    glcArea.add(xyCurPointMax[0], xyCurPointMax[1] + glcBuffer);
                }

                glcOutline.add(xyOriginPointMin[0], xyOriginPointMin[1] + glcBuffer);
                glcArea.add(xyOriginPointMin[0], xyOriginPointMin[1] + glcBuffer);

                glcBlocks[0].addSeries(glcOutline);
                glcBlocks[1].addSeries(glcOutline);
                glcBlocksArea[0].addSeries(glcArea);
                glcBlocksArea[1].addSeries(glcArea);

                glcChart[0].setNotify(true);
                glcChart[1].setNotify(true);

                break;
            }
        }

        String curBlockLabel = "<html><b>Hyperblock:</b> " + (visualized_block + 1) + "/" + hyper_blocks.size() + "<br/>";
        curBlockLabel += "<b>Class:</b> " + hyper_blocks.get(visualized_block).className + "<br/>";
        curBlockLabel += "<b>Datapoints:</b> " + hyper_blocks.get(visualized_block).hyper_block.size() + "<br/>";
        //curBlockLabel += "<b>Accuracy:</b> " + accuracy.get(visualized_block) + "</html>";

        graphLabel.setText(curBlockLabel);
    }

    private ChartPanel drawPCBlocks(ArrayList<ArrayList<DataObject>> obj)
    {
        // create main renderer and dataset
        XYLineAndShapeRenderer lineRenderer = new XYLineAndShapeRenderer(true, false);
        XYSeriesCollection graphLines = new XYSeriesCollection();

        // artificial renderer and dataset
        artRenderer = new XYLineAndShapeRenderer(true, false);
        artLines = new XYSeriesCollection();

        // hyperblock renderer and dataset
        pcBlockRenderer = new XYLineAndShapeRenderer(true, false);
        pcBlocks = new XYSeriesCollection();
        pcBlockAreaRenderer = new XYAreaRenderer(XYAreaRenderer.AREA);
        pcBlocksArea = new XYSeriesCollection();

        // refuse to classify area
        XYLineAndShapeRenderer refuseRenderer = new XYLineAndShapeRenderer(true, false);
        XYSeriesCollection refuse = new XYSeriesCollection();
        XYAreaRenderer refuseAreaRenderer = new XYAreaRenderer(XYAreaRenderer.AREA);
        XYSeriesCollection refuseArea = new XYSeriesCollection();

        // populate main series
        for (int d = 0, lineCnt = -1; d < obj.size(); d++)
        {
            for (DataObject data : obj.get(d))
            {
                for (int i = 0; i < data.data.length; i++)
                {
                    // start line at (0, 0)
                    XYSeries line = new XYSeries(++lineCnt, false, true);

                    // add points to lines
                    for (int j = 0; j < DV.fieldLength; j++)
                    {
                        line.add(j, data.data[i][j]);

                        // add endpoint and timeline
                        if (j == DV.fieldLength - 1)
                        {
                            // add series
                            graphLines.addSeries(line);

                            // set series paint
                            lineRenderer.setSeriesPaint(lineCnt, DV.graphColors[d]);
                        }
                    }
                }
            }
        }

        // populate art series
        for (int d = 0; d < artificial.size(); d++)
        {
            if (d == visualized_block)
            {
                XYSeries line = new XYSeries(0, false, true);

                for (int i = 0; i < artificial.get(d).length; i++)
                {
                    line.add(i, artificial.get(d)[i]);
                }

                artLines.addSeries(line);
                artRenderer.setSeriesPaint(0, Color.BLACK);
            }
        }

        // add hyperblocks
        for (int i = 0; i < hyper_blocks.size(); i++)
        {
            if (i == visualized_block)
            {
                XYSeries tmp1 = new XYSeries(0, false, true);
                XYSeries tmp2 = new XYSeries(0, false, true);

                for (int j = 0; j < DV.fieldLength; j++)
                {
                    tmp1.add(j, hyper_blocks.get(i).minimums[j]);
                    tmp2.add(j, hyper_blocks.get(i).minimums[j]);
                }

                for (int j = DV.fieldLength - 1; j > -1; j--)
                {
                    tmp1.add(j, hyper_blocks.get(i).maximums[j]);
                    tmp2.add(j, hyper_blocks.get(i).maximums[j]);
                }

                tmp1.add(0, hyper_blocks.get(i).minimums[0]);
                tmp2.add(0, hyper_blocks.get(i).minimums[0]);

                pcBlockRenderer.setSeriesPaint(0, Color.ORANGE);
                pcBlockAreaRenderer.setSeriesPaint(0, new Color(255, 200, 0, 65));

                pcBlocks.addSeries(tmp1);
                pcBlocksArea.addSeries(tmp2);

                break;
            }
        }

        // add refuse area
        for (int i = 0; i < refuse_area.size(); i++)
        {
            int atr = (int) refuse_area.get(i)[0];
            double low = refuse_area.get(i)[1];
            double high = refuse_area.get(i)[2];

            XYSeries outline = new XYSeries(i, false, true);
            XYSeries area = new XYSeries(i, false, true);

            // top left
            outline.add(atr - 0.25, high + 0.01);
            area.add(atr - 0.25, high + 0.01);

            // top right
            outline.add(atr + 0.25, high + 0.01);
            area.add(atr + 0.25, high + 0.01);

            // bottom right
            outline.add(atr + 0.25, low - 0.01);
            area.add(atr + 0.25, low - 0.01);

            // bottom left
            outline.add(atr - 0.25, low - 0.01);
            area.add(atr - 0.25, low - 0.01);

            // top left
            outline.add(atr - 0.25, high + 0.01);
            area.add(atr - 0.25, high + 0.01);

            refuseRenderer.setSeriesPaint(i, Color.RED);
            refuseAreaRenderer.setSeriesPaint(i, new Color(255, 0, 0, 20));
            refuse.addSeries(outline);
            refuseArea.addSeries(area);
        }

        pcChart = ChartFactory.createXYLineChart(
                "",
                "",
                "",
                graphLines,
                PlotOrientation.VERTICAL,
                false,
                true,
                false);

        // format chart
        pcChart.setBorderVisible(false);
        pcChart.setPadding(RectangleInsets.ZERO_INSETS);

        // get plot
        XYPlot plot = (XYPlot) pcChart.getPlot();

        // format plot
        plot.setDrawingSupplier(new DefaultDrawingSupplier(
                new Paint[] { DV.graphColors[0] },
                DefaultDrawingSupplier.DEFAULT_OUTLINE_PAINT_SEQUENCE,
                DefaultDrawingSupplier.DEFAULT_STROKE_SEQUENCE,
                DefaultDrawingSupplier.DEFAULT_OUTLINE_STROKE_SEQUENCE,
                DefaultDrawingSupplier.DEFAULT_SHAPE_SEQUENCE));
        plot.getRangeAxis().setVisible(true);
        plot.getDomainAxis().setVisible(false);
        plot.setOutlinePaint(null);
        plot.setOutlineVisible(false);
        plot.setInsets(RectangleInsets.ZERO_INSETS);
        plot.setDomainPannable(true);
        plot.setRangePannable(true);
        plot.setBackgroundPaint(DV.background);
        plot.setDomainGridlinePaint(Color.GRAY);

        // set domain
        ValueAxis domainView = plot.getDomainAxis();
        domainView.setRange(-0.1, DV.fieldLength);

        NumberAxis xAxis = (NumberAxis) plot.getDomainAxis();
        xAxis.setTickUnit(new NumberTickUnit(1));

        // set range
        NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();
        yAxis.setTickUnit(new NumberTickUnit(0.25));

        artRenderer.setBaseStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        artRenderer.setAutoPopulateSeriesStroke(false);
        plot.setRenderer(0, artRenderer);
        plot.setDataset(0, artLines);

        // set block renderer and dataset
        pcBlockRenderer.setBaseStroke(new BasicStroke(3f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        pcBlockRenderer.setAutoPopulateSeriesStroke(false);
        plot.setRenderer(1, pcBlockRenderer);
        plot.setDataset(1, pcBlocks);

        pcBlockAreaRenderer.setAutoPopulateSeriesStroke(false);
        plot.setRenderer(2, pcBlockAreaRenderer);
        plot.setDataset(2, pcBlocksArea);

        refuseRenderer.setBaseStroke(new BasicStroke(3f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        refuseRenderer.setAutoPopulateSeriesStroke(false);
        plot.setRenderer(3, refuseRenderer);
        plot.setDataset(3, refuse);

        refuseAreaRenderer.setAutoPopulateSeriesStroke(false);
        plot.setRenderer(4, refuseAreaRenderer);
        plot.setDataset(4, refuseArea);

        lineRenderer.setBaseStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        lineRenderer.setAutoPopulateSeriesStroke(false);
        plot.setRenderer(5, lineRenderer);
        plot.setDataset(5, graphLines);

        ChartPanel chartPanel = new ChartPanel(pcChart);
        chartPanel.setMouseWheelEnabled(true);
        chartPanel.restoreAutoBounds();
        return chartPanel;
    }

    private ChartPanel drawGLCBlocks(ArrayList<DataObject> obj, int curClass)
    {
        // create main renderer and dataset
        XYLineAndShapeRenderer lineRenderer = new XYLineAndShapeRenderer(true, false);
        XYSeriesCollection graphLines = new XYSeriesCollection();

        // hyperblock renderer and dataset
        glcBlockRenderer[curClass] = new XYLineAndShapeRenderer(true, false);
        glcBlocks[curClass] = new XYSeriesCollection();
        glcBlockAreaRenderer[curClass] = new XYAreaRenderer(XYAreaRenderer.AREA);
        glcBlocksArea[curClass] = new XYSeriesCollection();

        // create renderer for threshold line
        XYLineAndShapeRenderer thresholdRenderer = new XYLineAndShapeRenderer(true, false);
        XYSeriesCollection threshold = new XYSeriesCollection();
        XYSeries thresholdLine = new XYSeries(0, false, true);

        // get threshold line
        thresholdLine.add(DV.threshold, 0);
        thresholdLine.add(DV.threshold, DV.fieldLength);

        // add threshold series to collection
        threshold.addSeries(thresholdLine);

        // renderer for endpoint, midpoint, and timeline
        XYLineAndShapeRenderer endpointRenderer = new XYLineAndShapeRenderer(false, true);
        XYLineAndShapeRenderer midpointRenderer = new XYLineAndShapeRenderer(false, true);
        XYLineAndShapeRenderer timeLineRenderer = new XYLineAndShapeRenderer(false, true);
        XYSeriesCollection endpoints = new XYSeriesCollection();
        XYSeriesCollection midpoints = new XYSeriesCollection();
        XYSeriesCollection timeLine = new XYSeriesCollection();
        XYSeries midpointSeries = new XYSeries(0, false, true);

        // populate main series
        for (int q = 0, lineCnt = -1; q < obj.size(); q++)
        {
            DataObject data = obj.get(q);

            for (int i = 0; i < data.data.length; i++)
            {
                // start line at (0, 0)
                XYSeries line = new XYSeries(++lineCnt, false, true);
                XYSeries endpointSeries = new XYSeries(lineCnt, false, true);
                XYSeries timeLineSeries = new XYSeries(lineCnt, false, true);

                if (DV.showFirstSeg)
                    line.add(0, glcBuffer);

                // add points to lines
                for (int j = 0; j < data.coordinates[i].length; j++)
                {
                    line.add(data.coordinates[i][j][0], data.coordinates[i][j][1] + glcBuffer);

                    if (j > 0 && j < data.coordinates[i].length - 1 && DV.angles[j] == DV.angles[j + 1])
                        midpointSeries.add(data.coordinates[i][j][0], data.coordinates[i][j][1] + glcBuffer);

                    // add endpoint and timeline
                    if (j == data.coordinates[i].length - 1)
                    {
                        endpointSeries.add(data.coordinates[i][j][0], data.coordinates[i][j][1] + glcBuffer);
                        timeLineSeries.add(data.coordinates[i][j][0], 0);

                        // add series
                        graphLines.addSeries(line);
                        endpoints.addSeries(endpointSeries);
                        timeLine.addSeries(timeLineSeries);

                        // set series paint
                        endpointRenderer.setSeriesPaint(lineCnt, DV.endpoints);
                        lineRenderer.setSeriesPaint(lineCnt, DV.graphColors[curClass]);
                        timeLineRenderer.setSeriesPaint(lineCnt, DV.graphColors[curClass]);

                        endpointRenderer.setSeriesShape(lineCnt, new Ellipse2D.Double(-1, -1, 2, 2));
                        timeLineRenderer.setSeriesShape(lineCnt, new Rectangle2D.Double(-0.25, -3, 0.5, 3));
                    }
                }
            }
        }

        // add data to series
        midpoints.addSeries(midpointSeries);

        // add hyperblocks
        for (int i = 0; i < hyper_blocks.size(); i++)
        {
            // generate xy points for minimums and maximums
            if (i == visualized_block)
            {
                XYSeries outline = new XYSeries(0, false, true);
                XYSeries area = new XYSeries(0, false, true);

                double[] xyOriginPointMin = DataObject.getXYPointGLC(hyper_blocks.get(i).minimums[0], DV.angles[0]);
                double[] xyOriginPointMax = DataObject.getXYPointGLC(hyper_blocks.get(i).maximums[0], DV.angles[0]);

                double[] xyCurPointMin = Arrays.copyOf(xyOriginPointMin, xyOriginPointMin.length);
                double[] xyCurPointMax = Arrays.copyOf(xyOriginPointMax, xyOriginPointMax.length);

                outline.add(xyOriginPointMin[0], xyOriginPointMin[1] + glcBuffer);
                area.add(xyOriginPointMin[0], xyOriginPointMin[1] + glcBuffer);

                for (int j = 1; j < DV.fieldLength; j++)
                {
                    double[] xyPoint = DataObject.getXYPointGLC(hyper_blocks.get(i).minimums[j], DV.angles[j]);

                    xyCurPointMin[0] = xyCurPointMin[0] + xyPoint[0];
                    xyCurPointMin[1] = xyCurPointMin[1] + xyPoint[1];

                    outline.add(xyCurPointMin[0], xyCurPointMin[1] + glcBuffer);
                    area.add(xyCurPointMin[0], xyCurPointMin[1] + glcBuffer);

                    // get maximums
                    xyPoint = DataObject.getXYPointGLC(hyper_blocks.get(i).maximums[j], DV.angles[j]);

                    xyCurPointMax[0] = xyCurPointMax[0] + xyPoint[0];
                    xyCurPointMax[1] = xyCurPointMax[1] + xyPoint[1];
                }

                outline.add(xyCurPointMax[0], xyCurPointMax[1] + glcBuffer);
                area.add(xyCurPointMax[0], xyCurPointMax[1] + glcBuffer);

                for (int j = DV.fieldLength - 2; j > -1; j--)
                {
                    double[] xyPoint = DataObject.getXYPointGLC(hyper_blocks.get(i).maximums[j], DV.angles[j]);

                    xyCurPointMax[0] = xyCurPointMax[0] - xyPoint[0];
                    xyCurPointMax[1] = xyCurPointMin[1] - xyPoint[1];

                    outline.add(xyCurPointMax[0], xyCurPointMax[1] + glcBuffer);
                    area.add(xyCurPointMax[0], xyCurPointMax[1] + glcBuffer);
                }

                outline.add(xyOriginPointMin[0], xyOriginPointMin[1] + glcBuffer);
                area.add(xyOriginPointMin[0], xyOriginPointMin[1] + glcBuffer);

                glcBlocks[curClass].addSeries(outline);
                glcBlocksArea[curClass].addSeries(area);

                break;
            }
        }

        glcChart[curClass] = ChartFactory.createXYLineChart(
                "",
                "",
                "",
                graphLines,
                PlotOrientation.VERTICAL,
                false,
                true,
                false);

        // format chart
        glcChart[curClass].setBorderVisible(false);
        glcChart[curClass].setPadding(RectangleInsets.ZERO_INSETS);

        // get plot
        XYPlot plot = (XYPlot) glcChart[curClass].getPlot();

        // format plot
        plot.setDrawingSupplier(new DefaultDrawingSupplier(
                new Paint[] { DV.graphColors[0] },
                DefaultDrawingSupplier.DEFAULT_OUTLINE_PAINT_SEQUENCE,
                DefaultDrawingSupplier.DEFAULT_STROKE_SEQUENCE,
                DefaultDrawingSupplier.DEFAULT_OUTLINE_STROKE_SEQUENCE,
                DefaultDrawingSupplier.DEFAULT_SHAPE_SEQUENCE));
        plot.getRangeAxis().setVisible(false);
        plot.getDomainAxis().setVisible(false);
        plot.setOutlinePaint(null);
        plot.setOutlineVisible(false);
        plot.setInsets(RectangleInsets.ZERO_INSETS);
        plot.setDomainPannable(true);
        plot.setRangePannable(true);
        plot.setBackgroundPaint(DV.background);
        plot.setDomainGridlinePaint(Color.GRAY);
        plot.setRangeGridlinePaint(Color.GRAY);

        // set domain and range of graph
        double bound = DV.fieldLength;

        // set domain
        ValueAxis domainView = plot.getDomainAxis();
        domainView.setRange(-bound, bound);
        NumberAxis xAxis = (NumberAxis) plot.getDomainAxis();
        xAxis.setTickUnit(new NumberTickUnit(glcBuffer));

        // set range
        ValueAxis rangeView = plot.getRangeAxis();
        NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();
        yAxis.setTickUnit(new NumberTickUnit(glcBuffer));
        rangeView.setRange(0, bound * (DV.mainPanel.getHeight() * 0.7) / (DV.graphPanel.getWidth() * 0.8));

        // create basic strokes
        BasicStroke thresholdOverlapStroke = new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[] {12f, 6f}, 0.0f);

        // set strip renderer and dataset
        // set block renderer and dataset
        glcBlockRenderer[curClass].setBaseStroke(new BasicStroke(3f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        glcBlockRenderer[curClass].setSeriesPaint(0, Color.ORANGE);
        glcBlockRenderer[curClass].setAutoPopulateSeriesStroke(false);
        plot.setRenderer(0, glcBlockRenderer[curClass]);
        plot.setDataset(0, glcBlocks[curClass]);

        glcBlockAreaRenderer[curClass].setAutoPopulateSeriesStroke(false);
        glcBlockAreaRenderer[curClass].setSeriesPaint(0, new Color(255, 200, 0, 65));
        plot.setRenderer(1, glcBlockAreaRenderer[curClass]);
        plot.setDataset(1, glcBlocksArea[curClass]);

        // set endpoint renderer and dataset
        plot.setRenderer(2, endpointRenderer);
        plot.setDataset(2, endpoints);

        // set midpoint renderer and dataset
        midpointRenderer.setSeriesShape(0, new Ellipse2D.Double(-0.5, -0.5, 1, 1));
        midpointRenderer.setSeriesPaint(0, DV.endpoints);
        plot.setRenderer(3, midpointRenderer);
        plot.setDataset(3, midpoints);

        // set threshold renderer and dataset
        thresholdRenderer.setSeriesStroke(0, thresholdOverlapStroke);
        thresholdRenderer.setSeriesPaint(0, DV.thresholdLine);
        plot.setRenderer(4, thresholdRenderer);
        plot.setDataset(4, threshold);

        // set line renderer and dataset
        lineRenderer.setBaseStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        lineRenderer.setAutoPopulateSeriesStroke(false);
        plot.setRenderer(5, lineRenderer);
        plot.setDataset(5, graphLines);

        plot.setRenderer(6, timeLineRenderer);
        plot.setDataset(6, timeLine);

        ChartPanel chartPanel = new ChartPanel(glcChart[curClass]);
        chartPanel.setMouseWheelEnabled(true);
        chartPanel.restoreAutoBounds();
        return chartPanel;
    }
}
