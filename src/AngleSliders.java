import javax.swing.*;
import java.awt.*;
import java.util.Hashtable;

public class AngleSliders
{
    /**
     * Creates panel for a features angle/weight slider
     * @param fieldName name of feature to be weighted
     * @param angle weight of feature
     * @param index index of feature
     */
    public static void createSliderPanel_GLC(String fieldName, int angle, int index)
    {
        // text for angle
        JTextField angleText = new JTextField(4);
        angleText.setToolTipText("Change angles of visualization");
        angleText.setFont(angleText.getFont().deriveFont(12f));
        angleText.setText(Double.toString(angle / 100.0));

        // slider for angle
        JSlider angleSlider = new JSlider(0, 18000, angle);
        angleSlider.setToolTipText("Change angles of visualization");
        angleSlider.setMinorTickSpacing(1000);
        angleSlider.setPaintTicks(true);
        angleSlider.setPaintLabels(true);

        // label for angle
        JLabel angleLabel = new JLabel(fieldName + " Angle: " + angle / 100.0);
        angleLabel.setToolTipText("Change angles of visualization");

        // create table for slider labels
        Hashtable<Integer, JLabel> position = new Hashtable<>();
        position.put(0, new JLabel("0"));           // 0 degrees
        position.put(3000, new JLabel("30"));       // 30 degrees
        position.put(6000, new JLabel("60"));       // 60 degrees
        position.put(9000, new JLabel("90"));       // 90 degrees
        position.put(12000, new JLabel("120"));     // 120 degrees
        position.put(15000, new JLabel("150"));     // 150 degrees
        position.put(18000, new JLabel("180"));     // 180 degrees
        angleSlider.setLabelTable(position);

        // add listeners for text field and slider
        // action listener for text field
        angleText.addActionListener(e ->
        {
            // get raw value of inputted angle
            double fieldAngle = Double.parseDouble(angleText.getText());

            // transform inputted angle to a slider value
            int sliderValue = (int) (fieldAngle * 100);

            // check if angle is within 0 and 180 degrees
            if (sliderValue >= 0 && sliderValue <= 18000)
            {
                try
                {
                    // set slider to new value
                    angleSlider.setValue(sliderValue);

                    // set angle label to new value
                    angleLabel.setText(fieldName + " Angle: " + fieldAngle);

                    // set angle to new value
                    DV.angles[index] = fieldAngle;
                }
                catch (NumberFormatException nfe)
                {
                    System.err.println("Illegal Input");
                }

                // redraw graphs
                DataVisualization.drawGraphs();
            }
        });

        // change listener for slider
        angleSlider.addChangeListener(e ->
        {
            // get new angle
            double fieldAngle = angleSlider.getValue() / 100.0;

            try
            {
                // set text field to new value
                angleText.setText(Double.toString(fieldAngle));

                // set angle label to new value
                angleLabel.setText(fieldName + " Angle: " + fieldAngle);

                // set angle to new value
                DV.angles[index] = fieldAngle;
            }
            catch (NumberFormatException nfe)
            {
                System.err.println("Illegal Input");
            }

            // redraw graphs
            DataVisualization.drawGraphs();
        });

        // main panel for angle
        // holds textField, slider, and label
        JPanel anglePanel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        anglePanel.add(angleSlider, c);

        c.gridy = 1;
        anglePanel.add(angleLabel, c);

        c.gridx = 1;
        anglePanel.add(angleText, c);

        // add to angle slider panel
        DV.angleSliderPanel.add(anglePanel);
    }

