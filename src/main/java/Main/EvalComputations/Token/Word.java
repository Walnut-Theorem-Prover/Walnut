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

import Main.EvalComputations.Expressions.*;
import Main.EvalComputations.Expressions.Expression;
import Automata.Automaton;
import Main.UtilityMethods;

public class Word extends Token {
    private final Automaton wordAutomaton;
    private final String name;

    public Word(int position, String name, Automaton wordAutomaton, int indexCount) {
        this.name = name;
        setPositionInPredicate(position);
        this.wordAutomaton = wordAutomaton;
        setArity(indexCount);
        super.validateArity(name, wordAutomaton.getArity());
    }

    public String toString() {
        return name;
    }

    public void act(Stack<Expression> S, boolean print, String prefix, StringBuilder log) {
        super.validateArity(S, "word ", " indices");
        Stack<Expression> temp = reverseStack(S);
        StringBuilder stringValue = new StringBuilder(name);
        UtilityMethods.logAndPrint(print, prefix + "computing " + stringValue + "[...]", log);
        List<String> identifiers = new ArrayList<>();
        List<String> quantify = new ArrayList<>();
        Automaton M = new Automaton(true);
        for (int i = 0; i < getArity(); i++) {
            Expression expression = temp.pop();
            stringValue.append("[").append(expression).append("]");
            if (expression instanceof VariableExpression ve) {
                M = ve.act(print, prefix, log, this, wordAutomaton.getNS().get(i), identifiers, M, quantify);
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
        wordAutomaton.bind(identifiers);
        S.push(new WordExpression(stringValue.toString(), wordAutomaton, M, quantify));
        UtilityMethods.logAndPrint(print, prefix + "computed " + stringValue, log);
    }
}
