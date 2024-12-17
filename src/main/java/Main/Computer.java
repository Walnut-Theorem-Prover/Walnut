/*	 Copyright 2016 Hamoon Mousavi
 *
 * 	 This file is part of Walnut.
 *
 *   Walnut is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   Walnut is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with Walnut.  If not, see <http://www.gnu.org/licenses/>.
 */

package Main;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Stack;

import Automata.Automaton;
import Automata.AutomatonWriter;
import Token.Token;


public class Computer {
    Predicate predicate_object;
    String predicate_string;
    Expression result;
    StringBuilder log;
    StringBuilder log_details;
    String mpl;
    boolean printSteps;
    boolean printDetails;

    public Computer(String predicate, boolean printSteps, boolean printDetails) {
        this.log = new StringBuilder();
        this.log_details = new StringBuilder();
        mpl = "";
        this.predicate_string = predicate;
        predicate_object = new Predicate(predicate);
        this.printSteps = printSteps;
        this.printDetails = printDetails;
        compute();
    }

    public Automaton getTheFinalResult() {
        return result.M;
    }

    public void writeMatrices(String address, List<String> free_variables) {
        try {
            mpl = AutomatonWriter.write_matrices(result.M, address, free_variables);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    public void writeLog(String address) {
        PrintWriter out;
        try {
            out = new PrintWriter(address, "UTF-8");
            out.write(log.toString());
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    public void writeDetailedLog(String address) {
        PrintWriter out;
        try {
            out = new PrintWriter(address, "UTF-8");
            out.write(log_details.toString());
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    public void drawAutomaton(String address) {
        AutomatonWriter.draw(result.M, address, predicate_string, false);
    }

    public void write(String address) {
        AutomatonWriter.write(result.M, address);
    }

    public String toString() {
        return result.toString();
    }

    private void compute() {
        Stack<Expression> expression_Stack = new Stack<Expression>();
        List<Token> postOrder = predicate_object.get_postOrder();
        String prefix = "";
        long timeBeginning = System.currentTimeMillis();
        String step;

        for (Token t : postOrder) {
            try {

                long timeBefore = System.currentTimeMillis();
                t.act(expression_Stack, printDetails, prefix, log_details);
                long timeAfter = System.currentTimeMillis();
                if (t.isOperator() && expression_Stack.peek().is(Type.automaton)) {
                    step = prefix + expression_Stack.peek() + ":" +
                            expression_Stack.peek().M.Q + " states - " + (timeAfter - timeBefore) + "ms";
                    log.append(step + System.lineSeparator());
                    log_details.append(step + System.lineSeparator());
                    if (printSteps || printDetails) {
                        System.out.println(step);
                    }

                    prefix += " ";
                }
            } catch (Exception e) {
                e.printStackTrace();
                String message = e.getMessage();
                message += System.lineSeparator() + "\t: char at " + t.getPositionInPredicate();
                throw new RuntimeException(message);
            }
        }

        long timeEnd = System.currentTimeMillis();
        step = "Total computation time: " + (timeEnd - timeBeginning) + "ms.";
        log.append(step);
        log_details.append(step);
        if (printSteps || printDetails) {
            System.out.println(step);
        }

        if (expression_Stack.size() > 1) {
            String message =
                    "Cannot evaluate the following into a single automaton:" +
                            System.lineSeparator();
            Stack<Expression> tmp = new Stack<>();

            while (!expression_Stack.isEmpty()) {
                tmp.push(expression_Stack.pop());
            }

            while (!tmp.isEmpty()) {
                message += tmp.pop() + System.lineSeparator();
            }

            message += "Probably some operators are missing.";
            throw new RuntimeException(message);
        } else if (expression_Stack.isEmpty()) {
            throw new RuntimeException("Evaluation ended in no result.");
        } else if (expression_Stack.size() == 1) {
            result = expression_Stack.pop();
            if (!result.is(Type.automaton)) {
                throw new RuntimeException("The final result of the evaluation is not of type " + Type.automaton);
            }
        }
    }
}
