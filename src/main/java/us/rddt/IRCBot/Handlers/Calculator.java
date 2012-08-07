package us.rddt.IRCBot.Handlers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
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
    
    private static final int LEFT_ASSOC = 0;
    private static final int RIGHT_ASSOC = 1;
    
    private static final Map<String, int[]> operators = new HashMap<String, int[]>();
    
    static {
        operators.put("+", new int[] { 0, LEFT_ASSOC });
        operators.put("-", new int[] { 0, LEFT_ASSOC });
        operators.put("*", new int[] { 5, LEFT_ASSOC });
        operators.put("/", new int[] { 5, LEFT_ASSOC });
        operators.put("%", new int[] { 5, LEFT_ASSOC });
        operators.put("^", new int[] { 10, RIGHT_ASSOC });
    }
    
    private static boolean isOperator(String token) {
        return operators.containsKey(token);
    }
    
    private static boolean isAssociative(String token, int type) {
        if(!isOperator(token)) throw new IllegalArgumentException("Invalid token: " + token);
        if(operators.get(token)[1] == type) return true;
        else return false;
    }
    
    private static final int cmpPrecedence(String token1, String token2) {
        if(!isOperator(token1) || !isOperator(token2)) throw new IllegalArgumentException("Invalid tokens: " + token1 + " " + token2);
        return operators.get(token1)[0] - operators.get(token2)[0];
    }
    
    /**
     * Converts an inflix (in-order) mathematical expression to Reverse Polish notation
     * @param input the inflix expression to convert
     * @return the expression in postfix notation
     */
    private static String[] infixToRPN(String[] inputTokens) {
        ArrayList<String> out = new ArrayList<String>();
        Stack<String> stack = new Stack<String>();
        
        for(String token : inputTokens) {
            if(token.isEmpty()) continue;
            if(isOperator(token)) {
                while(!stack.empty() && isOperator(stack.peek())) {
                    if((isAssociative(token, LEFT_ASSOC) && cmpPrecedence(token, stack.peek()) <= 0) || (isAssociative(token, RIGHT_ASSOC) && cmpPrecedence(token, stack.peek()) < 0)) {
                        out.add(stack.pop());
                        continue;
                    }
                    break;
                }
                stack.push(token);
            } else if(token.equals("(")) {
                stack.push(token);
            } else if(token.equals(")")) {
                while(!stack.empty() && !stack.peek().equals("(")) {
                    out.add(stack.pop());
                }
                stack.pop();
            } else {
                out.add(token);
            }
        } while(!stack.empty()) {
            out.add(stack.pop());
        }
        String[] output = new String[out.size()];
        return out.toArray(output);
    }

    /**
     * Parses and outputs the result of an inflix calculation to the user
     * @param input the inflix expression to calculate
     */
    private static double rpnToDouble(String[] tokens) {
        Stack<String> stack = new Stack<String>();
        
        for(String token : tokens) {
            if(!isOperator(token)) stack.push(token);
            else {
                Double d2 = Double.valueOf(stack.pop());
                Double d1 = Double.valueOf(stack.pop());
                
                Double result = token.compareTo("+") == 0 ? d1 + d2 :
                                token.compareTo("-") == 0 ? d1 - d2 :
                                token.compareTo("*") == 0 ? d1 * d2 :
                                token.compareTo("/") == 0 ? d1 / d2 :    
                                token.compareTo("^") == 0 ? Math.pow(d1, d2) :
                                token.compareTo("%") == 0 ? d1 % d2 :
                                    d1 + d2;
                
                stack.push(String.valueOf(result));
            }
        }
        
        return Double.valueOf(stack.pop());
    }
    
    /**
     * Method that executes upon thread-start
     * (non-Javadoc)
     * @see java.lang.Runnable#run()
     */
    public void run() {
        String[] rpn = infixToRPN(event.getMessage().substring(6).replaceAll("[+*()-/^%]", " $0 ").split(" "));
        double result = rpnToDouble(rpn);
        event.respond(String.valueOf(result));
    }
}
