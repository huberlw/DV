package Sliders;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.basic.BasicSliderUI;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

public class RangeSliderUI extends BasicSliderUI
{
    // colors for track and thumbs
    private final Color trackColor;
    private final Color leftThumbColor;
    private final Color rightThumbColor;

    // shape of track
    private final RoundRectangle2D.Float trackShape = new RoundRectangle2D.Float();

    // upper thumb
    private Rectangle upperThumbRect;

    // indicator for thumb drag
    private boolean lowerDragging;
    private boolean upperDragging;

    // indicator for last thumb selected: true = upper, false = lower
    private boolean upperThumbSelected;


    /**
     * Constructs a RangeSliderUI for the specified slider component.
     * @param rs RangeSlider
     */
    public RangeSliderUI(RangeSlider rs, Color track, Color left, Color right)
    {
        super(rs);
        trackColor = track;
        leftThumbColor = left;
        rightThumbColor = right;
    }


    /**
     * Installs this UI delegate on the specified component.
     */
    @Override
    public void installUI(JComponent c)
    {
        upperThumbRect = new Rectangle();
        super.installUI(c);
    }


    /**
     * Creates a listener to handle track events in the specified slider.
     */
    @Override
    protected TrackListener createTrackListener(JSlider slider)
    {
        return new RangeTrackListener();
    }


    /**
     * Creates a listener to handle change events in the specified slider.
     */
    @Override
    protected ChangeListener createChangeListener(JSlider slider)
    {
        return new ChangeHandler();
    }


    /**
     * Calculates the track size and position
     */
    @Override
    protected void calculateTrackRect()
    {
        super.calculateTrackRect();

        trackRect.y = trackRect.y + (trackRect.height - 8) / 2;
        trackRect.height = 8;

        trackShape.setRoundRect(trackRect.x, trackRect.y, trackRect.width, trackRect.height, 5, 5);
    }


    /**
     * Updates the dimensions for both thumbs.
     */
    @Override
    protected void calculateThumbSize()
    {
        // Call superclass method for lower thumb size.
        super.calculateThumbSize();

        // Set upper thumb size.
        upperThumbRect.setSize(thumbRect.width, thumbRect.height);
    }


    /**
     * Updates the locations for both thumbs.
     */
    @Override
    protected void calculateThumbLocation()
    {
        // call superclass method for lower thumb location.
        super.calculateThumbLocation();
        thumbRect.y = trackRect.y + (trackRect.height - thumbRect.height) / 2;

        // calculate upper thumb location
        int upperPosition = xPositionForValue(slider.getValue() + slider.getExtent());
        upperThumbRect.x = upperPosition - (upperThumbRect.width / 2);
        upperThumbRect.y = trackRect.y + (trackRect.height - thumbRect.height) / 2;
    }


    /**
     * Returns the size of a thumb.
     */
    @Override
    protected Dimension getThumbSize()
    {
        return new Dimension(20, 20);
    }


    /**
     * Sets the location of the upper thumb, and repaints the slider
     */
    private void setUpperThumbLocation(int x, int y)
    {
        Rectangle upperUnionRect = new Rectangle(upperThumbRect);
        upperThumbRect.setLocation(x, y);
        SwingUtilities.computeUnion(upperThumbRect.x, upperThumbRect.y, upperThumbRect.width, upperThumbRect.height, upperUnionRect);

        slider.repaint(upperUnionRect);
    }


    /**
     * Paints the slider.  The selected thumb is always painted on top of the
     * other thumb.
     */
    @Override
    public void paint(Graphics g, JComponent c)
    {
        // apply antialiasing
        ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        super.paint(g, c);

        Rectangle clipRect = g.getClipBounds();

        // if true, draw upper thumb first
        if (upperThumbSelected)
        {
            if (clipRect.intersects(thumbRect))
                paintLowerThumb(g);

            if (clipRect.intersects(upperThumbRect))
                paintUpperThumb(g);
        }
        else
        {
            if (clipRect.intersects(upperThumbRect))
                paintUpperThumb(g);

            if (clipRect.intersects(thumbRect))
                paintLowerThumb(g);
        }
    }


    /**
     * Paints the track.
     */
    @Override
    public void paintTrack(Graphics g)
    {
        // save track size
        Graphics2D g2 = (Graphics2D) g;
        Shape clip = g2.getClip();

        // Paint track grey
        g2.setColor(new Color(200, 200 ,200));
        g2.fill(trackShape);

        // paint selected track
        int lowerThumbPos = thumbRect.x + thumbRect.width / 2;
        int width = (upperThumbRect.x + upperThumbRect.width / 2) - lowerThumbPos;
        g2.clipRect(lowerThumbPos, 0, width, slider.getHeight());
        g2.setColor(trackColor);
        g2.fill(trackShape);

        // reset clip to full track
        g2.setClip(clip);
    }


