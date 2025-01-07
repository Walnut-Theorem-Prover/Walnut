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

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Stack;

import Main.Expressions.AutomatonExpression;
import Token.Token;


public class Computer {
    private final Predicate predicateObject;
    String predicateString;
    Expression result;
    private final StringBuilder log;
    final StringBuilder logDetails;
    String mpl;
    private final boolean printSteps;
    private final boolean printDetails;

    public Computer(String predicate, boolean printSteps, boolean printDetails) {
        this.log = new StringBuilder();
        this.logDetails = new StringBuilder();
        mpl = "";
        this.predicateString = predicate;
        predicateObject = new Predicate(predicate);
        this.printSteps = printSteps;
        this.printDetails = printDetails;
        compute();
    }

    public String toString() {
        return result.toString();
    }

    void writeLogs(String resultName, Computer c, boolean printDetails) throws IOException {
        try (PrintWriter out = new PrintWriter(resultName + "_log.txt", StandardCharsets.UTF_8)) {
            out.write(c.log.toString());
        }
        if (printDetails) {
            try (PrintWriter out = new PrintWriter(resultName + "_detailed_log.txt", StandardCharsets.UTF_8)) {
                out.write(c.logDetails.toString());
            }
        }
    }

    private void compute() {
        Stack<Expression> expressions = new Stack<>();
        List<Token> postOrder = predicateObject.getPostOrder();
        String prefix = "";
        long timeBeginning = System.currentTimeMillis();
        String step;

        for (Token t : postOrder) {
            try {
                long timeBefore = System.currentTimeMillis();
                t.act(expressions, printDetails, prefix, logDetails);
                long timeAfter = System.currentTimeMillis();
                Expression nextExpression = expressions.peek();
                if (t.isOperator() && nextExpression instanceof AutomatonExpression) {
                    step = prefix + nextExpression + ":" +
                        nextExpression.M.getQ() + " states - " + (timeAfter - timeBefore) + "ms";
                    log.append(step + System.lineSeparator());
                    logDetails.append(step + System.lineSeparator());
                    if (printSteps || printDetails) {
                        System.out.println(step);
                    }

                    prefix += " ";
                }
            } catch (RuntimeException e) {
                e.printStackTrace();
                String message = e.getMessage();
                message += System.lineSeparator() + "\t: char at " + t.getPositionInPredicate();
                throw new RuntimeException(message);
            }
        }

        long timeEnd = System.currentTimeMillis();
        step = "Total computation time: " + (timeEnd - timeBeginning) + "ms.";
        log.append(step);
        logDetails.append(step);
        if (printSteps || printDetails) {
            System.out.println(step);
        }

        if (expressions.size() > 1) {
            String message =
                    "Cannot evaluate the following into a single automaton:" +
                            System.lineSeparator();
            Stack<Expression> tmp = new Stack<>();

            while (!expressions.isEmpty()) {
                tmp.push(expressions.pop());
            }

            while (!tmp.isEmpty()) {
                message += tmp.pop() + System.lineSeparator();
            }

            message += "Probably some operators are missing.";
            throw new RuntimeException(message);
        } else if (expressions.isEmpty()) {
            throw new RuntimeException("Evaluation ended in no result.");
        } else {
            result = expressions.pop();
            if (!(result instanceof AutomatonExpression)) {
                throw new RuntimeException("The final result of the evaluation is not of type automaton");
            }
        }
    }
}
