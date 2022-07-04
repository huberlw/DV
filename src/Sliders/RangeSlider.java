package Sliders;

import javax.swing.JSlider;

/**
 * An extension of JSlider to select a range of values using two thumb controls.
 */
public class RangeSlider extends JSlider
{
    /**
     * Constructs a RangeSlider with default minimum and maximum values of 0
     * and 100 and a specified color.
     */
    public RangeSlider()
    {
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
