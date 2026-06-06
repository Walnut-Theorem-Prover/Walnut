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
import Main.Logging;
import Main.WalnutException;
import Main.EvalComputations.Expressions.Expression;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import static Main.Logging.COMPUTED;
import static Main.Logging.COMPUTING;


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

    @SuppressWarnings("this-escape")
    public RelationalOperator(int position, String type, NumberSystem ns) {
        this.op = type;
        this.opp = Ops.fromSymbol(type);
        setPriority();
        this.arity = 2;
        this.positionInPredicate = position;
        this.ns = ns;
    }

    public String toString() {
        return op + "_" + ns;
    }

    public void act(Stack<Expression> S) {
        super.validateArity(S);
        Expression b = S.pop();
        Expression a = S.pop();

        if (isConstantExpression(a) && isConstantExpression(b)) {
            S.push(new AutomatonExpression(a + op + b, new Automaton(compare(opp, getConstantValue(a), getConstantValue(b)))));
            return;
        }
        Logging.logAndPrint( COMPUTING + " " + a + op + b);
        Logging.indent();

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
                WordAutomaton.compareWordAutomaton(N, o, Ops.EQUAL);
                Automaton C;
                if (reverse) {
                    C = ns.comparison(arithmetic.identifier, o, opp);
                } else {
                    C = ns.comparison(o, arithmetic.identifier, opp);
                }
                N = AutomatonLogicalOps.imply(N, C, LogicalOperator.IMPLY);
                M = AutomatonLogicalOps.and(M, N);
            }
            M = AutomatonLogicalOps.and(M, word.M);
            AutomatonQuantification.quantify(M, word.identifiersToQuantify);
            M = andThenQuantifyIfArithmetic(arithmetic, M);
            S.push(new AutomatonExpression(word.toString(), M));
        } else if ((a instanceof ArithmeticExpression || a instanceof VariableExpression)
                && (b instanceof ArithmeticExpression || b instanceof VariableExpression)) {
            Automaton M = ns.comparison(a.identifier, b.identifier, opp);
            M = andThenQuantifyIfArithmetic(a, M);
            M = andThenQuantifyIfArithmetic(b, M);
            S.push(new AutomatonExpression(a + op + b, M));
        } else if ((a instanceof NumberLiteralExpression || a instanceof AlphabetLetterExpression) && (b instanceof ArithmeticExpression || b instanceof VariableExpression)) {
            Automaton M = a instanceof NumberLiteralExpression ne
                ? ns.comparison(ne.value(), b.identifier, opp)
                : ns.comparison(a.constant, b.identifier, opp);
            M = andThenQuantifyIfArithmetic(b, M);
            S.push(new AutomatonExpression(a + op + b, M));
        } else if ((a instanceof ArithmeticExpression || a instanceof VariableExpression) && (b instanceof NumberLiteralExpression || b instanceof AlphabetLetterExpression)) {
            Automaton M = b instanceof NumberLiteralExpression ne
                ? ns.comparison(a.identifier, ne.value(), opp)
                : ns.comparison(a.identifier, b.constant, opp);
            M = andThenQuantifyIfArithmetic(a, M);
            S.push(new AutomatonExpression(a + op + b, M));
        } else if (a instanceof WordExpression && b instanceof WordExpression) {
            Automaton M = WordAutomaton.compareWordAutomata(a.wordAutomaton, b.wordAutomaton, op);
            M = AutomatonLogicalOps.and(M, a.M);
            M = AutomatonLogicalOps.and(M, b.M);
            AutomatonQuantification.quantify(M, ((WordExpression)a).identifiersToQuantify);
            AutomatonQuantification.quantify(M, ((WordExpression)b).identifiersToQuantify);
            S.push(new AutomatonExpression(a + op + b, M));
        } else if (a instanceof WordExpression && (b instanceof NumberLiteralExpression|| b instanceof AlphabetLetterExpression)) {
            WordAutomaton.compareWordAutomaton(a.wordAutomaton, getIntConstantForWord(b), opp);
            Automaton M = a.wordAutomaton;
            M = AutomatonLogicalOps.and(M, a.M);
            AutomatonQuantification.quantify(M, ((WordExpression)a).identifiersToQuantify);
            S.push(new AutomatonExpression(a + op + b, M));
        } else if ((a instanceof NumberLiteralExpression || a instanceof AlphabetLetterExpression) && b instanceof WordExpression) {
            WordAutomaton.compareWordAutomaton(b.wordAutomaton, getIntConstantForWord(a), reverseOperator(opp));
            Automaton M = b.wordAutomaton;
            M = AutomatonLogicalOps.and(M, b.M);
            AutomatonQuantification.quantify(M, ((WordExpression)b).identifiersToQuantify);
            S.push(new AutomatonExpression(a + op + b, M));
        } else {
            throw WalnutException.invalidDualOperators(op, a, b);
        }
        Logging.dedent();
        Logging.logAndPrint( COMPUTED + " " + a + op + b);
    }

    public static boolean compare(Ops op, int a, int b) {
        return compare(op, BigInteger.valueOf(a), BigInteger.valueOf(b));
    }

    public static boolean compare(Ops op, BigInteger a, BigInteger b) {
        int comparison = a.compareTo(b);
        return switch (op) {
            case EQUAL -> comparison == 0;
            case NOT_EQUAL -> comparison != 0;
            case LESS_THAN -> comparison < 0;
            case GREATER_THAN -> comparison > 0;
            case LESS_EQ_THAN -> comparison <= 0;
            case GREATER_EQ_THAN -> comparison >= 0;
        };
    }

    private static int getIntConstantForWord(Expression e) {
        if (e instanceof NumberLiteralExpression ne) {
            return ne.intValueExact("number literal " + ne + " used in word automaton output comparison");
        }
        return e.constant;
    }

    private static boolean isConstantExpression(Expression e) {
        return e instanceof NumberLiteralExpression || e instanceof AlphabetLetterExpression;
    }

    private static BigInteger getConstantValue(Expression e) {
        if (e instanceof NumberLiteralExpression ne) {
            return ne.value();
        }
        return BigInteger.valueOf(e.constant);
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
