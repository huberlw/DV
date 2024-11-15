import java.util.ArrayList;

public class HollowBlock
{
    double[] maximums;
    double[] minimums;

    int classNum;
    boolean mergable;

    HollowBlock(double[] max, double[] min, int classNum)
    {
        this.maximums = max;
        this.minimums = min;
        this.classNum = classNum;
        this.mergable = true;
    }
}
