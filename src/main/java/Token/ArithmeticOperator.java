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

import java.util.HashSet;
import java.util.Stack;

import Automata.AutomatonLogicalOps;
import Main.Expression;
import Automata.Automaton;
import Automata.NumberSystem;
import Main.Expressions.*;


public class ArithmeticOperator extends Operator {
    NumberSystem number_system;

    public ArithmeticOperator(int position, String op, NumberSystem number_system) {
        this.op = op;
        setPriority();
        if (op.equals("_")) {
            setArity(1);
        } else {
            setArity(2);
        }
        setPositionInPredicate(position);
        this.number_system = number_system;
    }

    public String toString() {
        return op + "_" + number_system;
    }

    public void act(Stack<Expression> S, boolean print, String prefix, StringBuilder log) {
        if (S.size() < getArity()) throw new RuntimeException("operator " + op + " requires " + getArity() + " operands");
        Expression b = S.pop();
        if (!(b instanceof AlphabetLetterExpression || b instanceof WordExpression || b instanceof ArithmeticExpression || b instanceof VariableExpression || b instanceof NumberLiteralExpression))
            throw new RuntimeException("operator " + op + " cannot be applied to the operand " + b + " of type " + b.getClass().getName());

        if (op.equals("_")) {
            if (b instanceof NumberLiteralExpression) {
                S.push(new NumberLiteralExpression(Integer.toString(-b.constant), -b.constant, number_system));
                return;
            } else if (b instanceof AlphabetLetterExpression) {
                S.push(new AlphabetLetterExpression("@" + (-b.constant), -b.constant));
                return;
            } else if (b instanceof WordExpression) {
                b.W.applyOperator(0, "_", print, prefix, log);
                S.push(b);
                return;
            }
            String c = getUniqueString();
            // b + c = 0
            Automaton M = number_system.arithmetic(b.identifier, c, 0, "+");
            String preStep = prefix + "computing " + op + b;
            log.append(preStep + System.lineSeparator());
            if (print) {
                System.out.println(preStep);
            }
            if (b instanceof ArithmeticExpression) {
                // Eb, b + c = 0 & M(b,...)
                M = AutomatonLogicalOps.and(M, b.M, print, prefix + " ", log);
                AutomatonLogicalOps.quantify(M, b.identifier, print, prefix + " ", log);
            }
            S.push(new ArithmeticExpression("(" + op + b + ")", M, c));
            String postStep = prefix + "computed " + op + b;
            log.append(postStep + System.lineSeparator());
            if (print) {
                System.out.println(postStep);
            }
            return;
        }

        Expression a = S.pop();
        if (!(a instanceof AlphabetLetterExpression || a instanceof WordExpression || a instanceof ArithmeticExpression || a instanceof VariableExpression || a instanceof NumberLiteralExpression))
            throw new RuntimeException("operator " + op + " cannot be applied to the operand " + a + " of type " + a.getClass().getName());

        if (a instanceof WordExpression && b instanceof WordExpression) {
            a.W = AutomatonLogicalOps.applyOperator(a.W, b.W, op, print, prefix, log);
            a.M = AutomatonLogicalOps.and(a.M, b.M, print, prefix + " ", log);
            ((WordExpression)a).list_of_identifiers_to_quantify.addAll(((WordExpression)b).list_of_identifiers_to_quantify);
            S.push(a);
            return;
        }
        if (a instanceof WordExpression && (b instanceof AlphabetLetterExpression || b instanceof NumberLiteralExpression)) {
            a.W.applyOperator(op, b.constant, print, prefix, log);
            S.push(a);
            return;
        }
        if ((a instanceof AlphabetLetterExpression || a instanceof NumberLiteralExpression) && b instanceof WordExpression) {
            b.W.applyOperator(a.constant, op, print, prefix, log);
            S.push(b);
            return;
        }

        if (
            (a instanceof NumberLiteralExpression || a instanceof AlphabetLetterExpression) && (b instanceof NumberLiteralExpression)) {
            switch (op) {
                case "+":
                    S.push(new NumberLiteralExpression(Integer.toString(a.constant + b.constant), a.constant + b.constant, number_system));
                    return;
                case "*":
                    S.push(new NumberLiteralExpression(Integer.toString(a.constant * b.constant), a.constant * b.constant, number_system));
                    return;
                case "/":
                    int c = Math.floorDiv(a.constant, b.constant);
                    S.push(new NumberLiteralExpression(Integer.toString(c), c, number_system));
                    return;
                case "-":
                    S.push(new NumberLiteralExpression(Integer.toString(a.constant - b.constant), a.constant - b.constant, number_system));
                    return;
            }
        }
        String c = getUniqueString();
        Automaton M;
        String preStep = prefix + "Computing " + a + op + b;
        log.append(preStep + System.lineSeparator());
        if (print) {
            System.out.println(preStep);
        }
        if (a instanceof WordExpression || (a instanceof ArithmeticExpression || a instanceof VariableExpression) && b instanceof WordExpression) {
            /* We rewrite T[a] * 5 = z as
             * (T[a] = @0 => 0 * 5 = z) & (T[a] = @1 => 1 * 5 = z)
             * With more statements of the form (T[a] = @i => i * 5 = z) for each output i.
             */
            WordExpression word;
            Expression arithmetic;
            boolean reverse;
            if (a instanceof WordExpression) {
                word = (WordExpression) a;
                arithmetic = b;
                reverse = false;
            } else {
                word = (WordExpression) b;
                arithmetic = a;
                reverse = true;
            }

            M = new Automaton(true);
            for (int o : word.W.getO()) {
                Automaton N = word.W.clone();
                AutomatonLogicalOps.compare(N, o, "=", print, prefix + " ", log);
                Automaton C;
                if (o == 0 && op.equals("*")) {
                    C = number_system.get(0);
                    C.bind(c);
                } else if (reverse) {
                    C = number_system.arithmetic(arithmetic.identifier, o, c, op);
                } else {
                    C = number_system.arithmetic(o, arithmetic.identifier, c, op);
                }
                N = AutomatonLogicalOps.imply(N, C, print, prefix + " ", log);
                M = AutomatonLogicalOps.and(M, N, print, prefix + " ", log);
            }
            M = AutomatonLogicalOps.and(M, word.M, print, prefix + " ", log);
            AutomatonLogicalOps.quantify(M, new HashSet<>(word.list_of_identifiers_to_quantify), print, prefix + " ", log);
            if (arithmetic instanceof ArithmeticExpression) {
                M = AutomatonLogicalOps.and(M, arithmetic.M, print, prefix + " ", log);
                AutomatonLogicalOps.quantify(M, arithmetic.identifier, print, prefix + " ", log);
            }
        } else {
            if (a instanceof NumberLiteralExpression) {
                if (a.constant == 0 && op.equals("*")) {
                    S.push(new NumberLiteralExpression("0", 0, number_system));
                    return;
                } else
                    M = number_system.arithmetic(a.constant, b.identifier, c, op);
            } else if (b instanceof NumberLiteralExpression) {
                if (b.constant == 0 && op.equals("*")) {
                    S.push(new NumberLiteralExpression("0", 0, number_system));
                    return;
                }
                M = number_system.arithmetic(a.identifier, b.constant, c, op);
            } else {
                M = number_system.arithmetic(a.identifier, b.identifier, c, op);
            }

            if (a instanceof ArithmeticExpression) {
                M = AutomatonLogicalOps.and(M, a.M, print, prefix + " ", log);
                AutomatonLogicalOps.quantify(M, a.identifier, print, prefix + " ", log);
            }
            if (b instanceof ArithmeticExpression) {
                M = AutomatonLogicalOps.and(M, b.M, print, prefix + " ", log);
                AutomatonLogicalOps.quantify(M, b.identifier, print, prefix + " ", log);
            }
        }
        S.push(new ArithmeticExpression("(" + a + op + b + ")", M, c));
        String postStep = prefix + "computed " + a + op + b;
        log.append(postStep + System.lineSeparator());
        if (print) {
            System.out.println(postStep);
        }
    }
}
