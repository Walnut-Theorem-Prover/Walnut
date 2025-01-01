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

package Token;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Stack;

import Automata.AutomatonLogicalOps;
import Main.ExceptionHelper;
import Main.Expression;
import Automata.Automaton;
import Main.Expressions.AutomatonExpression;
import Main.Expressions.VariableExpression;
import Main.UtilityMethods;

public class LogicalOperator extends Operator {
    private int quantifiedVariableCount;

    public LogicalOperator(int position, String op) {
        this.op = op;
        setPriority();

        if (this.isNegation(op) || op.equals("`")) setArity(1);
        else setArity(2);
        setPositionInPredicate(position);
    }

    public LogicalOperator(int position, String op, int quantifiedVariableCount) {
        this.quantifiedVariableCount = quantifiedVariableCount;
        this.op = op;

        setPriority();
        setArity(quantifiedVariableCount + 1);
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
            UtilityMethods.logAndPrint(print, prefix + "computing " + a + op + b, log);
            String opString = "(" + a + op + b + ")";
            AutomatonExpression ae = switch (op) {
              case "&" ->
                  new AutomatonExpression(opString, AutomatonLogicalOps.and(a.M, b.M, print, prefix + " ", log, op));
              case "|" -> new AutomatonExpression(opString, AutomatonLogicalOps.or(a.M, b.M, print, prefix + " ", log, op));
              case "^" ->
                  new AutomatonExpression(opString, AutomatonLogicalOps.xor(a.M, b.M, print, prefix + " ", log, op));
              case "=>" ->
                  new AutomatonExpression(opString, AutomatonLogicalOps.imply(a.M, b.M, print, prefix + " ", log, op));
              case "<=>" ->
                  new AutomatonExpression(opString, AutomatonLogicalOps.iff(a.M, b.M, print, prefix + " ", log, op));
              default -> throw new RuntimeException("Unexpected logical operator: " + op);
            };
          S.push(ae);

            UtilityMethods.logAndPrint(print, prefix + "computed " + a + op + b, log);
            return;
        }
        throw ExceptionHelper.invalidDualOperators(op, a, b);
    }

    private void actNegationOrReverse(Stack<Expression> S, boolean print, String prefix, StringBuilder log) {
        Expression a = S.pop();
        if (a instanceof AutomatonExpression) {
            UtilityMethods.logAndPrint(print, prefix + "computing " + op + a, log);
            if (op.equals("`"))
                AutomatonLogicalOps.reverse(a.M, print, prefix + " ", log, true);
            if (this.isNegation(op))
                AutomatonLogicalOps.not(a.M, print, prefix + " ", log);
            S.push(new AutomatonExpression(op + a, a.M));
            UtilityMethods.logAndPrint(print, prefix + "computed " + op + a, log);
            return;
        }
        throw ExceptionHelper.invalidOperator(op, a);
    }

    private void actQuantifier(Stack<Expression> S, boolean print, String prefix, StringBuilder log) {
        StringBuilder stringValue = new StringBuilder("(" + op + " ");
        Stack<Expression> temp = new Stack<>();
        List<Expression> operands = new ArrayList<>();
        Automaton M = null;
        for (int i = 0; i < getArity(); i++) {
            temp.push(S.pop());
        }
        UtilityMethods.logAndPrint(print, prefix + "computing quantifier " + op, log);
        List<String> identifiersToQuantify = new ArrayList<>();
        for (int i = 0; i < getArity(); i++) {
            operands.add(temp.pop());
            Expression operand = operands.get(i);
            if (i < getArity() - 1) {
                if (i == 0)
                    stringValue.append(operand).append(" ");
                else
                    stringValue.append(", ").append(operand).append(" ");
                if (!(operand instanceof VariableExpression))
                    throw new RuntimeException("operator " + op + " requires a list of " + quantifiedVariableCount + " variables");

                identifiersToQuantify.add(operand.identifier);
            } else if (i == getArity() - 1) {
                stringValue.append(operand);
                if (!(operand instanceof AutomatonExpression))
                    throw new RuntimeException("the last operand of " + op + " can only be of type automaton");
                M = operand.M;
                if (op.equals("E")) {
                    AutomatonLogicalOps.quantify(M, identifiersToQuantify, print, prefix + " ", log);
                } else if (op.equals("A")) {
                    AutomatonLogicalOps.not(M, print, prefix + " ", log);
                    AutomatonLogicalOps.quantify(M, identifiersToQuantify, print, prefix + " ", log);
                    AutomatonLogicalOps.not(M, print, prefix + " ", log);
                } else {
                    M = AutomatonLogicalOps.removeLeadingZeroes(M, identifiersToQuantify, print, prefix + " ", log);
                    String infReg = M.infinite();
                    M = infReg.isEmpty() ? new Automaton(false) : new Automaton(true);
                }
            }
        }
        stringValue.append(")");
        S.push(new AutomatonExpression(stringValue.toString(), M));
        UtilityMethods.logAndPrint(print, prefix + "computed quantifier " + stringValue, log);
    }
}
