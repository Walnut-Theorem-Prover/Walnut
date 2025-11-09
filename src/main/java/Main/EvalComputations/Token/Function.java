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
import Main.EvalComputations.Expressions.Expression;
import Main.EvalComputations.Expressions.ArithmeticExpression;
import Main.EvalComputations.Expressions.AutomatonExpression;
import Main.EvalComputations.Expressions.NumberLiteralExpression;
import Main.EvalComputations.Expressions.VariableExpression;
import Main.Logging;
import Main.UtilityMethods;
import Automata.Automaton;
import Automata.NumberSystem;

import static Main.Logging.COMPUTED;
import static Main.Logging.COMPUTING;


public class Function extends Token {
    private Automaton A;
    private final String name;
    private final NumberSystem ns;

    @SuppressWarnings("this-escape")
    public Function(String number_system, int position, String name, Automaton A, int argCount) {
        this.name = name;
        this.arity = argCount;
        this.positionInPredicate = position;
        this.A = A;
        this.ns = new NumberSystem(number_system);
        super.validateArity(name, A.getArity());
    }

    public String toString() {
        return name;
    }

    public void act(Stack<Expression> S, boolean print, String prefix, StringBuilder log) {
        super.validateArity(S, "function ", " arguments");
        Stack<Expression> temp = reverseStack(S);
        String stringValue = this + "(";
        Logging.logAndPrint(print, prefix + COMPUTING + " " + stringValue + "...)", log);
        Automaton M = new Automaton(true);
        List<String> identifiers = new ArrayList<>();
        List<String> quantify = new ArrayList<>();
        List<Expression> expressions = new ArrayList<>(arity);
        for (int i = 0; i < arity; i++) {
            expressions.add(temp.pop());
        }
        stringValue += UtilityMethods.genericListString(expressions, ",") + "))";
        for (int i = 0; i < arity; i++) {
            Expression expression = expressions.get(i);
            if (expression instanceof VariableExpression ve) {
                M = ve.act(print, prefix, log, this, this.ns, identifiers, M, quantify);
            } else if (expression instanceof ArithmeticExpression ae) {
                M = ae.act(print, prefix, log, identifiers, M, quantify);
            } else if (expression instanceof NumberLiteralExpression ne) {
                M = ne.act(print, prefix, log, this, identifiers, quantify, M);
            } else if (expression instanceof AutomatonExpression ae) {
                M = ae.act(print, prefix, name, log, i, M, identifiers);
            } else if (expression == null) {
                throw new IllegalArgumentException("Expression is null");
            } else {
                expression.act("argument " + (i + 1) + " of function " + this);
            }
        }
        A.bind(identifiers);
        A = AutomatonLogicalOps.and(A, M, print, prefix + " ", log);
        AutomatonQuantification.quantify(A, quantify, print, prefix + " ", log);

        S.push(new AutomatonExpression(stringValue, A));
        Logging.logAndPrint(print, prefix + COMPUTED + " " + stringValue, log);
    }
}
