@FunctionalInterface
interface Expression
{
    double eval();
}

public class secondTmpFile implements Expression
{
    final String str;
    double ans;

    public secondTmpFile(String str)
    {
        str = str.replaceAll("\\s", "");
        this.str = str;
        c = str.charAt(0);
        ans = parse();
    }

    int pos = 0;
    char c;

    void nextChar()
    {
        System.out.println(pos);
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

    double parse()
    {
        double x = parseAddSub();

        if (pos < str.length())
            System.out.println("Error with char " + c);

        return x;
    }

    double parseAddSub()
    {
        double x = parseMultDiv();

        while (true)
        {
            if (operator('+'))
                x += parseMultDiv();
            else if (operator('-'))
                x -= parseMultDiv();
            else
                return x;
        }
    }

    double parseMultDiv()
    {
        double x = parseNumber();

        while (true)
        {
            if (operator('*'))
                x *= parseMultDiv();
            else if (operator('/'))
                x /= parseMultDiv();
            else
                return x;
        }
    }

    double parseNumber()
    {
        // check for positive or negative signs
        if (operator('+'))
            return +parseNumber();
        else if (operator('-'))
            return -parseNumber();

        //
        double x = 0;
        int startPos = pos;

        if (operator('('))
        {
            x = parseAddSub();

            if (!operator(')'))
                System.out.println("Missing parenthesis: )");
        }
        else if (c >= '0' && c <= '9' || c == '.')
        {
            while (c >= '0' && c <= '9' || c == '.')
                nextChar();

            x = Double.parseDouble(str.substring(startPos, pos));
        }
        else if (c >= 'a' && c <= 'z')
        {
            while (c >= 'a' && c <= 'z')
                nextChar();

            String tool = str.substring(startPos, pos);

            if (operator('('))
            {
                x = parseAddSub();

                if (!operator(')'))
                    System.out.println("Missing parenthesis: )");
            }
            else
            {
                x = parseNumber();
            }

            x = switch (tool)
                    {
                        case "sqrt" -> Math.sqrt(x);
                        case "sin" -> Math.sin(Math.toRadians(x));
                        case "cos" -> Math.cos(Math.toRadians(x));
                        case "tan" -> Math.tan(Math.toRadians(x));
                        case "e" -> 2.7182818284590452;
                        default -> throw new RuntimeException("Unknown function: " + tool);
                    };
        }

        if (operator('^'))
            x = Math.pow(x, parseNumber());

        return x;
    }

    public double eval()
    {
        return 0;
    }

    public static void main (String[] args)
    {
        secondTmpFile tmp = new secondTmpFile("((4 - 2^3 + 1) * -sqrt(3*3+4*4)) / 2");
        System.out.println(tmp.ans);
    }
}
