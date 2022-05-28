import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

import Sliders.CustomSliderUI;
import Sliders.RangeSlider;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.DefaultDrawingSupplier;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeriesCollection;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DV extends JFrame
{
    /**************************************************
     * FOR GUI
     *************************************************/
    // sliders
    static RangeSlider domainSlider;
    static RangeSlider overlapSlider;
    static JSlider thresholdSlider;

    // panels
    static JPanel sliderPanel = new JPanel();
    static JPanel confusionMatrixPanel = new JPanel();
    static JPanel graphPanel = new JPanel();
    static JPanel graphDomainPanel = new JPanel();

    // confusion matrices
    static JTextArea allDataCM = new JTextArea(10, 40);
    static JTextArea dataWithoutOverlapCM = new JTextArea(10, 40);
    static JTextArea overlapCM = new JTextArea(10, 40);
    static JTextArea worstCaseCM = new JTextArea(10, 40);

    // scroll areas
    JScrollPane graphPane = new JScrollPane();
    JScrollPane anglesPane = new JScrollPane();
    JScrollPane confusionMatrixPane = new JScrollPane();

    // main frame for DV
    static JFrame mainFrame;

    /**************************************************
     * FOR GRAPHS
     *************************************************/
    // line colors
    static Color domainLines = Color.BLACK;
    static Color overlapLines = Color.ORANGE;
    static Color thresholdLine = Color.GREEN;

    // graph colors
    static Color[] graphColors = new Color[] {
            new Color(102, 34, 139),   // upper graph (purple)
            new Color(84, 133, 145)    // lower graph (dark cyan)
    };

    // show bars instead of endpoints for graphs
    // the height of a bar is equal to the number of points in its location
    static boolean showBars = false;

    // draw only overlap
    static boolean drawOverlap = false;

    // domain active
    static boolean domainActive = true;

    // domain area
    static double[] domainArea;

    // upper class is visualized on the upper graph
    // lower classes are visualized on the lower graph
    static int upperClass = 0;
    static ArrayList<Boolean> lowerClasses = new ArrayList<>(List.of(false));

    /**************************************************
     * FOR CONFUSION MATRICES
     *************************************************/
    // overlap area
    static double[] overlapArea;

    // threshold point
    static double threshold;

    // lesser class (lower mean)
    static int lowerRange = 0;

    // current and previous accuracies (only applicable to 3+ class datasets)
    static double accuracy;
    static ArrayList<Double> prevAccuracies;

    /************************************************
     * FOR INPUT DATA
     ***********************************************/
    // input data info
    static boolean hasID = false;
    static boolean hasClasses = false;

    // min-max or zScore min-max normalization
    static boolean zScoreMinMax = false;

    /************************************************
     * FOR DATA
     ***********************************************/
    // angles and initial angles (store angles before optimizing)
    public static double[] angles;
    public static double[] initialAngles; // ASK KOVALERCHUK ABOUT CHANGING TO LDA ANGLES

    // normalized and original data
    static ArrayList<DataObject> data = new ArrayList<>();
    static ArrayList<DataObject> originalData = new ArrayList<>();

    // classes for data
    static ArrayList<String> allClasses = new ArrayList<>();
    static ArrayList<String> uniqueClasses = new ArrayList<>();
    static int classNumber;

    // fieldnames and length
    static ArrayList<String> fieldNames = new ArrayList<>();
    static int fieldLength;


    /**
     * Main handler for UI
     * Creates main panel, menu bar, and toolbar.
     */
    public DV()
    {
        // set DV properties
        super("DV Program");
        this.setSize(Resolutions.dvWindow[0], Resolutions.dvWindow[1]);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setExtendedState(this.getExtendedState() & (~JFrame.ICONIFIED));
        this.setLocationRelativeTo(null);
        this.setResizable(false);
        this.setVisible(true);

        // set mainFrame to DV
        mainFrame = this;

        // create option bars
        createMenuBar();
        createToolBar();

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    }


    /**
     * Creates menu bar for DV Program
     * Menu bar features:
     * creating new projects, opening saved projects,
     * saving projects (old save), saving projects as (new save),
     * importing data, and informing users
     */
    private void createMenuBar()
    {
        // creates menu bar at top of screen
        JMenuBar menuBar = new JMenuBar();
        mainFrame.setJMenuBar(menuBar);

        // file menu
        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);
        menuBar.add(fileMenu);

        // file menu item: create new project
        JMenuItem newProjItem = new JMenuItem("Create New Project");
        newProjItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.ALT_DOWN_MASK));
        newProjItem.addActionListener(e -> createNewProject());
        fileMenu.add(newProjItem);

        // file menu item: open saved project
        JMenuItem openSavedItem = new JMenuItem("Open Saved Project");
        openSavedItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.ALT_DOWN_MASK));
        openSavedItem.addActionListener(e -> openSavedProject());
        fileMenu.add(openSavedItem);

        // file menu item: save project
        JMenuItem saveProjItem = new JMenuItem("Save Project");
        saveProjItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.ALT_DOWN_MASK));
        saveProjItem.addActionListener(e -> saveProject());
        fileMenu.add(saveProjItem);

        // file menu item: save project as
        JMenuItem saveProjAsItem = new JMenuItem("Save Project As");
        saveProjAsItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.ALT_DOWN_MASK));
        saveProjAsItem.addActionListener(e -> saveProjectAs());
        fileMenu.add(saveProjAsItem);

        // file menu item: import data
        JMenuItem importDataItem = new JMenuItem("Import Data");
        importDataItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I, InputEvent.ALT_DOWN_MASK));
        importDataItem.addActionListener(e -> importData());
        fileMenu.add(importDataItem);

        // help menu
        JMenu helpMenu = new JMenu("Help");
        helpMenu.setMnemonic(KeyEvent.VK_H);
        menuBar.add(helpMenu);

        // help menu item: open manual
        JMenuItem manualItem = new JMenuItem("User Manual");
        manualItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_M, InputEvent.ALT_DOWN_MASK));
        manualItem.addActionListener(e ->
        {
            // try opening DVManual
            try
            {
               Desktop.getDesktop().open(new File(System.getProperty("user.dir") + "\\DVManual.pdf"));
            }
            catch (IOException ioe)
            {
                JOptionPane.showMessageDialog(
                        mainFrame,
                        "Error opening user manual.\n" +
                                ioe,
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        });
        helpMenu.add(manualItem);

        // help manu item: normalization info
        JMenuItem normalizationInfoItem = new JMenuItem("Normalization Info");
        normalizationInfoItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_U, InputEvent.ALT_DOWN_MASK));
        normalizationInfoItem.addActionListener(e -> normalizationInfoPopup());
    }


    /**
     *
     */
    private void createToolBar()
    {
        // create toolbar
        JToolBar toolBar = new JToolBar("Tool Palette");
        toolBar.setFloatable(false);
        toolBar.setRollover(true);

        // add colors options
        JButton colorOptionsBtn = new JButton("Color Options");
        colorOptionsBtn.setToolTipText("Open the color options menu");
        colorOptionsBtn.addActionListener(e -> new ColorOptionsMenu(MouseInfo.getPointerInfo().getLocation()));
        toolBar.add(colorOptionsBtn);

        // add visualization options
        JButton visOptionsBtn = new JButton("Visualization Options");
        visOptionsBtn.setToolTipText("Open the visualization options menu");
        visOptionsBtn.addActionListener(e -> new VisOptionsMenu(MouseInfo.getPointerInfo().getLocation()));
        toolBar.add(visOptionsBtn);

        // result screen
        JButton resetScreenBtn = new JButton("Reset Screen");
        resetScreenBtn.setToolTipText("Resets rendered zoom area");
        resetScreenBtn.addActionListener(e -> DataVisualization.drawGraphs(0));
        toolBar.add(resetScreenBtn);

        // optimize visualization
        JButton optimizeBtn = new JButton("Optimize Visualization");
        optimizeBtn.setToolTipText("Attempts to optimize angles and threshold");
        optimizeBtn.addActionListener(e -> DataVisualization.optimizeVisualization());
        toolBar.add(optimizeBtn);

        // undo optimization
        JButton undoOptimizeBtn = new JButton("Undo Optimization");
        undoOptimizeBtn.setToolTipText("Reverses previous optimization operation");
        undoOptimizeBtn.addActionListener(e -> DataVisualization.undoOptimization());
        toolBar.add(undoOptimizeBtn);

        // toggle bar-line
        JButton barLineBtn = new JButton("Toggle Bar-line");
        barLineBtn.setToolTipText("Toggle graph showing bar-line of endpoint placement");
        barLineBtn.addActionListener(e ->
        {
            showBars = true;
            DataVisualization.drawGraphs(0);
        });
        toolBar.add(barLineBtn);

        // set toolbar north
        JPanel toolBarPanel = new JPanel(new BorderLayout());
        toolBarPanel.setVisible(true);
        toolBarPanel.add(toolBar, BorderLayout.NORTH);

        // add uiPanel
        toolBarPanel.add(uiPanel());

        // add toolbar to mainFrame
        mainFrame.add(toolBarPanel);
    }


    /**
     * Create main panel for DV program
     * @return main panel for DV
     */
    public JPanel uiPanel()
    {
        // create main panel for program
        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();

        // initial graph
        XYSeriesCollection data = new XYSeriesCollection();
        JFreeChart chart = ChartFactory.createXYLineChart("", "", "", data);
        XYPlot plot = (XYPlot) chart.getPlot();
        plot.setDrawingSupplier(new DefaultDrawingSupplier(
                new Paint[] { Color.RED },
                DefaultDrawingSupplier.DEFAULT_OUTLINE_PAINT_SEQUENCE,
                DefaultDrawingSupplier.DEFAULT_STROKE_SEQUENCE,
                DefaultDrawingSupplier.DEFAULT_OUTLINE_STROKE_SEQUENCE,
                DefaultDrawingSupplier.DEFAULT_SHAPE_SEQUENCE));
        plot.getRangeAxis().setVisible(false);
        plot.getDomainAxis().setVisible(false);
        plot.setRangeGridlinesVisible(false);
        chart.removeLegend();
        chart.setBorderVisible(false);
        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(Resolutions.chartPanel[0], Resolutions.chartPanel[1]));

        graphPanel.setLayout(new BoxLayout(graphPanel, BoxLayout.Y_AXIS));
        graphPanel.add(chartPanel);

        graphPane = new JScrollPane(graphPanel);

        graphPane.setAlignmentX(Component.CENTER_ALIGNMENT);
        graphPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        constraints.weightx = 0.7;
        constraints.gridx = 0;
        constraints.gridy = 0;

        graphDomainPanel.setLayout(new BoxLayout(graphDomainPanel, BoxLayout.Y_AXIS));
        graphDomainPanel.setPreferredSize(new Dimension(Resolutions.graphDomainPanel[0], Resolutions.graphDomainPanel[1]));

        graphDomainPanel.add(graphPane);

        // create domain slider
        JPanel domainSliderPanel = new JPanel();

        // set colors minimum and maximum of slider
        domainSlider = new RangeSlider(Color.ORANGE, Color.LIGHT_GRAY, Color.DARK_GRAY);
        domainSlider.setMinimum(0);
        domainSlider.setMaximum(400);
        domainSlider.setMajorTickSpacing(1);
        domainSlider.setValue(0);
        domainSlider.setUpperValue(400);
        domainSlider.setToolTipText("Control visible range of graph");

        // Add listeners to update display
        domainSlider.addMouseMotionListener(new MouseMotionListener()
        {
            @Override
            public void mouseDragged(MouseEvent e)
            {
                RangeSlider slider = (RangeSlider) e.getSource();
                domainArea[0] = (slider.getValue() - 200) * fieldLength / 200.0;
                domainArea[1] = (slider.getUpperValue() - 200) * fieldLength / 200.0;

                DataVisualization.drawGraphs(2);
                repaint();
                revalidate();
            }

            @Override
            public void mouseMoved(MouseEvent e) {}
        });

        domainSlider.addMouseListener(new MouseListener()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                DataVisualization.drawGraphs(2);
                repaint();
                revalidate();
            }

            @Override
            public void mousePressed(MouseEvent e) {}

            @Override
            public void mouseReleased(MouseEvent e)
            {
                DataVisualization.drawGraphs(0);
                repaint();
                revalidate();
            }

            @Override
            public void mouseEntered(MouseEvent e) {}

            @Override
            public void mouseExited(MouseEvent e) {}
        });

        // set preferred size and alignment
        domainSlider.setPreferredSize(new Dimension(Resolutions.domainSlider[0], Resolutions.domainSlider[1]));
        domainSlider.setAlignmentX(Component.CENTER_ALIGNMENT);

        // add to panels
        domainSliderPanel.add(domainSlider);
        graphDomainPanel.add(domainSliderPanel);

        // add label
        JPanel domainSliderLabel = new JPanel();
        domainSliderLabel.add(new JLabel("Subset of Utilized Data for All Classes"));
        domainSliderLabel.setToolTipText("Control visible range of graph");
        graphDomainPanel.add(domainSliderLabel);

        // create overlap slider
        JPanel overlapSliderPanel = new JPanel();

        // set colors and minimum and maximum of slider
        overlapSlider = new RangeSlider(Color.ORANGE, Color.LIGHT_GRAY, Color.DARK_GRAY);
        overlapSlider.setMinimum(0);
        overlapSlider.setMaximum(400);
        overlapSlider.setMajorTickSpacing(1);
        overlapSlider.setValue(0);
        overlapSlider.setUpperValue(400);
        overlapSlider.setToolTipText("Control overlap area of graph");

        // Add listener to update display
        overlapSlider.addMouseMotionListener(new MouseMotionListener()
        {
            @Override
            public void mouseDragged(MouseEvent e)
            {
                RangeSlider slider = (RangeSlider) e.getSource();
                overlapArea[0] = (slider.getValue() - 200) * fieldLength / 200.0;
                overlapArea[1] = (slider.getUpperValue() - 200) * fieldLength / 200.0;

                DataVisualization.drawGraphs(3);
                repaint();
                revalidate();
            }

            @Override
            public void mouseMoved(MouseEvent e) {}
        });

        overlapSlider.addMouseListener(new MouseListener()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                DataVisualization.drawGraphs(3);
                repaint();
                revalidate();
            }

            @Override
            public void mousePressed(MouseEvent e) {}

            @Override
            public void mouseReleased(MouseEvent e)
            {
                DataVisualization.drawGraphs(0);
                repaint();
                revalidate();

            }

            @Override
            public void mouseEntered(MouseEvent e) {}

            @Override
            public void mouseExited(MouseEvent e) {}
        });

        // set preferred size and alignment
        overlapSlider.setPreferredSize(new Dimension(Resolutions.domainSlider[0], Resolutions.domainSlider[1]));
        overlapSlider.setAlignmentX(Component.CENTER_ALIGNMENT);

        // add to panels
        overlapSliderPanel.add(overlapSlider);
        graphDomainPanel.add(overlapSliderPanel);

        // add label
        JPanel overlapSliderLabel = new JPanel();
        overlapSliderLabel.add(new JLabel("Overlap Area for All Classes"));
        overlapSliderLabel.setToolTipText("Control overlap Area of graph");
        graphDomainPanel.add(overlapSliderLabel);

        // create threshold slider
        JPanel thresholdSliderPanel = new JPanel();
        thresholdSlider = new JSlider()
        {
            @Override
            public void updateUI()
            {
                setUI(new CustomSliderUI(this));
            }
        };

        // set minimum and maximum of slider
        thresholdSlider.setMinimum(0);
        thresholdSlider.setMaximum(400);
        thresholdSlider.setMajorTickSpacing(1);
        thresholdSlider.setValue(200);
        thresholdSlider.setToolTipText("Change threshold value for visualization");

        // Add listener to update display
        thresholdSlider.addMouseMotionListener(new MouseMotionListener()
        {
            @Override
            public void mouseDragged(MouseEvent e) {
                JSlider slider = (JSlider) e.getSource();

                threshold = (slider.getValue() - 200) * fieldLength / 200.0;

                DataVisualization.drawGraphs(1);
                repaint();
                revalidate();
            }

            @Override
            public void mouseMoved(MouseEvent e) {}
        });

        thresholdSlider.addMouseListener(new MouseListener()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                DataVisualization.drawGraphs(1);
                repaint();
                revalidate();
            }

            @Override
            public void mousePressed(MouseEvent e) {}

            @Override
            public void mouseReleased(MouseEvent e)
            {
                DataVisualization.drawGraphs(0);
                repaint();
                revalidate();
            }

            @Override
            public void mouseEntered(MouseEvent e) {}

            @Override
            public void mouseExited(MouseEvent e) {}
        });

        // set preferred size and alignment
        thresholdSlider.setPreferredSize(new Dimension(Resolutions.domainSlider[0], Resolutions.domainSlider[1]));
        thresholdSlider.setAlignmentX(Component.CENTER_ALIGNMENT);

        // add to panels
        thresholdSliderPanel.add(thresholdSlider);
        graphDomainPanel.add(thresholdSliderPanel);

        // add label
        JPanel thresholdSliderLabel = new JPanel();
        thresholdSliderLabel.add(new JLabel("Control for Threshold Interval for Visualization"));
        thresholdSliderLabel.setToolTipText("Change threshold value for visualization");
        graphDomainPanel.add(thresholdSliderLabel);

        // finalize domain panel
        mainPanel.add(graphDomainPanel, constraints);

        // create angles scroll pane
        sliderPanel = new JPanel(new GridLayout(1, 0));
        sliderPanel.setPreferredSize(new Dimension(Resolutions.sliderPanel[0], Resolutions.sliderPanel[1]));
        anglesPane = new JScrollPane(sliderPanel);
        anglesPane.setPreferredSize(new Dimension(Resolutions.anglesPane[0], Resolutions.anglesPane[1]));
        constraints.weightx = 0.3;
        constraints.gridx = 1;
        constraints.gridy = 0;

        mainPanel.add(anglesPane, constraints);

        // create confusion matrix panel
        confusionMatrixPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        confusionMatrixPanel.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
        confusionMatrixPanel.add(allDataCM);
        confusionMatrixPanel.add(dataWithoutOverlapCM);
        confusionMatrixPanel.add(overlapCM);
        confusionMatrixPanel.add(worstCaseCM);

        // create confusion matrix pane
        confusionMatrixPane = new JScrollPane(confusionMatrixPanel);
        confusionMatrixPane.setPreferredSize(new Dimension(Resolutions.confusionMatrixPane[0], Resolutions.confusionMatrixPane[1]));
        confusionMatrixPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        constraints.weightx = 1;
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.gridwidth = 2;
        mainPanel.add(confusionMatrixPane, constraints);

        return mainPanel;
    }


    /**
     * Asks user questions about data then creates project
     */
    private void createNewProject() {
        // check for ID column
        int choice = JOptionPane.showConfirmDialog(
                mainFrame,
                "Does this project use the first column to designate ID?",
                "ID Column",
                JOptionPane.YES_NO_OPTION);

        if (choice == 0) hasID = true;
        else if (choice == -1) return;

        // check for class column
        choice = JOptionPane.showConfirmDialog(
                mainFrame,
                "Does this project use the last column to designate classes?",
                "Classes",
                JOptionPane.YES_NO_OPTION);

        if (choice == 0) hasClasses = true;
        else if (choice == -1) return;

        // buttons for JOptionPane
        Object[] normStyleButtons = { "z-Score Min-Max", "Min-Max", "Help" };
        boolean notChosen = true;

        // ask for normalization style and repeat if user asks for help
        while (notChosen)
        {
            choice = JOptionPane.showOptionDialog(mainFrame,
                    "Choose a normalization Style or click " +
                            "\"Help\" for more information on normalization styles.",
                    "Normalization Style",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    normStyleButtons,
                    normStyleButtons[0]);

            switch (choice)
            {
                case 0 ->
                        {
                            zScoreMinMax = true;
                            notChosen = false;
                        }
                case 1 -> notChosen = false;
                case 2 -> normalizationInfoPopup();
                default -> { return; }
            }
        }

        // set filter on file chooser
        JFileChooser fileDialog = new JFileChooser();
        fileDialog.setFileFilter(new FileNameExtensionFilter("csv", "csv"));

        // open file dialog
        if (fileDialog.showOpenDialog(mainFrame) == JFileChooser.APPROVE_OPTION)
        {
            File dataFile = fileDialog.getSelectedFile();

            // reset DV project
            resetProgram();

            // parse data from file into classes
            DataParser.parseData(dataFile);

            DataVisualization.optimizeSetup();
            sliderPanel.setPreferredSize(new Dimension(Resolutions.sliderPanel[0], (100 * fieldLength)));

            DataVisualization.drawGraphs(0);
        }
        else
        {
            JOptionPane.showMessageDialog(
                    mainFrame,
                    "Could not open file.\n Please ensure view the \"Help\" tab for additional information.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }

        // update angles then redraw
        DataVisualization.updateAngles();
        repaint();
        revalidate();
    }


    /**
     * Imports new data into current project
     */
    private void importData()
    {

    }


    /**
     * Opens saved project
     */
    private void openSavedProject()
    {

    }


    /** Saves project
     * Note: project must already have a save
     */
    private void saveProject()
    {

    }


    /**
     * Creates new save of project
     */
    private void saveProjectAs()
    {

    }


    /**
     * Creates informative popup explaining Min-Max normalization
     * and z-Score Min-Max normalization
     */
    private void normalizationInfoPopup()
    {
        JOptionPane.showMessageDialog(
                mainFrame,
                "Min-Max Normalization - normalizes data linearly " +
                        "from [0,1] by subtracting the minimum and dividing by the range.\n" +
                        "z-Score Min-Max Normalization - performs a standardization by subtracting " +
                        "the mean and dividing by the standard deviation before performing a Min-Max normalization.",
                "Normalization Help",
                JOptionPane.INFORMATION_MESSAGE
        );
    }


    /**
     * Resets DV program
     */
    private void resetProgram()
    {
        // reset data
        data.clear();
        originalData.clear();
        allClasses.clear();
        uniqueClasses.clear();

        // reset panels
        sliderPanel.removeAll();
        graphPanel.removeAll();
    }
}
