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

package Main.EvalComputations.Token;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import Automata.AutomatonLogicalOps;
import Automata.AutomatonQuantification;
import Automata.FA.Infinite;
import Main.*;
import Main.EvalComputations.Expressions.Expression;
import Automata.Automaton;
import Main.EvalComputations.Expressions.AutomatonExpression;
import Main.EvalComputations.Expressions.VariableExpression;

import static Main.Logging.COMPUTED;
import static Main.Logging.COMPUTING;

public class LogicalOperator extends Operator {
    public static final String AND = "&";
    public static final String OR = "|";
    public static final String XOR = "^";
    public static final String IMPLY = "=>";
    public static final String IFF = "<=>";
    private int quantifiedVariableCount;

    @SuppressWarnings("this-escape")
    public LogicalOperator(int position, String op) {
        this.op = op;
        setPriority();
        arity = (this.isNegation(op) || op.equals(Operator.REVERSE)) ? 1 : 2;
        this.positionInPredicate = position;
    }

    @SuppressWarnings("this-escape")
    public LogicalOperator(int position, String op, int quantifiedVariableCount) {
        this.quantifiedVariableCount = quantifiedVariableCount;
        this.op = op;
        setPriority();
        arity = quantifiedVariableCount + 1;
        this.positionInPredicate = position;
    }

    public void act(Stack<Expression> S, boolean print) {
        super.validateArity(S);

        if (this.isNegation(op) || op.equals(Operator.REVERSE)) {
            actNegationOrReverse(S, print);
            return;
        }
        if (op.equals(Operator.EXISTS) || op.equals(Operator.FORALL) || op.equals(Operator.INFINITE)) {
            actQuantifier(S, print);
            return;
        }

        Expression b = S.pop();
        Expression a = S.pop();

        if (a instanceof AutomatonExpression && b instanceof AutomatonExpression) {
            Logging.logAndPrint(COMPUTING + " " + a + op + b);
            Logging.indent();
            String opString = "(" + a + op + b + ")";
            AutomatonExpression ae = switch (op) {
              case AND ->
                  new AutomatonExpression(opString, AutomatonLogicalOps.and(a.M, b.M, print));
              case OR -> new AutomatonExpression(opString, AutomatonLogicalOps.or(a.M, b.M, print, op));
              case XOR ->
                  new AutomatonExpression(opString, AutomatonLogicalOps.xor(a.M, b.M, print, op));
              case IMPLY ->
                  new AutomatonExpression(opString, AutomatonLogicalOps.imply(a.M, b.M, print, op));
              case IFF ->
                  new AutomatonExpression(opString, AutomatonLogicalOps.iff(a.M, b.M, print, op));
              default -> throw new WalnutException("Unexpected logical operator: " + op);
            };
            S.push(ae);

            Logging.dedent();
            Logging.logAndPrint(COMPUTED + " " + a + op + b);
            return;
        }
        throw WalnutException.invalidDualOperators(op, a, b);
    }

    private void actNegationOrReverse(Stack<Expression> S, boolean print) {
        Expression a = S.pop();
        if (a instanceof AutomatonExpression) {
            Logging.logAndPrint(COMPUTING + " " + op + a);
            Logging.indent();
            if (op.equals(Operator.REVERSE))
                AutomatonLogicalOps.reverse(a.M, print, true);
            if (this.isNegation(op))
                AutomatonLogicalOps.not(a.M, print);
            S.push(new AutomatonExpression(op + a, a.M));
            Logging.dedent();
            Logging.logAndPrint(COMPUTED + " " + op + a);
            return;
        }
        throw WalnutException.invalidOperator(op, a);
    }

    private void actQuantifier(Stack<Expression> S, boolean print) {
        StringBuilder stringValue = new StringBuilder("(" + op + " ");
        Stack<Expression> temp = reverseStack(S);
        Automaton M = null;
        Logging.logAndPrint( COMPUTING + " quantifier " + op);
        Logging.indent();
        List<String> identifiersToQuantify = new ArrayList<>();
        for (int i = 0; i < arity; i++) {
            Expression operand = temp.pop();
            if (i < arity - 1) {
                if (i == 0)
                    stringValue.append(operand).append(" ");
                else
                    stringValue.append(", ").append(operand).append(" ");
                if (!(operand instanceof VariableExpression))
                    throw new WalnutException("operator " + op + " requires a list of " + quantifiedVariableCount + " variables");

                identifiersToQuantify.add(operand.identifier);
            } else if (i == arity - 1) {
                stringValue.append(operand);
                if (!(operand instanceof AutomatonExpression))
                    throw new WalnutException("the last operand of " + op + " can only be of type automaton");
                M = operand.M;
                if (op.equals(Operator.EXISTS)) {
                    AutomatonQuantification.quantify(M, identifiersToQuantify, print);
                } else if (op.equals(Operator.FORALL)) {
                    // A == ~ E ~
                    AutomatonLogicalOps.not(M, print);
                    AutomatonQuantification.quantify(M, identifiersToQuantify, print);
                    AutomatonLogicalOps.not(M, print);
                } else {
                    // op == I
                    M = AutomatonLogicalOps.removeLeadingZeros(M, identifiersToQuantify, print);
                    String infReg = Infinite.infinite(M.fa, M.richAlphabet);
                    M = new Automaton(!infReg.isEmpty());
                }
            }
        }
        stringValue.append(")");
        S.push(new AutomatonExpression(stringValue.toString(), M));
        Logging.dedent();
        Logging.logAndPrint( COMPUTED + " quantifier " + stringValue);
    }
}
