import java.util.HashMap;
import java.util.Map;
import java.util.function.DoubleUnaryOperator;

import static java.util.Map.entry;

public class FunctionParser
{
    @FunctionalInterface
    interface Expression
    {
        double eval();
    }

    static final Map<String,DoubleUnaryOperator> scalarFunctions = Map.ofEntries(
            entry("sqrt", x -> Math.sqrt(x)),
            entry("sin", x -> Math.sin(Math.toRadians(x))),
            entry("cos", x -> Math.cos(Math.toRadians(x))),
            entry("tan", x -> Math.tan(Math.toRadians(x)))
    );


    private static double dotProduct(double[] x, double[] y)
    {
        double product = 0;

        for (int i = 0; i < x.length; i++)
            product += x[i] * y[i];

        return product;
    }

    private static double norm(double[] vec)
    {
        double norm = 0;

        for (double x : vec)
            norm += Math.pow(x, 2);

        return Math.sqrt(norm);
    }

    private static double[] vAdd(double[] x, double[] y)
    {
        double[] sums = new double[x.length];

        for (int i = 0; i < x.length; i++)
            sums[i] += x[i] + y[i];

        return sums;
    }

    private static double[] vSub(double[] x, double[] y)
    {
        double[] sums = new double[x.length];

        for (int i = 0; i < x.length; i++)
            sums[i] += x[i] - y[i];

        return sums;
    }

    public static Expression parseScalerExpression(String strExp, Map<String, Double> variables)
    {
        return new Object()
        {
            int pos = -1;
            char c;
            String str;

            void nextChar()
            {
                if (++pos < str.length())
                    c = str.charAt(pos);
                else
                    c = ' ';
            }

            boolean operator(char charOp)
            {
                if (c == charOp)
                {
                    nextChar();
                    return true;
                }

                return false;
            }

            Expression parse()
            {
                str = strExp.replaceAll("\\s", "");
                str = str.replaceAll("e", "2.7182818");
                nextChar();

                Expression x = parseAddSub();

                if (pos < str.length())
                    throw new RuntimeException("Error with char: " + c);

                return x;
            }

            Expression parseAddSub()
            {
                Expression x = parseMultDiv();

                while (true)
                {
                    if (operator('+'))
                    {
                        Expression p = x;
                        Expression q = parseMultDiv();
                        x = () -> p.eval() + q.eval();
                    }
                    else if (operator('-'))
                    {
                        Expression p = x;
                        Expression q = parseMultDiv();
                        x = () -> p.eval() - q.eval();
                    }
                    else
                        return x;
                }
            }

            Expression parseMultDiv()
            {
                Expression x = parseNumber();

                while (true)
                {
                    if (operator('*'))
                    {
                        Expression p = x;
                        Expression q = parseNumber();
                        x = () -> p.eval() * q.eval();
                    }
                    else if (operator('/'))
                    {
                        Expression p = x;
                        Expression q = parseNumber();
                        x = () -> p.eval() / q.eval();
                    }
                    else
                        return x;
                }
            }

            Expression parseNumber()
            {
                // check for positive or negative signs
                if (operator('+'))
                    return () -> +parseNumber().eval();
                else if (operator('-'))
                    return () -> -parseNumber().eval();

                //
                Expression x = () -> 0;
                int startPos = pos;

                if (operator('('))
                {
                    x = parseAddSub();

                    if (!operator(')'))
                        throw new RuntimeException("Missing parenthesis: )");
                }
                else if (c >= '0' && c <= '9' || c == '.')
                {
                    while (c >= '0' && c <= '9' || c == '.')
                        nextChar();

                    String tmp = str.substring(startPos, pos);
                    x = () -> Double.parseDouble(tmp);
                }
                else if (c >= 'a' && c <= 'z')
                {
                    while (c >= 'a' && c <= 'z')
                        nextChar();

                    String tool_or_var = str.substring(startPos, pos);

                    if (scalarFunctions.containsKey(tool_or_var))
                    {
                        DoubleUnaryOperator func = scalarFunctions.get(tool_or_var);
                        Expression p = parseNumber();
                        x = () -> func.applyAsDouble(p.eval());
                    }
                    else
                    {
                        x = () -> variables.get(tool_or_var);
                    }
                }

                if (operator('^'))
                {
                    Expression p = x;
                    Expression q = parseNumber();
                    x = () -> Math.pow(p.eval(), q.eval());
                }

                return x;
            }
        }.parse();
    }

