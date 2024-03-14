import javax.swing.*;
import java.awt.*;
import java.util.Hashtable;

public class AngleSliders
{
    /**
     * Creates panel for a features angle/weight slider for GLC-Linear
     * @param fieldName name of feature to be weighted
     * @param angle weight of feature
     * @param index index of feature
     */
    public static void createSliderPanel_GLC(String fieldName, int angle, int index)
    {
        // text for angle
        JTextField angleText = createTextAngleInput(Double.toString(angle / 100.0));

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
            // transform raw value to slider value
            double fieldAngle = Double.parseDouble(angleText.getText());
            int sliderValue = (int) (fieldAngle * 100);

            // check if angle is within 0 and 180 degrees
            if (sliderValue >= 0 && sliderValue <= 18000)
            {
                try
                {
                    // set new value
                    angleSlider.setValue(sliderValue);
                    angleLabel.setText(fieldName + " Angle: " + fieldAngle);
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
                // set new value
                angleText.setText(Double.toString(fieldAngle));
                angleLabel.setText(fieldName + " Angle: " + fieldAngle);
                DV.angles[index] = fieldAngle;
            }
            catch (NumberFormatException nfe)
            {
                System.err.println("Illegal Input");
            }

            // redraw graphs
            DataVisualization.drawGraphs();
        });

        // add to angle slider panel
        DV.angleSliderPanel.add(createAnglePanel(angleSlider, angleLabel, angleText));
    }


    /**
     * Creates panel for a features angle/weight slider for Dynamic Scaffold Coordinates
     * @param fieldName name of feature to be weighted
     * @param angle weight of feature
     * @param index index of feature
     */
    public static void createSliderPanel_DSC(String fieldName, int angle, int index)
    {
        // text for angle
        JTextField angleText = createTextAngleInput(Double.toString(angle / 100.0));

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
                    // set new value
                    angleSlider.setValue(sliderValue);
                    angleLabel.setText(fieldName + " Angle: " + fieldAngle);
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
                // set new value
                angleText.setText(Double.toString(fieldAngle));
                angleLabel.setText(fieldName + " Angle: " + fieldAngle);
                DV.angles[index] = fieldAngle;
            }
            catch (NumberFormatException nfe)
            {
                System.err.println("Illegal Input");
            }

            // redraw graphs
            DataVisualization.drawGraphs();
        });

        // add to angle slider panel
        DV.angleSliderPanel.add(createAnglePanel(angleSlider, angleLabel, angleText));
    }


    /**
     * Creates panel for feature scaling for Hyperblock creation
     * @param weightSlider slider for attribute weight
     * @param fieldName name of feature to be weighted
     * @param scale value of weightSlider
     * @param featureNum index number of feature
     * @param ruleBase datapoint info
     * @param className class of datapoint
     * @param opClassName other class is visualization
     * @param curClass number of current class
     * @param index index of feature
     * @param upper_or_lower whether the current class is the upper or lower graph
     * @param bound feature bounds for given class
     */
    public static JPanel createWeightSliderPanel_GLC(final JSlider weightSlider, String fieldName, int scale, int featureNum, String ruleBase, String className, String opClassName, int curClass, int index, int upper_or_lower, int bound)
    {
        // text for angle
        JTextField angleText = createTextAngleInput(Integer.toString(scale));

        // slider for angle
        weightSlider.setToolTipText("Change weight of visualization");
        weightSlider.setMinorTickSpacing(1);
        weightSlider.setPaintTicks(true);
        weightSlider.setPaintLabels(true);

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
        weightSlider.setLabelTable(position);

        // add listeners for text field and slider
        // action listener for text field
        angleText.addActionListener(e ->
        {
            // get raw value of inputted angle
            int sliderValue = Integer.parseInt(angleText.getText());

            // check if scale is within 0 and 500
            if (sliderValue >= 0 && sliderValue <= 500)
            {
                // check is sliderValue is within limits
                if (bound == 0)
                {
                    if (DV.data.get(curClass).data[index][featureNum] * sliderValue / 100.0 < DV.limits[featureNum][bound])
                        return;
                }
                else
                {
                    if (DV.data.get(curClass).data[index][featureNum] * sliderValue / 100.0 > DV.limits[featureNum][bound])
                        return;
                }

                try
                {
                    // set slider to new value
                    weightSlider.setValue(sliderValue);

                    // set angle label to new value
                    if (bound == 0)
                        angleLabel.setText(fieldName + " Lower Scale: " + sliderValue + "%");
                    else
                        angleLabel.setText(fieldName + " Upper Scale: " + sliderValue + "%");

                    // set angle to new value
                    DV.scale[bound][featureNum] = sliderValue / 100.0;
                }
                catch (NumberFormatException nfe)
                {
                    System.err.println("Illegal Input");
                }

                // redraw
                DataVisualization.drawLDFRule(ruleBase, className, opClassName, curClass, index);
                DataVisualization.drawLDF(curClass, index, upper_or_lower, ruleBase, className, opClassName);
            }
        });

        // change listener for slider
        weightSlider.addChangeListener(e ->
        {
            // get new angle
            int sliderValue = weightSlider.getValue();

            // check is sliderValue is within limits
            if (bound == 0)
            {
                if (DV.data.get(curClass).data[index][featureNum] * sliderValue / 100.0 < DV.limits[featureNum][bound])
                {
                    weightSlider.setValue((int)(DV.limits[featureNum][bound] / DV.data.get(curClass).data[index][featureNum] * 100));
                    sliderValue = weightSlider.getValue();
                }
            }
            else
            {
                if (DV.data.get(curClass).data[index][featureNum] * sliderValue / 100.0 > DV.limits[featureNum][bound])
                {
                    weightSlider.setValue((int)(DV.limits[featureNum][bound] / DV.data.get(curClass).data[index][featureNum] * 100));
                    sliderValue = weightSlider.getValue();
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
                DV.scale[bound][featureNum] = sliderValue / 100.0;
            }
            catch (NumberFormatException nfe)
            {
                System.err.println("Illegal Input");
            }

            // redraw
            DataVisualization.drawLDFRule(ruleBase, className, opClassName, curClass, index);
            DataVisualization.drawLDF(curClass, index, upper_or_lower, ruleBase, className, opClassName);
        });

        // add to angle slider panel
        return createAnglePanel(weightSlider, angleLabel, angleText);
    }


    /**
     * Creates input textbox to change visualization angles
     * @param text initial angle
     * @return textbox
     */
    private static JTextField createTextAngleInput(String text)
    {
        JTextField angleText = new JTextField(4);
        angleText.setToolTipText("Change weight of visualization");
        angleText.setFont(angleText.getFont().deriveFont(12f));
        angleText.setText(text);

        return angleText;
    }


    /**
     * Creates panel which holds a slider and a text input box
     * @param angleSlider slider to adjust angle
     * @param angleLabel current angle
     * @param angleText text input box to adjust angle
     * @return panel holding slider and text input box
     */
    private static JPanel createAnglePanel(JSlider angleSlider, JLabel angleLabel, JTextField angleText)
    {
        // panel for angle
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

        return anglePanel;
    }
}
