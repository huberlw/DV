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

    static Map<String,DoubleUnaryOperator> functions = Map.ofEntries(
            entry("sqrt", x -> Math.sqrt(x)),
            entry("sin", x -> Math.sin(Math.toRadians(x))),
            entry("cos", x -> Math.cos(Math.toRadians(x))),
            entry("tan", x -> Math.tan(Math.toRadians(x)))
    );

    public static Expression parseExpression(String strExp, Map<String, Double> variables)
    {
        return new Object()
        {
            int pos;
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
                c = str.charAt(0);

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

                    if (functions.containsKey(tool_or_var))
                    {
                        DoubleUnaryOperator func = functions.get(tool_or_var);
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

    public static void main(String[] args)
    {
        Map<String,Double> variables = new HashMap<>();

        Expression exp = parseExpression("x + e^2", variables);

        for (double x = 1; x < 10; x++)
        {
            variables.put("x", x);
            System.out.println(x + " => " + exp.eval());
        }
    }
}
