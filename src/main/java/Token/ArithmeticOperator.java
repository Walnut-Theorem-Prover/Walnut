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

import java.util.List;
import java.util.Stack;

import Automata.AutomatonLogicalOps;
import Main.ExceptionHelper;
import Main.Expression;
import Automata.Automaton;
import Automata.NumberSystem;
import Main.Expressions.*;
import Main.UtilityMethods;


public class ArithmeticOperator extends Operator {
    private final NumberSystem ns;

    public ArithmeticOperator(int position, String op, NumberSystem ns) {
        this.op = op;
        setPriority();
        setArity(op.equals("_") ? 1 : 2);
        setPositionInPredicate(position);
        this.ns = ns;
    }

    public String toString() {
        return op + "_" + ns;
    }

    public void act(Stack<Expression> S, boolean print, String prefix, StringBuilder log) {
        super.validateArity(S);
        Expression b = S.pop();
        if (!isValidArithmeticOperator(b))
            throw ExceptionHelper.invalidOperator(op, b);
        if (op.equals("_")) {
            processUnaryOperator(b, S, print, prefix, log);
        } else {
            processBinaryOperator(b, S, print, prefix, log);
        }
    }

    private void processUnaryOperator(Expression b, Stack<Expression> S, boolean print, String prefix, StringBuilder log) {
        if (b instanceof NumberLiteralExpression) {
            S.push(new NumberLiteralExpression(Integer.toString(-b.constant), -b.constant, ns));
            return;
        }
        if (b instanceof AlphabetLetterExpression) {
            S.push(new AlphabetLetterExpression("@" + (-b.constant), -b.constant));
            return;
        }
        if (b instanceof WordExpression) {
            b.wordAutomaton.applyWordOperator(0, "-", false, print, prefix, log);
            S.push(b);
            return;
        }

        String c = getUniqueString();
        // b + c = 0
        Automaton M = ns.arithmetic(b.identifier, c, 0, "+");
        UtilityMethods.logAndPrint(print, prefix + "computing " + op + b, log);
        M = andAndQuantifyArithmeticExpression(print, prefix, log, b, M);
        S.push(new ArithmeticExpression("(" + op + b + ")", M, c));
        UtilityMethods.logAndPrint(print, prefix + "computed " + op + b, log);
    }

    private void processBinaryOperator(Expression b, Stack<Expression> S, boolean print, String prefix, StringBuilder log) {
        Expression a = S.pop();
        if (!isValidArithmeticOperator(a))
            throw ExceptionHelper.invalidOperator(op, a);

        if (a instanceof WordExpression && b instanceof WordExpression) {
            a.wordAutomaton = AutomatonLogicalOps.applyOperator(a.wordAutomaton, b.wordAutomaton, op, print, prefix, log);
            a.M = AutomatonLogicalOps.and(a.M, b.M, print, prefix + " ", log);
            ((WordExpression)a).identifiersToQuantify.addAll(((WordExpression)b).identifiersToQuantify);
            S.push(a);
            return;
        }
        if (a instanceof WordExpression && (b instanceof AlphabetLetterExpression || b instanceof NumberLiteralExpression)) {
            a.wordAutomaton.applyWordOperator(b.constant, op, true, print, prefix, log);
            S.push(a);
            return;
        }
        if ((a instanceof AlphabetLetterExpression || a instanceof NumberLiteralExpression) && b instanceof WordExpression) {
            b.wordAutomaton.applyWordOperator(a.constant, op, false, print, prefix, log);
            S.push(b);
            return;
        }

        if (
            (a instanceof NumberLiteralExpression || a instanceof AlphabetLetterExpression) && (b instanceof NumberLiteralExpression)) {
            int value = ArithmeticOperator.arith(op, a.constant, b.constant);
            S.push(new NumberLiteralExpression(Integer.toString(value), value, ns));
            return;
        }
        String c = getUniqueString();
        Automaton M;
        UtilityMethods.logAndPrint(print, prefix + "Computing " + a + op + b, log);
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
            for (int o : word.wordAutomaton.getO()) {
                Automaton N = word.wordAutomaton.clone();
                AutomatonLogicalOps.compareWordAutomaton(N.fa, o, "=", print, prefix + " ", log);
                Automaton C;
                if (o == 0 && op.equals("*")) {
                    C = ns.get(0);
                    C.bind(List.of(c));
                } else if (reverse) {
                    C = ns.arithmetic(arithmetic.identifier, o, c, op);
                } else {
                    C = ns.arithmetic(o, arithmetic.identifier, c, op);
                }
                N = AutomatonLogicalOps.imply(N, C, print, prefix + " ", log, "=>");
                M = AutomatonLogicalOps.and(M, N, print, prefix + " ", log);
            }
            M = AutomatonLogicalOps.and(M, word.M, print, prefix + " ", log);
            AutomatonLogicalOps.quantify(M, word.identifiersToQuantify, print, prefix + " ", log);
            M = andAndQuantifyArithmeticExpression(print, prefix, log, arithmetic, M);
        } else {
            if (a instanceof NumberLiteralExpression) {
                if (a.constant == 0 && op.equals("*")) {
                    S.push(new NumberLiteralExpression("0", 0, ns));
                    return;
                }
                M = ns.arithmetic(a.constant, b.identifier, c, op);
            } else if (b instanceof NumberLiteralExpression) {
                if (b.constant == 0 && op.equals("*")) {
                    S.push(new NumberLiteralExpression("0", 0, ns));
                    return;
                }
                M = ns.arithmetic(a.identifier, b.constant, c, op);
            } else {
                M = ns.arithmetic(a.identifier, b.identifier, c, op);
            }

            M = andAndQuantifyArithmeticExpression(print, prefix, log, a, M);
            M = andAndQuantifyArithmeticExpression(print, prefix, log, b, M);
        }
        S.push(new ArithmeticExpression("(" + a + op + b + ")", M, c));
        UtilityMethods.logAndPrint(print, prefix + "computed " + a + op + b, log);
    }

    private static Automaton andAndQuantifyArithmeticExpression(boolean print, String prefix, StringBuilder log,
                                                                Expression a, Automaton M) {
        if (a instanceof ArithmeticExpression) {
            M = AutomatonLogicalOps.and(M, a.M, print, prefix + " ", log);
            AutomatonLogicalOps.quantify(M, a.identifier, print, prefix + " ", log);
        }
        return M;
    }


    public static int arith(String op, int a, int b) {
        switch (op) {
            case "+" -> {
                return a + b;
            }
            case "-" -> {
                return a - b;
            }
            case "/" -> {
                if (b == 0) throw ExceptionHelper.divisionByZero();
                return Math.floorDiv(a, b);
            }
            case "*" -> {
                return a * b;
            }
            default -> throw ExceptionHelper.unexpectedOperator(op);
        }
    }

    public static boolean isValidArithmeticOperator(Expression a) {
        return (a instanceof AlphabetLetterExpression || a instanceof WordExpression || a instanceof ArithmeticExpression || a instanceof VariableExpression || a instanceof NumberLiteralExpression);
    }
}
