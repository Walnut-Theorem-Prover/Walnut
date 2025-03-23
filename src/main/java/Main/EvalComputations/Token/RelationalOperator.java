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

import Automata.*;
import Main.EvalComputations.Expressions.*;
import Main.ExceptionHelper;
import Main.EvalComputations.Expressions.Expression;
import Main.UtilityMethods;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;


public class RelationalOperator extends Operator {
    private final NumberSystem ns;
    private final Ops opp;
    public static final Map<String, Ops> RELATIONAL_OPERATORS = new HashMap<>();
    static {
        for (RelationalOperator.Ops op : RelationalOperator.Ops.values()) {
            RELATIONAL_OPERATORS.put(op.getSymbol(), op);
        }
    }
    public enum Ops {
        EQUAL("="),
        NOT_EQUAL("!="),
        LESS_THAN("<"),
        GREATER_THAN(">"),
        LESS_EQ_THAN("<="),
        GREATER_EQ_THAN(">=");

        private final String symbol;

        Ops(String symbol) {
            this.symbol = symbol;
        }

        public String getSymbol() {
            return symbol;
        }

        public static Ops fromSymbol(String symbol) {
            for (Ops op : values()) {
                if (op.symbol.equals(symbol)) {
                    return op;
                }
            }
            throw new IllegalArgumentException("Unknown comparison operator: " + symbol);
        }
    }


    public RelationalOperator(int position, String type, NumberSystem ns) {
        this.op = type;
        this.opp = Ops.fromSymbol(type);
        setPriority();
        setArity(2);
        setPositionInPredicate(position);
        this.ns = ns;
    }

    public String toString() {
        return op + "_" + ns;
    }

