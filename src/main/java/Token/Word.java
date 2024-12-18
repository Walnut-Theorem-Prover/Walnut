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
import java.util.List;
import java.util.Stack;

import Automata.AutomatonLogicalOps;
import Main.Expression;
import Main.Expressions.*;
import Main.UtilityMethods;
import Automata.Automaton;

public class Word extends Token {
    Automaton W;
    String name;

    public Word(int position, String name, Automaton W, int number_of_indices) {
        this.name = name;
        setPositionInPredicate(position);
        this.W = W;
        setArity(number_of_indices);
        if (W.getArity() != getArity())
            throw new RuntimeException("word " + name + " requires " + W.getArity() + " indices: char at " + getPositionInPredicate());
    }

    public String toString() {
        return name;
    }

    public void act(Stack<Expression> S, boolean print, String prefix, StringBuilder log) {
        if (S.size() < getArity()) throw new RuntimeException("word " + name + " requires " + getArity() + " indices");
        Stack<Expression> temp = new Stack<>();
        for (int i = 1; i <= getArity(); i++) {
            temp.push(S.pop());
        }
        String stringValue = name;
        String preStep = prefix + "computing " + stringValue + "[...]";
        log.append(preStep + System.lineSeparator());
        if (print) {
            System.out.println(preStep);
        }
        List<String> identifiers = new ArrayList<>();
        List<String> quantify = new ArrayList<>();
        Automaton M = new Automaton(true);
        for (int i = 0; i < getArity(); i++) {
            Expression expression = temp.pop();
            stringValue += "[" + expression + "]";
            switch (expression) {
                case VariableExpression ve -> M = ve.act(print, prefix, log, this, W.NS.get(i), identifiers, M, quantify);
                case ArithmeticExpression ae -> M = ae.act(print, prefix, log, identifiers, M, quantify);
                case NumberLiteralExpression ne -> M = ne.act(print, prefix, log, this, identifiers, quantify, M);
                case AutomatonExpression ae -> M = ae.act(print, prefix, name, log, i, M, identifiers);
                case null, default -> expression.act("argument " + (i + 1) + " of function " + name);
            }
        }
        W.bind(identifiers);
        S.push(new WordExpression(stringValue, W, M, quantify));
        String postStep = prefix + "computed " + stringValue;
        log.append(postStep + System.lineSeparator());
        if (print) {
            System.out.println(postStep);
        }
    }
}
