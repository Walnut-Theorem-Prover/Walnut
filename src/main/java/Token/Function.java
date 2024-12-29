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
import Main.Expression;
import Main.Expressions.ArithmeticExpression;
import Main.Expressions.AutomatonExpression;
import Main.Expressions.NumberLiteralExpression;
import Main.Expressions.VariableExpression;
import Main.UtilityMethods;
import Automata.Automaton;
import Automata.NumberSystem;


public class Function extends Token {
    private Automaton A;
    private final String name;
    private final NumberSystem ns;


    public Function(String number_system, int position, String name, Automaton A, int argCount) {
        this.name = name;
        setArity(argCount);
        setPositionInPredicate(position);
        this.A = A;
        this.ns = new NumberSystem(number_system);
        if (A.getArity() != getArity())
            throw new RuntimeException("function " + name + " requires " + A.getArity() + " arguments: char at " + getPositionInPredicate());
    }

    public String toString() {
        return name;
    }

    public void act(Stack<Expression> S, boolean print, String prefix, StringBuilder log) {
        if (S.size() < getArity()) throw new RuntimeException("function " + this + " requires " + getArity() + " arguments");
        Stack<Expression> temp = new Stack<>();
        for (int i = 0; i < getArity(); i++) {
            temp.push(S.pop());
        }
        String stringValue = this + "(";
        UtilityMethods.logAndPrint(print, prefix + "computing " + stringValue + "...)", log);
        Automaton M = new Automaton(true);
        List<String> identifiers = new ArrayList<>();
        List<String> quantify = new ArrayList<>();
        List<Expression> expressions = new ArrayList<>(getArity());
        for (int i = 0; i < getArity(); i++) {
            expressions.add(temp.pop());
        }
        stringValue += UtilityMethods.genericListString(expressions, ",") + "))";
        for (int i = 0; i < getArity(); i++) {
            Expression expression = expressions.get(i);
            switch (expression) {
                case VariableExpression ve -> M = ve.act(print, prefix, log, this, this.ns, identifiers, M, quantify);
                case ArithmeticExpression ae -> M = ae.act(print, prefix, log, identifiers, M, quantify);
                case NumberLiteralExpression ne -> M = ne.act(print, prefix, log, this, identifiers, quantify, M);
                case AutomatonExpression ae -> M = ae.act(print, prefix, name, log, i, M, identifiers);
                case null, default -> expression.act("argument " + (i + 1) + " of function " + this);
            }
        }
        A.bind(identifiers);
        A = AutomatonLogicalOps.and(A, M, print, prefix + " ", log);
        AutomatonLogicalOps.quantify(A, new HashSet<>(quantify), print, prefix + " ", log);

        S.push(new AutomatonExpression(stringValue, A));
        UtilityMethods.logAndPrint(print, prefix + "computed " + stringValue, log);
    }
}
