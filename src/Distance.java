public class Distance
{
    private double euclidean(double[] x, double[] y)
    {
        double dist = 0;

        for (int i = 0; i < x.length; i++)
        {
            double coef = Math.cos(Math.toRadians(DV.angles[i]));
            dist += Math.pow((Math.pow(x[i], 2) - Math.pow(y[i], 2)) * coef, 2);
        }

        return Math.sqrt(dist);
    }

    private double[] lossless(double[] x, double[] y)
    {
        double[] dist = new double[x.length];

        for (int i = 0; i < x.length; i++)
        {
            dist[i] = Math.abs(x[i] - y[i]);
        }

        return dist;
    }
}

