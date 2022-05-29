package Sliders;

import javax.swing.*;
import javax.swing.plaf.basic.BasicSliderUI;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

public class ThresholdSliderUI extends BasicSliderUI
{
    private final RoundRectangle2D.Float trackShape = new RoundRectangle2D.Float();

    public ThresholdSliderUI(final JSlider b)
    {
        super(b);
    }

    @Override
    protected void calculateTrackRect()
    {
        super.calculateTrackRect();

        trackRect.y = trackRect.y + (trackRect.height - 8) / 2;
        trackRect.height = 8;

        trackShape.setRoundRect(trackRect.x, trackRect.y, trackRect.width, trackRect.height, 5, 5);
    }

    @Override
    protected void calculateThumbLocation()
    {
        super.calculateThumbLocation();
        thumbRect.y = trackRect.y + (trackRect.height - thumbRect.height) / 2;
    }

    @Override
    protected Dimension getThumbSize()
    {
        return new Dimension(20, 20);
    }

    @Override
    public void paint(final Graphics g, final JComponent c)
    {
        // apply antialiasing
        ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        super.paint(g, c);
    }

    @Override
    public void paintTrack(final Graphics g)
    {
        // save track size
        Graphics2D g2 = (Graphics2D) g;
        Shape clip = g2.getClip();

        // Paint track grey
        g2.setColor(new Color(200, 200 ,200));
        g2.fill(trackShape);

        // Paint selected track green
        int thumbPos = thumbRect.x + thumbRect.width / 2;
        g2.clipRect(0, 0, thumbPos, slider.getHeight());
        g2.setColor(new Color(0, 200 ,0));
        g2.fill(trackShape);

        // reset clip to full track
        g2.setClip(clip);
    }

    @Override
    public void paintThumb(final Graphics g)
    {
        g.setColor(Color.GREEN);
        g.fillOval(thumbRect.x, thumbRect.y, thumbRect.width, thumbRect.height);
    }
}
