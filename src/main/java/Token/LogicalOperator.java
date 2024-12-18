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

package Token;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Stack;

import Automata.AutomatonLogicalOps;
import Main.Expression;
import Automata.Automaton;
import Main.Expressions.AutomatonExpression;
import Main.Expressions.VariableExpression;

public class LogicalOperator extends Operator {
    int number_of_quantified_variables;

    public LogicalOperator(int position, String op) {
        this.op = op;
        setPriority();

        if (this.isNegation(op) || op.equals("`")) setArity(1);
        else setArity(2);
        setPositionInPredicate(position);
    }

    public LogicalOperator(int position, String op, int number_of_quantified_variables) {
        this.number_of_quantified_variables = number_of_quantified_variables;
        this.op = op;

        setPriority();
        setArity(number_of_quantified_variables + 1);
        setPositionInPredicate(position);
    }

    public void act(Stack<Expression> S, boolean print, String prefix, StringBuilder log) {
        if (S.size() < getArity()) throw new RuntimeException("operator " + op + " requires " + getArity() + " operands");

        if (this.isNegation(op) || op.equals("`")) {
            actNegationOrReverse(S, print, prefix, log);
            return;
        }
        if (op.equals("E") || op.equals("A") || op.equals("I")) {
            actQuantifier(S, print, prefix, log);
            return;
        }

        Expression b = S.pop();
        Expression a = S.pop();

        if (a instanceof AutomatonExpression && b instanceof AutomatonExpression) {
            String preStep = prefix + "computing " + a + op + b;
            log.append(preStep + System.lineSeparator());
            if (print) {
                System.out.println(preStep);
            }
            String opString = "(" + a + op + b + ")";
            switch (op) {
                case "&":
                    S.push(new AutomatonExpression(opString, AutomatonLogicalOps.and(a.M, b.M, print, prefix + " ", log)));
                    break;
                case "|":
                    S.push(new AutomatonExpression(opString, AutomatonLogicalOps.or(a.M, b.M, print, prefix + " ", log)));
                    break;
                case "^":
                    S.push(new AutomatonExpression(opString, AutomatonLogicalOps.xor(a.M, b.M, print, prefix + " ", log)));
                    break;
                case "=>":
                    S.push(new AutomatonExpression(opString, AutomatonLogicalOps.imply(a.M, b.M, print, prefix + " ", log)));
                    break;
                case "<=>":
                    S.push(new AutomatonExpression(opString, AutomatonLogicalOps.iff(a.M, b.M, print, prefix + " ", log)));
                    break;
            }
            String postStep = prefix + "computed " + a + op + b;
            log.append(postStep + System.lineSeparator());
            if (print) {
                System.out.println(postStep);
            }
            return;
        }
        throw new RuntimeException("operator " + op + " cannot be applied to operands " + a + " and " + b + " of types " + a.getClass().getName() + " and " + b.getClass().getName() + " respectively");

    }

    private void actNegationOrReverse(Stack<Expression> S, boolean print, String prefix, StringBuilder log) {
        Expression a = S.pop();
        if (a instanceof AutomatonExpression) {
            String preStep = prefix + "computing " + op + a;
            log.append(preStep + System.lineSeparator());
            if (print) {
                System.out.println(preStep);
            }
            if (op.equals("`"))
                AutomatonLogicalOps.reverse(a.M, print, prefix + " ", log, true);
            if (this.isNegation(op))
                AutomatonLogicalOps.not(a.M, print, prefix + " ", log);
            S.push(new AutomatonExpression(op + a, a.M));
            String postStep = prefix + "computed " + op + a;
            log.append(postStep + System.lineSeparator());
            if (print) {
                System.out.println(postStep);
            }
            return;
        }
        throw new RuntimeException("operator " + op + " cannot be applied to the operand " + a + " of type " + a.getClass().getName());
    }

    private void actQuantifier(Stack<Expression> S, boolean print, String prefix, StringBuilder log) {
        String stringValue = "(" + op + " ";
        Stack<Expression> temp = new Stack<>();
        List<Expression> operands = new ArrayList<>();
        Automaton M = null;
        for (int i = 0; i < getArity(); i++) {
            temp.push(S.pop());
        }
        String preStep = prefix + "computing quantifier " + op;
        log.append(preStep + System.lineSeparator());
        if (print) {
            System.out.println(preStep);
        }
        List<String> list_of_identifiers_to_quantify = new ArrayList<>();
        for (int i = 0; i < getArity(); i++) {
            operands.add(temp.pop());
            Expression operand = operands.get(i);
            if (i < getArity() - 1) {
                if (i == 0)
                    stringValue += operand + " ";
                else
                    stringValue += ", " + operand + " ";
                if (!(operand instanceof VariableExpression))
                    throw new RuntimeException("operator " + op + " requires a list of " + number_of_quantified_variables + " variables");

                list_of_identifiers_to_quantify.add(operand.identifier);
            } else if (i == getArity() - 1) {
                stringValue += operand;
                if (!(operand instanceof AutomatonExpression))
                    throw new RuntimeException("the last operand of " + op + " can only be of type automaton");
                M = operand.M;
                if (op.equals("E")) {
                    AutomatonLogicalOps.quantify(M, new HashSet<>(list_of_identifiers_to_quantify), print, prefix + " ", log);
                } else if (op.equals("A")) {
                    AutomatonLogicalOps.not(M, print, prefix + " ", log);
                    AutomatonLogicalOps.quantify(M, new HashSet<>(list_of_identifiers_to_quantify), print, prefix + " ", log);
                    AutomatonLogicalOps.not(M, print, prefix + " ", log);
                } else {
                    M = AutomatonLogicalOps.removeLeadingZeroes(M, list_of_identifiers_to_quantify, print, prefix + " ", log);
                    String infReg = M.infinite();
                    M = infReg.equals("") ? new Automaton(false) : new Automaton(true);
                }
            }
        }
        stringValue += ")";
        S.push(new AutomatonExpression(stringValue, M));
        String postStep = prefix + "computed quantifier " + stringValue;
        log.append(postStep + System.lineSeparator());
        if (print) {
            System.out.println(postStep);
        }
    }
}