    public void act(Stack<Expression> S, boolean print, String prefix, StringBuilder log) {
        super.validateArity(S);
        Expression b = S.pop();
        Expression a = S.pop();

        if ((a instanceof NumberLiteralExpression || a instanceof AlphabetLetterExpression) && (b instanceof NumberLiteralExpression || b instanceof AlphabetLetterExpression)) {
            S.push(new AutomatonExpression(a + op + b, new Automaton(compare(opp, a.constant, b.constant))));
            return;
        }
        UtilityMethods.logAndPrint(print, prefix + "computing " + a + op + b, log);
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
            for (int o : word.wordAutomaton.fa.getO()) {
                Automaton N = word.wordAutomaton.clone();
                WordAutomaton.compareWordAutomaton(N.fa, o, Ops.EQUAL, print, prefix + " ", log);
                Automaton C;
                if (reverse) {
                    C = ns.comparison(arithmetic.identifier, o, opp);
                } else {
                    C = ns.comparison(o, arithmetic.identifier, opp);
                }
                N = AutomatonLogicalOps.imply(N, C, print, prefix + " ", log, LogicalOperator.IMPLY);
                M = AutomatonLogicalOps.and(M, N, print, prefix + " ", log);
            }
            M = AutomatonLogicalOps.and(M, word.M, print, prefix + " ", log);
            AutomatonQuantification.quantify(M, word.identifiersToQuantify, print, prefix + " ", log);
            M = andQuantifyIfArithmetic(print, prefix, log, arithmetic, M);
            S.push(new AutomatonExpression(word.toString(), M));
        } else if ((a instanceof ArithmeticExpression || a instanceof VariableExpression)
                && (b instanceof ArithmeticExpression || b instanceof VariableExpression)) {
            Automaton M = ns.comparison(a.identifier, b.identifier, opp);
            M = andQuantifyIfArithmetic(print, prefix, log, a, M);
            M = andQuantifyIfArithmetic(print, prefix, log, b, M);
            S.push(new AutomatonExpression(a + op + b, M));
        } else if ((a instanceof NumberLiteralExpression || a instanceof AlphabetLetterExpression) && (b instanceof ArithmeticExpression || b instanceof VariableExpression)) {
            Automaton M = ns.comparison(a.constant, b.identifier, opp);
            M = andQuantifyIfArithmetic(print, prefix, log, b, M);
            S.push(new AutomatonExpression(a + op + b, M));
        } else if ((a instanceof ArithmeticExpression || a instanceof VariableExpression) && (b instanceof NumberLiteralExpression || b instanceof AlphabetLetterExpression)) {
            Automaton M = ns.comparison(a.identifier, b.constant, opp);
            M = andQuantifyIfArithmetic(print, prefix, log, a, M);
            S.push(new AutomatonExpression(a + op + b, M));
        } else if (a instanceof WordExpression && b instanceof WordExpression) {
            Automaton M = AutomatonLogicalOps.compare(a.wordAutomaton, b.wordAutomaton, op, print, prefix + " ", log);
            M = AutomatonLogicalOps.and(M, a.M, print, prefix + " ", log);
            M = AutomatonLogicalOps.and(M, b.M, print, prefix + " ", log);
            AutomatonQuantification.quantify(M, ((WordExpression)a).identifiersToQuantify, print, prefix + " ", log);
            AutomatonQuantification.quantify(M, ((WordExpression)b).identifiersToQuantify, print, prefix + " ", log);
            S.push(new AutomatonExpression(a + op + b, M));
        } else if (a instanceof WordExpression && (b instanceof NumberLiteralExpression|| b instanceof AlphabetLetterExpression)) {
            WordAutomaton.compareWordAutomaton(a.wordAutomaton.fa, b.constant, opp, print, prefix + " ", log);
            Automaton M = a.wordAutomaton;
            M = AutomatonLogicalOps.and(M, a.M, print, prefix + " ", log);
            AutomatonQuantification.quantify(M, ((WordExpression)a).identifiersToQuantify, print, prefix + " ", log);
            S.push(new AutomatonExpression(a + op + b, M));
        } else if ((a instanceof NumberLiteralExpression || a instanceof AlphabetLetterExpression) && b instanceof WordExpression) {
            WordAutomaton.compareWordAutomaton(b.wordAutomaton.fa, a.constant, reverseOperator(opp), print, prefix + " ", log);
            Automaton M = b.wordAutomaton;
            M = AutomatonLogicalOps.and(M, b.M, print, prefix + " ", log);
            AutomatonQuantification.quantify(M, ((WordExpression)b).identifiersToQuantify, print, prefix + " ", log);
            S.push(new AutomatonExpression(a + op + b, M));
        } else {
            throw ExceptionHelper.invalidDualOperators(op, a, b);
        }
        UtilityMethods.logAndPrint(print, prefix + "computed " + a + op + b, log);
    }

    private static Automaton andQuantifyIfArithmetic(boolean print, String prefix, StringBuilder log, Expression arithmetic, Automaton M) {
        if (arithmetic instanceof ArithmeticExpression) {
            M = AutomatonLogicalOps.and(M, arithmetic.M, print, prefix + " ", log);
            AutomatonQuantification.quantify(M, arithmetic.identifier, print, prefix + " ", log);
        }
        return M;
    }

    public static boolean compare(Ops op, int a, int b) {
        return switch (op) {
            case EQUAL -> a == b;
            case NOT_EQUAL -> a != b;
            case LESS_THAN -> a < b;
            case GREATER_THAN -> a > b;
            case LESS_EQ_THAN -> a <= b;
            case GREATER_EQ_THAN -> a >= b;
        };
    }

    public static Ops reverseOperator(Ops op) {
        return switch (op) {
            case EQUAL -> Ops.EQUAL;
            case NOT_EQUAL -> Ops.NOT_EQUAL;
            case LESS_THAN -> Ops.GREATER_THAN;
            case GREATER_THAN -> Ops.LESS_THAN;
            case LESS_EQ_THAN -> Ops.GREATER_EQ_THAN;
            case GREATER_EQ_THAN -> Ops.LESS_EQ_THAN;
        };
    }
}