    public static void createSliderPanel_DSC(String fieldName, int angle, int index)
    {
        // text for angle
        JTextField angleText = new JTextField(4);
        angleText.setToolTipText("Change angles of visualization");
        angleText.setFont(angleText.getFont().deriveFont(12f));
        angleText.setText(Double.toString(angle / 100.0));

        // slider for angle
        JSlider angleSlider = new JSlider(0, 18000, angle + 9000);
        angleSlider.setToolTipText("Change angles of visualization");
        angleSlider.setMinorTickSpacing(1000);
        angleSlider.setPaintTicks(true);
        angleSlider.setPaintLabels(true);

        // label for angle
        JLabel angleLabel = new JLabel(fieldName + " Angle: " + angle / 100.0);
        angleLabel.setToolTipText("Change angles of visualization");

        // create table for slider labels
        Hashtable<Integer, JLabel> position = new Hashtable<>();
        position.put(0, new JLabel("-90"));        // -90 degrees
        position.put(3000, new JLabel("-60"));     // -60 degrees
        position.put(6000, new JLabel("-30"));     // -30 degrees
        position.put(9000, new JLabel("0"));       // 0 degrees
        position.put(12000, new JLabel("30"));     // 30 degrees
        position.put(15000, new JLabel("60"));     // 60 degrees
        position.put(18000, new JLabel("90"));     // 90 degrees
        angleSlider.setLabelTable(position);

        // add listeners for text field and slider
        // action listener for text field
        angleText.addActionListener(e ->
        {
            // get raw value of inputted angle
            double fieldAngle = Double.parseDouble(angleText.getText());

            // transform inputted angle to a slider value
            int sliderValue = (int) (fieldAngle * 100) + 9000;

            // check if angle is within 0 and 180 degrees
            if (sliderValue >= 0 && sliderValue <= 18000)
            {
                try
                {
                    // set slider to new value
                    angleSlider.setValue(sliderValue);

                    // set angle label to new value
                    angleLabel.setText(fieldName + " Angle: " + fieldAngle);

                    // set angle to new value
                    DV.angles[index] = fieldAngle;
                }
                catch (NumberFormatException nfe)
                {
                    System.err.println("Illegal Input");
                }

                // redraw graphs
                DataVisualization.drawGraphs();
            }
        });

        // change listener for slider
        angleSlider.addChangeListener(e ->
        {
            // get new angle
            double fieldAngle = (angleSlider.getValue() - 9000) / 100.0;

            try
            {
                // set text field to new value
                angleText.setText(Double.toString(fieldAngle));

                // set angle label to new value
                angleLabel.setText(fieldName + " Angle: " + fieldAngle);

                // set angle to new value
                DV.angles[index] = fieldAngle;
            }
            catch (NumberFormatException nfe)
            {
                System.err.println("Illegal Input");
            }

            // redraw graphs
            DataVisualization.drawGraphs();
        });

        // main panel for angle
        // holds textField, slider, and label
        JPanel anglePanel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        anglePanel.add(angleSlider, c);

        c.gridx = 1;
        anglePanel.add(angleText, c);

        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 2;
        anglePanel.add(angleLabel, c);

        // add to angle slider panel
        DV.angleSliderPanel.add(anglePanel);
    }

