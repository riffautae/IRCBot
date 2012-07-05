package us.rddt.IRCBot.Handlers;

import java.io.StringReader;
import java.util.Scanner;
import java.util.Stack;

import org.pircbotx.PircBotX;
import org.pircbotx.hooks.events.MessageEvent;

public class Calculator implements Runnable {
    /*
     * Class variables
     */
    private MessageEvent<PircBotX> event;

    /**
     * Class constructor
     * @param event the MessageEvent that triggered this class
     */
    public Calculator(MessageEvent<PircBotX> event) {
        this.event = event;
    }
    
    private double factorial(double n) {
        double ret = 1;
        for (int i = 1; i <= n; ++i) ret *= i;
        return ret;
    }

    /**
     * Converts an inflix (in-order) mathematical expression to postfix notation
     * @param input the inflix expression to convert
     * @return the expression in postfix notation
     */
    private static String inflixToPostfix(String input) {
        // Return null if the string is null or empty
        if (input == null || input.isEmpty()) return null;
        // Convert the string into a character array
        char[] in = input.toCharArray();
        
        // The stack to hold the expression during conversion
        Stack<Character> stack = new Stack<Character>();
        // The StringBuilder that will build the converted expression
        StringBuilder out = new StringBuilder();
        
        /*
         * We loop through each element in the character array of the original expression,
         * pushing and popping off the stack in an appropriate manner to convert the
         * expression into postfix notation
         */
        for (int i = 0; i < in.length; i++) {
            switch (in[i]) {
            case '+':
            case '-':
                while (!stack.empty() && (stack.peek() == '*' || stack.peek() == '/')) {
                    out.append(' ').append(stack.pop());
                }
            case '^':
            case '!':
            case '*':
            case '/':
                out.append(' ');
            case '(':
                stack.push(in[i]);
            case ' ':
                break;
            case ')':
                while (!stack.empty() && stack.peek() != '(') {
                    out.append(' ').append(stack.pop());
                }
                if (!stack.empty()) stack.pop();
                break;
            default:
                out.append(in[i]);
                break;
            }
        }
        
        // Pop the rest of the stack into the output string
        while (!stack.isEmpty()) {
            out.append(' ').append(stack.pop());
        }
        
        // Return the newly converted string
        return out.toString();
    }

    /**
     * Parses and outputs the result of an inflix calculation to the user
     * @param input the inflix expression to calculate
     */
    private void parseCalculation(String input) {
        // Convert the input expression from inflix to postfix notation
        String postfix = inflixToPostfix(input);

        // Make sure the conversion was successful - return an error if not
        if(postfix == null) {
            event.respond("An error has occurred parsing your expression - please ensure the expression is in valid inflix notation and uses compatible operators.");
            return;
        }
        
        StringReader reader = new StringReader(postfix);

        Scanner scan = new Scanner(reader);
        Stack<Double> s = new Stack<Double>();

        double tmp;

        // Loop through the expression string
        while(scan.hasNext()) {
            /*
             * If the next value in the string is a integer/double value, push
             * it onto the stack. Otherwise, perform the appropriate mathematical
             * function on the stack.
             */
            if(scan.hasNextDouble()) {
                s.push(scan.nextDouble());
            } else {
                String token = scan.next();
                if(token.equals("+")) {
                    s.push(s.pop() + s.pop());
                } else if(token.equals("-")) {
                    tmp = s.pop();
                    s.push(s.pop() - tmp);
                } else if(token.equals("*")) {
                    s.push(s.pop() * s.pop());
                } else if(token.equals("/")) {
                    tmp = s.pop();
                    s.push(s.pop() / tmp);
                } else if(token.equals("%")) {
                    tmp = s.pop();
                    s.push(s.pop() % tmp);
                } else if(token.equals("^")) {
                    tmp = s.pop();
                    s.push(Math.pow(s.pop(), tmp));
                } else if(token.equals("!")) {
                    s.push(factorial(s.pop()));
                }
            }
        }

        // Return the correct answer to the user
        event.respond(s.peek().toString());

    }

    /**
     * Method that executes upon thread-start
     * (non-Javadoc)
     * @see java.lang.Runnable#run()
     */
    public void run() {
        parseCalculation(event.getMessage().substring(6));
    }
}
