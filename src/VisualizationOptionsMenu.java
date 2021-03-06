import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class VisualizationOptionsMenu extends JPanel
{
    /**
     * Creates Visualization Options Menu on mouseLocation
     * @param mouseLocation location to create menu on
     */
    public VisualizationOptionsMenu(Point mouseLocation)
    {
        super(new BorderLayout());

        // create popup window
        JFrame visOptionsFrame = new JFrame("Visualization Options");
        visOptionsFrame.setLocation(mouseLocation);

        // choose class to visualize as main
        JPanel chooseUpperClassPanel = new JPanel();
        JButton chooseUpperClassBtn = new JButton("Upper Class");
        chooseUpperClassBtn.setToolTipText("Choose class to be visualized on upper graph");
        chooseUpperClassBtn.addActionListener(e ->
        {
            int chosen = JOptionPane.showOptionDialog(
                    visOptionsFrame,
                    "Choose upper class.\nThe upper class will be visualized on the upper graph.",
                    "Choose Upper Class",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    DV.uniqueClasses.toArray(),
                    null);

            if (chosen != -1)
            {
                // remove past accuracies and classifications
                DV.prevAllDataCM.clear();
                DV.prevAllDataClassifications.clear();

                // set upper class
                DV.upperClass = chosen;

                // lower class gets all others
                for (int i = 0; i < DV.classNumber; i++)
                {
                    if (i != chosen)
                        DV.lowerClasses.set(i, true);
                    else
                        DV.lowerClasses.set(i, false);
                }

                // optimize setup then draw graphs
                DataVisualization.optimizeSetup();
                DataVisualization.drawGraphs();

                visOptionsFrame.dispatchEvent(new WindowEvent(visOptionsFrame, WindowEvent.WINDOW_CLOSING));
            }
        });
        chooseUpperClassPanel.add(chooseUpperClassBtn);

        // specify visualization
        JPanel specifyVisPanel = new JPanel();
        JButton specifyVisBtn = new JButton("Specify Visualization");
        specifyVisBtn.setToolTipText("Removes one class from the lower graph");
        specifyVisBtn.addActionListener(e ->
        {
            // classes on lower graph
            ArrayList<String> removableClasses = new ArrayList<>();
            ArrayList<String> classes = new ArrayList<>(DV.uniqueClasses);

            for (int i = 0; i < DV.classNumber; i++)
            {
                if (DV.lowerClasses.get(i))
                    removableClasses.add(classes.get(i));
            }

            if (removableClasses.size() > 1)
            {
                JLabel removableLabel = new JLabel("Removable Classes");
                JComboBox<Object> removableList = new JComboBox<>(removableClasses.toArray());
                removableList.setSelectedIndex(0);
                removableList.setEditable(true);

                JPanel removablePanel = new JPanel();
                removablePanel.add(removableLabel);
                removablePanel.add(removableList);

                int choice = JOptionPane.showConfirmDialog(
                        visOptionsFrame,
                        removablePanel,
                        "Remove Class",
                        JOptionPane.OK_CANCEL_OPTION);

                if (choice == 0)
                {
                    // add previous analytics
                    DV.prevAllDataCM.add(DV.allDataCM);
                    DV.prevAllDataClassifications.add(DV.allDataClassifications);

                    // get class to be removed
                    int selected = removableList.getSelectedIndex();
                    String className = removableClasses.get(selected);

                    // remove class
                    for (int i = 0; i < DV.classNumber; i++)
                    {
                        if (className.equals(classes.get(i)))
                            DV.lowerClasses.set(i, false);
                    }

                    // optimize setup then draw graphs
                    DataVisualization.optimizeSetup();
                    DataVisualization.drawGraphs();
                }
            }
            else
            {
                JOptionPane.showMessageDialog(visOptionsFrame, "Classes cannot be further separated.");
            }
        });
        specifyVisPanel.add(specifyVisBtn);

        // visualize overlap area
        JPanel visOverlapPanel = new JPanel();
        JButton visOverlapBtn = new JButton("Visualize Overlap");
        visOverlapBtn.setToolTipText("Visualize the overlap area");
        visOverlapBtn.addActionListener(e ->
        {
            if (DV.classNumber > 1 && DV.accuracy < 100)
            {
                DV.drawOverlap = true;
                DataVisualization.drawGraphs();
            }
            else
                JOptionPane.showMessageDialog(visOptionsFrame, "No overlap area");
        });
        visOverlapPanel.add(visOverlapBtn);

        // stop visualizing overlap
        JPanel stopOverlapVisPanel = new JPanel();
        JButton stopOverlapVisBtn = new JButton("Stop Visualizing Overlap");
        stopOverlapVisBtn.setToolTipText("Visualize all data");
        stopOverlapVisBtn.addActionListener(e ->
        {
            DV.drawOverlap = false;
            DataVisualization.drawGraphs();
        });
        stopOverlapVisPanel.add(stopOverlapVisBtn);

        // change visualization function for each attribute of each vector
        JPanel scalarVisFuncPanel = new JPanel();
        JButton scalarVisFuncBtn = new JButton("Scalar Function");
        scalarVisFuncBtn.setToolTipText("Applies given function to all attributes of all data points");
        scalarVisFuncBtn.addActionListener(e ->
        {
            // popup asking for number of folds
            JPanel funcPanel = new JPanel(new BorderLayout());

            // text panel
            JPanel textPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

            // maximum text field
            JTextField funcField = new JTextField();
            funcField.setPreferredSize(new Dimension(200, 30));
            funcField.setText(DV.scalarFunction);
            textPanel.add(new JLabel("Function: f(x) = "));
            textPanel.add(funcField);

            // add text panel
            funcPanel.add(textPanel, BorderLayout.SOUTH);

            Object[] funcButtons = { "Ok", "Cancel", "Help" };

            boolean notChosen = true;

            // loop until folds are valid or user quits
            while (notChosen)
            {
                int choice = JOptionPane.showOptionDialog(
                        DV.mainFrame, funcPanel,
                        "Enter function",
                        JOptionPane.YES_NO_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE,
                        null,
                        funcButtons,
                        funcButtons[0]);

                switch (choice)
                {
                    case 0 ->
                    {
                        // get function and remove spaces
                        String func = funcField.getText();

                        try
                        {
                            // try new function with dummy variables
                            Map<String, Double> variables = new HashMap<>();
                            FunctionParser.Expression exp = FunctionParser.parseScalerExpression(func, variables);

                            for (double x = 0; x < 1; x += 0.1)
                            {
                                variables.put("x", x);
                                exp.eval();
                            }

                            // apply function if working
                            DV.scalarFunction = func;

                            for (int i = 0; i < DV.normalizedData.size(); i++)
                            {
                                for (int j = 0; j < DV.normalizedData.get(i).data.length; j++)
                                {
                                    for (int k = 0; k < DV.fieldLength; k++)
                                    {
                                        variables.put("x", DV.data.get(i).data[j][k]);
                                        DV.data.get(i).data[j][k] = exp.eval();
                                    }
                                }
                            }

                            DataVisualization.optimizeSetup();
                            DataVisualization.drawGraphs();
                        }
                        catch (Exception exc)
                        {
                            // invalid function input
                            JOptionPane.showMessageDialog(
                                    DV.mainFrame,
                                    """
                                            Error: input is invalid.
                                            Please enter a valid function.
                                            Select "Help" for more info.
                                            """,
                                    "Error",
                                    JOptionPane.ERROR_MESSAGE);
                        }

                        notChosen = false;
                    }
                    case 2 -> DV.scalarFuncInfoPopup();
                    default -> { return; }
                }
            }
        });
        scalarVisFuncPanel.add(scalarVisFuncBtn);

        /**
         * START CONSTRUCTION ZONE
         */
        // change visualization function for each vector
        JPanel vectorVisFuncPanel = new JPanel();
        JButton vectorVisFuncBtn = new JButton("Vector Function");
        vectorVisFuncBtn.setToolTipText("Applies given function to all data points");
        vectorVisFuncBtn.addActionListener(e ->
        {
            // popup asking for number of folds
            JPanel funcPanel = new JPanel(new BorderLayout());

            // radio button group
            ButtonGroup stockFunc = new ButtonGroup();
            JRadioButton svmPolyFunc = new JRadioButton("SVM - Polynomial Kernel", true);
            JRadioButton svmRBFFunc = new JRadioButton("SVM - RBF Kernel");
            JRadioButton customFunc = new JRadioButton("Custom");
            stockFunc.add(svmPolyFunc);
            stockFunc.add(svmRBFFunc);
            stockFunc.add(customFunc);

            // default function panel
            JPanel stockPanel = new JPanel();
            stockPanel.add(new JLabel("Built-In: "));
            stockPanel.add(svmPolyFunc);
            stockPanel.add(svmRBFFunc);
            stockPanel.add(customFunc);
            funcPanel.add(stockPanel, BorderLayout.NORTH);

            // text panel
            JPanel textPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

            // maximum text field
            JTextField funcField = new JTextField();
            /**
             * REMOVE THIS LINE LATER
             */
            funcField.setEditable(false);
            /**
             * REMOVE THIS LINE LATER
             */
            funcField.setPreferredSize(new Dimension(200, 30));
            funcField.setText(DV.scalarFunction);
            textPanel.add(new JLabel("Function: f(x, y) = "));
            textPanel.add(funcField);

            // add text panel
            funcPanel.add(textPanel, BorderLayout.SOUTH);

            // add listeners
            svmPolyFunc.addActionListener(e1 -> funcField.setText("(1/" + DV.fieldLength + " * dot(x, y) + 1)^3"));
            svmRBFFunc.addActionListener(e1 -> funcField.setText("e^(-1/" + DV.fieldLength + " * norm(vSub(x, y))^2)"));

            funcField.addKeyListener(new KeyListener() {
                @Override
                public void keyTyped(KeyEvent e)
                {
                    customFunc.setSelected(true);
                }

                @Override
                public void keyPressed(KeyEvent e) {}

                @Override
                public void keyReleased(KeyEvent e) {}
            });

            Object[] funcButtons = { "Ok", "Cancel", "Help" };

            boolean notChosen = true;

            // loop until folds are valid or user quits
            while (notChosen)
            {
                int choice = JOptionPane.showOptionDialog(
                        DV.mainFrame, funcPanel,
                        "Enter function",
                        JOptionPane.YES_NO_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE,
                        null,
                        funcButtons,
                        funcButtons[0]);

                switch (choice)
                {
                    case 0 ->
                    {
                        // get function and remove spaces
                        String func = funcField.getText();

                        try
                        {
                            // try new function with dummy variables
                            Map<String, double[]> variables = new HashMap<>();
                            FunctionParser.Expression exp = FunctionParser.parseVectorExpression(func, variables);

                            for (double x = 0; x < 1; x += 0.1)
                            {
                                variables.put("x", new double[]{ x, x, x });
                                variables.put("y", new double[]{ x, x, x });
                                exp.eval();
                            }

                            // apply function if working
                            DV.vectorFunction = func;
                            ArrayList<double[][]> splitByClass = new ArrayList<>();

                            for (int i = 0; i < DV.normalizedData.size(); i++)
                            {
                                ArrayList<double[]> classData = new ArrayList<>();

                                for (int j = 0; j < DV.normalizedData.get(i).data.length; j++)
                                {
                                    ArrayList<Double> newRow = new ArrayList<>();

                                    for (int k = 0; k < DV.supportVectors.data.length; k++)
                                    {
                                        variables.put("x", DV.data.get(i).data[j]);
                                        variables.put("y", DV.supportVectors.data[k]);
                                        newRow.add(exp.eval());
                                    }

                                    double[] newRowArray = new double[newRow.size()];

                                    for (int w = 0; w < newRow.size(); w++)
                                        newRowArray[w] = newRow.get(w);

                                    classData.add(newRowArray);
                                }

                                double[][] newClassData = new double[classData.size()][];

                                for (int w = 0; w < classData.size(); w++)
                                    newClassData[w] = classData.get(w);

                                splitByClass.add(newClassData);
                            }

                            DV.data = DataSetup.createDataObjects(splitByClass);

                            DV.fieldLength = splitByClass.get(0)[0].length;
                            System.out.println(DV.fieldLength);

                            DV.angles = new double[DV.fieldLength];
                            DV.prevAngles = new double[DV.fieldLength];
                            DV.fieldNames.clear();

                            for (int i = 0; i < DV.fieldLength; i++)
                            {
                                DV.fieldNames.add("feature " + i);
                                DV.angles[i] = 45;
                                DV.prevAngles[i] = 45;
                            }

                            DataVisualization.optimizeSetup();
                            DataVisualization.drawGraphs();
                        }
                        catch (Exception exc)
                        {
                            // invalid function input
                            JOptionPane.showMessageDialog(
                                    DV.mainFrame,
                                    """
                                            Error: input is invalid.
                                            Please enter a valid function.
                                            Select "Help" for more info.
                                            """,
                                    "Error",
                                    JOptionPane.ERROR_MESSAGE);
                        }

                        notChosen = false;
                    }
                    case 2 -> DV.vectorFuncInfoPopup();
                    default -> { return; }
                }
            }
        });
        vectorVisFuncPanel.add(vectorVisFuncBtn);
        /**
         * END CONSTRUCTION ZONE
         */

        // options panel
        JPanel visOptions = new JPanel();
        visOptions.add(chooseUpperClassPanel);

        // if there are more than two classes
        visOptions.add(specifyVisPanel);

        // check if not drawing overlap
        if (!DV.drawOverlap)
            visOptions.add(visOverlapPanel);

        // if drawing overlap
        if (DV.drawOverlap)
            visOptions.add(vectorVisFuncPanel);

        // add functions
        visOptions.add(scalarVisFuncPanel);
        visOptions.add(vectorVisFuncPanel);

        visOptionsFrame.add(visOptions);
        visOptionsFrame.pack();
        visOptionsFrame.setVisible(true);
    }
}
