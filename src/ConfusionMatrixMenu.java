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

        // set all data confusion matrix
        JCheckBox allDataCheckBox = new JCheckBox("All Data", DV.allDataChecked);
        allDataCheckBox.setToolTipText("Toggle all data confusion matrix");
        allDataCheckBox.addActionListener(e->
        {
            // reverse check
            DV.allDataChecked = !DV.allDataChecked;

            // regenerate confusion matrices
            DV.allDataCM.removeAll();
            DV.dataWithoutOverlapCM.removeAll();
            DV.overlapCM.removeAll();
            DV.worstCaseCM.removeAll();
            ConfusionMatrices.generateConfusionMatrices();

            // revalidate graphs and confusion matrices
            DV.graphPanel.repaint();
            DV.confusionMatrixPanel.repaint();
            DV.graphPanel.revalidate();
            DV.confusionMatrixPanel.revalidate();
        });
        CMPanel.add(allDataCheckBox);

        // set all data confusion matrix
        JCheckBox withoutOverlapCheckBox = new JCheckBox("Without Overlap", DV.withoutOverlapChecked);
        withoutOverlapCheckBox.setToolTipText("Toggle without overlap confusion matrix");
        withoutOverlapCheckBox.addActionListener(e->
        {
            // reverse check
            DV.withoutOverlapChecked = !DV.withoutOverlapChecked;

            // regenerate confusion matrices
            DV.allDataCM.removeAll();
            DV.dataWithoutOverlapCM.removeAll();
            DV.overlapCM.removeAll();
            DV.worstCaseCM.removeAll();
            ConfusionMatrices.generateConfusionMatrices();

            // revalidate graphs and confusion matrices
            DV.graphPanel.repaint();
            DV.confusionMatrixPanel.repaint();
            DV.graphPanel.revalidate();
            DV.confusionMatrixPanel.revalidate();
        });
        CMPanel.add(withoutOverlapCheckBox);

        // set all data confusion matrix
        JCheckBox overlapCheckBox = new JCheckBox("Overlap", DV.overlapChecked);
        overlapCheckBox.setToolTipText("Toggle overlap confusion matrix");
        overlapCheckBox.addActionListener(e->
        {
            // reverse check
            DV.overlapChecked = !DV.overlapChecked;

            // regenerate confusion matrices
            DV.allDataCM.removeAll();
            DV.dataWithoutOverlapCM.removeAll();
            DV.overlapCM.removeAll();
            DV.worstCaseCM.removeAll();
            ConfusionMatrices.generateConfusionMatrices();

            // revalidate graphs and confusion matrices
            DV.graphPanel.repaint();
            DV.confusionMatrixPanel.repaint();
            DV.graphPanel.revalidate();
            DV.confusionMatrixPanel.revalidate();
        });
        CMPanel.add(overlapCheckBox);

        // set all data confusion matrix
        JCheckBox worstCaseCheckBox = new JCheckBox("Worst Case", DV.worstCaseChecked);
        worstCaseCheckBox.setToolTipText("Toggle worst case confusion matrix");
        worstCaseCheckBox.addActionListener(e->
        {
            // reverse check
            DV.worstCaseChecked = !DV.worstCaseChecked;

            // regenerate confusion matrices
            DV.allDataCM.removeAll();
            DV.dataWithoutOverlapCM.removeAll();
            DV.overlapCM.removeAll();
            DV.worstCaseCM.removeAll();
            ConfusionMatrices.generateConfusionMatrices();

            // revalidate graphs and confusion matrices
            DV.graphPanel.repaint();
            DV.confusionMatrixPanel.repaint();
            DV.graphPanel.revalidate();
            DV.confusionMatrixPanel.revalidate();
        });
        CMPanel.add(worstCaseCheckBox);

        confusionOptionsFrame.add(CMPanel);
        confusionOptionsFrame.pack();
        confusionOptionsFrame.setVisible(true);
    }
}