    /**
     * Override to do nothing. Thumbs painted by paint method.
     */
    @Override
    public void paintThumb(Graphics g) {}


    /**
     * Paints upper thumb.
     */
    public void paintLowerThumb(Graphics g)
    {
        g.setColor(leftThumbColor);
        g.fillOval(thumbRect.x, thumbRect.y, thumbRect.width, thumbRect.height);
    }


    /**
     * Paints lower thumb.
     */
    public void paintUpperThumb(Graphics g)
    {
        g.setColor(rightThumbColor);
        g.fillOval(upperThumbRect.x, upperThumbRect.y, upperThumbRect.width, upperThumbRect.height);
    }


    /**
     * Listener to handle model change events.  This calculates the thumb
     * locations and repaints the slider if the value change is not caused by
     * dragging a thumb.
     */
    public class ChangeHandler implements ChangeListener
    {
        public void stateChanged(ChangeEvent arg0)
        {
            if (!lowerDragging && !upperDragging)
            {
                calculateThumbLocation();
                slider.repaint();
            }
        }
    }


    /**
     * Listener to handle mouse movements in the slider track.
     */
    public class RangeTrackListener extends TrackListener
    {
        @Override
        public void mousePressed(MouseEvent e)
        {
            currentMouseX = e.getX();
            currentMouseY = e.getY();

            if (slider.isRequestFocusEnabled())
                slider.requestFocus();


            // determine which thumb is pressed
            boolean lowerPressed = thumbRect.contains(currentMouseX, currentMouseY);
            boolean upperPressed = upperThumbRect.contains(currentMouseX, currentMouseY);

            // if overlapping, pick one
            if (lowerPressed && upperPressed)
            {
                int halfWayPoint = (trackRect.width + trackRect.width) / 2;

                if (currentMouseX < halfWayPoint)
                    lowerPressed = false;
                else
                    upperPressed = false;
            }

            // get offset and set dragging to true
            if (upperPressed)
            {
                offset = currentMouseX - upperThumbRect.x;
                upperDragging = true;
                upperThumbSelected = true;
                return;
            }

            upperDragging = false;

            if (lowerPressed)
            {
                offset = currentMouseX - thumbRect.x;
                lowerDragging = true;
                upperThumbSelected = false;
                return;
            }

            lowerDragging = false;
        }

        @Override
        public void mouseReleased(MouseEvent e)
        {
            lowerDragging = false;
            upperDragging = false;
            slider.setValueIsAdjusting(false);
            super.mouseReleased(e);
        }

        @Override
        public void mouseDragged(MouseEvent e)
        {
            currentMouseX = e.getX();
            currentMouseY = e.getY();

            if (lowerDragging)
            {
                slider.setValueIsAdjusting(true);
                moveLowerThumb();

            }
            else if (upperDragging)
            {
                slider.setValueIsAdjusting(true);
                moveUpperThumb();
            }
        }


        /**
         * Moves the location of the lower thumb, and sets its corresponding
         * value in the slider.
         */
        private void moveLowerThumb()
        {
            int halfThumbWidth = thumbRect.width / 2;
            int thumbLeft = currentMouseX - offset;
            int trackLeft = trackRect.x;
            int trackRight = xPositionForValue(slider.getValue() + slider.getExtent());

            thumbLeft = Math.max(thumbLeft, trackLeft - halfThumbWidth);
            thumbLeft = Math.min(thumbLeft, trackRight - halfThumbWidth);
            setThumbLocation(thumbLeft, thumbRect.y);

            // Update slider value.
            int thumbMiddle = thumbLeft + halfThumbWidth;
            slider.setValue(valueForXPosition(thumbMiddle));
        }


        /**
         * Moves the location of the upper thumb, and sets its corresponding
         * value in the slider.
         */
        private void moveUpperThumb()
        {
            int halfThumbWidth = upperThumbRect.width / 2;
            int thumbLeft = currentMouseX - offset;
            int trackLeft = xPositionForValue(slider.getValue());
            int trackRight = trackRect.x + (trackRect.width - 1);

            thumbLeft = Math.max(thumbLeft, trackLeft - halfThumbWidth);
            thumbLeft = Math.min(thumbLeft, trackRight - halfThumbWidth);
            setUpperThumbLocation(thumbLeft, upperThumbRect.y);

            // Update slider extent.
            int thumbMiddle = thumbLeft + halfThumbWidth;
            slider.setExtent(valueForXPosition(thumbMiddle) - slider.getValue());
        }
    }
}
