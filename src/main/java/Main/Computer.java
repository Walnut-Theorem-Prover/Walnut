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

import Automata.Automaton;
import Automata.AutomatonWriter;
import Main.Expressions.AutomatonExpression;
import Token.Token;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class Computer {
    private static final Logger LOGGER = LogManager.getLogger(Computer.class);

    private final Predicate predicate;
    private final String predicateStr;
    private Expression result;
    final StringBuilder log;
    final StringBuilder log_details;
    String mpl;
    private final boolean printSteps;
    private final boolean printDetails;

    public Computer(String predicate, boolean printSteps, boolean printDetails) {
        this.log = new StringBuilder();
        this.log_details = new StringBuilder();
        mpl = "";
        this.predicateStr = predicate;
        this.predicate = new Predicate(predicate);
        this.printSteps = printSteps;
        this.printDetails = printDetails;
        compute();
    }

    public Automaton getTheFinalResult() {
        return result.M;
    }

    public void writeLog(String address) throws IOException {
        PrintWriter out = new PrintWriter(address, StandardCharsets.UTF_8);
        out.write(log.toString());
        out.close();
    }

    public void writeDetailedLog(String address) throws IOException {
        PrintWriter out = new PrintWriter(address, StandardCharsets.UTF_8);
        out.write(log_details.toString());
        out.close();
    }

    public void drawAutomaton(String address) {
        AutomatonWriter.draw(result.M, address, predicateStr, false);
    }

    public void write(String address) {
        AutomatonWriter.write(result.M, address);
    }

    public String toString() {
        return result.toString();
    }

    private void compute() {
        Stack<Expression> expressions = new Stack<>();
        List<Token> postOrder = predicate.getPostOrder();
        String prefix = "";
        long timeBeginning = System.currentTimeMillis();
        String step;

        for (Token t : postOrder) {
            try {
                long timeBefore = System.currentTimeMillis();
                t.act(expressions, printDetails, prefix, log_details);
                long timeAfter = System.currentTimeMillis();
                Expression nextExpression = expressions.peek();
                if (t.isOperator() && nextExpression instanceof AutomatonExpression) {
                    step = prefix + nextExpression + ":" +
                        nextExpression.M.Q + " states - " + (timeAfter - timeBefore) + "ms";
                    log.append(step + System.lineSeparator());
                    log_details.append(step + System.lineSeparator());
                    if (printSteps || printDetails) {
                        LOGGER.info(step);
                    }

                    prefix += " ";
                }
            } catch (RuntimeException e) {
                LOGGER.catching(e);
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
            LOGGER.info(step);
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
