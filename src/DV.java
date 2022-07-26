import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

import Sliders.ThresholdSliderUI;
import Sliders.RangeSlider;
import Sliders.RangeSliderUI;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.DefaultDrawingSupplier;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeriesCollection;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.mariuszgromada.math.mxparser.Argument;
import org.mariuszgromada.math.mxparser.Expression;
import org.mariuszgromada.math.mxparser.Function;

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
    static JPanel angleSliderPanel;
    static JPanel confusionMatrixPanel;
    static JPanel crossValidationPanel;
    static JPanel analyticsPanel;
    static JPanel graphPanel;
    static JPanel sliderPanel;

    // scroll areas
    JScrollPane graphPane;
    JScrollPane anglesPane;
    JScrollPane analyticsPane;

    // main frame for DV
    static JFrame mainFrame;

    /**************************************************
     * FOR GRAPHS
     *************************************************/
    // line colors
    static Color domainLines = Color.BLACK;
    static Color overlapLines = Color.ORANGE;
    static Color thresholdLine = Color.GREEN;
    static Color background = Color.WHITE;

    // graph colors
    static Color[] graphColors = new Color[] {
            new Color(	147, 112, 219),   // upper graph (purple)
            new Color(84, 133, 145)       // lower graph (dark cyan)
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

    // warn user about scaling
    static boolean showPopup;

    /**************************************************
     * FOR ANALYTICS
     *************************************************/
    // overlap area
    static double[] overlapArea;

    // threshold point
    static double threshold;

    // threshold before optimizing
    static double prevThreshold;

    // true if upper class has lower mean
    static boolean upperIsLower = true;

    // current accuracy
    static double accuracy;

    // current all data confusion matrix
    static String allDataCM;

    // previous all data confusion matrices (only applicable if 3+ classes)
    static ArrayList<String> prevAllDataCM;

    // current all data correct and total
    static int[] allDataClassifications;

    // previous all data correct and total
    static ArrayList<int[]> prevAllDataClassifications;

    // display analytics
    static boolean prevAllDataChecked = true;
    static boolean allDataChecked = true;
    static boolean withoutOverlapChecked = false;
    static boolean overlapChecked = false;
    static boolean worstCaseChecked = false;
    static boolean userValidationChecked = true;
    static boolean userValidationImported = false;
    static boolean crossValidationChecked = true;
    static boolean crossValidationNotGenerated = true;

    // number of folds for k-fold cross validation
    static int kFolds = 10;

    /************************************************
     * FOR INPUT DATA
     ***********************************************/
    // input data info
    static boolean hasID;
    static boolean hasClasses;

    // min-max or zScore min-max normalization
    static boolean zScoreMinMax;

    /************************************************
     * FOR DATA
     ***********************************************/
    // angles and initial angles (store angles before optimizing)
    static double[] angles;
    static double[] prevAngles;

    // normalized and original data
    static ArrayList<DataObject> data;
    static ArrayList<DataObject> normalizedData;
    static ArrayList<DataObject> originalData;

    // user made validation data
    static ArrayList<DataObject> validationData;

    // classes for data
    static ArrayList<String> uniqueClasses;
    static int classNumber;

    // fieldnames and length
    static ArrayList<String> fieldNames;
    static int fieldLength;

    // initialize with linear function
    static String function = "f(x) = x";
    static Function dataFunction = new Function("f(x) = x");

    /************************************************
     * FOR PROJECT
     ***********************************************/
    // name of project (if saved)
    static String projectSaveName;


    /**
     * Main handler for UI
     * Creates main panel, menu bar, and toolbar.
     */
    public DV()
    {
        // set DV properties
        super("DV");
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
        // keyboard shortcut: alt + f
        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);
        menuBar.add(fileMenu);

        // file menu item: create new project
        // keyboard shortcut: alt + n
        JMenuItem newProjItem = new JMenuItem("Create New Project");
        newProjItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.ALT_DOWN_MASK));
        newProjItem.addActionListener(e -> createNewProject());
        fileMenu.add(newProjItem);

        // file menu item: open saved project
        // keyboard shortcut: alt + o
        JMenuItem openSavedItem = new JMenuItem("Open Saved Project");
        openSavedItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.ALT_DOWN_MASK));
        openSavedItem.addActionListener(e -> openSavedProject());
        fileMenu.add(openSavedItem);

        // file menu item: save project
        // keyboard shortcut: alt + s
        JMenuItem saveProjItem = new JMenuItem("Save Project");
        saveProjItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.ALT_DOWN_MASK));
        saveProjItem.addActionListener(e -> saveProject());
        fileMenu.add(saveProjItem);

        // file menu item: save project as
        // keyboard shortcut: alt + a
        JMenuItem saveProjAsItem = new JMenuItem("Save Project As");
        saveProjAsItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.ALT_DOWN_MASK));
        saveProjAsItem.addActionListener(e -> saveProjectAs());
        fileMenu.add(saveProjAsItem);

        // file menu item: import data
        // keyboard shortcut: alt + i
        JMenuItem importDataItem = new JMenuItem("Import Data");
        importDataItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I, InputEvent.ALT_DOWN_MASK));
        importDataItem.addActionListener(e -> importData());
        fileMenu.add(importDataItem);

        // file menu item: validation data
        // keyboard shortcut: alt + v
        JMenuItem validationDataItem = new JMenuItem("Validation Data");
        validationDataItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.ALT_DOWN_MASK));
        validationDataItem.addActionListener(e -> createUserValidationSet());
        fileMenu.add(validationDataItem);

        // help menu
        // keyboard shortcut: alt + h
        JMenu helpMenu = new JMenu("Help");
        helpMenu.setMnemonic(KeyEvent.VK_H);
        menuBar.add(helpMenu);

        // help menu item: open manual
        // keyboard shortcut: alt + m
        JMenuItem manualItem = new JMenuItem("User Manual");
        manualItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_M, InputEvent.ALT_DOWN_MASK));
        manualItem.addActionListener(e ->
        {
            // try opening DVManual
            try
            {
               Desktop.getDesktop().open(new File("documentation\\user\\DV_User_Manual.pdf"));
            }
            catch (IOException | IllegalArgumentException ioe)
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
        // keyboard shortcut: alt + z
        JMenuItem normalizationInfoItem = new JMenuItem("Normalization Info");
        normalizationInfoItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.ALT_DOWN_MASK));
        normalizationInfoItem.addActionListener(e -> normalizationInfoPopup());
        helpMenu.add(normalizationInfoItem);

        // help menu item: code help menu
        // keyboard shortcut: N/A
        JMenu codeHelpMenu = new JMenu("Code");
        codeHelpMenu.setMnemonic(KeyEvent.VK_E);
        helpMenu.add(codeHelpMenu);

        // code help menu item: UML diagram
        // keyboard shortcut: alt + u
        JMenuItem umlItem = new JMenuItem("UML Diagram");
        umlItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_U, InputEvent.ALT_DOWN_MASK));
        umlItem.addActionListener(e ->
        {
            // try opening DV_UML
            try
            {
                Desktop.getDesktop().open(new File("documentation\\code\\DV_UML.png"));
            }
            catch (IOException | IllegalArgumentException ioe)
            {
                JOptionPane.showMessageDialog(
                        mainFrame,
                        "Error opening UML diagram.\n" +
                                ioe,
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        });
        codeHelpMenu.add(umlItem);

        // code help menu: UML descriptions
        // keyboard shortcut: alt + l
        JMenuItem umlDescriptionItem = new JMenuItem("UML Descriptions");
        umlDescriptionItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.ALT_DOWN_MASK));
        umlDescriptionItem.addActionListener(e ->
        {
            // try opening DV_UML_Descriptions
            try
            {
                Desktop.getDesktop().open(new File("documentation\\code\\DV_UML_Descriptions.pdf"));
            }
            catch (IOException | IllegalArgumentException ioe)
            {
                JOptionPane.showMessageDialog(
                        mainFrame,
                        "Error opening UML descriptions.\n" +
                                ioe,
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        });
        codeHelpMenu.add(umlDescriptionItem);
    }


    /**
     * Creates toolbar for DV Program
     */
    private void createToolBar()
    {
        // create toolbar
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.setRollover(true);

        // colors options
        JButton colorOptionsBtn = new JButton("Color Options");
        colorOptionsBtn.setToolTipText("Open the color options menu");
        colorOptionsBtn.addActionListener(e -> new ColorOptionsMenu(MouseInfo.getPointerInfo().getLocation()));
        toolBar.addSeparator();
        toolBar.add(colorOptionsBtn);
        toolBar.addSeparator();

        // visualization options
        JButton visOptionsBtn = new JButton("Visualization Options");
        visOptionsBtn.setToolTipText("Open the visualization options menu");
        visOptionsBtn.addActionListener(e -> new VisualizationOptionsMenu(MouseInfo.getPointerInfo().getLocation()));
        toolBar.add(visOptionsBtn);
        toolBar.addSeparator();

        // confusion matrix options
        JButton analyticsBtn = new JButton("Analytic Options");
        analyticsBtn.setToolTipText("Open the analytics options menu");
        analyticsBtn.addActionListener(e -> new AnalyticsMenu(MouseInfo.getPointerInfo().getLocation()));
        toolBar.add(analyticsBtn);
        toolBar.addSeparator();

        // resets screen
        JButton resetScreenBtn = new JButton("Reset Screen");
        resetScreenBtn.setToolTipText("Resets rendered zoom area");
        resetScreenBtn.addActionListener(e ->
        {
            if (data != null)
            {
                DataVisualization.drawGraphs();
                repaint();
                revalidate();
            }
        });
        toolBar.add(resetScreenBtn);
        toolBar.addSeparator();

        // optimize visualization
        JButton optimizeBtn = new JButton("Optimize Visualization");
        optimizeBtn.setToolTipText("Attempts to optimize angles and threshold");
        optimizeBtn.addActionListener(e -> DataVisualization.optimizeVisualization());
        toolBar.add(optimizeBtn);
        toolBar.addSeparator();

        // undo optimization
        JButton undoOptimizeBtn = new JButton("Undo Optimization");
        undoOptimizeBtn.setToolTipText("Reverses previous optimization operation");
        undoOptimizeBtn.addActionListener(e -> DataVisualization.undoOptimization());
        toolBar.add(undoOptimizeBtn);
        toolBar.addSeparator();

        // toggle bar-line
        JButton barLineBtn = new JButton("Toggle Bar-line");
        barLineBtn.setToolTipText("Toggle for showing bar-line graph of endpoint placement");
        barLineBtn.addActionListener(e ->
        {
            if (data != null)
            {
                showBars = !showBars;
                DataVisualization.drawGraphs();
            }
        });
        toolBar.add(barLineBtn);
        toolBar.addSeparator();

        // set toolbar north
        JPanel toolBarPanel = new JPanel(new BorderLayout());
        toolBarPanel.setVisible(true);
        toolBarPanel.add(toolBar, BorderLayout.NORTH);

        // add uiPanel to toolbar
        toolBarPanel.add(uiPanel());

        // add toolbar to mainFrame
        mainFrame.add(toolBarPanel);
        mainFrame.pack();

        // add icons
        int offset = colorOptionsBtn.getInsets().top + colorOptionsBtn.getInsets().bottom;
        colorOptionsBtn.setIcon(resizeIcon(new ImageIcon("source\\icons\\color-palette.png"), colorOptionsBtn.getHeight() - offset, colorOptionsBtn.getHeight() - offset));
        visOptionsBtn.setIcon(resizeIcon(new ImageIcon("source\\icons\\line-chart.png"), visOptionsBtn.getHeight() - offset, visOptionsBtn.getHeight() - offset));
        analyticsBtn.setIcon(resizeIcon(new ImageIcon("source\\icons\\statistics.png"), analyticsBtn.getHeight() - offset, analyticsBtn.getHeight() - offset));
        resetScreenBtn.setIcon(resizeIcon(new ImageIcon("source\\icons\\undo.png"), resetScreenBtn.getHeight() - offset, resetScreenBtn.getHeight() - offset));
        optimizeBtn.setIcon(resizeIcon(new ImageIcon("source\\icons\\up-right-arrow.png"), optimizeBtn.getHeight() - offset, optimizeBtn.getHeight() - offset));
        undoOptimizeBtn.setIcon(resizeIcon(new ImageIcon("source\\icons\\revert.png"), undoOptimizeBtn.getHeight() - offset, undoOptimizeBtn.getHeight() - offset));
        barLineBtn.setIcon(resizeIcon(new ImageIcon("source\\icons\\bar-graph.png"), barLineBtn.getHeight() - offset, barLineBtn.getHeight() - offset));
    }


    /**
     * Create main panel for DV program
     * @return main panel for DV
     */
    private JPanel uiPanel()
    {
        // create main panel for program
        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();

        // set layout
        graphPanel = new JPanel();
        graphPanel.setLayout(new BoxLayout(graphPanel, BoxLayout.Y_AXIS));

        // add blank graph
        graphPanel.add(blankGraph());

        graphPane = new JScrollPane(graphPanel);

        // center graph
        graphPane.setAlignmentX(Component.CENTER_ALIGNMENT);
        graphPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        // set layout and size
        sliderPanel = new JPanel();
        sliderPanel.setLayout(new BoxLayout(sliderPanel, BoxLayout.Y_AXIS));
        sliderPanel.setPreferredSize(new Dimension(Resolutions.sliderPanel[0], Resolutions.sliderPanel[1]));

        sliderPanel.add(graphPane);

        // create domain slider
        JPanel domainSliderPanel = new JPanel();

        // set colors minimum and maximum of slider
        domainSlider = new RangeSlider()
        {
            @Override
            public void updateUI()
            {
                setUI(new RangeSliderUI(this, Color.BLACK, Color.GRAY, Color.DARK_GRAY));
                updateLabelUIs();
            }
        };
        domainSlider.setMinimum(0);
        domainSlider.setMaximum(400);
        domainSlider.setMajorTickSpacing(1);
        domainSlider.setValue(0);
        domainSlider.setUpperValue(400);
        domainSlider.setToolTipText("Control visible range of graph");

        // set preferred size and alignment
        domainSlider.setPreferredSize(new Dimension(Resolutions.domainSlider[0], Resolutions.domainSlider[1]));
        domainSlider.setAlignmentX(Component.CENTER_ALIGNMENT);

        // add to panels
        domainSliderPanel.add(domainSlider);
        sliderPanel.add(domainSliderPanel);

        // add label
        JPanel domainSliderLabel = new JPanel();
        domainSliderLabel.add(new JLabel("Setting up Subset of Utilized Data for All Classes"));
        domainSliderLabel.setToolTipText("Control visible range of graph");
        sliderPanel.add(domainSliderLabel);

        // create overlap slider
        JPanel overlapSliderPanel = new JPanel();

        // set colors and minimum and maximum of slider
        overlapSlider = new RangeSlider()
        {
            @Override
            public void updateUI()
            {
                setUI(new RangeSliderUI(this, Color.ORANGE, new Color(255, 165, 0), new Color(215,107,0)));
                updateLabelUIs();
            }
        };
        overlapSlider.setMinimum(0);
        overlapSlider.setMaximum(400);
        overlapSlider.setMajorTickSpacing(1);
        overlapSlider.setValue(0);
        overlapSlider.setUpperValue(400);
        overlapSlider.setToolTipText("Control overlap area of graph");

        // set preferred size and alignment
        overlapSlider.setPreferredSize(new Dimension(Resolutions.domainSlider[0], Resolutions.domainSlider[1]));
        overlapSlider.setAlignmentX(Component.CENTER_ALIGNMENT);

        // add to panels
        overlapSliderPanel.add(overlapSlider);
        sliderPanel.add(overlapSliderPanel);

        // add label
        JPanel overlapSliderLabel = new JPanel();
        overlapSliderLabel.add(new JLabel("Setting up Overlap Area for All Classes"));
        overlapSliderLabel.setToolTipText("Control overlap Area of graph");
        sliderPanel.add(overlapSliderLabel);

        // create threshold slider
        JPanel thresholdSliderPanel = new JPanel();
        thresholdSlider = new JSlider()
        {
            @Override
            public void updateUI()
            {
                setUI(new ThresholdSliderUI(this));
            }
        };

        // set minimum and maximum of slider
        thresholdSlider.setMinimum(0);
        thresholdSlider.setMaximum(400);
        thresholdSlider.setMajorTickSpacing(1);
        thresholdSlider.setValue(200);
        thresholdSlider.setToolTipText("Change threshold value for visualization");

        // set preferred size and alignment
        thresholdSlider.setPreferredSize(new Dimension(Resolutions.domainSlider[0], Resolutions.domainSlider[1]));
        thresholdSlider.setAlignmentX(Component.CENTER_ALIGNMENT);

        // add to panels
        thresholdSliderPanel.add(thresholdSlider);
        sliderPanel.add(thresholdSliderPanel);

        // add label
        JPanel thresholdSliderLabel = new JPanel();
        thresholdSliderLabel.add(new JLabel("Setting up Area of the Class Threshold"));
        thresholdSliderLabel.setToolTipText("Change threshold value for visualization");
        sliderPanel.add(thresholdSliderLabel);

        // finalize domain panel
        constraints.weightx = 0.7;
        constraints.gridx = 0;
        constraints.gridy = 0;
        mainPanel.add(sliderPanel, constraints);

        // create angles scroll pane
        angleSliderPanel = new JPanel(new GridLayout(1, 0));
        angleSliderPanel.setPreferredSize(new Dimension(Resolutions.angleSliderPanel[0], Resolutions.angleSliderPanel[1]));
        anglesPane = new JScrollPane(angleSliderPanel);
        anglesPane.setPreferredSize(new Dimension(Resolutions.anglesPane[0], Resolutions.anglesPane[1]));

        constraints.weightx = 0.3;
        constraints.gridx = 1;
        constraints.gridy = 0;
        mainPanel.add(anglesPane, constraints);

        // create confusion matrix and cross validation panels
        confusionMatrixPanel = new JPanel(new GridLayout(0, 4, 5, 5));
        crossValidationPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        // create analytics panel
        analyticsPanel = new JPanel();
        analyticsPanel.setLayout(new BoxLayout(analyticsPanel, BoxLayout.Y_AXIS));
        analyticsPanel.add(confusionMatrixPanel);
        analyticsPanel.add(crossValidationPanel);

        // create confusion matrix pane
        analyticsPane = new JScrollPane(analyticsPanel);
        analyticsPane.setPreferredSize(new Dimension(Resolutions.analyticsPane[0], Resolutions.analyticsPane[1]));
        analyticsPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        constraints.weightx = 1;
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.gridwidth = 2;
        mainPanel.add(analyticsPane, constraints);

        // add control panel to mainPanel
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel mouseControls = new JLabel("UI Controls: ");
        mouseControls.setFont(mouseControls.getFont().deriveFont(Font.BOLD, 12f));
        controlPanel.add(mouseControls);

        JLabel controls = new JLabel("panning = ctrl + hold left mouse button, " +
                "zooming = scroll wheel, " +
                "unequal zoom in selected rectangle = hold left mouse button + drag down and right, " +
                "unequal zoom out = hold left mouse button + drag left or up");
        controls.setFont(controls.getFont().deriveFont(12f));

        controlPanel.add(controls);
        controlPanel.setPreferredSize(new Dimension(Resolutions.uiControlsPanel[0], Resolutions.uiControlsPanel[1]));

        constraints.weightx = 1;
        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.gridwidth = 2;
        constraints.insets = new Insets(Resolutions.uiControlsPanel[2], Resolutions.uiControlsPanel[2], 0, 0);
        constraints.anchor = GridBagConstraints.WEST;
        mainPanel.add(controlPanel, constraints);

        return mainPanel;
    }


    /**
     * Resizes icon to specified width and height
     * @param img_icon Icon to be resized
     * @param width New width for img_icon
     * @param height New height for img_icon
     * @return Resized icon
     */
    private Icon resizeIcon(ImageIcon img_icon, int width, int height)
    {
        Image img = img_icon.getImage();
        Image resized = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);

        return new ImageIcon(resized);
    }


    /**
     * Creates blank graph
     */
    private ChartPanel blankGraph()
    {
        // create blank graph
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
        plot.setDomainPannable(true);
        plot.setRangePannable(true);
        plot.setBackgroundPaint(background);
        plot.setDomainGridlinePaint(Color.GRAY);
        chart.removeLegend();
        chart.setBorderVisible(false);
        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setMouseWheelEnabled(true);
        chartPanel.setPreferredSize(new Dimension(Resolutions.chartPanel[0], Resolutions.chartPanel[1]));

        return chartPanel;
    }


    /**
     * Asks user questions about data then creates project
     */
    private void createNewProject()
    {
        try
        {
            // check for ID column
            int choice = JOptionPane.showConfirmDialog(
                    mainFrame,
                    "Does this project use the first column to designate ID?",
                    "ID Column",
                    JOptionPane.YES_NO_OPTION);

            if (choice == 0) hasID = true;
            else if (choice == 1) hasID = false;
            else if (choice == -1) return;

            // check for class column
            choice = JOptionPane.showConfirmDialog(
                    mainFrame,
                    "Does this project use the last column to designate classes?",
                    "Classes",
                    JOptionPane.YES_NO_OPTION);

            if (choice == 0) hasClasses = true;
            else if (choice == 1) hasClasses = false;
            else if (choice == -1) return;

            // buttons for JOptionPane
            Object[] normStyleButtons = { "z-Score Min-Max", "Min-Max", "Help" };
            boolean notChosen = true;

            // ask for normalization style and repeat if user asks for help
            while (notChosen)
            {
                choice = JOptionPane.showOptionDialog(mainFrame,
                        "Choose a normalization style or click " +
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
                    case 1 ->
                    {
                        zScoreMinMax = false;
                        notChosen = false;
                    }
                    case 2 -> normalizationInfoPopup();
                    default -> { return; }
                }
            }

            // set filter on file chooser
            JFileChooser fileDialog = new JFileChooser();
            fileDialog.setFileFilter(new FileNameExtensionFilter("csv", "csv"));

            // set to current directory
            File workingDirectory = new File(System.getProperty("user.dir"));
            fileDialog.setCurrentDirectory(workingDirectory);

            // open file dialog
            int results = fileDialog.showOpenDialog(mainFrame);

            if (results == JFileChooser.APPROVE_OPTION)
            {
                File dataFile = fileDialog.getSelectedFile();

                // reset program
                resetProgram(true);

                // parse data from file into classes
                boolean success = DataSetup.setupWithData(dataFile);

                // create graphs
                if (success)
                {
                    // optimize data setup with Linear Discriminant Analysis
                    DataVisualization.optimizeSetup();
                    angleSliderPanel.setPreferredSize(new Dimension(Resolutions.angleSliderPanel[0], (100 * fieldLength)));

                    DataVisualization.drawGraphs();
                }
                else
                {
                    JOptionPane.showMessageDialog(
                            mainFrame,
                            "Please ensure the file is properly formatted.\nFor additional information, please view the \"Help\" tab.",
                            "Error: could not open file",
                            JOptionPane.ERROR_MESSAGE);

                    // add blank graph
                    graphPanel.add(blankGraph());
                }
            }
            else if (results != JFileChooser.CANCEL_OPTION)
            {
                JOptionPane.showMessageDialog(
                        mainFrame,
                        "Please ensure the file is properly formatted.\nFor additional information, please view the \"Help\" tab.",
                        "Error: could not open file",
                        JOptionPane.ERROR_MESSAGE);

                // add blank graph
                graphPanel.add(blankGraph());
            }

            // repaint and revalidate DV
            repaint();
            revalidate();
        }
        catch (Exception e)
        {
            JOptionPane.showMessageDialog(
                    mainFrame,
                    "Please ensure the file is properly formatted.\nFor additional information, please view the \"Help\" tab.",
                    "Error: could not open file",
                    JOptionPane.ERROR_MESSAGE);

            // add blank graph if data was bad
            graphPanel.add(blankGraph());

            // repaint and revalidate DV
            repaint();
            revalidate();
        }
    }


    /**
     * Imports new data into current project
     */
    private void importData()
    {
        try
        {
            if (data.size() > 0)
            {
                // set filter on file chooser
                JFileChooser fileDialog = new JFileChooser();
                fileDialog.setFileFilter(new FileNameExtensionFilter("csv", "csv"));

                // set to current directory
                File workingDirectory = new File(System.getProperty("user.dir"));
                fileDialog.setCurrentDirectory(workingDirectory);

                // open file dialog
                int results = fileDialog.showOpenDialog(mainFrame);

                if (results == JFileChooser.APPROVE_OPTION)
                {
                    File importFile = fileDialog.getSelectedFile();

                    // reset program
                    resetProgram(false);

                    // check if import was successful
                    boolean success = DataSetup.setupImportData(importFile);

                    // create graphs
                    if (success)
                    {
                        // optimize data setup with Linear Discriminant Analysis
                        DataVisualization.optimizeSetup();
                        angleSliderPanel.setPreferredSize(new Dimension(Resolutions.angleSliderPanel[0], (100 * fieldLength)));

                        DataVisualization.drawGraphs();
                    }
                    else
                    {
                        // add blank graph
                        JOptionPane.showMessageDialog(
                                mainFrame,
                                "Please ensure the file is properly formatted.\nFor additional information, please view the \"Help\" tab.",
                                "Error: could not open file",
                                JOptionPane.ERROR_MESSAGE);

                        // add blank graph
                        graphPanel.add(blankGraph());
                    }
                }
                else if (results != JFileChooser.CANCEL_OPTION)
                {
                    JOptionPane.showMessageDialog(
                            mainFrame,
                            "Please ensure the file is properly formatted.\nFor additional information, please view the \"Help\" tab.",
                            "Error: could not open file",
                            JOptionPane.ERROR_MESSAGE);

                    // add blank graph
                    graphPanel.add(blankGraph());
                }
            }
            else
            {
                JOptionPane.showMessageDialog(
                        mainFrame,
                        "Please create a project before importing data.\nFor additional information, please view the \"Help\" tab.",
                        "Error: could not import data",
                        JOptionPane.ERROR_MESSAGE);

                // add blank graph
                graphPanel.add(blankGraph());
            }

            // repaint and revalidate graph
            DV.graphPanel.repaint();
            DV.graphPanel.revalidate();
        }
        catch (Exception e)
        {
            JOptionPane.showMessageDialog(
                    mainFrame,
                    "Please ensure the file is properly formatted.\nFor additional information, please view the \"Help\" tab.",
                    "Error: could not open file",
                    JOptionPane.ERROR_MESSAGE);

            // add blank graph
            graphPanel.add(blankGraph());
            DV.graphPanel.repaint();
            DV.graphPanel.revalidate();
        }
    }


    /**
     * Opens saved project
     */
    private void openSavedProject()
    {
        try
        {
            // set filter on file chooser
            JFileChooser fileDialog = new JFileChooser();
            fileDialog.setFileFilter(new FileNameExtensionFilter("csv", "csv"));

            // set to current directory
            File workingDirectory = new File(System.getProperty("user.dir"));
            fileDialog.setCurrentDirectory(workingDirectory);

            // open file dialog
            int results = fileDialog.showOpenDialog(mainFrame);

            if (results == JFileChooser.APPROVE_OPTION)
            {
                File projectFile = fileDialog.getSelectedFile();

                // reset program
                resetProgram(true);

                // check if import was successful
                DataSetup.setupProjectData(projectFile);

                // set vertical scale of graphs
                DataVisualization.verticalScale = classNumber > 1 ? 0.4 : 0.8;

                // create angle sliders
                angleSliderPanel.setPreferredSize(new Dimension(Resolutions.angleSliderPanel[0], (100 * fieldLength)));
                DV.angleSliderPanel.setLayout(new GridLayout(DV.fieldLength, 0));

                for (int i = 0; i < fieldLength; i++)
                    AngleSliders.createSliderPanel(DV.fieldNames.get(i), (int) (DV.angles[i] * 100), i);

                // create graphs
                DataVisualization.drawGraphs();

                // repaint DV
                DV.mainFrame.repaint();
                DV.mainFrame.revalidate();
            }
            else if (results != JFileChooser.CANCEL_OPTION)
            {
                JOptionPane.showMessageDialog(
                        mainFrame,
                        "Please ensure the file is properly formatted.\nFor additional information, please view the \"Help\" tab.",
                        "Error: could not open file",
                        JOptionPane.ERROR_MESSAGE);

                // add blank graph
                graphPanel.add(blankGraph());
                DV.graphPanel.repaint();
                DV.crossValidationPanel.repaint();
                DV.graphPanel.revalidate();
                DV.crossValidationPanel.revalidate();
            }
        }
        catch (Exception e)
        {
            JOptionPane.showMessageDialog(
                    mainFrame,
                    "Please ensure the file is properly formatted.\nFor additional information, please view the \"Help\" tab.",
                    "Error: could not open file",
                    JOptionPane.ERROR_MESSAGE);

            // add blank graph
            graphPanel.add(blankGraph());
            DV.graphPanel.repaint();
            DV.crossValidationPanel.repaint();
            DV.graphPanel.revalidate();
            DV.crossValidationPanel.revalidate();
        }
    }


    /** Saves project
     * Note: project must already have a save
     */
    private void saveProject()
    {
        if (data != null && projectSaveName != null)
        {
            try
            {
                // write to csv file
                Writer out = new FileWriter(projectSaveName, false);

                // save graph colors
                out.write(graphColors[0].getRed() + ",");
                out.write(graphColors[0].getGreen() + ",");
                out.write(graphColors[0].getBlue() + "\n");
                out.write(graphColors[1].getRed() + ",");
                out.write(graphColors[1].getGreen() + ",");
                out.write(graphColors[1].getBlue() + "\n");
                out.write(background.getRed() + ",");
                out.write(background.getGreen() + ",");
                out.write(background.getBlue() + "\n");

                // save line colors
                out.write(domainLines.getRed() + ",");
                out.write(domainLines.getGreen() + ",");
                out.write(domainLines.getBlue() + "\n");
                out.write(overlapLines.getRed() + ",");
                out.write(overlapLines.getGreen() + ",");
                out.write(overlapLines.getBlue() + "\n");
                out.write(thresholdLine.getRed() + ",");
                out.write(thresholdLine.getGreen() + ",");
                out.write(thresholdLine.getBlue() + "\n");

                // save data format
                if (hasID) out.write("1,");
                else out.write("0,");
                if (hasClasses) out.write("1,");
                else out.write("0,");
                if (zScoreMinMax) out.write("1\n");
                else out.write("0\n");

                // save field length
                out.write(fieldLength + "\n");

                // save angles
                for (int i = 0; i < angles.length; i++)
                {
                    if (i != angles.length - 1)
                        out.write(angles[i] + ",");
                    else
                        out.write(angles[i] + "\n");
                }

                // save threshold
                out.write(threshold + "\n");

                // save overlap area
                out.write(overlapArea[0] + ",");
                out.write(overlapArea[1] + "\n");

                // save domain area
                out.write(domainArea[0] + ",");
                out.write(domainArea[1] + "\n");

                // save analytics toggles
                if (prevAllDataChecked) out.write("1,");
                else out.write("0,");
                if (allDataChecked) out.write("1,");
                else out.write("0,");
                if (withoutOverlapChecked) out.write("1,");
                else out.write("0,");
                if (overlapChecked) out.write("1,");
                else out.write("0,");
                if (worstCaseChecked) out.write("1,");
                else out.write("0,");
                if (userValidationChecked) out.write("1,");
                else out.write("0,");
                if (userValidationImported) out.write("1,");
                else out.write("0,");
                if (crossValidationChecked) out.write("1\n");
                else out.write("0\n");

                // are there previous confusion matrices
                if (prevAllDataCM.size() > 0)
                    out.write(prevAllDataCM.size() + "\n");
                else
                    out.write("0\n");

                // save previous confusion matrices
                for (String s : prevAllDataCM)
                {
                    char[] cm = s.toCharArray();

                    for (int j = 0; j < cm.length; j++)
                    {
                        // replace newline character with placeholder
                        if (cm[j] == '\n')
                            cm[j] = '~';
                    }

                    out.write(Arrays.toString(cm) + "\n");
                }

                // save k-folds
                out.write(kFolds + "\n");

                // save number of classes
                out.write(classNumber + "\n");

                // save visualized classes
                out.write(upperClass + "\n");

                for (int i = 0; i < lowerClasses.size(); i++)
                {
                    if (i != lowerClasses.size() - 1)
                    {
                        if (lowerClasses.get(i)) out.write("1,");
                        else out.write("0,");
                    }
                    else
                    {
                        if (lowerClasses.get(i)) out.write("1\n");
                        else out.write("0\n");
                    }
                }

                // save class order
                if (upperIsLower) out.write("1\n");
                else out.write("0\n");

                // save unique classes
                for (int i = 0; i < uniqueClasses.size(); i++)
                {
                    if (i != uniqueClasses.size() - 1)
                        out.write(uniqueClasses.get(i) + ",");
                    else
                        out.write(uniqueClasses.get(i) + "\n");
                }

                // save fieldNames
                for (int i = 0; i < fieldNames.size(); i++)
                {
                    if (i != fieldNames.size() - 1)
                        out.write(fieldNames.get(i) + ",");
                    else
                        out.write(fieldNames.get(i) + "\n");
                }

                // save data
                for (DataObject normData : data)
                {
                    // save number of datapoints
                    out.write(normData.data.length + "\n");

                    for (int j = 0; j < normData.data.length; j++)
                    {
                        for (int k = 0; k < fieldLength; k++)
                        {
                            if (k != fieldLength - 1)
                                out.write(normData.data[j][k] + ",");
                            else
                                out.write(normData.data[j][k] + "\n");
                        }
                    }
                }

                // save original data
                for (DataObject origData : originalData)
                {
                    // save number of datapoints
                    out.write(origData.data.length + "\n");

                    for (int j = 0; j < origData.data.length; j++)
                    {
                        for (int k = 0; k < fieldLength; k++)
                        {
                            if (k != fieldLength - 1)
                                out.write(origData.data[j][k] + ",");
                            else
                                out.write(origData.data[j][k] + "\n");
                        }
                    }
                }


                if (userValidationImported)
                {
                    // save validation data
                    for (DataObject valData : validationData)
                    {
                        // save number of datapoints
                        out.write(valData.data.length + "\n");

                        for (int j = 0; j < valData.data.length; j++)
                        {
                            for (int k = 0; k < fieldLength; k++)
                            {
                                if (k != fieldLength - 1)
                                    out.write(valData.data[j][k] + ",");
                                else
                                    out.write(valData.data[j][k] + "\n");
                            }
                        }
                    }
                }

                // close file
                out.close();
            }
            catch(IOException e)
            {
                e.printStackTrace();
            }
        }
        else if (data == null)
        {
            JOptionPane.showMessageDialog(
                    mainFrame,
                    "Please create a project before saving.\nFor additional information, please view the \"Help\" tab.",
                    "Error: could not create project save",
                    JOptionPane.ERROR_MESSAGE);
        }
        else
        {
            JOptionPane.showMessageDialog(
                    mainFrame,
                    "There is no project save available. Please use \"Save As\" instead.\nFor additional information, please view the \"Help\" tab.",
                    "Error: no project save available",
                    JOptionPane.ERROR_MESSAGE);
        }
    }


    /**
     * Creates new save of project
     */
    private void saveProjectAs()
    {
        if (data != null)
        {
            try
            {
                // create save file dialog
                JFileChooser fileSaver = new JFileChooser();
                fileSaver.setDialogType(JFileChooser.SAVE_DIALOG);
                fileSaver.setAcceptAllFileFilterUsed(false);
                fileSaver.addChoosableFileFilter(new FileNameExtensionFilter("CSV file", "csv"));

                // set to current directory
                File workingDirectory = new File(System.getProperty("user.dir"));
                fileSaver.setCurrentDirectory(workingDirectory);

                if (fileSaver.showSaveDialog(mainFrame) == JFileChooser.APPROVE_OPTION)
                {
                    // get file
                    File fileToSave = fileSaver.getSelectedFile();
                    String fileName = fileToSave.toString();

                    if (fileName.contains(".") && !fileName.contains(".csv"))
                    {
                        JOptionPane.showMessageDialog(
                                mainFrame,
                                "All save files must have a .csv extension.",
                                "Error: save file must be a CSV",
                                JOptionPane.ERROR_MESSAGE);
                    }

                    // add csv extension if not explicitly typed
                    if (!fileName.contains(".csv"))
                        fileName += ".csv";

                    projectSaveName = fileName;

                    // write to csv file
                    Writer out = new FileWriter(fileName, false);

                    // save graph colors
                    out.write(graphColors[0].getRed() + ",");
                    out.write(graphColors[0].getGreen() + ",");
                    out.write(graphColors[0].getBlue() + "\n");
                    out.write(graphColors[1].getRed() + ",");
                    out.write(graphColors[1].getGreen() + ",");
                    out.write(graphColors[1].getBlue() + "\n");
                    out.write(background.getRed() + ",");
                    out.write(background.getGreen() + ",");
                    out.write(background.getBlue() + "\n");

                    // save line colors
                    out.write(domainLines.getRed() + ",");
                    out.write(domainLines.getGreen() + ",");
                    out.write(domainLines.getBlue() + "\n");
                    out.write(overlapLines.getRed() + ",");
                    out.write(overlapLines.getGreen() + ",");
                    out.write(overlapLines.getBlue() + "\n");
                    out.write(thresholdLine.getRed() + ",");
                    out.write(thresholdLine.getGreen() + ",");
                    out.write(thresholdLine.getBlue() + "\n");

                    // save data format
                    if (hasID) out.write("1,");
                    else out.write("0,");
                    if (hasClasses) out.write("1,");
                    else out.write("0,");
                    if (zScoreMinMax) out.write("1\n");
                    else out.write("0\n");

                    // save field length
                    out.write(fieldLength + "\n");

                    // save angles
                    for (int i = 0; i < angles.length; i++)
                    {
                        if (i != angles.length - 1)
                            out.write(angles[i] + ",");
                        else
                            out.write(angles[i] + "\n");
                    }

                    // save threshold
                    out.write(threshold + "\n");

                    // save overlap area
                    out.write(overlapArea[0] + ",");
                    out.write(overlapArea[1] + "\n");

                    // save domain area
                    out.write(domainArea[0] + ",");
                    out.write(domainArea[1] + "\n");

                    // save analytics toggles
                    if (prevAllDataChecked) out.write("1,");
                    else out.write("0,");
                    if (allDataChecked) out.write("1,");
                    else out.write("0,");
                    if (withoutOverlapChecked) out.write("1,");
                    else out.write("0,");
                    if (overlapChecked) out.write("1,");
                    else out.write("0,");
                    if (worstCaseChecked) out.write("1,");
                    else out.write("0,");
                    if (userValidationChecked) out.write("1,");
                    else out.write("0,");
                    if (userValidationImported) out.write("1,");
                    else out.write("0,");
                    if (crossValidationChecked) out.write("1\n");
                    else out.write("0\n");

                    // number of previous confusion matrices
                    out.write(prevAllDataCM.size() + "\n");

                    // save previous confusion matrices
                    for (String s : prevAllDataCM)
                    {
                        char[] cm = s.toCharArray();

                        for (int j = 0; j < cm.length; j++)
                        {
                            // replace newline character with placeholder
                            if (cm[j] == '\n')
                                cm[j] = '~';
                            else if (cm[j] == ',')
                                cm[j] = '`';
                        }

                        out.write(new String(cm) + "\n");
                    }

                    // save k-folds
                    out.write(kFolds + "\n");

                    // save number of classes
                    out.write(classNumber + "\n");

                    // save visualized classes
                    out.write(upperClass + "\n");

                    for (int i = 0; i < lowerClasses.size(); i++)
                    {
                        if (i != lowerClasses.size() - 1)
                        {
                            if (lowerClasses.get(i)) out.write("1,");
                            else out.write("0,");
                        }
                        else
                        {
                            if (lowerClasses.get(i)) out.write("1\n");
                            else out.write("0\n");
                        }
                    }

                    // save class order
                    if (upperIsLower) out.write("1\n");
                    else out.write("0\n");

                    // save unique classes
                    for (int i = 0; i < uniqueClasses.size(); i++)
                    {
                        if (i != uniqueClasses.size() - 1)
                            out.write(uniqueClasses.get(i) + ",");
                        else
                            out.write(uniqueClasses.get(i) + "\n");
                    }

                    // save fieldNames
                    for (int i = 0; i < fieldNames.size(); i++)
                    {
                        if (i != fieldNames.size() - 1)
                            out.write(fieldNames.get(i) + ",");
                        else
                            out.write(fieldNames.get(i) + "\n");
                    }

                    // save data
                    for (DataObject normData : data)
                    {
                        // save number of datapoints
                        out.write(normData.data.length + "\n");

                        for (int j = 0; j < normData.data.length; j++)
                        {
                            for (int k = 0; k < fieldLength; k++)
                            {
                                if (k != fieldLength - 1)
                                    out.write(normData.data[j][k] + ",");
                                else
                                    out.write(normData.data[j][k] + "\n");
                            }
                        }
                    }

                    // save original data
                    for (DataObject origData : originalData)
                    {
                        // save number of datapoints
                        out.write(origData.data.length + "\n");

                        for (int j = 0; j < origData.data.length; j++)
                        {
                            for (int k = 0; k < fieldLength; k++)
                            {
                                if (k != fieldLength - 1)
                                    out.write(origData.data[j][k] + ",");
                                else
                                    out.write(origData.data[j][k] + "\n");
                            }
                        }
                    }


                    if (userValidationImported)
                    {
                        // save validation data
                        for (DataObject valData : validationData)
                        {
                            // save number of datapoints
                            out.write(valData.data.length + "\n");

                            for (int j = 0; j < valData.data.length; j++)
                            {
                                for (int k = 0; k < fieldLength; k++)
                                {
                                    if (k != fieldLength - 1)
                                        out.write(valData.data[j][k] + ",");
                                    else
                                        out.write(valData.data[j][k] + "\n");
                                }
                            }
                        }
                    }

                    // close file
                    out.close();
                }
            }
            catch(IOException e)
            {
                e.printStackTrace();
            }
        }
        else
        {
            JOptionPane.showMessageDialog(
                    mainFrame,
                    "Please create a project before saving.\nFor additional information, please view the \"Help\" tab.",
                    "Error: could not create project save",
                    JOptionPane.ERROR_MESSAGE);
        }
    }


    /**
     * Creates user validation set
     */
    private void createUserValidationSet()
    {
        try
        {
            if (data.size() > 0 && DV.classNumber > 1)
            {
                // set filter on file chooser
                JFileChooser fileDialog = new JFileChooser();
                fileDialog.setFileFilter(new FileNameExtensionFilter("csv", "csv"));

                // set to current directory
                File workingDirectory = new File(System.getProperty("user.dir"));
                fileDialog.setCurrentDirectory(workingDirectory);

                // open file dialog
                if (fileDialog.showOpenDialog(mainFrame) == JFileChooser.APPROVE_OPTION)
                {
                    File valFile = fileDialog.getSelectedFile();

                    // check if validation set was successful
                    userValidationImported = DataSetup.setupValidationData(valFile);

                    // informs of validation data status
                    if (userValidationImported)
                    {
                        JOptionPane.showMessageDialog(
                                mainFrame,
                                "Validation set has been successfully created.\nCreating confusion matrices.",
                                "Success: validation set has been created",
                                JOptionPane.INFORMATION_MESSAGE);

                        // regenerate confusion matrices
                        Analytics.GenerateAnalytics analytics = new Analytics.GenerateAnalytics();
                        analytics.execute();

                        // revalidate graphs and confusion matrices
                        DV.graphPanel.repaint();
                        DV.confusionMatrixPanel.repaint();
                        DV.graphPanel.revalidate();
                        DV.confusionMatrixPanel.revalidate();
                    }
                    else
                    {
                        JOptionPane.showMessageDialog(
                                mainFrame,
                                "The validation set was not able to be created.\nPlease ensure the validation data's file has the same format as the original data file.",
                                "Error: failed to create validation set",
                                JOptionPane.ERROR_MESSAGE);
                    }
                }
                else
                {
                    JOptionPane.showMessageDialog(
                            mainFrame,
                            "Please ensure the file is properly formatted.\nFor additional information, please view the \"Help\" tab.",
                            "Error: could not open file",
                            JOptionPane.ERROR_MESSAGE);
                }


            }
            else if (DV.classNumber == 1)
            {
                JOptionPane.showMessageDialog(
                        mainFrame,
                        "Not enough classes to create validation set.\nFor additional information, please view the \"Help\" tab.",
                        "Error: could not create validation set",
                        JOptionPane.ERROR_MESSAGE);
            }
            else
            {
                JOptionPane.showMessageDialog(
                        mainFrame,
                        "Please create a project before creating a validation set.\nFor additional information, please view the \"Help\" tab.",
                        "Error: could not create validation set",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
        catch (Exception e)
        {
            JOptionPane.showMessageDialog(
                    mainFrame,
                    "Please ensure the file is properly formatted.\nFor additional information, please view the \"Help\" tab.",
                    "Error: could not open file",
                    JOptionPane.ERROR_MESSAGE);
        }
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
     * Creates informative popup explaining how to
     * enter a function
     */
    public static void funcInfoPopup()
    {
        JOptionPane.showMessageDialog(
                mainFrame,
                """
                        Enter a function with "x" as the only variable.
                        All functions must use the symbols below.
                        Symbols not included below cannot be used.
                        
                            Addition: +
                            Subtraction: -
                            Multiplication: *
                            Division: /
                            Exponent: ^
                            Square Root: sqrt()
                            Parenthesis: ( )
                            Sine: sin()
                            Cosine: cos()
                            Tangent: tan()
                        
                        Example:
                            f(x) = 2 * sqrt(sin(x^2))
                        """,
                "Function Help",
                JOptionPane.INFORMATION_MESSAGE
        );
    }


    /**
     * Resets DV program
     * @param remove_classes whether to keep unique classes or not
     */
    private void resetProgram(boolean remove_classes)
    {
        // reset panels
        angleSliderPanel.removeAll();
        graphPanel.removeAll();

        // reset classes
        if (remove_classes)
            uniqueClasses = null;

        // reset previous confusion matrices
        prevAllDataCM = new ArrayList<>();

        // reset previous all data classifications
        prevAllDataClassifications = new ArrayList<>();

        // reset cross validation
        crossValidationNotGenerated = true;

        // reset graphs
        drawOverlap = false;

        // reset popup
        showPopup = true;
    }


    /**
     * DO SOMETHING
     */
    public static void updatePoints()
    {
        for (int i = 0; i < normalizedData.size(); i++)
        {
            for (int j = 0; j < normalizedData.get(i).data.length; j++)
            {
                for (int k = 0; k < fieldLength; k++)
                {
                    Argument x = new Argument("x = " + normalizedData.get(i).data[j][k]);
                    Expression e = new Expression("f(x)", dataFunction, x);
                    data.get(i).data[j][k] = e.calculate();
                }
            }
        }
    }
}