    public static JPanel createWeightSliderPanel_GLC(final JSlider angleSlider, String fieldName, int scale, int weightNum, String ruleBase, String className, String opClassName, int curClass, int index, int upper_or_lower, int bound)
    {
        // text for angle
        JTextField angleText = new JTextField(4);
        angleText.setToolTipText("Change weight of visualization");
        angleText.setFont(angleText.getFont().deriveFont(12f));
        angleText.setText(Integer.toString(scale));

        // slider for angle
        angleSlider.setToolTipText("Change weight of visualization");
        angleSlider.setMinorTickSpacing(1);
        angleSlider.setPaintTicks(true);
        angleSlider.setPaintLabels(true);

        // label for angle
        JLabel angleLabel = new JLabel();
        if (bound == 0)
            angleLabel.setText(fieldName + " Lower Scale: " + scale + "%");
        else
            angleLabel.setText(fieldName + " Upper Scale: " + scale + "%");
        angleLabel.setToolTipText("Change scale of attribute");

        // create table for slider labels
        Hashtable<Integer, JLabel> position = new Hashtable<>();
        position.put(0, new JLabel("0"));
        position.put(100, new JLabel("100"));
        position.put(200, new JLabel("200"));
        position.put(300, new JLabel("300"));
        position.put(400, new JLabel("400"));
        position.put(500, new JLabel("500"));
        angleSlider.setLabelTable(position);

        // add listeners for text field and slider
        // action listener for text field
        angleText.addActionListener(e ->
        {
            // get raw value of inputted angle
            int sliderValue = Integer.parseInt(angleText.getText());

            // check if angle is within 0 and 180 degrees
            if (sliderValue >= 0 && sliderValue <= 500)
            {
                // check is sliderValue is within limits
                if (bound == 0)
                {
                    if (DV.data.get(curClass).data[index][weightNum] * sliderValue / 100.0 < DV.limits[weightNum][bound])
                        return;
                }
                else
                {
                    if (DV.data.get(curClass).data[index][weightNum] * sliderValue / 100.0 > DV.limits[weightNum][bound])
                        return;
                }

                try
                {
                    // set slider to new value
                    angleSlider.setValue(sliderValue);

                    // set angle label to new value
                    if (bound == 0)
                        angleLabel.setText(fieldName + " Lower Scale: " + sliderValue + "%");
                    else
                        angleLabel.setText(fieldName + " Upper Scale: " + sliderValue + "%");

                    // set angle to new value
                    DV.scale[bound][weightNum] = sliderValue / 100.0;
                }
                catch (NumberFormatException nfe)
                {
                    System.err.println("Illegal Input");
                }

                // redraw rule
                DataVisualization.drawLDFRule(ruleBase, className, opClassName, curClass, index);

                // redraw graphs
                DataVisualization.drawLDF(curClass, index, upper_or_lower, ruleBase, className, opClassName);
            }
        });

        // change listener for slider
        angleSlider.addChangeListener(e ->
        {
            // get new angle
            int sliderValue = angleSlider.getValue();

            // check is sliderValue is within limits
            if (bound == 0)
            {
                if (DV.data.get(curClass).data[index][weightNum] * sliderValue / 100.0 < DV.limits[weightNum][bound])
                {
                    angleSlider.setValue((int)(DV.limits[weightNum][bound] / DV.data.get(curClass).data[index][weightNum] * 100));
                    sliderValue = angleSlider.getValue();
                }
            }
            else
            {
                if (DV.data.get(curClass).data[index][weightNum] * sliderValue / 100.0 > DV.limits[weightNum][bound])
                {
                    angleSlider.setValue((int)(DV.limits[weightNum][bound] / DV.data.get(curClass).data[index][weightNum] * 100));
                    sliderValue = angleSlider.getValue();
                }
            }

            try
            {
                // set text field to new value
                angleText.setText(Integer.toString(sliderValue));

                // set angle label to new value
                if (bound == 0)
                    angleLabel.setText(fieldName + " Lower Scale: " + sliderValue + "%");
                else
                    angleLabel.setText(fieldName + " Upper Scale: " + sliderValue + "%");

                // set angle to new value
                DV.scale[bound][weightNum] = sliderValue / 100.0;
            }
            catch (NumberFormatException nfe)
            {
                System.err.println("Illegal Input");
            }

            // redraw rule
            DataVisualization.drawLDFRule(ruleBase, className, opClassName, curClass, index);

            // redraw graphs
            DataVisualization.drawLDF(curClass, index, upper_or_lower, ruleBase, className, opClassName);
        });

        // main panel for angle
        // holds textField, slider, and label
        JPanel weightPanel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        weightPanel.add(angleSlider, c);

        c.gridx = 1;
        weightPanel.add(angleText, c);

        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 2;
        weightPanel.add(angleLabel, c);

        // add to angle slider panel
        return weightPanel;
    }
}
