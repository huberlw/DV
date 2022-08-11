import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicInteger;


public class ColorMenu extends JPanel
{
    public ColorMenu()
    {
        // current component being changed
        AtomicInteger colorOption = new AtomicInteger(0);

        // color panel
        JPanel colors = new JPanel();
        colors.setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();

        JButton[] colorOptions = new JButton[8];

        // set domain line color
        colorOptions[0] = new JButton("Domain Line Color");
        colorOptions[0].setToolTipText("Sets color of subset of utilized data lines");
        colorOptions[0].setBorderPainted(false);
        colorOptions[0].setFocusPainted(false);
        colorOptions[0].setHorizontalAlignment(SwingConstants.LEFT);
        colorOptions[0].setFont(colorOptions[0].getFont().deriveFont(12f));
        colorOptions[0].setBackground(Color.LIGHT_GRAY);
        colorOptions[0].setIcon(createIcon(DV.domainLines));
        colorOptions[0].addMouseListener(new MouseListener()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                colorOptions[colorOption.get()].setBackground(UIManager.getColor("control"));
                colorOption.set(0);
                colorOptions[0].setBackground(Color.LIGHT_GRAY);
            }

            @Override
            public void mousePressed(MouseEvent e) {}

            @Override
            public void mouseReleased(MouseEvent e) {}

            @Override
            public void mouseEntered(MouseEvent e)
            {
                colorOptions[0].setBackground(new Color(218,218,218));
            }

            @Override
            public void mouseExited(MouseEvent e)
            {
                if (colorOption.get() == 0)
                    colorOptions[0].setBackground(Color.LIGHT_GRAY);
                else
                    colorOptions[0].setBackground(UIManager.getColor("control"));
            }
        });

        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        colors.add(colorOptions[0], constraints);

        // set overlap line color
        colorOptions[1] = new JButton("Overlap Line Color");
        colorOptions[1].setToolTipText("Sets color of overlap lines");
        colorOptions[1].setBorderPainted(false);
        colorOptions[1].setFocusPainted(false);
        colorOptions[1].setHorizontalAlignment(SwingConstants.LEFT);
        colorOptions[1].setFont(colorOptions[1].getFont().deriveFont(12f));
        colorOptions[1].setIcon(createIcon(DV.overlapLines));
        colorOptions[1].addMouseListener(new MouseListener()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                colorOptions[colorOption.get()].setBackground(UIManager.getColor("control"));
                colorOption.set(1);
                colorOptions[1].setBackground(Color.LIGHT_GRAY);
            }

            @Override
            public void mousePressed(MouseEvent e) {}

            @Override
            public void mouseReleased(MouseEvent e) {}

            @Override
            public void mouseEntered(MouseEvent e)
            {
                colorOptions[1].setBackground(new Color(218,218,218));
            }

            @Override
            public void mouseExited(MouseEvent e)
            {
                if (colorOption.get() == 1)
                    colorOptions[1].setBackground(Color.LIGHT_GRAY);
                else
                    colorOptions[1].setBackground(UIManager.getColor("control"));
            }
        });

        constraints.gridy = 1;
        colors.add(colorOptions[1], constraints);

        // set threshold line color
        colorOptions[2] = new JButton("Threshold Line Color");
        colorOptions[2].setToolTipText("Sets color of threshold line");
        colorOptions[2].setBorderPainted(false);
        colorOptions[2].setFocusPainted(false);
        colorOptions[2].setHorizontalAlignment(SwingConstants.LEFT);
        colorOptions[2].setFont(colorOptions[2].getFont().deriveFont(12f));
        colorOptions[2].setIcon(createIcon(DV.thresholdLine));
        colorOptions[2].addMouseListener(new MouseListener()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                colorOptions[colorOption.get()].setBackground(UIManager.getColor("control"));
                colorOption.set(2);
                colorOptions[2].setBackground(Color.LIGHT_GRAY);
            }

            @Override
            public void mousePressed(MouseEvent e) {}

            @Override
            public void mouseReleased(MouseEvent e) {}

            @Override
            public void mouseEntered(MouseEvent e)
            {
                colorOptions[2].setBackground(new Color(218,218,218));
            }

            @Override
            public void mouseExited(MouseEvent e)
            {
                if (colorOption.get() == 2)
                    colorOptions[2].setBackground(Color.LIGHT_GRAY);
                else
                    colorOptions[2].setBackground(UIManager.getColor("control"));
            }
        });

        constraints.gridy = 2;
        colors.add(colorOptions[2], constraints);

        // set background color
        colorOptions[3] = new JButton("Background Color");
        colorOptions[3].setToolTipText("Sets color of graph background");
        colorOptions[3].setBorderPainted(false);
        colorOptions[3].setFocusPainted(false);
        colorOptions[3].setHorizontalAlignment(SwingConstants.LEFT);
        colorOptions[3].setFont(colorOptions[3].getFont().deriveFont(12f));
        colorOptions[3].setIcon(createIcon(DV.background));
        colorOptions[3].addMouseListener(new MouseListener()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                colorOptions[colorOption.get()].setBackground(UIManager.getColor("control"));
                colorOption.set(3);
                colorOptions[3].setBackground(Color.LIGHT_GRAY);
            }

            @Override
            public void mousePressed(MouseEvent e) {}

            @Override
            public void mouseReleased(MouseEvent e) {}

            @Override
            public void mouseEntered(MouseEvent e)
            {
                colorOptions[3].setBackground(new Color(218,218,218));
            }

            @Override
            public void mouseExited(MouseEvent e)
            {
                if (colorOption.get() == 3)
                    colorOptions[3].setBackground(Color.LIGHT_GRAY);
                else
                    colorOptions[3].setBackground(UIManager.getColor("control"));
            }
        });

        constraints.gridy = 3;
        colors.add(colorOptions[3], constraints);

        // set upper graph color
        colorOptions[4] = new JButton("Upper Graph Color");
        colorOptions[4].setToolTipText("Sets color of upperGraph");
        colorOptions[4].setBorderPainted(false);
        colorOptions[4].setFocusPainted(false);
        colorOptions[4].setHorizontalAlignment(SwingConstants.LEFT);
        colorOptions[4].setFont(colorOptions[4].getFont().deriveFont(12f));
        colorOptions[4].setIcon(createIcon(DV.graphColors[0]));
        colorOptions[4].addMouseListener(new MouseListener()
                {
                    @Override
                    public void mouseClicked(MouseEvent e)
                    {
                        colorOptions[colorOption.get()].setBackground(UIManager.getColor("control"));
                        colorOption.set(4);
                        colorOptions[4].setBackground(Color.LIGHT_GRAY);
                    }

                    @Override
                    public void mousePressed(MouseEvent e) {}

                    @Override
                    public void mouseReleased(MouseEvent e) {}

                    @Override
                    public void mouseEntered(MouseEvent e)
                    {
                        colorOptions[4].setBackground(new Color(218,218,218));
                    }

                    @Override
                    public void mouseExited(MouseEvent e)
                    {
                        if (colorOption.get() == 4)
                            colorOptions[4].setBackground(Color.LIGHT_GRAY);
                        else
                            colorOptions[4].setBackground(UIManager.getColor("control"));
                    }
                });

        constraints.gridy = 4;
        colors.add(colorOptions[4], constraints);

        // set lower graph color
        colorOptions[5] = new JButton("Lower Graph Color");
        colorOptions[5].setToolTipText("Sets color of lower graph");
        colorOptions[5].setBorderPainted(false);
        colorOptions[5].setFocusPainted(false);
        colorOptions[5].setHorizontalAlignment(SwingConstants.LEFT);
        colorOptions[5].setFont(colorOptions[5].getFont().deriveFont(12f));
        colorOptions[5].setIcon(createIcon(DV.graphColors[1]));
        colorOptions[5].addMouseListener(new MouseListener()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                colorOptions[colorOption.get()].setBackground(UIManager.getColor("control"));
                colorOption.set(5);
                colorOptions[5].setBackground(Color.LIGHT_GRAY);
            }

            @Override
            public void mousePressed(MouseEvent e) {}

            @Override
            public void mouseReleased(MouseEvent e) {}

            @Override
            public void mouseEntered(MouseEvent e)
            {
                colorOptions[5].setBackground(new Color(218,218,218));
            }

            @Override
            public void mouseExited(MouseEvent e)
            {
                if (colorOption.get() == 5)
                    colorOptions[5].setBackground(Color.LIGHT_GRAY);
                else
                    colorOptions[5].setBackground(UIManager.getColor("control"));
            }
        });

        constraints.gridy = 5;
        colors.add(colorOptions[5], constraints);

        // set endpoint color
        colorOptions[6] = new JButton("Endpoint Color");
        colorOptions[6].setToolTipText("Sets color of endpoints for upper and lower graphs");
        colorOptions[6].setBorderPainted(false);
        colorOptions[6].setFocusPainted(false);
        colorOptions[6].setHorizontalAlignment(SwingConstants.LEFT);
        colorOptions[6].setFont(colorOptions[6].getFont().deriveFont(12f));
        colorOptions[6].setIcon(createIcon(DV.endpoints));
        colorOptions[6].addMouseListener(new MouseListener()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                colorOptions[colorOption.get()].setBackground(UIManager.getColor("control"));
                colorOption.set(6);
                colorOptions[6].setBackground(Color.LIGHT_GRAY);
            }

            @Override
            public void mousePressed(MouseEvent e) {}

            @Override
            public void mouseReleased(MouseEvent e) {}

            @Override
            public void mouseEntered(MouseEvent e)
            {
                colorOptions[6].setBackground(new Color(218,218,218));
            }

            @Override
            public void mouseExited(MouseEvent e)
            {
                if (colorOption.get() == 6)
                    colorOptions[6].setBackground(Color.LIGHT_GRAY);
                else
                    colorOptions[6].setBackground(UIManager.getColor("control"));
            }
        });

        constraints.gridy = 6;
        colors.add(colorOptions[6], constraints);

        // set lower graph color
        colorOptions[7] = new JButton("SVM Color");
        colorOptions[7].setToolTipText("Sets color of support vectors when drawn.");
        colorOptions[7].setBorderPainted(false);
        colorOptions[7].setFocusPainted(false);
        colorOptions[7].setHorizontalAlignment(SwingConstants.LEFT);
        colorOptions[7].setFont(colorOptions[7].getFont().deriveFont(12f));
        colorOptions[7].setIcon(createIcon(DV.svmLines));
        colorOptions[7].addMouseListener(new MouseListener()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                colorOptions[colorOption.get()].setBackground(UIManager.getColor("control"));
                colorOption.set(7);
                colorOptions[7].setBackground(Color.LIGHT_GRAY);
            }

            @Override
            public void mousePressed(MouseEvent e) {}

            @Override
            public void mouseReleased(MouseEvent e) {}

            @Override
            public void mouseEntered(MouseEvent e)
            {
                colorOptions[7].setBackground(new Color(218,218,218));
            }

            @Override
            public void mouseExited(MouseEvent e)
            {
                if (colorOption.get() == 7)
                    colorOptions[7].setBackground(Color.LIGHT_GRAY);
                else
                    colorOptions[7].setBackground(UIManager.getColor("control"));
            }
        });

        constraints.gridy = 7;
        colors.add(colorOptions[7], constraints);

        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.gridheight = 9;
        constraints.fill = GridBagConstraints.VERTICAL;
        JColorChooser colorChooser = new JColorChooser();
        colors.add(colorChooser, constraints);

        JButton applyBtn = new JButton("Apply Color");
        applyBtn.setToolTipText("Applies the selected color to the selected graph component.");
        applyBtn.setFont(applyBtn.getFont().deriveFont(Font.BOLD, 12f));

        applyBtn.addActionListener(e ->
        {
            Color newColor = colorChooser.getColor();

            if (newColor != null)
            {
                switch(colorOption.get())
                {
                    case 0 ->
                    {
                        DV.domainLines = newColor;
                        colorOptions[0].setIcon(createIcon(newColor));
                    }
                    case 1 ->
                    {
                        DV.overlapLines = newColor;
                        colorOptions[1].setIcon(createIcon(newColor));
                    }
                    case 2 ->
                    {
                        DV.thresholdLine = newColor;
                        colorOptions[2].setIcon(createIcon(newColor));
                    }
                    case 3 ->
                    {
                        DV.background = newColor;
                        colorOptions[3].setIcon(createIcon(newColor));
                    }
                    case 4 ->
                    {
                        DV.graphColors[0] = newColor;
                        colorOptions[4].setIcon(createIcon(newColor));
                    }
                    case 5 ->
                    {
                        DV.graphColors[1] = newColor;
                        colorOptions[5].setIcon(createIcon(newColor));
                    }
                    case 6 ->
                    {
                        DV.endpoints = newColor;
                        colorOptions[6].setIcon(createIcon(newColor));
                    }
                    case 7 ->
                    {
                        DV.svmLines = newColor;
                        colorOptions[7].setIcon(createIcon(newColor));
                    }
                }
            }
        });

        constraints.gridx = 1;
        constraints.gridy = 9;
        constraints.gridheight = 1;
        constraints.gridwidth = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        colors.add(applyBtn, constraints);

        int choice = JOptionPane.showConfirmDialog(DV.mainFrame, colors, "Color Options", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (choice == 0)
        {
            Color newColor = colorChooser.getColor();

            if (newColor != null)
            {
                switch(colorOption.get())
                {
                    case 0 ->
                    {
                        DV.domainLines = newColor;
                        colorOptions[0].setIcon(createIcon(newColor));
                    }
                    case 1 ->
                    {
                        DV.overlapLines = newColor;
                        colorOptions[1].setIcon(createIcon(newColor));
                    }
                    case 2 ->
                    {
                        DV.thresholdLine = newColor;
                        colorOptions[2].setIcon(createIcon(newColor));
                    }
                    case 3 ->
                    {
                        DV.background = newColor;
                        colorOptions[3].setIcon(createIcon(newColor));
                    }
                    case 4 ->
                    {
                        DV.graphColors[0] = newColor;
                        colorOptions[4].setIcon(createIcon(newColor));
                    }
                    case 5 ->
                    {
                        DV.graphColors[1] = newColor;
                        colorOptions[5].setIcon(createIcon(newColor));
                    }
                    case 6 ->
                    {
                        DV.endpoints = newColor;
                        colorOptions[6].setIcon(createIcon(newColor));
                    }
                    case 7 ->
                    {
                        DV.svmLines = newColor;
                        colorOptions[7].setIcon(createIcon(newColor));
                    }
                }
            }
        }
    }


    private ImageIcon createIcon(Color color)
    {
        BufferedImage icon = new BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = icon.createGraphics();
        graphics.setPaint(color);
        graphics.fillRect(0, 0, 16, 16);

        return new ImageIcon(icon);
    }
}
