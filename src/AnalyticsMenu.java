import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AnalyticsMenu extends JPanel
{
    // exception logger
    private final static Logger LOGGER = Logger.getLogger(Analytics.class.getName());

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
        JCheckBox prevAllDataCheckBox = createCheckBox(
                "Previous All Data CM",
                "Toggle previous all data confusion matrices." +
                "For 3 or more classes, show the All Data Confusion Matrices" +
                "for the previous levels of grouping (one classes vs all other classes)",
                DV.allDataChecked);
        prevAllDataCheckBox.addActionListener(e ->
        {
            // reverse check and regenerate analytics
            DV.prevAllDataChecked = !DV.prevAllDataChecked;
            generateAnalytics();
        });

        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(5, 5, 5, 5);
        analyticsPanel.add(prevAllDataCheckBox, constraints);


        // set all data confusion matrix
        JCheckBox allDataCheckBox = createCheckBox(
                "All Data CM",
                "Toggle all data confusion matrix",
                DV.allDataChecked);
        allDataCheckBox.addActionListener(e ->
        {
            // reverse check and regenerate analytics
            DV.allDataChecked = !DV.allDataChecked;
            generateAnalytics();
        });

        constraints.gridx = 1;
        constraints.gridy = 0;
        analyticsPanel.add(allDataCheckBox, constraints);


        // set data without overlap confusion matrix
        JCheckBox withoutOverlapCheckBox = createCheckBox(
                "Data Without Overlap CM",
                "Toggle data without overlap confusion matrix",
                DV.withoutOverlapChecked);
        withoutOverlapCheckBox.addActionListener(e ->
        {
            // reverse check and regenerate analytics
            DV.withoutOverlapChecked = !DV.withoutOverlapChecked;
            generateAnalytics();
        });

        constraints.gridx = 0;
        constraints.gridy = 1;
        analyticsPanel.add(withoutOverlapCheckBox, constraints);


        // set overlap confusion matrix
        JCheckBox overlapCheckBox = createCheckBox(
                "Overlap Data CM",
                "Toggle overlap confusion matrix",
                DV.overlapChecked);
        overlapCheckBox.addActionListener(e ->
        {
            // reverse check and regenerate analytics
            DV.overlapChecked = !DV.overlapChecked;
            generateAnalytics();
        });

        constraints.gridx = 1;
        constraints.gridy = 1;
        analyticsPanel.add(overlapCheckBox, constraints);


        // set worst case confusion matrix
        JCheckBox worstCaseCheckBox = createCheckBox(
                "Worst Case CM",
                "Toggle worst case confusion matrix",
                DV.worstCaseChecked);
        worstCaseCheckBox.addActionListener(e ->
        {
            // reverse check and regenerate analytics
            DV.worstCaseChecked = !DV.worstCaseChecked;
            generateAnalytics();
        });

        constraints.gridx = 0;
        constraints.gridy = 2;
        analyticsPanel.add(worstCaseCheckBox, constraints);


        // set user validation confusion matrix
        JCheckBox userValCheckBox = createCheckBox(
                "User Validation CM",
                "Toggle user validation confusion matrix",
                DV.userValidationChecked);
        userValCheckBox.addActionListener(e ->
        {
            // reverse check and regenerate analytics
            DV.userValidationChecked = !DV.userValidationChecked;
            generateAnalytics();
        });

        constraints.gridx = 1;
        constraints.gridy = 2;
        analyticsPanel.add(userValCheckBox, constraints);


        // set cross validation
        JCheckBox crossValCheckBox = createCheckBox(
                "Cross Validation",
                "Toggle user k-fold cross validation table",
                DV.crossValidationChecked);
        crossValCheckBox.addActionListener(e ->
        {
            // reverse check and regenerate analytics
            DV.crossValidationChecked = !DV.crossValidationChecked;
            DV.crossValidationNotGenerated = true;
            DV.crossValidationPanel.removeAll();
            Analytics.generateCrossValidation();
        });

        constraints.gridx = 0;
        constraints.gridy = 3;
        analyticsPanel.add(crossValCheckBox, constraints);


        // set k number of folds for cross validation
        JButton kFoldsButton = createButton(
                "k-folds",
                "Number for folds for cross validation");
        kFoldsButton.addActionListener(e -> setKFoldPopup());

        constraints.gridx = 1;
        constraints.gridy = 3;
        analyticsPanel.add(kFoldsButton, constraints);


        // set support vectors analytics
        JCheckBox svmCheckBox = createCheckBox(
                "SVM Support Vectors",
                "Toggle SVM Support Vector analytics",
                DV.svmAnalyticsChecked);
        svmCheckBox.addActionListener(e ->
        {
            // reverse check and regenerate analytics
            DV.svmAnalyticsChecked = !DV.svmAnalyticsChecked;
            generateAnalytics();
        });

        constraints.gridx = 0;
        constraints.gridy = 4;
        analyticsPanel.add(svmCheckBox, constraints);


        // open analytics in another window
        JButton separateAnalyticsBtn = createButton(
                "Analytics Window",
                "Open another window displaying all analytics");
        separateAnalyticsBtn.addActionListener(e ->
        {
            if (!DV.displayRemoteAnalytics)
            {
                DV.displayRemoteAnalytics = true;

                if (DV.trainData != null)
                {
                    // generate analytics
                    generateAnalytics();
                    Analytics.generateCrossValidation();
                    createRemoteAnalyticPane();
                }
            }
        });

        constraints.gridx = 1;
        constraints.gridy = 4;
        analyticsPanel.add(separateAnalyticsBtn, constraints);

        JOptionPane.showOptionDialog(DV.mainFrame, analyticsPanel, "Analytics Options", JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, new Object[]{}, null);
    }


    /**
     * Creates a JCheckBox
     * @param label label of checkbox
     * @param tooltip tooltip info for checkbox
     * @param check boolean to trigger when checked
     * @return checkbox
     */
    private JCheckBox createCheckBox(String label, String tooltip, boolean check)
    {
        JCheckBox checkBox = new JCheckBox(label, check);
        checkBox.setToolTipText(tooltip);
        checkBox.setFont(checkBox.getFont().deriveFont(12f));

        return checkBox;
    }


    /**
     * Creates a JButton
     * @param label label of button
     * @param tooltip tooltip info for button
     * @return checkbox
     */
    private JButton createButton(String label, String tooltip)
    {
        JButton button = new JButton(label);
        button.setToolTipText(tooltip);
        button.setFont(button.getFont().deriveFont(12f));

        return button;
    }


    /**
     * Popup for selecting number of folds for k-fold cross validation
     */
    private void setKFoldPopup()
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
                        DV.warningPopup(
                                "Error",
                                "Error: input is invalid. Please enter a whole number greater than 0."
                        );

                        choice = -2;
                    }
                }
                catch (NumberFormatException nfe)
                {
                    DV.warningPopup(
                            "Error",
                            "Error: please enter a whole numerical value.");
                    choice = -2;
                }
            }
        }
    }


    /**
     * Creates remote analytics pane
     */
    private void createRemoteAnalyticPane()
    {
        // create separate analytic window
        JOptionPane optionPane = new JOptionPane(DV.remoteAnalyticsPanel, JOptionPane.PLAIN_MESSAGE, JOptionPane.DEFAULT_OPTION, null, new Object[]{}, null);
        JDialog dialog = optionPane.createDialog(DV.mainFrame, "Remote Analytics");
        dialog.setSize(Resolutions.dvWindow[0] / 2, Resolutions.dvWindow[1] / 2);
        dialog.setResizable(true);
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
            LOGGER.log(Level.SEVERE, ex.toString(), ex);
        }
    }
}
