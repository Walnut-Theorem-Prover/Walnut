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

import Automata.AutomatonLogicalOps;
import Main.Expression;
import Automata.Automaton;
import Automata.NumberSystem;
import Main.Expressions.*;

import java.util.HashSet;
import java.util.Stack;


public class RelationalOperator extends Operator {
    NumberSystem number_system;

    public RelationalOperator(int position, String type, NumberSystem number_system) {
        this.op = type;
        setPriority();
        setArity(2);
        setPositionInPredicate(position);
        this.number_system = number_system;
    }

    public String toString() {
        return op + "_" + number_system;
    }

    public void act(Stack<Expression> S, boolean print, String prefix, StringBuilder log) {

        if (S.size() < getArity()) throw new RuntimeException("operator " + op + " requires " + getArity() + " operands");
        Expression b = S.pop();
        Expression a = S.pop();

        if ((a instanceof NumberLiteralExpression || a instanceof AlphabetLetterExpression) && (b instanceof NumberLiteralExpression || b instanceof AlphabetLetterExpression)) {
            S.push(new AutomatonExpression(a + op + b, new Automaton(compare(op, a.constant, b.constant))));
            return;
        }
        String preStep = prefix + "computing " + a + op + b;
        log.append(preStep + System.lineSeparator());
        if (print) {
            System.out.println(preStep);
        }
        if ((a instanceof WordExpression && (b instanceof ArithmeticExpression || b instanceof VariableExpression)) ||
                ((a instanceof ArithmeticExpression || a instanceof VariableExpression) && b instanceof WordExpression)) {
            /* We rewrite T[a] < b as
             * (T[a] = @0 => 0 < b) & (T[a] = @1 => 1 < b)
             * With more statements of the form (T[a] = @i => i < b) for each output i.
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

            Automaton M = new Automaton(true);
            for (int o : word.W.O) {
                Automaton N = word.W.clone();
                AutomatonLogicalOps.compare(N, o, "=", print, prefix + " ", log);
                Automaton C;
                if (reverse) {
                    C = number_system.comparison(arithmetic.identifier, o, op);
                } else {
                    C = number_system.comparison(o, arithmetic.identifier, op);
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
            S.push(new AutomatonExpression(word.toString(), M));
        } else if ((a instanceof ArithmeticExpression || a instanceof VariableExpression)
                && (b instanceof ArithmeticExpression || b instanceof VariableExpression)) {
            Automaton M = number_system.comparison(a.identifier, b.identifier, op);
            if (a instanceof ArithmeticExpression) {
                M = AutomatonLogicalOps.and(M, a.M, print, prefix + " ", log);
                AutomatonLogicalOps.quantify(M, a.identifier, print, prefix + " ", log);
            }
            if (b instanceof ArithmeticExpression) {
                M = AutomatonLogicalOps.and(M, b.M, print, prefix + " ", log);
                AutomatonLogicalOps.quantify(M, b.identifier, print, prefix + " ", log);
            }

            S.push(new AutomatonExpression(a + op + b, M));
        } else if ((a instanceof NumberLiteralExpression || a instanceof AlphabetLetterExpression) && (b instanceof ArithmeticExpression || b instanceof VariableExpression)) {
            Automaton M = number_system.comparison(a.constant, b.identifier, op);
            if (b instanceof ArithmeticExpression) {
                M = AutomatonLogicalOps.and(M, b.M, print, prefix + " ", log);
                AutomatonLogicalOps.quantify(M, b.identifier, print, prefix + " ", log);
            }
            S.push(new AutomatonExpression(a + op + b, M));
        } else if ((a instanceof ArithmeticExpression || a instanceof VariableExpression) && (b instanceof NumberLiteralExpression || b instanceof AlphabetLetterExpression)) {
            Automaton M = number_system.comparison(a.identifier, b.constant, op);
            if (a instanceof ArithmeticExpression) {
                M = AutomatonLogicalOps.and(M, a.M, print, prefix + " ", log);
                AutomatonLogicalOps.quantify(M, a.identifier, print, prefix + " ", log);
            }
            S.push(new AutomatonExpression(a + op + b, M));
        } else if (a instanceof WordExpression && b instanceof WordExpression) {
            Automaton M = AutomatonLogicalOps.compare(a.W, b.W, op, print, prefix + " ", log);
            M = AutomatonLogicalOps.and(M, a.M, print, prefix + " ", log);
            M = AutomatonLogicalOps.and(M, b.M, print, prefix + " ", log);
            AutomatonLogicalOps.quantify(M, new HashSet<>(((WordExpression)a).list_of_identifiers_to_quantify), print, prefix + " ", log);
            AutomatonLogicalOps.quantify(M, new HashSet<>(((WordExpression)b).list_of_identifiers_to_quantify), print, prefix + " ", log);
            S.push(new AutomatonExpression(a + op + b, M));
        } else if (a instanceof WordExpression && (b instanceof NumberLiteralExpression|| b instanceof AlphabetLetterExpression)) {
            AutomatonLogicalOps.compare(a.W, b.constant, op, print, prefix + " ", log);
            Automaton M = a.W;
            M = AutomatonLogicalOps.and(M, a.M, print, prefix + " ", log);
            AutomatonLogicalOps.quantify(M, new HashSet<>(((WordExpression)a).list_of_identifiers_to_quantify), print, prefix + " ", log);
            S.push(new AutomatonExpression(a + op + b, M));
        } else if ((a instanceof NumberLiteralExpression || a instanceof AlphabetLetterExpression) && b instanceof WordExpression) {
            AutomatonLogicalOps.compare(b.W, a.constant, reverseOperator(op), print, prefix + " ", log);
            Automaton M = b.W;
            M = AutomatonLogicalOps.and(M, b.M, print, prefix + " ", log);
            AutomatonLogicalOps.quantify(M, new HashSet<>(((WordExpression)b).list_of_identifiers_to_quantify), print, prefix + " ", log);
            S.push(new AutomatonExpression(a + op + b, M));
        } else {
            throw new RuntimeException("operator " + op + " cannot be applied to operands " + a + " and " + b + " of types " + a.getClass().getName() + " and " + b.getClass().getName() + " respectively");
        }
        String postStep = prefix + "computed " + a + op + b;
        log.append(postStep + System.lineSeparator());
        if (print) {
            System.out.println(postStep);
        }
    }

    private static boolean compare(String op, int a, int b) {
        return switch (op) {
            case "=" -> a == b;
            case "!=" -> a != b;
            case "<" -> a < b;
            case ">" -> a > b;
            case "<=" -> a <= b;
            case ">=" -> a >= b;
            default -> false;
        };
    }

    private static String reverseOperator(String op) {
        return switch (op) {
            case "=" -> "=";
            case "!=" -> "!=";
            case "<" -> ">";
            case ">" -> "<";
            case "<=" -> ">=";
            case ">=" -> "<=";
            default -> "";
        };
    }
}
