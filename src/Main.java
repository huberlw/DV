import javax.swing.*;

public class Main
{
    /**
     * Main method of the program.
     * Will create the main UI components and manage their operation
     * @param args standard main method command line arguments.
     */
    public static void main(String[] args) throws Exception
    {
        // set look and feel to current system
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

        // start DV program
        DV dv = new DV();
        dv.setVisible(true);
    }
}
