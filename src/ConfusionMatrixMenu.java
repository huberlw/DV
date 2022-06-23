import javax.swing.*;
import java.awt.*;

public class ConfusionMatrixMenu extends JPanel
{
    public ConfusionMatrixMenu(Point mouseLocation)
    {
        super(new BorderLayout());

        // create popup window
        JFrame confusionOptionsFrame = new JFrame("Confusion Matrices");
        confusionOptionsFrame.setLocation(mouseLocation);

        // confusion matrix panel
        JPanel CMPanel = new JPanel();

        // set previous confusion matrices
        JCheckBox prevAllDataCheckBox = new JCheckBox("Previous All Data", DV.allDataChecked);
        prevAllDataCheckBox.setToolTipText("Toggle previous all data confusion matrices");
        prevAllDataCheckBox.addActionListener(e->
        {
            // reverse check
            DV.prevAllDataChecked = !DV.prevAllDataChecked;

            // regenerate confusion matrices
            ConfusionMatrices.generateConfusionMatrices();

            // revalidate graphs and confusion matrices
            DV.graphPanel.repaint();
            DV.confusionMatrixPanel.repaint();
            DV.graphPanel.revalidate();
            DV.confusionMatrixPanel.revalidate();
        });
        CMPanel.add(prevAllDataCheckBox);

        // set all data confusion matrix
        JCheckBox allDataCheckBox = new JCheckBox("All Data", DV.allDataChecked);
        allDataCheckBox.setToolTipText("Toggle all data confusion matrix");
        allDataCheckBox.addActionListener(e->
        {
            // reverse check
            DV.allDataChecked = !DV.allDataChecked;

            // regenerate confusion matrices
            ConfusionMatrices.generateConfusionMatrices();

            // revalidate graphs and confusion matrices
            DV.graphPanel.repaint();
            DV.confusionMatrixPanel.repaint();
            DV.graphPanel.revalidate();
            DV.confusionMatrixPanel.revalidate();
        });
        CMPanel.add(allDataCheckBox);

        // set data without overlap confusion matrix
        JCheckBox withoutOverlapCheckBox = new JCheckBox("Without Overlap", DV.withoutOverlapChecked);
        withoutOverlapCheckBox.setToolTipText("Toggle without overlap confusion matrix");
        withoutOverlapCheckBox.addActionListener(e->
        {
            // reverse check
            DV.withoutOverlapChecked = !DV.withoutOverlapChecked;

            // regenerate confusion matrices
            ConfusionMatrices.generateConfusionMatrices();

            // revalidate graphs and confusion matrices
            DV.graphPanel.repaint();
            DV.confusionMatrixPanel.repaint();
            DV.graphPanel.revalidate();
            DV.confusionMatrixPanel.revalidate();
        });
        CMPanel.add(withoutOverlapCheckBox);

        // set overlap confusion matrix
        JCheckBox overlapCheckBox = new JCheckBox("Overlap", DV.overlapChecked);
        overlapCheckBox.setToolTipText("Toggle overlap confusion matrix");
        overlapCheckBox.addActionListener(e->
        {
            // reverse check
            DV.overlapChecked = !DV.overlapChecked;

            // regenerate confusion matrices
            ConfusionMatrices.generateConfusionMatrices();

            // revalidate graphs and confusion matrices
            DV.graphPanel.repaint();
            DV.confusionMatrixPanel.repaint();
            DV.graphPanel.revalidate();
            DV.confusionMatrixPanel.revalidate();
        });
        CMPanel.add(overlapCheckBox);

        // set worst case confusion matrix
        JCheckBox worstCaseCheckBox = new JCheckBox("Worst Case", DV.worstCaseChecked);
        worstCaseCheckBox.setToolTipText("Toggle worst case confusion matrix");
        worstCaseCheckBox.addActionListener(e->
        {
            // reverse check
            DV.worstCaseChecked = !DV.worstCaseChecked;

            // regenerate confusion matrices
            ConfusionMatrices.generateConfusionMatrices();

            // revalidate graphs and confusion matrices
            DV.graphPanel.repaint();
            DV.confusionMatrixPanel.repaint();
            DV.graphPanel.revalidate();
            DV.confusionMatrixPanel.revalidate();
        });
        CMPanel.add(worstCaseCheckBox);

        // set user validation set confusion matrix
        JCheckBox userValCheckBox = new JCheckBox("User Validation", DV.userValidationChecked);
        userValCheckBox.setToolTipText("Toggle user validation confusion matrix");
        userValCheckBox.addActionListener(e->
        {
            // reverse check
            DV.userValidationChecked = !DV.userValidationChecked;

            // regenerate confusion matrices
            ConfusionMatrices.generateConfusionMatrices();

            // revalidate graphs and confusion matrices
            DV.graphPanel.repaint();
            DV.confusionMatrixPanel.repaint();
            DV.graphPanel.revalidate();
            DV.confusionMatrixPanel.revalidate();
        });
        CMPanel.add(userValCheckBox);

        confusionOptionsFrame.add(CMPanel);
        confusionOptionsFrame.pack();
        confusionOptionsFrame.setVisible(true);
    }
}
