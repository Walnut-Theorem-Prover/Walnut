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
import Main.UtilityMethods;
import Automata.Automaton;
import Automata.NumberSystem;


public class Function extends Token {
    Automaton A;
    String name;
    NumberSystem ns;


    public Function(String number_system, int position, String name, Automaton A, int number_of_arguments) throws Exception {
        this.name = name;
        setArity(number_of_arguments);
        setPositionInPredicate(position);
        this.A = A;
        this.ns = new NumberSystem(number_system);
        if (A.getArity() != getArity())
            throw new Exception("function " + name + " requires " + A.getArity() + " arguments: char at " + getPositionInPredicate());
    }

    public String toString() {
        return name;
    }

    public void act(Stack<Expression> S, boolean print, String prefix, StringBuilder log) throws Exception {
        if (S.size() < getArity()) throw new Exception("function " + name + " requires " + getArity() + " arguments");
        Stack<Expression> temp = new Stack<>();
        for (int i = 0; i < getArity(); i++) {
            temp.push(S.pop());
        }
        String stringValue = name + "(";
        String preStep = prefix + "computing " + stringValue + "...)";
        log.append(preStep + UtilityMethods.newLine());
        if (print) {
            System.out.println(preStep);
        }
        Automaton M = new Automaton(true);
        List<String> identifiers = new ArrayList<>();
        List<String> quantify = new ArrayList<>();
        List<Expression> args = new ArrayList<>(getArity());
        for (int i = 0; i < getArity(); i++) {
            args.add(temp.pop());
        }
        stringValue += UtilityMethods.genericListString(args, ",") + "))";
        for (int i = 0; i < getArity(); i++) {
            Expression currentArg = args.get(i);

            switch (currentArg.T) {
                case variable:
                    if (!identifiers.contains(currentArg.identifier)) {
                        identifiers.add(currentArg.identifier);
                    } else {
                        String new_identifier = currentArg.identifier + getUniqueString();
                        Automaton eq = this.ns.equality.clone();
                        eq.bind(currentArg.identifier, new_identifier);
                        M = AutomatonLogicalOps.and(M, eq, print, prefix + " ", log);
                        quantify.add(new_identifier);
                        identifiers.add(new_identifier);
                    }
                    break;
                case arithmetic:
                    identifiers.add(currentArg.identifier);
                    M = AutomatonLogicalOps.and(M, currentArg.M, print, prefix + " ", log);
                    quantify.add(currentArg.identifier);
                    break;
                case numberLiteral:
                    Automaton constant = currentArg.base.get(currentArg.constant);
                    String id = getUniqueString();
                    constant.bind(id);
                    identifiers.add(id);
                    quantify.add(id);
                    M = AutomatonLogicalOps.and(M, constant, print, prefix + " ", log);
                    break;
                case automaton:
                    if (currentArg.M.getArity() != 1) {
                        throw new Exception("argument " + (i + 1) + " of function " + name + " cannot be an automaton with != 1 inputs");
                    }
                    if (!currentArg.M.isBound()) {
                        throw new Exception("argument " + (i + 1) + " of function " + name + " cannot be an automaton with unlabeled input");
                    }
                    M = AutomatonLogicalOps.and(M, currentArg.M, print, prefix + " ", log);
                    identifiers.add(currentArg.M.getLabel().get(0));
                    break;
                default:
                    throw new Exception("argument " + (i + 1) + " of function " + name + " cannot be of type " + currentArg.getType());
            }

        }
        A.bind(identifiers);
        A = AutomatonLogicalOps.and(A, M, print, prefix + " ", log);
        AutomatonLogicalOps.quantify(A, new HashSet<>(quantify), print, prefix + " ", log);

        S.push(new Expression(stringValue, A));
        String postStep = prefix + "computed " + stringValue;
        log.append(postStep + UtilityMethods.newLine());
        if (print) {
            System.out.println(postStep);
        }
    }
}
