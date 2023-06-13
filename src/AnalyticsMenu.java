import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.concurrent.ExecutionException;

public class AnalyticsMenu extends JPanel
{
    /**
     * Creates Confusion Matrix Menu on mouseLocation
     */
    public AnalyticsMenu()
    {
        // analytics panel
        JPanel analyticsPanel = new JPanel();
        analyticsPanel.setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();

        // set previous confusion matrices
        JCheckBox prevAllDataCheckBox = new JCheckBox("Previous All Data CM", DV.allDataChecked);
        prevAllDataCheckBox.setToolTipText("Toggle previous all data confusion matrices." +
                                            "For 3 or more classes, show the All Data Confusion Matrices" +
                                            "for the previous levels of grouping (one classes vs all other classes) ");
        prevAllDataCheckBox.setFont(prevAllDataCheckBox.getFont().deriveFont(12f));
        prevAllDataCheckBox.addActionListener(e ->
        {
            // reverse check
            DV.prevAllDataChecked = !DV.prevAllDataChecked;

            // regenerate confusion matrices
            generateAnalytics();
        });

        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(5, 5, 5, 5);
        analyticsPanel.add(prevAllDataCheckBox, constraints);

        // set all data confusion matrix
        JCheckBox allDataCheckBox = new JCheckBox("All Data CM", DV.allDataChecked);
        allDataCheckBox.setToolTipText("Toggle all data confusion matrix");
        allDataCheckBox.setFont(allDataCheckBox.getFont().deriveFont(12f));
        allDataCheckBox.addActionListener(e ->
        {
            // reverse check
            DV.allDataChecked = !DV.allDataChecked;

            // regenerate confusion matrices
            generateAnalytics();
        });

        constraints.gridx = 1;
        constraints.gridy = 0;
        analyticsPanel.add(allDataCheckBox, constraints);

        // set data without overlap confusion matrix
        JCheckBox withoutOverlapCheckBox = new JCheckBox("Data Without Overlap CM", DV.withoutOverlapChecked);
        withoutOverlapCheckBox.setToolTipText("Toggle data without overlap confusion matrix");
        withoutOverlapCheckBox.setFont(withoutOverlapCheckBox.getFont().deriveFont(12f));
        withoutOverlapCheckBox.addActionListener(e ->
        {
            // reverse check
            DV.withoutOverlapChecked = !DV.withoutOverlapChecked;

            // regenerate confusion matrices
            generateAnalytics();
        });

        constraints.gridx = 0;
        constraints.gridy = 1;
        analyticsPanel.add(withoutOverlapCheckBox, constraints);

        // set overlap confusion matrix
        JCheckBox overlapCheckBox = new JCheckBox("Overlap Data CM", DV.overlapChecked);
        overlapCheckBox.setToolTipText("Toggle overlap confusion matrix");
        overlapCheckBox.setFont(overlapCheckBox.getFont().deriveFont(12f));
        overlapCheckBox.addActionListener(e ->
        {
            // reverse check
            DV.overlapChecked = !DV.overlapChecked;

            // regenerate confusion matrices
            generateAnalytics();
        });

        constraints.gridx = 1;
        constraints.gridy = 1;
        analyticsPanel.add(overlapCheckBox, constraints);

        // set worst case confusion matrix
        JCheckBox worstCaseCheckBox = new JCheckBox("Worst Case CM", DV.worstCaseChecked);
        worstCaseCheckBox.setToolTipText("Toggle worst case confusion matrix");
        worstCaseCheckBox.setFont(worstCaseCheckBox.getFont().deriveFont(12f));
        worstCaseCheckBox.addActionListener(e ->
        {
            // reverse check
            DV.worstCaseChecked = !DV.worstCaseChecked;

            // regenerate confusion matrices
            generateAnalytics();
        });

        constraints.gridx = 0;
        constraints.gridy = 2;
        analyticsPanel.add(worstCaseCheckBox, constraints);

        // set user validation confusion matrix
        JCheckBox userValCheckBox = new JCheckBox("User Validation CM", DV.userValidationChecked);
        userValCheckBox.setToolTipText("Toggle user validation confusion matrix");
        userValCheckBox.setFont(userValCheckBox.getFont().deriveFont(12f));
        userValCheckBox.addActionListener(e ->
        {
            // reverse check
            DV.userValidationChecked = !DV.userValidationChecked;

            // regenerate confusion matrices
            generateAnalytics();
        });

        constraints.gridx = 1;
        constraints.gridy = 2;
        analyticsPanel.add(userValCheckBox, constraints);

        // set cross validation
        JCheckBox crossValCheckBox = new JCheckBox("Cross Validation", DV.userValidationChecked);
        crossValCheckBox.setToolTipText("Toggle user k-fold cross validation table");
        crossValCheckBox.setFont(crossValCheckBox.getFont().deriveFont(12f));
        crossValCheckBox.addActionListener(e ->
        {
            // reverse check
            DV.crossValidationChecked = !DV.crossValidationChecked;
            DV.crossValidationNotGenerated = true;
            DV.crossValidationPanel.removeAll();

            // regenerate confusion matrices
            generateAnalytics();
        });

        constraints.gridx = 0;
        constraints.gridy = 3;
        analyticsPanel.add(crossValCheckBox, constraints);

        // set k number of folds for cross validation
        JButton kFoldsButton = new JButton("k-folds");
        kFoldsButton.setToolTipText("Number for folds for cross validation");
        kFoldsButton.setFont(kFoldsButton.getFont().deriveFont(12f));
        kFoldsButton.addActionListener(e ->
        {
            // popup asking for number of folds
            JPanel foldPanel = new JPanel(new BorderLayout());

            // text panel
            JPanel textPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

            // maximum text field
            JTextField foldField = new JTextField();
            foldField.setPreferredSize(new Dimension(30, 30));
            textPanel.add(new JLabel("Number of Folds: "));
            textPanel.add(foldField);

            // add text panel
            foldPanel.add(textPanel, BorderLayout.SOUTH);

            int choice = -2;
            // loop until folds are valid or user quits
            while (choice == -2)
            {
                choice = JOptionPane.showConfirmDialog(DV.mainFrame, foldPanel, "Enter the number of folds.", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

                if (choice == 0)
                {
                    try
                    {
                        // get text field values
                        int folds = Integer.parseInt(foldField.getText());

                        if (folds > 0)
                        {
                            // set folds
                            DV.kFolds = folds;

                            // reset k-fold generation
                            DV.crossValidationNotGenerated = true;

                            // regenerate confusion matrices
                            generateAnalytics();
                        }
                        else
                        {
                            // invalid fold input
                            JOptionPane.showMessageDialog(
                                    DV.mainFrame,
                                    "Error: input is invalid.\n" +
                                            "Please enter a whole number greater than 0.",
                                    "Error",
                                    JOptionPane.ERROR_MESSAGE);

                            choice = -2;
                        }
                    }
                    catch (NumberFormatException nfe)
                    {
                        JOptionPane.showMessageDialog(DV.mainFrame, "Error: please enter a whole numerical value.", "Error", JOptionPane.ERROR_MESSAGE);
                        choice = -2;
                    }
                }
            }
        });

        constraints.gridx = 1;
        constraints.gridy = 3;
        analyticsPanel.add(kFoldsButton, constraints);

        // set support vectors analytics
        JCheckBox svmCheckBox = new JCheckBox("SVM Support Vectors", DV.svmAnalyticsChecked);
        svmCheckBox.setToolTipText("Toggle SVM Support Vector analytics");
        svmCheckBox.setFont(svmCheckBox.getFont().deriveFont(12f));
        svmCheckBox.addActionListener(e ->
        {
            // reverse check
            DV.svmAnalyticsChecked = !DV.svmAnalyticsChecked;

            // regenerate confusion matrices
            generateAnalytics();
        });

        constraints.gridx = 0;
        constraints.gridy = 4;
        analyticsPanel.add(svmCheckBox, constraints);

        // open analytics in another window
        JButton separateAnalyticsBtn = new JButton("Analytics Window");
        separateAnalyticsBtn.setToolTipText("Open another window displaying all analytics");
        separateAnalyticsBtn.setFont(separateAnalyticsBtn.getFont().deriveFont(12f));
        separateAnalyticsBtn.addActionListener(e ->
        {
            if (!DV.displayRemoteAnalytics)
            {
                DV.displayRemoteAnalytics = true;

                if (DV.data != null)
                {
                    // generate analytics
                    generateAnalytics();
                }

                JOptionPane optionPane = new JOptionPane(DV.remoteAnalyticsPanel, JOptionPane.PLAIN_MESSAGE, JOptionPane.DEFAULT_OPTION, null, new Object[]{}, null);
                JDialog dialog = optionPane.createDialog(DV.mainFrame, "Analytics");
                dialog.setModal(false);
                dialog.setVisible(true);

                dialog.addWindowListener(new WindowAdapter()
                {
                    @Override
                    public void windowClosing(WindowEvent e)
                    {
                        DV.displayRemoteAnalytics = false;
                        DV.remoteConfusionMatrixPanel.removeAll();
                        DV.remoteCrossValidationPanel.removeAll();
                    }
                });
            }
        });

        constraints.gridx = 1;
        constraints.gridy = 4;
        analyticsPanel.add(separateAnalyticsBtn, constraints);

        JOptionPane.showOptionDialog(DV.mainFrame, analyticsPanel, "Analytics Options", JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, new Object[]{}, null);
    }


    /**
     * Generates updated analytics
     */
    private void generateAnalytics()
    {
        // regenerate confusion matrices
        Analytics.GenerateAnalytics analytics = new Analytics.GenerateAnalytics();
        analytics.execute();

        // wait before repainting and revalidating
        try
        {
            analytics.get();

            // revalidate confusion matrices
            DV.confusionMatrixPanel.repaint();
            DV.confusionMatrixPanel.revalidate();

            if (DV.displayRemoteAnalytics)
            {
                DV.remoteConfusionMatrixPanel.repaint();
                DV.remoteConfusionMatrixPanel.revalidate();
            }
        }
        catch (InterruptedException | ExecutionException ex)
        {
            ex.printStackTrace();
        }
    }
}
