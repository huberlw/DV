import org.jfree.chart.*;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.axis.TickUnits;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.DefaultDrawingSupplier;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYAreaRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.statistics.BoxAndWhiskerCalculator;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.RectangleInsets;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.util.*;
import java.util.List;

public class HyperBlockVisualization
{
    JPanel pcGraphPanel;
    JPanel glcGraphPanel;
    JTabbedPane graphTabs;

    JCheckBox visualizeWithin;
    JLabel graphLabel;
    JButton right;
    JButton left;
    JButton tile;
    int visualized_block = 0;
    boolean tiles_active = false;

    ArrayList<ArrayList<DataObject>> objects;
    ArrayList<DataObject> lowerObjects;
    ArrayList<DataObject> upperObjects;

    // hyperblock storage
    ArrayList<HyperBlock> hyper_blocks = new ArrayList<>();
    ArrayList<HyperBlock> pure_blocks = new ArrayList<>();
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

    // accuracy threshold for hyperblocks
    double acc_threshold = DV.accuracy / 100;//0.95;

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


    class BlockComparator implements Comparator<HyperBlock>
    {

        // override the compare() method
        public int compare(HyperBlock b1, HyperBlock b2)
        {
            int b1_size = 0;
            int b2_size = 0;

            for (int i = 0; i < b1.hyper_block.size(); i++)
                b1_size += b1.hyper_block.get(i).size();

            for (int i = 0; i < b2.hyper_block.size(); i++)
                b2_size += b2.hyper_block.get(i).size();

            if (b1_size == b2_size)
                return 0;
            else if (b1_size < b2_size)
                return 1;
            else
                return -1;
        }
    }

    private String block_desc(int block)
    {
        String desc = "<html><b>Block:</b> " + (block + 1) + "/" + hyper_blocks.size() + "<br/>";
        desc += "<b>Class:</b> " + hyper_blocks.get(block).className + "<br/>";
        desc += "<b>Seed Attribute:</b> " + hyper_blocks.get(block).attribute+ "<br/>";
        desc += "<b>Datapoints:</b> " + hyper_blocks.get(block).size + " (" + misclassified.get(block) + " misclassified)" + "<br/>";
        desc += "<b>Accuracy:</b> " + (Math.round(acc.get(block) * 10000) / 100.0) + "%</html>";

        return desc;
    }

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
        blockCheck();

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
        c.gridwidth = 3;
        tmpFrame.add(graphTabs, c);

        graphLabel = new JLabel("");
        graphLabel.setFont(graphLabel.getFont().deriveFont(20f));
        graphLabel.setToolTipText("The seed attribute starts the process of building and refining a block." +
                                "It contains the available points in an interval above a purity threshold to build a block at a given stage.");

        graphLabel.setText(block_desc(visualized_block));

        c.gridy = 1;
        c.weighty = 0;
        tmpFrame.add(graphLabel, c);

        visualizeWithin = new JCheckBox("Only Visualize Datapoints Within Block", true);
        visualizeWithin.setFont(visualizeWithin.getFont().deriveFont(Font.BOLD, 20f));
        visualizeWithin.addActionListener(al -> updateGraphs());

        c.gridy = 2;
        tmpFrame.add(visualizeWithin, c);

        left = new JButton("Previous Block");

        c.weightx = 0.33;
        c.gridy = 3;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        tmpFrame.add(left, c);

        right = new JButton("Next Block");

        c.gridx = 1;
        tmpFrame.add(right, c);

        tile = new JButton("All Blocks");

        c.gridx = 2;
        tmpFrame.add(tile, c);

        // holds classes to be graphed
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

        pcGraphPanel.removeAll();
        pcGraphPanel.add(drawPCBlocks(objects));

        glcGraphPanel.removeAll();
        glcGraphPanel.add(drawGLCBlocks(upperObjects, 0));
        glcGraphPanel.add(drawGLCBlocks(lowerObjects, 1));

        right.addActionListener(e ->
        {
            if (tiles_active)
            {
                tiles_active = false;
            }
            else
            {
                visualized_block++;

                if (visualized_block > hyper_blocks.size() - 1)
                    visualized_block = 0;
            }

            pcGraphPanel.removeAll();
            pcGraphPanel.add(drawPCBlocks(objects));

            glcGraphPanel.removeAll();
            glcGraphPanel.add(drawGLCBlocks(upperObjects, 0));
            glcGraphPanel.add(drawGLCBlocks(lowerObjects, 1));

            pcGraphPanel.revalidate();
            pcGraphPanel.repaint();

            glcGraphPanel.revalidate();
            glcGraphPanel.repaint();

            graphLabel.setText(block_desc(visualized_block));
        });

        left.addActionListener(e ->
        {
            if (tiles_active)
            {
                tiles_active = false;
            }
            else
            {
                visualized_block--;

                if (visualized_block < 0)
                    visualized_block = hyper_blocks.size() - 1;
            }

            pcGraphPanel.removeAll();
            pcGraphPanel.add(drawPCBlocks(objects));

            glcGraphPanel.removeAll();
            glcGraphPanel.add(drawGLCBlocks(upperObjects, 0));
            glcGraphPanel.add(drawGLCBlocks(lowerObjects, 1));

            pcGraphPanel.revalidate();
            pcGraphPanel.repaint();

            glcGraphPanel.revalidate();
            glcGraphPanel.repaint();

            //updateBlocks();

            graphLabel.setText(block_desc(visualized_block));
        });

        tile.addActionListener(e ->
        {
            pcGraphPanel.removeAll();
            glcGraphPanel.removeAll();

            tiles_active = !tiles_active;

            if (tiles_active)
            {
                pcGraphPanel.add(drawPCBlockTiles(objects));
                glcGraphPanel.add(drawGLCBlockTiles(objects));
                graphLabel.setText("");
            }
            else
            {
                pcGraphPanel.add(drawPCBlocks(objects));
                glcGraphPanel.add(drawGLCBlocks(upperObjects, 0));
                glcGraphPanel.add(drawGLCBlocks(lowerObjects, 1));

                graphLabel.setText(block_desc(visualized_block));
            }

            pcGraphPanel.revalidate();
            pcGraphPanel.repaint();

            glcGraphPanel.revalidate();
            glcGraphPanel.repaint();
        });