    public static Expression parseVectorExpression(String strExp, Map<String, double[]> variables)
    {
        return new Object()
        {
            int pos = -1;
            char c;
            String str;

            void nextChar()
            {
                if (++pos < str.length())
                    c = str.charAt(pos);
                else
                    c = ' ';
            }

            boolean operator(char charOp)
            {
                if (c == charOp)
                {
                    nextChar();
                    return true;
                }

                return false;
            }

            Expression parse()
            {
                str = strExp.replaceAll("\\s", "");
                str = str.replaceAll("e", "2.7182818");
                nextChar();

                Expression x = parseAddSub();

                if (pos < str.length())
                    throw new RuntimeException("Error with char: " + c);

                return x;
            }

            Expression parseAddSub()
            {
                Expression x = parseMultDiv();

                while (true)
                {
                    if (operator('+'))
                    {
                        Expression p = x;
                        Expression q = parseMultDiv();
                        x = () -> p.eval() + q.eval();
                    }
                    else if (operator('-'))
                    {
                        Expression p = x;
                        Expression q = parseMultDiv();
                        x = () -> p.eval() - q.eval();
                    }
                    else
                        return x;
                }
            }

            Expression parseMultDiv()
            {
                Expression x = parseNumber();

                while (true)
                {
                    if (operator('*'))
                    {
                        Expression p = x;
                        Expression q = parseNumber();
                        x = () -> p.eval() * q.eval();
                    }
                    else if (operator('/'))
                    {
                        Expression p = x;
                        Expression q = parseNumber();
                        x = () -> p.eval() / q.eval();
                    }
                    else
                        return x;
                }
            }

            String[] parseVecFunc()
            {
                String varX;
                String varY;

                if (operator('('))
                {
                    if (c >= 'a' && c <= 'z')
                        varX = Character.toString(c);
                    else
                        throw new RuntimeException("Missing variable: x");

                    nextChar();

                    if (c != ',')
                        throw new RuntimeException("Missing comma: ,");

                    nextChar();

                    if (c >= 'a' && c <= 'z')
                        varY = Character.toString(c);
                    else
                        throw new RuntimeException("Missing variable: y");

                    nextChar();

                    if (!operator(')'))
                        throw new RuntimeException("Missing parenthesis: )");

                    return new String[]{ varX, varY };
                }
                else
                    throw new RuntimeException("Missing parenthesis: (");
            }

            String parseVecNormFunc()
            {
                String varX;

                if (operator('('))
                {
                    int startPos = pos;

                    if (c >= 'a' && c <= 'z')
                    {
                        while(c >= 'a' && c <= 'z')
                            nextChar();

                        varX = str.substring(startPos, pos);
                    }
                    else
                        throw new RuntimeException("Missing variable: x");

                    nextChar();

                    if (!operator(')'))
                        throw new RuntimeException("Missing parenthesis: )");

                    return varX;
                }
                else
                    throw new RuntimeException("Missing parenthesis: (");
            }

            String[] parseVecAddSub()
            {
                String[] vectors = parseVecFunc();

                for (int i = 97; i < 123; i++)
                {
                    if (!variables.containsKey(Character.toString(i)))
                    {
                        String newVal = Character.toString(i);
                        nextChar();

                        StringBuilder updatedStr = new StringBuilder(str);
                        updatedStr.replace(pos - 5, pos, newVal);
                        str = updatedStr.toString();
                        pos = pos - 5;

                        return new String[]{ newVal, vectors[0], vectors[1] };
                    }
                }

                throw new RuntimeException("No available variables");
            }

            Expression parseNumber()
            {
                // check for positive or negative signs
                if (operator('+'))
                    return () -> +parseNumber().eval();
                else if (operator('-'))
                    return () -> -parseNumber().eval();

                //
                Expression x = () -> 0;
                int startPos = pos;

                if (operator('('))
                {
                    x = parseAddSub();

                    if (!operator(')'))
                        throw new RuntimeException("Missing parenthesis: )");
                }
                else if (c >= '0' && c <= '9' || c == '.')
                {
                    while (c >= '0' && c <= '9' || c == '.')
                        nextChar();

                    String tmp = str.substring(startPos, pos);
                    x = () -> Double.parseDouble(tmp);
                }
                else if (c >= 'a' && c <= 'z')
                {
                    while (c >= 'a' && c <= 'z')
                        nextChar();

                    String tool_or_var = str.substring(startPos, pos);

                    if (scalarFunctions.containsKey(tool_or_var))
                    {
                        DoubleUnaryOperator func = scalarFunctions.get(tool_or_var);
                        Expression p = parseNumber();
                        x = () -> func.applyAsDouble(p.eval());
                    }
                    else if (tool_or_var.equals("dot"))
                    {
                        String[] vectors = parseVecFunc();
                        x = () -> dotProduct(variables.get(vectors[0]), variables.get(vectors[1]));
                    }
                    else if (tool_or_var.equals("norm"))
                    {
                        String vector = parseVecNormFunc();
                        x = () -> norm(variables.get(vector));
                    }
                    else if (tool_or_var.equals("vAdd"))
                    {
                        String[] vectors = parseVecAddSub();
                        double[] sumVec = vAdd(variables.get(vectors[1]), variables.get(vectors[2]));

                        variables.put(vectors[0], sumVec);
                    }
                    else if (tool_or_var.equals("vSub"))
                    {
                        String[] vectors = parseVecAddSub();
                        double[] sumVec = vSub(variables.get(vectors[1]), variables.get(vectors[2]));

                        variables.put(vectors[0], sumVec);
                    }
                }

                if (operator('^'))
                {
                    Expression p = x;
                    Expression q = parseNumber();
                    x = () -> Math.pow(p.eval(), q.eval());
                }

                return x;
            }
        }.parse();
    }

    public static void main(String[] args)
    {
        //Map<String,Double> variables = new HashMap<>();
        Map<String,double[]> variables = new HashMap<>();

        //Expression exp = parseScalerExpression("x + e^2", variables);
        Expression exp = parseVectorExpression("1 + dot(x, y)", variables);
        variables.put("x", new double[]{1, 2, 3});
        variables.put("y", new double[]{3, 2, 1});

        for (double x = 1; x < 10; x++)
        {
            //variables.put("x", x);
            System.out.println(x + " => " + exp.eval());
        }
    }
}
