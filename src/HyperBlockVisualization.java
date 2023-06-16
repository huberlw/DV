import org.jfree.chart.*;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.labels.XYItemLabelGenerator;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.DefaultDrawingSupplier;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYAreaRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYDataset;
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
    int original_num = 0;

    JPanel graphPanel;

    JCheckBox visualizeWithin;
    JCheckBox visualizeOutline;
    JLabel graphLabel;
    JButton right;
    JButton left;
    JButton tile;
    int visualized_block = 0;
    boolean tiles_active = false;

    ArrayList<ArrayList<DataObject>> objects;
    ArrayList<DataObject> lowerObjects;
    ArrayList<DataObject> upperObjects;
    ArrayList<ArrayList<DataObject>> originalObjects;
    ArrayList<HyperBlock> originalHyperBlocks;

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

    JRadioButton separateView;
    JRadioButton tileView;
    JRadioButton combinedClassView;
    JRadioButton combinedView;
    boolean indSep = true;

    JRadioButton stuff;


    // plot options
    JRadioButton PC;
    JRadioButton SPC;
    JRadioButton GLCL;
    JRadioButton PC_cross;
    JRadioButton SPC_less;
    JRadioButton GLCL_turn;
    JRadioButton GLCL_turn_dot;
    JRadioButton GLCL_x_proj_dot;

    // hb level inc
    JSpinner hb_lvl;

    // plot choice
    int plot_id = 0;

    boolean lvl_inc = false;

    int new_case = 0;


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

    private String block_desc_tmp(int block)
    {
        return "<html><b>Block:</b> " + (block + 1) + "/" + hyper_blocks.size() + "</html>";
    }

    private String block_desc(int block)
    {
        StringBuilder tmp = new StringBuilder("<b>Rule:</b> if ");
        for (int i = 0; i < DV.fieldLength; i++)
        {
            if (hyper_blocks.get(block).maximums.get(0)[i] > 0)
            {
                if (hyper_blocks.get(block).minimums.get(0)[i] > 0)
                    tmp.append(String.format("%.2f &le; X%d &le; %.2f", hyper_blocks.get(block).minimums.get(0)[i], i, hyper_blocks.get(block).maximums.get(0)[i]));
                else
                    tmp.append(String.format("X%d &le; %.2f", i, hyper_blocks.get(block).maximums.get(0)[i]));

                if (i != DV.fieldLength - 1)
                    tmp.append(", ");
                else
                    tmp.append(", then class").append(hyper_blocks.get(block).className);
            }
        }

        String desc = "<html><b>Block:</b> " + (block + 1) + "/" + hyper_blocks.size() + "<br/>";
        desc += "<b>Class:</b> " + hyper_blocks.get(block).className + "<br/>";
        desc += "<b>Seed Attribute:</b> " + hyper_blocks.get(block).attribute+ "<br/>";
        desc += "<b>Datapoints:</b> " + hyper_blocks.get(block).size + " (" + misclassified.get(block) + " misclassified)" + "<br/>";
        desc += "<b>Accuracy:</b> " + (Math.round(acc.get(block) * 10000) / 100.0) + "%<br/>";
        desc += tmp;
        desc += "</html>";

        return desc;
    }

    private void choose_plot()
    {
        switch (plot_id)
        {
            case 0 ->
            { // PC
                if (separateView.isSelected())
                {
                    graphPanel.removeAll();
                    graphPanel.add(drawPCBlocks(objects));
                }
                else if (tileView.isSelected())
                {
                    if (tiles_active)
                    {
                        graphPanel.add(drawPCBlockTiles(objects));
                        graphLabel.setText("");
                    }
                    else
                    {
                        graphPanel.add(drawPCBlocks(objects));
                        graphLabel.setText(block_desc(visualized_block));
                    }
                }
                else if (combinedClassView.isSelected())
                {
                    graphPanel.add(drawPCBlockTilesCombinedClasses(objects));
                }
                else if (combinedView.isSelected())
                {
                    graphPanel.add(drawPCBlockTilesCombined(objects));
                }
            }
            case 1 ->
            { // GLC-L
                if (separateView.isSelected())
                {
                    graphPanel.removeAll();
                    graphPanel.add(drawGLCBlocks(upperObjects, 0));
                    graphPanel.add(drawGLCBlocks(lowerObjects, 1));
                }
                else if (tileView.isSelected())
                {
                    if (tiles_active)
                    {
                        graphPanel.add(drawGLCBlockTiles(objects));
                        graphLabel.setText("");
                    }
                    else
                    {
                        graphPanel.add(drawGLCBlocks(upperObjects, 0));
                        graphPanel.add(drawGLCBlocks(lowerObjects, 1));
                        graphLabel.setText(block_desc(visualized_block));
                    }
                }
                else if (combinedClassView.isSelected())
                {
                    graphPanel.add(drawGLCBlockTilesCombinedClasses(objects));
                }
                else if (combinedView.isSelected())
                {
                    graphPanel.add(drawGLCBlockTilesCombined(objects));
                }
            }
            case 2 ->
            { // SPC
                if (separateView.isSelected())
                {
                    graphPanel.removeAll();
                    graphPanel.add(SPC(objects));
                }
                else if (tileView.isSelected())
                {
                }
                else if (combinedClassView.isSelected())
                {
                }
                else if (combinedView.isSelected())
                {
                }
            }
            case 3 ->
            { // PC cross
                if (separateView.isSelected())
                {
                    graphPanel.removeAll();
                    graphPanel.add(drawStuff(visualized_block));
                }
                else if (tileView.isSelected())
                {
                }
                else if (combinedClassView.isSelected())
                {
                }
                else if (combinedView.isSelected())
                {
                }
            }
            case 4 ->
            { // SPC reduced
                if (separateView.isSelected())
                {
                    graphPanel.removeAll();
                    graphPanel.add(SPC_Reduced(objects));
                }
                else if (tileView.isSelected())
                {
                }
                else if (combinedClassView.isSelected())
                {
                }
                else if (combinedView.isSelected())
                {
                }
            }
        }
    }

    private void increase_level()
    {
        originalObjects = new ArrayList<>();
        originalObjects.addAll(objects);

        originalHyperBlocks = new ArrayList<>();
        originalHyperBlocks.addAll(hyper_blocks);

        for (int i = 0; i < hyper_blocks.size(); i++)
        {
            //drawStuff(i);
        }

        ArrayList<double[]> tmpData = new ArrayList<>();
        ArrayList<double[]> tmpData2 = new ArrayList<>();

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
                    tmpData.add(tmp1);
                else
                    tmpData2.add(tmp1);
            }
        }

        objects = new ArrayList<>();
        upperObjects = new ArrayList<>();
        lowerObjects = new ArrayList<>();

        double[][] newData = new double[tmpData.size()][DV.fieldLength * 2];
        tmpData.toArray(newData);
        DataObject newObj = new DataObject("upper", newData);

        double[][] newData2 = new double[tmpData2.size()][DV.fieldLength * 2];
        tmpData2.toArray(newData2);
        DataObject newObj2 = new DataObject("lower", newData2);

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

        //combineAll();

        //for (int i = 0; i < hyper_blocks.size(); i++)
            //pc_lvl_2_Test2(objects, i);
    }

    public HyperBlockVisualization()
    {
        //getNonOverlapData();
        getData();
        generateHyperblocks3();
        blockCheck();

        JFrame mainFrame = new JFrame();
        mainFrame.setLayout(new BorderLayout());

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        JPanel btnsPanel = new JPanel();
        btnsPanel.setLayout(new GridLayout(1, 2));

        // create toolbar
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.setRollover(true);

        JLabel plots = new JLabel("Plot Options: ");
        plots.setFont(plots.getFont().deriveFont(Font.BOLD, 12f));
        toolBar.add(plots);
        toolBar.addSeparator();

        ButtonGroup plotBtns = new ButtonGroup();

        PC = new JRadioButton("PC", true);
        PC.addActionListener(pce ->
        {
            plot_id = 0;
            updateGraphs();
        });
        toolBar.add(PC);
        toolBar.addSeparator();

        GLCL = new JRadioButton("GLC-L", false);
        GLCL.addActionListener(pce ->
        {
            plot_id = 1;
            updateGraphs();
        });
        toolBar.add(GLCL);
        toolBar.addSeparator();

        SPC = new JRadioButton("SPC-1B", false);
        SPC.addActionListener(pce ->
        {
            plot_id = 2;
            updateGraphs();
        });
        toolBar.add(SPC);
        toolBar.addSeparator();

        PC_cross = new JRadioButton("PC-2n", false);
        PC_cross.addActionListener(pcce ->
        {
            plot_id = 3;
            updateGraphs();
        });
        toolBar.add(PC_cross);
        toolBar.addSeparator();

        SPC_less = new JRadioButton("SPC-2B", false);
        SPC_less.addActionListener(spcle ->
        {
            plot_id = 4;
            updateGraphs();
        });
        toolBar.add(SPC_less);
        toolBar.addSeparator();
        toolBar.addSeparator();
        toolBar.addSeparator();

        plotBtns.add(PC);
        plotBtns.add(SPC);
        plotBtns.add(GLCL);
        plotBtns.add(PC_cross);
        plotBtns.add(SPC_less);

        JLabel views = new JLabel("View Options: ");
        views.setFont(views.getFont().deriveFont(Font.BOLD, 12f));
        toolBar.add(views);
        toolBar.addSeparator();

        ButtonGroup viewBtns = new ButtonGroup();

        separateView = new JRadioButton("Separate Blocks", true);
        separateView.setToolTipText("View all hyperblocks separately.");
        separateView.addActionListener(e ->
        {
            btnsPanel.removeAll();
            btnsPanel.add(left);
            btnsPanel.add(right);

            if (tiles_active)
            {
                tiles_active = false;
            }
            else
            {
                //visualized_block++;

                if (visualized_block > hyper_blocks.size() - 1)
                    visualized_block = 0;
            }

            choose_plot();

            graphPanel.revalidate();
            graphPanel.repaint();

            graphLabel.setText(block_desc(visualized_block));
        });
        toolBar.add(separateView);
        toolBar.addSeparator();

        tileView = new JRadioButton("All Blocks");
        tileView.setToolTipText("View all hyperblocks.");
        tileView.addActionListener(e ->
        {
            btnsPanel.removeAll();

            graphPanel.removeAll();

            tiles_active = !tiles_active;

            choose_plot();

            graphPanel.revalidate();
            graphPanel.repaint();
        });
        toolBar.add(tileView);
        toolBar.addSeparator();

        combinedClassView = new JRadioButton("Class Combined Blocks");
        combinedClassView.setToolTipText("View all hyperblocks with hyperblocks of the same class combined.");
        combinedClassView.addActionListener(e ->
        {
            btnsPanel.removeAll();

            JRadioButton viewIndSep = new JRadioButton("View Individual Hyperblocks Separately", indSep);
            viewIndSep.setFont(viewIndSep.getFont().deriveFont(Font.BOLD, 20f));
            viewIndSep.setToolTipText("View hyperblocks containing a single case in separate graphs");
            viewIndSep.addActionListener(al ->
            {
                indSep = !indSep;
                updateGraphs();
            });

            btnsPanel.add(viewIndSep);

            graphPanel.removeAll();

            tiles_active = false;
            visualized_block = 0;
            graphLabel.setText("");

            choose_plot();

            graphPanel.revalidate();
            graphPanel.repaint();
        });
        toolBar.add(combinedClassView);
        toolBar.addSeparator();

        combinedView = new JRadioButton("Combined Blocks");
        combinedView.setToolTipText("View all hyperblocks combined.");
        combinedView.addActionListener(e ->
        {
            btnsPanel.removeAll();

            JRadioButton viewIndSep = new JRadioButton("View Individual Hyperblocks Separately", indSep);
            viewIndSep.setFont(viewIndSep.getFont().deriveFont(Font.BOLD, 20f));
            viewIndSep.setToolTipText("View hyperblocks containing a single case in a separate graph");
            viewIndSep.addActionListener(al ->
            {
                indSep = !indSep;
                updateGraphs();
            });

            btnsPanel.add(viewIndSep);

            graphPanel.removeAll();

            tiles_active = false;
            visualized_block = 0;
            graphLabel.setText("");

            choose_plot();

            graphPanel.revalidate();
            graphPanel.repaint();
        });
        toolBar.add(combinedView);
        toolBar.addSeparator();
        toolBar.addSeparator();
        toolBar.addSeparator();

        viewBtns.add(separateView);
        viewBtns.add(tileView);
        viewBtns.add(combinedClassView);
        viewBtns.add(combinedView);

        JLabel blockType = new JLabel("Hyperblock Options: ");
        blockType.setFont(blockType.getFont().deriveFont(Font.BOLD, 12f));
        toolBar.add(blockType);
        toolBar.addSeparator();

        ButtonGroup dataBtns = new ButtonGroup();

        // default blocks
        JRadioButton defaultBlocksBtn = new JRadioButton("Default Blocks", true);
        defaultBlocksBtn.setToolTipText("Rebuild blocks using all data.");
        defaultBlocksBtn.addActionListener(e ->
        {
            getData();
            generateHyperblocks3();
            blockCheck();

            if (separateView.isSelected())
                separateView.doClick();
            else if (tileView.isSelected())
                tileView.doClick();
            else if (combinedClassView.isSelected())
                combinedClassView.doClick();
            else
                combinedView.doClick();
        });
        toolBar.add(defaultBlocksBtn);
        toolBar.addSeparator();

        /**
         * CHANGE THIS
         * stuff = Draw boxes for original SPC visualization
         */
        stuff = new JRadioButton("", true);
        //toolBar.add(stuff);

        // overlap blocks
        JRadioButton overlapBlocksBtn = new JRadioButton("Overlap Blocks");
        overlapBlocksBtn.setToolTipText("Rebuild blocks using only data in the overlap area.");
        overlapBlocksBtn.addActionListener(e ->
        {
            getOverlapData();
            generateHyperblocks3();
            blockCheck();

            if (separateView.isSelected())
                separateView.doClick();
            else if (tileView.isSelected())
                tileView.doClick();
            else if (combinedClassView.isSelected())
                combinedClassView.doClick();
            else
                combinedView.doClick();
        });
        toolBar.add(overlapBlocksBtn);
        toolBar.addSeparator();

        // non-overlap blocks
        JRadioButton nonOverlapBlocksBtn = new JRadioButton("Non-Overlap Blocks");
        nonOverlapBlocksBtn.setToolTipText("Rebuild blocks using only data not in the overlap area.");
        nonOverlapBlocksBtn.addActionListener(e ->
        {
            getNonOverlapData();
            generateHyperblocks3();
            blockCheck();

            if (separateView.isSelected())
                separateView.doClick();
            else if (tileView.isSelected())
                tileView.doClick();
            else if (combinedClassView.isSelected())
                combinedClassView.doClick();
            else
                combinedView.doClick();
        });
        toolBar.add(nonOverlapBlocksBtn);
        toolBar.addSeparator();
        toolBar.addSeparator();
        toolBar.addSeparator();

        dataBtns.add(defaultBlocksBtn);
        dataBtns.add(overlapBlocksBtn);
        dataBtns.add(nonOverlapBlocksBtn);

        mainFrame.add(toolBar, BorderLayout.PAGE_START);


        graphPanel = new JPanel();
        graphPanel.setLayout(new BoxLayout(graphPanel, BoxLayout.PAGE_AXIS));

        c.weightx = 1;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        c.ipady = 10;
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        mainPanel.add(graphPanel, c);

        graphLabel = new JLabel("");
        graphLabel.setFont(graphLabel.getFont().deriveFont(20f));
        graphLabel.setToolTipText("The seed attribute starts the process of building and refining a block." +
                                "It contains the available points in an interval above a purity threshold to build a block at a given stage.");

        graphLabel.setText(block_desc(visualized_block));

        c.gridy = 1;
        c.weighty = 0;
        mainPanel.add(graphLabel, c);

        JLabel dataView = new JLabel("Data View Options: ");
        dataView.setFont(dataView.getFont().deriveFont(Font.BOLD, 12f));
        toolBar.add(dataView);
        toolBar.addSeparator();

        visualizeWithin = new JCheckBox("Within Block", true);
        visualizeWithin.setToolTipText("Only Visualize Datapoints Within Block");
        visualizeWithin.addActionListener(al -> updateGraphs());
        toolBar.add(visualizeWithin);
        toolBar.addSeparator();

        visualizeOutline = new JCheckBox("Outline Block", false);
        visualizeOutline.setToolTipText("Only Visualize Outline of Block");
        visualizeOutline.addActionListener(al -> updateGraphs());
        toolBar.add(visualizeOutline);
        toolBar.addSeparator();
        toolBar.addSeparator();
        toolBar.addSeparator();

        JLabel lvlView = new JLabel("HB Level: ");
        lvlView.setFont(lvlView.getFont().deriveFont(Font.BOLD, 12f));
        toolBar.add(lvlView);

        hb_lvl = new JSpinner(new SpinnerNumberModel(1, 1, 3, 1));
        hb_lvl.setEditor(new JSpinner.NumberEditor(hb_lvl, "0"));
        hb_lvl.addChangeListener(hbe ->
        {
            if ((Integer) hb_lvl.getValue() == 2 || (Integer) hb_lvl.getValue() == 3)
            {
                increase_level();
                updateGraphs();
            }
        });
        toolBar.add(hb_lvl);
        toolBar.addSeparator();

        /**
         * REMOVE THIS LATER
         * JUST FOR TESTING NEW CASES
         */
        JButton ADD_NEW_CASE = new JButton("TEST CASE");
        ADD_NEW_CASE.addActionListener(e -> classify_new_case());
        toolBar.add(ADD_NEW_CASE);
        /**
         * REMOVE THIS LATER
         * JUST FOR TESTING NEW CASES
         */

        choose_plot();

        left = new JButton("Previous Block");
        btnsPanel.add(left);

        right = new JButton("Next Block");
        btnsPanel.add(right, c);

        mainFrame.add(btnsPanel, BorderLayout.PAGE_END);

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

            graphLabel.setText(block_desc(visualized_block));
            choose_plot();

            graphPanel.revalidate();
            graphPanel.repaint();
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

            graphLabel.setText(block_desc(visualized_block));
            choose_plot();

            graphPanel.revalidate();
            graphPanel.repaint();
        });

        mainFrame.add(mainPanel, BorderLayout.CENTER);

        // show
        mainFrame.setVisible(true);
        mainFrame.revalidate();
        mainFrame.pack();
        mainFrame.repaint();
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
                        tmp.getBounds();
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
                    tmp.getBounds();

                    // store this index to delete the cube that was combined
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
    }


    // create new HB around test data
    private void classify_new_case()
    {
        original_num = hyper_blocks.size();

        int cnt = 0;

        for (int i = 0; i < DV.testData.size(); i++)
        {
            for (int j = 0; j < DV.testData.get(i).data.length; j++)
            {
                /*if (cnt == new_case)
                {*/
                    // add to existing hyperblock or generate new hyperblock
                    if (!in_HB(DV.testData.get(i).data[j]))
                    {
                        System.out.println("\nCREATING NEW HB!");
                        System.out.println("Test Case Class: " + i);
                        create_kNN_HB(DV.testData.get(i).data[j], 5);
                    }

                    else
                        System.out.println("NEW CASE " + new_case + " IS ALREADY WITHIN AN HB");

                    new_case++;
                    /*return;
                }
                else
                    cnt++;*/
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
    private void create_kNN_HB(double[] c, int k)
    {
        // create HB
        HyperBlock hb_c = kNN(c, k);
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
        ArrayList<ArrayList<double[]>> data_c = new ArrayList<>(List.of(cluster));

        // create HB
        HyperBlock hb_c = new HyperBlock(data_c);
        hb_c.classNum = classify_hb(classes);

        return hb_c;
    }

    private int classify_hb(int[] classes)
    {
        int x = 0, y = 0;

        for (int i = 0; i < classes.length; i++)
        {
            if (classes[i] == 0) x++;
            else y++;
        }

        System.out.println("HB Purity: " + (x > y ? (x / (double)(x+y)) : (y / (double)(x+y))));
        return x > y ? 0 : 1;
    }

    private double euclidean_distance(double[] x, double[] y)
    {
        double dist = 0;

        for (int i = 0; i < x.length; i++)
            dist += Math.pow((x[i] - y[i]), 2);

        return Math.sqrt(dist);
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
                boolean ldf_cor = true;

                int cor_index = attributes.get(a).get(i).index;
                double[] cor_pnt = new double[attributes.size()];

                for (int k = 0; k < attributes.size(); k++)
                {
                    for (int h = 0; h < attributes.get(k).size(); h++)
                    {
                        if (cor_index == attributes.get(k).get(h).index)
                        {
                            // add point
                            cor_pnt[k] = attributes.get(k).get(h).value;
                            break;
                        }
                    }
                }

                for (int m = 0; m < DV.misclassifiedData.size(); m++)
                {
                    if (Arrays.deepEquals(new Object[]{ DV.misclassifiedData.get(m) }, new Object[]{ cor_pnt }))
                    {
                        ldf_cor = false;
                        break;
                    }
                }

                if (attributes.get(a).get(start).index < upper && attributes.get(a).get(i).index < upper && ldf_cor)
                {
                    tmp.add(attributes.get(a).get(i));

                    if (tmp.size() > ans.get(a).size())
                    {
                        ans.set(a, new ArrayList<>(tmp));
                        atri.set(a, a);
                        cls.set(a, 0);
                    }
                }
                else if (attributes.get(a).get(start).index >= upper && attributes.get(a).get(i).index >= upper && ldf_cor)
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
                    /*boolean ldf_mis = false;

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
                    }*/

                    // misclassified by ldf and misclassified by block
                    if (!ldf_cor && !((attributes.get(a).get(start).index < upper && attributes.get(a).get(i).index < upper) ||
                            (attributes.get(a).get(start).index >= upper && attributes.get(a).get(i).index >= upper)))
                    {
                        misclassified++;

                        if ((tmp.size() - misclassified) / (tmp.size()) >= acc_threshold)
                        {
                            tmp.add(attributes.get(a).get(i));

                            if (tmp.size() > ans.get(a).size())
                            {
                                ans.set(a, new ArrayList<>(tmp));
                            }
                        }
                        else if (i > 0)
                        {
                            int cnt = 0;
                            // classes are different, but the value is still the same
                            if (attributes.get(a).get(i-1).value == attributes.get(a).get(i).value)
                            {
                                double remove_val = attributes.get(a).get(i).value;

                                // increment until value changes
                                while (remove_val == attributes.get(a).get(i).value)
                                {
                                    if (i < attributes.get(a).size()-1)
                                    {
                                        i++;
                                        cnt++;
                                    }
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

                            if (attributes.get(a).get(i-1-cnt).value == attributes.get(a).get(i-cnt).value)
                                start = i;
                            else
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

                                start = i;
                            }

                            tmp.clear();
                            misclassified = 0;
                        }
                        else
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

                            start = i;
                            tmp.clear();
                            misclassified = 0;
                        }
                    }
                    else if (i > 1)
                    {
                        int cnt = 0;
                        // classes are different, but the value is still the same
                        if (attributes.get(a).get(i-1).value == attributes.get(a).get(i).value)
                        {
                            double remove_val = attributes.get(a).get(i).value;

                            // increment until value changes
                            while (remove_val == attributes.get(a).get(i).value)
                            {
                                if (i < attributes.get(a).size()-1)
                                {
                                    i++;
                                    cnt++;
                                }
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

                        if (attributes.get(a).get(i-1-cnt).value == attributes.get(a).get(i-cnt).value)
                            start = i;
                        else
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

                            start = i;
                        }
                        tmp.clear();
                        misclassified = 0;
                    }
                    else
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

                        start = i;
                        tmp.clear();
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
            // sort hb by index
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
                    /*if (Arrays.deepEquals(new Object[]{DV.misclassifiedData.get(i)}, new Object[]{data.get(j).get(k)}))
                    {
                        cnt++;
                    }*/
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
        if (separateView.isSelected())
            separateView.doClick();
        else if (tileView.isSelected())
            tileView.doClick();
        else if (combinedClassView.isSelected())
            combinedClassView.doClick();
        else
            combinedView.doClick();
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
                                if (!visualizeOutline.isSelected() && within)
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
                                    if (!(visualizeOutline.isSelected() && within))
                                        goodGraphLines.addSeries(line);

                                    goodLineRenderer.setSeriesPaint(lineCnt, DV.graphColors[d]);

                                    if (within)
                                        goodLineRenderer.setSeriesStroke(lineCnt, strokes[within_block]);
                                }
                                else
                                {
                                    if (!(visualizeOutline.isSelected() && within))
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

        //artRenderer.setBaseStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        //artRenderer.setAutoPopulateSeriesStroke(false);
        //plot.setRenderer(0, artRenderer);
        //plot.setDataset(0, artLines);

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
                double[] min_pnt = DataObject.getXYPointGLC(hyper_blocks.get(visualized_block).minimums.get(j)[i], DV.angles[i]);
                double[] max_pnt = DataObject.getXYPointGLC(hyper_blocks.get(visualized_block).maximums.get(j)[i], DV.angles[i]);

                amin1 += min_pnt[1];
                amax1 += max_pnt[1];

                min_pnt = DataObject.getXYPointGLC(hyper_blocks.get(visualized_block).minimums.get(j+1)[i], DV.angles[i]);
                max_pnt = DataObject.getXYPointGLC(hyper_blocks.get(visualized_block).maximums.get(j+1)[i], DV.angles[i]);

                amin2 += min_pnt[1];
                amax2 += max_pnt[1];
            }
        }

        // get high of separation
        double smin1 = 0;
        double smax1 = 0;
        double smin2 = 0;
        double smax2 = 0;

        for (int j = 0; j < hyper_blocks.get(visualized_block).minimums.size() - 1; j++)
        {
            for (int i = 0; i < separation.size(); i++)
            {
                smin1 += hyper_blocks.get(visualized_block).minimums.get(j)[separation.get(i)];
                smax1 += hyper_blocks.get(visualized_block).maximums.get(j)[separation.get(i)];

                smin2 += hyper_blocks.get(visualized_block).minimums.get(j+1)[separation.get(i)];
                smax2 += hyper_blocks.get(visualized_block).maximums.get(j+1)[separation.get(i)];
            }
        }

        // get largest maximum
        double scale;
        int apply;
        double thresh_len = Math.max(smax1 + amax1, smax2 + amax2);
        thresh_len = Math.max(thresh_len, DV.fieldLength);

        if (smax1 > smax2)
        {
            apply = 0;
            scale = (amax2 - amin1) / smin1;

            if (scale < 1 || smin1 == 0) scale = 1;
        }
        else
        {
            apply = 1;
            scale = (amax1 - amin2) / smin2;

            if (scale < 1 || smin2 == 0) scale = 1;
        }
        scale = 1;
        separation.clear();

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
        thresholdLine.add(DV.threshold, thresh_len);

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

                if (within_block == apply)
                {
                    for (int s = 0; s < separation.size(); s++)
                    {
                        double[] a_point = DataObject.getXYPointGLC((data.data[i][separation.get(s)]) * scale, 90);

                        a_point[0] += (double)line.getX(s);
                        a_point[1] += (double)line.getY(s);

                        line.add(a_point[0], a_point[1]);
                    }
                }
                else
                {
                    //line.add(0, 0);
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

                if (k == apply)
                {
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
                }
                else
                {
                    //min_bound.add(0, 0);
                    //max_bound.add(0, 0);
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
        int rows = (int)Math.ceil(hyper_blocks.size() / 9.0);

        JPanel tilePanel = new JPanel();
        tilePanel.setLayout(new GridLayout(rows, 9));

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
                                    if (!visualizeOutline.isSelected() && within)
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
                                        if (!(visualizeOutline.isSelected() && within))
                                            goodGraphLines.addSeries(line);

                                        goodLineRenderer.setSeriesPaint(lineCnt, DV.graphColors[d]);

                                        if (within)
                                            goodLineRenderer.setSeriesStroke(lineCnt, strokes[within_block]);
                                    }
                                    else
                                    {
                                        if (!(visualizeOutline.isSelected() && within))
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
                if (hyper_blocks.get(c).hyper_block.get(k).size() > -1)
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

                    if (original_num > 0)
                    {
                        if (c > original_num - 1)
                            pcBlockRenderer.setSeriesPaint(k - offset, Color.RED);
                    }

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

            //tmp.add(new JLabel(block_desc(c)));
            tmp.add(new JLabel(block_desc_tmp(c)));

            int finalC = c;
            chartPanel.addChartMouseListener(new ChartMouseListener()
            {
                @Override
                public void chartMouseClicked(ChartMouseEvent chartMouseEvent)
                {
                    if (SwingUtilities.isLeftMouseButton(chartMouseEvent.getTrigger()))
                    {
                        visualized_block = finalC;
                        separateView.doClick();
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

                JOptionPane.showMessageDialog(graphPanel, msg.toString());
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

        JPanel tmp = new JPanel();
        tmp.setLayout(new GridLayout(rows, 9, 10, 10));

        for (int i = 0; i < hyper_blocks.size(); i++)
        {
            tmp.add(new JLabel(block_desc(i)));
        }

        JOptionPane.showMessageDialog(null, tmp);

        return tilePanel;
    }

    private JPanel drawGLCBlockTiles(ArrayList<ArrayList<DataObject>> objs)
    {
        int rows = (int)Math.ceil(hyper_blocks.size() / 9.0);

        JPanel tilePanel = new JPanel();
        tilePanel.setLayout(new GridLayout(rows, 9));

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

        for (int c = 0; c < hyper_blocks.size(); c++)
        {
            JFreeChart[] graphs = new JFreeChart[2];

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
                //charts.add(chartPanel);

                graphs[curClass] = glcChart[curClass];
            }

            CombinedDomainXYPlot plot = new CombinedDomainXYPlot();
            plot.setOrientation(PlotOrientation.VERTICAL);
            plot.setGap(0);
            plot.getDomainAxis().setVisible(false);
            plot.setOutlinePaint(null);
            plot.setOutlineVisible(false);
            plot.setInsets(RectangleInsets.ZERO_INSETS);
            plot.setDomainPannable(true);
            plot.setRangePannable(true);
            plot.setBackgroundPaint(DV.background);
            plot.setDomainGridlinePaint(Color.GRAY);
            plot.setRangeGridlinePaint(Color.GRAY);

            JFreeChart chart = new JFreeChart("", null, plot, false);

            // add graphs in order
            for (int i = 0; i < graphs.length; i++)
            {
                    plot.add((XYPlot) graphs[i].getPlot(), 1);
            }

            ChartPanel stuff = new ChartPanel(chart);
            charts.add(stuff);

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

            //tmp.add(new JLabel(block_desc(c)));
            tmp.add(new JLabel(block_desc_tmp(c)));

            tilePanel.add(tmp);
        }

        return tilePanel;
    }

    private JPanel drawPCBlockTilesCombinedClasses(ArrayList<ArrayList<DataObject>> obj)
    {
        int good = 0;
        int bad = 0;
        // hyperblock renderer and dataset
        // create main renderer and dataset
        XYLineAndShapeRenderer goodLineRenderer = new XYLineAndShapeRenderer(true, false);
        XYSeriesCollection goodGraphLines = new XYSeriesCollection();
        XYLineAndShapeRenderer badLineRenderer = new XYLineAndShapeRenderer(true, false);
        XYSeriesCollection badGraphLines = new XYSeriesCollection();

        XYLineAndShapeRenderer pcBlockRendererUpper = new XYLineAndShapeRenderer(true, false);
        XYSeriesCollection pcBlocksUpper = new XYSeriesCollection();

        BasicStroke[] strokes = new BasicStroke[hyper_blocks.size()];

        double[] UB = new double[DV.fieldLength];
        double[] LB = new double[DV.fieldLength];

        for (int i = 0; i < DV.fieldLength; i++)
        {
            UB[i] = -999999;
            LB[i] = 999999;
        }

        // add hyperblocks
        for (int c = 0; c < hyper_blocks.size(); c++)
        {
            if (c == 0)
            {
                strokes[c] = new BasicStroke(3f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);
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

                strokes[c] = new BasicStroke(3f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1f, fa, 0f);
            }

            for (int k = 0; k < hyper_blocks.get(c).hyper_block.size(); k++)
            {
                if (hyper_blocks.get(c).classNum == 1)
                {
                    for (int i = 0; i < DV.fieldLength; i++)
                    {
                        if (hyper_blocks.get(c).minimums.get(k)[i] < LB[i])
                        {
                            LB[i] = hyper_blocks.get(c).minimums.get(k)[i];
                        }

                        if (hyper_blocks.get(c).maximums.get(k)[i] > UB[i])
                        {
                            UB[i] = hyper_blocks.get(c).maximums.get(k)[i];
                        }
                    }
                }

            }
        }

        XYSeries tmp1 = new XYSeries(0, false, true);

        for (int j = 0; j < DV.fieldLength; j++) {
            tmp1.add(j, LB[j]);
        }

        for (int j = DV.fieldLength - 1; j > -1; j--) {
            tmp1.add(j, UB[j]);
        }

        tmp1.add(0, LB[0]);

        pcBlockRendererUpper.setSeriesPaint(0, Color.BLACK);
        pcBlockRendererUpper.setSeriesStroke(0, strokes[0]);

        pcBlocksUpper.addSeries(tmp1);

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

                    boolean within_cur = true;

                    for (int j = 0; j < DV.fieldLength; j++)
                    {
                        if (data.data[i][j] < LB[j] || data.data[i][j] > UB[j])
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

                    // add points to lines
                    for (int j = 0; j < DV.fieldLength; j++)
                    {
                        line.add(j, data.data[i][j]);

                        // add endpoint and timeline
                        if (j == DV.fieldLength - 1)
                        {
                            if (!visualizeOutline.isSelected() && within)
                            {
                                // add series
                                if (d == 0)
                                {
                                    goodGraphLines.addSeries(line);

                                    goodLineRenderer.setSeriesPaint(lineCnt, Color.GREEN);
                                    goodLineRenderer.setSeriesStroke(lineCnt, strokes[0]);

                                    good++;
                                }
                                else
                                {
                                    badGraphLines.addSeries(line);

                                    badLineRenderer.setSeriesPaint(lineCnt, Color.RED);
                                    badLineRenderer.setSeriesStroke(lineCnt, strokes[0]);

                                    bad++;
                                }

                                lineCnt++;
                            }
                        }
                    }
                }
            }
        }

        JFreeChart pcChartUpper = ChartFactory.createXYLineChart(
                "",
                "",
                "",
                pcBlocksUpper,
                PlotOrientation.VERTICAL,
                false,
                true,
                false);

        // format chart
        pcChartUpper.setBorderVisible(false);
        pcChartUpper.setPadding(RectangleInsets.ZERO_INSETS);

        // get plot
        XYPlot plotUpper = (XYPlot) pcChartUpper.getPlot();

        // format plot
        plotUpper.setDrawingSupplier(new DefaultDrawingSupplier(
                new Paint[] { DV.graphColors[0] },
                DefaultDrawingSupplier.DEFAULT_OUTLINE_PAINT_SEQUENCE,
                DefaultDrawingSupplier.DEFAULT_STROKE_SEQUENCE,
                DefaultDrawingSupplier.DEFAULT_OUTLINE_STROKE_SEQUENCE,
                DefaultDrawingSupplier.DEFAULT_SHAPE_SEQUENCE));
        plotUpper.getRangeAxis().setVisible(true);
        plotUpper.getDomainAxis().setVisible(true);
        plotUpper.setOutlinePaint(null);
        plotUpper.setOutlineVisible(false);
        plotUpper.setInsets(RectangleInsets.ZERO_INSETS);
        plotUpper.setDomainPannable(true);
        plotUpper.setRangePannable(true);
        plotUpper.setBackgroundPaint(DV.background);
        plotUpper.setDomainGridlinePaint(Color.GRAY);

        // set domain
        ValueAxis domainView = plotUpper.getDomainAxis();
        domainView.setRange(-0.1, DV.fieldLength-1);

        NumberAxis xAxis = (NumberAxis) plotUpper.getDomainAxis();

        NumberTickUnit ntu = new NumberTickUnit(1)
        {
            @Override
            public String valueToString(double value) {
                return super.valueToString(value + 1);
            }
        };

        xAxis.setTickUnit(ntu);

        // set range
        NumberAxis yAxis = (NumberAxis) plotUpper.getRangeAxis();
        yAxis.setTickUnit(new NumberTickUnit(0.25));
        yAxis.setAutoRange(false);
        yAxis.setRange(0, 1);

        // set block renderer and dataset
        pcBlockRendererUpper.setBaseStroke(new BasicStroke(3f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        pcBlockRendererUpper.setAutoPopulateSeriesStroke(false);
        plotUpper.setRenderer(0, pcBlockRendererUpper);
        plotUpper.setDataset(0, pcBlocksUpper);

        badLineRenderer.setBaseStroke(new BasicStroke(3f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        badLineRenderer.setAutoPopulateSeriesStroke(false);
        plotUpper.setRenderer(1, badLineRenderer);
        plotUpper.setDataset(1, badGraphLines);

        goodLineRenderer.setBaseStroke(new BasicStroke(3f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        goodLineRenderer.setAutoPopulateSeriesStroke(false);
        plotUpper.setRenderer(2, goodLineRenderer);
        plotUpper.setDataset(2, goodGraphLines);

        System.out.println("Total Points: " + (good + bad));
        System.out.println("Benign Points: " + good);
        System.out.println("Malignant Points: " + bad);

        ChartPanel chartPanelUpper = new ChartPanel(pcChartUpper);
        chartPanelUpper.setMouseWheelEnabled(true);

        return chartPanelUpper;
    }

    /*private JPanel drawPCBlockTilesCombinedClasses(ArrayList<ArrayList<DataObject>> obj)
    {
        // hyperblock renderer and dataset
        XYLineAndShapeRenderer pcBlockRendererUpper = new XYLineAndShapeRenderer(true, false);
        XYSeriesCollection pcBlocksUpper = new XYSeriesCollection();
        XYAreaRenderer pcBlockAreaRendererUpper = new XYAreaRenderer(XYAreaRenderer.AREA);
        XYSeriesCollection pcBlocksAreaUpper = new XYSeriesCollection();

        XYLineAndShapeRenderer pcBlockRendererLower = new XYLineAndShapeRenderer(true, false);
        XYSeriesCollection pcBlocksLower = new XYSeriesCollection();
        XYAreaRenderer pcBlockAreaRendererLower = new XYAreaRenderer(XYAreaRenderer.AREA);
        XYSeriesCollection pcBlocksAreaLower = new XYSeriesCollection();

        XYLineAndShapeRenderer pcIndBlockRendererUpper = new XYLineAndShapeRenderer(true, false);
        XYSeriesCollection pcIndBlocksUpper = new XYSeriesCollection();
        XYAreaRenderer pcIndBlockAreaRendererUpper = new XYAreaRenderer(XYAreaRenderer.AREA);
        XYSeriesCollection pcIndBlocksAreaUpper = new XYSeriesCollection();

        XYLineAndShapeRenderer pcIndBlockRendererLower = new XYLineAndShapeRenderer(true, false);
        XYSeriesCollection pcIndBlocksLower = new XYSeriesCollection();
        XYAreaRenderer pcIndBlockAreaRendererLower = new XYAreaRenderer(XYAreaRenderer.AREA);
        XYSeriesCollection pcIndBlocksAreaLower = new XYSeriesCollection();

        BasicStroke[] strokes = new BasicStroke[hyper_blocks.size()];

        double[] largest = new double[]{-1, -1};
        for (int i = 0; i < hyper_blocks.size(); i++)
            for (int j = 0; j < hyper_blocks.get(i).hyper_block.size(); j++)
                if (hyper_blocks.get(i).hyper_block.get(j).size() > largest[hyper_blocks.get(i).classNum]) largest[hyper_blocks.get(i).classNum] = hyper_blocks.get(i).hyper_block.get(j).size();

        // add hyperblocks
        for (int c = 0, uKey = 0, lKey = 0, uIKey = 0, lIKey = 0; c < hyper_blocks.size(); c++)
        {
            if (c == 0)
            {
                strokes[c] = new BasicStroke(3f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);
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

                strokes[c] = new BasicStroke(3f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1f, fa, 0f);
            }

            for (int k = 0; k < hyper_blocks.get(c).hyper_block.size(); k++)
            {
                int key;

                if (hyper_blocks.get(c).hyper_block.get(k).size() > 1 || !indSep)
                    key = hyper_blocks.get(c).classNum == DV.upperClass ? uKey : lKey;
                else
                    key = hyper_blocks.get(c).classNum == DV.upperClass ? uIKey : lIKey;

                XYSeries tmp1 = new XYSeries(key, false, true);
                XYSeries tmp2 = new XYSeries(key, false, true);

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

                int emph = (int)((hyper_blocks.get(c).hyper_block.get(k).size() / Math.max(largest[0], largest[1])) * 255);
                Color blockEmph = new Color(255, 200, 0, emph);

                if (hyper_blocks.get(c).hyper_block.get(k).size() > 1 || !indSep)
                {
                    if (hyper_blocks.get(c).classNum == DV.upperClass)
                    {
                        pcBlockRendererUpper.setSeriesPaint(key, DV.graphColors[hyper_blocks.get(c).classNum]);
                        pcBlockAreaRendererUpper.setSeriesPaint(key, blockEmph);
                        pcBlockRendererUpper.setSeriesStroke(key, strokes[c]);

                        pcBlocksUpper.addSeries(tmp1);
                        pcBlocksAreaUpper.addSeries(tmp2);

                        uKey++;
                    }
                    else
                    {
                        pcBlockRendererLower.setSeriesPaint(key, DV.graphColors[hyper_blocks.get(c).classNum]);
                        pcBlockAreaRendererLower.setSeriesPaint(key, blockEmph);
                        pcBlockRendererLower.setSeriesStroke(key, strokes[c]);

                        pcBlocksLower.addSeries(tmp1);
                        pcBlocksAreaLower.addSeries(tmp2);

                        lKey++;
                    }
                }
                else
                {
                    if (hyper_blocks.get(c).classNum == DV.upperClass)
                    {
                        pcIndBlockRendererUpper.setSeriesPaint(key, DV.graphColors[hyper_blocks.get(c).classNum]);
                        pcIndBlockAreaRendererUpper.setSeriesPaint(key, blockEmph);
                        pcIndBlockRendererUpper.setSeriesStroke(key, strokes[c]);

                        pcIndBlocksUpper.addSeries(tmp1);
                        pcIndBlocksAreaUpper.addSeries(tmp2);

                        uIKey++;
                    }
                    else
                    {
                        pcIndBlockRendererLower.setSeriesPaint(key, DV.graphColors[hyper_blocks.get(c).classNum]);
                        pcIndBlockAreaRendererLower.setSeriesPaint(key, blockEmph);
                        pcIndBlockRendererLower.setSeriesStroke(key, strokes[c]);

                        pcIndBlocksLower.addSeries(tmp1);
                        pcIndBlocksAreaLower.addSeries(tmp2);

                        lIKey++;
                    }
                }
            }
        }

        JFreeChart pcChartUpper = ChartFactory.createXYLineChart(
                "",
                "",
                "",
                pcBlocksUpper,
                PlotOrientation.VERTICAL,
                false,
                true,
                false);

        // format chart
        pcChartUpper.setBorderVisible(false);
        pcChartUpper.setPadding(RectangleInsets.ZERO_INSETS);

        // get plot
        XYPlot plotUpper = (XYPlot) pcChartUpper.getPlot();

        // format plot
        plotUpper.setDrawingSupplier(new DefaultDrawingSupplier(
                new Paint[] { DV.graphColors[0] },
                DefaultDrawingSupplier.DEFAULT_OUTLINE_PAINT_SEQUENCE,
                DefaultDrawingSupplier.DEFAULT_STROKE_SEQUENCE,
                DefaultDrawingSupplier.DEFAULT_OUTLINE_STROKE_SEQUENCE,
                DefaultDrawingSupplier.DEFAULT_SHAPE_SEQUENCE));
        plotUpper.getRangeAxis().setVisible(true);
        plotUpper.getDomainAxis().setVisible(true);
        plotUpper.setOutlinePaint(null);
        plotUpper.setOutlineVisible(false);
        plotUpper.setInsets(RectangleInsets.ZERO_INSETS);
        plotUpper.setDomainPannable(true);
        plotUpper.setRangePannable(true);
        plotUpper.setBackgroundPaint(DV.background);
        plotUpper.setDomainGridlinePaint(Color.GRAY);

        // set domain
        ValueAxis domainView = plotUpper.getDomainAxis();
        domainView.setRange(-0.1, DV.fieldLength-1);

        NumberAxis xAxis = (NumberAxis) plotUpper.getDomainAxis();

        NumberTickUnit ntu = new NumberTickUnit(1)
        {
            @Override
            public String valueToString(double value) {
                return super.valueToString(value + 1);
            }
        };

        xAxis.setTickUnit(ntu);

        // set range
        NumberAxis yAxis = (NumberAxis) plotUpper.getRangeAxis();
        yAxis.setTickUnit(new NumberTickUnit(0.25));
        yAxis.setAutoRange(false);
        yAxis.setRange(0, 1);

        // set block renderer and dataset
        pcBlockRendererUpper.setBaseStroke(new BasicStroke(3f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        pcBlockRendererUpper.setAutoPopulateSeriesStroke(false);
        plotUpper.setRenderer(0, pcBlockRendererUpper);
        plotUpper.setDataset(0, pcBlocksUpper);

        pcBlockAreaRendererUpper.setAutoPopulateSeriesStroke(false);
        plotUpper.setRenderer(1, pcBlockAreaRendererUpper);
        plotUpper.setDataset(1, pcBlocksAreaUpper);

        ChartPanel chartPanelUpper = new ChartPanel(pcChartUpper);
        chartPanelUpper.setMouseWheelEnabled(true);

        JFreeChart pcChartLower = ChartFactory.createXYLineChart(
                "",
                "",
                "",
                pcBlocksLower,
                PlotOrientation.VERTICAL,
                false,
                true,
                false);

        // format chart
        pcChartLower.setBorderVisible(false);
        pcChartLower.setPadding(RectangleInsets.ZERO_INSETS);

        // get plot
        XYPlot plotLower = (XYPlot) pcChartLower.getPlot();

        // format plot
        plotLower.setDrawingSupplier(new DefaultDrawingSupplier(
                new Paint[] { DV.graphColors[0] },
                DefaultDrawingSupplier.DEFAULT_OUTLINE_PAINT_SEQUENCE,
                DefaultDrawingSupplier.DEFAULT_STROKE_SEQUENCE,
                DefaultDrawingSupplier.DEFAULT_OUTLINE_STROKE_SEQUENCE,
                DefaultDrawingSupplier.DEFAULT_SHAPE_SEQUENCE));
        plotLower.getRangeAxis().setVisible(true);
        plotLower.getDomainAxis().setVisible(true);
        plotLower.setOutlinePaint(null);
        plotLower.setOutlineVisible(false);
        plotLower.setInsets(RectangleInsets.ZERO_INSETS);
        plotLower.setDomainPannable(true);
        plotLower.setRangePannable(true);
        plotLower.setBackgroundPaint(DV.background);
        plotLower.setDomainGridlinePaint(Color.GRAY);

        // set domain
        ValueAxis domainViewLower = plotLower.getDomainAxis();
        domainViewLower.setRange(-0.1, DV.fieldLength-1);

        NumberAxis xAxisLower = (NumberAxis) plotLower.getDomainAxis();

        NumberTickUnit ntuLower = new NumberTickUnit(1)
        {
            @Override
            public String valueToString(double value) {
                return super.valueToString(value + 1);
            }
        };

        xAxisLower.setTickUnit(ntuLower);

        // set range
        NumberAxis yAxisLower = (NumberAxis) plotLower.getRangeAxis();
        yAxisLower.setTickUnit(new NumberTickUnit(0.25));
        yAxisLower.setAutoRange(false);
        yAxisLower.setRange(0, 1);

        // set block renderer and dataset
        pcBlockRendererLower.setBaseStroke(new BasicStroke(3f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        pcBlockRendererLower.setAutoPopulateSeriesStroke(false);
        plotLower.setRenderer(0, pcBlockRendererLower);
        plotLower.setDataset(0, pcBlocksLower);

        pcBlockAreaRendererLower.setAutoPopulateSeriesStroke(false);
        plotLower.setRenderer(1, pcBlockAreaRendererLower);
        plotLower.setDataset(1, pcBlocksAreaLower);

        ChartPanel chartPanelLower = new ChartPanel(pcChartLower);
        chartPanelLower.setMouseWheelEnabled(true);

        JFreeChart pcChartIndUpper = ChartFactory.createXYLineChart(
                "",
                "",
                "",
                pcBlocksUpper,
                PlotOrientation.VERTICAL,
                false,
                true,
                false);

        // format chart
        pcChartIndUpper.setBorderVisible(false);
        pcChartIndUpper.setPadding(RectangleInsets.ZERO_INSETS);

        // get plot
        XYPlot plotIndUpper = (XYPlot) pcChartIndUpper.getPlot();

        // format plot
        plotIndUpper.setDrawingSupplier(new DefaultDrawingSupplier(
                new Paint[] { DV.graphColors[0] },
                DefaultDrawingSupplier.DEFAULT_OUTLINE_PAINT_SEQUENCE,
                DefaultDrawingSupplier.DEFAULT_STROKE_SEQUENCE,
                DefaultDrawingSupplier.DEFAULT_OUTLINE_STROKE_SEQUENCE,
                DefaultDrawingSupplier.DEFAULT_SHAPE_SEQUENCE));
        plotIndUpper.getRangeAxis().setVisible(true);
        plotIndUpper.getDomainAxis().setVisible(true);
        plotIndUpper.setOutlinePaint(null);
        plotIndUpper.setOutlineVisible(false);
        plotIndUpper.setInsets(RectangleInsets.ZERO_INSETS);
        plotIndUpper.setDomainPannable(true);
        plotIndUpper.setRangePannable(true);
        plotIndUpper.setBackgroundPaint(DV.background);
        plotIndUpper.setDomainGridlinePaint(Color.GRAY);

        // set domain
        ValueAxis domainViewInd = plotIndUpper.getDomainAxis();
        domainViewInd.setRange(-0.1, DV.fieldLength-1);

        NumberAxis xAxisInd = (NumberAxis) plotIndUpper.getDomainAxis();

        NumberTickUnit ntuInd = new NumberTickUnit(1)
        {
            @Override
            public String valueToString(double value) {
                return super.valueToString(value + 1);
            }
        };

        xAxisInd.setTickUnit(ntuInd);

        // set range
        NumberAxis yAxisInd = (NumberAxis) plotIndUpper.getRangeAxis();
        yAxisInd.setTickUnit(new NumberTickUnit(0.25));
        yAxisInd.setAutoRange(false);
        yAxisInd.setRange(0, 1);

        // set block renderer and dataset
        pcIndBlockRendererUpper.setBaseStroke(new BasicStroke(3f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        pcIndBlockRendererUpper.setAutoPopulateSeriesStroke(false);
        plotIndUpper.setRenderer(0, pcIndBlockRendererUpper);
        plotIndUpper.setDataset(0, pcIndBlocksUpper);

        pcIndBlockAreaRendererUpper.setAutoPopulateSeriesStroke(false);
        plotIndUpper.setRenderer(1, pcIndBlockAreaRendererUpper);
        plotIndUpper.setDataset(1, pcIndBlocksAreaUpper);

        ChartPanel chartPanelIndUpper = new ChartPanel(pcChartIndUpper);
        chartPanelUpper.setMouseWheelEnabled(true);

        JFreeChart pcChartIndLower = ChartFactory.createXYLineChart(
                "",
                "",
                "",
                pcBlocksLower,
                PlotOrientation.VERTICAL,
                false,
                true,
                false);

        // format chart
        pcChartIndLower.setBorderVisible(false);
        pcChartIndLower.setPadding(RectangleInsets.ZERO_INSETS);

        // get plot
        XYPlot plotIndLower = (XYPlot) pcChartIndLower.getPlot();

        // format plot
        plotIndLower.setDrawingSupplier(new DefaultDrawingSupplier(
                new Paint[] { DV.graphColors[0] },
                DefaultDrawingSupplier.DEFAULT_OUTLINE_PAINT_SEQUENCE,
                DefaultDrawingSupplier.DEFAULT_STROKE_SEQUENCE,
                DefaultDrawingSupplier.DEFAULT_OUTLINE_STROKE_SEQUENCE,
                DefaultDrawingSupplier.DEFAULT_SHAPE_SEQUENCE));
        plotIndLower.getRangeAxis().setVisible(true);
        plotIndLower.getDomainAxis().setVisible(true);
        plotIndLower.setOutlinePaint(null);
        plotIndLower.setOutlineVisible(false);
        plotIndLower.setInsets(RectangleInsets.ZERO_INSETS);
        plotIndLower.setDomainPannable(true);
        plotIndLower.setRangePannable(true);
        plotIndLower.setBackgroundPaint(DV.background);
        plotIndLower.setDomainGridlinePaint(Color.GRAY);

        // set domain
        ValueAxis domainViewIndLower = plotIndLower.getDomainAxis();
        domainViewIndLower.setRange(-0.1, DV.fieldLength-1);

        NumberAxis xAxisIndLower = (NumberAxis) plotIndLower.getDomainAxis();

        NumberTickUnit ntuIndLower = new NumberTickUnit(1)
        {
            @Override
            public String valueToString(double value) {
                return super.valueToString(value + 1);
            }
        };

        xAxisIndLower.setTickUnit(ntuIndLower);

        // set range
        NumberAxis yAxisIndLower = (NumberAxis) plotIndLower.getRangeAxis();
        yAxisIndLower.setTickUnit(new NumberTickUnit(0.25));
        yAxisIndLower.setAutoRange(false);
        yAxisIndLower.setRange(0, 1);

        // set block renderer and dataset
        pcIndBlockRendererLower.setBaseStroke(new BasicStroke(3f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        pcIndBlockRendererLower.setAutoPopulateSeriesStroke(false);
        plotIndLower.setRenderer(0, pcIndBlockRendererLower);
        plotIndLower.setDataset(0, pcIndBlocksLower);

        pcIndBlockAreaRendererLower.setAutoPopulateSeriesStroke(false);
        plotIndLower.setRenderer(1, pcIndBlockAreaRendererLower);
        plotIndLower.setDataset(1, pcIndBlocksAreaLower);

        ChartPanel chartPanelIndLower = new ChartPanel(pcChartIndLower);
        chartPanelIndLower.setMouseWheelEnabled(true);

        JPanel charts = new JPanel();
        charts.setLayout(new GridLayout(2, 2));

        charts.add(chartPanelUpper);
        charts.add(chartPanelLower);
        if (indSep)
        {
            charts.add(chartPanelIndUpper);
            charts.add(chartPanelIndLower);
        }

        return charts;
    }*/

    private JPanel drawPCBlockTilesCombined(ArrayList<ArrayList<DataObject>> obj)
    {
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

        XYLineAndShapeRenderer pcIndBlockRenderer = new XYLineAndShapeRenderer(true, false);
        XYSeriesCollection pcIndBlocks = new XYSeriesCollection();
        XYAreaRenderer pcIndBlockAreaRenderer = new XYAreaRenderer(XYAreaRenderer.AREA);
        XYSeriesCollection pcIndBlocksArea = new XYSeriesCollection();

        // refuse to classify area
        XYLineAndShapeRenderer refuseRenderer = new XYLineAndShapeRenderer(true, false);
        XYSeriesCollection refuse = new XYSeriesCollection();
        XYAreaRenderer refuseAreaRenderer = new XYAreaRenderer(XYAreaRenderer.AREA);
        XYSeriesCollection refuseArea = new XYSeriesCollection();

        BasicStroke[] strokes = new BasicStroke[hyper_blocks.size()];

        // add hyperblocks
        for (int c = 0, ind = 0, all = 0; c < hyper_blocks.size(); c++)
        {
            if (c == 0)
            {
                strokes[c] = new BasicStroke(3f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);
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

                strokes[c] = new BasicStroke(3f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1f, fa, 0f);
            }

            for (int k = 0; k < hyper_blocks.get(c).hyper_block.size(); k++)
            {
                    int key = hyper_blocks.get(c).hyper_block.get(k).size() > 1 || !indSep ? all : ind;

                    XYSeries tmp1 = new XYSeries(key, false, true);
                    XYSeries tmp2 = new XYSeries(key, false, true);

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

                if (hyper_blocks.get(c).hyper_block.get(k).size() > 1 || !indSep)
                {
                    pcBlockRenderer.setSeriesPaint(key, DV.graphColors[hyper_blocks.get(c).classNum]);
                    pcBlockAreaRenderer.setSeriesPaint(key, new Color(255, 200, 0, 20));
                    pcBlockRenderer.setSeriesStroke(key, strokes[key]);

                    pcBlocks.addSeries(tmp1);
                    pcBlocksArea.addSeries(tmp2);

                    all++;
                }
                else
                {
                    pcIndBlockRenderer.setSeriesPaint(key, DV.graphColors[hyper_blocks.get(c).classNum]);
                    pcIndBlockAreaRenderer.setSeriesPaint(key, new Color(255, 200, 0, 20));
                    pcIndBlockRenderer.setSeriesStroke(key, strokes[key]);

                    pcIndBlocks.addSeries(tmp1);
                    pcIndBlocksArea.addSeries(tmp2);

                    ind++;
                }
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

        JFreeChart pcChartInd = ChartFactory.createXYLineChart(
                "",
                "",
                "",
                pcBlocks,
                PlotOrientation.VERTICAL,
                false,
                true,
                false);

        // format chart
        pcChartInd.setBorderVisible(false);
        pcChartInd.setPadding(RectangleInsets.ZERO_INSETS);

        // get plot
        XYPlot plotInd = (XYPlot) pcChartInd.getPlot();

        // format plot
        plotInd.setDrawingSupplier(new DefaultDrawingSupplier(
                new Paint[] { DV.graphColors[0] },
                DefaultDrawingSupplier.DEFAULT_OUTLINE_PAINT_SEQUENCE,
                DefaultDrawingSupplier.DEFAULT_STROKE_SEQUENCE,
                DefaultDrawingSupplier.DEFAULT_OUTLINE_STROKE_SEQUENCE,
                DefaultDrawingSupplier.DEFAULT_SHAPE_SEQUENCE));
        plotInd.getRangeAxis().setVisible(true);
        plotInd.getDomainAxis().setVisible(true);
        plotInd.setOutlinePaint(null);
        plotInd.setOutlineVisible(false);
        plotInd.setInsets(RectangleInsets.ZERO_INSETS);
        plotInd.setDomainPannable(true);
        plotInd.setRangePannable(true);
        plotInd.setBackgroundPaint(DV.background);
        plotInd.setDomainGridlinePaint(Color.GRAY);

        // set domain
        ValueAxis domainViewInd = plotInd.getDomainAxis();
        domainViewInd.setRange(-0.1, DV.fieldLength-1);

        NumberAxis xAxisInd = (NumberAxis) plotInd.getDomainAxis();

        NumberTickUnit ntuInd = new NumberTickUnit(1)
        {
            @Override
            public String valueToString(double value) {
                return super.valueToString(value + 1);
            }
        };

        xAxisInd.setTickUnit(ntuInd);

        // set range
        NumberAxis yAxisInd = (NumberAxis) plotInd.getRangeAxis();
        yAxisInd.setTickUnit(new NumberTickUnit(0.25));
        yAxisInd.setAutoRange(false);
        yAxisInd.setRange(0, 1);

        // set block renderer and dataset
        pcIndBlockRenderer.setBaseStroke(new BasicStroke(3f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        pcIndBlockRenderer.setAutoPopulateSeriesStroke(false);
        plotInd.setRenderer(0, pcIndBlockRenderer);
        plotInd.setDataset(0, pcIndBlocks);

        pcIndBlockAreaRenderer.setAutoPopulateSeriesStroke(false);
        plotInd.setRenderer(1, pcIndBlockAreaRenderer);
        plotInd.setDataset(1, pcIndBlocksArea);

        ChartPanel chartPanelInd = new ChartPanel(pcChartInd);
        chartPanelInd.setMouseWheelEnabled(true);

        if (!indSep)
            return chartPanel;
        else
        {
            JPanel charts = new JPanel();
            charts.setLayout(new GridLayout(2, 1));
            charts.add(chartPanel);
            charts.add(chartPanelInd);

            return charts;
        }
    }

    private JPanel drawGLCBlockTilesCombinedClasses(ArrayList<ArrayList<DataObject>> obj)
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

        // hyperblock renderer and dataset
        XYLineAndShapeRenderer pcBlockRendererUpper = new XYLineAndShapeRenderer(true, false);
        XYSeriesCollection pcBlocksUpper = new XYSeriesCollection();
        XYAreaRenderer pcBlockAreaRendererUpper = new XYAreaRenderer(XYAreaRenderer.AREA);
        XYSeriesCollection pcBlocksAreaUpper = new XYSeriesCollection();

        XYLineAndShapeRenderer pcBlockRendererLower = new XYLineAndShapeRenderer(true, false);
        XYSeriesCollection pcBlocksLower = new XYSeriesCollection();
        XYAreaRenderer pcBlockAreaRendererLower = new XYAreaRenderer(XYAreaRenderer.AREA);
        XYSeriesCollection pcBlocksAreaLower = new XYSeriesCollection();

        XYLineAndShapeRenderer pcIndBlockRendererUpper = new XYLineAndShapeRenderer(true, false);
        XYSeriesCollection pcIndBlocksUpper = new XYSeriesCollection();
        XYAreaRenderer pcIndBlockAreaRendererUpper = new XYAreaRenderer(XYAreaRenderer.AREA);
        XYSeriesCollection pcIndBlocksAreaUpper = new XYSeriesCollection();

        XYLineAndShapeRenderer pcIndBlockRendererLower = new XYLineAndShapeRenderer(true, false);
        XYSeriesCollection pcIndBlocksLower = new XYSeriesCollection();
        XYAreaRenderer pcIndBlockAreaRendererLower = new XYAreaRenderer(XYAreaRenderer.AREA);
        XYSeriesCollection pcIndBlocksAreaLower = new XYSeriesCollection();

        BasicStroke[] strokes = new BasicStroke[hyper_blocks.size()];

        double[] largest = new double[]{-1, -1};
        for (int i = 0; i < hyper_blocks.size(); i++)
            for (int j = 0; j < hyper_blocks.get(i).hyper_block.size(); j++)
                if (hyper_blocks.get(i).hyper_block.get(j).size() > largest[hyper_blocks.get(i).classNum]) largest[hyper_blocks.get(i).classNum] = hyper_blocks.get(i).hyper_block.get(j).size();

        // add hyperblocks
        for (int c = 0, uKey = 0, lKey = 0, uIKey = 0, lIKey = 0; c < hyper_blocks.size(); c++)
        {
            if (c == 0)
            {
                strokes[c] = new BasicStroke(3f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);
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

                strokes[c] = new BasicStroke(3f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1f, fa, 0f);
            }

            for (int k = 0; k < hyper_blocks.get(c).hyper_block.size(); k++)
            {
                int key;

                if (hyper_blocks.get(c).hyper_block.get(k).size() > 1 || !indSep)
                    key = hyper_blocks.get(c).classNum == DV.upperClass ? uKey : lKey;
                else
                    key = hyper_blocks.get(c).classNum == DV.upperClass ? uIKey : lIKey;

                XYSeries tmp1 = new XYSeries(key, false, true);
                XYSeries tmp2 = new XYSeries(key+1, false, true);

                //XYSeries area = new XYSeries(k, false, true);

                double[] xyCurPointMin = new double[0];// = DataObject.getXYPointGLC(hyper_blocks.get(c).minimums.get(k)[indexes[0]], DV.angles[indexes[0]]);
                double[] xyCurPointMax = new double[0];// = DataObject.getXYPointGLC(hyper_blocks.get(c).maximums.get(k)[indexes[0]], DV.angles[indexes[0]]);

                xyCurPointMin = DataObject.getXYPointGLC(hyper_blocks.get(c).minimums.get(k)[indexes[0]], DV.angles[indexes[0]]);
                xyCurPointMax = DataObject.getXYPointGLC(hyper_blocks.get(c).maximums.get(k)[indexes[0]], DV.angles[indexes[0]]);

                xyCurPointMin[1] += glcBuffer;
                xyCurPointMax[1] += glcBuffer;

                if (DV.showFirstSeg) {
                    tmp1.add(0, glcBuffer);
                    tmp2.add(0, glcBuffer);
                    //area.add(0, glcBuffer);
                }

                tmp1.add(xyCurPointMin[0], xyCurPointMin[1]);
                tmp2.add(xyCurPointMax[0], xyCurPointMax[1]);
                //area.add(xyOriginPointMin[0], xyOriginPointMin[1]);

                for (int j = 1; j < DV.fieldLength; j++)
                {
                    double[] xyPoint = new double[0];//DataObject.getXYPointGLC(hyper_blocks.get(c).minimums.get(k)[indexes[j]], DV.angles[indexes[j]]);

                    xyPoint = DataObject.getXYPointGLC(hyper_blocks.get(c).minimums.get(k)[indexes[j]], DV.angles[indexes[j]]);

                    xyCurPointMin[0] = xyCurPointMin[0] + xyPoint[0];
                    xyCurPointMin[1] = xyCurPointMin[1] + xyPoint[1];

                    tmp1.add(xyCurPointMin[0], xyCurPointMin[1]);
                    //area.add(xyCurPointMin[0], xyCurPointMin[1]);

                    // get maximums
                    //xyPoint = DataObject.getXYPointGLC(hyper_blocks.get(c).maximums.get(k)[indexes[j]], DV.angles[indexes[j]]);
                    xyPoint = DataObject.getXYPointGLC(hyper_blocks.get(c).maximums.get(k)[indexes[j]], DV.angles[indexes[j]]);

                    xyCurPointMax[0] = xyCurPointMax[0] + xyPoint[0];
                    xyCurPointMax[1] = xyCurPointMax[1] + xyPoint[1];

                    tmp2.add(xyCurPointMax[0], xyCurPointMax[1]);
                }

                //max_bound.add(xyCurPointMin[0], xyCurPointMin[1]);
                //area.add(xyCurPointMax[0], xyCurPointMax[1]);

                int emph = (int)((hyper_blocks.get(c).hyper_block.get(k).size() / Math.max(largest[0], largest[1])) * 255);
                Color blockEmph = new Color(255, 200, 0, emph);

                if (hyper_blocks.get(c).hyper_block.get(k).size() > 1 || !indSep)
                {
                    if (hyper_blocks.get(c).classNum == DV.upperClass)
                    {
                        pcBlockRendererUpper.setSeriesPaint(key, DV.graphColors[hyper_blocks.get(c).classNum]);
                        pcBlockAreaRendererUpper.setSeriesPaint(key, blockEmph);
                        pcBlockRendererUpper.setSeriesStroke(key, strokes[c]);

                        pcBlocksUpper.addSeries(tmp1);
                        //pcBlocksAreaUpper.addSeries(tmp2);

                        uKey += 2;
                    }
                    else
                    {
                        pcBlockRendererLower.setSeriesPaint(key, DV.graphColors[hyper_blocks.get(c).classNum]);
                        pcBlockAreaRendererLower.setSeriesPaint(key, blockEmph);
                        pcBlockRendererLower.setSeriesStroke(key, strokes[c]);

                        pcBlocksLower.addSeries(tmp1);
                        //pcBlocksAreaLower.addSeries(tmp2);

                        lKey += 2;
                    }
                }
                else
                {
                    if (hyper_blocks.get(c).classNum == DV.upperClass)
                    {
                        pcIndBlockRendererUpper.setSeriesPaint(key, DV.graphColors[hyper_blocks.get(c).classNum]);
                        pcIndBlockAreaRendererUpper.setSeriesPaint(key, blockEmph);
                        pcIndBlockRendererUpper.setSeriesStroke(key, strokes[c]);

                        pcIndBlocksUpper.addSeries(tmp1);
                        //pcIndBlocksAreaUpper.addSeries(tmp2);

                        uIKey++;
                    }
                    else
                    {
                        pcIndBlockRendererLower.setSeriesPaint(key, DV.graphColors[hyper_blocks.get(c).classNum]);
                        pcIndBlockAreaRendererLower.setSeriesPaint(key, blockEmph);
                        pcIndBlockRendererLower.setSeriesStroke(key, strokes[c]);

                        pcIndBlocksLower.addSeries(tmp1);
                        //pcIndBlocksAreaLower.addSeries(tmp2);

                        lIKey++;
                    }
                }
            }
        }

        JFreeChart pcChartUpper = ChartFactory.createXYLineChart(
                "",
                "",
                "",
                pcBlocksUpper,
                PlotOrientation.VERTICAL,
                false,
                true,
                false);

        // format chart
        pcChartUpper.setBorderVisible(false);
        pcChartUpper.setPadding(RectangleInsets.ZERO_INSETS);

        // get plot
        XYPlot plotUpper = (XYPlot) pcChartUpper.getPlot();

        // format plot
        plotUpper.setDrawingSupplier(new DefaultDrawingSupplier(
                new Paint[] { DV.graphColors[0] },
                DefaultDrawingSupplier.DEFAULT_OUTLINE_PAINT_SEQUENCE,
                DefaultDrawingSupplier.DEFAULT_STROKE_SEQUENCE,
                DefaultDrawingSupplier.DEFAULT_OUTLINE_STROKE_SEQUENCE,
                DefaultDrawingSupplier.DEFAULT_SHAPE_SEQUENCE));
        plotUpper.getRangeAxis().setVisible(true);
        plotUpper.getDomainAxis().setVisible(true);
        plotUpper.setOutlinePaint(null);
        plotUpper.setOutlineVisible(false);
        plotUpper.setInsets(RectangleInsets.ZERO_INSETS);
        plotUpper.setDomainPannable(true);
        plotUpper.setRangePannable(true);
        plotUpper.setBackgroundPaint(DV.background);
        plotUpper.setDomainGridlinePaint(Color.GRAY);

        // set domain
        ValueAxis domainView = plotUpper.getDomainAxis();
        domainView.setRange(-0.1, DV.fieldLength-1);

        NumberAxis xAxis = (NumberAxis) plotUpper.getDomainAxis();

        NumberTickUnit ntu = new NumberTickUnit(1)
        {
            @Override
            public String valueToString(double value) {
                return super.valueToString(value + 1);
            }
        };

        xAxis.setTickUnit(ntu);

        // set range
        NumberAxis yAxis = (NumberAxis) plotUpper.getRangeAxis();
        yAxis.setTickUnit(new NumberTickUnit(0.25));
        yAxis.setAutoRange(false);
        yAxis.setRange(0, 1);

        // set block renderer and dataset
        pcBlockRendererUpper.setBaseStroke(new BasicStroke(3f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        pcBlockRendererUpper.setAutoPopulateSeriesStroke(false);
        plotUpper.setRenderer(0, pcBlockRendererUpper);
        plotUpper.setDataset(0, pcBlocksUpper);

        pcBlockAreaRendererUpper.setAutoPopulateSeriesStroke(false);
        plotUpper.setRenderer(1, pcBlockAreaRendererUpper);
        plotUpper.setDataset(1, pcBlocksAreaUpper);

        ChartPanel chartPanelUpper = new ChartPanel(pcChartUpper);
        chartPanelUpper.setMouseWheelEnabled(true);

        JFreeChart pcChartLower = ChartFactory.createXYLineChart(
                "",
                "",
                "",
                pcBlocksLower,
                PlotOrientation.VERTICAL,
                false,
                true,
                false);

        // format chart
        pcChartLower.setBorderVisible(false);
        pcChartLower.setPadding(RectangleInsets.ZERO_INSETS);

        // get plot
        XYPlot plotLower = (XYPlot) pcChartLower.getPlot();

        // format plot
        plotLower.setDrawingSupplier(new DefaultDrawingSupplier(
                new Paint[] { DV.graphColors[0] },
                DefaultDrawingSupplier.DEFAULT_OUTLINE_PAINT_SEQUENCE,
                DefaultDrawingSupplier.DEFAULT_STROKE_SEQUENCE,
                DefaultDrawingSupplier.DEFAULT_OUTLINE_STROKE_SEQUENCE,
                DefaultDrawingSupplier.DEFAULT_SHAPE_SEQUENCE));
        plotLower.getRangeAxis().setVisible(true);
        plotLower.getDomainAxis().setVisible(true);
        plotLower.setOutlinePaint(null);
        plotLower.setOutlineVisible(false);
        plotLower.setInsets(RectangleInsets.ZERO_INSETS);
        plotLower.setDomainPannable(true);
        plotLower.setRangePannable(true);
        plotLower.setBackgroundPaint(DV.background);
        plotLower.setDomainGridlinePaint(Color.GRAY);

        // set domain
        ValueAxis domainViewLower = plotLower.getDomainAxis();
        domainViewLower.setRange(-0.1, DV.fieldLength-1);

        NumberAxis xAxisLower = (NumberAxis) plotLower.getDomainAxis();

        NumberTickUnit ntuLower = new NumberTickUnit(1)
        {
            @Override
            public String valueToString(double value) {
                return super.valueToString(value + 1);
            }
        };

        xAxisLower.setTickUnit(ntuLower);

        // set range
        NumberAxis yAxisLower = (NumberAxis) plotLower.getRangeAxis();
        yAxisLower.setTickUnit(new NumberTickUnit(0.25));
        yAxisLower.setAutoRange(false);
        yAxisLower.setRange(0, 1);

        // set block renderer and dataset
        pcBlockRendererLower.setBaseStroke(new BasicStroke(3f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        pcBlockRendererLower.setAutoPopulateSeriesStroke(false);
        plotLower.setRenderer(0, pcBlockRendererLower);
        plotLower.setDataset(0, pcBlocksLower);

        pcBlockAreaRendererLower.setAutoPopulateSeriesStroke(false);
        plotLower.setRenderer(1, pcBlockAreaRendererLower);
        plotLower.setDataset(1, pcBlocksAreaLower);

        ChartPanel chartPanelLower = new ChartPanel(pcChartLower);
        chartPanelLower.setMouseWheelEnabled(true);

        JFreeChart pcChartIndUpper = ChartFactory.createXYLineChart(
                "",
                "",
                "",
                pcBlocksUpper,
                PlotOrientation.VERTICAL,
                false,
                true,
                false);

        // format chart
        pcChartIndUpper.setBorderVisible(false);
        pcChartIndUpper.setPadding(RectangleInsets.ZERO_INSETS);

        // get plot
        XYPlot plotIndUpper = (XYPlot) pcChartIndUpper.getPlot();

        // format plot
        plotIndUpper.setDrawingSupplier(new DefaultDrawingSupplier(
                new Paint[] { DV.graphColors[0] },
                DefaultDrawingSupplier.DEFAULT_OUTLINE_PAINT_SEQUENCE,
                DefaultDrawingSupplier.DEFAULT_STROKE_SEQUENCE,
                DefaultDrawingSupplier.DEFAULT_OUTLINE_STROKE_SEQUENCE,
                DefaultDrawingSupplier.DEFAULT_SHAPE_SEQUENCE));
        plotIndUpper.getRangeAxis().setVisible(true);
        plotIndUpper.getDomainAxis().setVisible(true);
        plotIndUpper.setOutlinePaint(null);
        plotIndUpper.setOutlineVisible(false);
        plotIndUpper.setInsets(RectangleInsets.ZERO_INSETS);
        plotIndUpper.setDomainPannable(true);
        plotIndUpper.setRangePannable(true);
        plotIndUpper.setBackgroundPaint(DV.background);
        plotIndUpper.setDomainGridlinePaint(Color.GRAY);

        // set domain
        ValueAxis domainViewInd = plotIndUpper.getDomainAxis();
        domainViewInd.setRange(-0.1, DV.fieldLength-1);

        NumberAxis xAxisInd = (NumberAxis) plotIndUpper.getDomainAxis();

        NumberTickUnit ntuInd = new NumberTickUnit(1)
        {
            @Override
            public String valueToString(double value) {
                return super.valueToString(value + 1);
            }
        };

        xAxisInd.setTickUnit(ntuInd);

        // set range
        NumberAxis yAxisInd = (NumberAxis) plotIndUpper.getRangeAxis();
        yAxisInd.setTickUnit(new NumberTickUnit(0.25));
        yAxisInd.setAutoRange(false);
        yAxisInd.setRange(0, 1);

        // set block renderer and dataset
        pcIndBlockRendererUpper.setBaseStroke(new BasicStroke(3f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        pcIndBlockRendererUpper.setAutoPopulateSeriesStroke(false);
        plotIndUpper.setRenderer(0, pcIndBlockRendererUpper);
        plotIndUpper.setDataset(0, pcIndBlocksUpper);

        pcIndBlockAreaRendererUpper.setAutoPopulateSeriesStroke(false);
        plotIndUpper.setRenderer(1, pcIndBlockAreaRendererUpper);
        plotIndUpper.setDataset(1, pcIndBlocksAreaUpper);

        ChartPanel chartPanelIndUpper = new ChartPanel(pcChartIndUpper);
        chartPanelIndUpper.setMouseWheelEnabled(true);

        JFreeChart pcChartIndLower = ChartFactory.createXYLineChart(
                "",
                "",
                "",
                pcBlocksLower,
                PlotOrientation.VERTICAL,
                false,
                true,
                false);

        // format chart
        pcChartIndLower.setBorderVisible(false);
        pcChartIndLower.setPadding(RectangleInsets.ZERO_INSETS);

        // get plot
        XYPlot plotIndLower = (XYPlot) pcChartIndLower.getPlot();

        // format plot
        plotIndLower.setDrawingSupplier(new DefaultDrawingSupplier(
                new Paint[] { DV.graphColors[0] },
                DefaultDrawingSupplier.DEFAULT_OUTLINE_PAINT_SEQUENCE,
                DefaultDrawingSupplier.DEFAULT_STROKE_SEQUENCE,
                DefaultDrawingSupplier.DEFAULT_OUTLINE_STROKE_SEQUENCE,
                DefaultDrawingSupplier.DEFAULT_SHAPE_SEQUENCE));
        plotIndLower.getRangeAxis().setVisible(true);
        plotIndLower.getDomainAxis().setVisible(true);
        plotIndLower.setOutlinePaint(null);
        plotIndLower.setOutlineVisible(false);
        plotIndLower.setInsets(RectangleInsets.ZERO_INSETS);
        plotIndLower.setDomainPannable(true);
        plotIndLower.setRangePannable(true);
        plotIndLower.setBackgroundPaint(DV.background);
        plotIndLower.setDomainGridlinePaint(Color.GRAY);

        // set domain
        ValueAxis domainViewIndLower = plotIndLower.getDomainAxis();
        domainViewIndLower.setRange(-0.1, DV.fieldLength-1);

        NumberAxis xAxisIndLower = (NumberAxis) plotIndLower.getDomainAxis();

        NumberTickUnit ntuIndLower = new NumberTickUnit(1)
        {
            @Override
            public String valueToString(double value) {
                return super.valueToString(value + 1);
            }
        };

        xAxisIndLower.setTickUnit(ntuIndLower);

        // set range
        NumberAxis yAxisIndLower = (NumberAxis) plotIndLower.getRangeAxis();
        yAxisIndLower.setTickUnit(new NumberTickUnit(0.25));
        yAxisIndLower.setAutoRange(false);
        yAxisIndLower.setRange(0, 1);

        // set block renderer and dataset
        pcIndBlockRendererLower.setBaseStroke(new BasicStroke(3f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        pcIndBlockRendererLower.setAutoPopulateSeriesStroke(false);
        plotIndLower.setRenderer(0, pcIndBlockRendererLower);
        plotIndLower.setDataset(0, pcIndBlocksLower);

        pcIndBlockAreaRendererLower.setAutoPopulateSeriesStroke(false);
        plotIndLower.setRenderer(1, pcIndBlockAreaRendererLower);
        plotIndLower.setDataset(1, pcIndBlocksAreaLower);

        ChartPanel chartPanelIndLower = new ChartPanel(pcChartIndLower);
        chartPanelIndLower.setMouseWheelEnabled(true);

        JPanel charts = new JPanel();
        charts.setLayout(new GridLayout(2, 2));

        charts.add(chartPanelUpper);
        charts.add(chartPanelLower);
        if (indSep)
        {
            charts.add(chartPanelIndUpper);
            charts.add(chartPanelIndLower);
        }

        return charts;
    }

    private JPanel drawGLCBlockTilesCombined(ArrayList<ArrayList<DataObject>> obj)
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

        // create main renderer and dataset
        XYLineAndShapeRenderer goodLineRenderer = new XYLineAndShapeRenderer(true, false);
        XYLineAndShapeRenderer badLineRenderer = new XYLineAndShapeRenderer(true, false);
        XYSeriesCollection goodGraphLines = new XYSeriesCollection();
        XYSeriesCollection badGraphLines = new XYSeriesCollection();

        // artificial renderer and dataset
        XYLineAndShapeRenderer artRenderer = new XYLineAndShapeRenderer(true, false);
        XYSeriesCollection artLines = new XYSeriesCollection();

        // hyperblock renderer and dataset
        XYLineAndShapeRenderer glcBlockRenderer = new XYLineAndShapeRenderer(true, false);
        XYSeriesCollection glcBlocks = new XYSeriesCollection();
        XYAreaRenderer glcBlockAreaRenderer = new XYAreaRenderer(XYAreaRenderer.AREA);
        XYSeriesCollection glcBlocksArea = new XYSeriesCollection();

        XYLineAndShapeRenderer glcIndBlockRenderer = new XYLineAndShapeRenderer(true, false);
        XYSeriesCollection glcIndBlocks = new XYSeriesCollection();
        XYAreaRenderer glcIndBlockAreaRenderer = new XYAreaRenderer(XYAreaRenderer.AREA);
        XYSeriesCollection glcIndBlocksArea = new XYSeriesCollection();

        // refuse to classify area
        XYLineAndShapeRenderer refuseRenderer = new XYLineAndShapeRenderer(true, false);
        XYSeriesCollection refuse = new XYSeriesCollection();
        XYAreaRenderer refuseAreaRenderer = new XYAreaRenderer(XYAreaRenderer.AREA);
        XYSeriesCollection refuseArea = new XYSeriesCollection();

        BasicStroke[] strokes = new BasicStroke[hyper_blocks.size()];

        // add hyperblocks
        for (int c = 0, ind = 0, all = 0, offset = 0; c < hyper_blocks.size(); c++)
        {
            if (c == 0)
            {
                strokes[c] = new BasicStroke(3f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);
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

                strokes[c] = new BasicStroke(3f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1f, fa, 0f);
            }

            for (int k = 0; k < hyper_blocks.get(c).hyper_block.size(); k++)
            {
                int key = hyper_blocks.get(c).hyper_block.get(k).size() > 1 || !indSep ? all : ind;

                XYSeries tmp1 = new XYSeries(key, false, true);
                XYSeries tmp2 = new XYSeries(key+1, false, true);

                //XYSeries area = new XYSeries(k, false, true);

                double[] xyCurPointMin = new double[0];// = DataObject.getXYPointGLC(hyper_blocks.get(c).minimums.get(k)[indexes[0]], DV.angles[indexes[0]]);
                double[] xyCurPointMax = new double[0];// = DataObject.getXYPointGLC(hyper_blocks.get(c).maximums.get(k)[indexes[0]], DV.angles[indexes[0]]);

                xyCurPointMin = DataObject.getXYPointGLC(hyper_blocks.get(c).minimums.get(k)[indexes[0]], DV.angles[indexes[0]]);
                xyCurPointMax = DataObject.getXYPointGLC(hyper_blocks.get(c).maximums.get(k)[indexes[0]], DV.angles[indexes[0]]);

                xyCurPointMin[1] += glcBuffer;
                xyCurPointMax[1] += glcBuffer;

                if (DV.showFirstSeg) {
                    tmp1.add(0, glcBuffer);
                    tmp2.add(0, glcBuffer);
                    //area.add(0, glcBuffer);
                }

                tmp1.add(xyCurPointMin[0], xyCurPointMin[1]);
                tmp2.add(xyCurPointMax[0], xyCurPointMax[1]);
                //area.add(xyOriginPointMin[0], xyOriginPointMin[1]);

                for (int j = 1; j < DV.fieldLength; j++)
                {
                    double[] xyPoint = new double[0];//DataObject.getXYPointGLC(hyper_blocks.get(c).minimums.get(k)[indexes[j]], DV.angles[indexes[j]]);

                    xyPoint = DataObject.getXYPointGLC(hyper_blocks.get(c).minimums.get(k)[indexes[j]], DV.angles[indexes[j]]);

                    xyCurPointMin[0] = xyCurPointMin[0] + xyPoint[0];
                    xyCurPointMin[1] = xyCurPointMin[1] + xyPoint[1];

                    tmp1.add(xyCurPointMin[0], xyCurPointMin[1]);
                    //area.add(xyCurPointMin[0], xyCurPointMin[1]);

                    // get maximums
                    //xyPoint = DataObject.getXYPointGLC(hyper_blocks.get(c).maximums.get(k)[indexes[j]], DV.angles[indexes[j]]);
                    xyPoint = DataObject.getXYPointGLC(hyper_blocks.get(c).maximums.get(k)[indexes[j]], DV.angles[indexes[j]]);

                    xyCurPointMax[0] = xyCurPointMax[0] + xyPoint[0];
                    xyCurPointMax[1] = xyCurPointMax[1] + xyPoint[1];

                    tmp2.add(xyCurPointMax[0], xyCurPointMax[1]);
                }

                //max_bound.add(xyCurPointMin[0], xyCurPointMin[1]);
                //area.add(xyCurPointMax[0], xyCurPointMax[1]);

                if (hyper_blocks.get(c).hyper_block.get(k).size() > 1 || !indSep)
                {
                    glcBlockRenderer.setSeriesPaint(key, DV.graphColors[hyper_blocks.get(c).classNum]);
                    //glcBlockAreaRenderer.setSeriesPaint(key, new Color(255, 200, 0, 20));
                    glcBlockRenderer.setSeriesStroke(key, strokes[key - offset]);
                    glcBlockRenderer.setSeriesPaint(key+1, DV.graphColors[hyper_blocks.get(c).classNum]);
                    //glcBlockAreaRenderer.setSeriesPaint(key, new Color(255, 200, 0, 20));
                    glcBlockRenderer.setSeriesStroke(key+1, strokes[key - offset]);

                    glcBlocks.addSeries(tmp1);
                    glcBlocks.addSeries(tmp2);

                    //glcBlocksArea.addSeries(tmp2);

                    all += 2;
                    offset++;
                }
                else
                {
                    glcIndBlockRenderer.setSeriesPaint(key, DV.graphColors[hyper_blocks.get(c).classNum]);
                    //glcIndBlockAreaRenderer.setSeriesPaint(key, new Color(255, 200, 0, 20));
                    glcIndBlockRenderer.setSeriesStroke(key, strokes[key]);
                    glcIndBlocks.addSeries(tmp2);

                    //glcIndBlocksArea.addSeries(tmp2);

                    ind++;
                }
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
        glcBlockRenderer.setBaseStroke(new BasicStroke(3f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        glcBlockRenderer.setAutoPopulateSeriesStroke(false);
        plot.setRenderer(1, glcBlockRenderer);
        plot.setDataset(1, glcBlocks);

        glcBlockAreaRenderer.setAutoPopulateSeriesStroke(false);
        plot.setRenderer(2, glcBlockAreaRenderer);
        plot.setDataset(2, glcBlocksArea);

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

        JFreeChart pcChartInd = ChartFactory.createXYLineChart(
                "",
                "",
                "",
                glcBlocks,
                PlotOrientation.VERTICAL,
                false,
                true,
                false);

        // format chart
        pcChartInd.setBorderVisible(false);
        pcChartInd.setPadding(RectangleInsets.ZERO_INSETS);

        // get plot
        XYPlot plotInd = (XYPlot) pcChartInd.getPlot();

        // format plot
        plotInd.setDrawingSupplier(new DefaultDrawingSupplier(
                new Paint[] { DV.graphColors[0] },
                DefaultDrawingSupplier.DEFAULT_OUTLINE_PAINT_SEQUENCE,
                DefaultDrawingSupplier.DEFAULT_STROKE_SEQUENCE,
                DefaultDrawingSupplier.DEFAULT_OUTLINE_STROKE_SEQUENCE,
                DefaultDrawingSupplier.DEFAULT_SHAPE_SEQUENCE));
        plotInd.getRangeAxis().setVisible(true);
        plotInd.getDomainAxis().setVisible(true);
        plotInd.setOutlinePaint(null);
        plotInd.setOutlineVisible(false);
        plotInd.setInsets(RectangleInsets.ZERO_INSETS);
        plotInd.setDomainPannable(true);
        plotInd.setRangePannable(true);
        plotInd.setBackgroundPaint(DV.background);
        plotInd.setDomainGridlinePaint(Color.GRAY);

        // set domain
        ValueAxis domainViewInd = plotInd.getDomainAxis();
        domainViewInd.setRange(-0.1, DV.fieldLength-1);

        NumberAxis xAxisInd = (NumberAxis) plotInd.getDomainAxis();

        NumberTickUnit ntuInd = new NumberTickUnit(1)
        {
            @Override
            public String valueToString(double value) {
                return super.valueToString(value + 1);
            }
        };

        xAxisInd.setTickUnit(ntuInd);

        // set range
        NumberAxis yAxisInd = (NumberAxis) plotInd.getRangeAxis();
        yAxisInd.setTickUnit(new NumberTickUnit(0.25));
        yAxisInd.setAutoRange(false);
        yAxisInd.setRange(0, 1);

        // set block renderer and dataset
        glcIndBlockRenderer.setBaseStroke(new BasicStroke(3f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        glcIndBlockRenderer.setAutoPopulateSeriesStroke(false);
        plotInd.setRenderer(0, glcIndBlockRenderer);
        plotInd.setDataset(0, glcIndBlocks);

        glcIndBlockAreaRenderer.setAutoPopulateSeriesStroke(false);
        plotInd.setRenderer(1, glcIndBlockAreaRenderer);
        plotInd.setDataset(1, glcIndBlocksArea);

        ChartPanel chartPanelInd = new ChartPanel(pcChartInd);
        chartPanelInd.setMouseWheelEnabled(true);

        if (!indSep)
            return chartPanel;
        else
        {
            JPanel charts = new JPanel();
            charts.setLayout(new GridLayout(2, 1));
            charts.add(chartPanel);
            charts.add(chartPanelInd);

            return charts;
        }
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

    private ChartPanel drawStuff(int num_block)
    {
        // create main renderer and dataset
        XYLineAndShapeRenderer lineRenderer = new XYLineAndShapeRenderer(true, false);
        XYSeriesCollection graphLines = new XYSeriesCollection();

        // hyperblock renderer and dataset
        XYLineAndShapeRenderer pcBlockRenderer = new XYLineAndShapeRenderer(true, false);
        XYSeriesCollection pcBlocks = new XYSeriesCollection();

        BasicStroke strokes = new BasicStroke(3f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);

        // populate main series
        int cnt = 0;
        for (double[] dbs : hyper_blocks.get(num_block).hyper_block.get(0))
        {
            XYSeries tmp0 = new XYSeries(cnt, false, true);
            cnt++;

            int index = 0;
            for (int j = 0; j < DV.fieldLength; j++)
            {
                tmp0.add(index, dbs[j]);
                index++;
                tmp0.add(index, dbs[j]);
                index++;
            }

            graphLines.addSeries(tmp0);
        }

        // add hyperblocks
        XYSeries tmp1 = new XYSeries(0, false, true);

        int cnt1 = 0;
        for (int j = 0; j < DV.fieldLength; j++)
        {
            tmp1.add(cnt1, hyper_blocks.get(num_block).minimums.get(0)[j]);
            cnt1++;
            tmp1.add(cnt1, hyper_blocks.get(num_block).maximums.get(0)[j]);
            cnt1++;
        }

        pcBlockRenderer.setSeriesPaint(0, Color.ORANGE);
        pcBlockRenderer.setSeriesStroke(0, strokes);

        pcBlocks.addSeries(tmp1);

        JFreeChart pcChart = ChartFactory.createXYLineChart(
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
                new Paint[] { DV.graphColors[hyper_blocks.get(num_block).classNum] },
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
        domainView.setRange(-0.1, (DV.fieldLength*2) -1 + 0.1);

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

        // set block renderer and dataset
        pcBlockRenderer.setBaseStroke(new BasicStroke(3f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        pcBlockRenderer.setAutoPopulateSeriesStroke(false);
        plot.setRenderer(0, pcBlockRenderer);
        plot.setDataset(0, pcBlocks);

        lineRenderer.setBaseStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        lineRenderer.setAutoPopulateSeriesStroke(false);
        plot.setRenderer(1, lineRenderer);
        plot.setDataset(1, graphLines);

        ChartPanel chartPanel = new ChartPanel(pcChart);
        chartPanel.setMouseWheelEnabled(true);

        return chartPanel;
    }

    private ChartPanel SPC(ArrayList<ArrayList<DataObject>> obj)
    {
        // create main renderer and dataset
        XYLineAndShapeRenderer goodLineRenderer = new XYLineAndShapeRenderer(true, false);
        XYSeriesCollection goodGraphLines = new XYSeriesCollection();
        XYLineAndShapeRenderer badLineRenderer = new XYLineAndShapeRenderer(true, false);
        XYSeriesCollection badGraphLines = new XYSeriesCollection();

        // hyperblock renderer and dataset
        XYLineAndShapeRenderer pcBlockRenderer = new XYLineAndShapeRenderer(true, false);
        XYSeriesCollection pcBlocks = new XYSeriesCollection();
        XYLineAndShapeRenderer pcBlockOutlineRenderer = new XYLineAndShapeRenderer(true, false);
        XYSeriesCollection pcBlocksOutline = new XYSeriesCollection();
        XYAreaRenderer pcBlockAreaRenderer = new XYAreaRenderer(XYAreaRenderer.AREA);
        XYSeriesCollection pcBlocksArea = new XYSeriesCollection();

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

        double skip = 1.25;

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
                    double x_dist = 0;
                    for (int j = 0; j < DV.fieldLength; j++)
                    {
                        line.add(x_dist + data.data[i][j], data.data[i][j]);
                        x_dist += skip;

                        // add endpoint and timeline
                        if (j == DV.fieldLength - 1)
                        {
                            if (visualizeWithin.isSelected())
                            {
                                if (!visualizeOutline.isSelected() && within)
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
                                    if (!(visualizeOutline.isSelected() && within))
                                        goodGraphLines.addSeries(line);

                                    goodLineRenderer.setSeriesPaint(lineCnt, DV.graphColors[d]);

                                    if (within)
                                        goodLineRenderer.setSeriesStroke(lineCnt, strokes[within_block]);
                                }
                                else
                                {
                                    if (!(visualizeOutline.isSelected() && within))
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

        // add hyperblocks
        for (int k = 0, offset = 0; k < hyper_blocks.get(visualized_block).hyper_block.size(); k++)
        {
            if (hyper_blocks.get(visualized_block).hyper_block.get(k).size() > 1)
            {
                XYSeries tmp1 = new XYSeries(k-offset, false, true);

                int cnt = 0;

                double x_dist = 0;
                for (int j = 0; j < DV.fieldLength; j++)
                {
                    tmp1.add(x_dist + hyper_blocks.get(visualized_block).minimums.get(k)[j], hyper_blocks.get(visualized_block).maximums.get(k)[j]);

                    XYSeries tmp2 = new XYSeries(cnt, false, true);
                    XYSeries tmp3 = new XYSeries(cnt, false, true);

                    tmp2.add(x_dist + hyper_blocks.get(visualized_block).minimums.get(k)[j], hyper_blocks.get(visualized_block).maximums.get(k)[j]);
                    tmp2.add(x_dist + hyper_blocks.get(visualized_block).maximums.get(k)[j], hyper_blocks.get(visualized_block).maximums.get(k)[j]);
                    tmp2.add(x_dist + hyper_blocks.get(visualized_block).maximums.get(k)[j], hyper_blocks.get(visualized_block).minimums.get(k)[j]);
                    tmp2.add(x_dist + hyper_blocks.get(visualized_block).minimums.get(k)[j], hyper_blocks.get(visualized_block).minimums.get(k)[j]);
                    tmp2.add(x_dist + hyper_blocks.get(visualized_block).minimums.get(k)[j], hyper_blocks.get(visualized_block).maximums.get(k)[j]);

                    tmp3.add(x_dist + hyper_blocks.get(visualized_block).minimums.get(k)[j], hyper_blocks.get(visualized_block).maximums.get(k)[j]);
                    tmp3.add(x_dist + hyper_blocks.get(visualized_block).maximums.get(k)[j], hyper_blocks.get(visualized_block).maximums.get(k)[j]);
                    tmp3.add(x_dist + hyper_blocks.get(visualized_block).maximums.get(k)[j], hyper_blocks.get(visualized_block).minimums.get(k)[j]);
                    tmp3.add(x_dist + hyper_blocks.get(visualized_block).minimums.get(k)[j], hyper_blocks.get(visualized_block).minimums.get(k)[j]);
                    tmp3.add(x_dist + hyper_blocks.get(visualized_block).minimums.get(k)[j], hyper_blocks.get(visualized_block).maximums.get(k)[j]);

                    pcBlocksArea.addSeries(tmp2);
                    pcBlockAreaRenderer.setSeriesPaint(cnt, new Color(255, 200, 0, 20));

                    pcBlocksOutline.addSeries(tmp3);
                    pcBlockOutlineRenderer.setSeriesPaint(cnt, Color.ORANGE);
                    pcBlockOutlineRenderer.setSeriesStroke(cnt, new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
                    cnt++;

                    x_dist += skip;
                }

                pcBlockRenderer.setSeriesPaint(k-offset, Color.ORANGE);
                pcBlockRenderer.setSeriesStroke(k, strokes[k]);

                pcBlocks.addSeries(tmp1);
            }
            else
            {
                offset++;
            }
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
        domainView.setRange(-0.1, DV.fieldLength * skip);

        NumberAxis xAxis = (NumberAxis) plot.getDomainAxis();
        NumberTickUnit ntu = new NumberTickUnit(skip)
        {
            @Override
            public String valueToString(double value) {
                return super.valueToString((value / 1.25) + 1);
            }
        };

        xAxis.setTickUnit(ntu);

        // set range
        NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();
        yAxis.setTickUnit(new NumberTickUnit(0.25));
        yAxis.setAutoRange(false);
        yAxis.setRange(0, 1);

        // set block renderer and dataset
        pcBlockRenderer.setBaseStroke(new BasicStroke(3f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        pcBlockRenderer.setAutoPopulateSeriesStroke(false);
        plot.setRenderer(0, pcBlockRenderer);
        plot.setDataset(0, pcBlocks);

        if (stuff.isSelected())
        {
            pcBlockOutlineRenderer.setBaseStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
            pcBlockOutlineRenderer.setAutoPopulateSeriesStroke(false);
            plot.setRenderer(1, pcBlockOutlineRenderer);
            plot.setDataset(1, pcBlocksOutline);

            pcBlockAreaRenderer.setAutoPopulateSeriesStroke(false);
            plot.setRenderer(2, pcBlockAreaRenderer);
            plot.setDataset(2, pcBlocksArea);  
        }

        badLineRenderer.setBaseStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        badLineRenderer.setAutoPopulateSeriesStroke(false);
        plot.setRenderer(3, badLineRenderer);
        plot.setDataset(3, badGraphLines);

        goodLineRenderer.setBaseStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        goodLineRenderer.setAutoPopulateSeriesStroke(false);
        plot.setRenderer(4, goodLineRenderer);
        plot.setDataset(4, goodGraphLines);

        ChartPanel chartPanel = new ChartPanel(pcChart);
        chartPanel.setMouseWheelEnabled(true);
        return chartPanel;
    }

    private ChartPanel SPC_Reduced(ArrayList<ArrayList<DataObject>> obj)
    {
        // create main renderer and dataset
        XYLineAndShapeRenderer goodLineRenderer = new XYLineAndShapeRenderer(true, false);
        XYSeriesCollection goodGraphLines = new XYSeriesCollection();
        XYLineAndShapeRenderer badLineRenderer = new XYLineAndShapeRenderer(true, false);
        XYSeriesCollection badGraphLines = new XYSeriesCollection();

        XYLineAndShapeRenderer goodDotRenderer = new XYLineAndShapeRenderer(false, true);
        XYSeriesCollection goodGraphDots = new XYSeriesCollection();
        XYLineAndShapeRenderer badDotRenderer = new XYLineAndShapeRenderer(false, true);
        XYSeriesCollection badGraphDots = new XYSeriesCollection();

        // hyperblock renderer and dataset
        XYAreaRenderer pcBlockAreaRenderer = new XYAreaRenderer();
        XYSeriesCollection pcBlocksArea = new XYSeriesCollection();
        XYLineAndShapeRenderer pcBlockOutlineRenderer = new XYLineAndShapeRenderer(true, false);
        XYSeriesCollection pcBlocksOutline = new XYSeriesCollection();

        double skip = 1.25;
        pcBlockOutlineRenderer.setBaseItemLabelGenerator(new XYItemLabelGenerator()
        {
            @Override
            public String generateLabel(XYDataset xyDataset, int i, int i1)
            {
                // get x and y value
                double x = xyDataset.getX(i, i1).doubleValue();
                double y = xyDataset.getY(i, i1).doubleValue();

                // get x relative to self
                x -= i * skip;

                return String.format("(%.2f, %.2f)", x ,y);
            }
        });

        XYToolTipGenerator f_labels = new XYToolTipGenerator()
        {
            @Override
            public String generateToolTip(XYDataset xyDataset, int i, int i1)
            {
                // get name index
                int index = i * 2;

                // get x and y feature names
                if (DV.fieldNames.size() > index)
                {
                    String x = DV.fieldNames.get(index);
                    String y = index + 1 < DV.fieldNames.size() ? DV.fieldNames.get(index + 1) : DV.fieldNames.get(index);

                    return String.format("X = %s, Y = %s", x ,y);
                }
                else
                    return "";
            }
        };

        pcBlockAreaRenderer.setBaseToolTipGenerator(f_labels);
        pcBlockOutlineRenderer.setBaseToolTipGenerator(f_labels);
        pcBlockOutlineRenderer.setBaseItemLabelsVisible(true);

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
                    XYSeries dot = new XYSeries(lineCnt, false, true);
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
                    double x_dist = 0;
                    for (int j = 0; j < DV.fieldLength; j++)
                    {
                        if (j + 1 < DV.fieldLength)
                        {
                            line.add(x_dist + data.data[i][j], data.data[i][j+1]);
                            dot.add(x_dist + data.data[i][j], data.data[i][j+1]);
                        }
                        else
                        {
                            line.add(x_dist + data.data[i][j], data.data[i][j]);
                            dot.add(x_dist + data.data[i][j], data.data[i][j]);
                        }

                        j++;

                        x_dist += skip;

                        // add endpoint and timeline
                        if (j >= DV.fieldLength - 1)
                        {
                            if (visualizeWithin.isSelected())
                            {
                                if (!visualizeOutline.isSelected() && within)
                                {
                                    // add series
                                    if (d == hyper_blocks.get(visualized_block).classNum)
                                    {
                                        goodGraphLines.addSeries(line);
                                        goodGraphDots.addSeries(dot);

                                        goodLineRenderer.setSeriesPaint(lineCnt, DV.graphColors[d]);
                                        goodLineRenderer.setSeriesStroke(lineCnt, strokes[within_block]);

                                        goodDotRenderer.setSeriesPaint(lineCnt, Color.BLACK);
                                        goodDotRenderer.setSeriesShape(lineCnt, new Ellipse2D.Double(-2, -2, 4, 4));
                                    }
                                    else
                                    {
                                        badGraphLines.addSeries(line);
                                        badGraphDots.addSeries(dot);

                                        badLineRenderer.setSeriesPaint(lineCnt, DV.graphColors[d]);
                                        badLineRenderer.setSeriesStroke(lineCnt, strokes[within_block]);

                                        badDotRenderer.setSeriesPaint(lineCnt, Color.RED);
                                        badDotRenderer.setSeriesShape(lineCnt, new Ellipse2D.Double(-2, -2, 4, 4));
                                    }

                                    lineCnt++;
                                }
                            }
                            else
                            {
                                // add series
                                if (d == hyper_blocks.get(visualized_block).classNum)
                                {
                                    if (!(visualizeOutline.isSelected() && within))
                                    {
                                        goodGraphLines.addSeries(line);
                                        goodGraphDots.addSeries(dot);
                                    }

                                    goodLineRenderer.setSeriesPaint(lineCnt, DV.graphColors[d]);
                                    goodDotRenderer.setSeriesPaint(lineCnt, Color.BLACK);

                                    if (within)
                                    {
                                        goodLineRenderer.setSeriesStroke(lineCnt, strokes[within_block]);
                                        goodDotRenderer.setSeriesShape(lineCnt, new Ellipse2D.Double(-2, -2, 4, 4));
                                    }
                                }
                                else
                                {
                                    if (!(visualizeOutline.isSelected() && within))
                                    {
                                        badGraphLines.addSeries(line);
                                        badGraphDots.addSeries(dot);
                                    }

                                    badLineRenderer.setSeriesPaint(lineCnt, DV.graphColors[d]);
                                    badDotRenderer.setSeriesPaint(lineCnt, Color.RED);

                                    if (within)
                                    {
                                        badLineRenderer.setSeriesStroke(lineCnt, strokes[within_block]);
                                        badDotRenderer.setSeriesShape(lineCnt, new Ellipse2D.Double(-2, -2, 4, 4));
                                    }
                                }

                                lineCnt++;
                            }
                        }
                    }
                }
            }
        }

        // add hyperblocks
        for (int k = 0; k < hyper_blocks.get(visualized_block).hyper_block.size(); k++)
        {
            if (hyper_blocks.get(visualized_block).hyper_block.get(k).size() > 1)
            {
                int cnt = 0;
                double x_dist = 0;
                for (int j = 0; j < DV.fieldLength; j++)
                {
                    XYSeries line = new XYSeries(cnt, false, true);

                    if (j + 1 < DV.fieldLength)
                    {
                        line.add(x_dist + hyper_blocks.get(visualized_block).minimums.get(k)[j], hyper_blocks.get(visualized_block).minimums.get(k)[j+1]);
                        line.add(x_dist + hyper_blocks.get(visualized_block).minimums.get(k)[j], hyper_blocks.get(visualized_block).maximums.get(k)[j+1]);
                        line.add(x_dist + hyper_blocks.get(visualized_block).maximums.get(k)[j], hyper_blocks.get(visualized_block).maximums.get(k)[j+1]);
                        line.add(x_dist + hyper_blocks.get(visualized_block).maximums.get(k)[j], hyper_blocks.get(visualized_block).minimums.get(k)[j+1]);
                        line.add(x_dist + hyper_blocks.get(visualized_block).minimums.get(k)[j], hyper_blocks.get(visualized_block).minimums.get(k)[j+1]);
                    }
                    else
                    {
                        line.add(x_dist + hyper_blocks.get(visualized_block).minimums.get(k)[j], hyper_blocks.get(visualized_block).minimums.get(k)[j]);
                        line.add(x_dist + hyper_blocks.get(visualized_block).minimums.get(k)[j], hyper_blocks.get(visualized_block).maximums.get(k)[j]);
                        line.add(x_dist + hyper_blocks.get(visualized_block).maximums.get(k)[j], hyper_blocks.get(visualized_block).maximums.get(k)[j]);
                        line.add(x_dist + hyper_blocks.get(visualized_block).maximums.get(k)[j], hyper_blocks.get(visualized_block).minimums.get(k)[j]);
                        line.add(x_dist + hyper_blocks.get(visualized_block).minimums.get(k)[j], hyper_blocks.get(visualized_block).minimums.get(k)[j]);
                    }

                    j++;

                    x_dist += skip;

                    pcBlockOutlineRenderer.setSeriesPaint(cnt, Color.ORANGE);
                    pcBlockOutlineRenderer.setSeriesStroke(cnt, strokes[k]);
                    pcBlocksOutline.addSeries(line);

                    pcBlockAreaRenderer.setSeriesPaint(cnt, new Color(255, 200, 0, 20));
                    pcBlocksArea.addSeries(line);

                    cnt++;
                }
            }
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
        domainView.setRange(-0.2, (Math.ceil(DV.fieldLength / 2.0)) * skip);

        NumberAxis xAxis = (NumberAxis) plot.getDomainAxis();
        NumberTickUnit ntu = new NumberTickUnit(skip)
        {
            @Override
            public String valueToString(double value) {
                return super.valueToString((value / 1.25) + 1);
            }
        };

        xAxis.setTickUnit(ntu);

        // set range
        NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();
        yAxis.setTickUnit(new NumberTickUnit(0.25));
        yAxis.setAutoRange(false);
        yAxis.setRange(0, 1.05);

        // set block renderer and dataset
        badDotRenderer.setAutoPopulateSeriesStroke(false);
        plot.setRenderer(0, badDotRenderer);
        plot.setDataset(0, badGraphDots);

        goodDotRenderer.setAutoPopulateSeriesStroke(false);
        plot.setRenderer(1, goodDotRenderer);
        plot.setDataset(1, goodGraphDots);

        pcBlockOutlineRenderer.setBaseStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        pcBlockOutlineRenderer.setAutoPopulateSeriesStroke(false);
        plot.setRenderer(2, pcBlockOutlineRenderer);
        plot.setDataset(2, pcBlocksOutline);

        pcBlockAreaRenderer.setBaseStroke(new BasicStroke(3f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        pcBlockAreaRenderer.setAutoPopulateSeriesStroke(false);
        plot.setRenderer(3, pcBlockAreaRenderer);
        plot.setDataset(3, pcBlocksArea);

        badLineRenderer.setBaseStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        badLineRenderer.setAutoPopulateSeriesStroke(false);
        plot.setRenderer(4, badLineRenderer);
        plot.setDataset(4, badGraphLines);

        goodLineRenderer.setBaseStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        goodLineRenderer.setAutoPopulateSeriesStroke(false);
        plot.setRenderer(5, goodLineRenderer);
        plot.setDataset(5, goodGraphLines);

        ChartPanel chartPanel = new ChartPanel(pcChart);
        chartPanel.setMouseWheelEnabled(true);
        return chartPanel;
    }

    private ChartPanel SPC_Alt(ArrayList<ArrayList<DataObject>> obj)
    {
        // create main renderer and dataset
        XYLineAndShapeRenderer goodLineRenderer = new XYLineAndShapeRenderer(true, false);
        XYSeriesCollection goodGraphLines = new XYSeriesCollection();
        XYLineAndShapeRenderer badLineRenderer = new XYLineAndShapeRenderer(true, false);
        XYSeriesCollection badGraphLines = new XYSeriesCollection();

        XYLineAndShapeRenderer goodDotRenderer = new XYLineAndShapeRenderer(false, true);
        XYSeriesCollection goodGraphDots = new XYSeriesCollection();
        XYLineAndShapeRenderer badDotRenderer = new XYLineAndShapeRenderer(false, true);
        XYSeriesCollection badGraphDots = new XYSeriesCollection();

        // hyperblock renderer and dataset
        XYLineAndShapeRenderer pcBlockRenderer = new XYLineAndShapeRenderer(true, false);
        XYSeriesCollection pcBlocks = new XYSeriesCollection();
        XYLineAndShapeRenderer pcBlockOutlineRenderer = new XYLineAndShapeRenderer(true, false);
        XYSeriesCollection pcBlocksOutline = new XYSeriesCollection();
        XYAreaRenderer pcBlockAreaRenderer = new XYAreaRenderer(XYAreaRenderer.AREA);
        XYSeriesCollection pcBlocksArea = new XYSeriesCollection();

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

        double skip = 1.25;

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
                    XYSeries dot = new XYSeries(lineCnt, false, true);
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
                    double x_dist = 0;
                    for (int j = 0; j < DV.fieldLength; j++)
                    {
                        if (j + 1 < DV.fieldLength)
                        {
                            line.add(x_dist + data.data[i][j], data.data[i][j+1]);
                            dot.add(x_dist + data.data[i][j], data.data[i][j+1]);
                        }
                        else
                        {
                            line.add(x_dist + data.data[i][j], data.data[i][j]);
                            dot.add(x_dist + data.data[i][j], data.data[i][j]);
                        }

                        j++;

                        x_dist += skip;

                        // add endpoint and timeline
                        if (j >= DV.fieldLength - 1)
                        {
                            if (visualizeWithin.isSelected())
                            {
                                if (!visualizeOutline.isSelected() && within)
                                {
                                    // add series
                                    if (d == hyper_blocks.get(visualized_block).classNum)
                                    {
                                        goodGraphLines.addSeries(line);
                                        goodGraphDots.addSeries(dot);

                                        goodLineRenderer.setSeriesPaint(lineCnt, DV.graphColors[d]);
                                        goodLineRenderer.setSeriesStroke(lineCnt, strokes[within_block]);

                                        goodDotRenderer.setSeriesPaint(lineCnt, Color.BLACK);
                                        goodDotRenderer.setSeriesShape(lineCnt, new Ellipse2D.Double(-2, -2, 4, 4));
                                    }
                                    else
                                    {
                                        badGraphLines.addSeries(line);
                                        badGraphDots.addSeries(dot);

                                        badLineRenderer.setSeriesPaint(lineCnt, DV.graphColors[d]);
                                        badLineRenderer.setSeriesStroke(lineCnt, strokes[within_block]);

                                        badDotRenderer.setSeriesPaint(lineCnt, Color.RED);
                                        badDotRenderer.setSeriesShape(lineCnt, new Ellipse2D.Double(-2, -2, 4, 4));
                                    }

                                    lineCnt++;
                                }
                            }
                            else
                            {
                                // add series
                                if (d == hyper_blocks.get(visualized_block).classNum)
                                {
                                    if (!(visualizeOutline.isSelected() && within))
                                    {
                                        goodGraphLines.addSeries(line);
                                        goodGraphDots.addSeries(dot);
                                    }

                                    goodLineRenderer.setSeriesPaint(lineCnt, DV.graphColors[d]);
                                    goodDotRenderer.setSeriesPaint(lineCnt, Color.BLACK);

                                    if (within)
                                    {
                                        goodLineRenderer.setSeriesStroke(lineCnt, strokes[within_block]);
                                        goodDotRenderer.setSeriesShape(lineCnt, new Ellipse2D.Double(-2, -2, 4, 4));
                                    }
                                }
                                else
                                {
                                    if (!(visualizeOutline.isSelected() && within))
                                    {
                                        badGraphLines.addSeries(line);
                                        badGraphDots.addSeries(dot);
                                    }

                                    badLineRenderer.setSeriesPaint(lineCnt, DV.graphColors[d]);
                                    badDotRenderer.setSeriesPaint(lineCnt, Color.RED);

                                    if (within)
                                    {
                                        badLineRenderer.setSeriesStroke(lineCnt, strokes[within_block]);
                                        badDotRenderer.setSeriesShape(lineCnt, new Ellipse2D.Double(-2, -2, 4, 4));
                                    }
                                }

                                lineCnt++;
                            }
                        }
                    }
                }
            }
        }

        // add hyperblocks
        for (int k = 0; k < hyper_blocks.get(visualized_block).hyper_block.size(); k++)
        {
            if (hyper_blocks.get(visualized_block).hyper_block.get(k).size() > 1)
            {
                int cnt = 0;
                double x_dist = 0;
                for (int j = 0; j < DV.fieldLength; j++)
                {
                    XYSeries line = new XYSeries(cnt, false, true);

                    if (j + 1 < DV.fieldLength)
                    {
                        line.add(x_dist + hyper_blocks.get(visualized_block).minimums.get(k)[j], hyper_blocks.get(visualized_block).minimums.get(k)[j+1]);
                        line.add(x_dist + hyper_blocks.get(visualized_block).minimums.get(k)[j], hyper_blocks.get(visualized_block).maximums.get(k)[j+1]);
                        line.add(x_dist + hyper_blocks.get(visualized_block).maximums.get(k)[j], hyper_blocks.get(visualized_block).maximums.get(k)[j+1]);
                        line.add(x_dist + hyper_blocks.get(visualized_block).maximums.get(k)[j], hyper_blocks.get(visualized_block).minimums.get(k)[j+1]);
                        line.add(x_dist + hyper_blocks.get(visualized_block).minimums.get(k)[j], hyper_blocks.get(visualized_block).minimums.get(k)[j+1]);
                    }
                    else
                    {
                        line.add(x_dist + hyper_blocks.get(visualized_block).minimums.get(k)[j], hyper_blocks.get(visualized_block).minimums.get(k)[j]);
                        line.add(x_dist + hyper_blocks.get(visualized_block).minimums.get(k)[j], hyper_blocks.get(visualized_block).maximums.get(k)[j]);
                        line.add(x_dist + hyper_blocks.get(visualized_block).maximums.get(k)[j], hyper_blocks.get(visualized_block).maximums.get(k)[j]);
                        line.add(x_dist + hyper_blocks.get(visualized_block).maximums.get(k)[j], hyper_blocks.get(visualized_block).minimums.get(k)[j]);
                        line.add(x_dist + hyper_blocks.get(visualized_block).minimums.get(k)[j], hyper_blocks.get(visualized_block).minimums.get(k)[j]);
                    }

                    j++;

                    x_dist += skip;

                    pcBlockOutlineRenderer.setSeriesPaint(cnt, Color.ORANGE);
                    pcBlockOutlineRenderer.setSeriesStroke(cnt, strokes[k]);
                    pcBlocksOutline.addSeries(line);

                    cnt++;
                }
            }
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
        domainView.setRange(-0.1, (Math.ceil(DV.fieldLength / 2.0)) * skip);

        NumberAxis xAxis = (NumberAxis) plot.getDomainAxis();
        NumberTickUnit ntu = new NumberTickUnit(skip)
        {
            @Override
            public String valueToString(double value) {
                return super.valueToString((value / 1.25) + 1);
            }
        };

        xAxis.setTickUnit(ntu);

        // set range
        NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();
        yAxis.setTickUnit(new NumberTickUnit(0.25));
        yAxis.setAutoRange(false);
        yAxis.setRange(0, 1);

        // set block renderer and dataset
        badDotRenderer.setAutoPopulateSeriesStroke(false);
        plot.setRenderer(0, badDotRenderer);
        plot.setDataset(0, badGraphDots);

        goodDotRenderer.setAutoPopulateSeriesStroke(false);
        plot.setRenderer(1, goodDotRenderer);
        plot.setDataset(1, goodGraphDots);

        pcBlockRenderer.setBaseStroke(new BasicStroke(3f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        pcBlockRenderer.setAutoPopulateSeriesStroke(false);
        plot.setRenderer(2, pcBlockRenderer);
        plot.setDataset(2, pcBlocks);

        pcBlockOutlineRenderer.setBaseStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        pcBlockOutlineRenderer.setAutoPopulateSeriesStroke(false);
        plot.setRenderer(3, pcBlockOutlineRenderer);
        plot.setDataset(3, pcBlocksOutline);

        badLineRenderer.setBaseStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        badLineRenderer.setAutoPopulateSeriesStroke(false);
        plot.setRenderer(4, badLineRenderer);
        plot.setDataset(4, badGraphLines);

        goodLineRenderer.setBaseStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        goodLineRenderer.setAutoPopulateSeriesStroke(false);
        plot.setRenderer(5, goodLineRenderer);
        plot.setDataset(5, goodGraphLines);

        ChartPanel chartPanel = new ChartPanel(pcChart);
        chartPanel.setMouseWheelEnabled(true);
        return chartPanel;
    }

    private void pc_lvl_2_Test(ArrayList<ArrayList<DataObject>> obj, int num_block)
    {
        // FIND HB TO VISUALIZE
        double[] tmp_pnt = new double[DV.fieldLength];
        int tmp_cnt = 0;
        for (int i = 0; i < DV.fieldLength; i++)
        {
            tmp_pnt[i] = originalHyperBlocks.get(num_block).minimums.get(0)[tmp_cnt];
            i++;
            tmp_pnt[i] = originalHyperBlocks.get(num_block).maximums.get(0)[tmp_cnt];
            tmp_cnt++;
        }

        int block_to_vis = -1;

        for (int i = 0; i < hyper_blocks.size(); i++)
        {
            for (int j = 0; j < hyper_blocks.get(i).hyper_block.get(0).size(); j++)
            {
                    if (Arrays.equals(hyper_blocks.get(i).hyper_block.get(0).get(j), tmp_pnt))
                    {
                        block_to_vis = i;
                        break;
                    }
            }
            if (block_to_vis > -1)
                break;
        }

        // create main renderer and dataset
        XYLineAndShapeRenderer goodLineRenderer = new XYLineAndShapeRenderer(true, false);
        XYSeriesCollection goodGraphLines = new XYSeriesCollection();
        XYLineAndShapeRenderer badLineRenderer = new XYLineAndShapeRenderer(true, false);
        XYSeriesCollection badGraphLines = new XYSeriesCollection();

        XYLineAndShapeRenderer originalLineRenderer = new XYLineAndShapeRenderer(true, false);
        XYSeriesCollection originalGraphLines = new XYSeriesCollection();

        // hyperblock renderer and dataset
        XYLineAndShapeRenderer pcBlockRenderer = new XYLineAndShapeRenderer(true, false);
        XYSeriesCollection pcBlocks = new XYSeriesCollection();
        XYAreaRenderer pcBlockAreaRenderer = new XYAreaRenderer(XYAreaRenderer.AREA);
        XYSeriesCollection pcBlocksArea = new XYSeriesCollection();

        BasicStroke[] strokes = new BasicStroke[hyper_blocks.get(block_to_vis).hyper_block.size()];

        for (int k = 0; k < hyper_blocks.get(block_to_vis).hyper_block.size(); k++)
        {
            if (hyper_blocks.get(block_to_vis).hyper_block.get(k).size() > 1)
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

                    for (int k = 0; k < hyper_blocks.get(block_to_vis).hyper_block.size(); k++)
                    {
                        boolean within_cur = true;

                        for (int j = 0; j < DV.fieldLength; j++)
                        {
                            if (data.data[i][j] != tmp_pnt[j])//if (data.data[i][j] < hyper_blocks.get(block_to_vis).minimums.get(k)[j] || data.data[i][j] > hyper_blocks.get(block_to_vis).maximums.get(k)[j])
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
                                if (!visualizeOutline.isSelected() && within)
                                {
                                    // add series
                                    if (d == hyper_blocks.get(block_to_vis).classNum)
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
                                if (d == hyper_blocks.get(block_to_vis).classNum)
                                {
                                    if (!(visualizeOutline.isSelected() && within))
                                        goodGraphLines.addSeries(line);

                                    goodLineRenderer.setSeriesPaint(lineCnt, DV.graphColors[d]);

                                    if (within)
                                        goodLineRenderer.setSeriesStroke(lineCnt, strokes[within_block]);
                                }
                                else
                                {
                                    if (!(visualizeOutline.isSelected() && within))
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

        // add hyperblocks
        for (int k = 0, offset = 0; k < hyper_blocks.get(block_to_vis).hyper_block.size(); k++)
        {
            if (hyper_blocks.get(block_to_vis).hyper_block.get(k).size() > 1)
            {
                XYSeries tmp1 = new XYSeries(k-offset, false, true);
                XYSeries tmp2 = new XYSeries(k-offset, false, true);

                for (int j = 0; j < DV.fieldLength; j++)
                {
                    tmp1.add(j, hyper_blocks.get(block_to_vis).minimums.get(k)[j]);
                    tmp2.add(j, hyper_blocks.get(block_to_vis).minimums.get(k)[j]);
                }

                for (int j = DV.fieldLength - 1; j > -1; j--)
                {
                    tmp1.add(j, hyper_blocks.get(block_to_vis).maximums.get(k)[j]);
                    tmp2.add(j, hyper_blocks.get(block_to_vis).maximums.get(k)[j]);
                }

                tmp1.add(0, hyper_blocks.get(block_to_vis).minimums.get(k)[0]);
                tmp2.add(0, hyper_blocks.get(block_to_vis).minimums.get(k)[0]);

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


        int old_fieldlength = originalHyperBlocks.get(num_block).minimums.get(0).length;

        /*int cnt = 0;
        for (double[] dbs : originalHyperBlocks.get(num_block).hyper_block.get(0))
        {
            XYSeries tmp0 = new XYSeries(cnt, false, true);

            int index = 0;
            for (int j = 0; j < old_fieldlength; j++)
            {
                tmp0.add(index, dbs[j]);
                index++;
                tmp0.add(index, dbs[j]);
                index++;
            }

            originalGraphLines.addSeries(tmp0);
            originalLineRenderer.setSeriesPaint(cnt, Color.GREEN);
            originalLineRenderer.setSeriesStroke(cnt, strokes[0]);
            cnt++;
        }*/

        for (int d = 0, lineCnt = 0; d < originalObjects.size(); d++)
        {
            for (DataObject data : obj.get(d))
            {
                for (int i = 0; i < data.data.length; i++)
                {
                    // start line at (0, 0)
                    XYSeries line = new XYSeries(lineCnt, false, true);
                    boolean within = false;
                    int within_block = 0;

                    for (int k = 0; k < originalHyperBlocks.get(num_block).hyper_block.size(); k++)
                    {
                        boolean within_cur = true;

                        for (int j = 0; j < old_fieldlength; j++)
                        {
                            if (data.data[i][j] < originalHyperBlocks.get(num_block).minimums.get(k)[j] || data.data[i][j] > originalHyperBlocks.get(num_block).maximums.get(k)[j])
                            {
                                within_cur = false;
                            }

                            if (j == old_fieldlength - 1)
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
                    for (int j = 0; j < old_fieldlength; j++)
                    {
                        line.add(j, data.data[i][j]);

                        // add endpoint and timeline
                        if (j == old_fieldlength - 1)
                        {
                            originalGraphLines.addSeries(line);
                            originalLineRenderer.setSeriesPaint(lineCnt, Color.GREEN);
                            originalLineRenderer.setSeriesStroke(lineCnt, strokes[0]);
                            lineCnt++;
                        }
                    }
                }
            }

            if (d == originalObjects.size() - 1)
            {
                int size = 0;
                for (ArrayList<double[]> stuff : originalHyperBlocks.get(num_block).hyper_block)
                {
                    size += stuff.size();
                }

                System.out.println("Current HB: " + num_block);
                System.out.println("Num Pnts: " + size);
                System.out.println("Num Pnts: " + lineCnt);
                System.out.println("Diff Pnts: " + lineCnt);
            }
        }

        JFreeChart pcChart = ChartFactory.createXYLineChart(
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

        // set block renderer and dataset
        pcBlockRenderer.setBaseStroke(new BasicStroke(3f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        pcBlockRenderer.setAutoPopulateSeriesStroke(false);
        plot.setRenderer(0, pcBlockRenderer);
        plot.setDataset(0, pcBlocks);

        pcBlockAreaRenderer.setAutoPopulateSeriesStroke(false);
        plot.setRenderer(1, pcBlockAreaRenderer);
        plot.setDataset(1, pcBlocksArea);

        badLineRenderer.setBaseStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        badLineRenderer.setAutoPopulateSeriesStroke(false);
        plot.setRenderer(2, badLineRenderer);
        plot.setDataset(2, badGraphLines);

        goodLineRenderer.setBaseStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        goodLineRenderer.setAutoPopulateSeriesStroke(false);
        plot.setRenderer(3, goodLineRenderer);
        plot.setDataset(3, goodGraphLines);

        originalLineRenderer.setBaseStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        originalLineRenderer.setAutoPopulateSeriesStroke(false);
        plot.setRenderer(4, originalLineRenderer);
        plot.setDataset(4, originalGraphLines);

        ChartPanel chartPanel = new ChartPanel(pcChart);
        chartPanel.setMouseWheelEnabled(true);

        JOptionPane.showMessageDialog(null, chartPanel);
    }

    private void pc_lvl_2_Test2(ArrayList<ArrayList<DataObject>> obj, int num_block)
    {
        // create main renderer and dataset
        XYLineAndShapeRenderer goodLineRenderer = new XYLineAndShapeRenderer(true, false);
        XYSeriesCollection goodGraphLines = new XYSeriesCollection();
        XYLineAndShapeRenderer badLineRenderer = new XYLineAndShapeRenderer(true, false);
        XYSeriesCollection badGraphLines = new XYSeriesCollection();

        XYLineAndShapeRenderer originalLineRenderer = new XYLineAndShapeRenderer(true, false);
        XYSeriesCollection originalGraphLines = new XYSeriesCollection();

        // hyperblock renderer and dataset
        XYLineAndShapeRenderer pcBlockRenderer = new XYLineAndShapeRenderer(true, false);
        XYSeriesCollection pcBlocks = new XYSeriesCollection();
        XYAreaRenderer pcBlockAreaRenderer = new XYAreaRenderer(XYAreaRenderer.AREA);
        XYSeriesCollection pcBlocksArea = new XYSeriesCollection();

        BasicStroke[] strokes = new BasicStroke[hyper_blocks.get(num_block).hyper_block.size()];

        for (int k = 0; k < hyper_blocks.get(num_block).hyper_block.size(); k++)
        {
            if (hyper_blocks.get(num_block).hyper_block.get(k).size() > 1)
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
        int total = 0;
        for (int d = 0; d < originalObjects.size(); d++)
        {
            int lineCnt = 0;

            for (DataObject data : originalObjects.get(d))
            {
                for (int i = 0; i < data.data.length; i++)
                {
                    // start line at (0, 0)
                    XYSeries line = new XYSeries(lineCnt, false, true);
                    boolean within = false;
                    int within_block = 0;

                    for (int k = 0; k < hyper_blocks.get(num_block).hyper_block.size(); k++)
                    {
                        boolean within_cur = true;

                        for (int j = 0, cnt = 0; j < DV.fieldLength; j++)
                        {
                            if (data.data[i][cnt] < hyper_blocks.get(num_block).minimums.get(k)[j] || data.data[i][cnt] > hyper_blocks.get(num_block).maximums.get(k)[j])
                            {
                                within_cur = false;
                            }

                            if ((j+1) % 2 == 0)
                                cnt++;

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
                    for (int j = 0, cnt = 0; j < DV.fieldLength; j++)
                    {
                        line.add(j, data.data[i][cnt]);
                        j++;
                        line.add(j, data.data[i][cnt]);
                        cnt++;

                        // add endpoint and timeline
                        if (j == DV.fieldLength - 1)
                        {
                            if (visualizeWithin.isSelected())
                            {
                                if (!visualizeOutline.isSelected() && within)
                                {
                                    // add series
                                    if (d == hyper_blocks.get(num_block).classNum)
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
                                if (d == hyper_blocks.get(num_block).classNum)
                                {
                                    if (!(visualizeOutline.isSelected() && within))
                                        goodGraphLines.addSeries(line);

                                    goodLineRenderer.setSeriesPaint(lineCnt, DV.graphColors[d]);

                                    if (within)
                                        goodLineRenderer.setSeriesStroke(lineCnt, strokes[within_block]);
                                }
                                else
                                {
                                    if (!(visualizeOutline.isSelected() && within))
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
            total += lineCnt;
        }

        // add hyperblocks
        for (int k = 0, offset = 0; k < hyper_blocks.get(num_block).hyper_block.size(); k++)
        {
            if (hyper_blocks.get(num_block).hyper_block.get(k).size() > 1)
            {
                XYSeries tmp1 = new XYSeries(k-offset, false, true);
                XYSeries tmp2 = new XYSeries(k-offset, false, true);

                for (int j = 0; j < DV.fieldLength; j++)
                {
                    tmp1.add(j, hyper_blocks.get(num_block).minimums.get(k)[j]);
                    tmp2.add(j, hyper_blocks.get(num_block).minimums.get(k)[j]);
                }

                for (int j = DV.fieldLength - 1; j > -1; j--)
                {
                    tmp1.add(j, hyper_blocks.get(num_block).maximums.get(k)[j]);
                    tmp2.add(j, hyper_blocks.get(num_block).maximums.get(k)[j]);
                }

                tmp1.add(0, hyper_blocks.get(num_block).minimums.get(k)[0]);
                tmp2.add(0, hyper_blocks.get(num_block).minimums.get(k)[0]);

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

        JFreeChart pcChart = ChartFactory.createXYLineChart(
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

        // set block renderer and dataset
        pcBlockRenderer.setBaseStroke(new BasicStroke(3f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        pcBlockRenderer.setAutoPopulateSeriesStroke(false);
        plot.setRenderer(0, pcBlockRenderer);
        plot.setDataset(0, pcBlocks);

        pcBlockAreaRenderer.setAutoPopulateSeriesStroke(false);
        plot.setRenderer(1, pcBlockAreaRenderer);
        plot.setDataset(1, pcBlocksArea);

        badLineRenderer.setBaseStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        badLineRenderer.setAutoPopulateSeriesStroke(false);
        plot.setRenderer(2, badLineRenderer);
        plot.setDataset(2, badGraphLines);

        goodLineRenderer.setBaseStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        goodLineRenderer.setAutoPopulateSeriesStroke(false);
        plot.setRenderer(3, goodLineRenderer);
        plot.setDataset(3, goodGraphLines);

        originalLineRenderer.setBaseStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        originalLineRenderer.setAutoPopulateSeriesStroke(false);
        plot.setRenderer(4, originalLineRenderer);
        plot.setDataset(4, originalGraphLines);

        ChartPanel chartPanel = new ChartPanel(pcChart);
        chartPanel.setMouseWheelEnabled(true);

        JOptionPane.showMessageDialog(null, chartPanel);
    }

    /*private void combineAll()
    {
        // hyperblock renderer and dataset
        XYLineAndShapeRenderer pcBlockRenderer = new XYLineAndShapeRenderer(true, false);
        XYSeriesCollection pcBlocks = new XYSeriesCollection();
        XYAreaRenderer pcBlockAreaRenderer = new XYAreaRenderer(XYAreaRenderer.AREA);
        XYSeriesCollection pcBlocksArea = new XYSeriesCollection();

        // add hyperblocks
        XYSeries hb1 = new XYSeries(0, false, true);
        XYSeries hb1a = new XYSeries(0, false, true);

        XYSeries hb2 = new XYSeries(0, false, true);
        XYSeries hb2a = new XYSeries(0, false, true);

        double[] hb1_tmp = new double[DV.fieldLength];

        double[] hb2_tmp = new double[DV.fieldLength];

        for (int i = 0; i < DV.fieldLength; i++)
        {
            hb1_tmp[i] = -1;
            hb2_tmp[i] = -1;
        }

        for (int i = 0; i < hyper_blocks.size(); i++)
        {
            for (int j = 0; j < hyper_blocks.get(i).hyper_block.size(); j++)
            {
                for (int k = 0; k < DV.fieldLength; k++)
                {
                    if (hyper_blocks.get(i).classNum == 0)
                        hb1_tmp[j] = Math.maxhyper_blocks.get(i).minimums.get(0)[k];
                    else
                        hb2_tmp[j] = hyper_blocks.get(i).minimums.get(0)[k];
                }
            }
        }

        JFreeChart pcChart = ChartFactory.createXYLineChart(
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

        // set block renderer and dataset
        pcBlockRenderer.setBaseStroke(new BasicStroke(3f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        pcBlockRenderer.setAutoPopulateSeriesStroke(false);
        plot.setRenderer(0, pcBlockRenderer);
        plot.setDataset(0, pcBlocks);

        pcBlockAreaRenderer.setAutoPopulateSeriesStroke(false);
        plot.setRenderer(1, pcBlockAreaRenderer);
        plot.setDataset(1, pcBlocksArea);

        badLineRenderer.setBaseStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        badLineRenderer.setAutoPopulateSeriesStroke(false);
        plot.setRenderer(2, badLineRenderer);
        plot.setDataset(2, badGraphLines);

        goodLineRenderer.setBaseStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        goodLineRenderer.setAutoPopulateSeriesStroke(false);
        plot.setRenderer(3, goodLineRenderer);
        plot.setDataset(3, goodGraphLines);

        originalLineRenderer.setBaseStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        originalLineRenderer.setAutoPopulateSeriesStroke(false);
        plot.setRenderer(4, originalLineRenderer);
        plot.setDataset(4, originalGraphLines);

        ChartPanel chartPanel = new ChartPanel(pcChart);
        chartPanel.setMouseWheelEnabled(true);

        JOptionPane.showMessageDialog(null, chartPanel);
    }*/
}
