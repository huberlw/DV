import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.concurrent.ExecutionException;

public class AnalyticsMenu extends JPanel
{
    /**
     * Creates Confusion Matrix Menu on mouseLocation
     * @param mouseLocation location to create menu on
     */
    public AnalyticsMenu(Point mouseLocation)
    {
        super(new BorderLayout());

        // create popup window
        JFrame analyticsOptionFrame = new JFrame("Analytics");
        analyticsOptionFrame.setLocation(mouseLocation);

        // confusion matrix panel
        JPanel analyticsPanel = new JPanel();

        // set previous confusion matrices
        JCheckBox prevAllDataCheckBox = new JCheckBox("Previous All Data CM", DV.allDataChecked);
        prevAllDataCheckBox.setToolTipText("Toggle previous all data confusion matrices");
        prevAllDataCheckBox.addActionListener(e->
        {
            // reverse check
            DV.prevAllDataChecked = !DV.prevAllDataChecked;

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
        });
        analyticsPanel.add(prevAllDataCheckBox);

        // set all data confusion matrix
        JCheckBox allDataCheckBox = new JCheckBox("All Data CM", DV.allDataChecked);
        allDataCheckBox.setToolTipText("Toggle all data confusion matrix");
        allDataCheckBox.addActionListener(e->
        {
            // reverse check
            DV.allDataChecked = !DV.allDataChecked;

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
        });
        analyticsPanel.add(allDataCheckBox);

        // set data without overlap confusion matrix
        JCheckBox withoutOverlapCheckBox = new JCheckBox("Data Without Overlap CM", DV.withoutOverlapChecked);
        withoutOverlapCheckBox.setToolTipText("Toggle data without overlap confusion matrix");
        withoutOverlapCheckBox.addActionListener(e->
        {
            // reverse check
            DV.withoutOverlapChecked = !DV.withoutOverlapChecked;

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
        });
        analyticsPanel.add(withoutOverlapCheckBox);

        // set overlap confusion matrix
        JCheckBox overlapCheckBox = new JCheckBox("Overlap Data CM", DV.overlapChecked);
        overlapCheckBox.setToolTipText("Toggle overlap confusion matrix");
        overlapCheckBox.addActionListener(e->
        {
            // reverse check
            DV.overlapChecked = !DV.overlapChecked;

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
        });
        analyticsPanel.add(overlapCheckBox);

        // set worst case confusion matrix
        JCheckBox worstCaseCheckBox = new JCheckBox("Worst Case CM", DV.worstCaseChecked);
        worstCaseCheckBox.setToolTipText("Toggle worst case confusion matrix");
        worstCaseCheckBox.addActionListener(e->
        {
            // reverse check
            DV.worstCaseChecked = !DV.worstCaseChecked;

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
        });
        analyticsPanel.add(worstCaseCheckBox);

        // set user validation set confusion matrix
        JCheckBox userValCheckBox = new JCheckBox("User Validation CM", DV.userValidationChecked);
        userValCheckBox.setToolTipText("Toggle user validation confusion matrix");
        userValCheckBox.addActionListener(e->
        {
            // reverse check
            DV.userValidationChecked = !DV.userValidationChecked;

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
        });
        analyticsPanel.add(userValCheckBox);

        // set cross validation
        JCheckBox crossValCheckBox = new JCheckBox("Cross Validation", DV.userValidationChecked);
        crossValCheckBox.setToolTipText("Toggle user k-fold cross validation table");
        crossValCheckBox.addActionListener(e->
        {
            // reverse check
            DV.crossValidationChecked = !DV.crossValidationChecked;
            DV.crossValidationNotGenerated = true;

            // regenerate confusion matrices
            Analytics.GenerateAnalytics analytics = new Analytics.GenerateAnalytics();
            analytics.execute();

            // wait before repainting and revalidating
            try
            {
                DV.crossValidationPanel.removeAll();
                analytics.get();

                // revalidate cross validation panel
                DV.crossValidationPanel.repaint();
                DV.crossValidationPanel.revalidate();

                if (DV.displayRemoteAnalytics)
                {
                    DV.remoteCrossValidationPanel.repaint();
                    DV.remoteCrossValidationPanel.revalidate();
                }
            }
            catch (InterruptedException | ExecutionException ex)
            {
                ex.printStackTrace();
            }
        });
        analyticsPanel.add(crossValCheckBox);

        // set k number of folds for cross validation
        JButton kFoldsButton = new JButton("k-folds");
        kFoldsButton.setToolTipText("Number for folds for cross validation");
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
                            Analytics.GenerateAnalytics analytics = new Analytics.GenerateAnalytics();
                            analytics.execute();

                            // wait before repainting and revalidating
                            try
                            {
                                analytics.get();

                                // revalidate cross validation panel
                                DV.crossValidationPanel.repaint();
                                DV.crossValidationPanel.revalidate();

                                if (DV.displayRemoteAnalytics)
                                {
                                    DV.remoteCrossValidationPanel.repaint();
                                    DV.remoteCrossValidationPanel.revalidate();
                                }
                            }
                            catch (InterruptedException | ExecutionException ex)
                            {
                                ex.printStackTrace();
                            }
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
        analyticsPanel.add(kFoldsButton);

        // open analytics in another window
        JButton separateAnalyticsBtn = new JButton("Analytics Window");
        separateAnalyticsBtn.setToolTipText("Open another window displaying all analytics");
        separateAnalyticsBtn.addActionListener(e->
        {
            if (!DV.displayRemoteAnalytics)
            {
                DV.displayRemoteAnalytics = true;

                if (DV.data != null)
                {
                    // generate analytics
                    Analytics.GenerateAnalytics analytics = new Analytics.GenerateAnalytics();
                    analytics.execute();

                    // wait for analytics thread to finish
                    try
                    {
                        analytics.get();
                    }
                    catch (ExecutionException | InterruptedException ae)
                    {
                        ae.printStackTrace();
                        return;
                    }
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
        analyticsPanel.add(separateAnalyticsBtn);

        analyticsOptionFrame.add(analyticsPanel);
        analyticsOptionFrame.pack();
        analyticsOptionFrame.setVisible(true);
    }
}
