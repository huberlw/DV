import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicInteger;


public class ColorMenu extends JPanel
{
    /**
     * Creates menu that allows the color customization of the domain, overlap,
     * threshold background, graph lines, endpoints, svm lines, and svm endpoints.
     */
    public ColorMenu()
    {
        // current component being changed
        AtomicInteger colorOption = new AtomicInteger(0);

        // color panel
        JPanel colors = new JPanel();
        colors.setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();

        JButton[] colorOptions = new JButton[9];


        // set domain line color
        colorOptions[0] = colorChangeButton(
                "Domain Line Color",
                "Sets color of subset of utilized data lines",
                DV.domainLines);
        colorOptions[0].setBackground(Color.LIGHT_GRAY);
        colorOptions[0].addMouseListener(highlightButton(colorOptions, colorOption, 0));

        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        colors.add(colorOptions[0], constraints);


        // set overlap line color
        colorOptions[1] = colorChangeButton(
                "Overlap Line Color",
                "Sets color of overlap lines",
                DV.overlapLines);
        colorOptions[1].addMouseListener(highlightButton(colorOptions, colorOption, 1));

        constraints.gridy = 1;
        colors.add(colorOptions[1], constraints);


        // set threshold line color
        colorOptions[2] = colorChangeButton(
                "Threshold Line Color",
                "Sets color of threshold line",
                DV.thresholdLine);
        colorOptions[2].addMouseListener(highlightButton(colorOptions, colorOption, 2));

        constraints.gridy = 2;
        colors.add(colorOptions[2], constraints);


        // set background color
        colorOptions[3] = colorChangeButton(
                "Background Color",
                "Sets color of graph background",
                DV.background);
        colorOptions[3].addMouseListener(highlightButton(colorOptions, colorOption, 3));

        constraints.gridy = 3;
        colors.add(colorOptions[3], constraints);


        // set upper graph color
        colorOptions[4] = colorChangeButton(
                "Upper Graph Color",
                "Sets color of upperGraph",
                DV.graphColors[0]);
        colorOptions[4].addMouseListener(highlightButton(colorOptions, colorOption, 4));

        constraints.gridy = 4;
        colors.add(colorOptions[4], constraints);


        // set lower graph color
        colorOptions[5] = colorChangeButton(
                "Lower Graph Color",
                "Sets color of lower graph",
                DV.graphColors[1]);
        colorOptions[5].addMouseListener(highlightButton(colorOptions, colorOption, 5));

        constraints.gridy = 5;
        colors.add(colorOptions[5], constraints);


        // set endpoint color
        colorOptions[6] = colorChangeButton(
                "Endpoint Color",
                "Sets color of endpoints for upper and lower graphs",
                DV.endpoints);
        colorOptions[6].addMouseListener(highlightButton(colorOptions, colorOption, 6));

        constraints.gridy = 6;
        colors.add(colorOptions[6], constraints);


        // set lower graph color
        colorOptions[7] = colorChangeButton(
                "SVM Color",
                "Sets color of support vectors when drawn.",
                DV.svmLines);
        colorOptions[7].addMouseListener(highlightButton(colorOptions, colorOption, 7));

        constraints.gridy = 7;
        colors.add(colorOptions[7], constraints);


        // set lower graph color
        colorOptions[8] = colorChangeButton(
                "SVM Endpoint Color",
                "Sets color of support vectors endpoints when drawn.",
                DV.svmEndpoints);
        colorOptions[8].addMouseListener(highlightButton(colorOptions, colorOption, 8));

        constraints.gridy = 8;
        colors.add(colorOptions[8], constraints);


        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.gridheight = 10;
        constraints.fill = GridBagConstraints.VERTICAL;
        JColorChooser colorChooser = new JColorChooser();
        colors.add(colorChooser, constraints);


        constraints.gridx = 1;
        constraints.gridy = 10;
        constraints.gridheight = 1;
        constraints.gridwidth = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        colors.add(applyButton(colorChooser, colorOption, colorOptions), constraints);


        // display color menu
        int choice = JOptionPane.showConfirmDialog(DV.mainFrame, colors, "Color Options", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (choice == 0)
        {
            Color newColor = colorChooser.getColor();

            if (newColor != null)
            {
                int component = colorOption.get();
                setColor(component, newColor);
                colorOptions[component].setIcon(createIcon(newColor));
            }
        }
    }


    /**
     * Button which selects a DV feature to change its color
     * @param label label for button
     * @param tooltip tooltip for button
     * @param icon_color current color
     * @return button which selects a DV feature
     */
    private JButton colorChangeButton(String label, String tooltip, Color icon_color)
    {
        JButton colorBtn = new JButton(label);
        colorBtn.setToolTipText(tooltip);
        colorBtn.setBorderPainted(false);
        colorBtn.setFocusPainted(false);
        colorBtn.setHorizontalAlignment(SwingConstants.LEFT);
        colorBtn.setFont(colorBtn.getFont().deriveFont(12f));
        colorBtn.setIcon(createIcon(icon_color));

        return colorBtn;
    }


    /**
     * MouseListener which highlights the current button when clicked or hovered over
     * @param colorOptions all color options for DV
     * @param colorOption previously selected color option
     * @param cur current color option
     * @return MouseListener which highlights the current button
     */
    private MouseListener highlightButton(JButton[] colorOptions, AtomicInteger colorOption, int cur)
    {
        return new MouseListener()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                colorOptions[colorOption.get()].setBackground(UIManager.getColor("control"));
                colorOption.set(cur);
                colorOptions[cur].setBackground(Color.LIGHT_GRAY);
            }

            @Override
            public void mousePressed(MouseEvent e) {}

            @Override
            public void mouseReleased(MouseEvent e) {}

            @Override
            public void mouseEntered(MouseEvent e)
            {
                colorOptions[cur].setBackground(new Color(218,218,218));
            }

            @Override
            public void mouseExited(MouseEvent e)
            {
                if (colorOption.get() == cur)
                    colorOptions[cur].setBackground(Color.LIGHT_GRAY);
                else
                    colorOptions[cur].setBackground(UIManager.getColor("control"));
            }
        };
    }


    /**
     * Button that applies a color to a component
     * @param colorChooser color chooser
     * @param colorOption current DV component
     * @param colorOptions all DV components
     * @return Button that applies the selected color to the selected component
     */
    private JButton applyButton(JColorChooser colorChooser, AtomicInteger colorOption, JButton[] colorOptions)
    {
        JButton applyBtn = new JButton("Apply Color");
        applyBtn.setToolTipText("Applies the selected color to the selected graph component.");
        applyBtn.setFont(applyBtn.getFont().deriveFont(Font.BOLD, 12f));
        applyBtn.addActionListener(e ->
        {
            Color newColor = colorChooser.getColor();

            if (newColor != null)
            {
                int component = colorOption.get();
                setColor(component, newColor);
                colorOptions[component].setIcon(createIcon(newColor));
            }
        });

        return applyBtn;
    }


    /**
     * Sets component to new color
     * @param component component number
     * @param newColor new color of component
     */
    private void setColor(int component, Color newColor)
    {
        // sets component to newColor
        switch(component)
        {
            case 0 -> DV.domainLines = newColor;
            case 1 -> DV.overlapLines = newColor;
            case 2 -> DV.thresholdLine = newColor;
            case 3 -> DV.background = newColor;
            case 4 -> DV.graphColors[0] = newColor;
            case 5 -> DV.graphColors[1] = newColor;
            case 6 -> DV.endpoints = newColor;
            case 7 -> DV.svmLines = newColor;
            case 8 -> DV.svmEndpoints = newColor;
        }

        // redraw graphs
        if (DV.trainData != null)
            DataVisualization.drawGraphs();
    }


    /**
     * Creates square icon for a given color
     * @param color color of icon
     * @return 16 x 16 pixel icon of a given color
     */
    private ImageIcon createIcon(Color color)
    {
        BufferedImage icon = new BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = icon.createGraphics();
        graphics.setPaint(color);
        graphics.fillRect(0, 0, 16, 16);

        return new ImageIcon(icon);
    }
}
