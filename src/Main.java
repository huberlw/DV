import javax.swing.*;
import java.awt.*;

public class Main
{
    /**
     * Main method of the program.
     * Will create the main UI components and manage their operation
     * @param args standard main method command line arguments.
     */
    public static void main(String[] args) throws Exception
    {
        // set look and feel
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

        if(screenSize.getWidth() >= 1280)
        {
            if(screenSize.getWidth() >= 1920)
                Resolutions.setResolution(0);
            else
                Resolutions.setResolution(1);
        }

        // start DV program
        DV dv = new DV();
        dv.setVisible(true);
    }
}
