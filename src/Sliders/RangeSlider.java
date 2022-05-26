package Sliders;

import javax.swing.JSlider;
import java.awt.*;

/**
 * An extension of JSlider to select a range of values using two thumb controls.
 * The thumb controls are used to select the lower and upper value of a range
 * with predetermined minimum and maximum values.
 *
 * <p>Note that RangeSlider makes use of the default BoundedRangeModel, which
 * supports an inner range defined by a value and an extent.  The upper value
 * returned by RangeSlider is simply the lower value plus the extent.</p>
 */
public class RangeSlider extends JSlider
{
    private final Color trackColor;
    private final Color leftThumbColor;
    private final Color rightThumbColor;

    /**
     * Constructs a RangeSlider with default minimum and maximum values of 0
     * and 100 and a specified color.
     */
    public RangeSlider(Color track, Color left, Color right)
    {
        this.trackColor = track;
        this.leftThumbColor = left;
        this.rightThumbColor = right;
        initSlider();
    }

    /**
     * Initializes the slider by setting default properties.
     */
    private void initSlider()
    {
        setOrientation(HORIZONTAL);
    }

    /**
     * Overrides the superclass method to install the UI delegate to draw two
     * thumbs.
     */
    @Override
    public void updateUI()
    {
        setUI(new RangeSliderUI(this, trackColor, leftThumbColor, rightThumbColor));
        updateLabelUIs();
    }

    /**
     * Returns the lower value in the range.
     */
    @Override
    public int getValue()
    {
        return super.getValue();
    }

    /**
     * Sets the lower value in the range.
     */
    @Override
    public void setValue(int value)
    {
        getModel().setRangeProperties(value, getUpperValue() - value, getMinimum(),
                getMaximum(), getValueIsAdjusting());
    }

    /**
     * Returns the upper value in the range.
     */
    public int getUpperValue()
    {
        return getValue() + getExtent();
    }

    /**
     * Sets the upper value in the range.
     */
    public void setUpperValue(int value)
    {
        // Compute new extent.
        int lowerValue = getValue();
        int newExtent = value - lowerValue;

        // Set extent to set upper value.
        setExtent(newExtent);
    }
}