        // show
        tmpFrame.setVisible(true);
        tmpFrame.revalidate();
        tmpFrame.pack();
        tmpFrame.repaint();
    }

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

        System.out.println("TOTAL NUM IN BLOCKS: " + bcnt + "\n");

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

            System.out.println("\nBlock " + (h+1) + " Size: " + counter[h]);
            System.out.println("Block " + (h+1) + " Accuracy: " + (maj_cnt / counter[h]));

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

            System.out.println("Block " + (h+1) + " Duplicates: " + cnt);

            ArrayList<double[]> tmptmp = new ArrayList<>(inside);
            hello.add(tmptmp);
        }
    }

    // code taken from VisCanvas2.0 autoCluster function
    // CREDIT LATER
    private void generateHyperblocks(ArrayList<ArrayList<double[]>> data, ArrayList<ArrayList<double[]>> stuff)
    {
        ArrayList<HyperBlock> blocks = new ArrayList<>(hyper_blocks);
        hyper_blocks.clear();

        for (int i = 0, cnt = blocks.size(); i < stuff.size(); i++)
        {
            // create hyperblock from each datapoint
            for (int j = 0; j < stuff.get(i).size(); j++)
            {
                blocks.add(new HyperBlock(new ArrayList<>()));
                blocks.get(cnt).hyper_block.add(new ArrayList<>(List.of(stuff.get(i).get(j))));
                blocks.get(cnt).getBounds();
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

                tmp.hyper_block.get(0).clear();
                tmp.hyper_block.get(0).addAll(pointsInSpace);
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
    }

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
        ArrayList<ArrayList<Pnt>> ans = new ArrayList<>();
        ArrayList<Integer> atri = new ArrayList<>();
        ArrayList<Integer> cls = new ArrayList<>();

        for (int a = 0; a < attributes.size(); a++)
        {
            ans.add(new ArrayList<>());
            atri.add(-1);
            cls.add(-1);
            int start = 0;
            double misclassified = 0;

            ArrayList<Pnt> tmp = new ArrayList<>();

            for (int i = 0; i < attributes.get(a).size(); i++)
            {
                if (attributes.get(a).get(start).index < upper && attributes.get(a).get(i).index < upper)
                {
                    tmp.add(attributes.get(a).get(i));

                    if (tmp.size() > ans.get(a).size())
                    {
                        ans.set(a, new ArrayList<>(tmp));
                        atri.set(a, a);
                        cls.set(a, 0);
                    }
                }
                else if (attributes.get(a).get(start).index >= upper && attributes.get(a).get(i).index >= upper)
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
                    boolean ldf_mis = false;

                    int mis_index = attributes.get(a).get(i).index;
                    double[] mis_pnt = new double[attributes.size()];

                    for (int k = 0; k < attributes.size(); k++)
                    {
                        for (int h = 0; h < attributes.get(k).size(); h++)
                        {
                            if (mis_index == attributes.get(k).get(h).index)
                            {
                                // add point
                                mis_pnt[k] = attributes.get(k).get(h).value;
                                break;
                            }
                        }
                    }

                    for (int m = 0; m < DV.misclassifiedData.size(); m++)
                    {
                        if (Arrays.deepEquals(new Object[]{ DV.misclassifiedData.get(m) }, new Object[]{ mis_pnt }))
                        {
                            ldf_mis = true;
                            break;
                        }
                    }

                    if (ldf_mis)
                    {
                        misclassified++;

                        if ((tmp.size() - misclassified + 1) / (tmp.size() + 1) >= acc_threshold)
                        {
                            tmp.add(attributes.get(a).get(i));

                            if (tmp.size() > ans.get(a).size())
                            {
                                ans.set(a, new ArrayList<>(tmp));
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

                            if (ans.get(a).size() > 0)
                            {
                                ArrayList<ArrayList<Pnt>> block = new ArrayList<>();

                                for (int k = 0; k < DV.fieldLength; k++)
                                    block.add(new ArrayList<>());

                                for (Pnt pnt : ans.get(a))
                                {
                                    for (int k = 0; k < attributes.size(); k++)
                                    {
                                        int index = pnt.index;

                                        for (int h = 0; h < attributes.get(k).size(); h++)
                                        {
                                            if (index == attributes.get(k).get(h).index)
                                            {
                                                // add point
                                                block.get(k).add(new Pnt(index, attributes.get(k).get(h).value));
                                                break;
                                            }
                                        }
                                    }
                                }

                                for (ArrayList<Pnt> blk : block)
                                    blk.sort(Comparator.comparingDouble(o -> o.value));

                                for (ArrayList<ArrayList<Pnt>> tmp_pnts : hb)
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
                                        ans.get(a).clear();
                                        atri.set(a, -1);
                                        cls.set(a, -1);
                                        break;
                                    }
                                }
                            }

                            start = i;
                            tmp.clear();
                            tmp.add(attributes.get(a).get(i));
                            misclassified = 0;
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

                        if (ans.get(a).size() > 0)
                        {
                            ArrayList<ArrayList<Pnt>> block = new ArrayList<>();

                            for (int k = 0; k < DV.fieldLength; k++)
                                block.add(new ArrayList<>());

                            for (Pnt pnt : ans.get(a))
                            {
                                for (int k = 0; k < attributes.size(); k++)
                                {
                                    int index = pnt.index;

                                    for (int h = 0; h < attributes.get(k).size(); h++)
                                    {
                                        if (index == attributes.get(k).get(h).index)
                                        {
                                            // add point
                                            block.get(k).add(new Pnt(index, attributes.get(k).get(h).value));
                                            break;
                                        }
                                    }
                                }
                            }

                            for (ArrayList<Pnt> blk : block)
                                blk.sort(Comparator.comparingDouble(o -> o.value));

                            for (ArrayList<ArrayList<Pnt>> tmp_pnts : hb)
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
                                    ans.get(a).clear();
                                    atri.set(a, -1);
                                    cls.set(a, -1);
                                    break;
                                }
                            }
                        }

                        start = i;
                        tmp.clear();
                        tmp.add(attributes.get(a).get(i));
                        misclassified = 0;
                    }
                }
            }

            if (ans.get(a).size() > 0)
            {
                ArrayList<ArrayList<Pnt>> block = new ArrayList<>();

                for (int k = 0; k < DV.fieldLength; k++)
                    block.add(new ArrayList<>());

                for (Pnt pnt : ans.get(a))
                {
                    for (int k = 0; k < attributes.size(); k++)
                    {
                        int index = pnt.index;

                        for (int h = 0; h < attributes.get(k).size(); h++)
                        {
                            if (index == attributes.get(k).get(h).index)
                            {
                                // add point
                                block.get(k).add(new Pnt(index, attributes.get(k).get(h).value));
                                break;
                            }
                        }
                    }
                }

                for (ArrayList<Pnt> blk : block)
                    blk.sort(Comparator.comparingDouble(o -> o.value));

                for (ArrayList<ArrayList<Pnt>> tmp_pnts : hb)
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
                        ans.get(a).clear();
                        atri.set(a, -1);
                        cls.set(a, -1);
                        break;
                    }
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
        ans.get(big).add(new Pnt(atri.get(big), cls.get(big)));
        return ans.get(big);
    }


    private ArrayList<Pnt> findLargestOverlappedArea(ArrayList<ArrayList<Pnt>> attributes, int upper)
    {
        ArrayList<ArrayList<Pnt>> ans = new ArrayList<>();
        ArrayList<Integer> atri = new ArrayList<>();
        ArrayList<Integer> cls = new ArrayList<>();

        for (int a = 0; a < attributes.size(); a++)
        {
            ans.add(new ArrayList<>());
            atri.add(-1);
            cls.add(-1);
            int start = 0;

            ArrayList<Pnt> tmp = new ArrayList<>();

            for (int i = 0; i < attributes.get(a).size(); i++)
            {
                if (attributes.get(a).get(start).index < upper && attributes.get(a).get(i).index < upper)
                {
                    tmp.add(attributes.get(a).get(i));

                    if (tmp.size() > ans.get(a).size())
                    {
                        ans.set(a, new ArrayList<>(tmp));
                        atri.set(a, a);
                        cls.set(a, 0);
                    }
                }
                else if (attributes.get(a).get(start).index >= upper && attributes.get(a).get(i).index >= upper)
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
        ans.get(big).add(new Pnt(atri.get(big), cls.get(big)));
        return ans.get(big);
    }


    private void generateHyperblocks3()
    {
        // create hyperblocks
        hyper_blocks.clear();

        // hyperblocks and hyperblock info
        ArrayList<ArrayList<ArrayList<Pnt>>> hb = new ArrayList<>();
        ArrayList<Integer> hb_a = new ArrayList<>();
        ArrayList<Integer> hb_c = new ArrayList<>();

        // indexes of combined hyperblocks
        ArrayList<ArrayList<ArrayList<Pnt>>> combined_hb = new ArrayList<>();
        ArrayList<Integer> combined_hb_index = new ArrayList<>();

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
                break;
            }
            /*else
            {
                // get longest overlapping interval
                // combine if same class or refuse to classify interval
                ArrayList<Pnt> oa = findLargestOverlappedArea(attributes, DV.data.get(DV.upperClass).data.length);

                if (oa.size() > 1)
                {
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
            }*/
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
            hyper_blocks.get(index).getBounds();
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

        upper.sort(new BlockComparator());
        lower.sort(new BlockComparator());

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

                for (int k = 0; k < hyper_blocks.get(i).hyper_block.size(); k++)
                {
                    XYSeries pcOutline = new XYSeries(k, false, true);
                    XYSeries pcArea = new XYSeries(k, false, true);
                    XYSeries line = new XYSeries(k, false, true);

                    for (int j = 0; j < DV.fieldLength; j++)
                    {
                        pcOutline.add(j, hyper_blocks.get(i).minimums.get(k)[j]);
                        pcArea.add(j, hyper_blocks.get(i).minimums.get(k)[j]);
                    }

                    for (int j = DV.fieldLength - 1; j > -1; j--)
                    {
                        pcOutline.add(j, hyper_blocks.get(i).maximums.get(k)[j]);
                        pcArea.add(j, hyper_blocks.get(i).maximums.get(k)[j]);
                    }

                    for (int j = 0; j < artificial.get(i).get(k).length; j++)
                    {
                        line.add(j, artificial.get(i).get(k)[j]);
                    }

                    pcOutline.add(0, hyper_blocks.get(i).minimums.get(k)[0]);
                    pcArea.add(0, hyper_blocks.get(i).minimums.get(k)[0]);
                    artLines.addSeries(line);

                    pcBlocks.addSeries(pcOutline);
                    pcBlocksArea.addSeries(pcArea);
                    artRenderer.setSeriesPaint(k, Color.BLACK);
                }

                pcChart.setNotify(true);

                // glc graph
                // turn notify off
                glcChart[0].setNotify(false);
                glcChart[1].setNotify(false);

                glcBlocks[0].removeAllSeries();
                glcBlocks[1].removeAllSeries();
                glcBlocksArea[0].removeAllSeries();
                glcBlocksArea[1].removeAllSeries();

                for (int k = 0; k < hyper_blocks.get(i).hyper_block.size(); k++)
                {
                    XYSeries glcOutline = new XYSeries(k, false, true);
                    XYSeries glcArea = new XYSeries(k, false, true);

                    double[] xyOriginPointMin = DataObject.getXYPointGLC(hyper_blocks.get(i).minimums.get(k)[0], DV.angles[0]);
                    double[] xyOriginPointMax = DataObject.getXYPointGLC(hyper_blocks.get(i).maximums.get(k)[0], DV.angles[0]);
                    xyOriginPointMin[1] += glcBuffer;
                    xyOriginPointMax[1] += glcBuffer;

                    double[] xyCurPointMin = Arrays.copyOf(xyOriginPointMin, xyOriginPointMin.length);
                    double[] xyCurPointMax = Arrays.copyOf(xyOriginPointMax, xyOriginPointMax.length);

                    if (DV.showFirstSeg)
                    {
                        glcOutline.add(0, glcBuffer);
                        glcArea.add(0, glcBuffer);
                    }

                    glcOutline.add(xyOriginPointMin[0], xyOriginPointMin[1]);
                    glcArea.add(xyOriginPointMin[0], xyOriginPointMin[1]);

                    for (int j = 1; j < DV.fieldLength; j++)
                    {
                        double[] xyPoint = DataObject.getXYPointGLC(hyper_blocks.get(i).minimums.get(k)[j], DV.angles[j]);

                        xyCurPointMin[0] = xyCurPointMin[0] + xyPoint[0];
                        xyCurPointMin[1] = xyCurPointMin[1] + xyPoint[1];

                        glcOutline.add(xyCurPointMin[0], xyCurPointMin[1]);
                        glcArea.add(xyCurPointMin[0], xyCurPointMin[1]);

                        // get maximums
                        xyPoint = DataObject.getXYPointGLC(hyper_blocks.get(i).maximums.get(k)[j], DV.angles[j]);

                        xyCurPointMax[0] = xyCurPointMax[0] + xyPoint[0];
                        xyCurPointMax[1] = xyCurPointMax[1] + xyPoint[1];
                    }

                    glcOutline.add(xyCurPointMax[0], xyCurPointMax[1]);
                    glcArea.add(xyCurPointMax[0], xyCurPointMax[1]);

                    for (int j = DV.fieldLength - 2; j >= 0; j--)
                    {
                        double[] xyPoint = DataObject.getXYPointGLC(hyper_blocks.get(i).maximums.get(k)[j], DV.angles[j]);

                        xyCurPointMax[0] = xyCurPointMax[0] - xyPoint[0];
                        xyCurPointMax[1] = xyCurPointMax[1] - xyPoint[1];

                        glcOutline.add(xyCurPointMax[0], xyCurPointMax[1]);
                        glcArea.add(xyCurPointMax[0], xyCurPointMax[1]);
                    }

                    if (DV.showFirstSeg)
                    {
                        glcOutline.add(0, glcBuffer);
                        glcArea.add(0, glcBuffer);
                    }
                    else
                    {
                        glcOutline.add(xyOriginPointMin[0], xyOriginPointMin[1]);
                        glcArea.add(xyOriginPointMin[0], xyOriginPointMin[1]);
                    }

                    glcBlocks[0].addSeries(glcOutline);
                    glcBlocks[1].addSeries(glcOutline);
                    glcBlocksArea[0].addSeries(glcArea);
                    glcBlocksArea[1].addSeries(glcArea);
                }

                glcChart[0].setNotify(true);
                glcChart[1].setNotify(true);

                break;
            }
        }

        graphLabel.setText(block_desc(visualized_block));
    }

    private void updateGraphs()
    {
        pcGraphPanel.removeAll();
        glcGraphPanel.removeAll();

        if (tiles_active)
        {
            pcGraphPanel.add(drawPCBlockTiles(objects));
        }
        else
        {
            pcGraphPanel.add(drawPCBlocks(objects));
        }

        glcGraphPanel.add(drawGLCBlocks(upperObjects, 0));
        glcGraphPanel.add(drawGLCBlocks(lowerObjects, 1));

        pcGraphPanel.revalidate();
        pcGraphPanel.repaint();

        glcGraphPanel.revalidate();
        glcGraphPanel.repaint();

        graphLabel.setText("<html><b>All Blocks</b></html>");
    }

    private ChartPanel drawPCBlocks(ArrayList<ArrayList<DataObject>> obj)
    {
        // create main renderer and dataset
        XYLineAndShapeRenderer goodLineRenderer = new XYLineAndShapeRenderer(true, false);
        XYSeriesCollection goodGraphLines = new XYSeriesCollection();
        XYLineAndShapeRenderer badLineRenderer = new XYLineAndShapeRenderer(true, false);
        XYSeriesCollection badGraphLines = new XYSeriesCollection();

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

        BasicStroke[] strokes = new BasicStroke[ hyper_blocks.get(visualized_block).hyper_block.size()];

        for (int k = 0; k < hyper_blocks.get(visualized_block).hyper_block.size(); k++)
        {
            if (hyper_blocks.get(visualized_block).hyper_block.get(k).size() > 1)
            {
                if (k == 0)
                {
                    strokes[k] = new BasicStroke(3f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);
                }
                else
                {
                    Random r = new Random();
                    float max = 25f;
                    float min = 1f;

                    int len = r.nextInt(2) + 1;

                    float[] fa = new float[len];

                    for (int i = 0; i < len; i++)
                    {
                        fa[i] = r.nextFloat(max - min) + min;
                    }

                    strokes[k] = new BasicStroke(3f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1f, fa, 0f);
                }
            }
        }

        // populate main series
        for (int d = 0; d < obj.size(); d++)
        {
            int lineCnt = 0;

            for (DataObject data : obj.get(d))
            {
                for (int i = 0; i < data.data.length; i++)
                {
                    // start line at (0, 0)
                    XYSeries line = new XYSeries(lineCnt, false, true);
                    boolean within = false;
                    int within_block = 0;

                    for (int k = 0; k < hyper_blocks.get(visualized_block).hyper_block.size(); k++)
                    {
                        boolean within_cur = true;

                        for (int j = 0; j < DV.fieldLength; j++)
                        {
                            if (data.data[i][j] < hyper_blocks.get(visualized_block).minimums.get(k)[j] || data.data[i][j] > hyper_blocks.get(visualized_block).maximums.get(k)[j])
                            {
                                within_cur = false;
                            }

                            if (j == DV.fieldLength - 1)
                            {
                                if (within_cur)
                                {
                                    within_block = k;
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
                            if (visualizeWithin.isSelected())
                            {
                                if (within)
                                {
                                    // add series
                                    if (d == hyper_blocks.get(visualized_block).classNum)
                                    {
                                        goodGraphLines.addSeries(line);

                                        goodLineRenderer.setSeriesPaint(lineCnt, DV.graphColors[d]);
                                        goodLineRenderer.setSeriesStroke(lineCnt, strokes[within_block]);
                                    }
                                    else
                                    {
                                        badGraphLines.addSeries(line);

                                        badLineRenderer.setSeriesPaint(lineCnt, DV.graphColors[d]);
                                        badLineRenderer.setSeriesStroke(lineCnt, strokes[within_block]);
                                    }

                                    lineCnt++;
                                }
                            }
                            else
                            {
                                // add series
                                if (d == hyper_blocks.get(visualized_block).classNum)
                                {
                                    goodGraphLines.addSeries(line);

                                    goodLineRenderer.setSeriesPaint(lineCnt, DV.graphColors[d]);

                                    if (within)
                                        goodLineRenderer.setSeriesStroke(lineCnt, strokes[within_block]);
                                }
                                else
                                {
                                    badGraphLines.addSeries(line);

                                    badLineRenderer.setSeriesPaint(lineCnt, DV.graphColors[d]);

                                    if (within)
                                        badLineRenderer.setSeriesStroke(lineCnt, strokes[within_block]);
                                }



                                lineCnt++;
                            }
                        }
                    }
                }
            }
        }

        // populate art series
        for (int k = 0; k < artificial.get(visualized_block).size(); k++)
        {
            if (hyper_blocks.get(visualized_block).hyper_block.get(k).size() > 1)
            {
                XYSeries line = new XYSeries(k, false, true);

                for (int i = 0; i < artificial.get(visualized_block).get(k).length; i++)
                {
                    line.add(i, artificial.get(visualized_block).get(k)[i]);
                }

                artLines.addSeries(line);
                artRenderer.setSeriesPaint(k, Color.BLACK);
            }
        }

        // add hyperblocks
        for (int k = 0, offset = 0; k < hyper_blocks.get(visualized_block).hyper_block.size(); k++)
        {
            if (hyper_blocks.get(visualized_block).hyper_block.get(k).size() > 1)
            {
                XYSeries tmp1 = new XYSeries(k-offset, false, true);
                XYSeries tmp2 = new XYSeries(k-offset, false, true);

                for (int j = 0; j < DV.fieldLength; j++)
                {
                    tmp1.add(j, hyper_blocks.get(visualized_block).minimums.get(k)[j]);
                    tmp2.add(j, hyper_blocks.get(visualized_block).minimums.get(k)[j]);
                }

                for (int j = DV.fieldLength - 1; j > -1; j--)
                {
                    tmp1.add(j, hyper_blocks.get(visualized_block).maximums.get(k)[j]);
                    tmp2.add(j, hyper_blocks.get(visualized_block).maximums.get(k)[j]);
                }

                tmp1.add(0, hyper_blocks.get(visualized_block).minimums.get(k)[0]);
                tmp2.add(0, hyper_blocks.get(visualized_block).minimums.get(k)[0]);

                pcBlockRenderer.setSeriesPaint(k-offset, Color.ORANGE);
                pcBlockAreaRenderer.setSeriesPaint(k-offset, new Color(255, 200, 0, 20));
                pcBlockRenderer.setSeriesStroke(k, strokes[k]);

                pcBlocks.addSeries(tmp1);
                pcBlocksArea.addSeries(tmp2);
            }
            else
            {
                offset++;
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
            refuseAreaRenderer.setSeriesPaint(i, new Color(255, 0, 0, 10));
            refuse.addSeries(outline);
            refuseArea.addSeries(area);
        }

        pcChart = ChartFactory.createXYLineChart(
                "",
                "",
                "",
                goodGraphLines,
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
        plot.getDomainAxis().setVisible(true);
        plot.setOutlinePaint(null);
        plot.setOutlineVisible(false);
        plot.setInsets(RectangleInsets.ZERO_INSETS);
        plot.setDomainPannable(true);
        plot.setRangePannable(true);
        plot.setBackgroundPaint(DV.background);
        plot.setDomainGridlinePaint(Color.GRAY);

        // set domain
        ValueAxis domainView = plot.getDomainAxis();
        domainView.setRange(-0.1, DV.fieldLength-1);

        NumberAxis xAxis = (NumberAxis) plot.getDomainAxis();
        NumberTickUnit ntu = new NumberTickUnit(1)
        {
            @Override
            public String valueToString(double value) {
                return super.valueToString(value + 1);
            }
        };

        xAxis.setTickUnit(ntu);

        // set range
        NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();
        yAxis.setTickUnit(new NumberTickUnit(0.25));
        yAxis.setAutoRange(false);
        yAxis.setRange(0, 1);

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

        badLineRenderer.setBaseStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        badLineRenderer.setAutoPopulateSeriesStroke(false);
        plot.setRenderer(5, badLineRenderer);
        plot.setDataset(5, badGraphLines);

        goodLineRenderer.setBaseStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        goodLineRenderer.setAutoPopulateSeriesStroke(false);
        plot.setRenderer(6, goodLineRenderer);
        plot.setDataset(6, goodGraphLines);

        ChartPanel chartPanel = new ChartPanel(pcChart);
        chartPanel.setMouseWheelEnabled(true);
        return chartPanel;
    }

    private ChartPanel drawGLCBlocks(ArrayList<DataObject> obj, int curClass)
    {
        double[] dists = Arrays.copyOf(DV.angles, DV.angles.length);//new double[DV.fieldLength];
        int[] indexes = new int[DV.fieldLength];

        for (int i = 0; i < DV.fieldLength; i++)
        {
            dists[i] = 90 - dists[i];//hyper_blocks.get(visualized_block).maximums.get(0)[i] - hyper_blocks.get(visualized_block).minimums.get(0)[i];
            indexes[i] = i;
        }

        int n = DV.fieldLength;
        for (int i = 0; i < n - 1; i++)
        {
            for (int j = 0; j < n - i - 1; j++)
            {
                if (dists[j] > dists[j + 1])
                {
                    double temp1 = dists[j];
                    dists[j] = dists[j + 1];
                    dists[j + 1] = temp1;

                    int temp2 = indexes[j];
                    indexes[j] = indexes[j + 1];
                    indexes[j + 1] = temp2;
                }
            }
        }

        for (int i = 0; i < n - 1; i++)
        {
            for (int j = 0; j < n - i - 1; j++)
            {
                if (dists[j] < 0 && dists[j + 1] < 0)
                {
                    if (Math.abs(dists[j]) > Math.abs(dists[j + 1]))
                    {
                        double temp1 = dists[j];
                        dists[j] = dists[j + 1];
                        dists[j + 1] = temp1;

                        int temp2 = indexes[j];
                        indexes[j] = indexes[j + 1];
                        indexes[j + 1] = temp2;
                    }
                }
                else
                {
                    break;
                }
            }
        }

        ArrayList<Integer> separation = new ArrayList<>();

        // get separation between blocks
        if (hyper_blocks.get(visualized_block).hyper_block.size() > 1)
        {
            for (int j = 1; j < hyper_blocks.get(visualized_block).hyper_block.size(); j++)
            {
                for (int k = 0; k < DV.fieldLength; k++)
                {
                    if (hyper_blocks.get(visualized_block).maximums.get(j)[k] < hyper_blocks.get(visualized_block).minimums.get(0)[k] ||
                            hyper_blocks.get(visualized_block).minimums.get(j)[k] > hyper_blocks.get(visualized_block).maximums.get(0)[k])
                    {
                        separation.add(k);
                    }
                }
            }
        }

        // calculate scaler
        ArrayList<Double> art_scales = new ArrayList<>();

        double amin1 = 0;
        double amax1 = 0;
        double amin2 = 0;
        double amax2 = 0;

        for (int j = 0; j < hyper_blocks.get(visualized_block).minimums.size() - 1; j++)
        {
            for (int i = 0; i < DV.fieldLength; i++)
            {
                double[] min_pnt = DataObject.getXYPointGLC(hyper_blocks.get(visualized_block).minimums.get(j)[i], 15);
                double[] max_pnt = DataObject.getXYPointGLC(hyper_blocks.get(visualized_block).maximums.get(j)[i], 90);

                amin1 += min_pnt[1];
                amax1 += max_pnt[1];

                min_pnt = DataObject.getXYPointGLC(hyper_blocks.get(visualized_block).minimums.get(j+1)[i], 15);
                max_pnt = DataObject.getXYPointGLC(hyper_blocks.get(visualized_block).maximums.get(j+1)[i], 90);

                amin2 += min_pnt[1];
                amax2 += max_pnt[1];
            }
        }

        amin1 += 0.1;
        amax1 += 0.1;
        amin2 += 0.1;
        amax2 += 0.1;

        double tmp1 = amax1 / amin2;
        double tmp2 = amax2 / amin1;

        double scale = Math.max(tmp1, tmp2) + 1;

        BasicStroke[] strokes = new BasicStroke[hyper_blocks.get(visualized_block).hyper_block.size()];

        for (int k = 0; k < hyper_blocks.get(visualized_block).hyper_block.size(); k++)
        {
            if (hyper_blocks.get(visualized_block).hyper_block.get(k).size() > 1)
            {
                if (k == 0)
                {
                    strokes[k] = new BasicStroke(3f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);
                }
                else
                {
                    Random r = new Random();
                    float max = 25f;
                    float min = 1f;

                    int len = r.nextInt(2) + 1;

                    float[] fa = new float[len];

                    for (int i = 0; i < len; i++)
                    {
                        fa[i] = r.nextFloat(max - min) + min;
                    }

                    strokes[k] = new BasicStroke(3f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1f, new float[]{10, 5}, 0f);
                }
            }
        }

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
        for (int q = 0, lineCnt = 0; q < obj.size(); q++)
        {
            DataObject data = obj.get(q);

            for (int i = 0; i < data.data.length; i++)
            {
                // start line at (0, 0)
                XYSeries line = new XYSeries(lineCnt, false, true);
                XYSeries endpointSeries = new XYSeries(lineCnt, false, true);
                XYSeries timeLineSeries = new XYSeries(lineCnt, false, true);

                if (DV.showFirstSeg)
                    line.add(0, glcBuffer);

                boolean within = false;
                int within_block = 0;

                for (int k = 0; k < hyper_blocks.get(visualized_block).hyper_block.size(); k++)
                {
                    boolean within_cur = true;

                    for (int j = 0; j < data.coordinates[i].length; j++)
                    {
                        if (data.data[i][j] < hyper_blocks.get(visualized_block).minimums.get(k)[j] || data.data[i][j] > hyper_blocks.get(visualized_block).maximums.get(k)[j])
                        {
                            within_cur = false;
                        }

                        if (j == data.coordinates[i].length - 1)
                        {
                            if (within_cur)
                            {
                                within_block = k;
                                within = true;
                            }
                        }
                    }
                }

                for (int s = 0; s < separation.size(); s++)
                {
                    double[] a_point = DataObject.getXYPointGLC((data.data[i][separation.get(s)]) * scale, 90);

                    a_point[0] += (double)line.getX(s);
                    a_point[1] += (double)line.getY(s);

                    line.add(a_point[0], a_point[1]);
                }

                // add points to lines
                for (int j = 0; j < data.coordinates[i].length; j++)
                {
                    /*double angle;
                    if (j == 0)
                        angle = DV.angles[indexes[j]];
                    else
                        angle = 45;*/

                    double[] point = new double[0];

                    if (separation.size() > 0)
                    {
                        boolean not_set = true;

                        for (Integer sep : separation)
                        {
                            if (indexes[j] == sep)
                            {
                                point = DataObject.getXYPointGLC((data.data[i][indexes[j]]), DV.angles[indexes[j]]);
                                not_set = false;
                                break;
                            }
                        }

                        if (not_set)
                        {
                            point = DataObject.getXYPointGLC((data.data[i][indexes[j]]), DV.angles[indexes[j]]);
                        }
                    }
                    else
                    {
                        point = DataObject.getXYPointGLC((data.data[i][indexes[j]]), DV.angles[indexes[j]]);
                    }

                    point[0] += (double)line.getX(j + separation.size());
                    point[1] += (double)line.getY(j + separation.size());


                    line.add(point[0], point[1]);

                    //if (j > 0 && j < data.coordinates[i].length - 1 && DV.angles[indexes[j]] == DV.angles[indexes[j+1]])
                        //midpointSeries.add(point[0], point[1]);

                    // add endpoint and timeline
                    if (j == data.coordinates[i].length - 1)
                    {
                        if (visualizeWithin.isSelected())
                        {
                            if (within)
                            {
                                endpointSeries.add(point[0], point[1]);
                                timeLineSeries.add(point[0], 0);

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

                                lineRenderer.setSeriesStroke(lineCnt, strokes[within_block]);

                                lineCnt++;
                            }
                        }
                        else
                        {
                            endpointSeries.add(point[0], point[1]);
                            timeLineSeries.add(point[0], 0);

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

                            if (within)
                                lineRenderer.setSeriesStroke(lineCnt, strokes[within_block]);

                            lineCnt++;
                        }
                    }
                }
            }
        }

        // add data to series
        midpoints.addSeries(midpointSeries);

        // add hyperblocks
        // generate xy points for minimums and maximums
        for (int k = 0, cnt = 0; k < hyper_blocks.get(visualized_block).hyper_block.size(); k++)
        {
            if (hyper_blocks.get(visualized_block).hyper_block.get(k).size() > 1)
            {
                glcBlockRenderer[curClass].setSeriesPaint(cnt, Color.ORANGE);
                glcBlockRenderer[curClass].setSeriesStroke(cnt, strokes[k]);
                XYSeries max_bound = new XYSeries(cnt++, false, true);
                glcBlockRenderer[curClass].setSeriesPaint(cnt, Color.ORANGE);
                glcBlockRenderer[curClass].setSeriesStroke(cnt, strokes[k]);
                XYSeries min_bound = new XYSeries(cnt++, false, true);

                XYSeries area = new XYSeries(k, false, true);

                if (DV.showFirstSeg)
                {
                    max_bound.add(0, glcBuffer);
                    min_bound.add(0, glcBuffer);
                    //area.add(0, glcBuffer);
                }

                for (int s = 0; s < separation.size(); s++)
                {
                    double[] a_min_pnt = DataObject.getXYPointGLC((hyper_blocks.get(visualized_block).minimums.get(k)[separation.get(s)]) * scale, 90);
                    double[] a_max_pnt = DataObject.getXYPointGLC((hyper_blocks.get(visualized_block).maximums.get(k)[separation.get(s)]) * scale, 90);

                    a_min_pnt[0] += (double)min_bound.getX(s);
                    a_min_pnt[1] += (double)min_bound.getY(s);
                    a_max_pnt[0] += (double)max_bound.getX(s);
                    a_max_pnt[1] += (double)max_bound.getY(s);

                    min_bound.add(a_min_pnt[0], a_min_pnt[1]);
                    max_bound.add(a_max_pnt[0], a_max_pnt[1]);
                }

                for (int j = 0; j < DV.fieldLength; j++)
                {
                    double[] xyPoint = new double[0];//DataObject.getXYPointGLC(hyper_blocks.get(c).minimums.get(k)[indexes[j]], DV.angles[indexes[j]]);

                    if (separation.size() > 0)
                    {
                        boolean not_set = true;

                        for (Integer sep : separation)
                        {
                            if (indexes[j] == sep)
                            {
                                xyPoint = DataObject.getXYPointGLC((hyper_blocks.get(visualized_block).minimums.get(k)[indexes[j]]), DV.angles[indexes[j]]);
                                not_set = false;
                                break;
                            }
                        }

                        if (not_set)
                        {
                            xyPoint = DataObject.getXYPointGLC((hyper_blocks.get(visualized_block).minimums.get(k)[indexes[j]]), DV.angles[indexes[j]]);
                        }
                    }
                    else
                    {
                        xyPoint = DataObject.getXYPointGLC((hyper_blocks.get(visualized_block).minimums.get(k)[indexes[j]]), DV.angles[indexes[j]]);
                    }

                    xyPoint[0] += (double)min_bound.getX(j + separation.size());
                    xyPoint[1] += (double)min_bound.getY(j + separation.size());

                    min_bound.add(xyPoint[0], xyPoint[1]);
                    //area.add(xyCurPointMin[0], xyCurPointMin[1]);

                    // get maximums
                    //xyPoint = DataObject.getXYPointGLC(hyper_blocks.get(visualized_block).maximums.get(k)[indexes[j]], DV.angles[indexes[j]]);
                    if (separation.size() > 0)
                    {
                        boolean not_set = true;

                        for (Integer sep : separation)
                        {
                            if (indexes[j] == sep)
                            {
                                xyPoint = DataObject.getXYPointGLC((hyper_blocks.get(visualized_block).maximums.get(k)[indexes[j]]), DV.angles[indexes[j]]);
                                not_set = false;
                                break;
                            }
                        }

                        if (not_set)
                        {
                            xyPoint = DataObject.getXYPointGLC((hyper_blocks.get(visualized_block).maximums.get(k)[indexes[j]]), DV.angles[indexes[j]]);
                        }
                    }
                    else
                    {
                        xyPoint = DataObject.getXYPointGLC((hyper_blocks.get(visualized_block).maximums.get(k)[indexes[j]]), DV.angles[indexes[j]]);
                    }

                    xyPoint[0] += (double)max_bound.getX(j + separation.size());
                    xyPoint[1] += (double)max_bound.getY(j + separation.size());

                    max_bound.add(xyPoint[0], xyPoint[1]);
                }

                //max_bound.add(xyCurPointMin[0], xyCurPointMin[1]);
                //area.add(xyCurPointMax[0], xyCurPointMax[1]);

                glcBlocks[curClass].addSeries(max_bound);
                glcBlocks[curClass].addSeries(min_bound);
                glcBlocksArea[curClass].addSeries(area);

                glcBlockRenderer[curClass].setSeriesStroke(cnt-1, strokes[k]);
                glcBlockRenderer[curClass].setSeriesStroke(cnt, strokes[k]);
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

    private JPanel drawPCBlockTiles(ArrayList<ArrayList<DataObject>> obj)
    {
        int rows = (int)Math.ceil(hyper_blocks.size() / 5.0);

        JPanel tilePanel = new JPanel();
        tilePanel.setLayout(new GridLayout(rows, 5));

        for (int c = 0; c < hyper_blocks.size(); c++)
        {
            BasicStroke[] strokes = new BasicStroke[hyper_blocks.get(c).hyper_block.size()];

            for (int k = 0; k < hyper_blocks.get(c).hyper_block.size(); k++)
            {
                if (hyper_blocks.get(c).hyper_block.get(k).size() > 1)
                {
                    if (k == 0)
                    {
                        strokes[k] = new BasicStroke(3f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);
                    }
                    else
                    {
                        Random r = new Random();
                        float max = 25f;
                        float min = 1f;

                        int len = r.nextInt(2) + 1;

                        float[] fa = new float[len];

                        for (int i = 0; i < len; i++)
                        {
                            fa[i] = r.nextFloat(max - min) + min;
                        }

                        strokes[k] = new BasicStroke(3f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1f, fa, 0f);
                    }
                }
            }

            // create main renderer and dataset
            XYLineAndShapeRenderer goodLineRenderer = new XYLineAndShapeRenderer(true, false);
            XYLineAndShapeRenderer badLineRenderer = new XYLineAndShapeRenderer(true, false);
            XYSeriesCollection goodGraphLines = new XYSeriesCollection();
            XYSeriesCollection badGraphLines = new XYSeriesCollection();

            // artificial renderer and dataset
            XYLineAndShapeRenderer artRenderer = new XYLineAndShapeRenderer(true, false);
            XYSeriesCollection artLines = new XYSeriesCollection();

            // hyperblock renderer and dataset
            XYLineAndShapeRenderer pcBlockRenderer = new XYLineAndShapeRenderer(true, false);
            XYSeriesCollection pcBlocks = new XYSeriesCollection();
            XYAreaRenderer pcBlockAreaRenderer = new XYAreaRenderer(XYAreaRenderer.AREA);
            XYSeriesCollection pcBlocksArea = new XYSeriesCollection();

            // refuse to classify area
            XYLineAndShapeRenderer refuseRenderer = new XYLineAndShapeRenderer(true, false);
            XYSeriesCollection refuse = new XYSeriesCollection();
            XYAreaRenderer refuseAreaRenderer = new XYAreaRenderer(XYAreaRenderer.AREA);
            XYSeriesCollection refuseArea = new XYSeriesCollection();

            // populate main series
            for (int d = 0; d < obj.size(); d++)
            {
                int lineCnt = 0;

                for (DataObject data : obj.get(d))
                {
                    for (int i = 0; i < data.data.length; i++)
                    {
                        // start line at (0, 0)
                        XYSeries line = new XYSeries(lineCnt, false, true);
                        boolean within = false;
                        int within_block = 0;

                        for (int k = 0; k < hyper_blocks.get(c).hyper_block.size(); k++)
                        {
                            boolean within_cur = true;

                            for (int j = 0; j < DV.fieldLength; j++)
                            {
                                if (data.data[i][j] < hyper_blocks.get(c).minimums.get(k)[j] || data.data[i][j] > hyper_blocks.get(c).maximums.get(k)[j])
                                {
                                    within_cur = false;
                                }

                                if (j == DV.fieldLength - 1)
                                {
                                    if (within_cur)
                                    {
                                        within_block = k;
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
                                if (visualizeWithin.isSelected())
                                {
                                    if (within)
                                    {
                                        // add series
                                        if (d == hyper_blocks.get(c).classNum)
                                        {
                                            goodGraphLines.addSeries(line);

                                            goodLineRenderer.setSeriesPaint(lineCnt, DV.graphColors[d]);
                                            goodLineRenderer.setSeriesStroke(lineCnt, strokes[within_block]);
                                        }
                                        else
                                        {
                                            badGraphLines.addSeries(line);

                                            badLineRenderer.setSeriesPaint(lineCnt, DV.graphColors[d]);
                                            badLineRenderer.setSeriesStroke(lineCnt, strokes[within_block]);
                                        }

                                        // set series paint
                                        lineCnt++;
                                    }
                                }
                                else
                                {
                                    // add series
                                    if (d == hyper_blocks.get(c).classNum)
                                    {
                                        goodGraphLines.addSeries(line);

                                        goodLineRenderer.setSeriesPaint(lineCnt, DV.graphColors[d]);

                                        if (within)
                                            goodLineRenderer.setSeriesStroke(lineCnt, strokes[within_block]);
                                    }
                                    else
                                    {
                                        badGraphLines.addSeries(line);

                                        badLineRenderer.setSeriesPaint(lineCnt, DV.graphColors[d]);

                                        if (within)
                                            badLineRenderer.setSeriesStroke(lineCnt, strokes[within_block]);
                                    }

                                    lineCnt++;
                                }
                            }
                        }
                    }
                }
            }

            // populate art series
            for (int k = 0; k < artificial.get(c).size(); k++)
            {
                if (hyper_blocks.get(c).hyper_block.get(k).size() > 1)
                {
                    XYSeries line = new XYSeries(k, false, true);

                    for (int i = 0; i < artificial.get(c).get(k).length; i++) {
                        line.add(i, artificial.get(c).get(k)[i]);
                    }

                    artLines.addSeries(line);
                    artRenderer.setSeriesPaint(k, Color.BLACK);
                }
            }

            // add hyperblocks
            for (int k = 0, offset = 0; k < hyper_blocks.get(c).hyper_block.size(); k++)
            {
                if (hyper_blocks.get(c).hyper_block.get(k).size() > 1)
                {
                    XYSeries tmp1 = new XYSeries(k - offset, false, true);
                    XYSeries tmp2 = new XYSeries(k - offset, false, true);

                    for (int j = 0; j < DV.fieldLength; j++) {
                        tmp1.add(j, hyper_blocks.get(c).minimums.get(k)[j]);
                        tmp2.add(j, hyper_blocks.get(c).minimums.get(k)[j]);
                    }

                    for (int j = DV.fieldLength - 1; j > -1; j--) {
                        tmp1.add(j, hyper_blocks.get(c).maximums.get(k)[j]);
                        tmp2.add(j, hyper_blocks.get(c).maximums.get(k)[j]);
                    }

                    tmp1.add(0, hyper_blocks.get(c).minimums.get(k)[0]);
                    tmp2.add(0, hyper_blocks.get(c).minimums.get(k)[0]);

                    pcBlockRenderer.setSeriesPaint(k - offset, Color.ORANGE);
                    pcBlockAreaRenderer.setSeriesPaint(k - offset, new Color(255, 200, 0, 20));
                    pcBlockRenderer.setSeriesStroke(k - offset, strokes[k - offset]);

                    pcBlocks.addSeries(tmp1);
                    pcBlocksArea.addSeries(tmp2);
                }
                else
                {
                    offset++;
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
                refuseAreaRenderer.setSeriesPaint(i, new Color(255, 0, 0, 10));
                refuse.addSeries(outline);
                refuseArea.addSeries(area);
            }

            pcChart = ChartFactory.createXYLineChart(
                    "",
                    "",
                    "",
                    goodGraphLines,
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
            plot.getDomainAxis().setVisible(true);
            plot.setOutlinePaint(null);
            plot.setOutlineVisible(false);
            plot.setInsets(RectangleInsets.ZERO_INSETS);
            plot.setDomainPannable(true);
            plot.setRangePannable(true);
            plot.setBackgroundPaint(DV.background);
            plot.setDomainGridlinePaint(Color.GRAY);

            // set domain
            ValueAxis domainView = plot.getDomainAxis();
            domainView.setRange(-0.1, DV.fieldLength-1);

            NumberAxis xAxis = (NumberAxis) plot.getDomainAxis();

            NumberTickUnit ntu = new NumberTickUnit(1)
            {
                @Override
                public String valueToString(double value) {
                    return super.valueToString(value + 1);
                }
            };

            xAxis.setTickUnit(ntu);

            // set range
            NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();
            yAxis.setTickUnit(new NumberTickUnit(0.25));
            yAxis.setAutoRange(false);
            yAxis.setRange(0, 1);

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

            badLineRenderer.setBaseStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
            badLineRenderer.setAutoPopulateSeriesStroke(false);
            plot.setRenderer(5, badLineRenderer);
            plot.setDataset(5, badGraphLines);

            goodLineRenderer.setBaseStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
            goodLineRenderer.setAutoPopulateSeriesStroke(false);
            plot.setRenderer(6, goodLineRenderer);
            plot.setDataset(6, goodGraphLines);

            ChartPanel chartPanel = new ChartPanel(pcChart);
            chartPanel.setMouseWheelEnabled(true);

            JPanel tmp = new JPanel();
            tmp.setLayout(new BoxLayout(tmp, BoxLayout.PAGE_AXIS));

            tmp.add(chartPanel);

            tmp.add(new JLabel(block_desc(c)));

            int finalC = c;
            chartPanel.addChartMouseListener(new ChartMouseListener()
            {
                @Override
                public void chartMouseClicked(ChartMouseEvent chartMouseEvent)
                {
                    if (SwingUtilities.isLeftMouseButton(chartMouseEvent.getTrigger()))
                    {
                        visualized_block = finalC;
                        tile.doClick();
                    }
                }

                @Override
                public void chartMouseMoved(ChartMouseEvent chartMouseEvent) {}
            });

            JPopupMenu options = new JPopupMenu("Block Options");
            options.setLayout(new GridBagLayout());

            GridBagConstraints constraints = new GridBagConstraints();
            constraints.gridx = 0;
            constraints.gridy = 0;
            constraints.weightx = 1;
            constraints.weighty = 1;
            constraints.fill = GridBagConstraints.BOTH;

            options.add(new JLabel("<html><b>Combine Blocks</b></html>"), constraints);
            constraints.gridx = 2;
            options.add(new JLabel("<html><b>Analyse Blocks</b></html>"), constraints);
            constraints.gridx = 0;

            ArrayList<JCheckBox> combine_checks = new ArrayList<>();
            ArrayList<JCheckBox> analyse_checks = new ArrayList<>();
            ArrayList<Integer> block_index = new ArrayList<>();
            JPanel analysePanel = new JPanel();
            analysePanel.setLayout(new BoxLayout(analysePanel, BoxLayout.PAGE_AXIS));
            int cnt = 0;

            for (int i = 0; i < hyper_blocks.size(); i++)
            {
                if (hyper_blocks.get(i).classNum == hyper_blocks.get(c).classNum && i != c)
                {
                    combine_checks.add(new JCheckBox("Block " + (i+1)));
                    block_index.add(i);

                    constraints.gridy = cnt + 1;
                    options.add(combine_checks.get(cnt++), constraints);
                }

                analyse_checks.add(new JCheckBox("Block " + (i+1)));
                analysePanel.add(analyse_checks.get(i));
            }

            constraints.gridx = 2;
            constraints.gridy = 1;
            constraints.gridheight = cnt;
            analysePanel.setMaximumSize(new Dimension(options.getWidth(), options.getHeight()));
            options.add(analysePanel, constraints);

            JButton confirm_combine = new JButton("Confirm Combine");
            confirm_combine.addActionListener(e ->
            {
                // check cases in union
                /*int u_cnt = 0;
                for (int w = 0; w < checks.size(); w++)
                {
                    if (checks.get(w).isSelected())
                    {
                        for (int d = 0; d < obj.size(); d++)
                        {
                            for (DataObject data : obj.get(d))
                            {
                                for (int i = 0; i < data.data.length; i++)
                                {
                                    for (int k = 0; k < hyper_blocks.get(finalC).hyper_block.size(); k++)
                                    {
                                        boolean within_both = false;

                                        for (int j = 0; j < DV.fieldLength; j++)
                                        {
                                            boolean within_cur = false;
                                            boolean within_com = false;

                                            if (data.data[i][j] >= hyper_blocks.get(finalC).minimums.get(k)[j] && data.data[i][j] <= hyper_blocks.get(finalC).maximums.get(k)[j])
                                            {
                                                within_cur = true;
                                            }

                                            if (data.data[i][j] >= hyper_blocks.get(block_index.get(w)).minimums.get(k)[j] && data.data[i][j] <= hyper_blocks.get(block_index.get(w)).maximums.get(k)[j])
                                            {
                                                within_com = true;
                                            }

                                            if ((!within_cur && within_com) || (within_cur && !within_com))
                                            {
                                                within_both = true;
                                            }

                                            if (!within_cur && !within_com)
                                            {
                                                break;
                                            }

                                            if (j == DV.fieldLength - 1)
                                            {
                                                if (within_both)
                                                {
                                                    u_cnt++;
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }


                int tmp_size = block_size.get(finalC);
                // add all selected blocks to current block
                for (int i = 0; i < checks.size(); i++)
                {
                    if (checks.get(i).isSelected())
                    {
                        for (int j = 0; j < hyper_blocks.get(block_index.get(i)).hyper_block.size(); j++)
                        {
                            tmp_size += block_size.get(block_index.get(i));
                        }
                    }
                }

                System.out.println("Union Count: " + u_cnt);
                System.out.println("Block Size: " + tmp_size);
                System.out.println("U Acc: " + tmp_size / u_cnt);*/

                // add all selected blocks to current block
                for (int i = 0; i < combine_checks.size(); i++)
                {
                    if (combine_checks.get(i).isSelected())
                    {
                        for (int j = 0; j < hyper_blocks.get(block_index.get(i)).hyper_block.size(); j++)
                        {
                            hyper_blocks.get(finalC).hyper_block.add(hyper_blocks.get(block_index.get(i)).hyper_block.get(j));
                            artificial.get(finalC).add(artificial.get(block_index.get(i)).get(j));

                            misclassified.set(finalC, misclassified.get(finalC) + misclassified.get(block_index.get(i)));
                            block_size.set(finalC, block_size.get(finalC) + block_size.get(block_index.get(i)));
                            acc.set(finalC, ((double)(block_size.get(finalC) - misclassified.get(finalC)) / block_size.get(finalC)));
                        }
                    }
                }

                // get bounds
                hyper_blocks.get(finalC).getBounds();

                // remove all selected blocks from hyperblocks
                // remove all selected blocks from hyperblocks
                for (int i = 0, offset = 0; i < combine_checks.size(); i++)
                {
                    if (combine_checks.get(i).isSelected())
                    {
                        hyper_blocks.remove(block_index.get(i) - offset);
                        artificial.remove(block_index.get(i) - offset);
                        misclassified.remove(block_index.get(i) - offset);
                        block_size.remove(block_index.get(i) - offset);
                        acc.remove(block_index.get(i) - offset);
                        offset++;
                    }
                }

                visualized_block = 0;

                // redo graphs
                tiles_active = false;
                tile.doClick();
            });

            JButton confirm_analyse = new JButton("Confirm Analyse");
            confirm_analyse.addActionListener(e ->
            {
                // find where blocks are unique
                StringBuilder msg = new StringBuilder("<html><b>Block " + (finalC + 1)+ " Analysis:</b><br/>");

                for (int i = 0; i < analyse_checks.size(); i++)
                {
                    if (analyse_checks.get(i).isSelected())
                    {
                        for (int j = 0; j < hyper_blocks.get(block_index.get(i)).hyper_block.size(); j++)
                        {
                            for (int k = 0; k < DV.fieldLength; k++)
                            {
                                if (hyper_blocks.get(block_index.get(i)).maximums.get(j)[k] < hyper_blocks.get(finalC).minimums.get(j)[k] ||
                                        hyper_blocks.get(block_index.get(i)).minimums.get(j)[k] > hyper_blocks.get(finalC).maximums.get(j)[k])
                                {
                                    if (hyper_blocks.get(block_index.get(i)).hyper_block.size() == 1)
                                        msg.append("&emsp separate from ").append(analyse_checks.get(i).getText()).append(" on attribute ").append(k+1).append("<br/>");
                                    else
                                        msg.append("&emsp separate from block ").append((j + 1)).append("/").append(hyper_blocks.get(block_index.get(i)).hyper_block.size()).append(analyse_checks.get(i).getText()).append(" on attribute ").append(k+1).append("<br/>");
                                }
                            }
                        }
                    }
                }

                JOptionPane.showMessageDialog(pcGraphPanel, msg.toString());
            });

            constraints.gridx = 0;
            constraints.gridy = ++cnt;
            options.add(confirm_combine, constraints);

            constraints.gridx = 2;
            options.add(confirm_analyse, constraints);

            constraints.gridx = 1;
            constraints.gridy = 0;
            constraints.gridheight = ++cnt;
            options.add(new JSeparator(SwingConstants.VERTICAL), constraints);

            chartPanel.setPopupMenu(options);

            tilePanel.add(tmp);
        }

        return tilePanel;
    }

    private JPanel drawGLCBlockTiles(ArrayList<ArrayList<DataObject>> objs)
    {
        int rows = (int)Math.ceil(hyper_blocks.size() / 5.0);

        JPanel tilePanel = new JPanel();
        tilePanel.setLayout(new GridLayout(rows, 5));

        double[] dists = Arrays.copyOf(DV.angles, DV.angles.length);//new double[DV.fieldLength];
        int[] indexes = new int[DV.fieldLength];

        for (int i = 0; i < DV.fieldLength; i++)
        {
            dists[i] = Math.abs(90 - dists[i]);//hyper_blocks.get(visualized_block).maximums.get(0)[i] - hyper_blocks.get(visualized_block).minimums.get(0)[i];
            indexes[i] = i;
        }

        int n = DV.fieldLength;
        for (int i = 0; i < n - 1; i++)
        {
            for (int j = 0; j < n - i - 1; j++)
            {
                if (dists[j] > dists[j + 1])
                {
                    double temp1 = dists[j];
                    dists[j] = dists[j + 1];
                    dists[j + 1] = temp1;

                    int temp2 = indexes[j];
                    indexes[j] = indexes[j + 1];
                    indexes[j + 1] = temp2;
                }
            }
        }

        for (int c = 0; c < hyper_blocks.size(); c++)
        {
            ArrayList<Integer> separation = new ArrayList<>();

            // get separation between blocks
            if (hyper_blocks.get(c).hyper_block.size() > 1)
            {
                for (int j = 1; j < hyper_blocks.get(c).hyper_block.size(); j++)
                {
                    for (int k = 0; k < DV.fieldLength; k++)
                    {
                        if (hyper_blocks.get(c).maximums.get(j)[k] < hyper_blocks.get(0).minimums.get(0)[k] ||
                                hyper_blocks.get(c).minimums.get(j)[k] > hyper_blocks.get(0).maximums.get(0)[k])
                        {
                            separation.add(k);
                        }
                    }
                }
            }

            BasicStroke[] strokes = new BasicStroke[ hyper_blocks.get(c).hyper_block.size()];

            for (int k = 0; k < hyper_blocks.get(c).hyper_block.size(); k++)
            {
                if (hyper_blocks.get(c).hyper_block.get(k).size() > 1)
                {
                    if (k == 0)
                    {
                        strokes[k] = new BasicStroke(3f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);
                    }
                    else
                    {
                        Random r = new Random();
                        float max = 25f;
                        float min = 1f;

                        int len = r.nextInt(2) + 1;

                        float[] fa = new float[len];

                        for (int i = 0; i < len; i++)
                        {
                            fa[i] = r.nextFloat(max - min) + min;
                        }

                        strokes[k] = new BasicStroke(3f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1f, fa, 0f);
                    }
                }
            }

            ArrayList<ChartPanel> charts = new ArrayList<>();

            for (int curClass = 0; curClass < objs.size(); curClass++)
            {
                // get artificial datapoint
                int atri = -1;

                if (hyper_blocks.get(c).hyper_block.size() > 1)
                {
                    for (int i = 0; i < DV.fieldLength; i++)
                    {
                        if (hyper_blocks.get(c).maximums.get(0)[i] < hyper_blocks.get(c).minimums.get(1)[i] || hyper_blocks.get(c).minimums.get(0)[i] > hyper_blocks.get(c).maximums.get(1)[i])
                        {
                            atri = i;
                            break;
                        }
                    }
                }

                // get size
                /*double atri_size_max = 0;
                double atri_size_min = 0;

                if (hyper_blocks.get(c).hyper_block.size() > 1)
                {
                    atri_size_max = (hyper_blocks.get(c).maximums.get(0)[atri] + hyper_blocks.get(c).maximums.get(1)[atri]) / 2.0;
                    atri_size_min = (hyper_blocks.get(c).minimums.get(0)[atri] + hyper_blocks.get(c).minimums.get(1)[atri]) / 2.0;
                }*/

                ArrayList<DataObject> obj = objs.get(curClass);

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
                for (int q = 0, lineCnt = 0; q < obj.size(); q++)
                {
                    DataObject data = obj.get(q);

                    for (int i = 0; i < data.data.length; i++)
                    {
                        // start line at (0, 0)
                        XYSeries line = new XYSeries(lineCnt, false, true);
                        XYSeries endpointSeries = new XYSeries(lineCnt, false, true);
                        XYSeries timeLineSeries = new XYSeries(lineCnt, false, true);

                        if (DV.showFirstSeg)
                            line.add(0, glcBuffer);

                        boolean within = false;
                        int within_block = 0;

                        for (int k = 0; k < hyper_blocks.get(c).hyper_block.size(); k++)
                        {
                            boolean within_cur = true;

                            for (int j = 0; j < data.coordinates[i].length; j++)
                            {
                                if (data.data[i][j] < hyper_blocks.get(c).minimums.get(k)[j] || data.data[i][j] > hyper_blocks.get(c).maximums.get(k)[j])
                                {
                                    within_cur = false;
                                }

                                if (j == data.coordinates[i].length - 1)
                                {
                                    if (within_cur)
                                    {
                                        within_block = k;
                                        within = true;
                                    }
                                }
                            }
                        }

                        /*double[] a_point = DataObject.getXYPointGLC(atri_size_max, 90);

                        a_point[0] += (double)line.getX(0);
                        a_point[1] += (double)line.getY(0);

                        line.add(a_point[0], a_point[1]);*/

                        // add points to lines
                        for (int j = 0; j < data.coordinates[i].length; j++)
                        {
                            //double[] point = DataObject.getXYPointGLC(data.data[i][indexes[j]], DV.angles[indexes[j]]);
                            double[] point = new double[0];

                            if (separation.size() > 0)
                            {
                                boolean not_set = true;

                                for (Integer integer : separation)
                                {
                                    if (indexes[j] == integer)
                                    {
                                        point = DataObject.getXYPointGLC(data.data[i][indexes[j]], DV.angles[indexes[j]]);
                                        not_set = false;
                                        break;
                                    }
                                }

                                if (not_set)
                                {
                                    point = DataObject.getXYPointGLC(data.data[i][indexes[j]] , DV.angles[indexes[j]]);
                                }
                            }
                            else
                            {
                               point = DataObject.getXYPointGLC(data.data[i][indexes[j]], DV.angles[indexes[j]]);
                            }

                            point[0] += (double)line.getX(j);
                            point[1] += (double)line.getY(j);

                            line.add(point[0], point[1]);

                            //if (j > 0 && j < data.coordinates[i].length - 1 && DV.angles[indexes[j]] == DV.angles[indexes[j+1]])
                            //midpointSeries.add(point[0], point[1]);

                            // add endpoint and timeline
                            if (j == data.coordinates[i].length - 1)
                            {
                                if (visualizeWithin.isSelected())
                                {
                                    if (within)
                                    {
                                        endpointSeries.add(point[0], point[1]);
                                        timeLineSeries.add(point[0], 0);

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

                                        lineRenderer.setSeriesStroke(lineCnt, strokes[within_block]);

                                        lineCnt++;
                                    }
                                }
                                else
                                {
                                    endpointSeries.add(point[0], point[1]);
                                    timeLineSeries.add(point[0], 0);

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

                                    if (within)
                                        lineRenderer.setSeriesStroke(lineCnt, strokes[within_block]);

                                    lineCnt++;
                                }
                            }
                        }
                    }
                }

                // add data to series
                midpoints.addSeries(midpointSeries);

                // add hyperblocks
                // generate xy points for minimums and maximums
                for (int k = 0, cnt = 0; k < hyper_blocks.get(c).hyper_block.size(); k++)
                {
                    if (hyper_blocks.get(c).hyper_block.get(k).size() > 1)
                    {
                        glcBlockRenderer[curClass].setSeriesPaint(cnt, Color.ORANGE);
                        glcBlockRenderer[curClass].setSeriesStroke(cnt, strokes[k]);
                        XYSeries max_bound = new XYSeries(cnt++, false, true);
                        glcBlockRenderer[curClass].setSeriesPaint(cnt, Color.ORANGE);
                        glcBlockRenderer[curClass].setSeriesStroke(cnt, strokes[k]);
                        XYSeries min_bound = new XYSeries(cnt++, false, true);

                        XYSeries area = new XYSeries(k, false, true);

                        double[] xyCurPointMin = new double[0];// = DataObject.getXYPointGLC(hyper_blocks.get(c).minimums.get(k)[indexes[0]], DV.angles[indexes[0]]);
                        double[] xyCurPointMax = new double[0];// = DataObject.getXYPointGLC(hyper_blocks.get(c).maximums.get(k)[indexes[0]], DV.angles[indexes[0]]);

                        if (separation.size() > 0)
                        {
                            boolean not_set = true;

                            for (Integer integer : separation)
                            {
                                if (indexes[0] == integer)
                                {
                                    xyCurPointMin = DataObject.getXYPointGLC(hyper_blocks.get(c).minimums.get(k)[indexes[0]], DV.angles[indexes[0]]);
                                    xyCurPointMax = DataObject.getXYPointGLC(hyper_blocks.get(c).maximums.get(k)[indexes[0]], DV.angles[indexes[0]]);
                                    not_set = false;
                                    break;
                                }
                            }

                            if (not_set)
                            {
                                xyCurPointMin = DataObject.getXYPointGLC(hyper_blocks.get(c).minimums.get(k)[indexes[0]] , DV.angles[indexes[0]]);
                                xyCurPointMax = DataObject.getXYPointGLC(hyper_blocks.get(c).maximums.get(k)[indexes[0]] , DV.angles[indexes[0]]);
                            }
                        }
                        else
                        {
                            xyCurPointMin = DataObject.getXYPointGLC(hyper_blocks.get(c).minimums.get(k)[indexes[0]], DV.angles[indexes[0]]);
                            xyCurPointMax = DataObject.getXYPointGLC(hyper_blocks.get(c).maximums.get(k)[indexes[0]], DV.angles[indexes[0]]);
                        }

                        xyCurPointMin[1] += glcBuffer;
                        xyCurPointMax[1] += glcBuffer;

                        if (DV.showFirstSeg) {
                            max_bound.add(0, glcBuffer);
                            min_bound.add(0, glcBuffer);
                            //area.add(0, glcBuffer);
                        }

                        /*double[] a_point = DataObject.getXYPointGLC(atri_size_max, 90);

                        max_bound.add(a_point[0], a_point[1] + glcBuffer);
                        min_bound.add(a_point[0], a_point[1] + glcBuffer);

                        xyCurPointMax[0] += a_point[0];
                        xyCurPointMin[0] += a_point[0];
                        xyCurPointMax[1] += a_point[1];
                        xyCurPointMin[1] += a_point[1];*/

                        min_bound.add(xyCurPointMin[0], xyCurPointMin[1]);
                        max_bound.add(xyCurPointMax[0], xyCurPointMax[1]);
                        //area.add(xyOriginPointMin[0], xyOriginPointMin[1]);

                        for (int j = 1; j < DV.fieldLength; j++)
                        {
                            double[] xyPoint = new double[0];//DataObject.getXYPointGLC(hyper_blocks.get(c).minimums.get(k)[indexes[j]], DV.angles[indexes[j]]);

                            if (separation.size() > 0)
                            {
                                boolean not_set = true;

                                for (Integer integer : separation)
                                {
                                    if (indexes[j] == integer)
                                    {
                                        xyPoint = DataObject.getXYPointGLC(hyper_blocks.get(c).minimums.get(k)[indexes[j]], DV.angles[indexes[j]]);
                                        not_set = false;
                                        break;
                                    }
                                }

                                if (not_set)
                                {
                                    xyPoint = DataObject.getXYPointGLC(hyper_blocks.get(c).minimums.get(k)[indexes[j]] , DV.angles[indexes[j]]);
                                }
                            }
                            else
                            {
                                xyPoint = DataObject.getXYPointGLC(hyper_blocks.get(c).minimums.get(k)[indexes[j]], DV.angles[indexes[j]]);
                            }

                            xyCurPointMin[0] = xyCurPointMin[0] + xyPoint[0];
                            xyCurPointMin[1] = xyCurPointMin[1] + xyPoint[1];

                            min_bound.add(xyCurPointMin[0], xyCurPointMin[1]);
                            //area.add(xyCurPointMin[0], xyCurPointMin[1]);

                            // get maximums
                            //xyPoint = DataObject.getXYPointGLC(hyper_blocks.get(c).maximums.get(k)[indexes[j]], DV.angles[indexes[j]]);
                            if (separation.size() > 0)
                            {
                                boolean not_set = true;

                                for (Integer integer : separation)
                                {
                                    if (indexes[j] == integer)
                                    {
                                        xyPoint = DataObject.getXYPointGLC(hyper_blocks.get(c).maximums.get(k)[indexes[j]], DV.angles[indexes[j]]);
                                        not_set = false;
                                        break;
                                    }
                                }

                                if (not_set)
                                {
                                    xyPoint = DataObject.getXYPointGLC(hyper_blocks.get(c).maximums.get(k)[indexes[j]] , DV.angles[indexes[j]]);
                                }
                            }
                            else
                            {
                                xyPoint = DataObject.getXYPointGLC(hyper_blocks.get(c).maximums.get(k)[indexes[j]], DV.angles[indexes[j]]);
                            }

                            xyCurPointMax[0] = xyCurPointMax[0] + xyPoint[0];
                            xyCurPointMax[1] = xyCurPointMax[1] + xyPoint[1];

                            max_bound.add(xyCurPointMax[0], xyCurPointMax[1]);
                        }

                        //max_bound.add(xyCurPointMin[0], xyCurPointMin[1]);
                        //area.add(xyCurPointMax[0], xyCurPointMax[1]);

                        glcBlocks[curClass].addSeries(max_bound);
                        glcBlocks[curClass].addSeries(min_bound);
                        glcBlocksArea[curClass].addSeries(area);
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
                charts.add(chartPanel);
            }

            JPanel tmp = new JPanel();
            tmp.setLayout(new BoxLayout(tmp, BoxLayout.PAGE_AXIS));

            for (ChartPanel chartPanel : charts)
            {
                tmp.add(chartPanel);

                int finalC = c;
                chartPanel.addChartMouseListener(new ChartMouseListener() {
                    @Override
                    public void chartMouseClicked(ChartMouseEvent chartMouseEvent)
                    {
                        if (SwingUtilities.isLeftMouseButton(chartMouseEvent.getTrigger()))
                        {
                            visualized_block = finalC;
                            tile.doClick();
                        }
                    }

                    @Override
                    public void chartMouseMoved(ChartMouseEvent chartMouseEvent) {}
                });

                JPopupMenu combine = new JPopupMenu("Combine Blocks");
                combine.add(new JLabel("<html><b>Combine Blocks</b></html>"));

                ArrayList<JCheckBox> checks = new ArrayList<>();
                ArrayList<Integer> block_index = new ArrayList<>();

                for (int i = 0, cnt = 0; i < hyper_blocks.size(); i++)
                {
                    if (hyper_blocks.get(i).classNum == hyper_blocks.get(c).classNum && i != c)
                    {
                        checks.add(new JCheckBox("Block " + (i+1)));
                        block_index.add(i);
                        combine.add(checks.get(cnt++));
                    }
                }

                JButton confirm = new JButton("Confirm Selection");
                confirm.addActionListener(e ->
                {
                    // check cases in union of blocks


                    // add all selected blocks to current block
                    for (int i = 0; i < checks.size(); i++)
                    {
                        if (checks.get(i).isSelected())
                        {
                            for (int j = 0; j < hyper_blocks.get(block_index.get(i)).hyper_block.size(); j++)
                            {
                                hyper_blocks.get(finalC).hyper_block.add(hyper_blocks.get(block_index.get(i)).hyper_block.get(j));
                                artificial.get(finalC).add(artificial.get(block_index.get(i)).get(j));

                                misclassified.set(finalC, misclassified.get(finalC) + misclassified.get(block_index.get(i)));
                                block_size.set(finalC, block_size.get(finalC) + block_size.get(block_index.get(i)));
                                acc.set(finalC, ((double)(block_size.get(finalC) - misclassified.get(finalC)) / block_size.get(finalC)));
                            }
                        }
                    }

                    // get bounds
                    hyper_blocks.get(finalC).getBounds();

                    // remove all selected blocks from hyperblocks
                    for (int i = 0, offset = 0; i < checks.size(); i++)
                    {
                        if (checks.get(i).isSelected())
                        {
                            hyper_blocks.remove(block_index.get(i) - offset);
                            artificial.remove(block_index.get(i) - offset);
                            misclassified.remove(block_index.get(i) - offset);
                            block_size.remove(block_index.get(i) - offset);
                            acc.remove(block_index.get(i) - offset);
                            offset++;
                        }
                    }

                    visualized_block = 0;

                    // redo graphs
                    tiles_active = false;
                    tile.doClick();
                });

                combine.add(confirm);
                chartPanel.setPopupMenu(combine);
            }

            tmp.add(new JLabel(block_desc(c)));

            tilePanel.add(tmp);
        }

        return tilePanel;
    }
}
