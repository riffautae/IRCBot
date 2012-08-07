/*
 * This file is part of IRCBot.
 * Copyright (c) 2011 Ryan Morrison
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 *  * Redistributions of source code must retain the above copyright notice,
 *    this list of conditions, and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions, and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *  * Neither the name of the author of this software nor the name of
 *  contributors to this software may be used to endorse or promote products
 *  derived from this software without specific prior written consent.
 *  
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 *  AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 *  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 *  ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 *  LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 *  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 *  SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 *  INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 *  CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 *  ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *  POSSIBILITY OF SUCH DAMAGE.
 */

package us.rddt.IRCBot.Handlers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.StringTokenizer;

import org.pircbotx.PircBotX;
import org.pircbotx.hooks.events.MessageEvent;

/**
 * @author Ryan Morrison
 */
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
    
    // Associative constants for operators
    private static final int LEFT_ASSOC = 0;
    private static final int RIGHT_ASSOC = 1;
    
    // Supported operators
    private static final Map<String, int[]> operators = new HashMap<String, int[]>();
    static {
        /* 
         * Key is the token, value is an integer array where the first element is 
         * the precedence of the operator and the second element is the
         * associativity of the operator
         */
        operators.put("+", new int[] { 0, LEFT_ASSOC });
        operators.put("-", new int[] { 0, LEFT_ASSOC });
        operators.put("*", new int[] { 5, LEFT_ASSOC });
        operators.put("/", new int[] { 5, LEFT_ASSOC });
        operators.put("%", new int[] { 5, LEFT_ASSOC });
        operators.put("^", new int[] { 10, RIGHT_ASSOC });
    }
    
    /**
     * Returns if the provided token is a valid operator or not
     * @param token the token to validate
     * @return true if the token is a valid operator, false if it is not
     */
    private static boolean isOperator(String token) {
        return operators.containsKey(token);
    }
    
    /**
     * Returns a string of all valid operators (used as a parameter to a StringTokenizer)
     * @return a string of all valid operators
     */
    private static String getOperators() {
        StringBuilder builder = new StringBuilder();
        /* 
         * Convert the keys (operators) from the HashMap to an array, then iterate through
         * the array and add each operator to a string, then return the string.
         */
        String[] keys = (String[])(operators.keySet().toArray( new String[operators.size()]));
        for(int i = 0; i < keys.length; i++) {
            builder.append(keys[i]);
        }
        return builder.toString();
    }
    
    /**
     * Validates the associativity of a provided operator
     * @param token the token (operator) to validate
     * @param type the associativity type to validate against
     * @return true if the token type is equal to the provided type, false if it is not
     */
    private static boolean isAssociative(String token, int type) {
        // Throw an exception if the token is not valid
        if(!isOperator(token)) throw new IllegalArgumentException("Invalid token: " + token);
        if(operators.get(token)[1] == type) return true;
        else return false;
    }
    
    /**
     * Compares the precedence of two provided operators
     * @param token1 the first operator
     * @param token2 the second operator
     * @return A negative integer if token1 has less precedence than token2,
     * 0 if the precedences are equal and a positive integer if token1 has more
     * precedence than token2
     */
    private static final int cmpPrecedence(String token1, String token2) {
        // Throw an exception if the token is not valid
        if(!isOperator(token1) || !isOperator(token2)) throw new IllegalArgumentException("Invalid tokens: " + token1 + " " + token2);
        return operators.get(token1)[0] - operators.get(token2)[0];
    }
    
    /**
     * Converts an infix (in-order) mathematical expression to Reverse Polish notation
     * @param input the tokenized expression to convert
     * @return the expression in Reverse Polish notation
     */
    private static String[] infixToRPN(StringTokenizer inputTokens) {
        // Object initialization
        ArrayList<String> out = new ArrayList<String>();
        Stack<String> stack = new Stack<String>();
        
        while(inputTokens.hasMoreTokens()) {
            String token = inputTokens.nextToken();
            // It's possible that a token may be an empty string - just skip it
            if(token.isEmpty()) continue;
            // If the token is an operator...
            if(isOperator(token)) {
                /*
                 * While an operator currently sits on the top of the stack and
                 *  * the token is left associative and its precedence is less or equal than the value on the stack
                 * or
                 *  * the token is right associative and its precedence is less than the stack
                 * then pop off the stack into the output string builder and push the token onto the stack
                 */
                while(!stack.empty() && isOperator(stack.peek())) {
                    if((isAssociative(token, LEFT_ASSOC) && cmpPrecedence(token, stack.peek()) <= 0) || (isAssociative(token, RIGHT_ASSOC) && cmpPrecedence(token, stack.peek()) < 0)) {
                        out.add(stack.pop());
                        continue;
                    }
                    break;
                }
                stack.push(token);
            } else if(token.equals("(")) {
                // Push left parenthesis onto the stack
                stack.push(token);
            } else if(token.equals(")")) {
                // Pop tokens off the stack into the output string builder until the top of the stack is a left parenthesis
                while(!stack.empty() && !stack.peek().equals("(")) {
                    out.add(stack.pop());
                }
                // Pop but don't store the left parenthesis
                stack.pop();
            } else {
                // ...otherwise add the token to the output string builder
                out.add(token);
            }
        } while(!stack.empty()) {
            // If there are any remaining operators on the stack, pop them onto the output string builder
            out.add(stack.pop());
        }
        // Form an array containing the newly-converted expression and return it
        String[] output = new String[out.size()];
        return out.toArray(output);
    }

    /**
     * Calculates the result of a Reverse Polish notation expression to the user
     * @param input the tokenized expression to calculate
     */
    private static double rpnToDouble(String[] tokens) {
        // Object initialization
        Stack<String> stack = new Stack<String>();
        
        for(String token : tokens) {
            // If the token isn't an operator, push it onto the stack
            if(!isOperator(token)) stack.push(token);
            else {
                /*
                 * Pop the last two values off of the stack and perform the
                 * calculation required accordingly. Push the calculated value
                 * back onto the stack.
                 */
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
        
        // The last value in the stack is the final result - return it
        return Double.valueOf(stack.pop());
    }
    
    /**
     * Method that executes upon thread-start
     * (non-Javadoc)
     * @see java.lang.Runnable#run()
     */
    public void run() {
        // Tokenize the input string to separate values from operators
        StringTokenizer tokenizer = new StringTokenizer(event.getMessage().substring(6), getOperators() + "()", true);
        // Convert the expression from infix to Reverse Polish notation
        String[] rpn = infixToRPN(tokenizer);
        // Then determine the result
        double result = rpnToDouble(rpn);
        // Return the result to the user
        event.respond(String.valueOf(result));
    }
}
