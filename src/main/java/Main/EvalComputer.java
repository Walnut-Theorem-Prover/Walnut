/*	 Copyright 2016 Hamoon Mousavi, 2025 John Nicol
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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Stack;

import Main.EvalComputations.Expressions.AutomatonExpression;
import Main.EvalComputations.Expressions.Expression;
import Main.EvalComputations.Token.Token;


/**
 * This is used in eval/def commands to compute the predicate.
 */
public class EvalComputer {
    Expression result;
    private final StringBuilder log;
    final StringBuilder logDetails;
    private final boolean printSteps;
    private final boolean printDetails;

    public EvalComputer(Predicate predicate, boolean printSteps, boolean printDetails) {
        this.log = new StringBuilder();
        this.logDetails = new StringBuilder();
        this.printSteps = printSteps;
        this.printDetails = printDetails;
        compute(predicate);
    }

    public String toString() {
        return result.toString();
    }

    void writeLogs(String resultName, boolean printDetails) throws IOException {
        try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(resultName + "_log.txt")))) {
            out.write(log.toString());
        }
        if (printDetails) {
            try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(resultName + "_detailed_log.txt")))) {
                out.write(logDetails.toString());
            }
        }
    }

    private void compute(Predicate predicate) {
        Stack<Expression> expressions = new Stack<>();
        List<Token> postOrder = predicate.getPostOrder();
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
                    log.append(step).append(System.lineSeparator());
                    logDetails.append(step).append(System.lineSeparator());
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
            StringBuilder message =
                new StringBuilder("Cannot evaluate the following into a single automaton:" +
                    System.lineSeparator());
            Stack<Expression> tmp = new Stack<>();

            while (!expressions.isEmpty()) {
                tmp.push(expressions.pop());
            }

            while (!tmp.isEmpty()) {
                message.append(tmp.pop()).append(System.lineSeparator());
            }

            message.append("Probably some operators are missing.");
            throw new RuntimeException(message.toString());
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
