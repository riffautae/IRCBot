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
    private String inflixToPostfix(String input) {
        /*
         * Variables
         */
        String tmp;
        String finalExp = "";
        Stack<String> s = new Stack<String>();

        StringReader reader = new StringReader(input.replaceAll("[^0-9.]+", " $0 "));
        Scanner scan = new Scanner(reader);

        while(scan.hasNext()) {
            if(scan.hasNextDouble()) finalExp += scan.nextDouble() + " ";
            else {
                tmp = scan.next();
                if(isOperator(tmp)) {
                    // If the stack is empty there is no precedence so push
                    if(s.isEmpty()) s.push(tmp);
                    else {
                        /*
                         * If the current character is an operator, we need to check the stack 
                         * status and if the stack top contains an operator with lower
                         * precedence, we push the current character in the stack - otherwise, we pop
                         * the character from the stack and add it to the postfix string. This 
                         * continues until we either find an operator with lower precedence in the 
                         * stack or we find the stack to be empty.
                         */
                        String top = s.peek();
                        while(getPrecedence(top, tmp).equals(top) && !s.isEmpty()) {
                            finalExp += s.pop() + " ";
                            if(!s.isEmpty()) top = s.peek();
                        }
                        // Push the operator on the stack
                        s.push(tmp);
                    }
                } else if(tmp.equals("(")) {
                    s.push(tmp);
                } else if(tmp.equals(")")) {
                    while(!s.empty() && !s.peek().equals("(")) {
                        finalExp += s.pop() + " ";
                    }
                }
            }
        }

        // Append all the operators on the stack to the final expression and return it
        while(!s.isEmpty()) finalExp += s.pop() + " ";
        return finalExp;
    }

    /**
     * Helper method to check if a string is a mathematical operator
     * @param input the input string to check
     * @return true if the string is an operator, false if it is not
     */
    private boolean isOperator(String input) {
        String operators = "*/%^!+-";
        if (operators.indexOf(input) != -1) return true;
        else return false;
    }

    /**
     * Helper method to determine the precedence of mathematical operators
     * @param op1 the first operator to compare
     * @param op2 the second operator to compare
     * @return the operator with higher precedence
     */
    private String getPrecedence(String op1, String op2){
        // Variables to hold the appropriate operators
        String multiplicativeOps = "*/^!%";
        String additiveOps = "+-";

        // Determine which operator has higher precedence and return it
        if ((multiplicativeOps.indexOf(op1) != -1) && (additiveOps.indexOf(op2) != -1)) return op1;
        else if ((multiplicativeOps.indexOf(op2) != -1) && (additiveOps.indexOf(op1) != -1)) return op2;
        else if((multiplicativeOps.indexOf(op1) != -1) && (multiplicativeOps.indexOf(op2) != -1)) return op1;
        else return op1;
    }

    /**
     * Parses and outputs the result of an inflix calculation to the user
     * @param input the inflix expression to calculate
     */
    private void parseCalculation(String input) {
        /*
         * Variables
         */
        String postfix = inflixToPostfix(input);
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
